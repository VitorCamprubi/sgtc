package com.vitorcamprubi.sgtc.web.dto;

import com.vitorcamprubi.sgtc.domain.DocumentoVersao;
import java.time.LocalDateTime;

public class DocumentoDTO {
    public Long id; public String titulo; public int versao;
    public String enviadoPor; public LocalDateTime createdAt; public long tamanho;

    public static DocumentoDTO of(DocumentoVersao d) {
        DocumentoDTO dto = new DocumentoDTO();
        dto.id = d.getId(); dto.titulo = d.getTitulo(); dto.versao = d.getVersao();
        dto.enviadoPor = d.getEnviadoPor().getNome(); dto.createdAt = d.getCreatedAt(); dto.tamanho = d.getTamanho();
        return dto;
    }
}
