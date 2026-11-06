package academy.incidentradar.exception;

public class InternalException extends IncidentRadarException {

    public InternalException(String message) {
        super(message);
    }

    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
