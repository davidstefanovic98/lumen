package io.lumen.data.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@FunctionalInterface
public interface Specification<T> {

    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);

    default Specification<T> and(Specification<T> other) {
        return (root, query, cb) -> {
            Predicate left  = this.toPredicate(root, query, cb);
            Predicate right = other.toPredicate(root, query, cb);
            if (left == null)  return right;
            if (right == null) return left;
            return cb.and(left, right);
        };
    }

    default Specification<T> or(Specification<T> other) {
        return (root, query, cb) -> {
            Predicate left  = this.toPredicate(root, query, cb);
            Predicate right = other.toPredicate(root, query, cb);
            if (left == null)  return right;
            if (right == null) return left;
            return cb.or(left, right);
        };
    }

    static <T> Specification<T> not(Specification<T> spec) {
        return (root, query, cb) -> {
            Predicate p = spec.toPredicate(root, query, cb);
            return p == null ? null : cb.not(p);
        };
    }

    static <T> Specification<T> where(Specification<T> spec) {
        return spec == null ? (root, query, cb) -> null : spec;
    }
}