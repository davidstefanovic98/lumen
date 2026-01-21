package io.lumen.web.http;

import java.util.HashMap;
import java.util.Map;

public class ResponseEntity<T> {
    private final int status;
    private final Map<String, String> headers;
    private final T body;

    private ResponseEntity(int status, T body, Map<String, String> headers) {
        this.status = status;
        this.body = body;
        this.headers = headers;
    }

    public int getStatus() { return status; }
    public T getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }

    public static <T> BodyBuilder status(int status) {
        return new DefaultBuilder(status);
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return status(200).body(body);
    }

    public interface BodyBuilder {
        BodyBuilder header(String name, String value);
        <T> ResponseEntity<T> body(T body);
        <T> ResponseEntity<T> build();
    }

    private static class DefaultBuilder implements BodyBuilder {
        private final int status;
        private final Map<String, String> headers = new HashMap<>();

        DefaultBuilder(int status) { this.status = status; }

        public BodyBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public <T> ResponseEntity<T> body(T body) {
            return new ResponseEntity<>(status, body, headers);
        }

        public <T> ResponseEntity<T> build() {
            return new ResponseEntity<>(status, null, headers);
        }
    }
}
