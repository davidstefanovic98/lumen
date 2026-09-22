package io.lumen.data.repository;

import io.lumen.core.proxy.ProxyFactory;
import io.lumen.data.query.CompositeQueryParser;
import io.lumen.data.repository.proxy.JpaRepositoryInterceptor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeleteAllInBatchTest {

    static EntityManagerFactory emf;
    EntityManager em;
    TestUserRepository repo;

    @BeforeAll
    static void createFactory() {
        emf = Persistence.createEntityManagerFactory("lumen-test");
    }

    @AfterAll
    static void closeFactory() {
        if (emf != null) emf.close();
    }

    @BeforeEach
    void setup() {
        em   = emf.createEntityManager();
        repo = ProxyFactory.createInterfaceProxy(
                TestUserRepository.class,
                List.of(new JpaRepositoryInterceptor(emf, TestUser.class, new CompositeQueryParser())));

        em.getTransaction().begin();
        em.persist(new TestUser("Alice",   30));
        em.persist(new TestUser("Bob",     25));
        em.persist(new TestUser("Charlie", 35));
        em.getTransaction().commit();
    }

    @AfterEach
    void teardown() {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM TestUser").executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    @Test
    void deleteAllInBatch_removesOnlyGivenEntities() {
        List<TestUser> all = repo.findAll();
        TestUser bob = all.stream().filter(u -> u.getName().equals("Bob")).findFirst().orElseThrow();

        repo.deleteAllInBatch(List.of(bob));

        List<TestUser> remaining = repo.findAll();
        assertEquals(2, remaining.size());
        assertTrue(remaining.stream().noneMatch(u -> u.getName().equals("Bob")));
    }

    @Test
    void deleteAllInBatch_issuesOneBulkStatement_notOnePerEntity() {
        // findAll() runs in its own short-lived EntityManager (via inTransaction()), so the
        // returned entities are already detached by the time this call returns.
        List<TestUser> all = repo.findAll();
        assertEquals(3, all.size());

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        repo.deleteAllInBatch(all);

        assertEquals(1, stats.getPrepareStatementCount(),
                "deleteAllInBatch must issue exactly one JDBC statement, not one per entity");
        assertEquals(0, stats.getEntityDeleteCount(),
                "deleteAllInBatch must bypass the persistence context (bulk DELETE), " +
                "not remove entities one at a time");
        assertEquals(0, repo.findAll().size());
    }

    @Test
    void deleteAllInBatch_worksWithDetachedIdOnlyReferences() {
        // Regression: the previous implementation routed through deleteEntity(), which calls
        // em.merge(entity) for anything not already managed — for an entity carrying only its id
        // (a common bulk-delete-by-id-list pattern), that merge would have overwritten the row's
        // real name/age with null before removing it. The bulk statement never touches the row's
        // other columns at all.
        List<TestUser> all = repo.findAll();
        TestUser bobById = new TestUser();
        setId(bobById, all.stream().filter(u -> u.getName().equals("Bob")).findFirst().orElseThrow().getId());

        repo.deleteAllInBatch(List.of(bobById));

        List<TestUser> remaining = repo.findAll();
        assertEquals(2, remaining.size());
        assertTrue(remaining.stream().noneMatch(u -> u.getName().equals("Bob")));
    }

    @Test
    void deleteAllInBatch_emptyIterable_doesNothing() {
        repo.deleteAllInBatch(List.of());

        assertEquals(3, repo.findAll().size());
    }

    private static void setId(TestUser user, Long id) {
        try {
            var field = TestUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}