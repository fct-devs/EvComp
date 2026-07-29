package br.unesp.fct.evcomp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "comprovante_blob")
public class ComprovanteBlob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idComprovanteBlob")
    private Integer id;

    @Lob
    @Column(name = "conteudo", nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] conteudo;

    public ComprovanteBlob() {
    }

    public ComprovanteBlob(byte[] conteudo) {
        this.conteudo = conteudo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public void setConteudo(byte[] conteudo) {
        this.conteudo = conteudo;
    }
}
