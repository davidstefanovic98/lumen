package io.lumen.data.repository;

import io.lumen.core.proxy.ProxyFactory;
import io.lumen.data.pageable.Page;
import io.lumen.data.pageable.PageRequest;
import io.lumen.data.pageable.Sort;
import io.lumen.data.query.CompositeQueryParser;
import io.lumen.data.repository.proxy.JpaRepositoryInterceptor;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DerivedQueryTest {

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
        em.persist(new TestUser("Dave",    28));
        em.persist(new TestUser("alice",   22)); // lowercase for IgnoreCase tests
        em.getTransaction().commit();
    }

    @AfterEach
    void teardown() {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM TestUser").executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    // -------------------------------------------------------------------------
    // Basic equality & comparison
    // -------------------------------------------------------------------------

    @Test
    void findByName_exact() {
        assertEquals(1, repo.findByName("Alice").size());
        assertEquals(0, repo.findByName("Unknown").size());
    }

    @Test
    void findByAgeGreaterThan() {
        List<TestUser> result = repo.findByAgeGreaterThan(28);
        assertEquals(2, result.size()); // Alice(30), Charlie(35)
        assertTrue(result.stream().allMatch(u -> u.getAge() > 28));
    }

    @Test
    void findByAgeLessThan() {
        List<TestUser> result = repo.findByAgeLessThan(26);
        assertEquals(2, result.size()); // Bob(25), alice(22)
        assertTrue(result.stream().allMatch(u -> u.getAge() < 26));
    }

    @Test
    void findByAgeGreaterThanEqual() {
        List<TestUser> result = repo.findByAgeGreaterThanEqual(30);
        assertEquals(2, result.size()); // Alice(30), Charlie(35)
    }

    @Test
    void findByAgeLessThanEqual() {
        List<TestUser> result = repo.findByAgeLessThanEqual(25);
        assertEquals(2, result.size()); // Bob(25), alice(22)
    }

    @Test
    void findByAgeBetween() {
        List<TestUser> result = repo.findByAgeBetween(25, 30);
        assertEquals(3, result.size()); // Bob(25), Dave(28), Alice(30)
        assertTrue(result.stream().allMatch(u -> u.getAge() >= 25 && u.getAge() <= 30));
    }

    @Test
    void findByAgeIn() {
        List<TestUser> result = repo.findByAgeIn(List.of(25, 35));
        assertEquals(2, result.size()); // Bob(25), Charlie(35)
    }

    @Test
    void findByNameNot() {
        List<TestUser> result = repo.findByNameNot("Alice");
        assertEquals(4, result.size());
        assertTrue(result.stream().noneMatch(u -> u.getName().equals("Alice")));
    }

    // -------------------------------------------------------------------------
    // LIKE variants
    // -------------------------------------------------------------------------

    @Test
    void findByNameContaining() {
        List<TestUser> result = repo.findByNameContaining("li");
        // "Alice", "Charlie", "alice" all contain "li"
        assertEquals(3, result.size());
    }

    @Test
    void findByNameStartingWith() {
        List<TestUser> result = repo.findByNameStartingWith("Al");
        assertEquals(1, result.size()); // "Alice" (case-sensitive, not "alice")
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    void findByNameEndingWith() {
        List<TestUser> result = repo.findByNameEndingWith("e");
        // "Alice", "Charlie", "Dave", "alice" all end with "e"
        assertEquals(4, result.size());
    }

    // -------------------------------------------------------------------------
    // IgnoreCase
    // -------------------------------------------------------------------------

    @Test
    void findByNameIgnoreCase() {
        List<TestUser> result = repo.findByNameIgnoreCase("alice");
        assertEquals(2, result.size()); // "Alice" and "alice"
    }

    @Test
    void findByNameContainingIgnoreCase() {
        List<TestUser> result = repo.findByNameContainingIgnoreCase("LI");
        // "Alice", "Charlie", "alice" all contain "li" case-insensitively
        assertEquals(3, result.size());
    }

    // -------------------------------------------------------------------------
    // AND / OR
    // -------------------------------------------------------------------------

    @Test
    void findByNameAndAge() {
        List<TestUser> result = repo.findByNameAndAge("Alice", 30);
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    void findByNameOrAge() {
        List<TestUser> result = repo.findByNameOrAge("Alice", 25);
        assertEquals(2, result.size()); // Alice(30) and Bob(25)
    }

    // -------------------------------------------------------------------------
    // OrderBy suffix
    // -------------------------------------------------------------------------

    @Test
    void findByAgeGreaterThan_OrderByNameAsc() {
        List<TestUser> result = repo.findByAgeGreaterThanOrderByNameAsc(27);
        // Alice(30), Charlie(35), Dave(28) — sorted by name ASC
        assertEquals(3, result.size());
        assertEquals("Alice",   result.get(0).getName());
        assertEquals("Charlie", result.get(1).getName());
        assertEquals("Dave",    result.get(2).getName());
    }

    @Test
    void findByNameContaining_OrderByAgeDesc() {
        List<TestUser> result = repo.findByNameContainingOrderByAgeDesc("a");
        // Case-sensitive: "Charlie"(35), "Dave"(28), "alice"(22) contain lowercase "a"
        // "Alice" has no lowercase "a"
        assertEquals(3, result.size());
        assertEquals(35, result.get(0).getAge()); // Charlie first (highest age)
        assertEquals(22, result.get(2).getAge()); // alice last (lowest age)
    }

    // -------------------------------------------------------------------------
    // countBy / existsBy / deleteBy prefixes
    // -------------------------------------------------------------------------

    @Test
    void countByAgeGreaterThan() {
        assertEquals(2L, repo.countByAgeGreaterThan(28)); // Alice(30), Charlie(35)
    }

    @Test
    void existsByName_true() {
        assertTrue(repo.existsByName("Bob"));
    }

    @Test
    void existsByName_false() {
        assertFalse(repo.existsByName("NoOne"));
    }

    @Test
    void deleteByName() {
        long deleted = repo.deleteByName("Bob");
        assertEquals(1, deleted);
        assertFalse(repo.existsByName("Bob"));
    }

    // -------------------------------------------------------------------------
    // @Query — positional parameters
    // -------------------------------------------------------------------------

    @Test
    void query_positionalParams() {
        List<TestUser> result = repo.findInAgeRange(24, 31);
        // Bob(25), Dave(28), Alice(30)
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(u -> u.getAge() > 24 && u.getAge() < 31));
    }

    // -------------------------------------------------------------------------
    // @Query — named parameters
    // -------------------------------------------------------------------------

    @Test
    void query_namedParams_found() {
        Optional<TestUser> result = repo.findByNameAndMinAge("Alice", 25);
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
    }

    @Test
    void query_namedParams_notFound() {
        Optional<TestUser> result = repo.findByNameAndMinAge("Alice", 35);
        assertFalse(result.isPresent()); // Alice is 30, not > 35
    }

    // -------------------------------------------------------------------------
    // @Modifying
    // -------------------------------------------------------------------------

    @Test
    void modifying_update() {
        int affected = repo.updateAgeByName("Bob", 99);
        assertEquals(1, affected);

        em.clear(); // detach cached state
        List<TestUser> result = repo.findByName("Bob");
        assertEquals(99, result.get(0).getAge());
    }

    // -------------------------------------------------------------------------
    // Pageable on derived method
    // -------------------------------------------------------------------------

    @Test
    void findByAgeGreaterThan_pageable_firstPage() {
        // Alice(30), Charlie(35) are > 28; Bob(25), Dave(28), alice(22) are not
        Page<TestUser> page = repo.findByAgeGreaterThan(28, PageRequest.of(0, 1));

        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(1, page.getContent().size());
        assertTrue(page.isFirst());
        assertFalse(page.isLast());
    }

    @Test
    void findByAgeGreaterThan_pageable_secondPage() {
        Page<TestUser> page = repo.findByAgeGreaterThan(28, PageRequest.of(1, 1));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertFalse(page.isFirst());
        assertTrue(page.isLast());
    }

    @Test
    void findByAgeGreaterThan_pageable_withSort() {
        Page<TestUser> page = repo.findByAgeGreaterThan(20, PageRequest.of(0, 3, Sort.by("age")));

        assertEquals(3, page.getContent().size());
        assertEquals(22, page.getContent().get(0).getAge()); // alice first (lowest age)
    }

    // -------------------------------------------------------------------------
    // @Query with explicit countQuery + Pageable
    // -------------------------------------------------------------------------

    @Test
    void annotationQuery_pageable_correctCountAndContent() {
        // u.age > 24: Bob(25), Dave(28), Alice(30), Charlie(35) → 4 total
        Page<TestUser> page = repo.findOlderThanPaged(24, PageRequest.of(0, 2));

        assertEquals(4, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void annotationQuery_pageable_secondPage() {
        Page<TestUser> page = repo.findOlderThanPaged(24, PageRequest.of(1, 2));

        assertEquals(4, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertTrue(page.isLast());
    }

    @Test
    void modifying_delete() {
        int deleted = repo.deleteByAgeLessThan(26);
        assertEquals(2, deleted); // Bob(25), alice(22)

        em.clear();
        assertEquals(3, repo.findAll().size());
    }
}
