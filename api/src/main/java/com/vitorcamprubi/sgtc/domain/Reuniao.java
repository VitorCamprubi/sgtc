package com.vitorcamprubi.sgtc.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ReuniaoStatus status;

    @Lob
    @Column
    private String relatorio;

    @Column(name="encerrada_em")
    private LocalDateTime encerradaEm;

    @Column(name = "numero_encontro")
    private Integer numeroEncontro;

    @Column(name = "data_atividades_realizadas")
    private LocalDate dataAtividadesRealizadas;

    @Lob
    @Column(name = "atividades_realizadas")
    private String atividadesRealizadas;

    @Enumerated(EnumType.STRING)
    @Column(name = "desempenho_grupo", length = 20)
    private ReuniaoDesempenhoGrupo desempenhoGrupo;

    @Column(name = "professor_disciplina")
    private String professorDisciplina;

    @Column(name = "orientador_assinatura")
    private String orientadorAssinatura;

    @Column(name = "coorientador_assinatura")
    private String coorientadorAssinatura;

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
    public ReuniaoStatus getStatus() { return status; }
    public void setStatus(ReuniaoStatus status) { this.status = status; }
    public String getRelatorio() { return relatorio; }
    public void setRelatorio(String relatorio) { this.relatorio = relatorio; }
    public LocalDateTime getEncerradaEm() { return encerradaEm; }
    public void setEncerradaEm(LocalDateTime encerradaEm) { this.encerradaEm = encerradaEm; }
    public Integer getNumeroEncontro() { return numeroEncontro; }
    public void setNumeroEncontro(Integer numeroEncontro) { this.numeroEncontro = numeroEncontro; }
    public LocalDate getDataAtividadesRealizadas() { return dataAtividadesRealizadas; }
    public void setDataAtividadesRealizadas(LocalDate dataAtividadesRealizadas) { this.dataAtividadesRealizadas = dataAtividadesRealizadas; }
    public String getAtividadesRealizadas() { return atividadesRealizadas; }
    public void setAtividadesRealizadas(String atividadesRealizadas) { this.atividadesRealizadas = atividadesRealizadas; }
    public ReuniaoDesempenhoGrupo getDesempenhoGrupo() { return desempenhoGrupo; }
    public void setDesempenhoGrupo(ReuniaoDesempenhoGrupo desempenhoGrupo) { this.desempenhoGrupo = desempenhoGrupo; }
    public String getProfessorDisciplina() { return professorDisciplina; }
    public void setProfessorDisciplina(String professorDisciplina) { this.professorDisciplina = professorDisciplina; }
    public String getOrientadorAssinatura() { return orientadorAssinatura; }
    public void setOrientadorAssinatura(String orientadorAssinatura) { this.orientadorAssinatura = orientadorAssinatura; }
    public String getCoorientadorAssinatura() { return coorientadorAssinatura; }
    public void setCoorientadorAssinatura(String coorientadorAssinatura) { this.coorientadorAssinatura = coorientadorAssinatura; }
    public User getCriadoPor() { return criadoPor; }
    public void setCriadoPor(User criadoPor) { this.criadoPor = criadoPor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
