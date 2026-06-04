package io.lumen.security.context;

import io.lumen.security.authentication.Authentication;
import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextTaskDecoratorTest {

    private final SecurityContextTaskDecorator decorator = new SecurityContextTaskDecorator();

    @AfterEach
    void clear() {
        SecurityContextHolder.clear();
    }

    private Authentication authenticate(String user, String... roles) {
        var authorities = List.of(roles).stream()
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    @Test
    void propagatesAuthenticationToAnotherThread() throws Exception {
        Authentication caller = authenticate("alice", "ADMIN");

        AtomicReference<Authentication> seen = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Runnable task = decorator.decorate(() -> {
            seen.set(SecurityContextHolder.getContext().getAuthentication());
            done.countDown();
        });

        // Run on a separate thread that has no SecurityContext of its own.
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(task);
            assertTrue(done.await(2, TimeUnit.SECONDS), "task did not run");
        } finally {
            pool.shutdownNow();
        }

        assertNotNull(seen.get(), "authentication should be visible on the worker thread");
        assertEquals("alice", seen.get().getName());
        assertSame(caller, seen.get(), "the captured authentication should be propagated");
    }

    @Test
    void capturesAtDecorateTime_notAtRunTime() throws Exception {
        authenticate("alice", "USER");

        AtomicReference<Authentication> seen = new AtomicReference<>();
        // Capture happens now (alice is current).
        Runnable task = decorator.decorate(
                () -> seen.set(SecurityContextHolder.getContext().getAuthentication()));

        // Replace the calling thread's context after decorating.
        authenticate("bob", "ADMIN");

        CountDownLatch done = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> { task.run(); done.countDown(); });
            assertTrue(done.await(2, TimeUnit.SECONDS), "task did not run");
        } finally {
            pool.shutdownNow();
        }

        assertEquals("alice", seen.get().getName(),
                "decorator must capture the authentication present when decorate() was called");
    }

    @Test
    void clearsContextOnWorkerAfterRun_whenNoPriorContext() throws Exception {
        authenticate("bob", "USER");
        Runnable task = decorator.decorate(() -> {
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        });

        AtomicReference<Authentication> afterRun = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> {
                task.run();
                // After the decorated task finishes, the worker thread's context must be cleared.
                afterRun.set(SecurityContextHolder.getContext().getAuthentication());
                done.countDown();
            });
            assertTrue(done.await(2, TimeUnit.SECONDS), "task did not run");
        } finally {
            pool.shutdownNow();
        }

        assertNull(afterRun.get(), "worker thread context should be cleared after the task completes");
    }
}