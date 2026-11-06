package academy.incidentradar;

import academy.incidentradar.cli.CliOptions;
import academy.incidentradar.config.ConfigValidator;
import academy.incidentradar.config.RunConfig;
import academy.incidentradar.exception.IncidentRadarException;
import academy.incidentradar.exception.UsageException;
import academy.incidentradar.pipeline.LogLineStream;
import academy.incidentradar.source.LogSourceResolver;
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

        try (LogLineStream stream = new LogLineStream(config.sources());
                Stream<String> lines = stream.lines()) {
            long count = lines.count();
            LOG.info("Read {} lines from {} source(s)", count, config.sources().size());
        }
    }
}
