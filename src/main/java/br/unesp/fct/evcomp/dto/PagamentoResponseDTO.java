package br.unesp.fct.evcomp.dto;

import br.unesp.fct.evcomp.domain.Evento;
import br.unesp.fct.evcomp.domain.Inscrição;
import br.unesp.fct.evcomp.domain.Pagamento;
import br.unesp.fct.evcomp.domain.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PagamentoResponseDTO {

    private Integer id;
    private Integer inscricaoId;
    private Integer eventoId;
    private String tituloEvento;
    private StatusPagamento status;
    private boolean temComprovante;
    private String nomeArquivoOriginal;
    private String tipoArquivo;
    private Integer tamanhoArquivo;
    private LocalDateTime dataEnvio;
    private LocalDateTime dataAvaliacao;
    private String motivoRecusa;
    private String chavePix;
    private BigDecimal valorInscricao;
    private String urlComprovante;

    public PagamentoResponseDTO() {}

    public static PagamentoResponseDTO fromEntity(Pagamento pagamento) {
        if (pagamento == null) return null;

        PagamentoResponseDTO dto = new PagamentoResponseDTO();
        dto.setId(pagamento.getId());
        dto.setStatus(pagamento.getStatus());
        dto.setTemComprovante(pagamento.temComprovante());
        dto.setNomeArquivoOriginal(pagamento.getNomeArquivoOriginal());
        dto.setTipoArquivo(pagamento.getTipoArquivo());
        dto.setTamanhoArquivo(pagamento.getTamanhoArquivo());
        dto.setDataEnvio(pagamento.getDataEnvio());
        dto.setDataAvaliacao(pagamento.getDataAvaliacao());
        dto.setMotivoRecusa(pagamento.getMotivoRecusa());

        if (pagamento.temComprovante() && pagamento.getId() != null) {
            dto.setUrlComprovante("/api/pagamentos/" + pagamento.getId() + "/comprovante");
        }

        Inscrição inscricao = pagamento.getInscricao();
        if (inscricao != null) {
            dto.setInscricaoId(inscricao.getId());

            Evento evento = inscricao.getEvento();
            if (evento != null) {
                dto.setEventoId(evento.getId());
                dto.setTituloEvento(evento.getTitulo());
                dto.setChavePix(evento.getChavePix());
                dto.setValorInscricao(evento.getValorInscricao());
            }
        }

        return dto;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getInscricaoId() { return inscricaoId; }
    public void setInscricaoId(Integer inscricaoId) { this.inscricaoId = inscricaoId; }

    public Integer getEventoId() { return eventoId; }
    public void setEventoId(Integer eventoId) { this.eventoId = eventoId; }

    public String getTituloEvento() { return tituloEvento; }
    public void setTituloEvento(String tituloEvento) { this.tituloEvento = tituloEvento; }

    public StatusPagamento getStatus() { return status; }
    public void setStatus(StatusPagamento status) { this.status = status; }

    public boolean isTemComprovante() { return temComprovante; }
    public void setTemComprovante(boolean temComprovante) { this.temComprovante = temComprovante; }

    public String getNomeArquivoOriginal() { return nomeArquivoOriginal; }
    public void setNomeArquivoOriginal(String nomeArquivoOriginal) { this.nomeArquivoOriginal = nomeArquivoOriginal; }

    public String getTipoArquivo() { return tipoArquivo; }
    public void setTipoArquivo(String tipoArquivo) { this.tipoArquivo = tipoArquivo; }

    public Integer getTamanhoArquivo() { return tamanhoArquivo; }
    public void setTamanhoArquivo(Integer tamanhoArquivo) { this.tamanhoArquivo = tamanhoArquivo; }

    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }

    public LocalDateTime getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(LocalDateTime dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }

    public String getMotivoRecusa() { return motivoRecusa; }
    public void setMotivoRecusa(String motivoRecusa) { this.motivoRecusa = motivoRecusa; }

    public String getChavePix() { return chavePix; }
    public void setChavePix(String chavePix) { this.chavePix = chavePix; }

    public BigDecimal getValorInscricao() { return valorInscricao; }
    public void setValorInscricao(BigDecimal valorInscricao) { this.valorInscricao = valorInscricao; }

    public String getUrlComprovante() { return urlComprovante; }
    public void setUrlComprovante(String urlComprovante) { this.urlComprovante = urlComprovante; }
}
