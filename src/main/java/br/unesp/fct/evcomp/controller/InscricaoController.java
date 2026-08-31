package br.unesp.fct.evcomp.controller;

import br.unesp.fct.evcomp.domain.Atividade;
import br.unesp.fct.evcomp.domain.Evento;
import br.unesp.fct.evcomp.domain.Inscrição;

import br.unesp.fct.evcomp.domain.Participante;
import br.unesp.fct.evcomp.dto.AtividadesUpdateRequestDTO;
import br.unesp.fct.evcomp.dto.InscricaoResponseDTO;
import br.unesp.fct.evcomp.repository.AtividadeRepository;
import br.unesp.fct.evcomp.repository.EventoRepository;
import br.unesp.fct.evcomp.repository.InscricaoRepository;
import br.unesp.fct.evcomp.repository.ParticipanteRepository;
import br.unesp.fct.evcomp.service.pagamento.PagamentoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/inscricoes")

public class InscricaoController {

    private final InscricaoRepository inscricaoRepository;
    private final ParticipanteRepository participanteRepository;
    private final EventoRepository eventoRepository;
    private final AtividadeRepository atividadeRepository;
    private final PagamentoService pagamentoService;
    private final br.unesp.fct.evcomp.repository.ModalidadeInscricaoRepository modalidadeRepository;

    @Autowired
    public InscricaoController(InscricaoRepository inscricaoRepository, ParticipanteRepository participanteRepository, EventoRepository eventoRepository, AtividadeRepository atividadeRepository, PagamentoService pagamentoService, br.unesp.fct.evcomp.repository.ModalidadeInscricaoRepository modalidadeRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.participanteRepository = participanteRepository;
        this.eventoRepository = eventoRepository;
        this.atividadeRepository = atividadeRepository;
        this.pagamentoService = pagamentoService;
        this.modalidadeRepository = modalidadeRepository;
    }

