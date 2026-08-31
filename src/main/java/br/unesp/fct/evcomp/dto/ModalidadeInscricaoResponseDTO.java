package br.unesp.fct.evcomp.dto;

import br.unesp.fct.evcomp.domain.ModalidadeInscricao;
import java.math.BigDecimal;

public class ModalidadeInscricaoResponseDTO {
    private Integer id;
    private Integer eventoId;
    private String nome;
    private String descricao;
    private BigDecimal valor;
    private boolean ativo;

    public static ModalidadeInscricaoResponseDTO fromEntity(ModalidadeInscricao m) {
        if (m == null) return null;
        ModalidadeInscricaoResponseDTO dto = new ModalidadeInscricaoResponseDTO();
        dto.setId(m.getId());
        dto.setEventoId(m.getEvento() != null ? m.getEvento().getId() : null);
        dto.setNome(m.getNome());
        dto.setDescricao(m.getDescricao());
        dto.setValor(m.getValor());
        dto.setAtivo(m.isAtivo());
        return dto;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEventoId() { return eventoId; }
    public void setEventoId(Integer eventoId) { this.eventoId = eventoId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
