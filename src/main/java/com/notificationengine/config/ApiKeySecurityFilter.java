package com.notificationengine.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Production security filter for API key and Bearer token validation.
 * When enabled (via {@code app.security.api-key-enabled=true}), enforces authentication
 * on protected {@code /api/v1/**} endpoints while allowing public actuator health checks,
 * OpenAPI documentation, and static frontend assets.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeySecurityFilter extends OncePerRequestFilter {

    @Value("${app.security.api-key-enabled:false}")
    private boolean securityEnabled;

    @Value("${app.security.api-key:}")
    private String configuredApiKey;

    @Value("${app.security.jwt-secret:}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!securityEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        String method = request.getMethod();

        // 1. Allow preflight OPTIONS requests for CORS
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Allow public endpoints
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Validate API key or Bearer token for protected API endpoints
        String apiKeyHeader = request.getHeader("X-API-Key");
        String authHeader = request.getHeader("Authorization");

        boolean authorized = false;

        if (StringUtils.isNotBlank(configuredApiKey) && StringUtils.equals(apiKeyHeader, configuredApiKey)) {
            authorized = true;
        } else if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (StringUtils.isNotBlank(token)) {
                if (token.equals(configuredApiKey)) {
                    authorized = true;
                } else if (validateJwt(token)) {
                    authorized = true;
                }
            }
        }

        if (authorized) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("[SECURITY] Unauthorized access attempt: method={}, path={}, ip={}",
                    method, path, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {"status":401,"error":"Unauthorized","errorCode":"AUTH-401","message":"Valid API Key or Bearer token is required."}
                """);
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.equals("/")
                || path.equals("/index.html")
                || path.startsWith("/assets/")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".ico")
                || path.endsWith(".svg")
                || path.endsWith(".png");
    }

    private boolean validateJwt(String token) {
        if (StringUtils.isBlank(token) || StringUtils.isBlank(jwtSecret)) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        try {
            // Verify HMAC SHA-256 signature
            String content = parts[0] + "." + parts[1];
            javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] signatureBytes = hmac.doFinal(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String expectedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            if (!java.security.MessageDigest.isEqual(
                    expectedSignature.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    parts[2].getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                log.warn("[SECURITY] JWT signature validation failed for token");
                return false;
            }

            // Verify expiration claim (exp)
            byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
            com.fasterxml.jackson.databind.JsonNode jsonNode = 
                    com.notificationengine.util.JsonUtil.getObjectMapper().readTree(payloadBytes);
            if (jsonNode.has("exp")) {
                long exp = jsonNode.get("exp").asLong();
                long now = System.currentTimeMillis() / 1000L;
                if (now > exp) {
                    log.warn("[SECURITY] JWT token has expired: exp={}, now={}", exp, now);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("[SECURITY] JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}
