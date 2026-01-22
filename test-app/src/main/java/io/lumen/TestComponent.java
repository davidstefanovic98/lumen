package io.lumen;

import io.lumen.context.annotation.Component;
import io.lumen.core.annotation.PostConstruct;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

@Component
public class TestComponent {
    private static final Logger logger = LoggerFactory.getLogger(TestComponent.class);
    @PostConstruct
    public void init() {
        logger.info("PostConstruct called on TestComponent");
    }
}
