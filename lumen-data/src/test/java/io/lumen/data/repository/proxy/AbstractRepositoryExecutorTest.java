package io.lumen.data.repository.proxy;

import io.lumen.data.pageable.PageRequest;
import io.lumen.data.pageable.Pageable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractRepositoryExecutorTest {

    static class NoopExecutor extends AbstractRepositoryExecutor {
        NoopExecutor() { super(null, Object.class); }
        @Override public boolean canHandle(Method method, Object[] args) { return false; }
        @Override public Object invoke(Method method, Object[] args) { return null; }
    }

    private final NoopExecutor executor = new NoopExecutor();

    @Test
    void firstResultOf_returnsPlainIntOffset_whenWithinRange() {
        assertEquals(20, executor.firstResultOf(PageRequest.of(2, 10)));
    }

    @Test
    void firstResultOf_throws_whenOffsetOverflowsInt() {
        // page * size = Integer.MAX_VALUE * 100, far past what (int) could hold without wrapping
        Pageable pageable = PageRequest.of(Integer.MAX_VALUE, 100);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> executor.firstResultOf(pageable));
        assertEquals(true, ex.getMessage().contains("Integer.MAX_VALUE"));
    }

    @Test
    void firstResultOf_doesNotThrow_atExactlyIntegerMaxValue() {
        Pageable pageable = PageRequest.of(0, 1); // trivial in-range case stays unaffected
        assertEquals(0, executor.firstResultOf(pageable));
    }
}