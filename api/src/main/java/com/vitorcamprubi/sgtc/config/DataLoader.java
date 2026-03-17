package com.vitorcamprubi.sgtc.config;

import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DataLoader implements CommandLineRunner {
    private final UserRepository repo;
    private final PasswordEncoder enc;

    public DataLoader(UserRepository repo, PasswordEncoder enc) {
        this.repo = repo; this.enc = enc;
    }

    @Override
    public void run(String... args) {
        if (repo.count() == 0) {
            User admin = new User();
            admin.setNome("Admin");
            admin.setEmail("admin@sgtc.local");
            admin.setSenhaHash(enc.encode("admin123"));
            admin.setRole(Role.ADMIN);
            repo.save(admin);
        }
        if (repo.findByEmail("professor@sgtc.local").isEmpty()) {
            var u = new User();
            u.setNome("Professor");
            u.setEmail("professor@sgtc.local");
            u.setSenhaHash(enc.encode("prof123"));
            u.setRole(Role.PROFESSOR);
            repo.save(u);
        }
        if (repo.findByEmail("aluno@sgtc.local").isEmpty()) {
            var u = new User();
            u.setNome("Aluno");
            u.setEmail("aluno@sgtc.local");
            u.setSenhaHash(enc.encode("aluno123"));
            u.setRole(Role.ALUNO);
            u.setRa("0001");
            repo.save(u);
        }
    }
}
