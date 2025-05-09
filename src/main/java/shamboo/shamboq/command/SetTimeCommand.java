package shamboo.shamboq.command;

import org.bukkit.command.CommandSender;
import shamboo.shamboq.ShamboQ;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to set queue time
 */
public class SetTimeCommand implements Command {
    private final ShamboQ plugin;

    public SetTimeCommand(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().getMessage("usage_settime"));
            return true;
        }

        try {
            int newTime = Integer.parseInt(args[0]);
            plugin.getConfigManager().setQueueTime(newTime);
            sender.sendMessage(plugin.getMessageManager().getMessage("queue_time_set",
                    plugin.getConfigManager().getQueueTime()));

            // Track time change
            plugin.getMetricsCollector().incrementCounter("time_changes");
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("invalid_number", args[0]));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}