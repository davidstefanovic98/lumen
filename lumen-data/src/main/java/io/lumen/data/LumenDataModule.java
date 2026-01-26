package io.lumen.data;

import io.lumen.context.PackageScanner;
import io.lumen.core.LumenModule;
import io.lumen.core.component.LightContainer;
import io.lumen.data.annotation.Repository;

public class LumenDataModule implements LumenModule {
    @Override
    public void init(LightContainer container, String... basePackages) {
        RepositoryFactory repositoryFactory = new RepositoryFactory(container);
        container.registerExternalInstance(RepositoryFactory.class, repositoryFactory);

        PackageScanner.scan(basePackages).forEach(clazz -> {
            if (clazz.isInterface() && clazz.isAnnotationPresent(Repository.class)) {
                container.registerFactory(
                        clazz.getSimpleName(),
                        clazz,
                        (definition) -> repositoryFactory.create(clazz)
                );
            }
        });
    }
}
