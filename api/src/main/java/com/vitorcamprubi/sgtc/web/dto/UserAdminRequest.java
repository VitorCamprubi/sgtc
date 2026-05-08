package com.vitorcamprubi.sgtc.web.dto;

import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.security.password.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserAdminRequest {
    @NotBlank
    private String nome;

    @NotBlank @Email
    private String email;

    /**
     * Em criacao a senha eh obrigatoria; em atualizacao pode vir vazia para
     * manter a senha atual. Por isso allowBlank=true e a obrigatoriedade
     * eh checada no service apenas no fluxo de criacao.
     */
    @StrongPassword(allowBlank = true)
    private String senha;

    @NotNull
    private Role role;

    private String ra; // somente para alunos

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getRa() { return ra; }
    public void setRa(String ra) { this.ra = ra; }
}
