package academy.incidentradar;

import academy.incidentradar.cli.CliOptions;
import academy.incidentradar.config.ConfigValidator;
import academy.incidentradar.config.RunConfig;
import academy.incidentradar.exception.IncidentRadarException;
import academy.incidentradar.exception.UsageException;
import academy.incidentradar.filter.DateFilter;
import academy.incidentradar.model.LogRecord;
import academy.incidentradar.parser.LogParser;
import academy.incidentradar.pipeline.LogLineStream;
import academy.incidentradar.source.LogSourceResolver;
import academy.incidentradar.stats.Statistics;
import academy.incidentradar.stats.StatisticsCollector;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public final class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private App() {}

    public static int run(String[] args) {
        CliOptions options = new CliOptions();
        CommandLine cmd = new CommandLine(options);

        try {
            int code = cmd.execute(args);
            if (code != CommandLine.ExitCode.OK) {
                return code;
            }
        } catch (CommandLine.ParameterException e) {
            LOG.error(e.getMessage());
            return 2;
        }

        try {
            RunConfig config = new ConfigValidator(new LogSourceResolver()).validate(options);
            execute(config);
            return 0;
        } catch (UsageException e) {
            LOG.error(e.getMessage());
            return 2;
        } catch (IncidentRadarException e) {
            LOG.error(e.getMessage(), e);
            return 1;
        }
    }

    private static void execute(RunConfig config) {
        LogParser parser = new LogParser();
        Predicate<LogRecord> filter = DateFilter.of(config.from(), config.to());

        try (LogLineStream stream = new LogLineStream(config.sources());
                Stream<String> lines = stream.lines()) {

            Stream<LogRecord> parsed = lines.map(line -> {
                        Optional<LogRecord> r = parser.parse(line);
                        if (r.isEmpty()) {
                            LOG.warn("skipped malformed log line"); // точное сообщение из README
                        }
                        return r;
                    })
                    .flatMap(Optional::stream)
                    .filter(filter);

            Statistics stats = StatisticsCollector.collect(parsed);

            LOG.info(
                    "Read {} records, {} distinct codes, top resource '{}' ({}), avg {}, max {}, p95 {}",
                    stats.totalRequests(),
                    stats.responseCodes().size(),
                    stats.resources().isEmpty() ? "-" : stats.resources().get(0).resource(),
                    stats.resources().isEmpty() ? 0 : stats.resources().get(0).count(),
                    stats.responseSizeInBytes().average(),
                    stats.responseSizeInBytes().max(),
                    stats.responseSizeInBytes().p95());
        }
    }
}
