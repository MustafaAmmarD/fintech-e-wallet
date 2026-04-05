package com.fintech.ewallet.shared.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.ewallet.shared.exception.ApiErrorResponse;
import com.fintech.ewallet.shared.exception.DomainException;
import com.fintech.ewallet.shared.exception.IdempotencyKeyConflictException;
import com.fintech.ewallet.shared.exception.IdempotencyKeyRequiredException;
import com.fintech.ewallet.shared.exception.IdempotencyRequestInProgressException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final Duration RESPONSE_TTL = Duration.ofHours(24);
    private static final Duration LOCK_TTL = Duration.ofMinutes(2);

    private static final Set<String> PROTECTED_ENDPOINTS = Set.of(
            "/api/v1/transfers/execute",
            "/api/v1/wallets/transfer",
            "/api/v1/exchange/execute",
            "/api/v1/deposits/agent",
            "/api/v1/withdrawals/agent",
            "/api/v1/bills/execute");

    private final IdempotencyStore idempotencyStore;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!isProtectedRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userScope = resolveUserScope();
        if (userScope == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            writeDomainExceptionResponse(response, request, new IdempotencyKeyRequiredException());
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String requestHash = computeRequestHash(cachedRequest);
        String deduplicationKey = buildDeduplicationKey(userScope, cachedRequest, idempotencyKey.trim());

        Optional<IdempotencyStoredResponse> cachedResponse = Optional.empty();
        try {
            cachedResponse = idempotencyStore.findResponse(deduplicationKey);
        } catch (Exception ex) {
            log.error("Idempotency store error (findResponse) for key={} path={}: {}", 
                idempotencyKey, cachedRequest.getRequestURI(), ex.getMessage());
            // Proceed without idempotency if store is down
        }

        if (cachedResponse.isPresent()) {
            IdempotencyStoredResponse storedResponse = cachedResponse.get();
            if (!storedResponse.requestHash().equals(requestHash)) {
                writeDomainExceptionResponse(response, request, new IdempotencyKeyConflictException());
                return;
            }

            writeCachedResponse(response, storedResponse);
            log.debug("Idempotency replay key={} path={}", idempotencyKey, cachedRequest.getRequestURI());
            return;
        }

        boolean lockAcquired = false;
        try {
            lockAcquired = idempotencyStore.tryAcquireLock(deduplicationKey, LOCK_TTL);
        } catch (Exception ex) {
            log.error("Idempotency store error (tryAcquireLock) for key={} path={}: {}", 
                idempotencyKey, cachedRequest.getRequestURI(), ex.getMessage());
            // Assume lock acquired to proceed if store is down (best effort)
            lockAcquired = true;
        }

        if (!lockAcquired) {
            writeDomainExceptionResponse(response, request, new IdempotencyRequestInProgressException());
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachedRequest, responseWrapper);

            if (isSuccessStatus(responseWrapper.getStatus())) {
                try {
                    idempotencyStore.saveResponse(
                            deduplicationKey,
                            new IdempotencyStoredResponse(
                                    responseWrapper.getStatus(),
                                    responseWrapper.getContentType(),
                                    responseWrapper.getContentAsByteArray(),
                                    requestHash),
                            RESPONSE_TTL);
                } catch (RuntimeException ex) {
                    log.error("Failed to cache idempotent response for key={} path={}",
                            idempotencyKey, cachedRequest.getRequestURI(), ex);
                }
            }

            responseWrapper.copyBodyToResponse();
        } finally {
            try {
                idempotencyStore.releaseLock(deduplicationKey);
            } catch (RuntimeException ex) {
                log.warn("Failed to release idempotency lock for key={} path={}",
                        idempotencyKey, cachedRequest.getRequestURI(), ex);
            }
        }
    }

    private boolean isProtectedRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && PROTECTED_ENDPOINTS.contains(request.getRequestURI());
    }

    private String resolveUserScope() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            return null;
        }

        return principal.toString();
    }

    private String buildDeduplicationKey(String userScope, HttpServletRequest request, String idempotencyKey) {
        return userScope + ":" + request.getMethod() + ":" + request.getRequestURI() + ":" + idempotencyKey;
    }

    private String computeRequestHash(CachedBodyHttpServletRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(request.getMethod().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(request.getRequestURI().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(request.getQueryString() == null
                    ? new byte[0]
                    : request.getQueryString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(request.getCachedBody());
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte currentByte : bytes) {
            builder.append(String.format("%02x", currentByte));
        }
        return builder.toString();
    }

    private void writeCachedResponse(HttpServletResponse response, IdempotencyStoredResponse cachedResponse)
            throws IOException {
        response.setStatus(cachedResponse.statusCode());
        if (cachedResponse.contentType() != null) {
            response.setContentType(cachedResponse.contentType());
        }
        response.setHeader("X-Idempotent-Replay", "true");
        response.getOutputStream().write(cachedResponse.responseBody());
    }

    private boolean isSuccessStatus(int status) {
        return status >= 200 && status < 300;
    }

    private void writeDomainExceptionResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            DomainException exception) throws IOException {

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpServletResponse.SC_BAD_REQUEST)
                .error("Bad Request")
                .code(exception.getErrorCode())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
