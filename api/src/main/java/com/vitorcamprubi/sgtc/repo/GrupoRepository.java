package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.Grupo;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    List<Grupo> findByOrientadorIdOrCoorientadorId(Long orientadorId, Long coorientadorId);

    @Query("select ga.grupo from GrupoAluno ga where ga.aluno.id = :alunoId")
    List<Grupo> findByAlunoId(@Param("alunoId") Long alunoId);

    long countByOrientadorIdOrCoorientadorId(Long orientadorId, Long coorientadorId);
}