    @PostMapping
    public ResponseEntity<?> inscreverParticipanteWeb(@Valid @RequestBody br.unesp.fct.evcomp.dto.InscricaoRequestDTO req, jakarta.servlet.http.HttpServletRequest request) {
        try {
            Integer usuarioLogadoId = (Integer) request.getAttribute("usuarioLogadoId");
            String usuarioLogadoRole = (String) request.getAttribute("usuarioLogadoRole");
            
            Integer participanteId = req.getParticipanteId();

            if (!"ADMIN".equals(usuarioLogadoRole) && !participanteId.equals(usuarioLogadoId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Você só pode realizar inscrições para a sua própria conta."));
            }

            Integer eventoId = req.getEventoId();
            List<Integer> atividades = req.getAtividadeIds();

            return inscreverParticipante(participanteId, eventoId, atividades, req.getModalidadeId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erro interno ao processar a inscrição. Tente novamente."));
        }
    }

    public ResponseEntity<?> inscreverParticipante(Integer participanteId, Integer eventoId, List<Integer> atividades, Integer modalidadeId) {
        if (participanteId == null || eventoId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Participante e Evento são obrigatórios."));
        }

        Optional<Participante> participante = participanteRepository.buscarParticipantePorId(participanteId);
        Optional<Evento> evento = eventoRepository.buscarEventoPorId(eventoId);

        if (!participante.isPresent() || !evento.isPresent()) {
            return ResponseEntity.status(404).body(Map.of("error", "Participante ou Evento não encontrado."));
        }

        Evento eventoObj = evento.get();
        LocalDate hoje = LocalDate.now();
        if (hoje.isBefore(eventoObj.getDataInicioInscricao()) || hoje.isAfter(eventoObj.getDataFimInscricao())) {
            return ResponseEntity.badRequest().body(Map.of("error", "O período de inscrições para este evento não está aberto."));
        }

        // Resgata as atividades do evento e filtra apenas as selecionadas
        List<Atividade> todasAtividades = atividadeRepository.buscarAtividadesPorEvento(eventoId);
        List<Atividade> atividadesObjetos = todasAtividades.stream()
            .filter(a -> atividades.contains(a.getId()))
            .toList();

        if (atividadesObjetos.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nenhuma atividade válida selecionada."));
        }

        // Busca a Inscrição Atual
        Optional<br.unesp.fct.evcomp.domain.Inscrição> inscricaoExistente = inscricaoRepository.buscarPorParticipanteEEvento(participanteId, eventoId);

        List<br.unesp.fct.evcomp.domain.ModalidadeInscricao> modalidadesAtivas = modalidadeRepository.buscarAtivasPorEvento(eventoId);
        if (modalidadesAtivas.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Este evento não possui modalidades de inscrição disponíveis."));
        }

        br.unesp.fct.evcomp.domain.ModalidadeInscricao modalidadeEscolhida;
        if (modalidadeId != null) {
            modalidadeEscolhida = modalidadesAtivas.stream()
                .filter(m -> m.getId().equals(modalidadeId))
                .findFirst()
                .orElse(null);
            if (modalidadeEscolhida == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Modalidade de inscrição inválida ou indisponível para este evento."));
            }
        } else if (modalidadesAtivas.size() == 1) {
            modalidadeEscolhida = modalidadesAtivas.get(0);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Selecione uma modalidade de inscrição."));
        }

        java.math.BigDecimal valorAplicado = modalidadeEscolhida.getValor();

        // Evento pago -> inscrição nasce bloqueada (status=false) até o pagamento ser aprovado;
        // evento gratuito -> nasce ativa (status=true), como antes.
        boolean statusInicial = valorAplicado.compareTo(java.math.BigDecimal.ZERO) <= 0;

        br.unesp.fct.evcomp.domain.Inscrição inscricao;
        if (inscricaoExistente.isPresent()) {
            inscricao = inscricaoExistente.get();
            if (inscricao.isStatus()) { // Já inscrito e ativo
                return ResponseEntity.badRequest().body(Map.of("error", "Participante já inscrito neste evento."));
            }
            // Reativa a inscrição cancelada
            inscricao.setStatus(statusInicial);
            inscricao.setAtividade(atividadesObjetos);
            inscricao.setDataInscricao(LocalDateTime.now());
        } else {
            // Nova Inscrição
            inscricao = new br.unesp.fct.evcomp.domain.Inscrição(
                LocalDateTime.now(), statusInicial, participante.get(), evento.get(), atividadesObjetos
            );
        }

        inscricao.setModalidade(modalidadeEscolhida);
        inscricao.setValorAplicado(valorAplicado);

        inscricaoRepository.salvarInscricao(inscricao);

        // Cria o pagamento já na inscrição (vazio, sem comprovante) para que o participante
        // possa enviar o comprovante depois, em outro momento, sem refazer a inscrição.
        pagamentoService.obterOuCriar(inscricao);

        return ResponseEntity.ok(br.unesp.fct.evcomp.dto.InscricaoResponseDTO.fromEntity(inscricao));
    }

    @PutMapping("/{inscricaoId}")
    public ResponseEntity<?> atualizarAtividadesInscricao(
            @PathVariable Integer inscricaoId,
            @Valid @RequestBody AtividadesUpdateRequestDTO req,
            HttpServletRequest request) {
        try {
            Optional<Inscrição> inscricaoOpt = inscricaoRepository.buscarPorIdComDetalhes(inscricaoId);

            if (!inscricaoOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("error", "Inscrição não encontrada."));
            }

            Inscrição inscricao = inscricaoOpt.get();

            Integer usuarioLogadoId = (Integer) request.getAttribute("usuarioLogadoId");
            String usuarioLogadoRole = (String) request.getAttribute("usuarioLogadoRole");
            Integer donoId = inscricao.getParticipante().getId();

            if (!"ADMIN".equals(usuarioLogadoRole) && !donoId.equals(usuarioLogadoId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Você só pode alterar as suas próprias inscrições."));
            }

            Integer eventoId = inscricao.getEvento().getId();
            Evento evento = inscricao.getEvento();
            LocalDate hoje = LocalDate.now();

            if (evento.getDataInicioInscricao() != null && evento.getDataFimInscricao() != null) {
                if (hoje.isBefore(evento.getDataInicioInscricao()) || hoje.isAfter(evento.getDataFimInscricao())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "O período de inscrições para este evento está encerrado."));
                }
            }

            if (req.getModalidadeId() != null) {
                Integer modalidadeAtualId = inscricao.getModalidade() != null ? inscricao.getModalidade().getId() : null;
                if (!req.getModalidadeId().equals(modalidadeAtualId)) {
                    try {
                        br.unesp.fct.evcomp.domain.Pagamento pagamento = pagamentoService.buscarPorInscricao(inscricaoId);
                        if (pagamento != null) {
                            if (pagamento.getStatus() == br.unesp.fct.evcomp.domain.StatusPagamento.APROVADO) {
                                return ResponseEntity.badRequest().body(Map.of("error", "Não é permitido alterar a modalidade de uma inscrição com pagamento já aprovado."));
                            }
                            if (pagamento.getStatus() == br.unesp.fct.evcomp.domain.StatusPagamento.PENDENTE && pagamento.temComprovante()) {
                                return ResponseEntity.badRequest().body(Map.of("error", "Não é permitido alterar a modalidade enquanto houver um comprovante sob análise. Aguarde a avaliação da organização."));
                            }
                        }
                    } catch (Exception ignored) {}

                    Optional<br.unesp.fct.evcomp.domain.ModalidadeInscricao> novaModalidadeOpt = modalidadeRepository.buscarPorIdEEvento(req.getModalidadeId(), eventoId);
                    if (!novaModalidadeOpt.isPresent()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Modalidade de inscrição não encontrada para este evento."));
                    }

                    br.unesp.fct.evcomp.domain.ModalidadeInscricao novaModalidade = novaModalidadeOpt.get();
                    if (!novaModalidade.isAtivo()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Esta modalidade de inscrição não está ativa."));
                    }

                    inscricao.setModalidade(novaModalidade);
                    inscricao.setValorAplicado(novaModalidade.getValor());

                    boolean statusInicial = novaModalidade.getValor() == null || novaModalidade.getValor().compareTo(java.math.BigDecimal.ZERO) <= 0;
                    inscricao.setStatus(statusInicial);

                    pagamentoService.obterOuCriar(inscricao);
                }
            }

            List<Atividade> todasAtividades = atividadeRepository.buscarAtividadesPorEvento(eventoId);
            List<Atividade> atividadesObjetos = new ArrayList<>(todasAtividades.stream()
                .filter(a -> req.getAtividadeIds().contains(a.getId()))
                .toList());

            if (atividadesObjetos.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nenhuma atividade válida selecionada."));
            }

            inscricao.setAtividade(atividadesObjetos);
            inscricaoRepository.salvarInscricao(inscricao);

            return ResponseEntity.ok(InscricaoResponseDTO.fromEntity(inscricao));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erro interno ao atualizar as atividades da inscrição."));
        }
    }

