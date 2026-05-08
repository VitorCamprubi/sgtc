package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.notification.EmailService;
import com.vitorcamprubi.sgtc.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fluxo "Esqueci minha senha":
 *  1. solicitarRecuperacao(email) gera um token UUID com TTL de 1h e envia
 *     um e-mail com o link de redefinicao.
 *  2. redefinirSenha(token, novaSenha) valida o token (existe, nao expirou),
 *     aplica BCrypt na nova senha e invalida o token.
 *
 * Por seguranca, solicitarRecuperacao nunca revela se o e-mail existe -
 * sempre retorna sem erro, mesmo que nao encontre conta.
 */
@Service
public class PasswordResetService {
    private static final int TOKEN_TTL_HORAS = 1;

    private final UserRepository users;
    private final PasswordEncoder enc;
    private final EmailService emailService;

    public PasswordResetService(UserRepository users, PasswordEncoder enc, EmailService emailService) {
        this.users = users;
        this.enc = enc;
        this.emailService = emailService;
    }

    /**
     * Recebe um email; se houver conta verificada, gera token e dispara email.
     * Nunca lanca excecao por email inexistente (evita enumeration attack).
     */
    @Transactional
    public void solicitarRecuperacao(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        User u = users.findByEmail(email.trim()).orElse(null);
        if (u == null) {
            return;
        }
        // Mesmo se nao confirmou email, deixamos recuperar - usuario pode
        // ter perdido o link de confirmacao + senha. Se preferir bloquear,
        // adicionar check de isEmailConfirmado() aqui.

        String token = UUID.randomUUID().toString();
        u.setTokenRecuperacao(token);
        u.setTokenRecuperacaoExpiraEm(LocalDateTime.now().plusHours(TOKEN_TTL_HORAS));
        users.save(u);

        try {
            emailService.enviarRecuperacaoSenha(u, token);
        } catch (RuntimeException ignored) {
            // best-effort
        }
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido");
        }
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nova senha eh obrigatoria");
        }

        User u = users.findByTokenRecuperacao(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token nao encontrado"));

        if (u.getTokenRecuperacaoExpiraEm() == null
                || u.getTokenRecuperacaoExpiraEm().isBefore(LocalDateTime.now())) {
            // Invalida o token mesmo expirado para nao poluir o banco
            u.setTokenRecuperacao(null);
            u.setTokenRecuperacaoExpiraEm(null);
            users.save(u);
            throw new ResponseStatusException(HttpStatus.GONE, "Token expirado");
        }

        u.setSenhaHash(enc.encode(novaSenha));
        u.setTokenRecuperacao(null);
        u.setTokenRecuperacaoExpiraEm(null);
        // Recuperar a senha tambem confirma o email (provou que e dono da caixa)
        u.setEmailConfirmado(true);
        users.save(u);
    }
}
