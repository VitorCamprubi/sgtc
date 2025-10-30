package com.vitorcamprubi.sgtc.config;

import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
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
        if (repo.findByEmail("orientador@sgtc.local").isEmpty()) {
            var u = new User();
            u.setNome("Prof. Orientador");
            u.setEmail("orientador@sgtc.local");
            u.setSenhaHash(enc.encode("ori123"));
            u.setRole(Role.ORIENTADOR);
            repo.save(u);
        }
        if (repo.findByEmail("aluno@sgtc.local").isEmpty()) {
            var u = new User();
            u.setNome("Aluno 1");
            u.setEmail("aluno@sgtc.local");
            u.setSenhaHash(enc.encode("aluno123"));
            u.setRole(Role.ALUNO);
            u.setRa("0001");
            repo.save(u);
        }
    }
}
