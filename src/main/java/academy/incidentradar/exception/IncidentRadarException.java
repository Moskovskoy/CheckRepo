package academy.incidentradar.exception;

public class IncidentRadarException extends RuntimeException {

    public IncidentRadarException(String message) {
        super(message);
    }

    public IncidentRadarException(String message, Throwable cause) {
        super(message, cause);
    }
}
