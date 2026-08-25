package br.unesp.fct.evcomp.domain;

import jakarta.persistence.*;
import java.util.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idEvento")
    private Integer id;

    @Column(nullable = false, unique = true)
    private String titulo;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_termino", nullable = false)
    private LocalDate dataFim;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    private String link;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contabilizacao", nullable = false)
    private TipoContabilizacao tipoContabilizacao;

    @Column(name = "chave_pix")
    private String chavePix;

    @Column(name = "valor_inscricao", precision = 10, scale = 2)
    private java.math.BigDecimal valorInscricao;

    @Column(name = "data_inicio_inscricao", nullable = false)
    private LocalDate dataInicioInscricao;

    @Column(name = "data_fim_inscricao", nullable = false)
    private LocalDate dataFimInscricao;

    @Transient
    private Atividade[] atividade;

    @Transient
    private Participante[] participante;

    @Transient
    private Relatorio[] relatorio;

    public Evento() {
    }



    public java.util.Map<String, Object> pegarDadosEvento() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
    
        map.put("id", this.id);
        map.put("titulo", this.titulo);
        map.put("dataInicio", this.dataInicio);
        map.put("dataFim", this.dataFim);
        map.put("descricao", this.descricao);
        map.put("link", this.link);
        map.put("tipoContabilizacao", this.tipoContabilizacao);
        map.put("chavePix", this.chavePix);
        map.put("valorInscricao", this.valorInscricao);
        map.put("dataInicioInscricao", this.dataInicioInscricao);
        map.put("dataFimInscricao", this.dataFimInscricao);

        return map;
    }

    public boolean ehPago() {
        return this.valorInscricao != null
            && this.valorInscricao.compareTo(java.math.BigDecimal.ZERO) > 0;
    }

    public static Evento criarEvento(String titulo, LocalDate dataInicio, LocalDate dataTermino, String descricao, String link, TipoContabilizacao tipo, LocalDate dataInicioInscricao, LocalDate dataFimInscricao) {
        return criarEvento(titulo, dataInicio, dataTermino, descricao, link, tipo, dataInicioInscricao, dataFimInscricao, null, null);
    }

    public static Evento criarEvento(String titulo, LocalDate dataInicio, LocalDate dataTermino, String descricao, String link, TipoContabilizacao tipo, LocalDate dataInicioInscricao, LocalDate dataFimInscricao, String chavePix, java.math.BigDecimal valorInscricao) {
        Evento evento = new Evento();

        evento.setTitulo(titulo);
        evento.setDataInicio(dataInicio);
        evento.setDataFim(dataTermino);
        evento.setDescricao(descricao);
        evento.setLink(link);
        evento.setTipoContabilizacao(tipo);
        evento.setChavePix(chavePix);
        evento.setValorInscricao(valorInscricao);
        evento.setDataInicioInscricao(dataInicioInscricao);
        evento.setDataFimInscricao(dataFimInscricao);

        return evento;
    }

    public void removerAtividade(Atividade atividade) {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public TipoContabilizacao getTipoContabilizacao() {
        return tipoContabilizacao;
    }

    public void setTipoContabilizacao(TipoContabilizacao tipoContabilizacao) {
        this.tipoContabilizacao = tipoContabilizacao;
    }

    public String getChavePix() {
        return chavePix;
    }

    public void setChavePix(String chavePix) {
        this.chavePix = chavePix;
    }

    public java.math.BigDecimal getValorInscricao() {
        return valorInscricao;
    }

    public void setValorInscricao(java.math.BigDecimal valorInscricao) {
        this.valorInscricao = valorInscricao;
    }

    public LocalDate getDataInicioInscricao() {
        return dataInicioInscricao;
    }

    public void setDataInicioInscricao(LocalDate dataInicioInscricao) {
        this.dataInicioInscricao = dataInicioInscricao;
    }

    public LocalDate getDataFimInscricao() {
        return dataFimInscricao;
    }

    public void setDataFimInscricao(LocalDate dataFimInscricao) {
        this.dataFimInscricao = dataFimInscricao;
    }

}
