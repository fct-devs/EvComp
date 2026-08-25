package br.unesp.fct.evcomp.service.pagamento;

import br.unesp.fct.evcomp.domain.Administrador;
import br.unesp.fct.evcomp.domain.Inscrição;
import br.unesp.fct.evcomp.domain.Pagamento;
import br.unesp.fct.evcomp.domain.StatusPagamento;
import br.unesp.fct.evcomp.domain.TipoArmazenamento;
import br.unesp.fct.evcomp.repository.InscricaoRepository;
import br.unesp.fct.evcomp.repository.PagamentoRepository;
import br.unesp.fct.evcomp.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PagamentoService {

    /** Teto de 1 MB */
    public static final int TAMANHO_MAXIMO_BYTES = 1024 * 1024;

    private static final Set<String> TIPOS_ACEITOS = Set.of(
        "image/webp", "image/jpeg", "image/png", "application/pdf"
    );

    private final PagamentoRepository pagamentoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArmazenamentoComprovanteFactory armazenamentoFactory;

    @Autowired
    public PagamentoService(PagamentoRepository pagamentoRepository,
                            InscricaoRepository inscricaoRepository,
                            UsuarioRepository usuarioRepository,
                            ArmazenamentoComprovanteFactory armazenamentoFactory) {
        this.pagamentoRepository = pagamentoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.armazenamentoFactory = armazenamentoFactory;
    }

    public Pagamento obterOuCriar(Inscrição inscricao) {
        Optional<Pagamento> existente = pagamentoRepository.buscarPorInscricao(inscricao.getId());

        if (existente.isPresent()) {
            return existente.get();
        }

        StatusPagamento inicial = inscricao.getEvento() != null && inscricao.getEvento().ehPago()
            ? StatusPagamento.PENDENTE
            : StatusPagamento.ISENTO;

        return pagamentoRepository.salvarPagamento(new Pagamento(inscricao, inicial));
    }

    public Pagamento buscarPorInscricao(Integer inscricaoId) {
        Inscrição inscricao = inscricaoRepository.findById(inscricaoId)
            .orElseThrow(() -> new IllegalArgumentException("Inscrição não encontrada."));

        return obterOuCriar(inscricao);
    }

    public Pagamento buscarPorId(Integer pagamentoId) {
        return pagamentoRepository.buscarPagamentoPorId(pagamentoId)
            .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado."));
    }

    public List<Pagamento> listarPorParticipante(Integer participanteId) {
        List<Inscrição> inscricoes = inscricaoRepository.buscarInscricoesPorParticipante(participanteId);
        List<Pagamento> pagamentos = new ArrayList<>();

        for (Inscrição inscricao : inscricoes) {
            pagamentos.add(obterOuCriar(inscricao));
        }

        return pagamentos;
    }

    public List<Pagamento> listarPendentes() {
        return pagamentoRepository.buscarPagamentosPendentes();
    }

    public List<Pagamento> listarPorEvento(Integer eventoId) {
        return pagamentoRepository.buscarPagamentosPorEvento(eventoId);
    }

    @Transactional
    public Pagamento registrarComprovante(Integer inscricaoId, MultipartFile arquivo) {
        Pagamento pagamento = buscarPorInscricao(inscricaoId);

        if (pagamento.getStatus() == StatusPagamento.ISENTO) {
            throw new IllegalArgumentException("Este evento não possui cobrança de inscrição.");
        }

        if (pagamento.getStatus() == StatusPagamento.APROVADO) {
            throw new IllegalArgumentException("O pagamento desta inscrição já foi aprovado.");
        }

        ArquivoComprovante novoArquivo = validarEExtrair(arquivo);

        removerArquivoAtual(pagamento);

        ArmazenamentoComprovante armazenamento = armazenamentoFactory.obterPadrao();
        String referencia = armazenamento.salvar(novoArquivo);

        pagamento.setArmazenamentoTipo(armazenamento.getTipo());
        pagamento.setArmazenamentoRef(referencia);
        pagamento.setNomeArquivoOriginal(novoArquivo.nomeOriginal());
        pagamento.setTipoArquivo(novoArquivo.tipoArquivo());
        pagamento.setTamanhoArquivo(novoArquivo.conteudo().length);
        pagamento.setDataEnvio(LocalDateTime.now());

        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setMotivoRecusa(null);
        pagamento.setDataAvaliacao(null);
        pagamento.setAvaliador(null);

        return pagamentoRepository.salvarPagamento(pagamento);
    }

    @Transactional
    public Pagamento avaliar(Integer pagamentoId, StatusPagamento novoStatus, String motivoRecusa, Integer adminId) {
        if (novoStatus != StatusPagamento.APROVADO && novoStatus != StatusPagamento.RECUSADO) {
            throw new IllegalArgumentException("O novo status deve ser APROVADO ou RECUSADO.");
        }

        Pagamento pagamento = buscarPorId(pagamentoId);

        if (pagamento.getStatus() == StatusPagamento.ISENTO) {
            throw new IllegalArgumentException("Este evento não possui cobrança de inscrição.");
        }

        if (!pagamento.temComprovante()) {
            throw new IllegalArgumentException("Não é possível avaliar um pagamento sem comprovante enviado.");
        }

        if (pagamento.getStatus() == novoStatus) {
            throw new IllegalArgumentException("Este pagamento já está marcado como " + novoStatus + ".");
        }

        pagamento.setStatus(novoStatus);
        pagamento.setMotivoRecusa(novoStatus == StatusPagamento.RECUSADO ? motivoRecusa : null);
        pagamento.setDataAvaliacao(LocalDateTime.now());
        pagamento.setAvaliador(buscarAdministrador(adminId));

        pagamento.getInscricao().setStatus(novoStatus == StatusPagamento.APROVADO);
        inscricaoRepository.salvarInscricao(pagamento.getInscricao());

        return pagamentoRepository.salvarPagamento(pagamento);
    }

    public byte[] obterConteudoComprovante(Pagamento pagamento) {
        if (!pagamento.temComprovante()) {
            throw new IllegalArgumentException("Nenhum comprovante foi enviado para este pagamento.");
        }

        return armazenamentoFactory
            .obterPorTipo(pagamento.getArmazenamentoTipo())
            .recuperar(pagamento.getArmazenamentoRef());
    }

    private void removerArquivoAtual(Pagamento pagamento) {
        if (!pagamento.temComprovante()) {
            return;
        }

        TipoArmazenamento tipoAtual = pagamento.getArmazenamentoTipo();
        String referenciaAtual = pagamento.getArmazenamentoRef();

        pagamento.limparComprovante();
        armazenamentoFactory.obterPorTipo(tipoAtual).remover(referenciaAtual);
    }

    private Administrador buscarAdministrador(Integer adminId) {
        if (adminId == null) {
            return null;
        }

        return usuarioRepository.findById(adminId)
            .filter(usuario -> usuario instanceof Administrador)
            .map(usuario -> (Administrador) usuario)
            .orElse(null);
    }

    private ArquivoComprovante validarEExtrair(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo do comprovante é obrigatório.");
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("O comprovante excede o tamanho máximo de 1 MB.");
        }

        String tipoArquivo = arquivo.getContentType();

        if (tipoArquivo == null || !TIPOS_ACEITOS.contains(tipoArquivo.toLowerCase())) {
            throw new IllegalArgumentException("Formato inválido. Envie uma imagem (WebP, JPEG ou PNG) ou um PDF.");
        }

        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o arquivo enviado.", e);
        }

        if (!assinaturaConfere(conteudo, tipoArquivo.toLowerCase())) {
            throw new IllegalArgumentException("O conteúdo do arquivo não corresponde ao formato informado.");
        }

        return new ArquivoComprovante(conteudo, tipoArquivo.toLowerCase(), nomeSeguro(arquivo.getOriginalFilename()));
    }

    private boolean assinaturaConfere(byte[] conteudo, String tipoArquivo) {
        return switch (tipoArquivo) {
            // RIFF....WEBP
            case "image/webp" -> conteudo.length >= 12
                && comecaCom(conteudo, 0, 'R', 'I', 'F', 'F')
                && comecaCom(conteudo, 8, 'W', 'E', 'B', 'P');
            case "image/jpeg" -> conteudo.length >= 3
                && (conteudo[0] & 0xFF) == 0xFF && (conteudo[1] & 0xFF) == 0xD8 && (conteudo[2] & 0xFF) == 0xFF;
            case "image/png" -> conteudo.length >= 8
                && (conteudo[0] & 0xFF) == 0x89 && comecaCom(conteudo, 1, 'P', 'N', 'G');
            case "application/pdf" -> conteudo.length >= 4
                && comecaCom(conteudo, 0, '%', 'P', 'D', 'F');
            default -> false;
        };
    }

    private boolean comecaCom(byte[] conteudo, int posicao, char... esperados) {
        for (int i = 0; i < esperados.length; i++) {
            if (conteudo[posicao + i] != (byte) esperados[i]) {
                return false;
            }
        }
        return true;
    }

    private String nomeSeguro(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "comprovante";
        }

        String nome = nomeOriginal.replaceAll(".*[/\\\\]", "").replaceAll("[\\r\\n\"]", "");

        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }
}
