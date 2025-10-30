package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.DocumentoVersao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentoVersaoRepository extends JpaRepository<DocumentoVersao, Long> {
    int countByGrupoId(Long grupoId);
    List<DocumentoVersao> findByGrupoIdOrderByVersaoDesc(Long grupoId);
}
