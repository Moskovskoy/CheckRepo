package academy.incidentradar.source;

import academy.incidentradar.exception.InternalException;
import academy.incidentradar.exception.UsageException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

public final class UrlLogSource implements LogSource {

    private final int errorStatusCode404 = 404;
    private final int errorStatusCode400 = 400;

    private final URI uri;
    private final HttpClient client;

    public UrlLogSource(URI uri) {
        this.uri = uri;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Stream<String> lines() {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<Stream<String>> response;

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        } catch (IOException e) {
            throw new InternalException("Failed to read URL: " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalException("Interrupted while reading URL: " + uri, e);
        }

        int status = response.statusCode();

        if (status == errorStatusCode404) {
            throw new UsageException("URL returned 404: " + uri);
        }
        if (status >= errorStatusCode400) {
            throw new UsageException("URL returned " + status + ": " + uri);
        }

        return response.body();
    }

    @Override
    public String description() {
        return uri.toString();
    }
}
