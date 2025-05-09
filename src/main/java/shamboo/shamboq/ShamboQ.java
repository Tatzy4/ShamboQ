package shamboo.shamboq;

import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import shamboo.shamboq.config.ConfigManager;
import shamboo.shamboq.config.OptimizationConfig;
import shamboo.shamboq.connection.ConnectionHandler;
import shamboo.shamboq.event.PlayerEventListener;
import shamboo.shamboq.manager.*;
import shamboo.shamboq.util.LogLevel;
import shamboo.shamboq.util.MetricsCollector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main ShamboQ plugin class responsible for player queue management
 * Optimized for extreme server resource usage reduction
 * Version: 2.1
 */
public final class ShamboQ extends JavaPlugin {

    // Manager instances
    private QueueManager queueManager;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private VersionManager versionManager;
    private CommandManager commandManager;
    private SoundManager soundManager;
    private ConnectionHandler connectionHandler;
    private MetricsCollector metricsCollector;

    // Configuration
    private OptimizationConfig optimizationConfig;
    private ExecutorService queueThreadPool;

    private void displayStartupBanner() {
        getLogger().info("ShamboQ v" + getDescription().getVersion() + " - Queue system for Bukkit/Spigot/Paper");
        getLogger().info("Developed by §bShambonoor Ent. §7| Website: https://szam.boo/");
        getLogger().info("⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯");
    }

    /**
     * Enhanced logging method for the plugin
     * @param message Message to log
     * @param level Log level
     */
    public void logMessage(String message, LogLevel level) {
        switch (level) {
            case INFO:
                getLogger().info("[INFO]" + message);
                break;
            case FINE:
                if (getConfig().getBoolean("debug", false)) {
                    getLogger().info("[DEBUG]" + message);
                }
                break;
            case WARNING:
                getLogger().warning("[WARN]" + message);
                break;
            case ERROR:
                getLogger().severe("[ERROR]" + message);
                break;
        }

        // Track error metrics
        if (level == LogLevel.ERROR) {
            metricsCollector.incrementCounter("errors");
        }
    }

    @Override
    public void onEnable() {
        // Display logo and startup information
        displayStartupBanner();

        logMessage("Enabling ShamboQ...", LogLevel.INFO);

        // Save default configuration if it doesn't exist
        saveDefaultConfig();

        // Initialize metrics collector
        metricsCollector = new MetricsCollector();

        // Optimization configuration
        logMessage("Initializing config.yaml...", LogLevel.INFO);
        loadOptimizationConfig();

        // Initialize managers - all in one log instead of many
        logMessage("Initializing managers...", LogLevel.INFO);

        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        versionManager = new VersionManager(this);
        soundManager = new SoundManager(this);
        queueManager = new QueueManager(this);
        commandManager = new CommandManager(this);

        // Initialize connection handler
        connectionHandler = new ConnectionHandler(this);

        // Register BungeeCord/Velocity channel and commands
        logMessage("Registering commands...", LogLevel.INFO);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getCommand("shamboq").setExecutor(commandManager);
        getCommand("shamboq").setTabCompleter(commandManager);

        // Register event handlers
        registerEventHandlers();

        // Optimizations
        if (optimizationConfig.isDisableMobs()) {
            logMessage("Enabling optimizations...", LogLevel.INFO);
            queueManager.optimizeQueueArea();
        }

        if (optimizationConfig.isSetupNoTickZone()) {
            logMessage("Setting up NoTick zone...", LogLevel.INFO);
            configManager.setupNoTickZone();
        }

        if (optimizationConfig.isDisableTerrainGeneration()) {
            logMessage("Disabling terrain generator...", LogLevel.INFO);
            configManager.setupMinimalistEndWorld();
        }

        // Start periodic tasks
        logMessage("Enabling events...", LogLevel.INFO);
        startTasks();

        // Startup information
        logMessage("Running! " +
                (configManager.isQueueEnabled() ? "QUEUE ENABLED" : "QUEUE DISABLED"), LogLevel.INFO);
    }

    @Override
    public void onDisable() {
        logMessage("Disabling ShamboQ...", LogLevel.INFO);

        logMessage("Canceling events...", LogLevel.INFO);
        // Cancel all tasks
        queueManager.cancelAllTasks();

        // Release all players from queue
        queueManager.releaseAllPlayers();

        // Clear lists and maps
        queueManager.cleanup();

        // Shutdown connection handler
        if (connectionHandler != null) {
            connectionHandler.shutdown();
        }

        // Restore world settings
        logMessage("Restoring world properties...", LogLevel.INFO);
        configManager.restoreGameRules();

        // Close thread pool if it was used
        if (optimizationConfig.isDedicatedThreadPool() && queueThreadPool != null) {
            queueThreadPool.shutdown();
            logMessage("Closing threads", LogLevel.INFO);
        }

        // Log final metrics
        logMessage("Final metrics: " + metricsCollector.getAllMetrics(), LogLevel.INFO);

        logMessage("ShamboQ has been disabled!", LogLevel.INFO);
    }

