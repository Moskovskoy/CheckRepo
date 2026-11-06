package academy.incidentradar.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.App;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Обязательные тесты: валидация аргументов командной строки. */
@DisplayName("MR1. Валидация аргументов CLI")
class CliValidationTest {

    @Test
    @DisplayName("Не передан обязательный --path -> exit code 2")
    void missingPathReturnsExitCode2() {
        int exitCode = App.run(new String[] {
            "--output-format", "json",
            "--output-path", "report.json"
        });

        assertThat(exitCode).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"xml", "csv", "txt"})
    @DisplayName("Неподдерживаемый --output-format \"{0}\" -> exit code 2")
    void unsupportedOutputFormatExitsWithTwo(String format) {
        int exitCode = App.run(new String[] {
            "--path",
            "samples/logs/access-2025-03-01.log",
            "--output-format",
            format,
            "--output-path",
            "report." + format
        });

        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    @DisplayName("--from больше или равен --to -> exit code 2")
    void fromNotBeforeToExitsWithTwo() {
        int exitCode = App.run(new String[] {
            "--path", "samples/logs/access-2025-03-01.log",
            "--output-format", "json",
            "--output-path", "report.json",
            "--from", "2025-03-02",
            "--to", "2025-03-01"
        });

        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    @DisplayName("Несуществующий локальный файл -> exit code 2")
    void missingLocalFileExitsWithTwo() {
        int exitCode = App.run(new String[] {
            "--path", "samples/logs/does-not-exist.log",
            "--output-format", "json",
            "--output-path", "report.json"
        });

        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    @DisplayName("Расширение файла отчёта не соответствует формату -> exit code 2")
    void outputExtensionMismatchExitsWithTwo() {
        int exitCode = App.run(new String[] {
            "--path", "samples/logs/access-2025-03-01.log",
            "--output-format", "json",
            "--output-path", "report.md"
        });

        assertThat(exitCode).isEqualTo(2);
    }
}
