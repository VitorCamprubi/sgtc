package com.vitorcamprubi.sgtc.web.dto;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import java.time.LocalDateTime;

public class ReuniaoDTO {
    public Long id;
    public LocalDateTime dataHora;
    public String pauta;
    public String observacoes;
    public String criadoPor;

    public static ReuniaoDTO of(Reuniao r) {
        ReuniaoDTO dto = new ReuniaoDTO();
        dto.id = r.getId(); dto.dataHora = r.getDataHora();
        dto.pauta = r.getPauta(); dto.observacoes = r.getObservacoes();
        dto.criadoPor = r.getCriadoPor().getNome();
        return dto;
    }
}
