package academy.incidentradar.exception;

public class UsageException extends IncidentRadarException {

    public UsageException(String message) {
        super(message);
    }

    public UsageException(String message, Throwable cause) {
        super(message, cause);
    }
}
