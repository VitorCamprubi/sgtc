package com.vitorcamprubi.sgtc.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService - fail-fast e geracao de token")
class JwtServiceTest {

    private static final String DEV_DEFAULT = "sgtc-dev-jwt-secret-key-32-bytes-minimum";
    private static final String SECRET_FORTE =
            "test-only-jwt-secret-with-more-than-32-bytes-of-length-XYZ";

    @Test
    @DisplayName("perfil prod com secret de dev rejeita o boot")
    void prodComSecretDevFalha() {
        MockEnvironment env = new MockEnvironment().withProperty("foo", "bar");
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtService(DEV_DEFAULT, 60, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("desenvolvimento");
    }

    @Test
    @DisplayName("perfil dev aceita o secret default")
    void devComSecretDevPassa() {
        MockEnvironment env = new MockEnvironment();
        // sem profile ativo

        JwtService svc = new JwtService(DEV_DEFAULT, 60, env);
        assertThat(svc).isNotNull();
    }

    @Test
    @DisplayName("secret vazio rejeita boot em qualquer perfil")
    void secretVazioFalha() {
        MockEnvironment env = new MockEnvironment();
        assertThatThrownBy(() -> new JwtService("", 60, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nao configurado");
        assertThatThrownBy(() -> new JwtService(null, 60, env))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("secret menor que 32 bytes rejeita boot")
    void secretCurtoFalha() {
        MockEnvironment env = new MockEnvironment();
        assertThatThrownBy(() -> new JwtService("curto-demais", 60, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("perfil prod com secret forte passa")
    void prodComSecretForte() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtService svc = new JwtService(SECRET_FORTE, 60, env);
        assertThat(svc).isNotNull();
    }
}
