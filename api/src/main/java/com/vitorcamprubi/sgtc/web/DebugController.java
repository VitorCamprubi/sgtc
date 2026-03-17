package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.repo.UserRepository;
import com.vitorcamprubi.sgtc.web.dto.UserAdminDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/debug")
@PreAuthorize("hasRole('ADMIN')")
public class DebugController {
    private final UserRepository users;

    public DebugController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/users")
    public List<UserAdminDTO> listUsers() {
        return users.findAll().stream().map(UserAdminDTO::of).toList();
    }
}
