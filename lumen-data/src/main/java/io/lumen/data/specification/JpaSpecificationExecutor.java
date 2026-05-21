package io.lumen.data.specification;

import io.lumen.data.pageable.Page;
import io.lumen.data.pageable.Pageable;
import io.lumen.data.pageable.Sort;

import java.util.List;
import java.util.Optional;

public interface JpaSpecificationExecutor<T> {

    Optional<T> findOne(Specification<T> spec);

    List<T> findAll(Specification<T> spec);

    Page<T> findAll(Specification<T> spec, Pageable pageable);

    List<T> findAll(Specification<T> spec, Sort sort);

    long count(Specification<T> spec);

    boolean exists(Specification<T> spec);

    void delete(Specification<T> spec);
}