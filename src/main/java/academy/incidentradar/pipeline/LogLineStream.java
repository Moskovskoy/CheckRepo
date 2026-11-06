package academy.incidentradar.pipeline;

import academy.incidentradar.source.LogSource;
import java.util.List;
import java.util.stream.Stream;

public final class LogLineStream implements AutoCloseable {

    private final List<LogSource> sources;

    public LogLineStream(List<LogSource> sources) {
        this.sources = List.copyOf(sources);
    }

    public Stream<String> lines() {
        return sources.stream().flatMap(LogSource::lines);
    }

    @Override
    public void close() {
        sources.forEach(LogSource::close);
    }
}
