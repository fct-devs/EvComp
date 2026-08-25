package br.unesp.fct.evcomp.dto;

import br.unesp.fct.evcomp.domain.StatusPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AvaliacaoPagamentoRequestDTO {

    @NotNull(message = "O novo status é obrigatório (APROVADO ou RECUSADO).")
    private StatusPagamento novoStatus;

    @Size(max = 255, message = "O motivo da recusa deve ter no máximo 255 caracteres.")
    private String motivoRecusa;

    public StatusPagamento getNovoStatus() { return novoStatus; }
    public void setNovoStatus(StatusPagamento novoStatus) { this.novoStatus = novoStatus; }

    public String getMotivoRecusa() { return motivoRecusa; }
    public void setMotivoRecusa(String motivoRecusa) { this.motivoRecusa = motivoRecusa; }
}
