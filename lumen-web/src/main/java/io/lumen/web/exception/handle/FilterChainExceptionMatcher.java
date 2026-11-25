package io.lumen.web.exception.handle;

/**
 * SPI for exceptions that a servlet filter - not the DispatcherServlet - turns into a response.
 *
 * <p>When a handler throws and no {@code @ExceptionHandler} handles the exception, the exception
 * resolver chain asks every matcher about each throwable in the exception's cause chain. On a
 * match, the original exception is rethrown out of the DispatcherServlet instead of being written
 * as an error response, so it reaches the filter that owns it (lumen-security's
 * {@code SecurityExceptionMatcher} lets {@code AccessDeniedException}/{@code AuthenticationException}
 * reach {@code ExceptionTranslationFilter}). This keeps lumen-web free of any knowledge of
 * which modules translate which exceptions.
 *
 * <p>Register an implementation as a light, keyed by its own concrete class
 * ({@code container.registerExternalInstance(MyMatcher.class, new MyMatcher())}) so several can
 * coexist. A filter that translates the matched exception must actually be in the chain -
 * otherwise the servlet container's own error handling produces the response.
 */
@FunctionalInterface
public interface FilterChainExceptionMatcher {

    boolean matches(Throwable exception);
}