package br.unesp.fct.evcomp.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class AtividadesUpdateRequestDTO {

    @NotNull(message = "A lista de atividades é obrigatória.")
    private List<Integer> atividadeIds;

    public List<Integer> getAtividadeIds() { return atividadeIds; }
    public void setAtividadeIds(List<Integer> atividadeIds) { this.atividadeIds = atividadeIds; }
}
