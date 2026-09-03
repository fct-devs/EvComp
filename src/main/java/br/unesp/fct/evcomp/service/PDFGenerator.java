package br.unesp.fct.evcomp.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PDFGenerator {

    public byte[] gerarPDF(String htmlContent) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            String baseUri;
            java.io.File diskTemplates = new java.io.File("/app/templates");
            if (diskTemplates.exists() && diskTemplates.isDirectory()) {
                baseUri = diskTemplates.toURI().toURL().toExternalForm();
            } else {
                baseUri = getClass().getResource("/templates/").toExternalForm();
            }
            builder.withHtmlContent(htmlContent, baseUri);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF a partir do HTML", e);
        }
    }
}
