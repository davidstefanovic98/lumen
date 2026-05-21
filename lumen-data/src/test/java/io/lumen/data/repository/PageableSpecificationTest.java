package io.lumen.data.repository;

import io.lumen.core.proxy.ProxyFactory;
import io.lumen.data.pageable.*;
import io.lumen.data.query.CompositeQueryParser;
import io.lumen.data.repository.proxy.JpaRepositoryInterceptor;
import io.lumen.data.specification.Specification;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PageableSpecificationTest {

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
        em = emf.createEntityManager();
        var interceptor = new JpaRepositoryInterceptor(emf, TestUser.class, new CompositeQueryParser());
        repo = ProxyFactory.createInterfaceProxy(TestUserRepository.class, List.of(interceptor));

        em.getTransaction().begin();
        em.persist(new TestUser("Alice",   30));
        em.persist(new TestUser("Bob",     25));
        em.persist(new TestUser("Charlie", 35));
        em.persist(new TestUser("Dave",    28));
        em.persist(new TestUser("Eve",     22));
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
    // Pageable
    // -------------------------------------------------------------------------

    @Test
    void findAll_pageable_firstPage() {
        Page<TestUser> page = repo.findAll(PageRequest.of(0, 3));

        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(3, page.getContent().size());
        assertEquals(0, page.getNumber());
        assertTrue(page.isFirst());
        assertFalse(page.isLast());
        assertTrue(page.hasNext());
        assertFalse(page.hasPrevious());
    }

    @Test
    void findAll_pageable_secondPage() {
        Page<TestUser> page = repo.findAll(PageRequest.of(1, 3));

        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals(1, page.getNumber());
        assertFalse(page.isFirst());
        assertTrue(page.isLast());
        assertFalse(page.hasNext());
        assertTrue(page.hasPrevious());
    }

    @Test
    void findAll_pageable_withSort() {
        Page<TestUser> page = repo.findAll(PageRequest.of(0, 3, Sort.by("age")));

        assertEquals(3, page.getContent().size());
        assertEquals(22, page.getContent().get(0).getAge()); // Eve first
        assertEquals(25, page.getContent().get(1).getAge()); // Bob second
        assertEquals(28, page.getContent().get(2).getAge()); // Dave third
    }

    // -------------------------------------------------------------------------
    // Sort
    // -------------------------------------------------------------------------

    @Test
    void findAll_sort_ascending() {
        List<TestUser> result = repo.findAll(Sort.by("age"));

        assertEquals(5, result.size());
        assertEquals(22, result.get(0).getAge());
        assertEquals(35, result.get(4).getAge());
    }

    @Test
    void findAll_sort_descending() {
        List<TestUser> result = repo.findAll(Sort.by(Sort.Direction.DESC, "name"));

        assertEquals("Eve",   result.get(0).getName());
        assertEquals("Alice", result.get(4).getName());
    }

    @Test
    void findAll_sort_multipleProperties() {
        List<TestUser> result = repo.findAll(
                Sort.by(Sort.Order.asc("age"), Sort.Order.desc("name")));

        assertEquals("Eve", result.get(0).getName()); // age 22
        assertEquals("Bob", result.get(1).getName()); // age 25
    }

    // -------------------------------------------------------------------------
    // Specification
    // -------------------------------------------------------------------------

    @Test
    void findAll_specification_filtersCorrectly() {
        Specification<TestUser> over28 = (root, query, cb) -> cb.gt(root.get("age"), 28);
        List<TestUser> result = repo.findAll(over28);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(u -> u.getAge() > 28));
    }

    @Test
    void findAll_specification_andCombinator() {
        Specification<TestUser> over25 = (root, query, cb) -> cb.gt(root.get("age"), 25);
        Specification<TestUser> nameStartsA = (root, query, cb) -> cb.like(root.get("name"), "A%");

        List<TestUser> result = repo.findAll(over25.and(nameStartsA));

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    void findAll_specification_orCombinator() {
        Specification<TestUser> isAlice = (root, query, cb) -> cb.equal(root.get("name"), "Alice");
        Specification<TestUser> isBob   = (root, query, cb) -> cb.equal(root.get("name"), "Bob");

        List<TestUser> result = repo.findAll(isAlice.or(isBob));

        assertEquals(2, result.size());
    }

    @Test
    void findAll_specification_notCombinator() {
        Specification<TestUser> isAlice = (root, query, cb) -> cb.equal(root.get("name"), "Alice");

        List<TestUser> result = repo.findAll(Specification.not(isAlice));

        assertEquals(4, result.size());
        assertTrue(result.stream().noneMatch(u -> u.getName().equals("Alice")));
    }

    @Test
    void findAll_specificationWithPageable() {
        Specification<TestUser> over24 = (root, query, cb) -> cb.gt(root.get("age"), 24);
        Page<TestUser> page = repo.findAll(over24, PageRequest.of(0, 2));

        assertEquals(4, page.getTotalElements()); // Bob, Dave, Alice, Charlie
        assertEquals(2, page.getContent().size());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void findAll_specificationWithSort() {
        Specification<TestUser> over24 = (root, query, cb) -> cb.gt(root.get("age"), 24);
        List<TestUser> result = repo.findAll(over24, Sort.by("age"));

        assertEquals(4, result.size());
        assertEquals(25, result.get(0).getAge()); // Bob
        assertEquals(35, result.get(3).getAge()); // Charlie
    }

    // -------------------------------------------------------------------------
    // findOne / count / exists
    // -------------------------------------------------------------------------

    @Test
    void findOne_specification_found() {
        Specification<TestUser> isAlice = (root, query, cb) -> cb.equal(root.get("name"), "Alice");
        Optional<TestUser> result = repo.findOne(isAlice);

        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
    }

    @Test
    void findOne_specification_notFound() {
        Specification<TestUser> isZara = (root, query, cb) -> cb.equal(root.get("name"), "Zara");
        Optional<TestUser> result = repo.findOne(isZara);

        assertFalse(result.isPresent());
    }

    @Test
    void count_specification() {
        Specification<TestUser> over27 = (root, query, cb) -> cb.gt(root.get("age"), 27);
        assertEquals(3, repo.count(over27)); // Dave(28), Alice(30), Charlie(35)
    }

    @Test
    void exists_specification_true() {
        Specification<TestUser> isEve = (root, query, cb) -> cb.equal(root.get("name"), "Eve");
        assertTrue(repo.exists(isEve));
    }

    @Test
    void exists_specification_false() {
        Specification<TestUser> isZara = (root, query, cb) -> cb.equal(root.get("name"), "Zara");
        assertFalse(repo.exists(isZara));
    }

    // -------------------------------------------------------------------------
    // delete(Specification)
    // -------------------------------------------------------------------------

    @Test
    void delete_specification_removesMatchingRows() {
        Specification<TestUser> under28 = (root, query, cb) -> cb.lt(root.get("age"), 28);
        repo.delete(under28); // Eve(22), Bob(25)

        em.clear();
        List<TestUser> remaining = repo.findAll();
        assertEquals(3, remaining.size());
        assertTrue(remaining.stream().allMatch(u -> u.getAge() >= 28));
    }

    @Test
    void delete_specification_noMatch_deletesNothing() {
        Specification<TestUser> isZara = (root, query, cb) -> cb.equal(root.get("name"), "Zara");
        repo.delete(isZara);

        em.clear();
        assertEquals(5, repo.findAll().size());
    }
}