    /**
     * Loads optimization configuration
     */
    private void loadOptimizationConfig() {
        // Add defaults and save
        getConfig().addDefault("optimization.chunk-management", true);
        getConfig().addDefault("optimization.disable-mobs", true);
        getConfig().addDefault("optimization.reduce-view-distance", true);
        getConfig().addDefault("optimization.no-tick-zone", true);
        getConfig().addDefault("optimization.queue-view-distance", 2);
        getConfig().addDefault("optimization.disable-terrain-generation", true);
        getConfig().addDefault("optimization.disable-player-ticks", true);
        getConfig().addDefault("optimization.spectator-mode", true);
        getConfig().addDefault("optimization.dedicated-thread-pool", true);
        getConfig().addDefault("optimization.thread-pool-size", 1);
        getConfig().addDefault("optimization.aggressive-chunk-management", true);
        getConfig().addDefault("optimization.max-loaded-chunks", 9);
        getConfig().addDefault("debug", false);

        getConfig().options().copyDefaults(true);
        saveConfig();

        // Build optimization config using builder pattern
        OptimizationConfig.Builder builder = new OptimizationConfig.Builder()
                .optimizeChunks(getConfig().getBoolean("optimization.chunk-management"))
                .disableMobs(getConfig().getBoolean("optimization.disable-mobs"))
                .reduceViewDistance(getConfig().getBoolean("optimization.reduce-view-distance"))
                .setupNoTickZone(getConfig().getBoolean("optimization.no-tick-zone"))
                .disableTerrainGeneration(getConfig().getBoolean("optimization.disable-terrain-generation"))
                .disablePlayerTicks(getConfig().getBoolean("optimization.disable-player-ticks"))
                .spectatorMode(getConfig().getBoolean("optimization.spectator-mode"))
                .dedicatedThreadPool(getConfig().getBoolean("optimization.dedicated-thread-pool"))
                .threadPoolSize(getConfig().getInt("optimization.thread-pool-size"))
                .aggressiveChunkManagement(getConfig().getBoolean("optimization.aggressive-chunk-management"))
                .maxLoadedChunks(getConfig().getInt("optimization.max-loaded-chunks"))
                .queueViewDistance(getConfig().getInt("optimization.queue-view-distance", 2));

        this.optimizationConfig = builder.build();

        // Initialize thread pool if enabled
        if (optimizationConfig.isDedicatedThreadPool()) {
            queueThreadPool = Executors.newFixedThreadPool(optimizationConfig.getThreadPoolSize());
            logMessage("Initialized queue thread pool (size: " + optimizationConfig.getThreadPoolSize() + ")", LogLevel.INFO);
        }

        // Instead of many logs, one collective log with settings
        logMessage("Loaded optimization settings: chunks=" + optimizationConfig.isOptimizeChunks() +
                ", mobs=" + optimizationConfig.isDisableMobs() +
                ", visibility=" + optimizationConfig.isReduceViewDistance() +
                ", no-tick=" + optimizationConfig.isSetupNoTickZone() +
                ", terrain=" + optimizationConfig.isDisableTerrainGeneration() +
                ", ticks=" + optimizationConfig.isDisablePlayerTicks() +
                ", spectator=" + optimizationConfig.isSpectatorMode(), LogLevel.INFO);
    }

    /**
     * Registers event handlers
     */
    private void registerEventHandlers() {
        PlayerEventListener playerEventListener = new PlayerEventListener(this);
        getServer().getPluginManager().registerEvents(playerEventListener, this);
    }

    /**
     * Starts plugin periodic tasks
     */
    private void startTasks() {
        // Start notification task if queue is disabled
        if (!configManager.isQueueEnabled() && configManager.isShowQueueDisabledMessage()) {
            queueManager.startNotificationTask();
        }

        // Start chunk limitation task if enabled
        if (optimizationConfig.isOptimizeChunks() && optimizationConfig.isAggressiveChunkManagement()) {
            queueManager.startChunkLimitTask();
        }
    }

    // Manager access
    public QueueManager getQueueManager() {
        return queueManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    public OptimizationConfig getOptimizationConfig() {
        return optimizationConfig;
    }

    public ExecutorService getQueueThreadPool() {
        return queueThreadPool;
    }
}