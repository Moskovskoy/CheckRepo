package academy.incidentradar.cli;

import academy.incidentradar.exception.UsageException;

@Deprecated(forRemoval = true)
public class CliException extends UsageException {

    public CliException(String message) {
        super(message);
    }
}
