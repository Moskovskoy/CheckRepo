package academy.incidentradar.parser;

import academy.incidentradar.model.LogRecord;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("MagicNumber")
public class LogParser {

    private static final Pattern LINE = Pattern.compile(
            "^(\\S+) - (\\S+) \\[([^\\]]+)] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\d+) \"([^\"]*)\" \"([^\"]*)\"$");

    private static final DateTimeFormatter TIME = new DateTimeFormatterBuilder()
            .appendPattern("dd/MMM/yyyy:HH:mm:ss Z")
            .toFormatter(Locale.ENGLISH);

    public Optional<LogRecord> parse(String line) {
        Matcher m = LINE.matcher(line);

        if (!m.matches()) {
            return Optional.empty();
        }

        ZonedDateTime ts;
        try {
            ts = ZonedDateTime.parse(m.group(3), TIME);
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }

        return Optional.of(new LogRecord(
                m.group(1),
                "-".equals(m.group(2)) ? null : m.group(2),
                ts,
                m.group(4),
                m.group(5),
                m.group(6),
                Integer.parseInt(m.group(7)),
                Long.parseLong(m.group(8)),
                m.group(9),
                m.group(10)));
    }
}
