package br.unesp.fct.evcomp.service.relatorio;


import br.unesp.fct.evcomp.domain.Participante;
import br.unesp.fct.evcomp.domain.Relatorio;
import br.unesp.fct.evcomp.repository.InscricaoRepository;
import br.unesp.fct.evcomp.service.PDFGenerator;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

@Component
public class GraficoComparativoStrategy extends RelatorioStrategyFactory {

    private final InscricaoRepository inscricaoRepository;
    private final PDFGenerator pdfGenerator;

    @Autowired
    public GraficoComparativoStrategy(InscricaoRepository inscricaoRepository, PDFGenerator pdfGenerator) {
        this.inscricaoRepository = inscricaoRepository;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    public Object processarDados(Integer eventoId) {
        List<Participante> participantes = inscricaoRepository.buscarParticipantesPorEvento(eventoId);
        
        long internos = 0;
        long externos = 0;
        
        for (Participante p : participantes) {
            String email = p.getEmail().toLowerCase();
            if (email.endsWith("@unesp.br") || email.endsWith(".unesp.br") || email.contains("unesp.br")) {
                internos++;
            } else {
                externos++;
            }
        }
        
        Map<String, Long> resultado = new HashMap<>();
        resultado.put("internos", internos);
        resultado.put("externos", externos);
        return resultado;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Relatorio gerarPDF(Object dadosBrutos, String tituloEvento) {
        Map<String, Long> counts = (Map<String, Long>) dadosBrutos;
        long internos = counts.getOrDefault("internos", 0L);
        long externos = counts.getOrDefault("externos", 0L);
        long total = internos + externos;
        
        double pctInternos = total > 0 ? ((double) internos / total) * 100 : 0.0;
        double pctExternos = total > 0 ? ((double) externos / total) * 100 : 0.0;

        try {
            InputStream in = getClass().getResourceAsStream("/templates/relatorio_grafico.html");
            if (in == null) {
                throw new RuntimeException("Template HTML não encontrado: relatorio_grafico.html");
            }
            byte[] bdata = FileCopyUtils.copyToByteArray(in);
            String html = new String(bdata, StandardCharsets.UTF_8);

            html = html.replace("$nomeEvento", tituloEvento);
            html = html.replace("$dataGeracao", LocalDateTime.now().toString());

            html = html.replace("$qtdInternos", String.valueOf(internos));
            html = html.replace("$pctInternos", String.format(Locale.US, "%.2f%%", pctInternos));
            
            html = html.replace("$qtdExternos", String.valueOf(externos));
            html = html.replace("$pctExternos", String.format(Locale.US, "%.2f%%", pctExternos));
            
            html = html.replace("$qtdTotal", String.valueOf(total));

            int totalBlocks = 100;
            int internoBlocks = total > 0 ? (int) Math.round((double) internos / total * totalBlocks) : 0;
            int externoBlocks = total > 0 ? (totalBlocks - internoBlocks) : 0;

            html = html.replace("$wInterno", internoBlocks + "%");
            html = html.replace("$wExterno", externoBlocks + "%");

            byte[] pdfBytes = pdfGenerator.gerarPDF(html);
            return new Relatorio(new java.util.Date(), "COMPARATIVO_INTERNOS_EXTERNOS", pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar PDF do relatório comparativo", e);
        }
    }
}
