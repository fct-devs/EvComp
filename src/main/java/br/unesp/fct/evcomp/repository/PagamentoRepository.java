package br.unesp.fct.evcomp.repository;

import br.unesp.fct.evcomp.domain.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Integer> {

    @Query("SELECT p FROM Pagamento p JOIN FETCH p.inscricao i JOIN FETCH i.evento JOIN FETCH i.participante WHERE p.id = :id")
    Optional<Pagamento> buscarPagamentoPorId(@Param("id") Integer id);

    @Query("SELECT p FROM Pagamento p JOIN FETCH p.inscricao i JOIN FETCH i.evento JOIN FETCH i.participante WHERE i.id = :inscricaoId")
    Optional<Pagamento> buscarPorInscricao(@Param("inscricaoId") Integer inscricaoId);

    @Query("SELECT p FROM Pagamento p JOIN FETCH p.inscricao i JOIN FETCH i.evento "
         + "WHERE i.participante.id = :participanteId AND i.status = true "
         + "ORDER BY i.dataInscricao DESC")
    List<Pagamento> buscarPagamentosPorParticipante(@Param("participanteId") Integer participanteId);

    @Query("SELECT p FROM Pagamento p JOIN FETCH p.inscricao i JOIN FETCH i.evento JOIN FETCH i.participante "
         + "WHERE p.status = br.unesp.fct.evcomp.domain.StatusPagamento.PENDENTE "
         + "AND p.armazenamentoRef IS NOT NULL "
         + "ORDER BY p.dataEnvio ASC")
    List<Pagamento> buscarPagamentosPendentes();

    @Query("SELECT p FROM Pagamento p JOIN FETCH p.inscricao i JOIN FETCH i.evento JOIN FETCH i.participante "
         + "WHERE i.evento.id = :eventoId "
         + "ORDER BY p.status ASC, i.dataInscricao DESC")
    List<Pagamento> buscarPagamentosPorEvento(@Param("eventoId") Integer eventoId);

    default Pagamento salvarPagamento(Pagamento pagamento) {
        return this.save(pagamento);
    }
}
