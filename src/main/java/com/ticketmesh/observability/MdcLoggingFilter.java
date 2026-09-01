package com.ticketmesh.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC (Mapped Diagnostic Context) filter for distributed tracing and structured
 * logging. Spec §55 — every log line includes traceId, spanId, userId and
 * tenantId when available. The traceId is also exposed to the client via the
 * {@code X-Trace-Id} response header so external tools can correlate.
 *
 * <p>Order: highest priority so MDC is set before any other filter or controller
 * runs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_REQUEST_PATH = "requestPath";
    public static final String MDC_REQUEST_METHOD = "requestMethod";
    public static final String MDC_CLIENT_IP = "clientIp";

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_SPAN_ID = "X-Span-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Reuse incoming trace id if present (so upstream services can correlate)
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }
        String spanId = generateSpanId();

        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_SPAN_ID, spanId);
        MDC.put(MDC_REQUEST_PATH, request.getRequestURI());
        MDC.put(MDC_REQUEST_METHOD, request.getMethod());
        String clientIp = extractClientIp(request);
        if (clientIp != null) {
            MDC.put(MDC_CLIENT_IP, clientIp);
        }

        response.setHeader(HEADER_TRACE_ID, traceId);
        response.setHeader(HEADER_SPAN_ID, spanId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String generateSpanId() {
        // 16 hex chars (64 bits) drawn from UUID for guaranteed length
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // First IP in the comma-separated list
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Set the user id and tenant id in MDC after authentication. Called from
     * {@link com.ticketmesh.security.JwtAuthenticationFilter} once the principal
     * is known.
     */
    public static void setAuthenticated(String userId, String tenantId) {
        if (userId != null) MDC.put(MDC_USER_ID, userId);
        if (tenantId != null) MDC.put(MDC_TENANT_ID, tenantId);
    }
}
