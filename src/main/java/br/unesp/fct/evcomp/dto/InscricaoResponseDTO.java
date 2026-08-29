package br.unesp.fct.evcomp.dto;

import br.unesp.fct.evcomp.domain.Inscrição;
import br.unesp.fct.evcomp.domain.Evento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class InscricaoResponseDTO {
    private Integer id;
    private LocalDateTime dataInscricao;
    private boolean status;
    private Evento evento;
    private ParticipanteAutenticadoDTO participante;
    private List<AtividadeResponseDTO> atividade;
    private ModalidadeInscricaoResponseDTO modalidade;
    private BigDecimal valorAplicado;

    public InscricaoResponseDTO() {}

    public static InscricaoResponseDTO fromEntity(Inscrição inscricao) {
        if (inscricao == null) return null;
        
        InscricaoResponseDTO dto = new InscricaoResponseDTO();
        dto.setId(inscricao.getId());
        dto.setDataInscricao(inscricao.getDataInscricao());
        dto.setStatus(inscricao.isStatus());
        
        if (inscricao.getEvento() != null) {
            dto.setEvento(inscricao.getEvento());
        }
        
        if (inscricao.getParticipante() != null) {
            dto.setParticipante(ParticipanteAutenticadoDTO.fromEntity(inscricao.getParticipante()));
        }
        
        if (inscricao.getAtividade() != null) {
            dto.setAtividade(inscricao.getAtividade().stream()
                .map(AtividadeResponseDTO::fromEntity)
                .collect(Collectors.toList()));
        }

        dto.setValorAplicado(inscricao.getValorAplicado());
        if (inscricao.getModalidade() != null) {
            dto.setModalidade(ModalidadeInscricaoResponseDTO.fromEntity(inscricao.getModalidade()));
        }

        return dto;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDateTime getDataInscricao() { return dataInscricao; }
    public void setDataInscricao(LocalDateTime dataInscricao) { this.dataInscricao = dataInscricao; }

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public ParticipanteAutenticadoDTO getParticipante() { return participante; }
    public void setParticipante(ParticipanteAutenticadoDTO participante) { this.participante = participante; }

    public List<AtividadeResponseDTO> getAtividade() { return atividade; }
    public void setAtividade(List<AtividadeResponseDTO> atividade) { this.atividade = atividade; }

    public ModalidadeInscricaoResponseDTO getModalidade() { return modalidade; }
    public void setModalidade(ModalidadeInscricaoResponseDTO modalidade) { this.modalidade = modalidade; }

    public BigDecimal getValorAplicado() { return valorAplicado; }
    public void setValorAplicado(BigDecimal valorAplicado) { this.valorAplicado = valorAplicado; }
}
