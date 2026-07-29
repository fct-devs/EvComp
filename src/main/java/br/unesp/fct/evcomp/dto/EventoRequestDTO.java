package br.unesp.fct.evcomp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EventoRequestDTO {

    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.")
    private String titulo;

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(max = 5000, message = "A descrição deve ter no máximo 5000 caracteres.")
    private String descricao;

    @Size(max = 500, message = "O link deve ter no máximo 500 caracteres.")
    private String link;

    @NotNull(message = "O tipo de contabilização é obrigatório.")
    private String tipoContabilizacao;

    @NotNull(message = "A data de início é obrigatória.")
    private String dataInicio;

    @NotNull(message = "A data de término é obrigatória.")
    private String dataTermino;

    @Size(max = 255, message = "A chave PIX deve ter no máximo 255 caracteres.")
    private String chavePix;

    @DecimalMin(value = "0.0", message = "O valor da inscrição não pode ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "O valor da inscrição deve ter no máximo 8 dígitos inteiros e 2 decimais.")
    private java.math.BigDecimal valorInscricao;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getTipoContabilizacao() { return tipoContabilizacao; }
    public void setTipoContabilizacao(String tipoContabilizacao) { this.tipoContabilizacao = tipoContabilizacao; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataTermino() { return dataTermino; }
    public void setDataTermino(String dataTermino) { this.dataTermino = dataTermino; }

    public String getChavePix() { return chavePix; }
    public void setChavePix(String chavePix) { this.chavePix = chavePix; }

    public java.math.BigDecimal getValorInscricao() { return valorInscricao; }
    public void setValorInscricao(java.math.BigDecimal valorInscricao) { this.valorInscricao = valorInscricao; }
}
