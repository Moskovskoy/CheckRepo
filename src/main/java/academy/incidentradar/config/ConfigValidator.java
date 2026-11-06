package academy.incidentradar.config;

import academy.incidentradar.cli.CliOptions;
import academy.incidentradar.cli.OutputFormat;
import academy.incidentradar.exception.UsageException;
import academy.incidentradar.source.LogSource;
import academy.incidentradar.source.LogSourceResolver;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class ConfigValidator {

    private final LogSourceResolver resolver;

    public ConfigValidator(LogSourceResolver resolver) {
        this.resolver = resolver;
    }

    public RunConfig validate(CliOptions options) {
        String path = options.path();
        if (path == null || path.isBlank()) {
            throw new UsageException("--path is required");
        }

        OutputFormat format = options.outputFormat();
        Path outputPath = options.outputPath();

        if (format != null || outputPath != null) {
            if (format == null) {
                throw new UsageException("--output-format is required when --output-path is set");
            }
            if (outputPath == null) {
                throw new UsageException("--output-path is required when --output-format is set");
            }
            validateOutputPath(outputPath, format);
        }

        LocalDate from = options.from();
        LocalDate to = options.to();
        if (from != null && to != null && !from.isBefore(to)) {
            throw new UsageException("--from must be strictly before --to");
        }

        List<LogSource> sources = resolver.resolve(path);

        return new RunConfig(sources, format, outputPath, from, to);
    }

    private static void validateOutputPath(Path outputPath, OutputFormat format) {

        if (outputPath == null) {
            throw new UsageException("--output-path is required");
        }

        Path fileName = outputPath.getFileName();

        if (fileName == null) {
            throw new UsageException("--output-path must point to a file: " + outputPath);
        }

        String name = fileName.toString().toLowerCase(Locale.ROOT);
        String expected = format.extension();

        if (!name.endsWith(expected)) {
            throw new UsageException("Output file extension must be " + expected + ", got: " + outputPath);
        }
    }
}
