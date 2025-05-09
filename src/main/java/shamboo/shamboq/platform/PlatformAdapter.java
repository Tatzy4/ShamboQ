package shamboo.shamboq.platform;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Interface for handling platform-specific functionality
 */
public interface PlatformAdapter {
    /**
     * Sends an action bar message to a player
     */
    void sendActionBar(Player player, String message);

    /**
     * Sets whether a chunk is force loaded
     */
    void setForceLoaded(Chunk chunk, boolean value);

    /**
     * Sets the inhabited time for a chunk
     */
    void setInhabitedTime(Chunk chunk, long time);

    /**
     * Sets whether a chunk is a no-tick chunk (Paper only)
     */
    void setNoTickChunk(Chunk chunk, boolean value);

    /**
     * Sets world populators (Paper only)
     */
    void setPopulators(World world, List<?> populators);

    /**
     * Sets world view distance (Paper only)
     */
    void setWorldViewDistance(World world, int viewDistance);
}