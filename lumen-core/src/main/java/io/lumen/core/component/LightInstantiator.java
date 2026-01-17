package io.lumen.core.component;

public interface LightInstantiator {

    boolean supports(LightInstance light);

    Object instantiate(LightInstance light, LightContainer container);
}
