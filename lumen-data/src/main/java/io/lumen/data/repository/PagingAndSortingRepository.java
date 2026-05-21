package io.lumen.data.repository;

import io.lumen.data.pageable.Page;
import io.lumen.data.pageable.Pageable;
import io.lumen.data.pageable.Sort;

import java.util.List;

public interface PagingAndSortingRepository<T, ID> extends CrudRepository<T, ID> {

    List<T> findAll(Sort sort);

    Page<T> findAll(Pageable pageable);
}