    @GetMapping("/minhas")
    public ResponseEntity<?> listarEventosInscritos(@RequestParam("participanteId") String pId, jakarta.servlet.http.HttpServletRequest request) {
        try {
            Integer participanteId = Integer.valueOf(pId);
            Integer usuarioLogadoId = (Integer) request.getAttribute("usuarioLogadoId");
            String usuarioLogadoRole = (String) request.getAttribute("usuarioLogadoRole");

            if (!"ADMIN".equals(usuarioLogadoRole) && !participanteId.equals(usuarioLogadoId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Você só pode visualizar suas próprias inscrições."));
            }

            List<Integer> eventosIds = inscricaoRepository.buscarEventosInscritosPorParticipante(participanteId);

            return ResponseEntity.ok(Map.of("inscritos", eventosIds));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ocorreu um erro ao listar os eventos inscritos."));
        }
    }

    @GetMapping("/detalhes")
    public ResponseEntity<?> listarInscricoesDetalhadas(@RequestParam("participanteId") String pId, jakarta.servlet.http.HttpServletRequest request) {
        try {
            Integer participanteId = Integer.valueOf(pId);
            Integer usuarioLogadoId = (Integer) request.getAttribute("usuarioLogadoId");
            String usuarioLogadoRole = (String) request.getAttribute("usuarioLogadoRole");

            if (!"ADMIN".equals(usuarioLogadoRole) && !participanteId.equals(usuarioLogadoId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Você só pode visualizar os detalhes das suas próprias inscrições."));
            }

            List<br.unesp.fct.evcomp.domain.Inscrição> inscricoes = inscricaoRepository.buscarInscricoesPorParticipante(participanteId);
            List<br.unesp.fct.evcomp.dto.InscricaoResponseDTO> dtos = inscricoes.stream()
                .map(br.unesp.fct.evcomp.dto.InscricaoResponseDTO::fromEntity)
                .toList();
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ocorreu um erro ao carregar os detalhes de inscrições."));
        }
    }
}
