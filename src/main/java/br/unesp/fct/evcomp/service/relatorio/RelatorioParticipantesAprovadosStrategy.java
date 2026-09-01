package br.unesp.fct.evcomp.service.relatorio;

import br.unesp.fct.evcomp.domain.Inscrição;
import br.unesp.fct.evcomp.domain.Pagamento;
import br.unesp.fct.evcomp.domain.Participante;
import br.unesp.fct.evcomp.domain.Relatorio;
import br.unesp.fct.evcomp.domain.StatusPagamento;
import br.unesp.fct.evcomp.repository.PagamentoRepository;
import br.unesp.fct.evcomp.service.PDFGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class RelatorioParticipantesAprovadosStrategy extends RelatorioStrategyFactory {

    private final PagamentoRepository pagamentoRepository;
    private final PDFGenerator pdfGenerator;

    @Autowired
    public RelatorioParticipantesAprovadosStrategy(PagamentoRepository pagamentoRepository, PDFGenerator pdfGenerator) {
        this.pagamentoRepository = pagamentoRepository;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    public Object processarDados(Integer eventoId) {
        List<Pagamento> pagamentos = pagamentoRepository.buscarPagamentosPorEvento(eventoId);
        List<Map<String, String>> participantesAprovados = new ArrayList<>();

        for (Pagamento pag : pagamentos) {
            if (pag.getStatus() == StatusPagamento.APROVADO || pag.getStatus() == StatusPagamento.ISENTO) {
                Inscrição insc = pag.getInscricao();
                if (insc != null && insc.getParticipante() != null) {
                    Participante part = insc.getParticipante();
                    String nome = part.getNomeCompleto() != null ? part.getNomeCompleto() : "";
                    String email = part.getEmail() != null ? part.getEmail() : "";
                    String modalidade = (insc.getModalidade() != null && insc.getModalidade().getNome() != null)
                        ? insc.getModalidade().getNome()
                        : "Geral";

                    participantesAprovados.add(Map.of(
                        "nome", nome,
                        "email", email,
                        "modalidade", modalidade
                    ));
                }
            }
        }

        // Ordenação alfabética por nome
        participantesAprovados.sort(Comparator.comparing(m -> m.get("nome"), String.CASE_INSENSITIVE_ORDER));
        return participantesAprovados;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Relatorio gerarPDF(Object dadosBrutos, String tituloEvento) {
        List<Map<String, String>> participantes = (List<Map<String, String>>) dadosBrutos;

        try {
            InputStream in = getClass().getResourceAsStream("/templates/relatorio_participantes_aprovados.html");
            if (in == null) {
                throw new RuntimeException("Template HTML não encontrado: relatorio_participantes_aprovados.html");
            }
            byte[] bdata = FileCopyUtils.copyToByteArray(in);
            String html = new String(bdata, StandardCharsets.UTF_8);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String dataGeracaoStr = LocalDateTime.now().format(formatter);

            html = html.replace("$nomeEvento", tituloEvento != null ? tituloEvento : "");
            html = html.replace("$dataGeracao", dataGeracaoStr);
            html = html.replace("$totalParticipantes", String.valueOf(participantes.size()));

            StringBuilder linhas = new StringBuilder();
            if (participantes.isEmpty()) {
                linhas.append("<tr><td colspan=\"3\" style=\"text-align: center; color: #64748b; padding: 20px;\">Nenhum participante com pagamento aprovado para este evento.</td></tr>");
            } else {
                for (Map<String, String> p : participantes) {
                    linhas.append("<tr>");
                    linhas.append("<td>").append(p.get("nome")).append("</td>");
                    linhas.append("<td>").append(p.get("email")).append("</td>");
                    linhas.append("<td><span class=\"badge-modalidade\">").append(p.get("modalidade")).append("</span></td>");
                    linhas.append("</tr>");
                }
            }

            html = html.replace("$linhasTabela", linhas.toString());

            byte[] pdfBytes = pdfGenerator.gerarPDF(html);
            return new Relatorio(new Date(), "PARTICIPANTES_APROVADOS", pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar PDF de relatório de participantes aprovados", e);
        }
    }
}
