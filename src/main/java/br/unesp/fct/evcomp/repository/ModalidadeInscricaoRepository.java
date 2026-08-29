package br.unesp.fct.evcomp.repository;

import br.unesp.fct.evcomp.domain.ModalidadeInscricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ModalidadeInscricaoRepository extends JpaRepository<ModalidadeInscricao, Integer> {

    @Query("SELECT m FROM ModalidadeInscricao m WHERE m.evento.id = :eventoId ORDER BY m.id")
    List<ModalidadeInscricao> buscarPorEvento(@Param("eventoId") Integer eventoId);

    @Query("SELECT m FROM ModalidadeInscricao m WHERE m.evento.id = :eventoId AND m.ativo = true ORDER BY m.id")
    List<ModalidadeInscricao> buscarAtivasPorEvento(@Param("eventoId") Integer eventoId);

    @Query("SELECT m FROM ModalidadeInscricao m JOIN FETCH m.evento ORDER BY m.evento.id, m.id")
    List<ModalidadeInscricao> buscarTodas();

    @Query("SELECT m FROM ModalidadeInscricao m WHERE m.id = :id AND m.evento.id = :eventoId")
    Optional<ModalidadeInscricao> buscarPorIdEEvento(@Param("id") Integer id, @Param("eventoId") Integer eventoId);

    @Query("SELECT m FROM ModalidadeInscricao m WHERE m.evento.id = :eventoId AND LOWER(m.nome) = LOWER(:nome)")
    Optional<ModalidadeInscricao> buscarPorEventoENome(@Param("eventoId") Integer eventoId, @Param("nome") String nome);

    default boolean salvarModalidade(ModalidadeInscricao modalidade) {
        save(modalidade);
        return true;
    }
}
