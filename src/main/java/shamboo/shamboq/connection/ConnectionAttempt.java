package shamboo.shamboq.connection;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Record representing a single connection attempt
 */
public record ConnectionAttempt(UUID playerId,
                                String targetServer,
                                AtomicInteger attemptCount,
                                long lastAttemptTime) {

    /**
     * Create a new connection attempt with default values
     */
    public ConnectionAttempt(UUID playerId, String targetServer) {
        this(playerId,
                targetServer,
                new AtomicInteger(0),
                System.currentTimeMillis());
    }

    /**
     * Increment the attempt count and update the timestamp
     * @return The new attempt count
     */
    public int incrementAttempt() {
        return attemptCount.incrementAndGet();
    }

    /**
     * Update the last attempt time
     * @return The updated ConnectionAttempt
     */
    public ConnectionAttempt updateLastAttemptTime() {
        return new ConnectionAttempt(
                playerId,
                targetServer,
                attemptCount,
                System.currentTimeMillis()
        );
    }
}