package shamboo.shamboq.connection;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.util.LogLevel;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connection handler for managing server transfer attempts
 */
public class ConnectionHandler implements PluginMessageListener {
    private final ShamboQ plugin;

    // Map to track ongoing connection attempts and retries
    private final Map<UUID, ConnectionAttempt> connectionAttempts = new ConcurrentHashMap<>();

    // Configuration
    private int maxRetries = 3;
    private int retryDelaySeconds = 5;

    public ConnectionHandler(ShamboQ plugin) {
        this.plugin = plugin;

        // Register for incoming plugin messages from BungeeCord/Velocity
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);

        // Load configuration
        loadConfig();

        // Start a periodic task to check for timed out connection attempts
        startTimeoutChecker();
    }

    /**
     * Loads connection handler configuration from plugin config
     */
    private void loadConfig() {
        plugin.getConfig().addDefault("connection.max-retries", 3);
        plugin.getConfig().addDefault("connection.retry-delay", 5);
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        // Load values
        maxRetries = plugin.getConfig().getInt("connection.max-retries");
        retryDelaySeconds = plugin.getConfig().getInt("connection.retry-delay");
    }

    /**
     * Starts a task to check for timed out connection attempts
     */
    private void startTimeoutChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Map.Entry<UUID, ConnectionAttempt> entry : connectionAttempts.entrySet()) {
                    ConnectionAttempt attempt = entry.getValue();

                    // Check if connection attempt has timed out (15 seconds timeout)
                    if (currentTime - attempt.lastAttemptTime() > 15000) {
                        handleTimeout(attempt);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // Run every 5 seconds (100 ticks)
    }

    /**
     * Sends a player to the specified server with error handling
     */
    public void sendToServer(Player player, String serverName) {
        final UUID playerId = player.getUniqueId();

        // Create and store connection attempt
        ConnectionAttempt attempt = new ConnectionAttempt(playerId, serverName);
        attempt.incrementAttempt(); // Set attempt to 1
        connectionAttempts.put(playerId, attempt);

        // Track connection attempts
        plugin.getMetricsCollector().incrementCounter("connection_attempts");

        // Send player to server asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Send plugin message to BungeeCord/Velocity
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(serverName);

                // This must run on main thread
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                            plugin.logMessage("Sending player " + player.getName() + " to server " + serverName +
                                    " (Attempt " + attempt.attemptCount().get() + ")", LogLevel.INFO);
                        } catch (Exception e) {
                            // Handle immediate errors
                            plugin.logMessage("Error sending plugin message: " + e.getMessage(), LogLevel.ERROR);
                            handleConnectionError(player, "Internal error: " + e.getMessage());
                        }
                    }
                }.runTask(plugin);
            } catch (Exception e) {
                plugin.logMessage("Error preparing connection: " + e.getMessage(), LogLevel.ERROR);
                handleConnectionError(player, "Internal error: " + e.getMessage());
            }
        });
    }

    /**
     * Process incoming plugin messages from BungeeCord/Velocity
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        try {
            // Parse the message
            String subChannel = new String(message, StandardCharsets.UTF_8);

            // Handle connection response messages
            if (subChannel.startsWith("ConnectFailed")) {
                UUID playerId = player.getUniqueId();
                if (connectionAttempts.containsKey(playerId)) {
                    handleConnectionError(player, "Server unavailable");
                }
            }
        } catch (Exception e) {
            plugin.logMessage("Error processing plugin message: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Handles connection timeout for a player
     */
    private void handleTimeout(ConnectionAttempt attempt) {
        UUID playerId = attempt.playerId();
        Player player = Bukkit.getPlayer(playerId);

        if (player == null || !player.isOnline()) {
            // Player is no longer online, remove the attempt
            connectionAttempts.remove(playerId);
            return;
        }

        int attemptCount = attempt.attemptCount().get();
        if (attemptCount >= maxRetries) {
            // Max retries reached
            handleMaxRetriesReached(player);
        } else {
            // Retry the connection
            String message = plugin.getMessageManager().getMessage("connection_timeout", retryDelaySeconds);
            player.sendMessage(message);

            // Schedule retry
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        retryConnection(player, attempt.targetServer());
                    } else {
                        connectionAttempts.remove(playerId);
                    }
                }
            }.runTaskLater(plugin, retryDelaySeconds * 20L);
        }
    }

    /**
     * Handles connection error
     */
    private void handleConnectionError(Player player, String errorMessage) {
        UUID playerId = player.getUniqueId();
        ConnectionAttempt attempt = connectionAttempts.get(playerId);

        if (attempt == null) {
            // No ongoing connection attempt found
            plugin.logMessage("Connection error for " + player.getName() + " but no attempt found", LogLevel.WARNING);
            return;
        }

        int attemptCount = attempt.attemptCount().get();
        if (attemptCount >= maxRetries) {
            // Max retries reached
            handleMaxRetriesReached(player);
        } else {
            // Show error message
            String message = plugin.getMessageManager().getMessage("connection_error", errorMessage);
            player.sendMessage(message);

            // Schedule retry
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        retryConnection(player, attempt.targetServer());
                    } else {
                        connectionAttempts.remove(playerId);
                    }
                }
            }.runTaskLater(plugin, retryDelaySeconds * 20L);
        }
    }

    /**
     * Retries a connection attempt
     */
    private void retryConnection(Player player, String serverName) {
        UUID playerId = player.getUniqueId();
        ConnectionAttempt attempt = connectionAttempts.get(playerId);

        if (attempt == null) {
            // Create a new attempt if none exists
            attempt = new ConnectionAttempt(playerId, serverName);
            connectionAttempts.put(playerId, attempt);
        }

        // Increment attempt count
        int newAttemptCount = attempt.incrementAttempt();

        // Track retry attempts
        plugin.getMetricsCollector().incrementCounter("connection_retries");

        // Send player to server again
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        try {
            player.sendMessage(plugin.getMessageManager().getMessage("connection_failed"));
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            plugin.logMessage("Retrying connection for " + player.getName() + " to server " + serverName +
                    " (Attempt " + newAttemptCount + "/" + maxRetries + ")", LogLevel.INFO);
        } catch (Exception e) {
            plugin.logMessage("Error during retry: " + e.getMessage(), LogLevel.ERROR);
            handleConnectionError(player, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Handles case when max retries are reached
     */
    private void handleMaxRetriesReached(Player player) {
        UUID playerId = player.getUniqueId();

        // Remove the connection attempt
        connectionAttempts.remove(playerId);

        // Show message to player
        player.sendMessage(plugin.getMessageManager().getMessage("max_retries_reached", maxRetries));

        // Track max retries
        plugin.getMetricsCollector().incrementCounter("max_retries_reached");

        // Make sure player stays frozen
        if (!plugin.getQueueManager().isPlayerFrozen(player)) {
            // Player was removed from queue during connection attempts, add back
            player.sendMessage(plugin.getMessageManager().getMessage("back_in_queue"));
            plugin.getQueueManager().addToQueue(player);
        }
    }

    /**
     * Checks if player has an ongoing connection attempt
     */
    public boolean hasOngoingConnectionAttempt(UUID playerId) {
        return connectionAttempts.containsKey(playerId);
    }

    /**
     * Cancels a connection attempt
     */
    public void cancelConnectionAttempt(UUID playerId) {
        connectionAttempts.remove(playerId);
    }

    /**
     * Called when the plugin is disabled
     */
    public void shutdown() {
        // Unregister the plugin channel
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, "BungeeCord", this);
        connectionAttempts.clear();
    }
}