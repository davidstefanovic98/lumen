package io.lumen.websocket;

public class BinaryMessage {

    private final byte[] payload;

    public BinaryMessage(byte[] payload) {
        this.payload = payload;
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getPayloadLength() {
        return payload.length;
    }
}