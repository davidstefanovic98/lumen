package io.lumen.async.cron;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CronExpressionTest {

    private static ZonedDateTime at(int year, int month, int day, int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("UTC"));
    }

    private static ZonedDateTime next(String cron, ZonedDateTime from) {
        return CronExpression.parse(cron).nextExecution(from);
    }

    // ── every-second / every-minute / every-hour ──────────────────────────────

    @Test
    void everySecond_advancesOneSecond() {
        ZonedDateTime from = at(2026, 1, 1, 0, 0, 0);
        assertEquals(at(2026, 1, 1, 0, 0, 1), next("* * * * * *", from));
    }

    @Test
    void everyMinute_atSecondZero() {
        ZonedDateTime from = at(2026, 1, 1, 0, 0, 30);
        assertEquals(at(2026, 1, 1, 0, 1, 0), next("0 * * * * *", from));
    }

    @Test
    void everyHour_atMinuteAndSecondZero() {
        ZonedDateTime from = at(2026, 6, 15, 10, 30, 0);
        assertEquals(at(2026, 6, 15, 11, 0, 0), next("0 0 * * * *", from));
    }

    // ── fixed second / minute / hour ──────────────────────────────────────────

    @Test
    void fixedSecond_withinSameMinute() {
        ZonedDateTime from = at(2026, 1, 1, 0, 0, 10);
        assertEquals(at(2026, 1, 1, 0, 0, 30), next("30 * * * * *", from));
    }

    @Test
    void fixedSecond_wrapsToNextMinute() {
        ZonedDateTime from = at(2026, 1, 1, 0, 0, 45);
        assertEquals(at(2026, 1, 1, 0, 1, 30), next("30 * * * * *", from));
    }

    @Test
    void fixedMinute_withinSameHour() {
        ZonedDateTime from = at(2026, 1, 1, 10, 10, 0);
        assertEquals(at(2026, 1, 1, 10, 30, 0), next("0 30 * * * *", from));
    }

    @Test
    void fixedMinute_wrapsToNextHour() {
        ZonedDateTime from = at(2026, 1, 1, 10, 45, 0);
        assertEquals(at(2026, 1, 1, 11, 30, 0), next("0 30 * * * *", from));
    }

    // ── step expressions ──────────────────────────────────────────────────────

    @Test
    void everyFiveMinutes() {
        ZonedDateTime from = at(2026, 1, 1, 0, 3, 0);
        assertEquals(at(2026, 1, 1, 0, 5, 0), next("0 */5 * * * *", from));
    }

    @Test
    void everyFiveMinutes_wrapsHour() {
        ZonedDateTime from = at(2026, 1, 1, 0, 58, 0);
        assertEquals(at(2026, 1, 1, 1, 0, 0), next("0 */5 * * * *", from));
    }

    // ── midnight daily ────────────────────────────────────────────────────────

    @Test
    void dailyAtMidnight() {
        ZonedDateTime from = at(2026, 6, 1, 12, 0, 0);
        assertEquals(at(2026, 6, 2, 0, 0, 0), next("0 0 0 * * *", from));
    }

    @Test
    void dailyAt9am() {
        ZonedDateTime from = at(2026, 6, 1, 10, 0, 0);
        assertEquals(at(2026, 6, 2, 9, 0, 0), next("0 0 9 * * *", from));
    }

    @Test
    void dailyAt9am_beforeNine_firesThisDay() {
        ZonedDateTime from = at(2026, 6, 1, 8, 0, 0);
        assertEquals(at(2026, 6, 1, 9, 0, 0), next("0 0 9 * * *", from));
    }

    // ── day of month ─────────────────────────────────────────────────────────

    @Test
    void firstOfMonth_midnight() {
        ZonedDateTime from = at(2026, 6, 5, 0, 0, 0);
        assertEquals(at(2026, 7, 1, 0, 0, 0), next("0 0 0 1 * *", from));
    }

    @Test
    void lastDayOfYear() {
        ZonedDateTime from = at(2026, 12, 30, 0, 0, 0);
        assertEquals(at(2026, 12, 31, 0, 0, 0), next("0 0 0 31 * *", from));
    }

    // ── month restriction ─────────────────────────────────────────────────────

    @Test
    void januaryFirst_midnight() {
        ZonedDateTime from = at(2026, 6, 1, 0, 0, 0);
        assertEquals(at(2027, 1, 1, 0, 0, 0), next("0 0 0 1 1 *", from));
    }

    @Test
    void everyMonthOnFirst() {
        ZonedDateTime from = at(2026, 1, 15, 0, 0, 0);
        assertEquals(at(2026, 2, 1, 0, 0, 0), next("0 0 0 1 * *", from));
    }

    // ── day of week ───────────────────────────────────────────────────────────

    @Test
    void everyMondayAtNine() {
        // 2026-06-01 is a Monday
        ZonedDateTime from = at(2026, 6, 1, 10, 0, 0); // after 9am Monday
        ZonedDateTime result = next("0 0 9 * * 1", from);
        assertEquals(1, result.getDayOfWeek().getValue(), "result must be a Monday");
        assertEquals(at(2026, 6, 8, 9, 0, 0), result);
    }

    @Test
    void everySundayAtMidnight_sunday0() {
        // 2026-06-07 is a Sunday
        ZonedDateTime from = at(2026, 6, 1, 0, 0, 0); // Monday
        ZonedDateTime result = next("0 0 0 * * 0", from); // 0 = Sunday
        assertEquals(7, result.getDayOfWeek().getValue(), "result must be Sunday");
    }

    @Test
    void everySundayAtMidnight_sunday7() {
        ZonedDateTime from = at(2026, 6, 1, 0, 0, 0);
        ZonedDateTime result = next("0 0 0 * * 7", from); // 7 = Sunday (alias)
        assertEquals(7, result.getDayOfWeek().getValue());
    }

    @Test
    void weekdays_monday_to_friday() {
        // 2026-06-05 is Friday
        ZonedDateTime from = at(2026, 6, 5, 9, 0, 0);
        ZonedDateTime result = next("0 0 9 * * 1-5", from);
        // next weekday at 9am after Friday 9am is Monday
        assertEquals(at(2026, 6, 8, 9, 0, 0), result);
        assertEquals(1, result.getDayOfWeek().getValue());
    }

    // ── list expressions ──────────────────────────────────────────────────────

    @Test
    void minuteList() {
        ZonedDateTime from = at(2026, 1, 1, 0, 14, 0);
        assertEquals(at(2026, 1, 1, 0, 15, 0), next("0 15,30,45 * * * *", from));
    }

    @Test
    void hourList() {
        ZonedDateTime from = at(2026, 1, 1, 10, 0, 0);
        assertEquals(at(2026, 1, 1, 14, 0, 0), next("0 0 8,14,20 * * *", from));
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    void parse_wrongFieldCount_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> CronExpression.parse("* * * * *")); // 5 fields instead of 6
    }

    @Test
    void parse_invalidValue_throws() {
        assertThrows(Exception.class,
                () -> CronExpression.parse("abc * * * * *"));
    }

    // ── next is always strictly after from ───────────────────────────────────

    @Test
    void nextIsAlwaysAfterFrom() {
        String[] expressions = {
            "* * * * * *",
            "0 * * * * *",
            "0 0 * * * *",
            "0 0 0 * * *",
            "0 0 9 * * 1-5"
        };
        ZonedDateTime from = at(2026, 6, 1, 9, 0, 0);
        for (String expr : expressions) {
            ZonedDateTime result = next(expr, from);
            assertTrue(result.isAfter(from),
                    expr + " returned " + result + " which is not after " + from);
        }
    }
}