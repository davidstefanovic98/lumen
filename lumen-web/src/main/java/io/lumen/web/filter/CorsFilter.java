package io.lumen.web.filter;

import io.lumen.web.cors.CorsConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.stream.Collectors;

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

        String origins = String.join(", ", config.getAllowedOrigins());
        String methods = String.join(", ", config.getAllowedMethods());
        String headers = String.join(", ", config.getAllowedHeaders());

        response.setHeader("Access-Control-Allow-Origin",      origins);
        response.setHeader("Access-Control-Allow-Methods",     methods);
        response.setHeader("Access-Control-Allow-Headers",     headers);
        response.setHeader("Access-Control-Allow-Credentials", String.valueOf(config.isAllowCredentials()));
        response.setHeader("Access-Control-Max-Age",           String.valueOf(config.getMaxAge()));

        if (!config.getExposedHeaders().isEmpty()) {
            response.setHeader("Access-Control-Expose-Headers",
                    String.join(", ", config.getExposedHeaders()));
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public int getOrder() { return -100; }
}