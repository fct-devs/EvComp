package br.unesp.fct.evcomp.controller;

import br.unesp.fct.evcomp.domain.Evento;
import br.unesp.fct.evcomp.domain.Participante;
import br.unesp.fct.evcomp.domain.TipoContabilizacao;
import br.unesp.fct.evcomp.dto.EventoRequestDTO;
import br.unesp.fct.evcomp.dto.ParticipanteResponseDTO;
import br.unesp.fct.evcomp.repository.EventoRepository;
import br.unesp.fct.evcomp.repository.ParticipanteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/eventos")

public class EventoController {

    private final EventoRepository eventoRepository;
    private final ParticipanteRepository participanteRepository;
    private final br.unesp.fct.evcomp.repository.InscricaoRepository inscricaoRepository;
    
    @Autowired
    private br.unesp.fct.evcomp.repository.AtividadeRepository atividadeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public EventoController(EventoRepository eventoRepository, ParticipanteRepository participanteRepository, br.unesp.fct.evcomp.repository.InscricaoRepository inscricaoRepository) {
        this.eventoRepository = eventoRepository;
        this.participanteRepository = participanteRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    @GetMapping
    public ResponseEntity<List<Evento>> solicitarEventos() {
        return ResponseEntity.ok(eventoRepository.buscarTodosEventos());
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<Evento>> listarEventosDisponiveis() {
        return ResponseEntity.ok(eventoRepository.buscarEventosDisponiveis());
    }

    @GetMapping("/disponiveis/{participanteId}")
    public ResponseEntity<List<Evento>> solicitarEventosDisponiveis(@PathVariable Integer participanteId) {
        return ResponseEntity.ok(eventoRepository.buscarEventosDisponiveisPorParticipante(participanteId));
    }

    @GetMapping("/{id}/participantes")
    public ResponseEntity<List<ParticipanteResponseDTO>> listarParticipantesDoEvento(@PathVariable Integer id) {
        List<Participante> participantes = inscricaoRepository.buscarParticipantesPorEvento(id);
        List<ParticipanteResponseDTO> dtos = participantes.stream()
            .map(ParticipanteResponseDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/coletor")
    public ResponseEntity<?> listarEventosDoColetor(jakarta.servlet.http.HttpServletRequest request) {
        Integer usuarioLogadoId = (Integer) request.getAttribute("usuarioLogadoId");

        if (usuarioLogadoId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuário não autenticado."));
        }

        Optional<Participante> partOpt = participanteRepository.buscarParticipantePorId(usuarioLogadoId);
        if (partOpt.isPresent() && partOpt.get() instanceof br.unesp.fct.evcomp.domain.ColetorDePresenca) {
            br.unesp.fct.evcomp.domain.ColetorDePresenca coletor = (br.unesp.fct.evcomp.domain.ColetorDePresenca) partOpt.get();
            List<Evento> eventosColetados = coletor.getEventosColetados();
            
            // Filtra eventos que estão ocorrendo no momento
            LocalDate dataAtual = LocalDate.now();
            List<Evento> eventosAtivos = eventosColetados.stream()
                    .filter(e -> (e.getDataInicio() == null || !dataAtual.isBefore(e.getDataInicio())) && 
                                 (e.getDataFim() == null || !dataAtual.isAfter(e.getDataFim())))
                    .toList();
                    
            return ResponseEntity.ok(eventosAtivos);
        } else {
            return ResponseEntity.status(403).body(Map.of("error", "Usuário não é um Coletor"));
        }
    }

    @PostMapping("/{eventoId}/coletores/{participanteId}")
    public ResponseEntity<?> tornarColetor(@PathVariable Integer eventoId, @PathVariable("participanteId") Integer participanteId) {
        try {
            boolean ehColetor = participanteRepository.verificarColetor(participanteId, eventoId);
            
            if (ehColetor) {
                 return ResponseEntity.status(400).body(Map.of("error", "Participante já é coletor deste evento."));
            }
            
            participanteRepository.atribuirPapelColetor(participanteId, eventoId);
            entityManager.clear();
            
            return ResponseEntity.ok(Map.of("message", "Coletor associado com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro interno no servidor ao associar o coletor."));
        }
    }

    @DeleteMapping("/{eventoId}/coletores/{coletorId}")
    public ResponseEntity<?> removerColetor(@PathVariable Integer eventoId, @PathVariable Integer coletorId) {
        try {
            boolean coletorRemovido = participanteRepository.deletarColetor(eventoId, coletorId);
            entityManager.clear();
            
            if (coletorRemovido) {
                return ResponseEntity.ok(Map.of("message", "Coletor removido com sucesso."));
            } else {
                return ResponseEntity.status(400).body(Map.of("error", "Participante não era coletor deste evento ou a exclusão falhou."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro interno ao remover o coletor."));
        }
    }

    @GetMapping("/{eventoId}/detalhes")
    public ResponseEntity<?> selecionarEvento(@PathVariable Integer eventoId) {
        Optional<Evento> eventoEncontrado = eventoRepository.buscarEventoPorId(eventoId);

        if (!eventoEncontrado.isPresent()) {
            return ResponseEntity.status(404).body(Map.of("error", "Evento não encontrado."));
        }

        Evento evento = eventoEncontrado.get();

        Map<String, Object> dadosEvento = evento.pegarDadosEvento();

        List<br.unesp.fct.evcomp.domain.Atividade> atividades = atividadeRepository.buscarAtividadesPorEvento(eventoId);
        List<br.unesp.fct.evcomp.dto.AtividadeResponseDTO> atividadesDto = atividades.stream()
            .map(br.unesp.fct.evcomp.dto.AtividadeResponseDTO::fromEntity)
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "dadosEvento", dadosEvento,
            "atividades", atividadesDto
        ));
    }




    @PostMapping
    public ResponseEntity<?> confirmarCriacao(@Valid @RequestBody EventoRequestDTO req) {
        try {
            String titulo = req.getTitulo();
            String descricao = req.getDescricao();
            String link = req.getLink();
            String tipo = req.getTipoContabilizacao();

            LocalDate dataInicio = LocalDate.parse(req.getDataInicio());
            LocalDate dataTermino = LocalDate.parse(req.getDataTermino());
            LocalDate dataInicioInscricao = LocalDate.parse(req.getDataInicioInscricao());
            LocalDate dataFimInscricao = LocalDate.parse(req.getDataFimInscricao());

            if (eventoRepository.verificarEventoCadastrado(titulo)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Já existe um evento cadastrado com este título."));
            }

            TipoContabilizacao tipoC = TipoContabilizacao.valueOf(tipo);

            Evento evento = Evento.criarEvento(titulo, dataInicio, dataTermino, descricao, link, tipoC, dataInicioInscricao, dataFimInscricao);

            boolean eventoCriado = eventoRepository.salvarNovoEvento(evento);
            
            if (eventoCriado) {
                return ResponseEntity.ok(Map.of("message", "Evento criado com sucesso."));
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "Não foi possível criar o evento."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro interno no servidor ao cadastrar o evento."));
        }
    }

    private boolean verificarAlteracaoTitulo(String tituloEventoEncontrado, String titulo) {
        return titulo != null && !titulo.equals(tituloEventoEncontrado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> confirmarEdicao(@PathVariable Integer id, @Valid @RequestBody EventoRequestDTO req) {
        try {
            String titulo = req.getTitulo();
            String descricao = req.getDescricao();
            String link = req.getLink();
            String tipo = req.getTipoContabilizacao();
            LocalDate dataInicio = LocalDate.parse(req.getDataInicio());
            LocalDate dataTermino = LocalDate.parse(req.getDataTermino());
            LocalDate dataInicioInscricao = LocalDate.parse(req.getDataInicioInscricao());
            LocalDate dataFimInscricao = LocalDate.parse(req.getDataFimInscricao());

            Optional<Evento> eventoEncontrado = eventoRepository.buscarEventoPorId(id);
            Evento evento = eventoEncontrado.get();
            
            if (verificarAlteracaoTitulo(evento.getTitulo(), titulo)) {
                if (eventoRepository.verificarEventoCadastrado(titulo)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Já existe um evento cadastrado com este título."));
                }
            }
            
            evento.setTitulo(titulo);
            evento.setDataInicio(dataInicio);
            evento.setDataFim(dataTermino);
            evento.setDescricao(descricao);
            evento.setLink(link);
            evento.setTipoContabilizacao(TipoContabilizacao.valueOf(tipo));
            evento.setDataInicioInscricao(dataInicioInscricao);
            evento.setDataFimInscricao(dataFimInscricao);

            boolean editado = eventoRepository.salvarEvento(evento);
            
            if (editado) {
                return ResponseEntity.ok(Map.of("message", "Evento editado com sucesso."));
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "Erro ao tentar salvar o evento no banco."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ocorreu um erro interno no servidor ao editar o evento."));
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<?> confirmarConsulta(@RequestParam String tituloEvento) {
        java.util.List<Evento> listaEventos = eventoRepository.buscarEventosPorTituloParcial(tituloEvento);

        if(listaEventos.isEmpty()){
            return ResponseEntity.status(404).body(Map.of("error", "Nenhum evento encontrado com este título."));
        }

        return ResponseEntity.ok(listaEventos);
    }
}
