package shamboo.shamboq.manager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.command.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Class managing commands
 */
public class CommandManager implements CommandExecutor, TabCompleter {
    private final ShamboQ plugin;
    private final Map<String, shamboo.shamboq.command.Command> subCommands = new HashMap<>();

    // Command rate limiting
    private final Map<UUID, Map<String, Long>> commandCooldowns = new HashMap<>();

    public CommandManager(ShamboQ plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("toggle", new ToggleCommand(plugin));
        subCommands.put("settime", new SetTimeCommand(plugin));
        subCommands.put("message", new MessageCommand(plugin));
        subCommands.put("notify", new NotifyCommand(plugin));
        subCommands.put("send", new SendCommand(plugin));
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("status", new StatusCommand(plugin));
        subCommands.put("help", new HelpCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shamboq")) {
            // Check if sender has permission
            if (!sender.hasPermission("shamboq.admin")) {
                sender.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                return true;
            }

            // Check rate limiting if it's a player
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!checkCommandCooldown(player, "shamboq", 1)) {
                    return true;
                }
            }

            // Track command usage
            plugin.getMetricsCollector().incrementCounter("command_uses");

            // No arguments - show help
            if (args.length == 0) {
                subCommands.get("help").execute(sender, new String[0]);
                return true;
            }

            // Handle subcommands
            String subCommandName = args[0].toLowerCase();
            if (subCommands.containsKey(subCommandName)) {
                // Track specific command usage
                plugin.getMetricsCollector().incrementCounter("command_" + subCommandName);

                return subCommands.get(subCommandName).execute(sender,
                        Arrays.copyOfRange(args, 1, args.length));
            } else {
                subCommands.get("help").execute(sender, new String[0]);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("shamboq")) {
            if (!sender.hasPermission("shamboq.admin")) {
                return completions;
            }

            if (args.length == 1) {
                String partialCommand = args[0].toLowerCase();
                for (String subCommand : subCommands.keySet()) {
                    if (subCommand.startsWith(partialCommand)) {
                        completions.add(subCommand);
                    }
                }
            } else if (args.length > 1) {
                String subCommandName = args[0].toLowerCase();
                if (subCommands.containsKey(subCommandName)) {
                    return subCommands.get(subCommandName).tabComplete(sender,
                            Arrays.copyOfRange(args, 1, args.length));
                }
            }
        }

        return completions;
    }

    private boolean checkCommandCooldown(Player player, String command, int cooldownSeconds) {
        UUID playerId = player.getUniqueId();

        commandCooldowns.putIfAbsent(playerId, new HashMap<>());
        Map<String, Long> playerCooldowns = commandCooldowns.get(playerId);

        long now = System.currentTimeMillis();
        Long lastUse = playerCooldowns.get(command);

        if (lastUse != null && now - lastUse < cooldownSeconds * 1000) {
            player.sendMessage(org.bukkit.ChatColor.RED + "You must wait " +
                    TimeUnit.MILLISECONDS.toSeconds(cooldownSeconds * 1000 - (now - lastUse)) +
                    " seconds before using this command again.");
            return false;
        }

        playerCooldowns.put(command, now);
        return true;
    }
}