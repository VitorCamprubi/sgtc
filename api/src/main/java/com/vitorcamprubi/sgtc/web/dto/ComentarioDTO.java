package com.vitorcamprubi.sgtc.web.dto;

import com.vitorcamprubi.sgtc.domain.DocumentoComentario;
import java.time.LocalDateTime;

public class ComentarioDTO {
    public Long id;
    public String autor;
    public String texto;
    public LocalDateTime createdAt;

    public static ComentarioDTO of(DocumentoComentario c) {
        ComentarioDTO dto = new ComentarioDTO();
        dto.id = c.getId();
        dto.autor = c.getAutor().getNome();
        dto.texto = c.getTexto();
        dto.createdAt = c.getCreatedAt();
        return dto;
    }
}
