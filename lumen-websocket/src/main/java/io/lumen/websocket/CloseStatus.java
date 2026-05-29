package io.lumen.websocket;

public class CloseStatus {

    public static final CloseStatus NORMAL      = new CloseStatus(1000, "Normal closure");
    public static final CloseStatus GOING_AWAY  = new CloseStatus(1001, "Going away");
    public static final CloseStatus SERVER_ERROR = new CloseStatus(1011, "Server error");

    private final int code;
    private final String reason;

    public CloseStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "CloseStatus[" + code + ", " + reason + "]";
    }
}