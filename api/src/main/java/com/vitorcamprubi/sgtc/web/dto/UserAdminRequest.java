package com.vitorcamprubi.sgtc.web.dto;

import com.vitorcamprubi.sgtc.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserAdminRequest {
    @NotBlank
    private String nome;

    @NotBlank @Email
    private String email;

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
