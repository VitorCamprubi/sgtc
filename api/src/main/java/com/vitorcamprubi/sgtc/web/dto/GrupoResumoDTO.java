package com.vitorcamprubi.sgtc.web.dto;

public class GrupoResumoDTO {
    private Long id;
    private String titulo;
    private String orientadorNome;
    private String coorientadorNome;
    private long totalMembros;

    public GrupoResumoDTO() {}
    public GrupoResumoDTO(Long id, String titulo, String orientadorNome, String coorientadorNome, long totalMembros) {
        this.id = id; this.titulo = titulo; this.orientadorNome = orientadorNome;
        this.coorientadorNome = coorientadorNome; this.totalMembros = totalMembros;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getOrientadorNome() { return orientadorNome; }
    public void setOrientadorNome(String orientadorNome) { this.orientadorNome = orientadorNome; }
    public String getCoorientadorNome() { return coorientadorNome; }
    public void setCoorientadorNome(String coorientadorNome) { this.coorientadorNome = coorientadorNome; }
    public long getTotalMembros() { return totalMembros; }
    public void setTotalMembros(long totalMembros) { this.totalMembros = totalMembros; }
}
