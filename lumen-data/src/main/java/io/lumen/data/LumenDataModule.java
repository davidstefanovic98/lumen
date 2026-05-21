package io.lumen.data;

import io.lumen.context.PackageScanner;
import io.lumen.core.LumenModule;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.data.annotation.Repository;
import io.lumen.data.transaction.JpaTransactionManager;
import io.lumen.data.transaction.LumenTransactionManager;
import io.lumen.data.transaction.TransactionalProcessor;
import io.lumen.data.web.DataWebLumenInitializer;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.List;

public class LumenDataModule implements LumenModule {

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
            configureDataSource(env, container, basePackages);
        }
    }

    private void configureDataSource(Environment env, LightContainer container, String[] basePackages) {
        List<Class<?>> entityClasses = PackageScanner.scan(basePackages).stream()
                .filter(c -> c.isAnnotationPresent(Entity.class))
                .toList();

        StandardServiceRegistry serviceRegistry =
                new StandardServiceRegistryBuilder()
                        .applySetting("jakarta.persistence.jdbc.url",
                                env.getProperty("lumen.datasource.url"))
                        .applySetting("jakarta.persistence.jdbc.user",
                                env.getProperty("lumen.datasource.username", ""))
                        .applySetting("jakarta.persistence.jdbc.password",
                                env.getProperty("lumen.datasource.password", ""))
                        .applySetting("hibernate.hbm2ddl.auto",
                                env.getProperty("lumen.jpa.ddl-auto", "none"))
                        .build();

        MetadataSources metadataSources =
                new MetadataSources(serviceRegistry);
        entityClasses.forEach(metadataSources::addAnnotatedClass);

        EntityManagerFactory emf = metadataSources.buildMetadata().buildSessionFactory();

        container.registerExternalInstance(EntityManagerFactory.class, emf);
        container.registerExternalInstance(LumenTransactionManager.class, new JpaTransactionManager(emf));
    }

    private boolean isWebPresent() {
        try {
            Class.forName("io.lumen.web.argument.CompositeMethodArgumentResolver");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
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
