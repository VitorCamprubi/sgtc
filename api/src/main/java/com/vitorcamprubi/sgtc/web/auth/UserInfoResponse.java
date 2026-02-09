package com.vitorcamprubi.sgtc.web.auth;

import com.vitorcamprubi.sgtc.domain.Role;

public class UserInfoResponse {
    private final Long id;
    private final String nome;
    private final String email;
    private final Role role;
    private final String ra;

    public UserInfoResponse(Long id, String nome, String email, Role role, String ra) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.role = role;
        this.ra = ra;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public String getRa() {
        return ra;
    }
}
