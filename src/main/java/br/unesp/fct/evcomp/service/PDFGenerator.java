package br.unesp.fct.evcomp.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;

@Service
public class PDFGenerator {

    public byte[] gerarPDF(String htmlContent) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            
            File externalDir = new File("/app/templates");
            String baseUri;
            if (externalDir.exists() && externalDir.isDirectory()) {
                baseUri = externalDir.toURI().toString();
            } else if (getClass().getResource("/templates/") != null) {
                baseUri = getClass().getResource("/templates/").toExternalForm();
            } else {
                baseUri = new File(".").toURI().toString();
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
