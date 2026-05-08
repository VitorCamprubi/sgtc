package com.vitorcamprubi.sgtc.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("LoginRateLimitFilter - Bucket4j em memoria por IP")
class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter(new ObjectMapper());
        chain = mock(FilterChain.class);
    }

    private static MockHttpServletRequest postLogin(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    @DisplayName("primeiras 5 tentativas passam e a 6a recebe 429")
    void seisTentativasUmEhBloqueada() throws Exception {
        String ip = "10.0.0.1";

        for (int i = 0; i < 5; i++) {
            HttpServletRequest req = postLogin(ip);
            HttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertThat(((MockHttpServletResponse) res).getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse res6 = new MockHttpServletResponse();
        filter.doFilter(postLogin(ip), res6, chain);

        assertThat(res6.getStatus()).isEqualTo(429);
        assertThat(res6.getHeader("Retry-After")).isNotBlank();
        assertThat(res6.getContentAsString()).contains("Muitas tentativas");

        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("IPs diferentes nao compartilham bucket")
    void ipsDiferentesIndependentes() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilter(postLogin("10.0.0.1"), new MockHttpServletResponse(), chain);
        }

        // 5 vezes esgotaram o IP 1; IP 2 ainda tem 5 disponiveis
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(postLogin("10.0.0.2"), res, chain);
        assertThat(res.getStatus()).isEqualTo(200);
        verify(chain, atLeastOnce()).doFilter(any(), any());
    }

    @Test
    @DisplayName("filter ignora outros endpoints e metodos")
    void naoLimitaOutrosEndpoints() throws Exception {
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/grupos/me");
        get.setRemoteAddr("10.0.0.99");

        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(get, res, chain);
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("respeita X-Forwarded-For (cenario com reverse proxy)")
    void respeitaXForwardedFor() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = postLogin("10.0.0.250"); // IP do proxy
            req.addHeader("X-Forwarded-For", "203.0.113.42");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        // 6a tentativa do mesmo IP real (atras do proxy) eh bloqueada
        MockHttpServletRequest req = postLogin("10.0.0.250");
        req.addHeader("X-Forwarded-For", "203.0.113.42");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(429);
    }
}
