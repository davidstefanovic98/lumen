package io.lumen.data.transaction;

import io.lumen.core.component.LightContainer;
import io.lumen.core.context.DefaultApplicationContext;
import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.data.annotation.Transactional;
import io.lumen.data.transaction.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionalTest {

    // -------------------------------------------------------------------------
    // Stubs
    // -------------------------------------------------------------------------

    static class FakeTransaction implements EntityTransaction {
        int begins;
        int commits;
        int rollbacks;
        boolean active;

        @Override public void begin()    { begins++;    active = true;  }
        @Override public void commit()   { commits++;   active = false; }
        @Override public void rollback() { rollbacks++; active = false; }
        @Override public boolean isActive() { return active; }
        @Override public void setRollbackOnly() {}
        @Override public boolean getRollbackOnly() { return false; }
        @Override public Integer getTimeout() { return null; }
        @Override public void setTimeout(Integer timeout) {}

        void reset() { begins = commits = rollbacks = 0; active = false; }
    }

    static class FakeEntityManager implements EntityManager {
        final FakeTransaction tx = new FakeTransaction();
        @Override public EntityTransaction getTransaction() { return tx; }
        @Override public void persist(Object o) { throw new UnsupportedOperationException(); }
        @Override public <T> T merge(T t) { throw new UnsupportedOperationException(); }
        @Override public void remove(Object o) { throw new UnsupportedOperationException(); }
        @Override public <T> T find(Class<T> c, Object k) { throw new UnsupportedOperationException(); }
        @Override public <T> T find(Class<T> c, Object k, Map<String, Object> p) { throw new UnsupportedOperationException(); }
        @Override public <T> T find(Class<T> c, Object k, LockModeType l) { throw new UnsupportedOperationException(); }
        @Override public <T> T find(Class<T> c, Object k, LockModeType l, Map<String, Object> p) { throw new UnsupportedOperationException(); }
        @Override public <T> T find(Class<T> c, Object k, FindOption... opts) { throw new UnsupportedOperationException(); }
        @Override public <T> T find(EntityGraph<T> g, Object k, FindOption... opts) { throw new UnsupportedOperationException(); }
        @Override public <T> T getReference(Class<T> c, Object k) { throw new UnsupportedOperationException(); }
        @Override public <T> T getReference(T t) { throw new UnsupportedOperationException(); }
        @Override public void flush() {}
        @Override public void setFlushMode(FlushModeType f) {}
        @Override public FlushModeType getFlushMode() { return FlushModeType.AUTO; }
        @Override public void lock(Object o, LockModeType l) {}
        @Override public void lock(Object o, LockModeType l, Map<String, Object> p) {}
        @Override public void lock(Object o, LockModeType l, LockOption... opts) {}
        @Override public void refresh(Object o) {}
        @Override public void refresh(Object o, Map<String, Object> p) {}
        @Override public void refresh(Object o, LockModeType l) {}
        @Override public void refresh(Object o, LockModeType l, Map<String, Object> p) {}
        @Override public void refresh(Object o, RefreshOption... opts) {}
        @Override public void clear() {}
        @Override public void detach(Object o) {}
        @Override public boolean contains(Object o) { return false; }
        @Override public LockModeType getLockMode(Object o) { return LockModeType.NONE; }
        @Override public void setCacheRetrieveMode(CacheRetrieveMode m) {}
        @Override public void setCacheStoreMode(CacheStoreMode m) {}
        @Override public CacheRetrieveMode getCacheRetrieveMode() { return CacheRetrieveMode.USE; }
        @Override public CacheStoreMode getCacheStoreMode() { return CacheStoreMode.USE; }
        @Override public void setProperty(String k, Object v) {}
        @Override public Map<String, Object> getProperties() { return Map.of(); }
        @Override public Query createQuery(String s) { throw new UnsupportedOperationException(); }
        @Override public <T> TypedQuery<T> createQuery(CriteriaQuery<T> q) { throw new UnsupportedOperationException(); }
        @Override public <T> TypedQuery<T> createQuery(CriteriaSelect<T> q) { throw new UnsupportedOperationException(); }
        @Override public Query createQuery(CriteriaUpdate<?> q) { throw new UnsupportedOperationException(); }
        @Override public Query createQuery(CriteriaDelete<?> q) { throw new UnsupportedOperationException(); }
        @Override public <T> TypedQuery<T> createQuery(String s, Class<T> c) { throw new UnsupportedOperationException(); }
        @Override public Query createNamedQuery(String n) { throw new UnsupportedOperationException(); }
        @Override public <T> TypedQuery<T> createNamedQuery(String n, Class<T> c) { throw new UnsupportedOperationException(); }
        @Override public <T> TypedQuery<T> createQuery(TypedQueryReference<T> r) { throw new UnsupportedOperationException(); }
        @Override public Query createNativeQuery(String s) { throw new UnsupportedOperationException(); }
        @Override public <T> Query createNativeQuery(String s, Class<T> c) { throw new UnsupportedOperationException(); }
        @Override public Query createNativeQuery(String s, String r) { throw new UnsupportedOperationException(); }
        @Override public StoredProcedureQuery createNamedStoredProcedureQuery(String n) { throw new UnsupportedOperationException(); }
        @Override public StoredProcedureQuery createStoredProcedureQuery(String n) { throw new UnsupportedOperationException(); }
        @Override public StoredProcedureQuery createStoredProcedureQuery(String n, Class<?>... c) { throw new UnsupportedOperationException(); }
        @Override public StoredProcedureQuery createStoredProcedureQuery(String n, String... r) { throw new UnsupportedOperationException(); }
        @Override public void joinTransaction() {}
        @Override public boolean isJoinedToTransaction() { return false; }
        @Override public <T> T unwrap(Class<T> c) { throw new UnsupportedOperationException(); }
        @Override public Object getDelegate() { return null; }
        @Override public void close() {}
        @Override public boolean isOpen() { return true; }
        @Override public EntityManagerFactory getEntityManagerFactory() { throw new UnsupportedOperationException(); }
        @Override public CriteriaBuilder getCriteriaBuilder() { throw new UnsupportedOperationException(); }
        @Override public Metamodel getMetamodel() { throw new UnsupportedOperationException(); }
        @Override public <T> EntityGraph<T> createEntityGraph(Class<T> c) { throw new UnsupportedOperationException(); }
        @Override public EntityGraph<?> createEntityGraph(String n) { throw new UnsupportedOperationException(); }
        @Override public <C, T> T callWithConnection(ConnectionFunction<C, T> fn) { throw new UnsupportedOperationException(); }
        @Override public <C> void runWithConnection(ConnectionConsumer<C> fn) { throw new UnsupportedOperationException(); }
        @Override public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) { throw new UnsupportedOperationException(); }
        @Override public EntityGraph<?> getEntityGraph(String name) { throw new UnsupportedOperationException(); }
    }

    // -------------------------------------------------------------------------
    // Test beans
    // -------------------------------------------------------------------------

    interface Greeter {
        String greet(String name) throws Exception;
        String hello();
    }

    static class GreeterImpl implements Greeter {
        @Transactional
        @Override public String greet(String name) { return "Hello " + name; }
        @Override public String hello() { return "hi"; }
    }

    @Transactional
    static class FullyTransactionalGreeterImpl implements Greeter {
        @Override public String greet(String name) { return "Hello " + name; }
        @Override public String hello() { return "hi"; }
    }

    static class ThrowingGreeterImpl implements Greeter {
        @Transactional
        @Override public String greet(String name) { throw new IllegalStateException("boom"); }
        @Override public String hello() { return "hi"; }
    }

    static class NoRollbackGreeterImpl implements Greeter {
        @Transactional(noRollbackFor = IllegalStateException.class)
        @Override public String greet(String name) { throw new IllegalStateException("boom"); }
        @Override public String hello() { return "hi"; }
    }

    static class CheckedRollbackGreeterImpl implements Greeter {
        @Transactional(rollbackFor = Exception.class)
        @Override public String greet(String name) throws Exception { throw new Exception("checked"); }
        @Override public String hello() { return "hi"; }
    }

    // -------------------------------------------------------------------------
    // Fake transaction manager — delegates to FakeEntityManager so existing
    // assertions on fakeEm.tx.begins/commits/rollbacks continue to work.
    // -------------------------------------------------------------------------

    static class FakeTransactionManager implements LumenTransactionManager {
        final FakeEntityManager em;

        FakeTransactionManager(FakeEntityManager em) { this.em = em; }

        @Override
        public TransactionStatus getTransaction(io.lumen.data.annotation.Transactional annotation) {
            boolean readOnly = annotation.readOnly();
            boolean isNew = !em.getTransaction().isActive();
            if (isNew) em.getTransaction().begin();
            EntityManagerHolder.set(em);
            return new SimpleTransactionStatus(isNew);
        }

        @Override
        public void commit(TransactionStatus status) {
            if (!status.isNewTransaction()) return;
            if (status.isRollbackOnly()) em.getTransaction().rollback();
            else em.getTransaction().commit();
            EntityManagerHolder.clear();
        }

        @Override
        public void rollback(TransactionStatus status) {
            if (!status.isNewTransaction()) return;
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            EntityManagerHolder.clear();
        }
    }

    // ── Propagation-specific test beans ──────────────────────────────────────

    static class RequiresNewService {
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public String run() { return "new-tx"; }
    }

    static class SupportsService {
        @Transactional(propagation = Propagation.SUPPORTS)
        public String run() { return "supports"; }
    }

    static class MandatoryService {
        @Transactional(propagation = Propagation.MANDATORY)
        public String run() { return "mandatory"; }
    }

    static class NeverService {
        @Transactional(propagation = Propagation.NEVER)
        public String run() { return "never"; }
    }

    static class NotSupportedService {
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        public String run() { return "not-supported"; }
    }

    FakeEntityManager fakeEm;
    LightContainer container;

    @BeforeEach
    void setup() {
        fakeEm = new FakeEntityManager();
        DefaultApplicationContext ctx = new DefaultApplicationContext();
        ctx.registerInstance("transactionManager", new FakeTransactionManager(fakeEm));
        ctx.initialize();
        container = ctx.getLightContainer();
    }

    private TransactionalInterceptor interceptorFor(Object target) {
        return new TransactionalInterceptor(container, target);
    }

    private MethodInvocation invocationOf(String methodName, Object... args) throws NoSuchMethodException {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
        Method method = Greeter.class.getMethod(methodName, types);
        return new MethodInvocation() {
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return args; }
            @Override public Object proceed() { throw new UnsupportedOperationException(); }
        };
    }

    // -------------------------------------------------------------------------
    // TransactionalInterceptor unit tests
    // -------------------------------------------------------------------------

    @Test
    void nonTransactionalMethod_neverTouchesTransaction() throws Throwable {
        TransactionalInterceptor interceptor = interceptorFor(new GreeterImpl());
        interceptor.invoke(invocationOf("hello"));
        assertEquals(0, fakeEm.tx.begins);
    }

    @Test
    void transactionalMethod_beginsAndCommitsOnSuccess() throws Throwable {
        TransactionalInterceptor interceptor = interceptorFor(new GreeterImpl());
        Object result = interceptor.invoke(invocationOf("greet", "World"));
        assertEquals("Hello World", result);
        assertEquals(1, fakeEm.tx.begins);
        assertEquals(1, fakeEm.tx.commits);
        assertEquals(0, fakeEm.tx.rollbacks);
    }

    @Test
    void classLevelTransactional_wrapsAllMethods() throws Throwable {
        TransactionalInterceptor interceptor = interceptorFor(new FullyTransactionalGreeterImpl());
        interceptor.invoke(invocationOf("greet", "World"));
        interceptor.invoke(invocationOf("hello"));
        assertEquals(2, fakeEm.tx.begins);
        assertEquals(2, fakeEm.tx.commits);
    }

    @Test
    void runtimeException_rollsBackTransaction() {
        TransactionalInterceptor interceptor = interceptorFor(new ThrowingGreeterImpl());
        assertThrows(IllegalStateException.class,
                () -> interceptor.invoke(invocationOf("greet", "World")));
        assertEquals(1, fakeEm.tx.begins);
        assertEquals(0, fakeEm.tx.commits);
        assertEquals(1, fakeEm.tx.rollbacks);
    }

    @Test
    void noRollbackFor_commitsInsteadOfRollingBack() {
        TransactionalInterceptor interceptor = interceptorFor(new NoRollbackGreeterImpl());
        assertThrows(IllegalStateException.class,
                () -> interceptor.invoke(invocationOf("greet", "World")));
        assertEquals(1, fakeEm.tx.begins);
        assertEquals(1, fakeEm.tx.commits);
        assertEquals(0, fakeEm.tx.rollbacks);
    }

    @Test
    void rollbackFor_rollsBackOnCheckedException() {
        TransactionalInterceptor interceptor = interceptorFor(new CheckedRollbackGreeterImpl());
        assertThrows(Exception.class,
                () -> interceptor.invoke(invocationOf("greet", "World")));
        assertEquals(1, fakeEm.tx.begins);
        assertEquals(0, fakeEm.tx.commits);
        assertEquals(1, fakeEm.tx.rollbacks);
    }

    @Test
    void joinsExistingTransaction_noDoubleBegin() throws Throwable {
        fakeEm.tx.begin();
        fakeEm.tx.begins = 0;

        TransactionalInterceptor interceptor = interceptorFor(new GreeterImpl());
        interceptor.invoke(invocationOf("greet", "World"));

        assertEquals(0, fakeEm.tx.begins);
        assertEquals(0, fakeEm.tx.commits);
        assertEquals(0, fakeEm.tx.rollbacks);
    }

    @Test
    void processor_wrapsTransactionalBeanInProxy() {
        DefaultApplicationContext ctx = new DefaultApplicationContext();
        ctx.registerInstance("transactionManager", new FakeTransactionManager(fakeEm));
        ctx.getLightContainer().addPostProcessor(new TransactionalProcessor(ctx.getLightContainer()));
        ctx.register(GreeterImpl.class);
        ctx.initialize();

        Greeter greeter = ctx.getLight(Greeter.class);

        assertNotNull(greeter);
        assertNotSame(GreeterImpl.class, greeter.getClass(), "Bean should be wrapped in a proxy");
    }

    @Test
    void processor_proxyManagesTransactionsOnAnnotatedMethods() throws Exception {
        DefaultApplicationContext ctx = new DefaultApplicationContext();
        ctx.registerInstance("transactionManager", new FakeTransactionManager(fakeEm));
        ctx.getLightContainer().addPostProcessor(new TransactionalProcessor(ctx.getLightContainer()));
        ctx.register(GreeterImpl.class);
        ctx.initialize();

        Greeter greeter = ctx.getLight(Greeter.class);

        fakeEm.tx.reset();
        greeter.greet("Lumen");
        assertEquals(1, fakeEm.tx.begins,  "greet() should start a transaction");
        assertEquals(1, fakeEm.tx.commits, "greet() should commit");

        fakeEm.tx.reset();
        greeter.hello();
        assertEquals(0, fakeEm.tx.begins, "hello() should not start a transaction");
    }

    @Test
    void processor_doesNotWrapNonTransactionalBean() {
        DefaultApplicationContext ctx = new DefaultApplicationContext();
        ctx.registerInstance("transactionManager", new FakeTransactionManager(fakeEm));
        ctx.getLightContainer().addPostProcessor(new TransactionalProcessor(ctx.getLightContainer()));

        ctx.register(PlainService.class);
        ctx.initialize();

        PlainService svc = ctx.getLight(PlainService.class);
        assertSame(PlainService.class, svc.getClass(), "Plain bean should not be proxied");
    }

    static class PlainService {
        public String doWork() { return "done"; }
    }

    // ── Fake EntityManagerFactory for JpaTransactionManager tests ─────────────

    static EntityManagerFactory fakeEmf(List<FakeEntityManager> created) {
        return (EntityManagerFactory) java.lang.reflect.Proxy.newProxyInstance(
                EntityManagerFactory.class.getClassLoader(),
                new Class[]{EntityManagerFactory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "createEntityManager" -> {
                        FakeEntityManager em = new FakeEntityManager();
                        created.add(em);
                        yield em;
                    }
                    case "isOpen" -> true;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    // ── Annotation fixtures (one method per propagation type) ─────────────────

    @Transactional(propagation = Propagation.REQUIRED)    static void _required()    {}
    @Transactional(propagation = Propagation.REQUIRES_NEW)static void _requiresNew() {}
    @Transactional(propagation = Propagation.SUPPORTS)    static void _supports()    {}
    @Transactional(propagation = Propagation.NOT_SUPPORTED)static void _notSupported(){}
    @Transactional(propagation = Propagation.MANDATORY)   static void _mandatory()   {}
    @Transactional(propagation = Propagation.NEVER)       static void _never()       {}

    private static io.lumen.data.annotation.Transactional txOf(String name) throws Exception {
        return TransactionalTest.class.getDeclaredMethod(name).getAnnotation(Transactional.class);
    }

    static class ConcreteService {
        @Transactional
        public String run() { return "ran"; }
        public String plain() { return "plain"; }
    }

    @Test
    void processor_wrapsConcreteClassWithNoInterface() {
        DefaultApplicationContext ctx = new DefaultApplicationContext();
        ctx.registerInstance("transactionManager", new FakeTransactionManager(fakeEm));
        ctx.getLightContainer().addPostProcessor(new TransactionalProcessor(ctx.getLightContainer()));
        ctx.register(ConcreteService.class);
        ctx.initialize();

        ConcreteService svc = ctx.getLight(ConcreteService.class);
        assertNotNull(svc);
        assertNotSame(ConcreteService.class, svc.getClass(), "Concrete bean should be proxied");

        fakeEm.tx.reset();
        svc.run();
        assertEquals(1, fakeEm.tx.begins,  "run() should start a transaction");
        assertEquals(1, fakeEm.tx.commits, "run() should commit");

        fakeEm.tx.reset();
        svc.plain();
        assertEquals(0, fakeEm.tx.begins, "plain() should not start a transaction");
    }

    // ── JpaTransactionManager propagation tests ───────────────────────────────

    @Test
    void required_noActiveTx_beginsNewTransaction() throws Exception {
        List<FakeEntityManager> created = new ArrayList<>();
        JpaTransactionManager tm = new JpaTransactionManager(fakeEmf(created));

        TransactionStatus status = tm.getTransaction(txOf("_required"));

        assertTrue(status.isNewTransaction());
        assertEquals(1, created.size());
        assertTrue(created.getFirst().tx.active);
    }

    @Test
    void required_activeTx_joinsExisting() throws Exception {
        List<FakeEntityManager> created = new ArrayList<>();
        JpaTransactionManager tm = new JpaTransactionManager(fakeEmf(created));

        TransactionStatus outer = tm.getTransaction(txOf("_required"));
        int emCountAfterOuter = created.size();

        TransactionStatus inner = tm.getTransaction(txOf("_required"));

        assertFalse(inner.isNewTransaction());
        assertEquals(emCountAfterOuter, created.size(), "inner REQUIRED must not create a new EM");

        tm.commit(outer);
    }

    @Test
    void requiresNew_suspendsExistingAndStartsFresh() throws Exception {
        List<FakeEntityManager> created = new ArrayList<>();
        JpaTransactionManager tm = new JpaTransactionManager(fakeEmf(created));

        TransactionStatus outer = tm.getTransaction(txOf("_required"));
        FakeEntityManager outerEm = created.getFirst();

        TransactionStatus inner = tm.getTransaction(txOf("_requiresNew"));

        assertTrue(inner.isNewTransaction());
        assertEquals(2, created.size(), "REQUIRES_NEW must create a second EM");
        FakeEntityManager innerEm = created.get(1);
        assertTrue(innerEm.tx.active, "inner TX must be active");

        tm.commit(inner);
        assertFalse(innerEm.tx.active);
        assertEquals(outerEm, EntityManagerHolder.get(), "outer EM must be restored after REQUIRES_NEW commit");

        tm.commit(outer);
    }

    @Test
    void mandatory_withActiveTx_joins() throws Exception {
        List<FakeEntityManager> created = new ArrayList<>();
        JpaTransactionManager tm = new JpaTransactionManager(fakeEmf(created));

        TransactionStatus outer = tm.getTransaction(txOf("_required"));
        TransactionStatus inner = tm.getTransaction(txOf("_mandatory"));

        assertFalse(inner.isNewTransaction());

        tm.commit(outer);
    }

    @Test
    void mandatory_noActiveTx_throws() throws Exception {
        JpaTransactionManager tm = new JpaTransactionManager(fakeEmf(new ArrayList<>()));

        assertThrows(IllegalTransactionStateException.class,
                () -> tm.getTransaction(txOf("_mandatory")));
    }

    @Test
    void never_withActiveTx_throws() throws Exception {
        List<FakeEntityManager> created = new ArrayList<>();
        JpaTransactionManager tm = new JpaTransactionManager(fakeEmf(created));

        tm.getTransaction(txOf("_required"));

        assertThrows(IllegalTransactionStateException.class,
                () -> tm.getTransaction(txOf("_never")));

        EntityManagerHolder.clear();
    }

    @Test
    void never_noActiveTx_succeeds() throws Exception {
        List<FakeEntityManager> created = new ArrayList<>();
        JpaTransactionManager tm = new JpaTransactionManager(fakeEmf(created));

        TransactionStatus status = tm.getTransaction(txOf("_never"));

        assertFalse(status.isNewTransaction());
        assertEquals(0, created.size(), "NEVER must not create an EM");
    }

    @Test
    void notSupported_suspendsAndRestores() throws Exception {
        List<FakeEntityManager> created = new ArrayList<>();
        JpaTransactionManager tm = new JpaTransactionManager(fakeEmf(created));

        TransactionStatus outer = tm.getTransaction(txOf("_required"));
        FakeEntityManager outerEm = created.getFirst();

        TransactionStatus inner = tm.getTransaction(txOf("_notSupported"));

        assertFalse(inner.isNewTransaction());
        assertNull(EntityManagerHolder.get(), "NOT_SUPPORTED must clear the EM holder");

        tm.commit(inner);
        assertEquals(outerEm, EntityManagerHolder.get(), "outer EM must be restored after NOT_SUPPORTED");

        tm.commit(outer);
    }
}