package academy.incidentradar.cli;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "incident-radar",
        mixinStandardHelpOptions = true,
        description = "Analyzes NGINX logs and builds a report.")
@SuppressWarnings("NullAway")
public class CliOptions implements Callable<Integer> {

    @Option(
            names = {"-p", "--path"},
            required = true,
            paramLabel = "<path>",
            description = "File, glob pattern or URL.")
    private String path;

    @Option(
            names = {"-f", "--output-format"},
            paramLabel = "<format>",
            description = "Output format: json or markdown.")
    private OutputFormat outputFormat;

    @Option(
            names = {"-o", "--output-path"},
            paramLabel = "<file>",
            description = "Path to the report file.")
    private Path outputPath;

    @Option(names = "--from", paramLabel = "<yyyy-MM-dd>", description = "Inclusive lower date bound.")
    private LocalDate from;

    @Option(names = "--to", paramLabel = "<yyyy-MM-dd>", description = "Inclusive upper date bound.")
    private LocalDate to;

    public String path() {
        return path;
    }

    public OutputFormat outputFormat() {
        return outputFormat;
    }

    public Path outputPath() {
        return outputPath;
    }

    public LocalDate from() {
        return from;
    }

    public LocalDate to() {
        return to;
    }

    @Override
    public Integer call() {
        return 0;
    }
}
