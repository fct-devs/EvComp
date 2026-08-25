package br.unesp.fct.evcomp.service.pagamento;

import br.unesp.fct.evcomp.domain.ComprovanteBlob;
import br.unesp.fct.evcomp.domain.TipoArmazenamento;
import br.unesp.fct.evcomp.repository.ComprovanteBlobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ArmazenamentoBancoStrategy implements ArmazenamentoComprovante {

    private final ComprovanteBlobRepository comprovanteBlobRepository;

    @Autowired
    public ArmazenamentoBancoStrategy(ComprovanteBlobRepository comprovanteBlobRepository) {
        this.comprovanteBlobRepository = comprovanteBlobRepository;
    }

    @Override
    public TipoArmazenamento getTipo() {
        return TipoArmazenamento.BANCO;
    }

    @Override
    public String salvar(ArquivoComprovante arquivo) {
        ComprovanteBlob blob = comprovanteBlobRepository.salvarComprovante(new ComprovanteBlob(arquivo.conteudo()));
        return String.valueOf(blob.getId());
    }

    @Override
    public byte[] recuperar(String referencia) {
        Optional<ComprovanteBlob> blob = comprovanteBlobRepository.buscarComprovantePorId(converterReferencia(referencia));

        if (blob.isEmpty()) {
            throw new IllegalArgumentException("Arquivo do comprovante não encontrado.");
        }

        return blob.get().getConteudo();
    }

    @Override
    public void remover(String referencia) {
        Integer id = converterReferencia(referencia);

        if (comprovanteBlobRepository.buscarComprovantePorId(id).isPresent()) {
            comprovanteBlobRepository.removerComprovantePorId(id);
        }
    }

    private Integer converterReferencia(String referencia) {
        try {
            return Integer.valueOf(referencia);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Referência de comprovante inválida para armazenamento em banco.");
        }
    }
}
