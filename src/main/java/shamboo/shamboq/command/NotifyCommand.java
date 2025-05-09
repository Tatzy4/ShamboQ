package shamboo.shamboq.command;

import org.bukkit.command.CommandSender;
import shamboo.shamboq.ShamboQ;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to toggle notifications
 */
public class NotifyCommand implements Command {
    private final ShamboQ plugin;

    public NotifyCommand(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        boolean newValue = !plugin.getConfigManager().isShowQueueDisabledMessage();
        plugin.getConfigManager().setShowQueueDisabledMessage(newValue);

        if (newValue) {
            sender.sendMessage(plugin.getMessageManager().getMessage("notifications_enabled"));
            if (!plugin.getConfigManager().isQueueEnabled()) {
                plugin.getQueueManager().startNotificationTask();
            }

            // Track notifications enabled
            plugin.getMetricsCollector().incrementCounter("notifications_enabled");
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("notifications_disabled"));
            plugin.getQueueManager().stopNotificationTask();

            // Track notifications disabled
            plugin.getMetricsCollector().incrementCounter("notifications_disabled");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}