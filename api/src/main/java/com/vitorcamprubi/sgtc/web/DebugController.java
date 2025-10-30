package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DebugController {
    private final UserRepository users;
    public DebugController(UserRepository users) { this.users = users; }

    @GetMapping("/public/debug/users") // liberado pelo SecurityConfig
    public List<User> listUsers() { return users.findAll(); } // DEV ONLY
}
