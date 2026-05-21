package io.lumen.data.pageable;

public class PageRequest implements Pageable {

    private final int page;
    private final int size;
    private final Sort sort;

    private PageRequest(int page, int size, Sort sort) {
        if (page < 0)  throw new IllegalArgumentException("Page index must not be negative");
        if (size < 1)  throw new IllegalArgumentException("Page size must be greater than zero");
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, Sort.unsorted());
    }

    public static PageRequest of(int page, int size, Sort sort) {
        return new PageRequest(page, size, sort);
    }

    public static PageRequest of(int page, int size, Sort.Direction direction, String... properties) {
        return new PageRequest(page, size, Sort.by(direction, properties));
    }

    @Override public int getPageNumber() { return page; }
    @Override public int getPageSize()   { return size; }
    @Override public long getOffset()    { return (long) page * size; }
    @Override public Sort getSort()      { return sort; }
    @Override public boolean isPaged()   { return true; }
}