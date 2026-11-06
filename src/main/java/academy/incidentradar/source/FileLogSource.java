package academy.incidentradar.source;

import academy.incidentradar.exception.UsageException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class FileLogSource implements LogSource {

    private final Path path;

    public FileLogSource(Path path) {
        this.path = path;
    }

    @Override
    @SuppressWarnings("StreamResourceLeak")
    public Stream<String> lines() {
        try {
            return Files.lines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UsageException("Cannot read file: " + path, e);
        }
    }

    @Override
    public String description() {
        return path.toString();
    }
}
