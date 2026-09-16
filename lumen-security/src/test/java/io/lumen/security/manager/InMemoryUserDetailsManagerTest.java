package io.lumen.security.manager;

import io.lumen.security.authentication.UserDetails;
import io.lumen.security.authority.GrantedAuthority;
import io.lumen.security.exception.UsernameNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserDetailsManagerTest {

    private static UserDetails user(String username) {
        return new UserDetails() {
            @Override public String getUsername() { return username; }
            @Override public String getPassword() { return "password"; }
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public boolean isEnabled() { return true; }
        };
    }

    @Test
    void loadUserByUsername_returnsCreatedUser() {
        var manager = new InMemoryUserDetailsManager();
        manager.createUser(user("alice"));

        assertEquals("alice", manager.loadUserByUsername("alice").getUsername());
    }

    @Test
    void loadUserByUsername_unknownUser_throws() {
        var manager = new InMemoryUserDetailsManager();

        assertThrows(UsernameNotFoundException.class, () -> manager.loadUserByUsername("nobody"));
    }

    @Test
    void concurrentUserCreationAndLookup_noLostUpdatesOrExceptions() throws InterruptedException {
        var manager = new InMemoryUserDetailsManager();
        int threadCount = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                manager.createUser(user("user-" + index));
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        for (int i = 0; i < threadCount; i++) {
            assertEquals("user-" + i, manager.loadUserByUsername("user-" + i).getUsername());
        }
    }
}