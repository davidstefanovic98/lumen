package io.lumen.data;

import io.lumen.context.PackageScanner;
import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.data.annotation.Repository;
import io.lumen.data.exception.InvalidDdlAutoException;
import io.lumen.data.transaction.JpaTransactionManager;
import io.lumen.data.transaction.LumenTransactionManager;
import io.lumen.data.transaction.TransactionalProcessor;
import io.lumen.data.web.DataWebLumenInitializer;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;

@Order(Integer.MAX_VALUE)
public class LumenDataModule implements LumenModule {

    private static final Set<String> VALID_DDL_AUTO_VALUES =
            Set.of("none", "validate", "update", "create", "create-drop");

    @Override
    public void init(LightContainer container, String... basePackages) {
        container.addPostProcessor(new TransactionalProcessor(container));

        if (isWebPresent()) {
            container.register(DataWebLumenInitializer.class);
        }

        RepositoryFactory repositoryFactory = new RepositoryFactory(container);
        container.registerExternalInstance(RepositoryFactory.class, repositoryFactory);

        PackageScanner.scan(basePackages).forEach(clazz -> {
            if (clazz.isInterface() && isRepositoryInterface(clazz)) {
                container.registerFactory(
                        clazz.getSimpleName(),
                        clazz,
                        (definition) -> repositoryFactory.create(clazz)
                );
            }
        });

        Environment env = container.getLight(Environment.class);
        if (env != null && env.getProperty("lumen.datasource.url") != null && isHibernatePresent()) {
            configureJpa(env, container, basePackages);
        }
    }

    private void configureJpa(Environment env, LightContainer container, String[] basePackages) {
        List<Class<?>> entityClasses = PackageScanner.scan(basePackages).stream()
                .filter(c -> c.isAnnotationPresent(Entity.class))
                .toList();

        String ddlAuto = env.getProperty("lumen.jpa.ddl-auto", "none");
        validateDdlAuto(ddlAuto);

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.hbm2ddl.auto", ddlAuto);

        // LumenDatasourceModule (@Order 5) registers the DataSource as an external instance
        // before this module runs. Fall back to plain JDBC settings if HikariCP was absent.
        DataSource ds = container.getLight(DataSource.class);
        if (ds != null) {
            builder.applySetting("hibernate.connection.datasource", ds);
        } else {
            builder.applySetting("jakarta.persistence.jdbc.url",      env.getProperty("lumen.datasource.url"))
                   .applySetting("jakarta.persistence.jdbc.user",     env.getProperty("lumen.datasource.username", ""))
                   .applySetting("jakarta.persistence.jdbc.password", env.getProperty("lumen.datasource.password", ""));
        }

        StandardServiceRegistry serviceRegistry = builder.build();
        MetadataSources metadataSources = new MetadataSources(serviceRegistry);
        entityClasses.forEach(metadataSources::addAnnotatedClass);

        EntityManagerFactory emf = metadataSources.buildMetadata().buildSessionFactory();
        container.registerExternalInstance(EntityManagerFactory.class, emf);
        container.registerExternalInstance(LumenTransactionManager.class, new JpaTransactionManager(emf));
        container.registerExternalInstance(EntityManagerFactoryDisposable.class, new EntityManagerFactoryDisposable(emf));
    }

    private boolean isWebPresent() {
        try {
            Class.forName("io.lumen.web.argument.CompositeMethodArgumentResolver");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Hibernate treats an unrecognized {@code hibernate.hbm2ddl.auto} value as "do nothing" —
     * a typo like {@code drop-and-create} would otherwise silently no-op instead of failing.
     */
    void validateDdlAuto(String ddlAuto) {
        if (!VALID_DDL_AUTO_VALUES.contains(ddlAuto)) {
            throw new InvalidDdlAutoException(
                    "'" + ddlAuto + "' is not a valid value for lumen.jpa.ddl-auto");
        }
    }

    private boolean isHibernatePresent() {
        try {
            Class.forName("org.hibernate.boot.registry.StandardServiceRegistryBuilder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isRepositoryInterface(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Repository.class)) return true;
        for (Class<?> iface : clazz.getInterfaces()) {
            if (io.lumen.data.repository.Repository.class.isAssignableFrom(iface)) return true;
        }
        return false;
    }
}
