package io.lumen.web.filter;

import io.lumen.core.annotation.Order;
import jakarta.servlet.Filter;

public interface LumenFilter extends Filter {

    /**
     * Falls back to the class-level {@link Order} annotation when not overridden, so a filter
     * can express its order either way without the two mechanisms silently disagreeing.
     */
    default int getOrder() {
        Order order = getClass().getAnnotation(Order.class);
        return order != null ? order.value() : 0;
    }
}
