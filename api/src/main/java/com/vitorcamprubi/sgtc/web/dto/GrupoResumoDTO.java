package com.vitorcamprubi.sgtc.web.dto;

import com.vitorcamprubi.sgtc.domain.Materia;
import com.vitorcamprubi.sgtc.domain.GrupoStatus;

import java.time.LocalDateTime;

public class GrupoResumoDTO {
    private Long id;
    private String titulo;
    private Materia materia;
    private GrupoStatus status;
    private Double notaFinal;
    private LocalDateTime arquivadoEm;
    private Long orientadorId;
    private String orientadorNome;
    private Long coorientadorId;
    private String coorientadorNome;
    private long totalMembros;

    public GrupoResumoDTO() {}
    public GrupoResumoDTO(Long id, String titulo, Materia materia, GrupoStatus status, Double notaFinal,
                          LocalDateTime arquivadoEm,
                          Long orientadorId, String orientadorNome,
                          Long coorientadorId, String coorientadorNome,
                          long totalMembros) {
        this.id = id; this.titulo = titulo; this.materia = materia;
        this.status = status;
        this.notaFinal = notaFinal;
        this.arquivadoEm = arquivadoEm;
        this.orientadorId = orientadorId;
        this.orientadorNome = orientadorNome;
        this.coorientadorId = coorientadorId;
        this.coorientadorNome = coorientadorNome;
        this.totalMembros = totalMembros;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Materia getMateria() { return materia; }
    public void setMateria(Materia materia) { this.materia = materia; }
    public GrupoStatus getStatus() { return status; }
    public void setStatus(GrupoStatus status) { this.status = status; }
    public Double getNotaFinal() { return notaFinal; }
    public void setNotaFinal(Double notaFinal) { this.notaFinal = notaFinal; }
    public LocalDateTime getArquivadoEm() { return arquivadoEm; }
    public void setArquivadoEm(LocalDateTime arquivadoEm) { this.arquivadoEm = arquivadoEm; }
    public Long getOrientadorId() { return orientadorId; }
    public void setOrientadorId(Long orientadorId) { this.orientadorId = orientadorId; }
    public String getOrientadorNome() { return orientadorNome; }
    public void setOrientadorNome(String orientadorNome) { this.orientadorNome = orientadorNome; }
    public Long getCoorientadorId() { return coorientadorId; }
    public void setCoorientadorId(Long coorientadorId) { this.coorientadorId = coorientadorId; }
    public String getCoorientadorNome() { return coorientadorNome; }
    public void setCoorientadorNome(String coorientadorNome) { this.coorientadorNome = coorientadorNome; }
    public long getTotalMembros() { return totalMembros; }
    public void setTotalMembros(long totalMembros) { this.totalMembros = totalMembros; }
}
