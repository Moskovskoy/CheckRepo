package academy.incidentradar.cli;

import java.util.Locale;

public enum OutputFormat {
    JSON(".json"),
    MARKDOWN(".md");

    private final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
