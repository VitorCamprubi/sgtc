package com.vitorcamprubi.sgtc.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_comentarios")
public class DocumentoComentario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "documento_id")
    private DocumentoVersao documento;

    @ManyToOne(optional = false) @JoinColumn(name = "autor_id")
    private User autor;

    @Column(nullable = false, length = 4000)
    private String texto;

    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public DocumentoVersao getDocumento() { return documento; }
    public void setDocumento(DocumentoVersao documento) { this.documento = documento; }
    public User getAutor() { return autor; }
    public void setAutor(User autor) { this.autor = autor; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
