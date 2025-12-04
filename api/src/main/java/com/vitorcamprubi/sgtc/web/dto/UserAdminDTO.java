package com.vitorcamprubi.sgtc.web.dto;

import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;

public class UserAdminDTO {
    public Long id;
    public String nome;
    public String email;
    public Role role;
    public String ra;

    public static UserAdminDTO of(User u) {
        UserAdminDTO dto = new UserAdminDTO();
        dto.id = u.getId();
        dto.nome = u.getNome();
        dto.email = u.getEmail();
        dto.role = u.getRole();
        dto.ra = u.getRa();
        return dto;
    }
}
