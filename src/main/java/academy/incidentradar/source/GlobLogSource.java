package academy.incidentradar.source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class GlobLogSource implements LogSource {

    private final String pattern;
    private final List<Path> matched;

    public GlobLogSource(String pattern, List<Path> matched) {
        this.pattern = pattern;
        this.matched = List.copyOf(matched);
    }

    @Override
    @SuppressWarnings("StreamResourceLeak")
    public Stream<String> lines() {
        return matched.stream().flatMap(p -> {
            try {
                return Files.lines(p, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + p, e);
            }
        });
    }

    @Override
    public String description() {
        return "glob:" + pattern + " (" + matched.size() + " files)";
    }

    public List<Path> matchedFiles() {
        return matched;
    }
}
