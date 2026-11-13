package academy.incidentradar.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.LogRecord;
import academy.incidentradar.parser.LogParser;
import academy.incidentradar.stats.ResourceCount;
import academy.incidentradar.stats.ResponseSizeStats;
import academy.incidentradar.stats.Statistics;
import academy.incidentradar.stats.StatisticsCollector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: подсчёт статистики. */
@DisplayName("MR2. Статистика")
class StatisticsTest {

    private static final Path LOGS = Path.of("samples", "logs");
    private static final LogParser PARSER = new LogParser();

    @Test
    @DisplayName("Распределение кодов: 200,200,404,500,500,500 -> 200:2, 404:1, 500:3")
    void responseCodesAreCounted() {
        Statistics stats = collect("reference-codes.log");

        assertThat(stats.totalRequests()).isEqualTo(6);
        assertThat(stats.responseCodes()).containsExactly(Map.entry(200, 2L), Map.entry(404, 1L), Map.entry(500, 3L));
    }

    @Test
    @DisplayName("Top-10 ресурсов отсортирован по убыванию количества")
    void topResourcesAreSortedDescending() {
        Statistics stats = collect("access-2025-03-01.log", "access-2025-03-02.log");

        List<ResourceCount> top = stats.resources();
        assertThat(top).hasSize(10);
        assertThat(top.get(0).resource()).isEqualTo("/api/v1/accounts");
        assertThat(top.get(0).count()).isEqualTo(581L);

        for (int i = 1; i < top.size(); i++) {
            assertThat(top.get(i).count())
                    .as("top[%d].count <= top[%d].count", i, i - 1)
                    .isLessThanOrEqualTo(top.get(i - 1).count());
        }
    }

    @Test
    @DisplayName("Размеры 100..500: average 300, max 500, p95 500")
    void responseSizeStatistics() {
        ResponseSizeStats size = collect("reference-sizes.log").responseSizeInBytes();

        assertThat(size.average()).isEqualTo(300.0);
        assertThat(size.max()).isEqualTo(500L);
        assertThat(size.p95()).isEqualTo(500L);
    }

    @Test
    @DisplayName("Дробные значения округляются до 2 знаков после запятой")
    void fractionalValuesAreRoundedToTwoDecimals() {
        // Синтетика: 1, 2, 2 -> average = 1.666..., ожидаем 1.67
        String template = "1.1.1.1 - - [01/Mar/2025:00:00:00 +0000] \"GET / HTTP/1.1\" 200 %d \"-\" \"x\"";
        List<LogRecord> records = Stream.of(1, 2, 2)
                .map(n -> String.format(Locale.ROOT, template, n))
                .map(PARSER::parse)
                .flatMap(Optional::stream)
                .toList();

        double average = StatisticsCollector.collect(records.stream())
                .responseSizeInBytes()
                .average();

        assertThat(average).isEqualTo(1.67);
        assertThat(String.format(Locale.ROOT, "%.2f", average)).isEqualTo("1.67");
    }

    @Test
    @DisplayName("Выбранный метод расчёта p95 зафиксирован и воспроизводим")
    void p95MethodIsDocumentedAndStable() {
        // Метод: nearest rank, index = ceil(0.95 * n) - 1 (0-based).
        // На полном наборе: ceil(0.95 * 3000) - 1 = 2849, sizes[2849] = 5450.
        Statistics stats = collect("access-2025-03-01.log", "access-2025-03-02.log");

        assertThat(stats.totalRequests()).isEqualTo(3000);
        assertThat(stats.responseSizeInBytes().p95())
                .as("nearest rank, а не линейная интерполяция (та дала бы 5450.20)")
                .isEqualTo(5450L);
    }

    private static Statistics collect(String... names) {
        List<LogRecord> records = Stream.of(names)
                .map(LOGS::resolve)
                .flatMap(StatisticsTest::linesOf)
                .map(PARSER::parse)
                .flatMap(Optional::stream)
                .toList();
        return StatisticsCollector.collect(records.stream());
    }

    private static Stream<String> linesOf(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8).stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
