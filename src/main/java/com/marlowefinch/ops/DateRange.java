package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * A closed date range for the query endpoints.
 *
 * Both bounds default to "the last 30 days ending today". {@link #resolve} validates the
 * raw request parameters (TODO-232); the record constructor does not, so repositories and
 * tests can build any range directly.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public static final int DEFAULT_DAYS = 30;
    public static final int MAX_DAYS = 366;

    public static DateRange resolve(String from, String to, Clock clock) {
        List<String> errors = new ArrayList<>();
        DateRange range = resolve(from, to, clock, errors);
        InvalidRequestException.throwIfAny(errors);
        return range;
    }

    /** Like {@link #resolve(String, String, Clock)} but collects problems so callers can report several at once. */
    public static DateRange resolve(String from, String to, Clock clock, List<String> errors) {
        LocalDate today = LocalDate.now(clock);
        LocalDate end = parse("to", to, today, errors);
        LocalDate start = parse("from", from, today.minusDays(DEFAULT_DAYS), errors);
        if (end == null || start == null) {
            return null;
        }
        if (start.isAfter(end)) {
            errors.add("from must be on or before to");
        } else if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            errors.add("the date range may span at most " + MAX_DAYS + " days");
        }
        return new DateRange(start, end);
    }

    private static LocalDate parse(String name, String value, LocalDate fallback, List<String> errors) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            errors.add(name + " must be an ISO date (YYYY-MM-DD)");
            return null;
        }
    }
}
