package io.lumen.core.component;

import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.ApplicationContextAware;
import io.lumen.core.context.DefaultApplicationContext;
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
        // Register dependencies
        context.register(Repo.class);
        context.register(ClassLight.class);

        // Initialize (resolve dependencies)
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
    void testProfileAndConditional() {
        DefaultApplicationContext context = new DefaultApplicationContext();

        LightDefinition devBean = LightDefinition.fromClass(DevService.class);
        devBean.setProfile("dev");
        context.getLightContainer().registerDefinition(devBean);

        LightDefinition prodBean = LightDefinition.fromClass(ProdService.class);
        prodBean.setProfile("prod");
        context.getLightContainer().registerDefinition(prodBean);

        LightDefinition conditionalBean = LightDefinition.fromClass(ConditionalService.class);
        conditionalBean.setCondition(() -> false);
        context.getLightContainer().registerDefinition(conditionalBean);

        context.getEnvironment().setActiveProfile("dev");

        context.initialize();

        assertNotNull(context.getLight(DevService.class));

        assertThrows(Exception.class, () -> context.getLight(ProdService.class));

        assertThrows(Exception.class, () -> context.getLight(ConditionalService.class));
    }
}
