package br.unesp.fct.evcomp.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "atividade")
public class Atividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAtividade")
    private Integer id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Column(name = "pre_requisitos", columnDefinition = "TEXT")
    private String preRequisitos;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horarioInicio;

    @Column(name = "data_termino", nullable = false)
    private LocalDate dataFim;

    @Column(name = "hora_termino", nullable = false)
    private LocalTime horarioFim;

    @Column(name = "max_participantes", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private int maxParticipantes;

    @Column(name = "carga_horaria_total", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private int cargaHorariaTotal;

    @Column(name = "carga_horaria_ministrante", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private int cargaHorariaMinistrante;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idEvento", nullable = false)
    private Evento evento;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "ministrante_atividade",
        joinColumns = @JoinColumn(name = "idAtividade"),
        inverseJoinColumns = @JoinColumn(name = "idUsuário")
    )
    private List<Usuário> ministrantes = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToMany(mappedBy = "atividade", fetch = FetchType.LAZY)
    private List<Inscrição> inscricoes = new ArrayList<>();

    @Transient
    private RegistroDePresenca[] registroDePresenca;

    @PreRemove
    private void removeInscricoes() {
        for (Inscrição inscricao : inscricoes) {
            inscricao.getAtividade().remove(this);
            if (inscricao.getAtividade().isEmpty()) {
                inscricao.setStatus(false);
            }
        }
    }

    public Atividade() {

    }

    public Atividade(String titulo, String descricao, String preRequisitos, LocalDate dataInicio, LocalTime horarioInicio, LocalDate dataFim, LocalTime horarioFim, int maxParticipantes, int cargaHorariaTotal, int cargaHorariaMinistrante) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.preRequisitos = preRequisitos;
        this.dataInicio = dataInicio;
        this.horarioInicio = horarioInicio;
        this.dataFim = dataFim;
        this.horarioFim = horarioFim;
        this.maxParticipantes = maxParticipantes;
        this.cargaHorariaTotal = cargaHorariaTotal;
        this.cargaHorariaMinistrante = cargaHorariaMinistrante;
    }

    public static Atividade criarAtividade(String titulo, String descricao, String preRequisitos, LocalDate data_inicio, LocalTime horario_inicio, LocalDate data_termino, LocalTime horario_termino, int max_participantes, int carga_horaria_total, List<Participante> ministrantes, int carga_horaria_ministrantes) {
        Atividade atv = new Atividade(titulo, descricao, preRequisitos, data_inicio, horario_inicio, data_termino, horario_termino, max_participantes, carga_horaria_total, carga_horaria_ministrantes);
        if (ministrantes != null) {
            atv.getMinistrantes().addAll(ministrantes);
        }
        return atv;
    }

    public Object pegarDadosAtividade() {
        java.util.Map<String, Object> dados = new java.util.HashMap<>();
        dados.put("id", this.id);
        dados.put("titulo", this.titulo);
        dados.put("descricao", this.descricao);
        dados.put("preRequisitos", this.preRequisitos);
        dados.put("dataInicio", this.dataInicio);
        dados.put("horarioInicio", this.horarioInicio);
        dados.put("dataFim", this.dataFim);
        dados.put("horarioFim", this.horarioFim);
        dados.put("maxParticipantes", this.maxParticipantes);
        dados.put("cargaHorariaTotal", this.cargaHorariaTotal);
        dados.put("cargaHorariaMinistrante", this.cargaHorariaMinistrante);
        
        java.util.List<Integer> ministrantesIds = new java.util.ArrayList<>();
        if (this.ministrantes != null) {
            for (Usuário m : this.ministrantes) {
                ministrantesIds.add(m.getId());
            }
        }
        dados.put("ministrantes_ids", ministrantesIds);
        
        if (this.evento != null) {
            dados.put("evento_id", this.evento.getId());
        }
        return dados;
    }

    @SuppressWarnings("unchecked")
    public boolean alterarDadosAtividade(Object novosDadosAtividade) {
        try {
            java.util.Map<String, Object> req = (java.util.Map<String, Object>) novosDadosAtividade;
            
            if (req.get("titulo") != null) this.titulo = String.valueOf(req.get("titulo"));
            if (req.get("descricao") != null) this.descricao = String.valueOf(req.get("descricao"));
            if (req.get("pre_requisitos") != null) this.preRequisitos = String.valueOf(req.get("pre_requisitos"));
            if (req.get("data_inicio") != null) this.dataInicio = java.time.LocalDate.parse(String.valueOf(req.get("data_inicio")));
            if (req.get("data_termino") != null) this.dataFim = java.time.LocalDate.parse(String.valueOf(req.get("data_termino")));
            if (req.get("horario_inicio") != null) this.horarioInicio = java.time.LocalTime.parse(String.valueOf(req.get("horario_inicio")));
            if (req.get("horario_termino") != null) this.horarioFim = java.time.LocalTime.parse(String.valueOf(req.get("horario_termino")));
            if (req.get("max_participantes") != null) this.maxParticipantes = Integer.parseInt(String.valueOf(req.get("max_participantes")));
            if (req.get("carga_horaria_total") != null) this.cargaHorariaTotal = Integer.parseInt(String.valueOf(req.get("carga_horaria_total")));
            if (req.get("carga_horaria_ministrantes") != null) this.cargaHorariaMinistrante = Integer.parseInt(String.valueOf(req.get("carga_horaria_ministrantes")));
            
            if (req.get("novos_ministrantes") != null) {
                this.ministrantes.clear();
                this.ministrantes.addAll((java.util.List<Participante>) req.get("novos_ministrantes"));
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public boolean verificarConflitoHorarios(Atividade outra) {
        LocalDateTime inicioEste = LocalDateTime.of(getDataInicio(), getHorarioInicio());
        LocalDateTime fimEste = LocalDateTime.of(getDataFim(), getHorarioFim());
        LocalDateTime inicioOutro = LocalDateTime.of(outra.getDataInicio(), outra.getHorarioInicio());
        LocalDateTime fimOutro = LocalDateTime.of(outra.getDataFim(), outra.getHorarioFim());

        return inicioEste.isBefore(fimOutro) && fimEste.isAfter(inicioOutro);
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

    public String getDescricao() { return descricao; }

    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getPreRequisitos() { return preRequisitos; }

    public void setPreRequisitos(String preRequisitos) { this.preRequisitos = preRequisitos; }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalTime getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(LocalTime horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public LocalTime getHorarioFim() {
        return horarioFim;
    }

    public void setHorarioFim(LocalTime horarioFim) {
        this.horarioFim = horarioFim;
    }

    public int getMaxParticipantes() {
        return maxParticipantes;
    }

    public void setMaxParticipantes(int maxParticipantes) {
        this.maxParticipantes = maxParticipantes;
    }

    public int getCargaHorariaTotal() {
        return cargaHorariaTotal;
    }

    public void setCargaHorariaTotal(int cargaHorariaTotal) {
        this.cargaHorariaTotal = cargaHorariaTotal;
    }

    public int getCargaHorariaMinistrante() {
        return cargaHorariaMinistrante;
    }

    public void setCargaHorariaMinistrante(int cargaHorariaMinistrante) {
        this.cargaHorariaMinistrante = cargaHorariaMinistrante;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public List<Usuário> getMinistrantes() {
        return ministrantes;
    }

    public void setMinistrantes(List<Usuário> ministrantes) {
        this.ministrantes = ministrantes;
    }

    public List<Inscrição> getInscricoes() {
        return inscricoes;
    }

    public void setInscricoes(List<Inscrição> inscricoes) {
        this.inscricoes = inscricoes;
    }

}
