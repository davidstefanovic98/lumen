package io.lumen.it;

import io.lumen.cache.CacheProcessor;
import io.lumen.cache.SimpleCacheManager;
import io.lumen.cache.annotation.Cacheable;
import io.lumen.security.annotation.PreAuthorize;
import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.authority.SimpleGrantedAuthority;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import io.lumen.security.method.MethodSecurityProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the interaction between @Cacheable and @PreAuthorize on the same method.
 *
 * The proxy wrapping order is:
 *   CacheProcessor (order -1) runs first  → CacheProxy(real)
 *   SecurityProcessor (order 0) runs second → SecurityProxy(CacheProxy(real))
 *
 * Callers receive SecurityProxy (outermost), so security is enforced on every call —
 * including cache hits. These tests guard against regressions where the ordering is
 * reversed, causing cache hits to bypass @PreAuthorize entirely.
 */
class CacheSecurityProxyOrderTest {

    interface TaskService {
        String findById(Long id);
    }

    static class TaskServiceImpl implements TaskService {
        int callCount = 0;

        @PreAuthorize("hasRole('ADMIN')")
        @Cacheable(value = "tasks", key = "#id")
        @Override
        public String findById(Long id) {
            callCount++;
            return "task-" + id;
        }
    }

    private SimpleCacheManager cacheManager;
    private TaskServiceImpl delegate;
    private TaskService proxy;

    @BeforeEach
    void setUp() {
        cacheManager = new SimpleCacheManager();
        delegate = new TaskServiceImpl();

        // Apply processors in the correct order: cache first (inner), security second (outer).
        // This mirrors DefaultLightCreator with lumen.cache.proxy-order=-1 and
        // lumen.security.method.proxy-order=0.
        CacheProcessor cacheProcessor = new CacheProcessor(cacheManager);
        MethodSecurityProcessor securityProcessor = new MethodSecurityProcessor();

        Object afterCache    = cacheProcessor.afterInstantiation(null, delegate);
        Object afterSecurity = securityProcessor.afterInstantiation(null, afterCache);

        proxy = (TaskService) afterSecurity;
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    // --- security enforcement ---

    @Test
    void unauthenticated_cacheMiss_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> proxy.findById(1L));
        assertEquals(0, delegate.callCount);
    }

    @Test
    void authenticated_nonAdmin_cacheMiss_throwsAccessDenied() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class, () -> proxy.findById(1L));
        assertEquals(0, delegate.callCount);
    }

    @Test
    void authenticated_admin_cacheMiss_returnsResult() {
        authenticate("ADMIN");
        assertEquals("task-1", proxy.findById(1L));
        assertEquals(1, delegate.callCount);
    }

    // --- cache hit must not bypass security ---

    @Test
    void authenticated_admin_cacheHit_skipsDelegate() {
        authenticate("ADMIN");
        proxy.findById(1L); // miss
        proxy.findById(1L); // hit — delegate not called again
        assertEquals(1, delegate.callCount);
    }

    @Test
    void unauthenticated_cacheHit_stillThrowsAccessDenied() {
        // Warm the cache as admin
        authenticate("ADMIN");
        proxy.findById(1L);
        assertEquals(1, delegate.callCount);

        // Now clear auth and call again — should be rejected even though the value is cached
        SecurityContextHolder.clear();
        assertThrows(AccessDeniedException.class, () -> proxy.findById(1L));
        assertEquals(1, delegate.callCount); // delegate was NOT called again
    }

    @Test
    void nonAdmin_cacheHit_stillThrowsAccessDenied() {
        // Warm the cache as admin
        authenticate("ADMIN");
        proxy.findById(2L);
        assertEquals(1, delegate.callCount);

        // Switch to a non-admin caller — must be rejected even though the value is cached
        SecurityContextHolder.clear();
        authenticate("USER");
        assertThrows(AccessDeniedException.class, () -> proxy.findById(2L));
        assertEquals(1, delegate.callCount);
    }

    // --- reversed order: cache outer bypasses security on cache hits ---

    @Test
    void reversedOrder_cacheHit_bypassesSecurity() {
        // Security inner (order -1), cache outer (order 0).
        // Call chain: CacheProxy → SecurityProxy → real
        // Cache hit returns before SecurityProxy is ever reached — @PreAuthorize is skipped.
        // This is why lumen.cache.proxy-order must be lower than
        // lumen.security.method.proxy-order when both annotations coexist on a method.
        MethodSecurityProcessor securityProcessor = new MethodSecurityProcessor();
        CacheProcessor cacheProcessor = new CacheProcessor(new SimpleCacheManager());
        TaskServiceImpl localDelegate = new TaskServiceImpl();

        Object afterSecurity = securityProcessor.afterInstantiation(null, localDelegate);
        Object afterCache    = cacheProcessor.afterInstantiation(null, afterSecurity);
        TaskService badProxy = (TaskService) afterCache;

        // Warm the cache as admin
        authenticate("ADMIN");
        assertEquals("task-99", badProxy.findById(99L));
        assertEquals(1, localDelegate.callCount);

        // Now call without any auth — cache hit returns before security runs
        SecurityContextHolder.clear();
        assertEquals("task-99", badProxy.findById(99L));
        assertEquals(1, localDelegate.callCount); // delegate not called again — it was a cache hit
    }

    // --- DefaultLightCreator sort order regression ---

    @Test
    void defaultLightCreator_sortsProcessorsByOrder() {
        var creator = new io.lumen.core.component.DefaultLightCreator(null);
        var log = new java.util.ArrayList<String>();

        creator.addPostProcessor(loggingProcessor(log, "order0"),    0);
        creator.addPostProcessor(loggingProcessor(log, "orderNeg1"), -1);
        creator.addPostProcessor(loggingProcessor(log, "orderMax"));

        for (var p : creator.getPostProcessors()) p.afterInstantiation(null, new Object());

        assertEquals(List.of("orderNeg1", "order0", "orderMax"), log);
    }

    @Test
    void defaultLightCreator_insertionOrderTiebreak() {
        var creator = new io.lumen.core.component.DefaultLightCreator(null);
        var log = new java.util.ArrayList<String>();

        creator.addPostProcessor(loggingProcessor(log, "first"),  5);
        creator.addPostProcessor(loggingProcessor(log, "second"), 5);
        creator.addPostProcessor(loggingProcessor(log, "third"),  5);

        for (var p : creator.getPostProcessors()) p.afterInstantiation(null, new Object());

        assertEquals(List.of("first", "second", "third"), log);
    }

    private static io.lumen.core.component.processor.LightProcessor loggingProcessor(
            java.util.List<String> log, String label) {
        return new io.lumen.core.component.processor.LightProcessor() {
            @Override
            public Object afterInstantiation(io.lumen.core.component.LightInstance light, Object instance) {
                log.add(label);
                return instance;
            }
        };
    }

    // --- helpers ---

    private void authenticate(String... roles) {
        var authorities = List.of(roles).stream()
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, authorities));
    }
}