package br.unesp.fct.evcomp.repository;

import br.unesp.fct.evcomp.domain.ComprovanteBlob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComprovanteBlobRepository extends JpaRepository<ComprovanteBlob, Integer> {

    @Query("SELECT c FROM ComprovanteBlob c WHERE c.id = :id")
    Optional<ComprovanteBlob> buscarComprovantePorId(@Param("id") Integer id);

    default ComprovanteBlob salvarComprovante(ComprovanteBlob comprovante) {
        return this.save(comprovante);
    }

    default void removerComprovantePorId(Integer id) {
        this.deleteById(id);
    }
}
