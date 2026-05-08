package com.vitorcamprubi.sgtc.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitorcamprubi.sgtc.web.error.ApiError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter para o endpoint POST /api/auth/login.
 *
 * Limite atual: 5 tentativas por minuto por IP. Quando excedido, devolve
 * 429 Too Many Requests com header Retry-After. Usa Bucket4j em memoria,
 * o que e suficiente para uma instancia unica. Se um dia escalar para
 * varias instancias atras de um load balancer, trocar para o backend
 * distribuido (Redis/Hazelcast) que o Bucket4j ja suporta.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    /** Numero maximo de requests por janela. */
    private static final long CAPACITY = 5;
    /** Duracao da janela em que o limite eh contado. */
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public LoginRateLimitFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/login".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> novoBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        log.warn("[rate-limit] Login bloqueado para IP={} retryAfter={}s", key, retryAfterSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ApiError body = new ApiError(
                "TOO_MANY_REQUESTS",
                "Muitas tentativas de login. Tente novamente em alguns minutos.",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                null
        );
        mapper.writeValue(response.getOutputStream(), body);
    }

    private static Bucket novoBucket() {
        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.greedy(CAPACITY, WINDOW));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Extrai IP do cliente respeitando X-Forwarded-For quando presente
     * (cenario com reverse proxy / Caddy).
     */
    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return req.getRemoteAddr();
    }

    // Disponivel para testes
    Map<String, Bucket> snapshot() {
        return Map.copyOf(buckets);
    }
}
