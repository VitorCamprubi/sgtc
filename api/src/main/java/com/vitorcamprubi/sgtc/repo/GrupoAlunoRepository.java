package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.GrupoAluno;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoAlunoRepository extends JpaRepository<GrupoAluno, Long> {
    boolean existsByGrupoIdAndAlunoId(Long grupoId, Long alunoId);
    long countByGrupoId(Long grupoId);
}
