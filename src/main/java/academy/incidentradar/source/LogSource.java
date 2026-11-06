package academy.incidentradar.source;

import java.util.stream.Stream;

public interface LogSource extends AutoCloseable {

    Stream<String> lines();

    String description();

    @Override
    default void close() {}
}
