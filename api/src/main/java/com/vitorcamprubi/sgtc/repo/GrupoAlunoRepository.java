package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.GrupoAluno;
import com.vitorcamprubi.sgtc.domain.GrupoStatus;
import com.vitorcamprubi.sgtc.domain.Materia;
import com.vitorcamprubi.sgtc.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GrupoAlunoRepository extends JpaRepository<GrupoAluno, Long> {
    boolean existsByGrupoIdAndAlunoId(Long grupoId, Long alunoId);
    List<GrupoAluno> findByGrupoId(Long grupoId);
    boolean existsByAlunoIdAndGrupoMateriaAndGrupoStatus(Long alunoId, Materia materia, GrupoStatus status);
    @Query("""
            select ga.grupo.id, ga.aluno.nome
            from GrupoAluno ga
            where ga.grupo.id in :grupoIds
            """)
    List<Object[]> findGrupoIdAndAlunoNomeByGrupoIds(@Param("grupoIds") List<Long> grupoIds);

    @Query("select ga.aluno from GrupoAluno ga where ga.grupo.id = :grupoId order by ga.aluno.nome asc")
    List<User> findAlunosByGrupoId(@Param("grupoId") Long grupoId);
    long countByGrupoId(Long grupoId);
    void deleteByGrupoId(Long grupoId);
    int deleteByGrupoIdAndAlunoId(Long grupoId, Long alunoId);
    long countByAlunoId(Long alunoId);
}
