package io.lumen.web.support;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A {@link ServletOutputStream} that records everything written to it, so tests can assert on
 * the exact response body a converter produced instead of only on the status code.
 */
public class CapturingOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    @Override
    public void write(int b) {
        buffer.write(b);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {}

    public String body() {
        return buffer.toString(StandardCharsets.UTF_8);
    }
}