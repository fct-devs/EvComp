package br.unesp.fct.evcomp.service.certificado;

import br.unesp.fct.evcomp.domain.Certificado;
import br.unesp.fct.evcomp.domain.Participante;
import br.unesp.fct.evcomp.domain.Evento;
import br.unesp.fct.evcomp.domain.Atividade;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

import org.springframework.util.FileCopyUtils;
import br.unesp.fct.evcomp.service.PDFGenerator;

public class CertificadoAtividadeBuilder implements CertificadoBuilder {

    private Certificado certificado;
    private Participante participante;
    private Evento evento;
    private Atividade atividade;
    private String tipo;

    private int cargaHoraria;
    private String papel;
    private String detalheExtra;

    private byte[] pdfBytes;

    @Override
    public void buildMetaDados(Participante participante, Evento evento, Atividade atividade, String tipo) {
        this.participante = participante;
        this.evento = evento;
        this.atividade = atividade;
        this.tipo = tipo;
    }

    @Override
    public void buildConteudo(int cargaHoraria, String papel, String detalheExtra) {
        this.cargaHoraria = cargaHoraria;
        this.papel = papel;
        this.detalheExtra = detalheExtra;
    }

    @Override
    public void gerarPDF(PDFGenerator pdfGenerator) {
        try {
            String templateName = "template.html";
            
            java.io.File diskTemplate = new java.io.File("/app/templates/" + templateName);
            InputStream in;
            if (diskTemplate.exists() && diskTemplate.isFile()) {
                in = new java.io.FileInputStream(diskTemplate);
            } else {
                in = getClass().getResourceAsStream("/templates/" + templateName);
            }
            if (in == null) {
                throw new RuntimeException("Template HTML não encontrado: " + templateName);
            }

            byte[] bdata = FileCopyUtils.copyToByteArray(in);
            String html = new String(bdata, StandardCharsets.UTF_8);

            html = html.replace("$nomeParticipante", participante.getNomeCompleto().toUpperCase());
            html = html.replace("$nomeEventoOuAtividade", atividade.getTitulo());
            html = html.replace("$eventoOUatividade", "da atividade");
            html = html.replace("$nomeEvento", evento.getTitulo());
            html = html.replace("$nomeAtividade", atividade.getTitulo());
            html = html.replace("$cargaHoraria", String.valueOf(cargaHoraria));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
            java.time.LocalDate dataFimEvento = (evento != null && evento.getDataFim() != null)
                ? evento.getDataFim()
                : (atividade != null && atividade.getDataFim() != null ? atividade.getDataFim() : java.time.LocalDate.now());
            
            String dataFimStr = atividade.getDataFim() != null ? atividade.getDataFim().format(formatter) : dataFimEvento.format(formatter);
            String dataEmissaoStr = dataFimEvento.plusDays(1).format(formatter);
            
            html = html.replace("$dataFim", dataFimStr);
            html = html.replace("$dataAtual", dataEmissaoStr);
            html = html.replace("$papel", papel != null ? papel : "Ouvinte");

            this.pdfBytes = pdfGenerator.gerarPDF(html);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar PDF do certificado de atividade com template DOCX", e);
        }
    }

    @Override
    public Certificado obterCertificado() {
        float percentual = 100.0f;
        this.certificado = new Certificado(
            new java.util.Date(),
            percentual,
            tipo,
            null,
            participante,
            atividade
        );
        return this.certificado;
    }

    @Override
    public byte[] getPdfBytes() {
        return this.pdfBytes;
    }
}
