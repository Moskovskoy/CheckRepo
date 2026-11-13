package academy.incidentradar.model;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.jspecify.annotations.Nullable;

public record LogRecord(
        String remoteAddr,
        @Nullable String remoteUser,
        ZonedDateTime timeLocal,
        String method,
        String resource,
        String protocol,
        int status,
        long bodyBytesSent,
        String httpReferer,
        String httpUserAgent) {

    public LocalDate date() {
        return timeLocal.toLocalDate();
    }
}
