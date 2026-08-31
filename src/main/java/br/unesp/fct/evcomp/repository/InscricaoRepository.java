package br.unesp.fct.evcomp.repository;

import br.unesp.fct.evcomp.domain.Inscrição;
import br.unesp.fct.evcomp.domain.Participante;
import br.unesp.fct.evcomp.domain.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InscricaoRepository extends JpaRepository<Inscrição, Integer> {
    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inscrição i WHERE i.participante.id = :participanteId AND i.evento.id = :eventoId")
    Optional<Inscrição> buscarPorParticipanteEEvento(@org.springframework.data.repository.query.Param("participanteId") Integer participanteId, @org.springframework.data.repository.query.Param("eventoId") Integer eventoId);

    @Query("SELECT i FROM Inscrição i JOIN FETCH i.evento JOIN FETCH i.participante WHERE i.id = :inscricaoId")
    Optional<Inscrição> buscarPorIdComDetalhes(@Param("inscricaoId") Integer inscricaoId);
    
    @org.springframework.data.jpa.repository.Query("SELECT i.participante FROM Inscrição i WHERE i.evento.id = :eventoId AND i.status = true")
    List<Participante> buscarParticipantesPorEvento(@Param("eventoId") Integer eventoId);

    @org.springframework.data.jpa.repository.Query("SELECT i.evento.id FROM Inscrição i WHERE i.participante.id = :participanteId AND i.status = true")
    List<Integer> buscarEventosInscritosPorParticipante(@Param("participanteId") Integer participanteId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inscrição i LEFT JOIN FETCH i.atividade JOIN FETCH i.evento WHERE i.participante.id = :participanteId AND i.status = true")
    List<Inscrição> buscarInscricoesAtivasPorParticipante(@Param("participanteId") Integer participanteId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inscrição i LEFT JOIN FETCH i.atividade JOIN FETCH i.evento WHERE i.participante.id = :participanteId")
    List<Inscrição> buscarInscricoesPorParticipante(@Param("participanteId") Integer participanteId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inscrição i JOIN i.atividade a WHERE a.id = :atividadeId AND i.status = true")
    List<Inscrição> buscarInscricoesPorAtividade(@Param("atividadeId") Integer atividadeId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inscrição i JOIN i.atividade a WHERE i.participante.id = :participanteId AND a.id = :atividadeId")
    Optional<Inscrição> buscarPorParticipanteEAtividade(@Param("participanteId") Integer participanteId, @Param("atividadeId") Integer atividadeId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM Inscrição i JOIN i.atividade a WHERE a.id = :atividadeId")
    int contarInscritosPorAtividadeInt(@org.springframework.data.repository.query.Param("atividadeId") Integer atividadeId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM inscrição_atividade WHERE idAtividade = :atividadeId", nativeQuery = true)
    void removerInscricoesPorAtividade(@org.springframework.data.repository.query.Param("atividadeId") Integer atividadeId);

    default int contarInscritosPorAtividade(String atividadeId) { 
        return contarInscritosPorAtividadeInt(Integer.valueOf(atividadeId));
    }

    default void salvarInscricao(Inscrição inscricao) {
        this.save(inscricao);
    }

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM Inscrição i WHERE i.modalidade.id = :modalidadeId")
    int contarInscricoesPorModalidade(@org.springframework.data.repository.query.Param("modalidadeId") Integer modalidadeId);

}
