package shamboo.shamboq.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import shamboo.shamboq.ShamboQ;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Command to toggle queue functionality
 */
public class ToggleCommand implements Command {
    private final ShamboQ plugin;

    public ToggleCommand(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        boolean newValue = !plugin.getConfigManager().isQueueEnabled();
        plugin.getConfigManager().setQueueEnabled(newValue);

        if (newValue) {
            sender.sendMessage(plugin.getMessageManager().getMessage("queue_enabled"));
            // Stop notification task
            plugin.getQueueManager().stopNotificationTask();

            // Add all frozen players to queue process
            int processedCount = 0;
            for (UUID playerId : new HashSet<>(plugin.getQueueManager().getFrozenPlayers())) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    // Remove player from previous state (if was frozen without queue)
                    plugin.getQueueManager().removeFromQueue(player);

                    // Add player to full queue process
                    plugin.getQueueManager().addToQueue(player);
                    processedCount++;
                }
            }

            if (processedCount > 0) {
                sender.sendMessage(ChatColor.GREEN + "Added " + processedCount + " players to the queue process.");
            }

            // Track toggle enable
            plugin.getMetricsCollector().incrementCounter("queue_enabled");
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("queue_disabled"));

            // NOTE: Don't release players from queue when queue is disabled

            // Start notification task
            if (plugin.getConfigManager().isShowQueueDisabledMessage()) {
                plugin.getQueueManager().startNotificationTask();
            }

            // Track toggle disable
            plugin.getMetricsCollector().incrementCounter("queue_disabled");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}