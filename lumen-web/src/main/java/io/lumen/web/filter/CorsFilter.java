package io.lumen.web.filter;

import io.lumen.web.cors.CorsConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class CorsFilter implements LumenFilter {

    private final CorsConfiguration config;

    public CorsFilter(CorsConfiguration config) {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest  request  = (HttpServletRequest)  req;

        String allowedOrigin = resolveAllowedOrigin(request.getHeader("Origin"));
        if (allowedOrigin != null) {
            response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
            response.setHeader("Vary", "Origin");
            if (config.isAllowCredentials() && !"*".equals(allowedOrigin)) {
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }
            response.setHeader("Access-Control-Allow-Methods", String.join(", ", config.getAllowedMethods()));
            response.setHeader("Access-Control-Allow-Headers", String.join(", ", config.getAllowedHeaders()));
            response.setHeader("Access-Control-Max-Age", String.valueOf(config.getMaxAge()));

            if (!config.getExposedHeaders().isEmpty()) {
                response.setHeader("Access-Control-Expose-Headers",
                        String.join(", ", config.getExposedHeaders()));
            }
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    /**
     * Resolves the single Access-Control-Allow-Origin value for this request, or null if
     * there's no Origin header (not a CORS request) or the origin isn't allowed. The CORS spec
     * requires a single origin or "*", never a joined list of every configured origin.
     *
     * <p>A wildcard config only reflects back a literal "*" when credentials aren't allowed -
     * browsers reject "Access-Control-Allow-Origin: *" combined with
     * "Access-Control-Allow-Credentials: true", so when both are configured together the actual
     * request origin is echoed back instead, matching the wildcard's intent without violating
     * the spec.
     */
    private String resolveAllowedOrigin(String requestOrigin) {
        if (requestOrigin == null) {
            return null;
        }
        List<String> allowedOrigins = config.getAllowedOrigins();
        if (allowedOrigins.contains("*")) {
            return config.isAllowCredentials() ? requestOrigin : "*";
        }
        return allowedOrigins.contains(requestOrigin) ? requestOrigin : null;
    }

    @Override
    public int getOrder() { return -100; }
}