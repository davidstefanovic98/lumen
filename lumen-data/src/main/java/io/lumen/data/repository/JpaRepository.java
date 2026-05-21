package io.lumen.data.repository;

import io.lumen.data.specification.JpaSpecificationExecutor;

import java.util.List;

public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID>, JpaSpecificationExecutor<T> {

    void flush();

    T saveAndFlush(T entity);

    List<T> saveAllAndFlush(Iterable<T> entities);

    void deleteAllInBatch();

    void deleteAllInBatch(Iterable<T> entities);

    T getReferenceById(ID id);
}