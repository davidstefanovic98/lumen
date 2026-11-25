package io.lumen.web.filter;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class LoggingFilter implements LumenFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String START_ATTRIBUTE = LoggingFilter.class.getName() + ".START";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Kept on the request so an async request's duration spans every dispatch, not just the last one
        long start = request.getAttribute(START_ATTRIBUTE) instanceof Long first ? first : System.currentTimeMillis();
        request.setAttribute(START_ATTRIBUTE, start);
        try {
            chain.doFilter(request, response);
        } finally {
            // A CompletableFuture handler leaves the request in async mode here with no final
            // status yet; it is logged on the ASYNC re-dispatch that actually writes the response.
            if (!request.isAsyncStarted()) {
                long ms = System.currentTimeMillis() - start;
                logger.debug("{} {} → {} ({}ms)",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), ms);
            }
        }
    }

    @Override
    public int getOrder() {
        return -200;
    }
}