package shamboo.shamboq.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Collection of metrics for monitoring plugin performance
 */
public class MetricsCollector {
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    /**
     * Increment a counter by name
     * @param name Counter name
     */
    public void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * Increment a counter by name with a specific value
     * @param name Counter name
     * @param value Value to increment by
     */
    public void incrementCounter(String name, long value) {
        counters.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(value);
    }

    /**
     * Get the value of a counter
     * @param name Counter name
     * @return Current value of the counter
     */
    public long getCounter(String name) {
        return counters.getOrDefault(name, new AtomicLong()).get();
    }

    /**
     * Get all metrics as a map
     * @return Map of metric names to values
     */
    public Map<String, Long> getAllMetrics() {
        return counters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    /**
     * Reset all counters
     */
    public void resetCounters() {
        counters.clear();
    }

    /**
     * Reset a specific counter
     * @param name Counter name
     */
    public void resetCounter(String name) {
        counters.remove(name);
    }
}