package br.unesp.fct.evcomp.service.pagamento;

import br.unesp.fct.evcomp.domain.TipoArmazenamento;
import org.springframework.stereotype.Component;

/**
 * Ponto de extensão para armazenamento em bucket (AWS S3, Cloudflare R2, MinIO).
 *
 * NÃO IMPLEMENTADO de propósito. Existe para marcar o caminho de evolução quando a
 * aplicação for para a nuvem e guardar os comprovantes no banco deixar de fazer sentido
 * — e para provar que a abstração de armazenamento comporta um destino remoto sem
 * mudar nada em PagamentoService nem em PagamentoController.
 *
 * Para implementar:
 *   1. adicionar o SDK ao build.gradle (software.amazon.awssdk:s3);
 *   2. injetar bucket, região e credenciais via @Value / variáveis de ambiente;
 *   3. salvar    -> PutObject com chave "comprovantes/{uuid}{extensão}", devolvendo a chave;
 *   4. recuperar -> GetObject pela chave (ou devolver URL pré-assinada de curta duração,
 *                 o que exigiria mudar o contrato de leitura do controller);
 *   5. remover  -> DeleteObject.
 *
 * Enquanto isso, falha alto: configurar pagamento.armazenamento=S3 deve quebrar de
 * imediato e de forma legível, nunca aceitar um upload e perdê-lo em silêncio.
 */
@Component
public class ArmazenamentoS3Strategy implements ArmazenamentoComprovante {

    private static final String NAO_IMPLEMENTADO =
        "Armazenamento de comprovantes em S3 ainda não foi implementado. "
        + "Use pagamento.armazenamento=BANCO ou DISCO.";

    @Override
    public TipoArmazenamento getTipo() {
        return TipoArmazenamento.S3;
    }

    @Override
    public String salvar(ArquivoComprovante arquivo) {
        throw new UnsupportedOperationException(NAO_IMPLEMENTADO);
    }

    @Override
    public byte[] recuperar(String referencia) {
        throw new UnsupportedOperationException(NAO_IMPLEMENTADO);
    }

    @Override
    public void remover(String referencia) {
        throw new UnsupportedOperationException(NAO_IMPLEMENTADO);
    }
}
