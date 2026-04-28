package com.vitorcamprubi.sgtc.web.auth;

import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.UserRepository;
import com.vitorcamprubi.sgtc.security.AuthService;
import com.vitorcamprubi.sgtc.security.JwtService;
import com.vitorcamprubi.sgtc.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthService authService;
    private final UserAdminService userAdminService;
    private final UserRepository userRepository;

    @Value("${app.url.web:http://localhost:4200}")
    private String webUrl;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          AuthService authService, UserAdminService userAdminService,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authService = authService;
        this.userAdminService = userAdminService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        // Bloqueia login se o e-mail ainda nao foi confirmado
        User u = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (u != null && !u.isEmailConfirmado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "E-mail nao confirmado. Verifique sua caixa de entrada ou solicite o reenvio.");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
            );
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);
            return new LoginResponse(token);
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
        }
    }

    @GetMapping("/me")
    public UserInfoResponse me() {
        User user = authService.getCurrentUser();
        return new UserInfoResponse(user.getId(), user.getNome(), user.getEmail(), user.getRole(), user.getRa());
    }

    /**
     * Endpoint publico chamado pelo link enviado por e-mail.
     * Redireciona o usuario para a pagina de login do front-end com um
     * parametro indicando o resultado.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        String status;
        try {
            userAdminService.confirmarEmail(token);
            status = "ok";
        } catch (ResponseStatusException ex) {
            status = switch (ex.getStatusCode().value()) {
                case 410 -> "expirado";
                case 404 -> "invalido";
                default -> "erro";
            };
        } catch (RuntimeException ex) {
            status = "erro";
        }
        String destino = webUrl + "/login?verificacao=" +
                URLEncoder.encode(status, StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(destino))
                .build();
    }

    /**
     * Reenvia o e-mail de confirmacao para um endereco.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ResendVerificationRequest req) {
        try {
            userAdminService.reenviarConfirmacao(req.getEmail());
        } catch (ResponseStatusException ex) {
            // Por seguranca, nao revela se o email existe ou nao
        }
        return ResponseEntity.noContent().build();
    }

    public static class ResendVerificationRequest {
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
