package io.lumen.data.repository.proxy;

import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.data.query.QueryParser;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class JpaRepositoryInterceptor implements MethodInterceptor {
    private final List<RepositoryFragment> fragments;

    public JpaRepositoryInterceptor(EntityManagerFactory emf, Class<?> entityClass, QueryParser queryParser) {
        this.fragments = List.of(
                new JpaRepositoryExecutor(emf, entityClass),
                new SpecificationExecutor(emf, entityClass),
                new PagingAndSortingExecutor(emf, entityClass),
                new CrudRepositoryExecutor(emf, entityClass),
                new DynamicQueryExecutor(emf, entityClass, queryParser)
        );
    }

    @Override
    public Object invoke(MethodInvocation invocation) {
        var method = invocation.getMethod();
        var args = invocation.getArguments();
        return fragments.stream()
                .filter(f -> f.canHandle(method, args))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("No fragment handles: " + method.getName()))
                .invoke(method, args);
    }
}
