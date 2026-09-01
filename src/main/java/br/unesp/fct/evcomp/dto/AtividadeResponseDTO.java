package br.unesp.fct.evcomp.dto;

import br.unesp.fct.evcomp.domain.Atividade;
import br.unesp.fct.evcomp.domain.Evento;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class AtividadeResponseDTO {
    private Integer id;
    private String titulo;
    private String local;
    private String descricao;
    private String preRequisitos;
    private LocalDate dataInicio;
    private LocalTime horarioInicio;
    private LocalDate dataFim;
    private LocalTime horarioFim;
    private int maxParticipantes;
    private int cargaHorariaTotal;
    private int cargaHorariaMinistrante;
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Evento evento;
    private List<ParticipanteResumoDTO> ministrantes;

    public AtividadeResponseDTO() {}

    public static AtividadeResponseDTO fromEntity(Atividade atividade) {
        if (atividade == null) return null;
        
        AtividadeResponseDTO dto = new AtividadeResponseDTO();
        dto.setId(atividade.getId());
        dto.setTitulo(atividade.getTitulo());
        dto.setLocal(atividade.getLocal());
        dto.setDescricao(atividade.getDescricao());
        dto.setPreRequisitos(atividade.getPreRequisitos());
        dto.setDataInicio(atividade.getDataInicio());
        dto.setHorarioInicio(atividade.getHorarioInicio());
        dto.setDataFim(atividade.getDataFim());
        dto.setHorarioFim(atividade.getHorarioFim());
        dto.setMaxParticipantes(atividade.getMaxParticipantes());
        dto.setCargaHorariaTotal(atividade.getCargaHorariaTotal());
        dto.setCargaHorariaMinistrante(atividade.getCargaHorariaMinistrante());
        dto.setEvento(atividade.getEvento());
        
        if (atividade.getMinistrantes() != null) {
            dto.setMinistrantes(atividade.getMinistrantes().stream()
                .map(ParticipanteResumoDTO::fromEntity)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }

    private void setDescricao(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }

    private void setPreRequisitos(String preRequisitos) { this.preRequisitos = preRequisitos; }
    public String getPreRequisitos() { return preRequisitos; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalTime getHorarioInicio() { return horarioInicio; }
    public void setHorarioInicio(LocalTime horarioInicio) { this.horarioInicio = horarioInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public LocalTime getHorarioFim() { return horarioFim; }
    public void setHorarioFim(LocalTime horarioFim) { this.horarioFim = horarioFim; }

    public int getMaxParticipantes() { return maxParticipantes; }
    public void setMaxParticipantes(int maxParticipantes) { this.maxParticipantes = maxParticipantes; }

    public int getCargaHorariaTotal() { return cargaHorariaTotal; }
    public void setCargaHorariaTotal(int cargaHorariaTotal) { this.cargaHorariaTotal = cargaHorariaTotal; }

    public int getCargaHorariaMinistrante() { return cargaHorariaMinistrante; }
    public void setCargaHorariaMinistrante(int cargaHorariaMinistrante) { this.cargaHorariaMinistrante = cargaHorariaMinistrante; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public List<ParticipanteResumoDTO> getMinistrantes() { return ministrantes; }
    public void setMinistrantes(List<ParticipanteResumoDTO> ministrantes) { this.ministrantes = ministrantes; }
}
