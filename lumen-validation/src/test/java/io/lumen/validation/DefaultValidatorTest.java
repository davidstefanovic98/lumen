package io.lumen.validation;

import io.lumen.validation.annotation.*;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultValidatorTest {

    private final DefaultValidator validator = new DefaultValidator();

    private List<ConstraintViolation> validate(Object obj) {
        return validator.validate(obj);
    }

    private void assertViolation(List<ConstraintViolation> violations, String field) {
        assertTrue(violations.stream().anyMatch(v -> v.field().equals(field)),
                "Expected violation on field: " + field + " but got: " + violations);
    }

    private void assertNoViolation(List<ConstraintViolation> violations, String field) {
        assertFalse(violations.stream().anyMatch(v -> v.field().equals(field)),
                "Unexpected violation on field: " + field);
    }

    // -----------------------------------------------------------------------
    // Null object
    // -----------------------------------------------------------------------

    @Test
    void nullObject_returnsEmptyList() {
        assertTrue(validator.validate(null).isEmpty());
    }

    // -----------------------------------------------------------------------
    // @NotNull
    // -----------------------------------------------------------------------

    static class NotNullBean {
        @NotNull String value;
        NotNullBean(String v) { value = v; }
    }

    @Test
    void notNull_null_violation() { assertViolation(validate(new NotNullBean(null)), "value"); }

    @Test
    void notNull_nonNull_noViolation() { assertTrue(validate(new NotNullBean("x")).isEmpty()); }

    // -----------------------------------------------------------------------
    // @NotBlank
    // -----------------------------------------------------------------------

    static class NotBlankBean {
        @NotBlank String name;
        NotBlankBean(String n) { name = n; }
    }

    @Test
    void notBlank_null_violation() { assertViolation(validate(new NotBlankBean(null)), "name"); }

    @Test
    void notBlank_blank_violation() { assertViolation(validate(new NotBlankBean("  ")), "name"); }

    @Test
    void notBlank_empty_violation() { assertViolation(validate(new NotBlankBean("")), "name"); }

    @Test
    void notBlank_nonBlank_noViolation() { assertTrue(validate(new NotBlankBean("hi")).isEmpty()); }

    // -----------------------------------------------------------------------
    // @NotEmpty
    // -----------------------------------------------------------------------

    static class NotEmptyBean {
        @NotEmpty String text;
        @NotEmpty List<String> items;
        NotEmptyBean(String t, List<String> i) { text = t; items = i; }
    }

    @Test
    void notEmpty_emptyString_violation() {
        assertViolation(validate(new NotEmptyBean("", List.of())), "text");
    }

    @Test
    void notEmpty_emptyList_violation() {
        assertViolation(validate(new NotEmptyBean("x", List.of())), "items");
    }

    @Test
    void notEmpty_nonEmpty_noViolation() {
        assertTrue(validate(new NotEmptyBean("x", List.of("a"))).isEmpty());
    }

    // -----------------------------------------------------------------------
    // @Size
    // -----------------------------------------------------------------------

    static class SizeBean {
        @Size(min = 2, max = 5) String tag;
        SizeBean(String t) { tag = t; }
    }

    @Test
    void size_tooShort_violation() { assertViolation(validate(new SizeBean("a")), "tag"); }

    @Test
    void size_tooLong_violation() { assertViolation(validate(new SizeBean("toolong")), "tag"); }

    @Test
    void size_withinBounds_noViolation() { assertTrue(validate(new SizeBean("ok")).isEmpty()); }

    @Test
    void size_null_skipped() { assertTrue(validate(new SizeBean(null)).isEmpty()); }

    // -----------------------------------------------------------------------
    // @Email
    // -----------------------------------------------------------------------

    static class EmailBean {
        @Email String email;
        EmailBean(String e) { email = e; }
    }

    @Test
    void email_invalid_violation() { assertViolation(validate(new EmailBean("not-an-email")), "email"); }

    @Test
    void email_valid_noViolation() { assertTrue(validate(new EmailBean("user@example.com")).isEmpty()); }

    @Test
    void email_null_skipped() { assertTrue(validate(new EmailBean(null)).isEmpty()); }

    // -----------------------------------------------------------------------
    // @Min / @Max
    // -----------------------------------------------------------------------

    static class RangeBean {
        @Min(1) @Max(10) int count;
        RangeBean(int c) { count = c; }
    }

    @Test
    void min_belowMin_violation() { assertViolation(validate(new RangeBean(0)), "count"); }

    @Test
    void max_aboveMax_violation() { assertViolation(validate(new RangeBean(11)), "count"); }

    @Test
    void range_withinBounds_noViolation() { assertTrue(validate(new RangeBean(5)).isEmpty()); }

    // -----------------------------------------------------------------------
    // @Pattern
    // -----------------------------------------------------------------------

    static class PatternBean {
        @Pattern(regexp = "\\d{3}-\\d{4}") String phone;
        PatternBean(String p) { phone = p; }
    }

    @Test
    void pattern_noMatch_violation() { assertViolation(validate(new PatternBean("abc")), "phone"); }

    @Test
    void pattern_match_noViolation() { assertTrue(validate(new PatternBean("123-4567")).isEmpty()); }

    // -----------------------------------------------------------------------
    // @Positive / @PositiveOrZero
    // -----------------------------------------------------------------------

    static class SignBean {
        @Positive int pos;
        @PositiveOrZero int poz;
        SignBean(int p, int z) { pos = p; poz = z; }
    }

    @Test
    void positive_zero_violation() { assertViolation(validate(new SignBean(0, 0)), "pos"); }

    @Test
    void positive_negative_violation() { assertViolation(validate(new SignBean(-1, -1)), "pos"); }

    @Test
    void positiveOrZero_negative_violation() { assertViolation(validate(new SignBean(1, -1)), "poz"); }

    @Test
    void positiveOrZero_zero_noViolation() { assertNoViolation(validate(new SignBean(1, 0)), "poz"); }

    // -----------------------------------------------------------------------
    // Null field — only null-specific constraints fire
    // -----------------------------------------------------------------------

    static class MixedBean {
        @NotNull @Size(min = 3) String val;
        MixedBean(String v) { val = v; }
    }

    @Test
    void nullField_onlyNotNullFires_sizeSkipped() {
        List<ConstraintViolation> violations = validate(new MixedBean(null));
        assertViolation(violations, "val");
        assertEquals(1, violations.size(), "@Size should not fire on a null field");
    }

    // -----------------------------------------------------------------------
    // Multiple violations on one field
    // -----------------------------------------------------------------------

    static class MultiBean {
        @NotBlank @Size(min = 5) String code;
        MultiBean(String c) { code = c; }
    }

    @Test
    void multipleViolations_allCollected() {
        List<ConstraintViolation> violations = validate(new MultiBean("ab"));
        assertEquals(1, violations.size(), "@NotBlank passes; @Size should fire");
        assertViolation(violations, "code");
    }

    // -----------------------------------------------------------------------
    // Hierarchy walking — parent-class fields validated
    // -----------------------------------------------------------------------

    static class Base {
        @NotNull String id;
        Base(String id) { this.id = id; }
    }

    static class Child extends Base {
        @NotBlank String name;
        Child(String id, String name) { super(id); this.name = name; }
    }

    @Test
    void hierarchyWalking_parentFieldsValidated() {
        List<ConstraintViolation> violations = validate(new Child(null, "ok"));
        assertViolation(violations, "id");
    }

    // -----------------------------------------------------------------------
    // Record support
    // -----------------------------------------------------------------------

    record UserRecord(@NotBlank String username, @Min(18) int age) {}

    @Test
    void record_blankUsername_violation() {
        assertViolation(validate(new UserRecord("", 20)), "username");
    }

    @Test
    void record_ageBelowMin_violation() {
        assertViolation(validate(new UserRecord("alice", 16)), "age");
    }

    @Test
    void record_valid_noViolation() {
        assertTrue(validate(new UserRecord("alice", 20)).isEmpty());
    }

    // -----------------------------------------------------------------------
    // validateAndThrow integration
    // -----------------------------------------------------------------------

    @Test
    void validateAndThrow_throws_whenViolationsExist() {
        assertThrows(ValidationException.class,
                () -> validator.validateAndThrow(new NotNullBean(null)));
    }

    @Test
    void validateAndThrow_noop_whenNoViolations() {
        assertDoesNotThrow(() -> validator.validateAndThrow(new NotNullBean("ok")));
    }
}