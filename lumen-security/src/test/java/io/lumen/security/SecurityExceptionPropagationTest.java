package io.lumen.security;

import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import io.lumen.security.exception.BadCredentialsException;
import io.lumen.web.annotation.ExceptionHandler;
import io.lumen.web.http.ResponseEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security exceptions escaping a controller: a matching {@code @ExceptionHandler} gets the first
 * chance (Spring MVC order); only an unhandled one escapes the DispatcherServlet to the security
 * filter chain's ExceptionTranslationFilter.
 */
class SecurityExceptionPropagationTest {

    static class ProjectAccessDeniedException extends AccessDeniedException {
        ProjectAccessDeniedException() { super("not a project member"); }
    }

    public static class SecuredController {
        public String denied() {
            throw new AccessDeniedException("nope");
        }

        public String deniedSubclass() {
            throw new ProjectAccessDeniedException();
        }

        public String deniedWrapped() {
            throw new IllegalStateException("wrapper", new AccessDeniedException("nope"));
        }

        public String badCredentials() {
            throw new BadCredentialsException("bad password");
        }

        public CompletableFuture<String> badCredentialsAsync() {
            return CompletableFuture.supplyAsync(() -> { throw new BadCredentialsException("bad password"); });
        }
    }

    @AfterAll
    static void clearContext() {
        SecurityContextHolder.clear();
    }

    @Nested
    class WithoutExceptionHandler {

        static EmbeddedLumenServer server;

        @BeforeAll
        static void startServer() throws Exception {
            server = EmbeddedLumenServer.start(new SecuredController(), "/secured");
        }

        @AfterAll
        static void stopServer() throws Exception {
            server.close();
        }

        @Test
        void unhandledAccessDenied_isTranslatedByFilterChainTo403() throws Exception {
            HttpResponse<String> response = server.get("/secured/denied");

            assertEquals(403, response.statusCode());
            assertTrue(response.body().contains("Access denied"), response.body());
        }

        @Test
        void unhandledAccessDeniedSubclass_isTranslatedTo403_notA500() throws Exception {
            // Used to be a 500: the old check compared the exact class name, missing subclasses
            assertEquals(403, server.get("/secured/deniedSubclass").statusCode());
        }

        @Test
        void accessDeniedAnywhereInCauseChain_isStillTranslated() throws Exception {
            assertEquals(403, server.get("/secured/deniedWrapped").statusCode());
        }

        @Test
        void unhandledAuthenticationException_isTranslatedTo401_notA500() throws Exception {
            assertEquals(401, server.get("/secured/badCredentials").statusCode());
        }

        @Test
        void unhandledAuthenticationExceptionFromFuture_isTranslatedTo401() throws Exception {
            assertEquals(401, server.get("/secured/badCredentialsAsync").statusCode());
        }
    }

    @Nested
    class WithExceptionHandlerForAccessDenied {

        public static class Advice {
            @ExceptionHandler(AccessDeniedException.class)
            public ResponseEntity<String> handle(AccessDeniedException ex) {
                return ResponseEntity.status(403).body("handled-by-advice");
            }
        }

        static EmbeddedLumenServer server;

        @BeforeAll
        static void startServer() throws Exception {
            server = EmbeddedLumenServer.start(new SecuredController(), "/secured", new Advice());
        }

        @AfterAll
        static void stopServer() throws Exception {
            server.close();
        }

        @Test
        void accessDenied_isHandledByTheAdviceNotTheFilterChain() throws Exception {
            HttpResponse<String> response = server.get("/secured/denied");

            // Used to be unreachable: DispatcherServlet rethrew AccessDeniedException before any
            // @ControllerAdvice was consulted.
            assertEquals(403, response.statusCode());
            assertEquals("handled-by-advice", response.body());
        }

        @Test
        void authenticationExceptionWithoutMatchingHandler_stillEscapesToFilterChain() throws Exception {
            assertEquals(401, server.get("/secured/badCredentials").statusCode());
        }
    }
}