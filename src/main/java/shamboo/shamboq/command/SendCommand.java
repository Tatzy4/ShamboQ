package shamboo.shamboq.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import shamboo.shamboq.ShamboQ;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to send a player to SMP server
 */
public class SendCommand implements Command {
    private final ShamboQ plugin;

    public SendCommand(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().getMessage("usage_send"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player_not_found", args[0]));
            return true;
        }

        // Remove from frozen if in queue
        if (plugin.getQueueManager().isPlayerFrozen(target)) {
            plugin.getQueueManager().removeFromQueue(target);
        }

        // Send to SMP using ConnectionHandler for error handling
        plugin.getConnectionHandler().sendToServer(target, plugin.getConfigManager().getSmpServer());

        sender.sendMessage(plugin.getMessageManager().getMessage("sending_player",
                target.getName(), plugin.getConfigManager().getSmpServer()));

        // Track manual sends
        plugin.getMetricsCollector().incrementCounter("manual_sends");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}