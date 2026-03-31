package com.vitorcamprubi.sgtc.web.dto;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.domain.ReuniaoStatus;
import java.time.LocalDateTime;

public class ReuniaoDTO {
    public Long id;
    public LocalDateTime dataHora;
    public String pauta;
    public String observacoes;
    public String status;
    public String relatorio;
    public LocalDateTime encerradaEm;
    public String criadoPor;

    public static ReuniaoDTO of(Reuniao r) {
        ReuniaoDTO dto = new ReuniaoDTO();
        dto.id = r.getId(); dto.dataHora = r.getDataHora();
        dto.pauta = r.getPauta(); dto.observacoes = r.getObservacoes();
        dto.status = (r.getStatus() == null ? ReuniaoStatus.AGUARDANDO_DATA_REUNIAO : r.getStatus()).name();
        dto.relatorio = r.getRelatorio();
        dto.encerradaEm = r.getEncerradaEm();
        dto.criadoPor = r.getCriadoPor().getNome();
        return dto;
    }
}
