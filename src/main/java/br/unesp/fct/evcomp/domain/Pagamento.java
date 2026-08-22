package br.unesp.fct.evcomp.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"idInscrição"})
})
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPagamento")
    private Integer id;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idInscrição", nullable = false, unique = true)
    private Inscrição inscricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private StatusPagamento status;

    @Enumerated(EnumType.STRING)
    @Column(name = "armazenamento_tipo", length = 20)
    private TipoArmazenamento armazenamentoTipo;

    @Column(name = "armazenamento_ref", length = 255)
    private String armazenamentoRef;

    @Column(name = "nome_arquivo_original", length = 255)
    private String nomeArquivoOriginal;

    @Column(name = "tipo_arquivo", length = 100)
    private String tipoArquivo;

    @Column(name = "tamanho_arquivo")
    private Integer tamanhoArquivo;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(name = "data_avaliacao")
    private LocalDateTime dataAvaliacao;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuário_avaliador")
    private Administrador avaliador;

    @Column(name = "motivo_recusa", length = 255)
    private String motivoRecusa;

    public Pagamento() {
    }

    public Pagamento(Inscrição inscricao, StatusPagamento status) {
        this.inscricao = inscricao;
        this.status = status;
    }

    public boolean temComprovante() {
        return this.armazenamentoRef != null && !this.armazenamentoRef.isBlank();
    }

    public void limparComprovante() {
        this.armazenamentoTipo = null;
        this.armazenamentoRef = null;
        this.nomeArquivoOriginal = null;
        this.tipoArquivo = null;
        this.tamanhoArquivo = null;
        this.dataEnvio = null;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Inscrição getInscricao() {
        return inscricao;
    }

    public void setInscricao(Inscrição inscricao) {
        this.inscricao = inscricao;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public void setStatus(StatusPagamento status) {
        this.status = status;
    }

    public TipoArmazenamento getArmazenamentoTipo() {
        return armazenamentoTipo;
    }

    public void setArmazenamentoTipo(TipoArmazenamento armazenamentoTipo) {
        this.armazenamentoTipo = armazenamentoTipo;
    }

    public String getArmazenamentoRef() {
        return armazenamentoRef;
    }

    public void setArmazenamentoRef(String armazenamentoRef) {
        this.armazenamentoRef = armazenamentoRef;
    }

    public String getNomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }

    public void setNomeArquivoOriginal(String nomeArquivoOriginal) {
        this.nomeArquivoOriginal = nomeArquivoOriginal;
    }

    public String getTipoArquivo() {
        return tipoArquivo;
    }

    public void setTipoArquivo(String tipoArquivo) {
        this.tipoArquivo = tipoArquivo;
    }

    public Integer getTamanhoArquivo() {
        return tamanhoArquivo;
    }

    public void setTamanhoArquivo(Integer tamanhoArquivo) {
        this.tamanhoArquivo = tamanhoArquivo;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public LocalDateTime getDataAvaliacao() {
        return dataAvaliacao;
    }

    public void setDataAvaliacao(LocalDateTime dataAvaliacao) {
        this.dataAvaliacao = dataAvaliacao;
    }

    public Administrador getAvaliador() {
        return avaliador;
    }

    public void setAvaliador(Administrador avaliador) {
        this.avaliador = avaliador;
    }

    public String getMotivoRecusa() {
        return motivoRecusa;
    }

    public void setMotivoRecusa(String motivoRecusa) {
        this.motivoRecusa = motivoRecusa;
    }
}
