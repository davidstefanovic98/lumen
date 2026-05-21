package io.lumen.data.pageable;

public interface Pageable {
    int getPageNumber();
    int getPageSize();
    long getOffset();
    Sort getSort();
    boolean isPaged();

    static Pageable unpaged() {
        return new Pageable() {
            @Override public int getPageNumber() { return 0; }
            @Override public int getPageSize()   { return Integer.MAX_VALUE; }
            @Override public long getOffset()    { return 0; }
            @Override public Sort getSort()      { return Sort.unsorted(); }
            @Override public boolean isPaged()   { return false; }
        };
    }
}