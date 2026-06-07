package io.lumen.security;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.core.task.TaskDecorator;
import io.lumen.security.context.SecurityContextTaskDecorator;
import io.lumen.security.manager.DaoAuthenticationProvider;
import io.lumen.security.manager.ProviderManager;
import io.lumen.security.method.MethodSecurityProcessor;
import io.lumen.security.websocket.SecurityHandshakeInterceptor;

@Order(0)
public class LumenSecurityModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        container.register(LumenSecurityFilter.class);
        container.register(ProviderManager.class);
        container.register(DaoAuthenticationProvider.class);

        Environment env = container.getLight(Environment.class);
        Integer configured = env.getProperty("lumen.security.method.proxy-order", Integer.class);
        int proxyOrder = configured != null ? configured : -1;

        container.addPostProcessor(new MethodSecurityProcessor(), proxyOrder);

        // Propagate SecurityContext into @Async worker threads. Picked up by lumen-async as a
        // TaskDecorator if that module is present; otherwise it sits unused (no async↔security coupling).
        container.registerExternalInstance(TaskDecorator.class, new SecurityContextTaskDecorator());

        // Propagate SecurityContext into WebSocket handler threads. Registered only when
        // lumen-websocket is on the classpath; no websocket↔security coupling.
        try {
            Class.forName("io.lumen.websocket.HandshakeInterceptor");
            container.registerExternalInstance(
                    io.lumen.websocket.HandshakeInterceptor.class,
                    new SecurityHandshakeInterceptor());
        } catch (ClassNotFoundException ignored) {
            // lumen-websocket not on classpath — skip
        }
    }
}
