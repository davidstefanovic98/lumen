package io.lumen.data.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CountQueryDeriverTest {

    @Test
    void simpleSelect_derivesCountOnAlias() {
        String jpql = "SELECT u FROM TestUser u WHERE u.age > ?1";
        assertEquals("SELECT COUNT(u) FROM TestUser u WHERE u.age > ?1", CountQueryDeriver.derive(jpql));
    }

    @Test
    void orderBy_isStrippedBeforeDeriving() {
        String jpql = "SELECT u FROM TestUser u WHERE u.age > ?1 ORDER BY u.name ASC";
        assertEquals("SELECT COUNT(u) FROM TestUser u WHERE u.age > ?1", CountQueryDeriver.derive(jpql));
    }

    @Test
    void subqueryInSelectClause_doesNotStopAtTheSubquerysFrom() {
        // Regression: the previous regex-based reluctant match stopped at the FIRST "FROM" it
        // saw, which was inside the SELECT-clause subquery, producing malformed JPQL.
        String jpql = "SELECT o, (SELECT COUNT(i) FROM OrderItem i WHERE i.order = o) "
                + "FROM Order o WHERE o.status = 'OPEN'";
        assertEquals("SELECT COUNT(o) FROM Order o WHERE o.status = 'OPEN'", CountQueryDeriver.derive(jpql));
    }

    @Test
    void nestedSubqueriesInSelectClause_stillFindsTopLevelFrom() {
        String jpql = "SELECT o, (SELECT MAX(i.price) FROM OrderItem i WHERE i.order = "
                + "(SELECT o2 FROM Order o2 WHERE o2.id = o.id)) FROM Order o";
        assertEquals("SELECT COUNT(o) FROM Order o", CountQueryDeriver.derive(jpql));
    }

    @Test
    void stringLiteralContainingFrom_isNotMistakenForTheKeyword() {
        String jpql = "SELECT u FROM TestUser u WHERE u.name = 'FROM'";
        assertEquals("SELECT COUNT(u) FROM TestUser u WHERE u.name = 'FROM'", CountQueryDeriver.derive(jpql));
    }

    @Test
    void noAliasAfterEntityName_fallsBackToDefaultAlias() {
        String jpql = "SELECT e FROM TestUser";
        assertEquals("SELECT COUNT(e) FROM TestUser", CountQueryDeriver.derive(jpql));
    }

    @Test
    void noTopLevelFrom_throwsClearError() {
        assertThrows(IllegalArgumentException.class, () -> CountQueryDeriver.derive("SELECT 1"));
    }
}