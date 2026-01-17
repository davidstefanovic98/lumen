package io.lumen.core.component;

import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.ApplicationContextAware;
import io.lumen.core.context.DefaultApplicationContext;
import io.lumen.core.exception.MissingDependencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LightInstantiatorTest {

    ApplicationContext context;
    @BeforeEach
    void setup() {
        context = new DefaultApplicationContext();
    }

    // ---------- Test beans ----------

    // CLASS bean
    static class ClassLight implements ApplicationContextAware {
        private ApplicationContext context;
        public boolean initialized = false;

        @Override
        public void setApplicationContext(ApplicationContext context) {
            this.context = context;
        }

        public ApplicationContext getContext() {
            return context;
        }

        public ClassLight(Repo repo) {
            initialized = repo != null;
        }
    }

    // FACTORY bean
    static class FactoryLight implements ApplicationContextAware {
        private ApplicationContext context;

        @Override
        public void setApplicationContext(ApplicationContext context) {
            this.context = context;
        }

        public ApplicationContext getContext() {
            return context;
        }
    }

    static class Foo {
        Bar bar;
        public Foo(Bar bar) { this.bar = bar; }
    }

    static class Bar {
        String value = "hello";
    }


    static class Repo {}

    // ---------- Factory implementation ----------
    static class Factory implements LightFactory {
        @Override
        public Object create(LightDefinition definition) {
            return new FactoryLight();
        }
    }

    static class DevService {}
    static class ProdService {}
    static class ConditionalService {}

    // ---------- TESTS ----------

    @Test
    void testClassBeanWithDependencyAndApplicationContextAware() {
        context.register(Repo.class);
        context.register(ClassLight.class);

        context.initialize();

        // Retrieve ClassLight
        ClassLight light = context.getLight(ClassLight.class);

        assertNotNull(light);
        assertTrue(light.initialized);
        assertNotNull(light.getContext());
    }

    @Test
    void testFactoryBeanApplicationContextAware() {
        context.registerFactory("factoryBean", FactoryLight.class, new Factory());

        context.initialize();

        FactoryLight bean = context.getLight(FactoryLight.class);

        assertNotNull(bean);
        assertNotNull(bean.getContext());
    }

    @Test
    void testPreCreatedInstanceApplicationContextAware() {
        ClassLight instance = new ClassLight(new Repo());
        context.registerInstance("preBean", instance);

        context.initialize();

        ClassLight light = context.getLight("preBean");

        assertNotNull(light);
        assertSame(instance, light);
        assertNotNull(light.getContext());
    }

    @Test
    void testClassInstantiation() {
        context.register(Bar.class);
        context.register(Foo.class);

        context.initialize();

        Foo foo = context.getLight(Foo.class);
        Bar bar = context.getLight(Bar.class);

        assertNotNull(foo);
        assertNotNull(bar);
        assertSame(bar, foo.bar, "Constructor injection should inject the same instance");
    }

    @Test
    void testMissingDependencyThrows() {
        context.register(Foo.class); // Foo depends on Bar, which is missing
        MissingDependencyException ex = assertThrows(MissingDependencyException.class, context::initialize);
        assertTrue(ex.getMessage().contains("Missing dependency"));
    }


}
