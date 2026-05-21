package io.lumen.data.web;

import io.lumen.core.LumenInitializer;
import io.lumen.core.annotation.Order;
import io.lumen.web.argument.CompositeMethodArgumentResolver;

@Order(3)
public class DataWebLumenInitializer implements LumenInitializer {

    private final CompositeMethodArgumentResolver argumentResolver;

    public DataWebLumenInitializer(CompositeMethodArgumentResolver argumentResolver) {
        this.argumentResolver = argumentResolver;
    }

    @Override
    public void onStartup() {
        argumentResolver.addResolver(new PageableMethodArgumentResolver());
    }
}
