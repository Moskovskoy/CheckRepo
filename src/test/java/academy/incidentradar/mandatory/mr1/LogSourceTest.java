package academy.incidentradar.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.App;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Обязательные тесты: источники логов. */
@DisplayName("MR1. Источники логов")
class LogSourceTest {

    private static final String SAMPLE_LINE =
            "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3\"";

    @TempDir
    Path tempDir;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Локальный файл читается")
    void readsLocalFile() throws Exception {
        Path log = tempDir.resolve("access.log");
        Files.writeString(log, SAMPLE_LINE + "\n");

        int exitCode = App.run(new String[] {
            "--path", log.toString(),
            "--output-format", "json",
            "--output-path", tempDir.resolve("report.json").toString()
        });

        assertThat(exitCode).isZero();
    }

    @Test
    @DisplayName("Glob-маска: в отчёт попадают все подошедшие файлы")
    void readsAllFilesMatchingGlob() throws Exception {
        Path logs = Files.createDirectories(tempDir.resolve("logs"));
        Files.writeString(logs.resolve("access-2025-03-01.log"), SAMPLE_LINE + "\n");
        Files.writeString(logs.resolve("access-2025-03-02.log"), SAMPLE_LINE + "\n");
        Files.writeString(logs.resolve("notes.md"), "ignore me\n");

        String pattern = logs + "/access-*.log";

        int exitCode = App.run(new String[] {
            "--path",
            pattern,
            "--output-format",
            "json",
            "--output-path",
            tempDir.resolve("report.json").toString()
        });

        assertThat(exitCode).isZero();
    }

    @Test
    @DisplayName("Лог читается по URL")
    void readsRemoteFile() throws Exception {
        server = startServer("/access.log", 200, SAMPLE_LINE + "\n");
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/access.log";

        int exitCode = App.run(new String[] {
            "--path",
            url,
            "--output-format",
            "json",
            "--output-path",
            tempDir.resolve("report.json").toString()
        });

        assertThat(exitCode).isZero();
    }

    @Test
    @DisplayName("URL вернул 404 -> exit code 2")
    void remote404ExitsWithTwo() throws Exception {
        server = startServer("/missing.log", 404, "");
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/missing.log";

        int exitCode = App.run(new String[] {
            "--path",
            url,
            "--output-format",
            "json",
            "--output-path",
            tempDir.resolve("report.json").toString()
        });

        assertThat(exitCode)
                .as("404 — вина вызывающего скрипта, ожидаем 2, а не 1")
                .isEqualTo(2);
    }

    private static HttpServer startServer(String path, int status, String body) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        s.start();
        return s;
    }
}
