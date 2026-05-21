package io.lumen.data.transaction;

import io.lumen.core.component.LightContainer;
import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.data.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TransactionalInterceptor implements MethodInterceptor {

    private final LightContainer container;
    private final Object target;
    private final Class<?> targetType;
    private LumenTransactionManager txManager;

    public TransactionalInterceptor(LightContainer container, Object target) {
        this.container = container;
        this.target = target;
        this.targetType = target.getClass();
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Transactional tx = resolveTransactional(invocation.getMethod());
        if (tx == null) {
            try {
                return invocation.getMethod().invoke(target, invocation.getArguments());
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        }
        return runInTransaction(invocation.getMethod(), invocation.getArguments(), tx);
    }

    private Object runInTransaction(Method method, Object[] args, Transactional tx) throws Throwable {
        LumenTransactionManager tm = transactionManager();
        TransactionStatus status = tm.getTransaction(tx.readOnly());
        try {
            Object result = method.invoke(target, args);
            if (status.isRollbackOnly()) tm.rollback(status);
            else tm.commit(status);
            return result;
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (shouldRollback(cause, tx)) tm.rollback(status);
            else tm.commit(status);
            throw cause;
        } catch (Throwable t) {
            tm.rollback(status);
            throw t;
        }
    }

    private Transactional resolveTransactional(Method interfaceMethod) {
        Transactional tx = interfaceMethod.getAnnotation(Transactional.class);
        if (tx != null) return tx;
        try {
            Method impl = targetType.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            tx = impl.getAnnotation(Transactional.class);
            if (tx != null) return tx;
        } catch (NoSuchMethodException ignored) {}
        return targetType.getAnnotation(Transactional.class);
    }

    private boolean shouldRollback(Throwable t, Transactional tx) {
        for (Class<? extends Throwable> no : tx.noRollbackFor()) {
            if (no.isInstance(t)) return false;
        }
        for (Class<? extends Throwable> yes : tx.rollbackFor()) {
            if (yes.isInstance(t)) return true;
        }
        return t instanceof RuntimeException || t instanceof Error;
    }

    private LumenTransactionManager transactionManager() {
        if (txManager == null)
            txManager = container.internals().getLightByType(LumenTransactionManager.class);
        return txManager;
    }
}