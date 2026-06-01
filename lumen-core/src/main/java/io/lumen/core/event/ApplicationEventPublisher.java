package io.lumen.core.event;

public interface ApplicationEventPublisher {
    void publishEvent(ApplicationEvent event);
}