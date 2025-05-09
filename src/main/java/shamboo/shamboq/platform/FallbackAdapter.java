package shamboo.shamboq.platform;

import org.bukkit.Chunk;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import shamboo.shamboq.ShamboQ;

import java.util.List;

/**
 * Fallback platform adapter for any server
 */
public class FallbackAdapter implements PlatformAdapter {
    private final ShamboQ plugin;

    public FallbackAdapter(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendActionBar(Player player, String message) {
        // Simply send normal message as fallback
        player.sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "Queue" + ChatColor.GRAY + "] " + message);

        // Track fallback usage
        plugin.getMetricsCollector().incrementCounter("fallback_messages");
    }

    @Override
    public void setForceLoaded(Chunk chunk, boolean value) {
        // Not supported in fallback
    }

    @Override
    public void setInhabitedTime(Chunk chunk, long time) {
        // Not supported in fallback
    }

    @Override
    public void setNoTickChunk(Chunk chunk, boolean value) {
        // Not supported in fallback
    }

    @Override
    public void setPopulators(World world, List<?> populators) {
        // Not supported in fallback
    }

    @Override
    public void setWorldViewDistance(World world, int viewDistance) {
        // Not supported in fallback
    }
}