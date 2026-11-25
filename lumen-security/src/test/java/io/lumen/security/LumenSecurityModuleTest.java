package io.lumen.security;

import io.lumen.core.component.LightContainer;
import io.lumen.core.context.DefaultApplicationContext;
import io.lumen.core.context.Environment;
import io.lumen.web.exception.handle.FilterChainExceptionMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LumenSecurityModuleTest {

    @Test
    void init_registersSecurityExceptionMatcher_whereLumenWebModuleLooksItUp() {
        LightContainer container = new DefaultApplicationContext().getLightContainer();
        container.registerExternalInstance(Environment.class, new Environment());

        new LumenSecurityModule().init(container);

        // The same lookup LumenWebModule hands to CompositeExceptionResolver
        List<FilterChainExceptionMatcher> matchers = container.getLights(FilterChainExceptionMatcher.class);
        assertEquals(1, matchers.size());
        assertInstanceOf(SecurityExceptionMatcher.class, matchers.getFirst());
    }
}