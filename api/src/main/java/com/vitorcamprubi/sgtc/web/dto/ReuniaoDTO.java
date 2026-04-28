package com.vitorcamprubi.sgtc.web.dto;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.domain.ReuniaoStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReuniaoDTO {
    public Long id;
    public LocalDateTime dataHora;
    public String pauta;
    public String observacoes;
    public String status;
    public String relatorio;
    public LocalDateTime encerradaEm;
    public Integer numeroEncontro;
    public LocalDate dataAtividadesRealizadas;
    public String atividadesRealizadas;
    public String desempenhoGrupo;
    public String professorDisciplina;
    public String orientadorAssinatura;
    public String coorientadorAssinatura;
    public String criadoPor;

    public static ReuniaoDTO of(Reuniao r) {
        ReuniaoDTO dto = new ReuniaoDTO();
        dto.id = r.getId(); dto.dataHora = r.getDataHora();
        dto.pauta = r.getPauta(); dto.observacoes = r.getObservacoes();
        dto.status = (r.getStatus() == null ? ReuniaoStatus.AGUARDANDO_DATA_REUNIAO : r.getStatus()).name();
        dto.relatorio = r.getRelatorio();
        dto.encerradaEm = r.getEncerradaEm();
        dto.numeroEncontro = r.getNumeroEncontro();
        dto.dataAtividadesRealizadas = r.getDataAtividadesRealizadas();
        dto.atividadesRealizadas = r.getAtividadesRealizadas();
        dto.desempenhoGrupo = (r.getDesempenhoGrupo() == null ? null : r.getDesempenhoGrupo().name());
        dto.professorDisciplina = r.getProfessorDisciplina();
        dto.orientadorAssinatura = r.getOrientadorAssinatura();
        dto.coorientadorAssinatura = r.getCoorientadorAssinatura();
        dto.criadoPor = r.getCriadoPor().getNome();
        return dto;
    }
}
