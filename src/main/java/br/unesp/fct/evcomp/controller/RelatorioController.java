package br.unesp.fct.evcomp.controller;

import br.unesp.fct.evcomp.domain.Evento;
import br.unesp.fct.evcomp.domain.Relatorio;
import br.unesp.fct.evcomp.repository.EventoRepository;

import br.unesp.fct.evcomp.service.relatorio.RelatorioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/relatorios")

public class RelatorioController {

    private final RelatorioService relatorioService;
    private final EventoRepository eventoRepository;

    @Autowired
    public RelatorioController(RelatorioService relatorioService, EventoRepository eventoRepository) {
        this.relatorioService = relatorioService;
        this.eventoRepository = eventoRepository;
    }

    @GetMapping("/eventos")
    public ResponseEntity<?> solicitarEventosEncerrados() {
        List<Evento> eventos = eventoRepository.findAll().stream()
            .filter(ev -> !eventoRepository.checarAndamentoEvento(ev.getId()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/tipos")
    public ResponseEntity<?> listarTiposRelatorio() {
        return ResponseEntity.ok(relatorioService.obterTiposRelatoriosDisponiveis());
    }

    @PostMapping("/emitir")
    public ResponseEntity<?> emitirRelatorio(@RequestBody Map<String, Object> payload) {
        Map<String, Object> dadosEventoMap = (Map<String, Object>) payload.get("dadosEvento");
        Integer eventoId = (Integer) dadosEventoMap.get("id");
        String tituloEvento = String.valueOf(dadosEventoMap.get("titulo"));
        String tipo = String.valueOf(payload.get("tipo"));

        br.unesp.fct.evcomp.domain.TipoRelatorio tipoRelatorio = br.unesp.fct.evcomp.domain.TipoRelatorio.valueOf(tipo.toUpperCase());

        Relatorio rel = relatorioService.gerarRelatorio(eventoId, tituloEvento, tipoRelatorio);
        
        String filename = "Relatorio.pdf";
        if ("PARTICIPANTES".equals(tipoRelatorio.name())) {
            filename = "Relatorio Participantes - " + tituloEvento + ".pdf";
        } else if ("GRAFICO".equals(tipoRelatorio.name())) {
            filename = "Relatorio Grafico - " + tituloEvento + ".pdf";
        } else if ("PARTICIPANTES_APROVADOS".equals(tipoRelatorio.name())) {
            filename = "Relatorio Participantes Aprovados - " + tituloEvento + ".pdf";
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(rel.getPdfConteudo());
    }


}
