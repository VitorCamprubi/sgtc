package com.vitorcamprubi.sgtc.domain;

import jakarta.persistence.*;

@Entity
@Table(name="grupo_aluno",
        uniqueConstraints=@UniqueConstraint(columnNames={"grupo_id","aluno_id"}))
public class GrupoAluno {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="grupo_id")
    private Grupo grupo;

    @ManyToOne(optional=false) @JoinColumn(name="aluno_id")
    private User aluno;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }
    public User getAluno() { return aluno; }
    public void setAluno(User aluno) { this.aluno = aluno; }
}
