package io.lumen.core.component;

import io.lumen.core.annotation.Inject;
import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.DefaultApplicationContext;
import io.lumen.core.exception.LightInstantiationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SetterInjectionTest {

    ApplicationContext context;
    @BeforeEach
    void setup() {
        context = new DefaultApplicationContext();
    }

    static class SetterInjectedService {

        private Repo repo;
        private OptionalRepo optionalRepo;

        @Inject
        public void setRepo(Repo repo) {
            this.repo = repo;
        }

        public Repo getRepo() {
            return repo;
        }

        @Inject(required = false)
        public void setOptionalRepo(OptionalRepo optionalRepo) {
            this.optionalRepo = optionalRepo;
        }

        public OptionalRepo getOptionalRepo() {
            return optionalRepo;
        }
    }

    static class Repo {}
    static class OptionalRepo {}

    @Test
    void testSetterInjection() {
        context.register(Repo.class);
        context.register(SetterInjectedService.class);

        context.initialize();

        SetterInjectedService service =
                context.getLight(SetterInjectedService.class);

        assertNotNull(service);
        assertNotNull(service.getRepo(),
                "Setter with @Inject should be invoked");
    }

    @Test
    void setterInjectionMissingDependencyFails() {
        context.register(SetterInjectedService.class);

        assertThrows(
                LightInstantiationException.class,
                context::initialize,
                "Missing setter dependency should fail container initialization"
        );
    }

    @Test
    void testOptionalSetterInjection() {
        context.register(Repo.class);
        context.register(SetterInjectedService.class);

        context.initialize();

        SetterInjectedService service =
                context.getLight(SetterInjectedService.class);

        assertNotNull(service);
        assertNull(service.getOptionalRepo(),
                "Setter with @Inject should not be invoked");
    }
}
