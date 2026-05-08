package com.vitorcamprubi.sgtc.security;

import com.vitorcamprubi.sgtc.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

@Service
public class JwtService {
    /**
     * Valor de desenvolvimento que NUNCA deve ser usado em producao.
     * Mantido em uma constante para o fail-fast detectar com facilidade.
     */
    private static final String DEV_DEFAULT_SECRET = "sgtc-dev-jwt-secret-key-32-bytes-minimum";

    /** Tamanho minimo do segredo em bytes (HS256 exige 256 bits = 32 bytes). */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes,
            Environment environment
    ) {
        validarSegredo(secret, environment);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    private static void validarSegredo(String secret, Environment environment) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET nao configurado. Defina a variavel de ambiente JWT_SECRET com pelo menos "
                            + MIN_SECRET_BYTES + " bytes (use openssl rand -base64 64).");
        }

        boolean prod = isProdProfile(environment);
        if (prod && DEV_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET esta usando o valor de desenvolvimento em producao. "
                            + "Defina a variavel de ambiente JWT_SECRET com um valor aleatorio forte "
                            + "(ex: openssl rand -base64 64) antes de subir a aplicacao.");
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET tem menos de " + MIN_SECRET_BYTES
                            + " bytes; HS256 exige pelo menos 256 bits.");
        }
    }

    private static boolean isProdProfile(Environment environment) {
        if (environment == null) {
            return false;
        }
        String[] active = environment.getActiveProfiles();
        return Arrays.stream(active).anyMatch(p -> p.equalsIgnoreCase("prod"));
    }

    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .map(a -> a.replace("ROLE_", ""))
                .orElse(null);
        return buildToken(userDetails.getUsername(), role);
    }

    public String generateToken(User user) {
        String role = user.getRole() != null ? user.getRole().name() : null;
        return buildToken(user.getEmail(), role);
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractEmail(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    private String buildToken(String subject, String role) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)));

        if (role != null) {
            builder.claim("role", role);
        }

        return builder
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
