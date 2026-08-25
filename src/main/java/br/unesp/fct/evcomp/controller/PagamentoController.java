package br.unesp.fct.evcomp.controller;

import br.unesp.fct.evcomp.domain.Pagamento;
import br.unesp.fct.evcomp.dto.AvaliacaoPagamentoRequestDTO;
import br.unesp.fct.evcomp.dto.PagamentoPendenteResponseDTO;
import br.unesp.fct.evcomp.dto.PagamentoResponseDTO;
import br.unesp.fct.evcomp.service.pagamento.PagamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @Autowired
    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @GetMapping("/minhas")
    public ResponseEntity<?> listarMeusPagamentos(@RequestParam("participanteId") String pId,
                                                  jakarta.servlet.http.HttpServletRequest request) {
        try {
            Integer participanteId = Integer.valueOf(pId);

            if (!ehAdmin(request) && !participanteId.equals(usuarioLogadoId(request))) {
                return ResponseEntity.status(403).body(Map.of("error", "Você só pode visualizar os seus próprios pagamentos."));
            }

            List<PagamentoResponseDTO> dtos = pagamentoService.listarPorParticipante(participanteId).stream()
                .map(PagamentoResponseDTO::fromEntity)
                .toList();

            return ResponseEntity.ok(dtos);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Identificador de participante inválido."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro ao listar os pagamentos."));
        }
    }

    @GetMapping("/minha/{inscricaoId}")
    public ResponseEntity<?> consultarPagamentoDaInscricao(@PathVariable Integer inscricaoId,
                                                            jakarta.servlet.http.HttpServletRequest request) {
        try {
            Pagamento pagamento = pagamentoService.buscarPorInscricao(inscricaoId);

            ResponseEntity<?> negado = validarAcessoAoPagamento(pagamento, request);
            if (negado != null) return negado;

            return ResponseEntity.ok(PagamentoResponseDTO.fromEntity(pagamento));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro ao consultar o pagamento."));
        }
    }

    @PostMapping("/{inscricaoId}/upload")
    public ResponseEntity<?> enviarComprovante(@PathVariable Integer inscricaoId,
                                                @RequestParam("arquivo") MultipartFile arquivo,
                                                jakarta.servlet.http.HttpServletRequest request) {
        try {
            Pagamento pagamento = pagamentoService.buscarPorInscricao(inscricaoId);

            ResponseEntity<?> negado = validarAcessoAoPagamento(pagamento, request);
            if (negado != null) return negado;

            Pagamento atualizado = pagamentoService.registrarComprovante(inscricaoId, arquivo);

            return ResponseEntity.ok(PagamentoResponseDTO.fromEntity(atualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro ao enviar o comprovante."));
        }
    }

    @GetMapping("/{id}/comprovante")
    public ResponseEntity<?> visualizarComprovante(@PathVariable Integer id,
                                                   jakarta.servlet.http.HttpServletRequest request) {
        try {
            Pagamento pagamento = pagamentoService.buscarPorId(id);

            ResponseEntity<?> negado = validarAcessoAoPagamento(pagamento, request);
            if (negado != null) return negado;

            byte[] conteudo = pagamentoService.obterConteudoComprovante(pagamento);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(pagamento.getTipoArquivo()));
            headers.setContentDisposition(ContentDisposition.inline()
                .filename(pagamento.getNomeArquivoOriginal() == null ? "comprovante" : pagamento.getNomeArquivoOriginal())
                .build());

            return ResponseEntity.ok().headers(headers).body(conteudo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro ao carregar o comprovante."));
        }
    }

    @GetMapping("/pendentes")
    public ResponseEntity<?> listarPendentes() {
        try {
            List<PagamentoPendenteResponseDTO> dtos = pagamentoService.listarPendentes().stream()
                .map(PagamentoPendenteResponseDTO::fromEntityAdmin)
                .toList();

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro ao listar os pagamentos pendentes."));
        }
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<?> listarPorEvento(@PathVariable Integer eventoId) {
        try {
            List<PagamentoPendenteResponseDTO> dtos = pagamentoService.listarPorEvento(eventoId).stream()
                .map(PagamentoPendenteResponseDTO::fromEntityAdmin)
                .toList();

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro ao listar os pagamentos do evento."));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> avaliarPagamento(@PathVariable Integer id,
                                               @Valid @RequestBody AvaliacaoPagamentoRequestDTO req,
                                               jakarta.servlet.http.HttpServletRequest request) {
        try {
            Pagamento avaliado = pagamentoService.avaliar(id, req.getNovoStatus(), req.getMotivoRecusa(), usuarioLogadoId(request));

            return ResponseEntity.ok(PagamentoPendenteResponseDTO.fromEntityAdmin(avaliado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro ao avaliar o pagamento."));
        }
    }

    /**
     * Um pagamento só pode ser visto pelo dono da inscrição ou por um administrador.
     * Devolve null quando o acesso é permitido.
     */
    private ResponseEntity<?> validarAcessoAoPagamento(Pagamento pagamento, jakarta.servlet.http.HttpServletRequest request) {
        if (ehAdmin(request)) {
            return null;
        }

        Integer donoId = pagamento.getInscricao() != null && pagamento.getInscricao().getParticipante() != null
            ? pagamento.getInscricao().getParticipante().getId()
            : null;

        if (donoId != null && donoId.equals(usuarioLogadoId(request))) {
            return null;
        }

        return ResponseEntity.status(403).body(Map.of("error", "Você só pode acessar o pagamento da sua própria inscrição."));
    }

    private Integer usuarioLogadoId(jakarta.servlet.http.HttpServletRequest request) {
        return (Integer) request.getAttribute("usuarioLogadoId");
    }

    private boolean ehAdmin(jakarta.servlet.http.HttpServletRequest request) {
        return "ADMIN".equals(request.getAttribute("usuarioLogadoRole"));
    }
}
