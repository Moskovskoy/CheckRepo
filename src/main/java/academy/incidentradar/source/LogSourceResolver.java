package academy.incidentradar.source;

import academy.incidentradar.exception.UsageException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class LogSourceResolver {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".log", ".txt");

    public List<LogSource> resolve(String path) {
        String trimmed = path.strip();
        if (trimmed.isEmpty()) {
            throw new UsageException("--path must not be blank");
        }
        if (isUrl(trimmed)) {
            return List.of(resolveUrl(trimmed));
        }
        if (containsGlobMeta(trimmed)) {
            return resolveGlob(trimmed);
        }
        return List.of(resolveFile(Path.of(trimmed)));
    }

    private static boolean isUrl(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static boolean containsGlobMeta(String s) {
        return s.indexOf('*') >= 0 || s.indexOf('?') >= 0 || s.indexOf('[') >= 0;
    }

    private LogSource resolveUrl(String s) {
        URI uri;
        try {
            uri = new URI(s);
        } catch (URISyntaxException e) {
            throw new UsageException("Invalid URL: " + s, e);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new UsageException("Unsupported URL scheme: " + uri.getScheme());
        }
        return new UrlLogSource(uri);
    }

    private List<LogSource> resolveGlob(String pattern) {

        Path baseDir = baseDirOf(pattern);
        if (!Files.isDirectory(baseDir)) {
            throw new UsageException("Glob base directory not found: " + baseDir + " (pattern: " + pattern + ")");
        }

        String separator = FileSystems.getDefault().getSeparator();
        String baseStr = baseDir.toString();
        String normalized = pattern.replace('/', separator.charAt(0));

        String relativePattern;

        if (normalized.startsWith(baseStr + separator)) {
            relativePattern = normalized.substring(baseStr.length() + separator.length());
        } else if (normalized.equals(baseStr)) {
            relativePattern = "";
        } else {
            relativePattern = normalized;
        }

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + relativePattern);

        try (Stream<Path> walk = Files.walk(baseDir)) {
            List<Path> matched = walk.filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(baseDir.relativize(p)))
                    .filter(LogSourceResolver::hasSupportedExtension)
                    .sorted()
                    .toList();
            if (matched.isEmpty()) {
                throw new UsageException("No files matched glob: " + pattern);
            }
            return List.of(new GlobLogSource(pattern, matched));
        } catch (IOException e) {
            throw new UsageException("Cannot expand glob: " + pattern, e);
        }
    }

    private LogSource resolveFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new UsageException("File not found: " + path);
        }
        if (!hasSupportedExtension(path)) {
            throw new UsageException("Unsupported file extension: " + path + " (expected .log or .txt)");
        }
        return new FileLogSource(path);
    }

    private static boolean hasSupportedExtension(Path p) {
        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private static Path baseDirOf(String pattern) {
        int idx = firstMetaIndex(pattern);
        String prefix = idx < 0 ? pattern : pattern.substring(0, idx);
        Path p = Path.of(prefix.isEmpty() ? "." : prefix);

        if (!Files.isDirectory(p)) {
            p = p.getParent() == null ? Path.of(".") : p.getParent();
        }

        return p;
    }

    private static int firstMetaIndex(String s) {
        int best = -1;
        for (char c : new char[] {'*', '?', '['}) {
            int i = s.indexOf(c);
            if (i >= 0 && (best < 0 || i < best)) {
                best = i;
            }
        }
        return best;
    }
}
