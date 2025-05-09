package shamboo.shamboq.util;

import org.bukkit.GameRule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for GameRule lookups to avoid repetitive calls
 */
public class GameRuleCache {
    private final Map<String, GameRule<?>> gameRuleCache = new ConcurrentHashMap<>();

    /**
     * Get a GameRule from cache or lookup by name
     * @param name The name of the GameRule
     * @return The GameRule or null if it doesn't exist
     */
    public GameRule<?> getGameRule(String name) {
        return gameRuleCache.computeIfAbsent(name, this::lookupGameRule);
    }

    /**
     * Look up a GameRule by name
     * @param name The name of the GameRule
     * @return The GameRule or null if it doesn't exist
     */
    private GameRule<?> lookupGameRule(String name) {
        try {
            return GameRule.getByName(name);
        } catch (Exception e) {
            // GameRule doesn't exist in this version
            return null;
        }
    }

    /**
     * Clear the cache
     */
    public void clearCache() {
        gameRuleCache.clear();
    }

    /**
     * Get the size of the cache
     * @return The number of cached GameRules
     */
    public int getCacheSize() {
        return gameRuleCache.size();
    }
}