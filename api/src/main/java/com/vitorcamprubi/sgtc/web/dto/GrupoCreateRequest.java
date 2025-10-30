package com.vitorcamprubi.sgtc.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GrupoCreateRequest {
    @NotBlank private String titulo;
    @NotNull  private Long orientadorId;
    private Long coorientadorId;

    // getters/setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Long getOrientadorId() { return orientadorId; }
    public void setOrientadorId(Long orientadorId) { this.orientadorId = orientadorId; }
    public Long getCoorientadorId() { return coorientadorId; }
    public void setCoorientadorId(Long coorientadorId) { this.coorientadorId = coorientadorId; }
}
