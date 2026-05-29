package io.lumen.web;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PathMatcherTest {

    // --- matches ---

    @Test
    void exactPath_matches() {
        assertTrue(PathMatcher.compile("/users").matches("/users"));
    }

    @Test
    void exactPath_noMatchOnDifferentPath() {
        assertFalse(PathMatcher.compile("/users").matches("/posts"));
    }

    @Test
    void pathVariable_matchesAnySegment() {
        assertTrue(PathMatcher.compile("/users/{id}").matches("/users/42"));
        assertTrue(PathMatcher.compile("/users/{id}").matches("/users/abc"));
    }

    @Test
    void pathVariable_doesNotMatchSlash() {
        assertFalse(PathMatcher.compile("/users/{id}").matches("/users/42/extra"));
    }

    @Test
    void multiplePathVariables_allMatch() {
        assertTrue(PathMatcher.compile("/a/{x}/b/{y}").matches("/a/1/b/2"));
    }

    // --- extractVariables ---

    @Test
    void extractVariables_singleVariable() {
        Map<String, String> vars = PathMatcher.compile("/users/{id}").extractVariables("/users/99");
        assertEquals("99", vars.get("id"));
    }

    @Test
    void extractVariables_multipleVariables() {
        Map<String, String> vars = PathMatcher.compile("/projects/{proj}/tasks/{task}")
                .extractVariables("/projects/alpha/tasks/42");
        assertEquals("alpha", vars.get("proj"));
        assertEquals("42", vars.get("task"));
    }

    @Test
    void extractVariables_noVariables_returnsEmptyMap() {
        Map<String, String> vars = PathMatcher.compile("/ping").extractVariables("/ping");
        assertTrue(vars.isEmpty());
    }

    // --- specificity ---

    @Test
    void literalPath_hasHigherSpecificityThanVariable() {
        PathMatcher literal = PathMatcher.compile("/users/profile");
        PathMatcher variable = PathMatcher.compile("/users/{id}");
        assertTrue(literal.getSpecificity() > variable.getSpecificity());
    }

    // --- isAmbiguous ---

    @Test
    void sameExactPaths_areAmbiguous() {
        assertTrue(PathMatcher.compile("/users").isAmbiguous(PathMatcher.compile("/users")));
    }

    @Test
    void differentLiterals_areNotAmbiguous() {
        assertFalse(PathMatcher.compile("/users").isAmbiguous(PathMatcher.compile("/posts")));
    }

    @Test
    void twoVariablePatterns_sameDepth_areAmbiguous() {
        assertTrue(PathMatcher.compile("/users/{id}").isAmbiguous(PathMatcher.compile("/users/{name}")));
    }

    @Test
    void literalVsVariable_sameSegment_notAmbiguous() {
        assertFalse(PathMatcher.compile("/users/profile").isAmbiguous(PathMatcher.compile("/users/{id}")));
    }

    @Test
    void differentLengthPaths_notAmbiguous() {
        assertFalse(PathMatcher.compile("/a/b").isAmbiguous(PathMatcher.compile("/a/b/c")));
    }
}