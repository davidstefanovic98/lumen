package io.lumen.data.query;

import io.lumen.data.exception.PropertyResolveException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NameResolvingQueryParserTest {

    static class Item {
        String name;
    }

    static class Order {
        String status;
        List<Item> items;
    }

    interface OrderRepository {
        List<Order> findByStatus(String status);
        List<Order> findByItems_Name(String name);
    }

    private final NameResolvingQueryParser parser = new NameResolvingQueryParser();

    @Test
    void singleValuedProperty_resolvesNormally() throws NoSuchMethodException {
        Method method = OrderRepository.class.getMethod("findByStatus", String.class);
        QueryDescriptor descriptor = parser.parse(method, Order.class);
        assertEquals("SELECT e FROM Order e WHERE (e.status = ?1)", descriptor.query());
    }

    @Test
    void collectionValuedAssociation_throwsClearError_notGenericPropertyNotFound() throws NoSuchMethodException {
        Method method = OrderRepository.class.getMethod("findByItems_Name", String.class);

        PropertyResolveException ex = assertThrows(PropertyResolveException.class,
                () -> parser.parse(method, Order.class));

        // Must explain it's a collection-navigation limitation, not claim the property is missing
        // (it isn't — 'items' and 'items.name' both exist on the entity graph).
        assertTrue(ex.getMessage().contains("collection-valued association"));
        assertTrue(ex.getMessage().contains("items"));
        assertTrue(ex.getMessage().contains("Item"));
        assertFalse(ex.getMessage().contains("has no property path"));
    }
}