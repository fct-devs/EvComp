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

    @Autowired
    public InscricaoController(InscricaoRepository inscricaoRepository, ParticipanteRepository participanteRepository, EventoRepository eventoRepository, AtividadeRepository atividadeRepository, PagamentoService pagamentoService) {
        this.inscricaoRepository = inscricaoRepository;
        this.participanteRepository = participanteRepository;
        this.eventoRepository = eventoRepository;
        this.atividadeRepository = atividadeRepository;
        this.pagamentoService = pagamentoService;
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

            return inscreverParticipante(participanteId, eventoId, atividades);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erro interno ao processar a inscrição. Tente novamente."));
        }
    }

    public ResponseEntity<?> inscreverParticipante(Integer participanteId, Integer eventoId, List<Integer> atividades) {
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

        br.unesp.fct.evcomp.domain.Inscrição inscricao;
        if (inscricaoExistente.isPresent()) {
            inscricao = inscricaoExistente.get();
            if (inscricao.isStatus()) { // Já inscrito e ativo
                return ResponseEntity.badRequest().body(Map.of("error", "Participante já inscrito neste evento."));
            }
            // Reativa a inscrição cancelada
            inscricao.setStatus(true);
            inscricao.setAtividade(atividadesObjetos);
            inscricao.setDataInscricao(LocalDateTime.now());
        } else {
            // Nova Inscrição
            inscricao = new br.unesp.fct.evcomp.domain.Inscrição(
                LocalDateTime.now(), true, participante.get(), evento.get(), atividadesObjetos
            );
        }

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

            List<br.unesp.fct.evcomp.domain.Inscrição> inscricoes = inscricaoRepository.buscarInscricoesAtivasPorParticipante(participanteId);
            List<br.unesp.fct.evcomp.dto.InscricaoResponseDTO> dtos = inscricoes.stream()
                .map(br.unesp.fct.evcomp.dto.InscricaoResponseDTO::fromEntity)
                .toList();
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ocorreu um erro ao carregar os detalhes de inscrições."));
        }
    }
}
