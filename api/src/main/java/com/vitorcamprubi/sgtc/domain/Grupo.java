package com.vitorcamprubi.sgtc.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "grupos")
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String titulo;

    @ManyToOne(optional=false) @JoinColumn(name="orientador_id")
    private User orientador;

    @ManyToOne @JoinColumn(name="coorientador_id")
    private User coorientador;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 3)
    private Materia materia = Materia.TG;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GrupoStatus status = GrupoStatus.EM_CURSO;

    @Column(name = "nota_final")
    private Double notaFinal;

    @Column(name = "arquivado_em")
    private LocalDateTime arquivadoEm;

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
    public Materia getMateria() { return materia; }
    public void setMateria(Materia materia) { this.materia = materia; }
    public GrupoStatus getStatus() { return status; }
    public void setStatus(GrupoStatus status) { this.status = status; }
    public Double getNotaFinal() { return notaFinal; }
    public void setNotaFinal(Double notaFinal) { this.notaFinal = notaFinal; }
    public LocalDateTime getArquivadoEm() { return arquivadoEm; }
    public void setArquivadoEm(LocalDateTime arquivadoEm) { this.arquivadoEm = arquivadoEm; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
