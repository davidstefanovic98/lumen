package io.lumen.gleam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GleamTest {

    private StandardEvaluationContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new StandardEvaluationContext();
    }

    // ── literals ──────────────────────────────────────────────────────────────

    @Nested class Literals {
        @Test void string()  { assertEquals("hello", eval("'hello'")); }
        @Test void integer() { assertEquals(42,      eval("42")); }
        @Test void longVal() { assertEquals(9999999999L, eval("9999999999L")); }
        @Test void dbl()     { assertEquals(3.14,    eval("3.14")); }
        @Test void trueVal() { assertEquals(true,    eval("true")); }
        @Test void falseVal(){ assertEquals(false,   eval("false")); }
        @Test void nullVal() { assertNull(            eval("null")); }
    }

    // ── variables ─────────────────────────────────────────────────────────────

    @Nested class Variables {
        @Test void present() {
            ctx.setVariable("name", "David");
            assertEquals("David", eval("#name"));
        }
        @Test void absent_returnsNull() {
            assertNull(eval("#missing"));
        }
        @Test void equality() {
            ctx.setVariable("x", 5);
            assertEquals(true,  eval("#x == 5"));
            assertEquals(false, eval("#x == 6"));
        }
    }

    // ── comparison ────────────────────────────────────────────────────────────

    @Nested class Comparisons {
        @Test void lt()  { assertEquals(true,  eval("3 < 5"));  assertEquals(false, eval("5 < 3")); }
        @Test void gt()  { assertEquals(true,  eval("5 > 3"));  assertEquals(false, eval("3 > 5")); }
        @Test void lte() { assertEquals(true,  eval("3 <= 3")); assertEquals(false, eval("4 <= 3")); }
        @Test void gte() { assertEquals(true,  eval("3 >= 3")); assertEquals(false, eval("2 >= 3")); }
        @Test void eq()  { assertEquals(true,  eval("'a' == 'a'")); assertEquals(false, eval("'a' == 'b'")); }
        @Test void neq() { assertEquals(true,  eval("'a' != 'b'")); assertEquals(false, eval("'a' != 'a'")); }
        @Test void nullEquality() {
            assertEquals(true,  eval("null == null"));
            assertEquals(false, eval("null != null"));
            ctx.setVariable("x", null);
            assertEquals(true,  eval("#x == null"));
        }
    }

    // ── logical operators ─────────────────────────────────────────────────────

    @Nested class Logical {
        @Test void and_bothTrue()   { assertEquals(true,  eval("true && true"));  }
        @Test void and_oneFalse()   { assertEquals(false, eval("true && false")); }
        @Test void or_oneTrue()     { assertEquals(true,  eval("false || true")); }
        @Test void or_bothFalse()   { assertEquals(false, eval("false || false"));}
        @Test void not_true()       { assertEquals(false, eval("!true"));         }
        @Test void not_false()      { assertEquals(true,  eval("!false"));        }
        @Test void compound() {
            ctx.setVariable("role", "ADMIN");
            assertEquals(true,  eval("#role == 'ADMIN' && true"));
            assertEquals(false, eval("#role == 'USER'  || false"));
        }
    }

    // ── short-circuit ─────────────────────────────────────────────────────────

    @Test void and_shortCircuits_doesNotEvalRight() {
        // If left is false, the right side (which would throw) is never evaluated.
        ctx.registerFunction("boom", args -> { throw new RuntimeException("evaluated!"); });
        assertDoesNotThrow(() -> eval("false && boom()"));
    }

    @Test void or_shortCircuits_doesNotEvalRight() {
        ctx.registerFunction("boom", args -> { throw new RuntimeException("evaluated!"); });
        assertDoesNotThrow(() -> eval("true || boom()"));
    }

    // ── function calls ────────────────────────────────────────────────────────

    @Nested class Functions {
        @Test void noArgs() {
            ctx.registerFunction("answer", args -> 42);
            assertEquals(42, eval("answer()"));
        }
        @Test void withStringArg() {
            ctx.registerFunction("upper", args -> ((String) args[0]).toUpperCase());
            assertEquals("HELLO", eval("upper('hello')"));
        }
        @Test void withMultipleArgs() {
            ctx.registerFunction("add", args -> (int) args[0] + (int) args[1]);
            assertEquals(7, eval("add(3, 4)"));
        }
        @Test void unknownFunction_throws() {
            assertThrows(EvaluationException.class, () -> eval("unknown()"));
        }

        @Test void securityStyleHasRole() {
            List<String> roles = List.of("ROLE_ADMIN", "ROLE_USER");
            ctx.registerFunction("hasRole",
                    args -> roles.contains("ROLE_" + args[0]));
            ctx.registerFunction("isAuthenticated", args -> true);

            assertEquals(true,  eval("hasRole('ADMIN')"));
            assertEquals(false, eval("hasRole('GUEST')"));
            assertEquals(true,  eval("isAuthenticated() && hasRole('ADMIN')"));
            assertEquals(false, eval("isAuthenticated() && hasRole('GUEST')"));
        }
    }

    // ── property access ───────────────────────────────────────────────────────

    record User(Long id, String name, String role) {
        public String getRole() { return role; }   // also expose via getter
    }

    @Nested class PropertyAccess {
        @Test void rootObject() {
            ctx.setRootObject(new User(1L, "David", "ADMIN"));
            assertEquals("David", eval("name"));
            assertEquals("ADMIN", eval("role"));
        }
        @Test void onVariable() {
            ctx.setVariable("user", new User(1L, "David", "ADMIN"));
            assertEquals("David", eval("#user.name"));
            assertEquals("ADMIN", eval("#user.role"));
            assertEquals(1L,      eval("#user.id"));
        }
        @Test void nullSafe_returnsNull() {
            ctx.setVariable("user", null);
            assertNull(eval("#user.name"));
        }
        @Test void nestedChain() {
            record Address(String city) {}
            record Person(String name, Address address) {}
            ctx.setVariable("p", new Person("Alice", new Address("Berlin")));
            assertEquals("Berlin", eval("#p.address.city"));
        }
    }

    // ── parentheses ───────────────────────────────────────────────────────────

    @Test void parentheses_changesPrecedence() {
        // Without parens: false && true || true = (false && true) || true = true
        assertEquals(true,  eval("false && true || true"));
        // With parens: false && (true || true) = false
        assertEquals(false, eval("false && (true || true)"));
    }

    // ── complex expressions ───────────────────────────────────────────────────

    @Test void complexSecurityExpression() {
        ctx.setVariable("userId", 42L);
        ctx.setVariable("ownerId", 42L);
        ctx.registerFunction("hasRole", args -> "ADMIN".equals(args[0]));
        ctx.registerFunction("isAuthenticated", args -> true);

        // isAuthenticated() && (hasRole('ADMIN') || #userId == #ownerId)
        assertEquals(true, eval("isAuthenticated() && (hasRole('ADMIN') || #userId == #ownerId)"));

        ctx.setVariable("ownerId", 99L);
        assertEquals(false, eval("isAuthenticated() && !hasRole('ADMIN') && #userId != #ownerId"));
    }

    @Test void cachingParser_returnsSameInstance() {
        CachingExpressionParser parser = Gleam.newCachingParser();
        Expression e1 = parser.parse("true");
        Expression e2 = parser.parse("true");
        assertSame(e1, e2, "Same expression string should return cached instance");
        assertEquals(1, parser.cacheSize());
    }

    @Test void parseException_onInvalidInput() {
        assertThrows(ParseException.class, () -> Gleam.parse(""));
        assertThrows(ParseException.class, () -> Gleam.parse("=== invalid"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Object eval(String expression) {
        return Gleam.parse(expression).evaluate(ctx);
    }
}