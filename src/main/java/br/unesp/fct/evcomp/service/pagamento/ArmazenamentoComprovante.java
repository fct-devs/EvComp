package br.unesp.fct.evcomp.service.pagamento;

import br.unesp.fct.evcomp.domain.TipoArmazenamento;

/**
 * Estratégia de armazenamento do arquivo de comprovante.
 *
 * O Pagamento guarda apenas o par (tipo, referência); o significado da referência
 * é privado de cada implementação — id do blob no banco, caminho relativo em disco
 * ou chave de objeto no bucket. É isso que permite trocar de estratégia sem migrar
 * os comprovantes já existentes.
 */
public interface ArmazenamentoComprovante {

    /**
     * Identifica esta estratégia. Gravado em Pagamento.armazenamentoTipo para que a
     * leitura futura seja roteada de volta para a implementação correta.
     */
    TipoArmazenamento getTipo();

    /**
     * Persiste o arquivo e devolve a referência opaca usada para recuperá-lo depois.
     */
    String salvar(ArquivoComprovante arquivo);

    /**
     * Recupera o conteúdo do arquivo a partir da referência devolvida por {@link #salvar}.
     * Devolve só os bytes: nome original e content-type vivem no próprio Pagamento,
     * que é a fonte única desses metadados.
     *
     * @throws IllegalArgumentException se a referência não corresponder a nenhum arquivo
     */
    byte[] recuperar(String referencia);

    /**
     * Remove o arquivo. Usado no reenvio de comprovante, para não deixar órfãos.
     * Não deve lançar exceção se o arquivo já não existir.
     */
    void remover(String referencia);
}
