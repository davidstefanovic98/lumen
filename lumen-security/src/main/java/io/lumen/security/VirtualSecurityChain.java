package io.lumen.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.util.List;

class VirtualSecurityChain implements FilterChain {
    private final FilterChain originalChain;
    private final List<SecuritySubFilter> additionalFilters;
    private int currentPosition = 0;

    VirtualSecurityChain(FilterChain originalChain, List<SecuritySubFilter> additionalFilters) {
        this.originalChain = originalChain;
        this.additionalFilters = additionalFilters;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
        if (currentPosition == additionalFilters.size()) {
            originalChain.doFilter(request, response);
        } else {
            currentPosition++;
            SecuritySubFilter filter = additionalFilters.get(currentPosition - 1);
            filter.doFilter(request, response, this);
        }
    }
}
