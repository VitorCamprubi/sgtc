package com.vitorcamprubi.sgtc.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grupos")
public class Grupo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String titulo;

    @ManyToOne(optional=false) @JoinColumn(name="orientador_id")
    private User orientador;

    @ManyToOne @JoinColumn(name="coorientador_id")
    private User coorientador;

    @Column(nullable=false, updatable=false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public User getOrientador() { return orientador; }
    public void setOrientador(User orientador) { this.orientador = orientador; }
    public User getCoorientador() { return coorientador; }
    public void setCoorientador(User coorientador) { this.coorientador = coorientador; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
