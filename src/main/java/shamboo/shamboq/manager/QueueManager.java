package shamboo.shamboq.manager;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.util.LogLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class managing player queue
 */
public class QueueManager {
    private final ShamboQ plugin;
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    private final Map<Player, BukkitTask> playerTasks = new WeakHashMap<>();
    private final Map<UUID, Integer> originalViewDistances = new HashMap<>();
    private final Map<UUID, GameMode> originalGameModes = new HashMap<>();
    private BukkitTask notificationTask;
    private BukkitTask chunkLimitTask;

    public QueueManager(ShamboQ plugin) {
        this.plugin = plugin;
    }

    public boolean isPlayerFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    /**
     * Optimizes queue area - disables mobs, AI, etc.
     */
    public void optimizeQueueArea() {
        World endWorld = plugin.getConfigManager().getEndWorld();
        Location spawnLoc = plugin.getConfigManager().createSpawnLocation();

        // Disable mob spawning and other resource-intensive features in End world
        if (plugin.getOptimizationConfig().isDisableMobs()) {
            // Disable all GameRules that cause resource usage
            endWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            endWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
            endWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            endWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
            endWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

            // Gamerules that might not exist in older versions
            try {
                endWorld.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
                endWorld.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
                endWorld.setGameRule(GameRule.DO_INSOMNIA, false);
            } catch (Exception e) {
                // Ignore if gamerule doesn't exist
            }

            // Disable AI for existing mobs nearby
            plugin.logMessage("Disabling AI for nearby mobs...", LogLevel.FINE);
            int disableRadius = 100;
            int disabledCount = 0;

            for (Entity entity : endWorld.getEntities()) {
                if (!(entity instanceof Player) && entity.getLocation().distance(spawnLoc) < disableRadius) {
                    if (entity instanceof LivingEntity) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        livingEntity.setAI(false);
                        disabledCount++;

                        // Additional flag to remove collision detection and processing
                        try {
                            Method setCollidableMethod = entity.getClass().getMethod("setCollidable", boolean.class);
                            if (setCollidableMethod != null) {
                                setCollidableMethod.invoke(entity, false);
                            }
                        } catch (Exception e) {
                            // Ignore, method may not exist in some versions
                        }
                    }
                }
            }

            plugin.logMessage("Disabled AI for " + disabledCount + " mobs", LogLevel.INFO);
            plugin.logMessage("Successfully optimized queue area", LogLevel.INFO);
        }
    }

    /**
     * Aggressive chunk management with distance limit
     */
    public void enforceChunkLimit() {
        if (!plugin.getOptimizationConfig().isOptimizeChunks() || !plugin.getOptimizationConfig().isAggressiveChunkManagement()) {
            return;
        }

        World world = plugin.getConfigManager().getEndWorld();
        int maxChunks = plugin.getOptimizationConfig().getMaxLoadedChunks();

        // Get list of all loaded chunks
        Chunk[] loadedChunks = world.getLoadedChunks();

        if (loadedChunks.length > maxChunks) {
            plugin.logMessage("Enforcing chunk limit: " + loadedChunks.length + " -> " + maxChunks, LogLevel.FINE);

            // Find chunks furthest from spawn
            Location spawnLoc = plugin.getConfigManager().createSpawnLocation();
            int spawnChunkX = spawnLoc.getBlockX() >> 4;
            int spawnChunkZ = spawnLoc.getBlockZ() >> 4;

            // Sort chunks by distance from spawn
            Arrays.sort(loadedChunks, (c1, c2) -> {
                double d1 = Math.pow(c1.getX() - spawnChunkX, 2) + Math.pow(c1.getZ() - spawnChunkZ, 2);
                double d2 = Math.pow(c2.getX() - spawnChunkX, 2) + Math.pow(c2.getZ() - spawnChunkZ, 2);
                return Double.compare(d1, d2);
            });

            // Unload all chunks above limit
            for (int i = maxChunks; i < loadedChunks.length; i++) {
                loadedChunks[i].unload(true);
            }

            plugin.logMessage("Unloaded " + (loadedChunks.length - maxChunks) + " extra chunks", LogLevel.FINE);

            // Track chunk unloads
            plugin.getMetricsCollector().incrementCounter("chunks_unloaded", loadedChunks.length - maxChunks);
        }
    }

    /**
     * Unloads unnecessary chunks, optimizing server resources
     */
    private void unloadUnnecessaryChunks(Location spawnLocation) {
        if (!plugin.getOptimizationConfig().isOptimizeChunks()) {
            return;
        }

        World world = spawnLocation.getWorld();
        if (world == null) return;

        int spawnChunkX = spawnLocation.getBlockX() >> 4;
        int spawnChunkZ = spawnLocation.getBlockZ() >> 4;

        int unloadedCount = 0;
        // Keep only chunks within distance 1 from spawn
        for (Chunk chunk : world.getLoadedChunks()) {
            if (Math.abs(chunk.getX() - spawnChunkX) > 1 ||
                    Math.abs(chunk.getZ() - spawnChunkZ) > 1) {
                chunk.unload(true);
                unloadedCount++;
            }
        }

        plugin.logMessage("Unloaded " + unloadedCount + " unnecessary chunks around spawn", LogLevel.FINE);

        // Track chunk unloads
        plugin.getMetricsCollector().incrementCounter("chunks_unloaded", unloadedCount);
    }

    /**
     * Adds player to queue with full process (welcome, countdown, sounds)
     */
    public void addToQueue(Player player) {
        final UUID playerId = player.getUniqueId();

        // Add player to frozen list
        frozenPlayers.add(playerId);

        // Optimization - spectator mode for players in queue
        if (plugin.getOptimizationConfig().isSpectatorMode()) {
            // Save original game mode
            originalGameModes.put(playerId, player.getGameMode());
            // Set spectator mode, which significantly reduces resource usage
            player.setGameMode(GameMode.SPECTATOR);
        }

        // Teleport player to established location
        Location spawnLocation = plugin.getConfigManager().createSpawnLocation();
        player.teleport(spawnLocation);

        // Optimization - unload unnecessary chunks
        if (plugin.getOptimizationConfig().isOptimizeChunks()) {
            unloadUnnecessaryChunks(spawnLocation);

            // Aggressive chunk management
            if (plugin.getOptimizationConfig().isAggressiveChunkManagement()) {
                enforceChunkLimit();
            }
        }

        // Optimization - disable player ticking
        if (plugin.getOptimizationConfig().isDisablePlayerTicks()) {
            pausePlayerTicking(player);
        }

        // Hide player from others and others from this player
        hidePlayerFromOthers(player);

        // Optimization - reduce view distance for player in queue
        if (plugin.getOptimizationConfig().isReduceViewDistance()) {
            // Save original view distance and set low one
            originalViewDistances.put(playerId, getPlayerViewDistance(player));
            setReducedViewDistance(player);
        }

        // Display welcome message
        int queueTime = plugin.getConfigManager().getQueueTime();
        player.sendTitle(
                plugin.getMessageManager().getMessage("welcome_title"),
                plugin.getMessageManager().getMessage("welcome_subtitle", queueTime),
                10, 70, 20
        );

        // Play welcome sound
        plugin.getSoundManager().playRandomSound(player);

        // Start task for player (combined countdown and sounds)
        startPlayerTask(player);
        plugin.logMessage("Added player " + player.getName() + " to queue", LogLevel.INFO);

        // Track queue adds
        plugin.getMetricsCollector().incrementCounter("players_queued");
    }

    /**
     * Stops player ticking, significantly reducing CPU usage
     */
    private void pausePlayerTicking(Player player) {
        try {
            // Use NMS or reflection to stop player processing
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);

            // Try to find 'frozen' field or similar
            Field frozenField = null;
            try {
                frozenField = entityPlayer.getClass().getDeclaredField("frozen");
            } catch (NoSuchFieldException e) {
                // Try alternative field names
                try {
                    frozenField = entityPlayer.getClass().getDeclaredField("ticksDisabled");
                } catch (NoSuchFieldException e2) {
                    try {
                        frozenField = entityPlayer.getClass().getDeclaredField("noTickTime");
                    } catch (NoSuchFieldException e3) {
                        // No field to stop ticks
                    }
                }
            }

            if (frozenField != null) {
                frozenField.setAccessible(true);

                // Check field type and set appropriate value
                if (frozenField.getType() == boolean.class) {
                    frozenField.setBoolean(entityPlayer, true);
                } else if (frozenField.getType() == int.class) {
                    frozenField.setInt(entityPlayer, Integer.MAX_VALUE);
                } else if (frozenField.getType() == long.class) {
                    frozenField.setLong(entityPlayer, Long.MAX_VALUE);
                }

                plugin.logMessage("Successfully paused ticks for player: " + player.getName(), LogLevel.FINE);

                // Track tick pause
                plugin.getMetricsCollector().incrementCounter("ticks_paused");
            }
        } catch (Exception e) {
            // Ignore if method doesn't work
            plugin.logMessage("Failed to pause player ticks: " + e.getMessage(), LogLevel.FINE);
        }
    }

    /**
     * Resumes player ticking before teleportation
     */
    private void resumePlayerTicking(Player player) {
        try {
            // Use NMS or reflection to resume player processing
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);

            // Try to find 'frozen' field or similar
            Field frozenField = null;
            try {
                frozenField = entityPlayer.getClass().getDeclaredField("frozen");
            } catch (NoSuchFieldException e) {
                // Try alternative field names
                try {
                    frozenField = entityPlayer.getClass().getDeclaredField("ticksDisabled");
                } catch (NoSuchFieldException e2) {
                    try {
                        frozenField = entityPlayer.getClass().getDeclaredField("noTickTime");
                    } catch (NoSuchFieldException e3) {
                        // No field to stop ticks
                    }
                }
            }

            if (frozenField != null) {
                frozenField.setAccessible(true);

                // Check field type and set appropriate value
                if (frozenField.getType() == boolean.class) {
                    frozenField.setBoolean(entityPlayer, false);
                } else if (frozenField.getType() == int.class || frozenField.getType() == long.class) {
                    frozenField.set(entityPlayer, 0);
                }

                // Track tick resume
                plugin.getMetricsCollector().incrementCounter("ticks_resumed");
            }
        } catch (Exception e) {
            // Ignore if method doesn't work
        }
    }

    /**
     * Adds player to frozen list without starting queue process
     */
    public void freezePlayerWithoutQueue(Player player) {
        final UUID playerId = player.getUniqueId();

        // Add player to frozen list
        frozenPlayers.add(playerId);

        // Teleport player to established location
        Location spawnLocation = plugin.getConfigManager().createSpawnLocation();
        player.teleport(spawnLocation);

        // Optimization - unload unnecessary chunks
        if (plugin.getOptimizationConfig().isOptimizeChunks()) {
            unloadUnnecessaryChunks(spawnLocation);
        }

        // Optimization - reduce view distance for player in queue
        if (plugin.getOptimizationConfig().isReduceViewDistance()) {
            // Save original view distance and set low one
            originalViewDistances.put(playerId, getPlayerViewDistance(player));
            setReducedViewDistance(player);
        }

        // Hide player from others and others from this player
        hidePlayerFromOthers(player);

        // Don't start countdown or sounds, just notify about freezing
        plugin.getVersionManager().sendActionBar(player,
                plugin.getMessageManager().getMessage("frozen_player"));

        plugin.logMessage("Froze player " + player.getName() + " without queue", LogLevel.INFO);

        // Track frozen players
        plugin.getMetricsCollector().incrementCounter("players_frozen");
    }

    private int getPlayerViewDistance(Player player) {
        try {
            Method getViewDistanceMethod = Player.class.getMethod("getViewDistance");
            return (int) getViewDistanceMethod.invoke(player);
        } catch (Exception e) {
            // Fallback if method doesn't exist
            return Bukkit.getViewDistance();
        }
    }

    private void setReducedViewDistance(Player player) {
        try {
            int reducedDistance = plugin.getOptimizationConfig().getQueueViewDistance();
            Method setViewDistanceMethod = Player.class.getMethod("setViewDistance", int.class);
            setViewDistanceMethod.invoke(player, reducedDistance);
            plugin.logMessage("Reduced view distance for " + player.getName() + " to " + reducedDistance, LogLevel.FINE);
        } catch (Exception e) {
            // Ignore if method doesn't exist
        }
    }

    private void restoreViewDistance(Player player) {
        UUID playerId = player.getUniqueId();
        if (plugin.getOptimizationConfig().isReduceViewDistance() && originalViewDistances.containsKey(playerId)) {
            try {
                int originalDistance = originalViewDistances.remove(playerId);
                Method setViewDistanceMethod = Player.class.getMethod("setViewDistance", int.class);
                setViewDistanceMethod.invoke(player, originalDistance);
                plugin.logMessage("Restored view distance for " + player.getName() + " to " + originalDistance, LogLevel.FINE);
            } catch (Exception e) {
                // Ignore if method doesn't exist
            }
        }
    }

    public void removeFromQueue(Player player) {
        UUID playerId = player.getUniqueId();

        // Remove from frozen list
        frozenPlayers.remove(playerId);

        // Cancel task for this player
        cancelPlayerTask(player);

        // Resume player ticking
        if (plugin.getOptimizationConfig().isDisablePlayerTicks()) {
            resumePlayerTicking(player);
        }

        // Restore original game mode
        if (plugin.getOptimizationConfig().isSpectatorMode() && originalGameModes.containsKey(playerId)) {
            GameMode originalMode = originalGameModes.remove(playerId);
            player.setGameMode(originalMode);
        }

        // Restore visibility
        makePlayerVisible(player);

        // Restore original view distance
        restoreViewDistance(player);

        plugin.logMessage("Removed player " + player.getName() + " from queue", LogLevel.INFO);

        // Track queue removals
        plugin.getMetricsCollector().incrementCounter("players_unqueued");
    }

    public void releaseAllPlayers() {
        // Release all frozen players
        int releasedCount = 0;
        for (UUID uuid : new HashSet<>(frozenPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeFromQueue(player);
                player.sendMessage(plugin.getMessageManager().getMessage("queue_disabled_player"));
                releasedCount++;
            }
        }

        plugin.logMessage("Released " + releasedCount + " players from queue", LogLevel.INFO);

        // Track mass releases
        if (releasedCount > 0) {
            plugin.getMetricsCollector().incrementCounter("mass_release_count");
            plugin.getMetricsCollector().incrementCounter("mass_released_players", releasedCount);
        }
    }

    public void cancelAllTasks() {
        // Cancel notification task
        stopNotificationTask();

        // Cancel all player tasks
        for (BukkitTask task : new ArrayList<>(playerTasks.values())) {
            if (task != null) {
                task.cancel();
            }
        }

        playerTasks.clear();
        plugin.logMessage("Canceled all tasks", LogLevel.INFO);
    }

    public void cleanup() {
        frozenPlayers.clear();
        playerTasks.clear();
        originalViewDistances.clear();
        originalGameModes.clear();

        // Stop chunk limiting task if running
        if (chunkLimitTask != null) {
            chunkLimitTask.cancel();
            chunkLimitTask = null;
        }

        plugin.logMessage("Cleared queue state data", LogLevel.FINE);
    }

    /**
     * Starts periodic chunk limiting task
     */
    public void startChunkLimitTask() {
        if (!plugin.getOptimizationConfig().isOptimizeChunks() || !plugin.getOptimizationConfig().isAggressiveChunkManagement()) {
            return;
        }

        // Cancel existing task
        if (chunkLimitTask != null) {
            chunkLimitTask.cancel();
        }

        // Start new task
        chunkLimitTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!frozenPlayers.isEmpty()) {
                    enforceChunkLimit();
                }
            }
        }.runTaskTimer(plugin, 100L, 200L); // Every 10 seconds (200 ticks)

        plugin.logMessage("Started chunk limiting task", LogLevel.INFO);
    }

    public void startNotificationTask() {
        // Cancel existing task if running
        stopNotificationTask();

        notificationTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Skip players with bypass permission
                    if (player.hasPermission("shamboq.bypass")) {
                        continue;
                    }

                    // Send message in action bar
                    plugin.getVersionManager().sendActionBar(player,
                            ChatColor.RED + plugin.getConfigManager().getQueueDisabledMessage());
                }
            }
        }.runTaskTimer(plugin, 20L, plugin.getConfigManager().getNotificationInterval() * 20L);

        plugin.logMessage("Started notifications task for disabled queue", LogLevel.INFO);
    }

    public void stopNotificationTask() {
        if (notificationTask != null) {
            notificationTask.cancel();
            notificationTask = null;
            plugin.logMessage("Stopped notifications task", LogLevel.FINE);
        }
    }

    private void startPlayerTask(final Player player) {
        final UUID playerId = player.getUniqueId();
        final int queueTime = plugin.getConfigManager().getQueueTime();

        // Use dedicated thread pool if enabled
        if (plugin.getOptimizationConfig().isDedicatedThreadPool() && plugin.getQueueThreadPool() != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    processQueuePlayer(player, playerId, queueTime);
                } catch (Exception e) {
                    plugin.logMessage("Error in queue thread: " + e.getMessage(), LogLevel.ERROR);
                }
            }, plugin.getQueueThreadPool());

            plugin.logMessage("Started queue task for " + player.getName() + " in dedicated thread", LogLevel.FINE);
            return;
        }

        // If we don't use dedicated thread pool, use standard Bukkit scheduler
        BukkitTask task = new BukkitRunnable() {
            int timeLeft = queueTime;
            int soundCounter = 0;
            // OPTIMIZATION: Reduce sound frequency (1 sound per second instead of 4)
            int maxSoundCount = queueTime; // Was: queueTime * 4

            @Override
            public void run() {
                // Check if player is online
                if (!player.isOnline() || !frozenPlayers.contains(playerId)) {
                    playerTasks.remove(player);
                    this.cancel();
                    return;
                }

                // Countdown handling (every second)
                if (soundCounter % 4 == 0) { // Every 4 iterations = 1 second
                    if (timeLeft <= 0) {
                        // Time expired, transfer player but only if queue is enabled
                        if (plugin.getConfigManager().isQueueEnabled()) {
                            // Restore normal settings before teleportation
                            if (plugin.getOptimizationConfig().isDisablePlayerTicks()) {
                                resumePlayerTicking(player);
                            }

                            // Restore game mode
                            if (plugin.getOptimizationConfig().isSpectatorMode() && originalGameModes.containsKey(playerId)) {
                                GameMode originalMode = originalGameModes.remove(playerId);
                                player.setGameMode(originalMode);
                            }

                            // NOTE: Don't remove player from frozen list until the connection is confirmed successful
                            // We'll keep the player's visibility settings until connection is confirmed

                            // Use ConnectionHandler to send to server with error handling
                            plugin.getConnectionHandler().sendToServer(player, plugin.getConfigManager().getSmpServer());

                            // Cancel further countdowns but leave player frozen until connection is confirmed
                            playerTasks.remove(player);
                            this.cancel();
                            return;
                        }
                        frozenPlayers.remove(playerId);
                        playerTasks.remove(player);
                        this.cancel();
                        return;
                    }

                    // Update countdown
                    plugin.getVersionManager().sendActionBar(player,
                            plugin.getMessageManager().getMessage("countdown", timeLeft));

                    // OPTIMIZATION: Only 1 sound per second instead of 4
                    if (soundCounter < maxSoundCount) {
                        plugin.getSoundManager().playRandomSound(player);
                    }

                    timeLeft--;
                }

                soundCounter++;
            }
        }.runTaskTimer(plugin, 5L, 5L); // Every 5 ticks (0.25 seconds)

        // Save task
        playerTasks.put(player, task);
        plugin.logMessage("Started queue task for " + player.getName() + " in main thread", LogLevel.FINE);
    }

    /**
     * Function handling player in queue for dedicated thread pool
     */
    private void processQueuePlayer(Player player, UUID playerId, int queueTime) {
        // Function running in dedicated thread from pool
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (queueTime * 1000L);
        int soundsPlayed = 0;

        try {
            while (System.currentTimeMillis() < endTime) {
                // Synchronize with main thread only what's necessary
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Check if player is online
                        if (!player.isOnline() || !frozenPlayers.contains(playerId)) {
                            return;
                        }

                        // Update countdown
                        long currentTime = System.currentTimeMillis();
                        int secondsLeft = (int)((endTime - currentTime) / 1000) + 1;

                        plugin.getVersionManager().sendActionBar(player,
                                plugin.getMessageManager().getMessage("countdown", secondsLeft));
                    }
                }.runTask(plugin);

                // Play sounds once per second
                if (soundsPlayed < queueTime) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && frozenPlayers.contains(playerId)) {
                                plugin.getSoundManager().playRandomSound(player);
                            }
                        }
                    }.runTask(plugin);
                    soundsPlayed++;
                }

                // Check status every second
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }

            // After countdown finishes, transfer player
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && frozenPlayers.contains(playerId)) {
                        if (plugin.getConfigManager().isQueueEnabled()) {
                            // Restore normal settings before teleportation
                            if (plugin.getOptimizationConfig().isDisablePlayerTicks()) {
                                resumePlayerTicking(player);
                            }

                            // Restore game mode
                            if (plugin.getOptimizationConfig().isSpectatorMode() && originalGameModes.containsKey(playerId)) {
                                GameMode originalMode = originalGameModes.remove(playerId);
                                player.setGameMode(originalMode);
                            }

                            // NOTE: Don't remove player from frozen list until the connection is confirmed successful
                            // We'll keep the player's visibility settings until connection is confirmed

                            // Use ConnectionHandler to send to server with error handling
                            plugin.getConnectionHandler().sendToServer(player, plugin.getConfigManager().getSmpServer());
                            plugin.logMessage("Moving player " + player.getName() + " after countdown finished", LogLevel.INFO);

                            // Player remains frozen until connection is confirmed or fails
                            playerTasks.remove(player);
                        } else {
                            frozenPlayers.remove(playerId);
                            playerTasks.remove(player);
                        }
                    }
                }
            }.runTask(plugin);
        } catch (Exception e) {
            plugin.logMessage("Error processing player in queue: " + e.getMessage(), LogLevel.ERROR);
            e.printStackTrace();
        }
    }

    private void cancelPlayerTask(Player player) {
        BukkitTask task = playerTasks.remove(player);
        if (task != null) {
            task.cancel();
            plugin.logMessage("Canceled queue task for " + player.getName(), LogLevel.FINE);
        }
    }

    private void hidePlayerFromOthers(Player player) {
        // Hide this player from all other players and hide all others from this player
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player otherPlayer : onlinePlayers) {
            if (!otherPlayer.equals(player)) {
                otherPlayer.hidePlayer(plugin, player);
                player.hidePlayer(plugin, otherPlayer);
            }
        }
    }

    private void makePlayerVisible(Player player) {
        // Skip if player is not online
        if (!player.isOnline()) {
            return;
        }

        // Restore visibility
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player otherPlayer : onlinePlayers) {
            if (otherPlayer.isOnline() && !otherPlayer.equals(player)) {
                otherPlayer.showPlayer(plugin, player);
                player.showPlayer(plugin, otherPlayer);
            }
        }
    }

    // Getters
    public Set<UUID> getFrozenPlayers() {
        return new HashSet<>(frozenPlayers);
    }
}