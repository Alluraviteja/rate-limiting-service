package com.app.ratelimiter.mcp.filter;

import com.app.ratelimiter.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpAuthFilter extends OncePerRequestFilter {

    private static final String MCP_SECRET_HEADER = "X-MCP-Secret";
    private static final String MCP_PATH_PREFIX = "/mcp";

    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(MCP_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        String provided = request.getHeader(MCP_SECRET_HEADER);
        String expected = appProperties.getMcp().getSecret();
        if (provided == null || !provided.equals(expected)) {
            log.warn("MCP auth rejected: missing or invalid secret from ip={}", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
