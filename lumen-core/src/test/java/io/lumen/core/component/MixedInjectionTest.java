package io.lumen.core.component;

import io.lumen.core.annotation.Inject;
import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.DefaultApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MixedInjectionTest {

    ApplicationContext context;
    @BeforeEach
    void setup() {
        context = new DefaultApplicationContext();
    }

    static class MixedInjectionService {

        @Inject
        private Repo repo;

        private Helper helper;

        @Inject
        public void setHelper(Helper helper) {
            this.helper = helper;
        }

        Repo getRepo() { return repo; }
        Helper getHelper() { return helper; }
    }

    static class Helper {}
    static class Repo {}

    @Test
    void testMixedInjectionWorks() {
        context.register(Repo.class);
        context.register(Helper.class);
        context.register(MixedInjectionService.class);

        context.initialize();

        MixedInjectionService service =
                context.getLight(MixedInjectionService.class);

        assertNotNull(service.getRepo());
        assertNotNull(service.getHelper());
    }
}
