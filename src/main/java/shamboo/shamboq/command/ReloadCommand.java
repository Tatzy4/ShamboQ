package shamboo.shamboq.command;

import org.bukkit.command.CommandSender;
import shamboo.shamboq.ShamboQ;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to reload plugin configuration
 */
public class ReloadCommand implements Command {
    private final ShamboQ plugin;

    public ReloadCommand(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Stop all tasks before reloading
        plugin.getQueueManager().stopNotificationTask();
        plugin.getQueueManager().cancelAllTasks();

        // Reload configuration
        plugin.getConfigManager().reload();
        plugin.getMessageManager().reload();
        plugin.getSoundManager().reload();

        // Update optimizations
        if (plugin.getOptimizationConfig().isDisableMobs()) {
            plugin.getQueueManager().optimizeQueueArea();
        }

        if (plugin.getOptimizationConfig().isSetupNoTickZone()) {
            plugin.getConfigManager().setupNoTickZone();
        }

        // Restart notification task if needed
        if (!plugin.getConfigManager().isQueueEnabled() &&
                plugin.getConfigManager().isShowQueueDisabledMessage()) {
            plugin.getQueueManager().startNotificationTask();
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("config_reloaded"));

        // Track reloads
        plugin.getMetricsCollector().incrementCounter("config_reloads");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}