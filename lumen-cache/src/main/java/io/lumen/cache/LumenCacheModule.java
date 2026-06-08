package io.lumen.cache;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;

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

        Environment env = container.getLight(Environment.class);
        Integer configured = env.getProperty("lumen.cache.proxy-order", Integer.class);
        int proxyOrder = configured != null ? configured : -1;

        container.addPostProcessor(new CacheProcessor(cacheManager), proxyOrder);
    }
}