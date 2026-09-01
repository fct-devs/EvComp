package br.unesp.fct.evcomp.controller;

import br.unesp.fct.evcomp.domain.Atividade;
import br.unesp.fct.evcomp.domain.Evento;

import br.unesp.fct.evcomp.repository.AtividadeRepository;
import br.unesp.fct.evcomp.repository.EventoRepository;
import br.unesp.fct.evcomp.repository.InscricaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/atividades")

public class AtividadeController {

    private final AtividadeRepository atividadeRepository;
    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final br.unesp.fct.evcomp.service.AtividadeService atividadeService;
    private final br.unesp.fct.evcomp.repository.ParticipanteRepository participanteRepository;
    private final br.unesp.fct.evcomp.repository.PresencaRepository presencaRepository;

    @Autowired
    public AtividadeController(AtividadeRepository atividadeRepository, EventoRepository eventoRepository, InscricaoRepository inscricaoRepository, br.unesp.fct.evcomp.service.AtividadeService atividadeService, br.unesp.fct.evcomp.repository.ParticipanteRepository participanteRepository, br.unesp.fct.evcomp.repository.PresencaRepository presencaRepository) {
        this.atividadeRepository = atividadeRepository;
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.atividadeService = atividadeService;
        this.participanteRepository = participanteRepository;
        this.presencaRepository = presencaRepository;
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<br.unesp.fct.evcomp.dto.AtividadeResponseDTO>> listarAtividades() {
        List<br.unesp.fct.evcomp.dto.AtividadeResponseDTO> dtos = atividadeRepository.findAll().stream()
            .map(br.unesp.fct.evcomp.dto.AtividadeResponseDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/ativas-coletor")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> listarAtividadesDeEventosAtivos(jakarta.servlet.http.HttpServletRequest request) {
        Integer usuarioId = (Integer) request.getAttribute("usuarioLogadoId");
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Não autenticado."));
        }

        Optional<br.unesp.fct.evcomp.domain.Participante> partOpt = participanteRepository.buscarParticipantePorId(usuarioId);
        if (!partOpt.isPresent() || !(partOpt.get() instanceof br.unesp.fct.evcomp.domain.ColetorDePresenca)) {
            return ResponseEntity.status(403).body(Map.of("error", "Usuário não é um coletor."));
        }

        br.unesp.fct.evcomp.domain.ColetorDePresenca coletor = (br.unesp.fct.evcomp.domain.ColetorDePresenca) partOpt.get();
        java.util.List<br.unesp.fct.evcomp.domain.Evento> eventosColetados = coletor.getEventosColetados();

        List<Atividade> todas = atividadeRepository.findAll();
        List<Atividade> ativas = todas.stream()
            .filter(a -> a.getEvento() != null && 
                         eventoRepository.checarAndamentoEvento(a.getEvento().getId()) &&
                         eventosColetados.stream().anyMatch(e -> e.getId().equals(a.getEvento().getId())))
            .collect(java.util.stream.Collectors.toList());

        List<br.unesp.fct.evcomp.dto.AtividadeResponseDTO> dtos = ativas.stream()
            .map(br.unesp.fct.evcomp.dto.AtividadeResponseDTO::fromEntity)
            .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarAtividade(@PathVariable Integer id) {
        Optional<Atividade> atividadeOpt = atividadeRepository.buscarAtividadePorId(id);

        if (atividadeOpt.isPresent()) {
            Atividade atividade = atividadeOpt.get();

            return ResponseEntity.ok(atividade.pegarDadosAtividade());
        }

        return ResponseEntity.status(404).body(Map.of("error", "Atividade não encontrada"));
    }

    @GetMapping("/{id}/selecionar")
    public ResponseEntity<?> selecionarAtividade(@PathVariable Integer id) {
        Optional<Atividade> atividadeOpt = atividadeRepository.buscarAtividadePorId(id);
        
        if (atividadeOpt.isPresent()) {
            Atividade atividadeEncontrada = atividadeOpt.get();
            Object dadosAtividade = atividadeEncontrada.pegarDadosAtividade();
            boolean periodoValido = atividadeService.checarPeriodoPresenca(atividadeEncontrada);
            
            if (periodoValido) {
                return ResponseEntity.ok(dadosAtividade);
            } else {
                return ResponseEntity.status(403).body(Map.of("error", "Atividade fora do período válido de registro de presença"));
            }
        }
        
        return ResponseEntity.status(404).body(Map.of("error", "Atividade não encontrada"));
    }

    @PostMapping
    public ResponseEntity<?> confirmarCriacao(@RequestBody Map<String, Object> req) {
        try {
            Integer eventoId = (Integer) req.get("evento_id");
            
            Evento evento = eventoRepository.buscarEventoPorId(eventoId).get();

            String titulo = String.valueOf(req.get("titulo"));
            
            Atividade atividadeJaCadastrada = atividadeRepository.verificarAtividadeCadastrada(titulo, eventoId);
            if (atividadeJaCadastrada != null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Já existe uma atividade com este título neste evento."));
            }

            String descricao = String.valueOf(req.get("descricao"));
            String local = req.get("local") != null ? String.valueOf(req.get("local")) : null;
            String preRequisitos = String.valueOf(req.getOrDefault("pre_requisitos", ""));

            LocalDate data_inicio = LocalDate.parse(String.valueOf(req.get("data_inicio")));
            LocalDate data_termino = LocalDate.parse(String.valueOf(req.get("data_termino")));
            
            LocalTime horario_inicio = LocalTime.parse(String.valueOf(req.get("horario_inicio")));
            LocalTime horario_termino = LocalTime.parse(String.valueOf(req.get("horario_termino")));
            
            int max_participantes = req.get("max_participantes") != null ? Integer.parseInt(String.valueOf(req.get("max_participantes"))) : 0;
            int carga_horaria_ministrantes = req.get("carga_horaria_ministrantes") != null ? Integer.parseInt(String.valueOf(req.get("carga_horaria_ministrantes"))) : 0;
            int carga_horaria_total = req.get("carga_horaria_total") != null ? Integer.parseInt(String.valueOf(req.get("carga_horaria_total"))) : 0;

            List<br.unesp.fct.evcomp.domain.Participante> ministrantes = new java.util.ArrayList<>();
            if (req.get("ministrantes_ids") != null) {
                List<Integer> ids = (List<Integer>) req.get("ministrantes_ids");
                if (!ids.isEmpty()) {
                    ministrantes = participanteRepository.buscarParticipantesPorId(ids);
                }
            }

            Atividade novaAtividade = Atividade.criarAtividade(titulo, descricao, preRequisitos, data_inicio, horario_inicio, data_termino, horario_termino, max_participantes, carga_horaria_total, ministrantes, carga_horaria_ministrantes);
            novaAtividade.setLocal(local);
            novaAtividade.setEvento(evento);

            boolean salva = atividadeRepository.salvarAtividade(novaAtividade);
            
            if(salva) {
                return ResponseEntity.ok(novaAtividade);
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "Erro interno ao salvar atividade."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro interno no servidor."));
        }
    }

    @org.springframework.transaction.annotation.Transactional
    @PutMapping("/{id}")
    public ResponseEntity<?> edicaoAtividade(@PathVariable Integer id, @RequestBody Map<String, Object> req) {
        try {
            Optional<Atividade> atOpt = atividadeRepository.buscarAtividadePorId(id);

            if (!atOpt.isPresent()) return ResponseEntity.status(404).body(Map.of("error", "Atividade não encontrada."));
            Atividade at = atOpt.get();
            
            String titulo = req.get("titulo") != null ? String.valueOf(req.get("titulo")) : null;
            if (titulo != null && !titulo.equals(at.getTitulo())) {
                Atividade atividadeJaCadastrada = atividadeRepository.verificarAtividadeCadastrada(titulo, at.getEvento().getId());
                
                if (atividadeJaCadastrada != null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Já existe uma atividade com este título neste evento."));
                }
            }
            
            if (req.get("max_participantes") != null) {
                atividadeService.verificarCapacidadeMinima(id, (Integer) req.get("max_participantes"));
            }
            
            if (req.get("ministrantes_ids") != null) {
                List<Integer> ids = (List<Integer>) req.get("ministrantes_ids");
                List<br.unesp.fct.evcomp.domain.Participante> novosMinistrantes = new java.util.ArrayList<>();
                if (!ids.isEmpty()) {
                    novosMinistrantes = participanteRepository.buscarParticipantesPorId(ids);
                }
                req.put("novos_ministrantes", novosMinistrantes);
            }
            
            boolean editada = at.alterarDadosAtividade(req);
            
            if (editada) {
                int desinscritos = atividadeService.resolverConflitosDeHorario(at);

                atividadeRepository.salvarAtividade(at);
                
                if (desinscritos > 0) {
                    return ResponseEntity.ok(Map.of("message", "Atividade editada com sucesso! ATENÇÃO: " + desinscritos + " participante(s) foram desinscritos automaticamente devido a conflito de horário."));
                }

                return ResponseEntity.ok(Map.of("message", "Atividade editada com sucesso!"));
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro interno no servidor ao editar a atividade."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro interno no servidor ao editar a atividade."));
        }
    }

    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> excluirAtividade(@PathVariable("id") Integer atividadeId, @RequestParam(required = false) boolean confirmar) {
        Optional<Atividade> atividade = atividadeRepository.buscarAtividadePorId(atividadeId);
        
        if (atividade.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Atividade não encontrada."));
        }
        
        int inscritos = inscricaoRepository.contarInscritosPorAtividadeInt(atividadeId);
        
        if (inscritos > 0) {
            if (!confirmar) {
                return ResponseEntity.status(409).body(Map.of("error", "Atividade com participantes inscritos. Confirmar exclusão?"));
            }
            
            presencaRepository.removerPresencasPorAtividade(atividadeId);
            inscricaoRepository.removerInscricoesPorAtividade(atividadeId);
        }
        
        atividadeRepository.removerAtividade(atividadeId);
        
        return ResponseEntity.ok(Map.of("message", "Atividade excluída com sucesso."));
    }

    @GetMapping("/{id}/vagas")
    public ResponseEntity<?> verificarVagas(@PathVariable("id") Integer atividadeId) {
        Integer vagas = atividadeRepository.consultarVagasDisponiveis(atividadeId);
        
        int vagasDisponiveis = vagas != null ? vagas : 0;

        return ResponseEntity.ok(Map.of("vagasDisponiveis", Math.max(0, vagasDisponiveis)));
    }

}
