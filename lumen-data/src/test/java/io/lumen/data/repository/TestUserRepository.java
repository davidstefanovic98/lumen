package io.lumen.data.repository;

import io.lumen.data.annotation.Modifying;
import io.lumen.data.annotation.Param;
import io.lumen.data.annotation.Query;
import io.lumen.data.pageable.Page;
import io.lumen.data.pageable.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TestUserRepository extends JpaRepository<TestUser, Long> {

    // --- Basic operators ---
    List<TestUser> findByName(String name);
    List<TestUser> findByAgeGreaterThan(int age);
    List<TestUser> findByAgeLessThan(int age);
    List<TestUser> findByAgeGreaterThanEqual(int age);
    List<TestUser> findByAgeLessThanEqual(int age);
    List<TestUser> findByAgeBetween(int min, int max);
    List<TestUser> findByAgeIn(Collection<Integer> ages);
    List<TestUser> findByNameNot(String name);

    // --- LIKE variants ---
    List<TestUser> findByNameContaining(String part);
    List<TestUser> findByNameStartingWith(String prefix);
    List<TestUser> findByNameEndingWith(String suffix);

    // --- IgnoreCase variants ---
    List<TestUser> findByNameIgnoreCase(String name);
    List<TestUser> findByNameContainingIgnoreCase(String part);

    // --- AND / OR combinations ---
    List<TestUser> findByNameAndAge(String name, int age);
    List<TestUser> findByNameOrAge(String name, int age);

    // --- OrderBy suffix ---
    List<TestUser> findByAgeGreaterThanOrderByNameAsc(int age);
    List<TestUser> findByNameContainingOrderByAgeDesc(String part);

    // --- count / exists / delete prefixes ---
    long countByAgeGreaterThan(int age);
    boolean existsByName(String name);
    long deleteByName(String name);

    // --- @Query with positional parameters ---
    @Query("SELECT u FROM TestUser u WHERE u.age > ?1 AND u.age < ?2")
    List<TestUser> findInAgeRange(int min, int max);

    // --- @Query with named parameters ---
    @Query("SELECT u FROM TestUser u WHERE u.name = :name AND u.age > :minAge")
    Optional<TestUser> findByNameAndMinAge(@Param("name") String name, @Param("minAge") int minAge);

    // --- @Modifying UPDATE ---
    @Modifying
    @Query("UPDATE TestUser u SET u.age = :newAge WHERE u.name = :name")
    int updateAgeByName(@Param("name") String name, @Param("newAge") int newAge);

    // --- Pageable overloads ---
    Page<TestUser> findByAgeGreaterThan(int age, Pageable pageable);

    @Query(value = "SELECT u FROM TestUser u WHERE u.age > ?1",
           countQuery = "SELECT COUNT(u) FROM TestUser u WHERE u.age > ?1")
    Page<TestUser> findOlderThanPaged(int age, Pageable pageable);

    // --- @Modifying DELETE ---
    @Modifying
    @Query("DELETE FROM TestUser u WHERE u.age < :maxAge")
    int deleteByAgeLessThan(@Param("maxAge") int maxAge);
}
