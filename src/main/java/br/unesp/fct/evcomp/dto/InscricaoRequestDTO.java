package br.unesp.fct.evcomp.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class InscricaoRequestDTO {

    @NotNull(message = "O ID do participante é obrigatório.")
    private Integer participanteId;

    @NotNull(message = "O ID do evento é obrigatório.")
    private Integer eventoId;

    @NotNull(message = "As atividades são obrigatórias.")
    private List<Integer> atividadeIds;

    private Integer modalidadeId;

    public Integer getParticipanteId() { return participanteId; }
    public void setParticipanteId(Integer participanteId) { this.participanteId = participanteId; }

    public Integer getEventoId() { return eventoId; }
    public void setEventoId(Integer eventoId) { this.eventoId = eventoId; }

    public List<Integer> getAtividadeIds() { return atividadeIds; }
    public void setAtividadeIds(List<Integer> atividadeIds) { this.atividadeIds = atividadeIds; }

    public Integer getModalidadeId() { return modalidadeId; }
    public void setModalidadeId(Integer modalidadeId) { this.modalidadeId = modalidadeId; }
}
