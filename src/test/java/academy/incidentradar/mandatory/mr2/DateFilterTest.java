package academy.incidentradar.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.filter.DateFilter;
import academy.incidentradar.parser.LogParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: фильтрация по датам. */
@DisplayName("MR2. Фильтр по датам")
class DateFilterTest {

    private static final Path LOGS = Path.of("samples", "logs");
    private static final List<String> FILES = List.of("access-2025-03-01.log", "access-2025-03-02.log");

    private final LogParser parser = new LogParser();

    @Test
    @DisplayName("Без --from и --to обрабатываются все записи")
    void noFilterKeepsEverything() {
        assertThat(count(null, null)).isEqualTo(3000);
    }

    @Test
    @DisplayName("Только --from: открытая правая граница")
    void onlyFromIsOpenEnded() {
        assertThat(count(LocalDate.of(2025, 3, 2), null))
                .as("только записи 2 марта")
                .isEqualTo(1500);
    }

    @Test
    @DisplayName("Только --to: открытая левая граница")
    void onlyToIsOpenEnded() {
        assertThat(count(null, LocalDate.of(2025, 3, 1)))
                .as("только записи 1 марта")
                .isEqualTo(1500);
    }

    @Test
    @DisplayName("Обе границы: период включительно")
    void bothBoundsAreInclusive() {
        assertThat(count(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 2)))
                .as("обе даты включительно — 3000, а не 2999 или 3001")
                .isEqualTo(3000);
    }

    private long count(LocalDate from, LocalDate to) {
        var filter = DateFilter.of(from, to);
        return allLines().stream()
                .map(parser::parse)
                .flatMap(Optional::stream)
                .filter(filter)
                .count();
    }

    private static List<String> allLines() {
        List<String> result = new ArrayList<>();
        for (String name : FILES) {
            try {
                result.addAll(Files.readAllLines(LOGS.resolve(name), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return result;
    }
}
