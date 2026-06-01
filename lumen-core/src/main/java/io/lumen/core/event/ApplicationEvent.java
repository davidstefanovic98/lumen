package io.lumen.core.event;

public abstract class ApplicationEvent {

    private final Object source;
    private final long timestamp;

    protected ApplicationEvent(Object source) {
        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }

    public Object getSource() { return source; }
    public long getTimestamp() { return timestamp; }
}