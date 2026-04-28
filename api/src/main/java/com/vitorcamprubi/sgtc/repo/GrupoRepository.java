package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.Grupo;
import com.vitorcamprubi.sgtc.domain.GrupoStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    List<Grupo> findByOrientadorIdOrCoorientadorId(Long orientadorId, Long coorientadorId);
    List<Grupo> findByStatusOrderByIdAsc(GrupoStatus status);

    @Query("""
            select g from Grupo g
            where g.status = :status
              and (g.orientador.id = :professorId or g.coorientador.id = :professorId)
            order by g.id asc
            """)
    List<Grupo> findByProfessorAndStatus(@Param("professorId") Long professorId,
                                         @Param("status") GrupoStatus status);

    @Query("select g.id from Grupo g order by g.id asc")
    List<Long> findAllIdsOrderByIdAsc();

    @Query("select ga.grupo from GrupoAluno ga where ga.aluno.id = :alunoId")
    List<Grupo> findByAlunoId(@Param("alunoId") Long alunoId);

    @Query("""
            select ga.grupo from GrupoAluno ga
            where ga.aluno.id = :alunoId
              and ga.grupo.status = :status
            order by ga.grupo.id asc
            """)
    List<Grupo> findByAlunoIdAndGrupoStatus(@Param("alunoId") Long alunoId,
                                            @Param("status") GrupoStatus status);

    long countByOrientadorIdOrCoorientadorId(Long orientadorId, Long coorientadorId);
}
