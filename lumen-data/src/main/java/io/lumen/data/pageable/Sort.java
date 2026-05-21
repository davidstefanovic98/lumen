package io.lumen.data.pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Sort {

    public enum Direction { ASC, DESC }

    public static class Order {
        private final Direction direction;
        private final String property;

        public Order(Direction direction, String property) {
            this.direction = direction;
            this.property = property;
        }

        public static Order asc(String property)  { return new Order(Direction.ASC,  property); }
        public static Order desc(String property) { return new Order(Direction.DESC, property); }

        public Direction getDirection() { return direction; }
        public String getProperty()    { return property; }
    }

    private static final Sort UNSORTED = new Sort(Collections.emptyList());

    private final List<Order> orders;

    private Sort(List<Order> orders) {
        this.orders = Collections.unmodifiableList(orders);
    }

    public static Sort unsorted() { return UNSORTED; }

    public static Sort by(String... properties) {
        return by(Direction.ASC, properties);
    }

    public static Sort by(Direction direction, String... properties) {
        List<Order> orders = Arrays.stream(properties)
                .map(p -> new Order(direction, p))
                .toList();
        return new Sort(orders);
    }

    public static Sort by(Order... orders) {
        return new Sort(Arrays.asList(orders));
    }

    public List<Order> getOrders() { return orders; }

    public boolean isSorted() { return !orders.isEmpty(); }
}