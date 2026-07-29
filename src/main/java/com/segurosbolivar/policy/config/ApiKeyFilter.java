package com.segurosbolivar.policy.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "x-api-key";

    private final byte[] expectedApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(
            @Value("${app.security.api-key}") String expectedApiKey,
            ObjectMapper objectMapper
    ) {
        this.expectedApiKey = expectedApiKey.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String supplied = request.getHeader(API_KEY_HEADER);
        boolean authenticated = supplied != null && MessageDigest.isEqual(
                expectedApiKey,
                supplied.getBytes(StandardCharsets.UTF_8)
        );

        if (!authenticated) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("type", "about:blank");
            body.put("title", "Unauthorized");
            body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            body.put("detail", "A valid x-api-key header is required");
            body.put("code", "INVALID_API_KEY");
            body.put("path", request.getRequestURI());
            body.put("timestamp", Instant.now());
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/error")
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/");
    }
}
