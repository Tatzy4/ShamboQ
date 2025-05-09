package shamboo.shamboq.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.config.OptimizationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Command to show plugin status
 */
public class StatusCommand implements Command {
    private final ShamboQ plugin;

    public StatusCommand(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OptimizationConfig config = plugin.getOptimizationConfig();

        sender.sendMessage(ChatColor.GOLD + "ShamboQ Status:");
        sender.sendMessage(ChatColor.YELLOW + "Queue Enabled: " +
                (plugin.getConfigManager().isQueueEnabled() ? "Yes" : "No"));
        sender.sendMessage(ChatColor.YELLOW + "Queue Time: " +
                plugin.getConfigManager().getQueueTime() + " seconds");
        sender.sendMessage(ChatColor.YELLOW + "SMP Server: " +
                plugin.getConfigManager().getSmpServer());
        sender.sendMessage(ChatColor.YELLOW + "Show Disabled Message: " +
                (plugin.getConfigManager().isShowQueueDisabledMessage() ? "Yes" : "No"));
        sender.sendMessage(ChatColor.YELLOW + "Disabled Message: \"" +
                plugin.getConfigManager().getQueueDisabledMessage() + "\"");
        sender.sendMessage(ChatColor.YELLOW + "Players in Queue: " +
                plugin.getQueueManager().getFrozenPlayers().size());

        // Add connection related information
        int connectionAttempts = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getConnectionHandler().hasOngoingConnectionAttempt(player.getUniqueId())) {
                connectionAttempts++;
            }
        }
        sender.sendMessage(ChatColor.YELLOW + "Players in Connection Process: " + connectionAttempts);

        sender.sendMessage(ChatColor.GOLD + "Optimizations:");
        sender.sendMessage(ChatColor.YELLOW + "  - Chunk Management: " +
                (config.isOptimizeChunks() ? "Enabled" : "Disabled"));
        if (config.isOptimizeChunks() && config.isAggressiveChunkManagement()) {
            sender.sendMessage(ChatColor.YELLOW + "    ↳ Aggressive Management: Enabled (max " +
                    config.getMaxLoadedChunks() + " chunks)");
        }
        sender.sendMessage(ChatColor.YELLOW + "  - Disable Mobs: " +
                (config.isDisableMobs() ? "Enabled" : "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "  - Reduced View Distance: " +
                (config.isReduceViewDistance() ? "Enabled (" + config.getQueueViewDistance() + ")" : "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "  - No-Tick Zone: " +
                (config.isSetupNoTickZone() ? "Enabled" : "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "  - Disable Terrain Generation: " +
                (config.isDisableTerrainGeneration() ? "Enabled" : "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "  - Disable Player Ticks: " +
                (config.isDisablePlayerTicks() ? "Enabled" : "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "  - Spectator Mode: " +
                (config.isSpectatorMode() ? "Enabled" : "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "  - Dedicated Thread Pool: " +
                (config.isDedicatedThreadPool() ? "Enabled (" + config.getThreadPoolSize() + " thread" +
                        (config.getThreadPoolSize() > 1 ? "s" : "") + ")" : "Disabled"));

        // Connection handler information
        sender.sendMessage(ChatColor.GOLD + "Connection Settings:");
        sender.sendMessage(ChatColor.YELLOW + "  - Max Retries: " +
                plugin.getConfig().getInt("connection.max-retries", 3));
        sender.sendMessage(ChatColor.YELLOW + "  - Retry Delay: " +
                plugin.getConfig().getInt("connection.retry-delay", 5) + " seconds");

        // Memory usage information
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        sender.sendMessage(ChatColor.GOLD + "System Resources:");
        sender.sendMessage(ChatColor.YELLOW + "  - Memory Usage: " +
                usedMemory + "MB / " + totalMemory + "MB");

        // Plugin metrics
        sender.sendMessage(ChatColor.GOLD + "Plugin Metrics:");
        Map<String, Long> metrics = plugin.getMetricsCollector().getAllMetrics();
        metrics.forEach((key, value) -> {
            sender.sendMessage(ChatColor.YELLOW + "  - " + key + ": " + value);
        });

        // System information
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        sender.sendMessage(ChatColor.YELLOW + "  - Java: " + javaVersion);
        sender.sendMessage(ChatColor.YELLOW + "  - OS: " + osName);

        // Track status command
        plugin.getMetricsCollector().incrementCounter("status_checks");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}