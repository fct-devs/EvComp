package br.unesp.fct.evcomp.dto;

import br.unesp.fct.evcomp.domain.Inscrição;
import br.unesp.fct.evcomp.domain.Pagamento;
import br.unesp.fct.evcomp.domain.Participante;

public class PagamentoPendenteResponseDTO extends PagamentoResponseDTO {

    private Integer participanteId;
    private String nomeParticipante;
    private String emailParticipante;

    public PagamentoPendenteResponseDTO() {}

    public static PagamentoPendenteResponseDTO fromEntityAdmin(Pagamento pagamento) {
        if (pagamento == null) return null;

        PagamentoResponseDTO base = PagamentoResponseDTO.fromEntity(pagamento);

        PagamentoPendenteResponseDTO dto = new PagamentoPendenteResponseDTO();
        dto.setId(base.getId());
        dto.setInscricaoId(base.getInscricaoId());
        dto.setEventoId(base.getEventoId());
        dto.setTituloEvento(base.getTituloEvento());
        dto.setStatus(base.getStatus());
        dto.setTemComprovante(base.isTemComprovante());
        dto.setNomeArquivoOriginal(base.getNomeArquivoOriginal());
        dto.setTipoArquivo(base.getTipoArquivo());
        dto.setTamanhoArquivo(base.getTamanhoArquivo());
        dto.setDataEnvio(base.getDataEnvio());
        dto.setDataAvaliacao(base.getDataAvaliacao());
        dto.setMotivoRecusa(base.getMotivoRecusa());
        dto.setChavePix(base.getChavePix());
        dto.setValorInscricao(base.getValorInscricao());
        dto.setModalidadeNome(base.getModalidadeNome());
        dto.setUrlComprovante(base.getUrlComprovante());

        Inscrição inscricao = pagamento.getInscricao();
        if (inscricao != null && inscricao.getParticipante() != null) {
            Participante participante = inscricao.getParticipante();
            dto.setParticipanteId(participante.getId());
            dto.setNomeParticipante(participante.getNomeCompleto());
            dto.setEmailParticipante(participante.getEmail());
        }

        return dto;
    }

    public Integer getParticipanteId() { return participanteId; }
    public void setParticipanteId(Integer participanteId) { this.participanteId = participanteId; }

    public String getNomeParticipante() { return nomeParticipante; }
    public void setNomeParticipante(String nomeParticipante) { this.nomeParticipante = nomeParticipante; }

    public String getEmailParticipante() { return emailParticipante; }
    public void setEmailParticipante(String emailParticipante) { this.emailParticipante = emailParticipante; }
}
