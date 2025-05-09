package shamboo.shamboq.platform;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.util.LogLevel;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Platform adapter for Spigot servers
 */
public class SpigotAdapter implements PlatformAdapter {
    private final ShamboQ plugin;

    public SpigotAdapter(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendActionBar(Player player, String message) {
        try {
            // Use reflection to handle different versions
            Method sendActionBarMethod = Player.class.getMethod("sendActionBar", String.class);
            sendActionBarMethod.invoke(player, message);

            // Track successful action bar sends
            plugin.getMetricsCollector().incrementCounter("actionbar_sends");
        } catch (Exception e) {
            // Fallback in case of error
            player.sendMessage(message);
            plugin.logMessage("Failed to send action bar through Spigot API: " + e.getMessage(), LogLevel.WARNING);
        }
    }

    @Override
    public void setForceLoaded(Chunk chunk, boolean value) {
        // Not supported in base Spigot
        plugin.logMessage("setForceLoaded not supported in Spigot", LogLevel.FINE);
    }

    @Override
    public void setInhabitedTime(Chunk chunk, long time) {
        // Not supported in base Spigot
        plugin.logMessage("setInhabitedTime not supported in Spigot", LogLevel.FINE);
    }

    @Override
    public void setNoTickChunk(Chunk chunk, boolean value) {
        // Not supported in base Spigot
        plugin.logMessage("setNoTickChunk not supported in Spigot", LogLevel.FINE);
    }

    @Override
    public void setPopulators(World world, List<?> populators) {
        // Not supported in base Spigot
        plugin.logMessage("setPopulators not supported in Spigot", LogLevel.FINE);
    }

    @Override
    public void setWorldViewDistance(World world, int viewDistance) {
        // Not supported in base Spigot
        plugin.logMessage("setViewDistance not supported in Spigot", LogLevel.FINE);
    }
}