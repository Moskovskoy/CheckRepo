package academy.incidentradar.config;

import academy.incidentradar.cli.OutputFormat;
import academy.incidentradar.source.LogSource;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record RunConfig(
        List<LogSource> sources,
        OutputFormat outputFormat,
        @Nullable Path outputPath,
        @Nullable LocalDate from,
        @Nullable LocalDate to) {}
