package shamboo.shamboq.command;

import org.bukkit.command.CommandSender;
import shamboo.shamboq.ShamboQ;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to set queue message
 */
public class MessageCommand implements Command {
    private final ShamboQ plugin;

    public MessageCommand(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().getMessage("usage_message"));
            return true;
        }

        // Combine all remaining arguments into message
        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }

        // Trim and save
        String message = messageBuilder.toString().trim();
        plugin.getConfigManager().setQueueDisabledMessage(message);

        sender.sendMessage(plugin.getMessageManager().getMessage("message_set", message));

        // Update notification task if running
        if (!plugin.getConfigManager().isQueueEnabled() &&
                plugin.getConfigManager().isShowQueueDisabledMessage()) {
            plugin.getQueueManager().startNotificationTask(); // Restart to use new message
        }

        // Track message changes
        plugin.getMetricsCollector().incrementCounter("message_changes");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}