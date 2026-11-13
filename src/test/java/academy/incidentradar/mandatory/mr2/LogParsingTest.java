package academy.incidentradar.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.LogRecord;
import academy.incidentradar.parser.LogParser;
import academy.incidentradar.support.CliRunner;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: разбор строки лога. */
@DisplayName("MR2. Парсер строки лога")
class LogParsingTest {

    private static final String REFERENCE_LINE =
            "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3\"";

    /** Структурно валидная строка, но дата не парсится — второй вид поломки из samples/logs/README.md. */
    private static final String BROKEN_DATE_LINE =
            "93.180.71.3 - - [НЕ-ДАТА] \"GET /api/v1/cards HTTP/1.1\" 200 512 \"-\" \"curl/8.7.1\"";

    private final LogParser parser = new LogParser();

    @Test
    @DisplayName("Эталонная строка разбирается на корректные поля")
    void referenceLineIsParsedCorrectly() {
        Optional<LogRecord> parsed = parser.parse(REFERENCE_LINE);

        assertThat(parsed).isPresent();
        LogRecord record = parsed.orElseThrow();

        assertThat(record.remoteAddr()).isEqualTo("93.180.71.3");
        assertThat(record.remoteUser()).as("'-' в логе превращается в null").isNull();
        assertThat(record.date()).isEqualTo(LocalDate.of(2015, 5, 17));
        assertThat(record.status()).isEqualTo(304);
        assertThat(record.bodyBytesSent()).isZero();
        assertThat(record.httpReferer()).isEqualTo("-");
        assertThat(record.httpUserAgent()).isEqualTo("Debian APT-HTTP/1.3");
    }

    @Test
    @DisplayName("Метод, ресурс и протокол разбираются в три отдельных поля")
    void requestIsSplitIntoThreeFields() {
        LogRecord record = parser.parse(REFERENCE_LINE).orElseThrow();

        assertThat(record.method()).isEqualTo("GET");
        assertThat(record.resource()).isEqualTo("/downloads/product_1");
        assertThat(record.protocol()).isEqualTo("HTTP/1.1");
    }

    @Test
    @DisplayName("Битая строка пропускается с warning'ом, программа не падает")
    void malformedLineIsSkippedWithWarning() {
        // Оба вида поломки возвращают Optional.empty(), не бросают.
        assertThat(parser.parse("это вообще не лог")).isEmpty();
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse(BROKEN_DATE_LINE)).isEmpty();

        // End-to-end на реальном файле: 13 строк, 7 валидных, 6 битых.
        var result = CliRunner.run("--path", "samples/logs/malformed.log");

        assertThat(result.exitCode())
                .as("Битые строки не должны валить программу%n%s", result)
                .isZero();

        long warnCount = result.stderr()
                .lines()
                .filter(line -> line.contains("skipped malformed log line"))
                .count();
        assertThat(warnCount)
                .as("13 строк, 7 валидных, 6 битых — ровно 6 WARN'ов в stderr%n%s", result)
                .isEqualTo(6);
    }
}
