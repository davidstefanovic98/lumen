package io.lumen.core.component.processor;

import io.lumen.core.component.LightInstance;
import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.ApplicationContextAware;

public class ApplicationContextAwareProcessor implements LightProcessor {

    private final ApplicationContext context;

    public ApplicationContextAwareProcessor(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        if (instance instanceof ApplicationContextAware aware) {
            aware.setApplicationContext(context);
        }
        return instance;
    }
}
