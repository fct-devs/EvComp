package br.unesp.fct.evcomp.service.pagamento;

import br.unesp.fct.evcomp.domain.TipoArmazenamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Component
public class ArmazenamentoDiscoStrategy implements ArmazenamentoComprovante {

    private static final Map<String, String> EXTENSOES = Map.of(
        "image/webp", ".webp",
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "application/pdf", ".pdf"
    );

    private final Path diretorioBase;

    public ArmazenamentoDiscoStrategy(@Value("${pagamento.diretorio-uploads:uploads/comprovantes}") String diretorioUploads) {
        this.diretorioBase = Paths.get(diretorioUploads).toAbsolutePath().normalize();
    }

    @Override
    public TipoArmazenamento getTipo() {
        return TipoArmazenamento.DISCO;
    }

    @Override
    public String salvar(ArquivoComprovante arquivo) {
        String nomeArquivo = UUID.randomUUID() + EXTENSOES.getOrDefault(arquivo.tipoArquivo(), "");

        try {
            Files.createDirectories(diretorioBase);
            Files.write(diretorioBase.resolve(nomeArquivo), arquivo.conteudo());
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gravar o comprovante em disco.", e);
        }

        return nomeArquivo;
    }

    @Override
    public byte[] recuperar(String referencia) {
        Path caminho = resolverCaminho(referencia);

        if (!Files.exists(caminho)) {
            throw new IllegalArgumentException("Arquivo do comprovante não encontrado.");
        }

        try {
            return Files.readAllBytes(caminho);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o comprovante em disco.", e);
        }
    }

    @Override
    public void remover(String referencia) {
        try {
            Files.deleteIfExists(resolverCaminho(referencia));
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível remover o comprovante em disco.", e);
        }
    }

    private Path resolverCaminho(String referencia) {
        Path caminho = diretorioBase.resolve(referencia).normalize();

        if (!caminho.startsWith(diretorioBase)) {
            throw new IllegalArgumentException("Referência de comprovante inválida.");
        }

        return caminho;
    }
}
