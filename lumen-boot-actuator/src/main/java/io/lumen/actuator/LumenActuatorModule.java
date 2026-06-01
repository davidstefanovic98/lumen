package io.lumen.actuator;

import io.lumen.actuator.health.SimpleHealthIndicator;
import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

@Order(10)
public class LumenActuatorModule implements LumenModule {

    private static final Logger logger = LoggerFactory.getLogger(LumenActuatorModule.class);

    @Override
    public void init(LightContainer container, String... basePackages) {
        Environment env = container.getLight(Environment.class);
        boolean enabled = Boolean.parseBoolean(
                env != null ? env.getProperty("lumen.actuator.enabled", "true") : "true");

        if (!enabled) {
            logger.info("Actuator disabled (lumen.actuator.enabled=false)");
            return;
        }

        container.register(ActuatorController.class);
        container.register(SimpleHealthIndicator.class);

        String basePath = env != null ? env.getProperty("lumen.actuator.base-path", "/actuator") : "/actuator";
        logger.info("Actuator endpoints registered at {}", basePath);
    }
}