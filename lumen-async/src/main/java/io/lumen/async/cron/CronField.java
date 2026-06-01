package io.lumen.async.cron;

import java.util.TreeSet;

/**
 * One field of a 6-part cron expression.
 * Supports: * | n | n,m | n-m | *\/step | n-m/step
 */
class CronField {

    private final TreeSet<Integer> values;

    private CronField(TreeSet<Integer> values) {
        this.values = values;
    }

    static CronField parse(String expr, int min, int max) {
        TreeSet<Integer> values = new TreeSet<>();
        for (String part : expr.split(",")) {
            parsePart(part.trim(), min, max, values);
        }
        return new CronField(values);
    }

    private static void parsePart(String part, int min, int max, TreeSet<Integer> out) {
        if (part.contains("/")) {
            String[] split = part.split("/", 2);
            int step = Integer.parseInt(split[1]);
            int start = "*".equals(split[0]) ? min : Integer.parseInt(split[0].split("-")[0]);
            int end   = split[0].contains("-") ? Integer.parseInt(split[0].split("-")[1]) : max;
            for (int i = start; i <= end; i += step) addClamped(i, min, max, out);
        } else if (part.contains("-")) {
            String[] split = part.split("-", 2);
            int from = Integer.parseInt(split[0]);
            int to   = Integer.parseInt(split[1]);
            for (int i = from; i <= to; i++) addClamped(i, min, max, out);
        } else if ("*".equals(part)) {
            for (int i = min; i <= max; i++) out.add(i);
        } else {
            addClamped(Integer.parseInt(part), min, max, out);
        }
    }

    private static void addClamped(int v, int min, int max, TreeSet<Integer> out) {
        if (v >= min && v <= max) out.add(v);
    }

    boolean matches(int value) {
        return values.contains(value);
    }

    /** First value >= {@code from}, or -1 if none. */
    int nextMatch(int from) {
        Integer ceil = values.ceiling(from);
        return ceil != null ? ceil : -1;
    }

    int first() {
        return values.first();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}