package io.lumen.cache;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;

@Order(-1)
public class LumenCacheModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        CacheManager cacheManager;

        if (container.hasLight(CacheManager.class)) {
            // User provided their own CacheManager bean — use it
            cacheManager = container.getLight(CacheManager.class);
        } else {
            cacheManager = new SimpleCacheManager();
            container.registerExternalInstance(CacheManager.class, cacheManager);
        }

        container.addPostProcessor(new CacheProcessor(cacheManager));
    }
}