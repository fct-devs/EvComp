package br.unesp.fct.evcomp.controller;

import br.unesp.fct.evcomp.domain.Evento;
import br.unesp.fct.evcomp.domain.ModalidadeInscricao;
import br.unesp.fct.evcomp.dto.ModalidadeInscricaoRequestDTO;
import br.unesp.fct.evcomp.dto.ModalidadeInscricaoResponseDTO;
import br.unesp.fct.evcomp.repository.EventoRepository;
import br.unesp.fct.evcomp.repository.InscricaoRepository;
import br.unesp.fct.evcomp.repository.ModalidadeInscricaoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ModalidadeInscricaoController {

    private final ModalidadeInscricaoRepository modalidadeRepository;
    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;

    @Autowired
    public ModalidadeInscricaoController(ModalidadeInscricaoRepository modalidadeRepository,
                                          EventoRepository eventoRepository,
                                          InscricaoRepository inscricaoRepository) {
        this.modalidadeRepository = modalidadeRepository;
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    @GetMapping("/modalidades")
    public ResponseEntity<List<ModalidadeInscricaoResponseDTO>> listarTodas() {
        return ResponseEntity.ok(modalidadeRepository.buscarTodas().stream()
            .map(ModalidadeInscricaoResponseDTO::fromEntity).toList());
    }

    @GetMapping("/eventos/{eventoId}/modalidades")
    public ResponseEntity<?> listarPorEvento(@PathVariable Integer eventoId) {
        if (eventoRepository.buscarEventoPorId(eventoId).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Evento não encontrado."));
        }
        return ResponseEntity.ok(modalidadeRepository.buscarPorEvento(eventoId).stream()
            .map(ModalidadeInscricaoResponseDTO::fromEntity).toList());
    }

    @PostMapping("/eventos/{eventoId}/modalidades")
    public ResponseEntity<?> criar(@PathVariable Integer eventoId, @Valid @RequestBody ModalidadeInscricaoRequestDTO req) {
        Optional<Evento> eventoOpt = eventoRepository.buscarEventoPorId(eventoId);
        if (eventoOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Evento não encontrado."));

        if (modalidadeRepository.buscarPorEventoENome(eventoId, req.getNome()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Já existe uma modalidade com este nome neste evento."));
        }

        ModalidadeInscricao nova = new ModalidadeInscricao(eventoOpt.get(), req.getNome(), req.getDescricao(),
            req.getValor(), req.getAtivo() == null || req.getAtivo());
        modalidadeRepository.salvarModalidade(nova);
        return ResponseEntity.ok(ModalidadeInscricaoResponseDTO.fromEntity(nova));
    }

    @PutMapping("/eventos/{eventoId}/modalidades/{modalidadeId}")
    public ResponseEntity<?> editar(@PathVariable Integer eventoId, @PathVariable Integer modalidadeId,
                                     @Valid @RequestBody ModalidadeInscricaoRequestDTO req) {
        Optional<ModalidadeInscricao> modOpt = modalidadeRepository.buscarPorIdEEvento(modalidadeId, eventoId);
        if (modOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Modalidade não encontrada."));

        ModalidadeInscricao mod = modOpt.get();
        if (!mod.getNome().equalsIgnoreCase(req.getNome())
            && modalidadeRepository.buscarPorEventoENome(eventoId, req.getNome()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Já existe uma modalidade com este nome neste evento."));
        }

        mod.setNome(req.getNome());
        mod.setDescricao(req.getDescricao());
        mod.setValor(req.getValor());
        if (req.getAtivo() != null) mod.setAtivo(req.getAtivo());
        modalidadeRepository.salvarModalidade(mod);
        return ResponseEntity.ok(ModalidadeInscricaoResponseDTO.fromEntity(mod));
    }

    @DeleteMapping("/eventos/{eventoId}/modalidades/{modalidadeId}")
    public ResponseEntity<?> excluir(@PathVariable Integer eventoId, @PathVariable Integer modalidadeId) {
        Optional<ModalidadeInscricao> modOpt = modalidadeRepository.buscarPorIdEEvento(modalidadeId, eventoId);
        if (modOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Modalidade não encontrada."));

        if (inscricaoRepository.contarInscricoesPorModalidade(modalidadeId) > 0) {
            return ResponseEntity.badRequest().body(Map.of("error",
                "Não é possível excluir uma modalidade com inscrições associadas. Desative-a em vez de excluir."));
        }

        modalidadeRepository.delete(modOpt.get());
        return ResponseEntity.ok(Map.of("message", "Modalidade excluída com sucesso."));
    }
}
