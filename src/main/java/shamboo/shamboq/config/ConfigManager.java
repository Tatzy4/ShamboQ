package shamboo.shamboq.config;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.FileConfiguration;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.util.GameRuleCache;
import shamboo.shamboq.util.LogLevel;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class managing configuration
 */
public class ConfigManager {
    private final ShamboQ plugin;
    private final GameRuleCache gameRuleCache;

    // Queue configuration
    private boolean queueEnabled;
    private int queueTime;
    private String smpServer;
    private boolean showQueueDisabledMessage;
    private String queueDisabledMessage;
    private int notificationInterval;

    // Spawn point configuration
    private double spawnX;
    private int spawnY;
    private double spawnZ;

    // End world (The End)
    private World endWorld;

    // Saves original world settings to restore later
    private Map<GameRule<?>, Object> originalGameRules = new HashMap<>();

    public ConfigManager(ShamboQ plugin) {
        this.plugin = plugin;
        this.gameRuleCache = new GameRuleCache();
        loadConfig();
        findEndWorld();
    }

    public void loadConfig() {
        // Get configuration
        FileConfiguration config = plugin.getConfig();

        // Set default values if they don't exist
        config.addDefault("queue.enabled", true);
        config.addDefault("queue.time", 10);
        config.addDefault("queue.smp-server", "smp");
        config.addDefault("queue.disabled-message", "Queue is currently disabled");
        config.addDefault("queue.show-disabled-message", true);
        config.addDefault("queue.notification-interval", 5); // seconds
        config.addDefault("spawn.x", -1.5);
        config.addDefault("spawn.y", 64);
        config.addDefault("spawn.z", 0.5);

        config.options().copyDefaults(true);
        plugin.saveConfig();

        // Load values from configuration
        queueEnabled = config.getBoolean("queue.enabled");
        queueTime = validateQueueTime(config.getInt("queue.time"));
        smpServer = config.getString("queue.smp-server");
        queueDisabledMessage = config.getString("queue.disabled-message");
        showQueueDisabledMessage = config.getBoolean("queue.show-disabled-message");
        notificationInterval = Math.max(1, config.getInt("queue.notification-interval"));
        spawnX = config.getDouble("spawn.x");
        spawnY = config.getInt("spawn.y");
        spawnZ = config.getDouble("spawn.z");
    }

    private int validateQueueTime(int time) {
        if (time < 1) {
            plugin.logMessage("Attempt to set invalid queue time: " + time, LogLevel.WARNING);
            return 1; // Minimum value
        }
        if (time > 3600) {
            plugin.logMessage("Attempt to set very long queue time: " + time, LogLevel.WARNING);
            return 3600; // Maximum value
        }
        return time;
    }

