package academy.incidentradar.stats;

import java.util.List;
import java.util.Map;

public record Statistics(
        long totalRequests,
        Map<Integer, Long> responseCodes, // LinkedHashMap, ключи по возрастанию
        List<ResourceCount> resources, // top-10, по убыванию count
        ResponseSizeStats responseSizeInBytes) {}
