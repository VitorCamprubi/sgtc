package com.vitorcamprubi.sgtc.domain;

import jakarta.persistence.*;

<<<<<<< HEAD
import java.time.LocalDateTime;

=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String nome;

    @Column(nullable=false, unique=true)
    private String email;

    @Column(nullable=false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 20, columnDefinition = "varchar(20)")
    private Role role;

    @Column(unique = true)
    private String ra; // opcional (só p/ alunos)

<<<<<<< HEAD
    // === Verificação de e-mail ===
    @Column(name = "email_confirmado", nullable = false, columnDefinition = "boolean default false")
    private boolean emailConfirmado = false;

    @Column(name = "token_confirmacao", length = 100)
    private String tokenConfirmacao;

    @Column(name = "token_confirmacao_expira_em")
    private LocalDateTime tokenConfirmacaoExpiraEm;

=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getRa() { return ra; }
    public void setRa(String ra) { this.ra = ra; }
<<<<<<< HEAD
    public boolean isEmailConfirmado() { return emailConfirmado; }
    public void setEmailConfirmado(boolean emailConfirmado) { this.emailConfirmado = emailConfirmado; }
    public String getTokenConfirmacao() { return tokenConfirmacao; }
    public void setTokenConfirmacao(String tokenConfirmacao) { this.tokenConfirmacao = tokenConfirmacao; }
    public LocalDateTime getTokenConfirmacaoExpiraEm() { return tokenConfirmacaoExpiraEm; }
    public void setTokenConfirmacaoExpiraEm(LocalDateTime tokenConfirmacaoExpiraEm) {
        this.tokenConfirmacaoExpiraEm = tokenConfirmacaoExpiraEm;
    }
=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
}
