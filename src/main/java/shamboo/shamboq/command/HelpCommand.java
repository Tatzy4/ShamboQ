package shamboo.shamboq.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import shamboo.shamboq.ShamboQ;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to show help information
 */
public class HelpCommand implements Command {
    private final ShamboQ plugin;

    public HelpCommand(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "ShamboQ Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/shamboq toggle - Enable or disable the queue");
        sender.sendMessage(ChatColor.YELLOW + "/shamboq settime <seconds> - Set the queue time");
        sender.sendMessage(ChatColor.YELLOW + "/shamboq message <text> - Set the queue disabled message");
        sender.sendMessage(ChatColor.YELLOW + "/shamboq notify - Toggle queue disabled notifications");
        sender.sendMessage(ChatColor.YELLOW + "/shamboq send <player> - Send a player to the SMP server");
        sender.sendMessage(ChatColor.YELLOW + "/shamboq reload - Reload the configuration");
        sender.sendMessage(ChatColor.YELLOW + "/shamboq status - Show the current status");

        // Track help command usage
        plugin.getMetricsCollector().incrementCounter("help_views");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}