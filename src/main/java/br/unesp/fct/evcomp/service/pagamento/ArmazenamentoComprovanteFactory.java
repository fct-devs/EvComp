package br.unesp.fct.evcomp.service.pagamento;

import br.unesp.fct.evcomp.domain.TipoArmazenamento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


@Component
public class ArmazenamentoComprovanteFactory {

    private final Map<TipoArmazenamento, ArmazenamentoComprovante> estrategias = new EnumMap<>(TipoArmazenamento.class);
    private final TipoArmazenamento tipoPadrao;

    @Autowired
    public ArmazenamentoComprovanteFactory(List<ArmazenamentoComprovante> implementacoes,
                                           @Value("${pagamento.armazenamento:BANCO}") String armazenamentoPadrao) {
        for (ArmazenamentoComprovante implementacao : implementacoes) {
            estrategias.put(implementacao.getTipo(), implementacao);
        }

        try {
            this.tipoPadrao = TipoArmazenamento.valueOf(armazenamentoPadrao.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Valor inválido para pagamento.armazenamento: '" + armazenamentoPadrao
                + "'. Valores aceitos: BANCO, DISCO, S3.", e);
        }
    }

    public ArmazenamentoComprovante obterPadrao() {
        return obterPorTipo(tipoPadrao);
    }

    public TipoArmazenamento getTipoPadrao() {
        return tipoPadrao;
    }

    public ArmazenamentoComprovante obterPorTipo(TipoArmazenamento tipo) {
        ArmazenamentoComprovante estrategia = estrategias.get(tipo);

        if (estrategia == null) {
            throw new IllegalStateException("Nenhuma estratégia de armazenamento registrada para o tipo " + tipo + ".");
        }

        return estrategia;
    }
}