    private void findEndWorld() {
        endWorld = null;
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() == Environment.THE_END) {
                endWorld = world;
                break;
            }
        }

        // If End world doesn't exist, use default world
        if (endWorld == null) {
            endWorld = plugin.getServer().getWorlds().get(0);
            plugin.logMessage("End world not found, using default world!", LogLevel.WARNING);
        } else {
            plugin.logMessage("Using End world: " + endWorld.getName(), LogLevel.FINE);
        }
    }

    /**
     * Sets up no-tick zone for queue area (only for Paper servers or forks)
     */
    public void setupNoTickZone() {
        if (!plugin.getVersionManager().isPaperServer()) {
            plugin.logMessage("No-tick zone feature requires Paper server - skipping setup", LogLevel.INFO);
            return;
        }

        try {
            World endWorld = getEndWorld();
            Location spawnLoc = createSpawnLocation();

            // Get chunk where spawn is located
            org.bukkit.Chunk spawnChunk = spawnLoc.getChunk();
            plugin.logMessage("Setting up no-tick zone for chunks around spawn...", LogLevel.FINE);

            int configuredChunks = 0;
            // Apply optimizations to spawn and surrounding chunks
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    org.bukkit.Chunk chunk = endWorld.getChunkAt(spawnChunk.getX() + x, spawnChunk.getZ() + z);

                    // Use the platform adapter for cross-version compatibility
                    if (plugin.getVersionManager().isPaperMethodAvailable("setForceLoaded", boolean.class)) {
                        try {
                            plugin.getVersionManager().getPlatformAdapter().setForceLoaded(chunk, true);
                            configuredChunks++;

                            // Set inhabited time to 0
                            if (plugin.getVersionManager().isPaperMethodAvailable("setInhabitedTime", long.class)) {
                                plugin.getVersionManager().getPlatformAdapter().setInhabitedTime(chunk, 0L);
                            }

                            // Set no-tick chunk
                            if (plugin.getVersionManager().isPaperMethodAvailable("setNoTickChunk", boolean.class)) {
                                plugin.getVersionManager().getPlatformAdapter().setNoTickChunk(chunk, true);
                            }
                        } catch (Exception e) {
                            plugin.logMessage("Error while setting up chunk optimizations: " + e.getMessage(), LogLevel.WARNING);
                        }
                    }
                }
            }

            plugin.logMessage("Successfully set up no-tick zone for " + configuredChunks + " chunks", LogLevel.INFO);
        } catch (Exception e) {
            plugin.logMessage("Failed to set up no-tick zone: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Sets up minimalist End world - disables terrain generation and other resource-consuming features
     */
    public void setupMinimalistEndWorld() {
        if (endWorld == null) return;

        try {
            // Save original GameRules values
            saveOriginalGameRules();

            // Stop terrain generation
            endWorld.setKeepSpawnInMemory(false);
            plugin.logMessage("Disabled keeping spawn in memory", LogLevel.FINE);

            // GameRules settings using cache
            setGameRuleSafely(endWorld, "DO_MOB_SPAWNING", false);
            setGameRuleSafely(endWorld, "DO_FIRE_TICK", false);
            setGameRuleSafely(endWorld, "DO_WEATHER_CYCLE", false);
            setGameRuleSafely(endWorld, "RANDOM_TICK_SPEED", 0);
            setGameRuleSafely(endWorld, "DO_DAYLIGHT_CYCLE", false);
            setGameRuleSafely(endWorld, "DO_PATROL_SPAWNING", false);
            setGameRuleSafely(endWorld, "DO_TRADER_SPAWNING", false);
            setGameRuleSafely(endWorld, "DO_INSOMNIA", false);

            // Try to use Paper API via platform adapter
            if (plugin.getVersionManager().isPaperServer()) {
                if (plugin.getVersionManager().isPaperMethodAvailable("setPopulators", java.util.List.class)) {
                    plugin.getVersionManager().getPlatformAdapter().setPopulators(endWorld, Collections.emptyList());
                    plugin.logMessage("Successfully disabled terrain generators", LogLevel.INFO);
                } else {
                    plugin.logMessage("Method setPopulators unavailable - skipping this optimization", LogLevel.INFO);
                }

                if (plugin.getVersionManager().isPaperMethodAvailable("setViewDistance", int.class)) {
                    plugin.getVersionManager().getPlatformAdapter().setWorldViewDistance(endWorld, 2);
                    plugin.logMessage("Set minimum view distance for End world", LogLevel.INFO);
                }
            }

            plugin.logMessage("Successfully set up minimalist End world", LogLevel.INFO);

            // Track this metric
            plugin.getMetricsCollector().incrementCounter("world_optimizations");
        } catch (Exception e) {
            plugin.logMessage("Failed to set up minimalist world: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    // Helper method to safely set GameRules using cache
    private void setGameRuleSafely(World world, String ruleName, Object value) {
        try {
            GameRule<?> rule = gameRuleCache.getGameRule(ruleName);
            if (rule != null) {
                // Use reflection to bypass generic type constraints
                Method method = World.class.getMethod("setGameRule", GameRule.class, Object.class);
                method.invoke(world, rule, value);
                plugin.logMessage("Set GameRule " + ruleName + " = " + value, LogLevel.FINE);
            }
        } catch (Exception e) {
            // Ignore - this GameRule doesn't exist in this version
            plugin.logMessage("GameRule " + ruleName + " doesn't exist in this version - skipping", LogLevel.FINE);
        }
    }

    private void saveOriginalGameRules() {
        originalGameRules.clear();

        for (GameRule<?> rule : GameRule.values()) {
            try {
                Object value = endWorld.getGameRuleValue(rule);
                if (value != null) {
                    originalGameRules.put(rule, value);
                }
            } catch (Exception e) {
                // Ignore errors for unknown GameRules
            }
        }

        plugin.logMessage("Saved " + originalGameRules.size() + " original GameRule settings", LogLevel.FINE);
    }

    public void restoreGameRules() {
        if (endWorld == null || originalGameRules.isEmpty()) {
            return;
        }

        for (Map.Entry<GameRule<?>, Object> entry : originalGameRules.entrySet()) {
            try {
                // Unfortunately Java doesn't allow direct casting of generic type,
                // so we use reflection
                @SuppressWarnings("unchecked")
                GameRule<Object> rule = (GameRule<Object>) entry.getKey();
                endWorld.setGameRule(rule, entry.getValue());
            } catch (Exception e) {
                plugin.logMessage("Failed to restore gamerule: " + entry.getKey().getName(), LogLevel.WARNING);
            }
        }

        plugin.logMessage("Restored original GameRule settings for End world", LogLevel.INFO);
    }

    // Getters and setters
    public boolean isQueueEnabled() {
        return queueEnabled;
    }

    public void setQueueEnabled(boolean enabled) {
        this.queueEnabled = enabled;
        plugin.getConfig().set("queue.enabled", enabled);
        plugin.saveConfig();
    }

    public int getQueueTime() {
        return queueTime;
    }

    public void setQueueTime(int time) {
        this.queueTime = validateQueueTime(time);
        plugin.getConfig().set("queue.time", queueTime);
        plugin.saveConfig();
    }

    public String getSmpServer() {
        return smpServer;
    }

    public void setSmpServer(String server) {
        this.smpServer = server;
        plugin.getConfig().set("queue.smp-server", server);
        plugin.saveConfig();
    }

    public boolean isShowQueueDisabledMessage() {
        return showQueueDisabledMessage;
    }

    public void setShowQueueDisabledMessage(boolean show) {
        this.showQueueDisabledMessage = show;
        plugin.getConfig().set("queue.show-disabled-message", show);
        plugin.saveConfig();
    }

    public String getQueueDisabledMessage() {
        return queueDisabledMessage;
    }

    public void setQueueDisabledMessage(String message) {
        this.queueDisabledMessage = message;
        plugin.getConfig().set("queue.disabled-message", message);
        plugin.saveConfig();
    }

    public int getNotificationInterval() {
        return notificationInterval;
    }

    public double getSpawnX() {
        return spawnX;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public double getSpawnZ() {
        return spawnZ;
    }

    public World getEndWorld() {
        return endWorld;
    }

    // Reload configuration
    public void reload() {
        plugin.reloadConfig();
        loadConfig();
        findEndWorld();
    }

    // Create location based on settings
    public Location createSpawnLocation() {
        Location location = new Location(endWorld, spawnX, spawnY, spawnZ);

        // Ensure solid block under player
        Location blockLocation = location.clone();
        blockLocation.setY(blockLocation.getY() - 1);
        if (blockLocation.getBlock().getType() == org.bukkit.Material.AIR) {
            blockLocation.getBlock().setType(org.bukkit.Material.END_STONE);
        }

        return location;
    }
}