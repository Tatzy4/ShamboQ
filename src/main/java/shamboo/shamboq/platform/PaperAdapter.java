package shamboo.shamboq.platform;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.util.LogLevel;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Platform adapter for Paper servers
 */
public class PaperAdapter implements PlatformAdapter {
    private final ShamboQ plugin;

    public PaperAdapter(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendActionBar(Player player, String message) {
        try {
            // Use reflection to avoid direct dependencies on Adventure API
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Object component = componentClass.getMethod("text", String.class).invoke(null, message);

            // Get audience from player
            Method audienceMethod = player.getClass().getMethod("sendActionBar", componentClass);
            audienceMethod.invoke(player, component);

            // Track successful action bar sends
            plugin.getMetricsCollector().incrementCounter("actionbar_sends");
        } catch (Exception e) {
            // Fallback in case of error
            player.sendMessage(message);
            plugin.logMessage("Failed to send action bar through Adventure API: " + e.getMessage(), LogLevel.WARNING);
        }
    }

    @Override
    public void setForceLoaded(Chunk chunk, boolean value) {
        try {
            Method setForceLoaded = Chunk.class.getMethod("setForceLoaded", boolean.class);
            setForceLoaded.invoke(chunk, value);
        } catch (Exception e) {
            plugin.logMessage("Failed to set force loaded: " + e.getMessage(), LogLevel.WARNING);
        }
    }

    @Override
    public void setInhabitedTime(Chunk chunk, long time) {
        try {
            Method setInhabitedTime = Chunk.class.getMethod("setInhabitedTime", long.class);
            setInhabitedTime.invoke(chunk, time);
        } catch (Exception e) {
            plugin.logMessage("Failed to set inhabited time: " + e.getMessage(), LogLevel.WARNING);
        }
    }

    @Override
    public void setNoTickChunk(Chunk chunk, boolean value) {
        try {
            Method setNoTickChunk = Chunk.class.getMethod("setNoTickChunk", boolean.class);
            setNoTickChunk.invoke(chunk, value);
        } catch (Exception e) {
            plugin.logMessage("Failed to set no-tick chunk: " + e.getMessage(), LogLevel.WARNING);
        }
    }

    @Override
    public void setPopulators(World world, List<?> populators) {
        try {
            Method setPopulatorsMethod = World.class.getMethod("setPopulators", List.class);
            setPopulatorsMethod.invoke(world, populators);
        } catch (Exception e) {
            plugin.logMessage("Failed to set populators: " + e.getMessage(), LogLevel.WARNING);
        }
    }

    @Override
    public void setWorldViewDistance(World world, int viewDistance) {
        try {
            Method setViewDistanceMethod = World.class.getMethod("setViewDistance", int.class);
            setViewDistanceMethod.invoke(world, viewDistance);
        } catch (Exception e) {
            plugin.logMessage("Failed to set world view distance: " + e.getMessage(), LogLevel.WARNING);
        }
    }
}