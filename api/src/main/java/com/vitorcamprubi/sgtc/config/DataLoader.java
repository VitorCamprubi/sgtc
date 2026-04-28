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
            admin.setEmailConfirmado(true);
            repo.save(admin);
        }
        if (repo.findByEmail("professor@sgtc.local").isEmpty()) {
            var u = new User();
            u.setNome("Professor");
            u.setEmail("professor@sgtc.local");
            u.setSenhaHash(enc.encode("prof123"));
            u.setRole(Role.PROFESSOR);
            u.setEmailConfirmado(true);
            repo.save(u);
        }
        if (repo.findByEmail("aluno@sgtc.local").isEmpty()) {
            var u = new User();
            u.setNome("Aluno");
            u.setEmail("aluno@sgtc.local");
            u.setSenhaHash(enc.encode("aluno123"));
            u.setRole(Role.ALUNO);
            u.setRa("0001");
            u.setEmailConfirmado(true);
            repo.save(u);
        }

        // Marca usuarios pre-existentes como verificados (migracao leve)
        repo.findAll().forEach(usuario -> {
            if (!usuario.isEmailConfirmado() && usuario.getTokenConfirmacao() == null) {
                // Apenas usuarios criados ANTES desta funcionalidade (sem token gerado)
                // sao considerados verificados, para nao quebrar logins existentes.
                usuario.setEmailConfirmado(true);
                repo.save(usuario);
            }
        });
    }
}
