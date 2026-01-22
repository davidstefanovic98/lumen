package io.lumen.core.component;

import io.lumen.core.annotation.Order;

import java.util.Comparator;

class OrderComparator implements Comparator<Object> {
    public static final OrderComparator INSTANCE = new OrderComparator();

    @Override
    public int compare(Object o1, Object o2) {
        return Integer.compare(getOrder(o1), getOrder(o2));
    }

    private int getOrder(Object obj) {
        if (obj == null) return Integer.MAX_VALUE;
        Order order = obj.getClass().getAnnotation(Order.class);
        return (order != null) ? order.value() : Integer.MAX_VALUE;
    }
}
