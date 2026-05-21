package io.lumen.data.repository.proxy;

import java.lang.reflect.Method;

public interface RepositoryFragment {

    boolean canHandle(Method method, Object[] args);

    Object invoke(Method method, Object[] args);
}
