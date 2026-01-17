package io.lumen.core.component;

import io.lumen.core.annotation.Inject;
import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.DefaultApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FieldInjectionTest {

    ApplicationContext context;
    @BeforeEach
    void setup() {
        context = new DefaultApplicationContext();
    }

    static class FieldInjectedService {
        @Inject
        private Repo repo;

        @Inject(required = false)
        private OptionalRepo optionalRepo;

        public Repo getRepo() {
            return repo;
        }

        public OptionalRepo getOptionalRepo() {
            return optionalRepo;
        }
    }

    static class Repo {}
    static class OptionalRepo {}

    @Test
    void testFieldInjection() {
        context.register(Repo.class);
        context.register(FieldInjectedService.class);

        context.initialize();

        FieldInjectedService service = context.getLight(FieldInjectedService.class);

        assertNotNull(service);
        assertNotNull(service.getRepo(), "Field with @Inject should be populated");
    }

    @Test
    void testOptionalFieldInjection() {
        context.register(Repo.class);
//        context.register(OptionalRepo.class);
        context.register(FieldInjectedService.class);

        context.initialize();

        FieldInjectedService service = context.getLight(FieldInjectedService.class);

        assertNotNull(service);
        assertNotNull(service.getRepo(), "Field with @Inject should be populated");
        assertNull(service.getOptionalRepo(), "Field with @Inject(required=false) should not be populated");
    }
}
