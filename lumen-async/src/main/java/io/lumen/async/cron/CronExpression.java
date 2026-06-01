package io.lumen.async.cron;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Parses and evaluates 6-field cron expressions:
 * {@code <second> <minute> <hour> <day-of-month> <month> <day-of-week>}
 *
 * <p>Supported syntax per field:
 * <ul>
 *   <li>{@code *}        — every value
 *   <li>{@code n}        — specific value
 *   <li>{@code n,m}      — list
 *   <li>{@code n-m}      — range (inclusive)
 *   <li>{@code *\/step}  — every step-th value
 *   <li>{@code n-m/step} — range with step
 * </ul>
 *
 * <p>Day-of-week: 0 and 7 both represent Sunday.
 * Day-of-month and day-of-week interact with OR when both are restricted
 * (i.e., either may satisfy the constraint independently).
 */
public class CronExpression {

    private static final int MAX_SEARCH_DAYS = 366 * 4; // 4 years

    private final CronField seconds;
    private final CronField minutes;
    private final CronField hours;
    private final CronField daysOfMonth;
    private final CronField months;
    private final CronField daysOfWeek;
    private final boolean anyDayOfMonth;
    private final boolean anyDayOfWeek;
    private final String original;

    private CronExpression(CronField seconds, CronField minutes, CronField hours,
                           CronField daysOfMonth, CronField months, CronField daysOfWeek,
                           boolean anyDayOfMonth, boolean anyDayOfWeek, String original) {
        this.seconds      = seconds;
        this.minutes      = minutes;
        this.hours        = hours;
        this.daysOfMonth  = daysOfMonth;
        this.months       = months;
        this.daysOfWeek   = daysOfWeek;
        this.anyDayOfMonth = anyDayOfMonth;
        this.anyDayOfWeek  = anyDayOfWeek;
        this.original     = original;
    }

    /**
     * Parses a 6-field cron expression.
     * @throws IllegalArgumentException if the expression is malformed
     */
    public static CronExpression parse(String expression) {
        String[] parts = expression.trim().split("\\s+");
        if (parts.length != 6) {
            throw new IllegalArgumentException(
                "Cron expression must have exactly 6 fields " +
                "(second minute hour day-of-month month day-of-week). Got: \"" + expression + "\"");
        }
        return new CronExpression(
            CronField.parse(parts[0], 0, 59),  // seconds
            CronField.parse(parts[1], 0, 59),  // minutes
            CronField.parse(parts[2], 0, 23),  // hours
            CronField.parse(parts[3], 1, 31),  // day-of-month
            CronField.parse(parts[4], 1, 12),  // month
            CronField.parse(parts[5], 0, 7),   // day-of-week (0 and 7 = Sunday)
            "*".equals(parts[3]),
            "*".equals(parts[5]),
            expression
        );
    }

    /**
     * Returns the next execution time strictly after {@code after}.
     */
    public ZonedDateTime nextExecution(ZonedDateTime after) {
        ZonedDateTime t = after.plusSeconds(1).truncatedTo(ChronoUnit.SECONDS);

        for (int day = 0; day < MAX_SEARCH_DAYS; day++) {
            // Month
            int month = t.getMonthValue();
            if (!months.matches(month)) {
                int next = months.nextMatch(month + 1);
                if (next == -1) {
                    t = t.plusYears(1).withMonth(months.first())
                          .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                } else {
                    t = t.withMonth(next).withDayOfMonth(1)
                          .withHour(0).withMinute(0).withSecond(0);
                }
                continue;
            }

            // Day (day-of-month OR day-of-week when both are restricted)
            if (!dayMatches(t)) {
                t = t.plusDays(1).withHour(0).withMinute(0).withSecond(0);
                continue;
            }

            // Hour
            int hour = t.getHour();
            if (!hours.matches(hour)) {
                int next = hours.nextMatch(hour + 1);
                if (next == -1) {
                    t = t.plusDays(1).withHour(hours.first()).withMinute(0).withSecond(0);
                } else {
                    t = t.withHour(next).withMinute(0).withSecond(0);
                }
                continue;
            }

            // Minute
            int minute = t.getMinute();
            if (!minutes.matches(minute)) {
                int next = minutes.nextMatch(minute + 1);
                if (next == -1) {
                    int nextHour = hours.nextMatch(hour + 1);
                    if (nextHour == -1) {
                        t = t.plusDays(1).withHour(hours.first())
                              .withMinute(minutes.first()).withSecond(seconds.first());
                    } else {
                        t = t.withHour(nextHour).withMinute(minutes.first()).withSecond(seconds.first());
                    }
                } else {
                    t = t.withMinute(next).withSecond(seconds.first());
                }
                continue;
            }

            // Second
            int second = t.getSecond();
            if (!seconds.matches(second)) {
                int next = seconds.nextMatch(second + 1);
                if (next == -1) {
                    int nextMinute = minutes.nextMatch(minute + 1);
                    if (nextMinute == -1) {
                        int nextHour = hours.nextMatch(hour + 1);
                        if (nextHour == -1) {
                            t = t.plusDays(1).withHour(hours.first())
                                  .withMinute(minutes.first()).withSecond(seconds.first());
                        } else {
                            t = t.withHour(nextHour).withMinute(minutes.first()).withSecond(seconds.first());
                        }
                    } else {
                        t = t.withMinute(nextMinute).withSecond(seconds.first());
                    }
                } else {
                    t = t.withSecond(next);
                }
                continue;
            }

            return t;
        }

        throw new IllegalStateException(
            "No next execution found within " + MAX_SEARCH_DAYS + " days for: \"" + original + "\"");
    }

    private boolean dayMatches(ZonedDateTime t) {
        int dom = t.getDayOfMonth();
        // Java DayOfWeek: Mon=1 … Sun=7; cron dow: Sun=0, Mon=1 … Sat=6 (7=Sun alias)
        int javaDow = t.getDayOfWeek().getValue();
        int cronDow = javaDow % 7; // Sun: 7 % 7 = 0

        if (anyDayOfMonth && anyDayOfWeek) return true;
        if (anyDayOfMonth) return dowMatches(cronDow);
        if (anyDayOfWeek)  return daysOfMonth.matches(dom);
        // Both restricted: OR semantics (matches either condition)
        return daysOfMonth.matches(dom) || dowMatches(cronDow);
    }

    /** Checks cron dow value, treating 0 and 7 as equivalent (both = Sunday). */
    private boolean dowMatches(int cronDow) {
        return daysOfWeek.matches(cronDow) || (cronDow == 0 && daysOfWeek.matches(7));
    }

    @Override
    public String toString() {
        return original;
    }
}