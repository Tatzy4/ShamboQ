package shamboo.shamboq.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.platform.FallbackAdapter;
import shamboo.shamboq.platform.PaperAdapter;
import shamboo.shamboq.platform.PlatformAdapter;
import shamboo.shamboq.platform.SpigotAdapter;
import shamboo.shamboq.util.LogLevel;

/**
 * Class managing server version and compatibility
 */
public class VersionManager {
    private final ShamboQ plugin;
    private final String serverVersion;
    private final PlatformAdapter platformAdapter;

    public VersionManager(ShamboQ plugin) {
        this.plugin = plugin;

        // Get server version
        String serverClassName = Bukkit.getServer().getClass().getName();
        this.serverVersion = Bukkit.getBukkitVersion();

        plugin.logMessage("Detected server version: " + serverVersion + " (" + serverClassName + ")", LogLevel.FINE);

        // Select and create the appropriate platform adapter
        platformAdapter = createPlatformAdapter();
    }

    private PlatformAdapter createPlatformAdapter() {
        // Try to use Paper adapter first
        if (isPaperServer()) {
            try {
                // Check if Adventure API exists
                Class.forName("net.kyori.adventure.text.Component");
                plugin.logMessage("Using Paper platform adapter with Adventure API", LogLevel.INFO);
                return new PaperAdapter(plugin);
            } catch (ClassNotFoundException e) {
                plugin.logMessage("Paper server detected but Adventure API not found, falling back", LogLevel.FINE);
            }
        }

        // Try to use Spigot adapter
        try {
            // Check if sendActionBar method exists
            Player.class.getMethod("sendActionBar", String.class);
            plugin.logMessage("Using Spigot platform adapter", LogLevel.INFO);
            return new SpigotAdapter(plugin);
        } catch (NoSuchMethodException e) {
            // Method doesn't exist, use fallback
        }

        // Use fallback adapter
        plugin.logMessage("Using fallback platform adapter", LogLevel.INFO);
        return new FallbackAdapter(plugin);
    }

    public void sendActionBar(Player player, String message) {
        platformAdapter.sendActionBar(player, message);
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public PlatformAdapter getPlatformAdapter() {
        return platformAdapter;
    }

    /**
     * Checks if server is Paper or a fork
     */
    public boolean isPaperServer() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("io.papermc.paper.configuration.Configuration");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    /**
     * Checks if Paper method is available
     */
    public boolean isPaperMethodAvailable(String methodName, Class<?>... parameterTypes) {
        try {
            // First check if it's Paper
            if (!isPaperServer()) {
                return false;
            }

            // Then check if specific method exists based on method name
            switch (methodName) {
                case "setPopulators":
                    org.bukkit.World.class.getMethod("setPopulators", parameterTypes);
                    return true;
                case "setViewDistance":
                    org.bukkit.World.class.getMethod("setViewDistance", parameterTypes);
                    return true;
                case "setNoTickChunk":
                    org.bukkit.Chunk.class.getMethod("setNoTickChunk", parameterTypes);
                    return true;
                case "setForceLoaded":
                    org.bukkit.Chunk.class.getMethod("setForceLoaded", parameterTypes);
                    return true;
                case "setInhabitedTime":
                    org.bukkit.Chunk.class.getMethod("setInhabitedTime", parameterTypes);
                    return true;
                default:
                    return false;
            }
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            plugin.logMessage("Error checking Paper method " + methodName + ": " + e.getMessage(), LogLevel.WARNING);
            return false;
        }
    }
}