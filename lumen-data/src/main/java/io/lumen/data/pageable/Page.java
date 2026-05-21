package io.lumen.data.pageable;

import java.util.List;

public interface Page<T> {
    List<T> getContent();
    long getTotalElements();
    int getTotalPages();
    int getNumber();
    int getSize();
    boolean hasContent();
    boolean isFirst();
    boolean isLast();
    boolean hasNext();
    boolean hasPrevious();
}