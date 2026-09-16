package io.lumen.web.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebServerTest {

    @Test
    void stopGracefully_idleServer_returnsQuicklyInsteadOfWaitingFullGracePeriod() {
        WebServer server = new WebServer(0);
        server.start();

        long start = System.currentTimeMillis();
        // A large grace period configured, but nothing is in flight - should return almost
        // immediately rather than blocking for the full 30s waiting on idle keep-alive sockets.
        server.stopGracefully(30);
        long elapsedMillis = System.currentTimeMillis() - start;

        assertTrue(elapsedMillis < 3000,
                "idle shutdown should not wait out the full grace period, took " + elapsedMillis + "ms");
    }
}