package io.lumen.data.query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PartTree {
    private final List<OrPart> orParts = new ArrayList<>();

    public PartTree(String source) {
        // Split by "Or" to handle the highest level of logical separation
        String[] split = source.split("Or(?=[A-Z])");
        for (String orSource : split) {
            orParts.add(new OrPart(orSource));
        }
    }

    public List<OrPart> getOrParts() {
        return orParts;
    }

    /**
     * Represents a collection of parts joined by "And"
     */
    public static class OrPart {
        private final List<Part> andParts = new ArrayList<>();

        public OrPart(String source) {
            String[] split = source.split("And(?=[A-Z])");
            for (String andSource : split) {
                andParts.add(new Part(andSource));
            }
        }

        public List<Part> getAndParts() {
            return andParts;
        }
    }
}