package com.vitorcamprubi.sgtc.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reunioes")
public class Reuniao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="grupo_id")
    private Grupo grupo;

    @Column(nullable=false, name="data_hora")
    private LocalDateTime dataHora;

    @Column(nullable=false)
    private String pauta;

    @Column
    private String observacoes;

    @ManyToOne(optional=false) @JoinColumn(name="criado_por")
    private User criadoPor;

    @Column(nullable=false, updatable=false, name="created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getPauta() { return pauta; }
    public void setPauta(String pauta) { this.pauta = pauta; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public User getCriadoPor() { return criadoPor; }
    public void setCriadoPor(User criadoPor) { this.criadoPor = criadoPor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
