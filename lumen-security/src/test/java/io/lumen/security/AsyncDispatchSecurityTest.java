package io.lumen.security;

import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real embedded-Tomcat round trip for controllers returning {@link CompletableFuture}: the
 * future's outcome must go through the same response and exception path as a synchronous
 * return value, including the security filter chain's exception translation.
 */
class AsyncDispatchSecurityTest {

    public static class AsyncController {
        static final AtomicInteger invocations = new AtomicInteger();

        public CompletableFuture<String> ok() {
            invocations.incrementAndGet();
            return CompletableFuture.supplyAsync(() -> "hello");
        }

        public CompletableFuture<String> denied() {
            invocations.incrementAndGet();
            return CompletableFuture.supplyAsync(() -> { throw new AccessDeniedException("nope"); });
        }

        public CompletableFuture<String> boom() {
            invocations.incrementAndGet();
            return CompletableFuture.supplyAsync(() -> { throw new IllegalStateException("async-boom"); });
        }
    }

    private static EmbeddedLumenServer server;

    @BeforeAll
    static void startServer() throws Exception {
        server = EmbeddedLumenServer.start(new AsyncController(), "/async");
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.close();
        SecurityContextHolder.clear();
    }

    @Test
    void completedFuture_writesValueThroughNormalResultHandling() throws Exception {
        int before = AsyncController.invocations.get();

        HttpResponse<String> response = server.get("/async/ok");

        assertEquals(200, response.statusCode());
        assertEquals("hello", response.body());
        // The ASYNC re-dispatch must pick up the stored outcome, not re-invoke the controller
        assertEquals(before + 1, AsyncController.invocations.get());
    }

    @Test
    void futureFailingWithAccessDenied_isTranslatedBySecurityChain_notA500() throws Exception {
        HttpResponse<String> response = server.get("/async/denied");

        assertEquals(403, response.statusCode());
    }

    @Test
    void futureFailingWithOtherException_goesThroughExceptionResolvers() throws Exception {
        HttpResponse<String> response = server.get("/async/boom");

        assertEquals(500, response.statusCode());
        assertEquals("{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"async-boom\"}", response.body());
    }
}