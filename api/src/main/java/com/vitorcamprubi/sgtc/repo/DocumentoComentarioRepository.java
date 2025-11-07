package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.DocumentoComentario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentoComentarioRepository extends JpaRepository<DocumentoComentario, Long> {
    List<DocumentoComentario> findByDocumentoIdOrderByCreatedAtAsc(Long documentoId);
    void deleteByDocumentoId(Long documentoId);
}
