package io.lumen.security;

import io.lumen.context.annotation.Component;
import io.lumen.core.annotation.Order;
import io.lumen.web.filter.LumenFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;

@Component
@Order(100)
public class LumenSecurityFilter implements LumenFilter {

    private final List<SecurityFilterChain> filterChains;

    public LumenSecurityFilter(List<SecurityFilterChain> filterChains) {
        this.filterChains = filterChains;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        SecurityFilterChain matchedChain = filterChains.stream()
                .filter(c -> c.matches(httpRequest))
                .findFirst()
                .orElse(null);

        if (matchedChain == null) {
            chain.doFilter(request, response);
            return;
        }
        new VirtualSecurityChain(chain, matchedChain.getFilters()).doFilter(request, response);
    }
}
