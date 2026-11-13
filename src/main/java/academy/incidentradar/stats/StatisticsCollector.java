package academy.incidentradar.stats;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import academy.incidentradar.model.LogRecord;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

@SuppressWarnings("MagicNumber")
public final class StatisticsCollector {

    private StatisticsCollector() {}

    public static Statistics collect(Stream<LogRecord> records) {
        List<LogRecord> list = records.toList(); // нужно для сортировки размеров
        long total = list.size();

        Map<Integer, Long> codes = list.stream().collect(groupingBy(LogRecord::status, TreeMap::new, counting()));

        List<ResourceCount> top = list.stream().collect(groupingBy(LogRecord::resource, counting())).entrySet().stream()
                .map(e -> new ResourceCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ResourceCount::count)
                        .reversed()
                        .thenComparing(ResourceCount::resource))
                .limit(10)
                .toList();

        long[] sizes =
                list.stream().mapToLong(LogRecord::bodyBytesSent).sorted().toArray();

        double average =
                sizes.length == 0 ? 0 : round2(Arrays.stream(sizes).average().orElse(0));
        long max = sizes.length == 0 ? 0 : sizes[sizes.length - 1];
        long p95 = sizes.length == 0 ? 0 : sizes[(int) Math.ceil(0.95 * sizes.length) - 1];

        return new Statistics(total, codes, top, new ResponseSizeStats(average, max, p95));
    }

    private static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }
}
