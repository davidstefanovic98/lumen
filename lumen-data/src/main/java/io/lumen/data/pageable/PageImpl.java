package io.lumen.data.pageable;

import java.util.Collections;
import java.util.List;

public class PageImpl<T> implements Page<T> {

    private final List<T> content;
    private final Pageable pageable;
    private final long total;

    public PageImpl(List<T> content, Pageable pageable, long total) {
        this.content  = Collections.unmodifiableList(content);
        this.pageable = pageable;
        this.total    = total;
    }

    @Override public List<T> getContent()       { return content; }
    @Override public long getTotalElements()     { return total; }
    @Override public int getSize()               { return pageable.isPaged() ? pageable.getPageSize() : content.size(); }
    @Override public int getNumber()             { return pageable.isPaged() ? pageable.getPageNumber() : 0; }
    @Override public boolean hasContent()        { return !content.isEmpty(); }

    @Override
    public int getTotalPages() {
        return getSize() == 0 ? 1 : (int) Math.ceil((double) total / getSize());
    }

    @Override public boolean isFirst()      { return getNumber() == 0; }
    @Override public boolean isLast()       { return getNumber() + 1 >= getTotalPages(); }
    @Override public boolean hasNext()      { return !isLast(); }
    @Override public boolean hasPrevious()  { return !isFirst(); }
}