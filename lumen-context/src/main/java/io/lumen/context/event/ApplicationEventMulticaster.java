package io.lumen.context.event;

import io.lumen.context.annotation.EventListener;
import io.lumen.core.event.ApplicationEvent;
import io.lumen.core.event.ApplicationEventPublisher;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ApplicationEventMulticaster implements ApplicationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationEventMulticaster.class);

    private final List<ListenerMethod> listeners = new CopyOnWriteArrayList<>();

    public void registerListener(Object bean, Method method) {
        listeners.add(new ListenerMethod(bean, method));
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        for (ListenerMethod lm : listeners) {
            lm.invokeIfSupports(event);
        }
    }

    private record ListenerMethod(Object bean, Method method) {
        void invokeIfSupports(ApplicationEvent event) {
            try {
                if (method.getParameterCount() == 0) {
                    method.invoke(bean);
                } else if (method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(event.getClass())) {
                    method.invoke(bean, event);
                }
            } catch (Exception e) {
                Logger log = LoggerFactory.getLogger(ApplicationEventMulticaster.class);
                log.error("Error invoking @EventListener {}.{}: {}",
                        bean.getClass().getSimpleName(), method.getName(), e.getMessage(), e);
            }
        }
    }
}