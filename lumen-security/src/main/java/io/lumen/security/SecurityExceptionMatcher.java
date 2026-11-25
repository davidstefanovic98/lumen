package io.lumen.security;

import io.lumen.security.exception.AccessDeniedException;
import io.lumen.security.exception.AuthenticationException;
import io.lumen.web.exception.handle.FilterChainExceptionMatcher;

/**
 * Lets security exceptions that no {@code @ExceptionHandler} handled escape the DispatcherServlet
 * to {@link ExceptionTranslationFilter}, which turns them into 401/403/login-redirect responses.
 * Matches by type, so subclasses are covered too.
 */
public class SecurityExceptionMatcher implements FilterChainExceptionMatcher {

    @Override
    public boolean matches(Throwable exception) {
        return exception instanceof AccessDeniedException || exception instanceof AuthenticationException;
    }
}