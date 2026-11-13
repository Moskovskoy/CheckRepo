package academy.incidentradar.filter;

import academy.incidentradar.model.LogRecord;
import java.time.LocalDate;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public final class DateFilter {

    private DateFilter() {}

    public static Predicate<LogRecord> of(@Nullable LocalDate from, @Nullable LocalDate to) {
        return r -> {
            LocalDate d = r.date();
            return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
        };
    }
}
