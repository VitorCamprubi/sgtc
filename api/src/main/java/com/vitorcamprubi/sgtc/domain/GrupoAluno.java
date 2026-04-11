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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, columnDefinition = "varchar(16) default 'EM_CURSO'")
    private GrupoAlunoStatus status = GrupoAlunoStatus.EM_CURSO;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }
    public User getAluno() { return aluno; }
    public void setAluno(User aluno) { this.aluno = aluno; }
    public GrupoAlunoStatus getStatus() { return status; }
    public void setStatus(GrupoAlunoStatus status) { this.status = status; }
}
