package io.lumen.websocket;

public class TextMessage {

    private final String payload;

    public TextMessage(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return payload;
    }
}