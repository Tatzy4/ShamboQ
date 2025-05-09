package shamboo.shamboq.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Interface for subcommands
 */
public interface Command {
    /**
     * Execute the command
     * @param sender Who sent the command
     * @param args Command arguments
     * @return Whether the command was successful
     */
    boolean execute(CommandSender sender, String[] args);

    /**
     * Tab completion for the command
     * @param sender Who is tab completing
     * @param args Current arguments
     * @return List of possible completions
     */
    List<String> tabComplete(CommandSender sender, String[] args);
}