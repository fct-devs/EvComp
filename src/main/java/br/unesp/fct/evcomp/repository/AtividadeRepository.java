package br.unesp.fct.evcomp.repository;

import br.unesp.fct.evcomp.domain.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AtividadeRepository extends JpaRepository<Atividade, Integer> {
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Atividade a WHERE a.titulo = :titulo AND a.evento.id = :eventoId")
    Optional<Atividade> buscarAtividadePorTitulo(@org.springframework.data.repository.query.Param("titulo") String titulo, @org.springframework.data.repository.query.Param("eventoId") Integer eventoId);
   
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Atividade a WHERE a.evento.id = :eventoId ORDER BY a.dataInicio ASC, a.horarioInicio ASC, a.titulo ASC")
    List<Atividade> buscarAtividadesPorEvento(@org.springframework.data.repository.query.Param("eventoId") Integer eventoId);

    @org.springframework.data.jpa.repository.Query("SELECT a.maxParticipantes - (SELECT COUNT(i) FROM Inscrição i JOIN i.atividade atv WHERE atv.id = a.id AND i.status = true) FROM Atividade a WHERE a.id = :atividadeId")
    Integer consultarVagasDisponiveis(@org.springframework.data.repository.query.Param("atividadeId") Integer atividadeId);
    
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Atividade a WHERE a.id = :atividadeId")
    Optional<Atividade> buscarAtividadePorId(@org.springframework.data.repository.query.Param("atividadeId") Integer atividadeId);
    
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Atividade a JOIN a.ministrantes m WHERE m.id = :ministranteId")
    List<Atividade> buscarAtividadesPorMinistrante(@org.springframework.data.repository.query.Param("ministranteId") Integer ministranteId);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM Atividade a WHERE a.id = :atividadeId")
    void removerAtividade(@org.springframework.data.repository.query.Param("atividadeId") Integer atividadeId);
    

    default boolean checarAndamentoAtividade(Integer atividadeId) { 
        Optional<Atividade> atv = buscarAtividadePorId(atividadeId);
        
        if (atv.isPresent()) {
            Atividade atividade = atv.get();
            
            if (atividade.getDataFim() == null) return true;
            
            if (atividade.getDataInicio() != null && atividade.getDataInicio().equals(atividade.getDataFim())) {
                return !java.time.LocalDate.now().isAfter(atividade.getDataFim());
            } else {
                return atividade.getDataFim().isAfter(java.time.LocalDate.now());
            }
        }
        
        return false;
    }
    
    default int buscarCargaHorariaAtividade(Integer atividadeId) { 
        return findById(atividadeId).map(Atividade::getCargaHorariaTotal).orElse(0); 
    }
    
    default br.unesp.fct.evcomp.domain.TipoContabilizacao buscarTipoEventoPorAtividade(Integer atividadeId) {
        return findById(atividadeId).map(a -> a.getEvento().getTipoContabilizacao()).orElse(null);
    }
    
    default Atividade verificarAtividadeCadastrada(String titulo, Integer eventoId) { 
        return buscarAtividadePorTitulo(titulo, eventoId).orElse(null);
    }

    default boolean salvarAtividade(Atividade atividade) {
        try {
            save(atividade);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
