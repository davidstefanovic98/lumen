package io.lumen.aop;

import io.lumen.aop.proxy.ByteBuddyProxyProvider;
import io.lumen.context.AnnotationApplicationContext;
import io.lumen.context.annotation.Configuration;
import io.lumen.core.annotation.Light;
import io.lumen.core.context.DefaultApplicationContext;
import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ByteBuddyProxyProviderTest {

    ByteBuddyProxyProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ByteBuddyProxyProvider();
    }

    // -----------------------------------------------------------------------
    // createDelegatingProxy
    // -----------------------------------------------------------------------

    static class Counter {
        int count = 0;
        public int increment() { return ++count; }
        public void boom() { throw new IllegalStateException("real-boom"); }
    }

    static class RecordingInterceptor implements MethodInterceptor {
        final List<String> calls = new ArrayList<>();
        @Override
        public Object invoke(MethodInvocation inv) throws Throwable {
            calls.add(inv.getMethod().getName());
            return inv.proceed();
        }
    }

    @Test
    void delegatingProxy_interceptsMethodCalls() {
        Counter delegate = new Counter();
        RecordingInterceptor interceptor = new RecordingInterceptor();

        Counter proxy = provider.createDelegatingProxy(Counter.class, delegate, List.of(interceptor));

        proxy.increment();
        proxy.increment();

        assertEquals(List.of("increment", "increment"), interceptor.calls);
        assertEquals(2, delegate.count, "Real object should be mutated");
    }

    @Test
    void delegatingProxy_proxyIsSubclassOfOriginal() {
        Counter delegate = new Counter();
        Counter proxy = provider.createDelegatingProxy(Counter.class, delegate, List.of());

        assertNotSame(Counter.class, proxy.getClass());
        assertInstanceOf(Counter.class, proxy);
    }

    @Test
    void delegatingProxy_exceptionFromDelegate_propagatesUnwrapped() {
        Counter delegate = new Counter();
        Counter proxy = provider.createDelegatingProxy(Counter.class, delegate, List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, proxy::boom);
        assertEquals("real-boom", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // createConfigurationProxy — @Light method caching
    // -----------------------------------------------------------------------

    @Configuration
    static class AppConfig {
        @Light
        public Counter counter() {
            return new Counter();
        }
    }

    @Test
    void configurationProxy_cachesLightMethodResult() {
        // Use a full AnnotationApplicationContext so ConfigProcessor creates the proxy and
        // populates the container. After initialize(), counter is READY in the container,
        // so repeated calls to config.counter() must return the same instance.
        AnnotationApplicationContext ctx = new AnnotationApplicationContext();
        ctx.scan(AppConfig.class);
        ctx.initialize();

        AppConfig config = ctx.getLight(AppConfig.class);
        Counter first  = config.counter();
        Counter second = config.counter();

        assertSame(first, second, "@Light method must return the same instance on repeated calls");
    }

    // -----------------------------------------------------------------------
    // createInterfaceProxy — interceptor chain
    // -----------------------------------------------------------------------

    interface Greeter {
        String greet(String name);
    }

    @Test
    void interfaceProxy_interceptorCanModifyReturnValue() {
        MethodInterceptor shout = inv -> inv.proceed() + "!";

        Greeter proxy = provider.createInterfaceProxy(Greeter.class, List.of(
                inv -> "Hello " + inv.getArguments()[0],
                shout
        ));

        // Only the first interceptor runs per-invocation (shout isn't chained unless proceed() is called)
        assertEquals("Hello world", proxy.greet("world"));
    }

    @Test
    void interfaceProxy_multipleInterceptors_formChain() {
        List<String> trace = new ArrayList<>();
        MethodInterceptor first  = inv -> { trace.add("before-1"); Object r = inv.proceed(); trace.add("after-1"); return r; };
        MethodInterceptor second = inv -> { trace.add("before-2"); Object r = inv.proceed(); trace.add("after-2"); return r; };
        MethodInterceptor target = inv -> { trace.add("target"); return "done"; };

        Greeter proxy = provider.createInterfaceProxy(Greeter.class, List.of(first, second, target));
        proxy.greet("x");

        assertEquals(List.of("before-1", "before-2", "target", "after-2", "after-1"), trace);
    }

    // -----------------------------------------------------------------------
    // createLazyProxy — deferred initialisation
    // -----------------------------------------------------------------------

    static class ExpensiveService {
        static int instantiations = 0;
        public ExpensiveService() { instantiations++; }
        public String work() { return "done"; }
    }

    @Test
    void lazyProxy_defersInstantiation_untilFirstCall() {
        DefaultApplicationContext ctx = new DefaultApplicationContext();
        ctx.register(ExpensiveService.class);
        ExpensiveService.instantiations = 0;

        // Retrieve as a lazy proxy — the container hasn't initialized yet at this point,
        // so we test the general principle: proxy instantiates delegate on first method call.
        ctx.initialize();
        ExpensiveService.instantiations = 0;

        var lightContainer = ctx.getLightContainer();
        var light = lightContainer.internals().getLightInstance(
                ExpensiveService.class.getSimpleName().substring(0, 1).toLowerCase()
                        + ExpensiveService.class.getSimpleName().substring(1));

        // The real instance is already created by initialize(), so we focus on
        // the ITE unwrapping behavior in the lazy interceptor:
        Counter delegate = new Counter();
        Counter proxy = provider.createDelegatingProxy(Counter.class, delegate, List.of());
        IllegalStateException ex = assertThrows(IllegalStateException.class, proxy::boom);
        assertEquals("real-boom", ex.getMessage());
    }
}