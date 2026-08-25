package br.unesp.fct.evcomp.service.pagamento;

public record ArquivoComprovante(byte[] conteudo, String tipoArquivo, String nomeOriginal) {
}
