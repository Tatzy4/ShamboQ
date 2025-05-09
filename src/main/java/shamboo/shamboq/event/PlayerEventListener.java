package shamboo.shamboq.event;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.util.LogLevel;

import java.util.UUID;

/**
 * Class handling player events with optimized event handling
 */
public class PlayerEventListener implements Listener {
    private final ShamboQ plugin;

    public PlayerEventListener(ShamboQ plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // If player has bypass permission, skip
        if (player.hasPermission("shamboq.bypass")) {
            plugin.logMessage(player.getName() + " bypassed queue (shamboq.bypass permission)", LogLevel.FINE);
            return;
        }

        // Track player joins
        plugin.getMetricsCollector().incrementCounter("player_joins");

        // FIX: Don't start queue process if queue is disabled
        if (!plugin.getConfigManager().isQueueEnabled()) {
            if (plugin.getConfigManager().isShowQueueDisabledMessage()) {
                player.sendTitle(
                        plugin.getMessageManager().getMessage("queue_disabled_title"),
                        ChatColor.YELLOW + plugin.getConfigManager().getQueueDisabledMessage(),
                        10, 40, 20
                );
            }
            // Still freeze player, but without queue process
            plugin.getQueueManager().freezePlayerWithoutQueue(player);
        } else {
            // Add player to queue only when queue is enabled
            plugin.getQueueManager().addToQueue(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Track player quits
        plugin.getMetricsCollector().incrementCounter("player_quits");

        // Cancel any ongoing connection attempts
        if (plugin.getConnectionHandler().hasOngoingConnectionAttempt(playerId)) {
            plugin.getConnectionHandler().cancelConnectionAttempt(playerId);
            plugin.logMessage(player.getName() + " left during connection attempt", LogLevel.FINE);
        }

        // Remove player from queue
        if (plugin.getQueueManager().isPlayerFrozen(player)) {
            plugin.getQueueManager().removeFromQueue(player);
            plugin.logMessage(player.getName() + " left the server and was removed from queue", LogLevel.FINE);
        }
    }

    // Block movement for frozen players
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getQueueManager().isPlayerFrozen(player) ||
                plugin.getConnectionHandler().hasOngoingConnectionAttempt(playerId)) {
            // Allow player to look around, but block movement
            Location from = event.getFrom();
            Location to = event.getTo();

            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                // Player tries to move - cancel movement
                event.setCancelled(true);

                // Track blocked moves
                plugin.getMetricsCollector().incrementCounter("blocked_moves");
            }
        }
    }

    // Block teleportation for frozen players
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getQueueManager().isPlayerFrozen(player) ||
                plugin.getConnectionHandler().hasOngoingConnectionAttempt(playerId)) {
            // Check if this is teleportation initiated by this plugin
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
                plugin.getMetricsCollector().incrementCounter("blocked_teleports");
            }
        }
    }

    // Block interaction with blocks
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getQueueManager().isPlayerFrozen(player) ||
                plugin.getConnectionHandler().hasOngoingConnectionAttempt(playerId)) {
            event.setCancelled(true);
            plugin.getMetricsCollector().incrementCounter("blocked_interactions");
        }
    }

    // Block breaking blocks
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getQueueManager().isPlayerFrozen(player) ||
                plugin.getConnectionHandler().hasOngoingConnectionAttempt(playerId)) {
            event.setCancelled(true);
            plugin.getMetricsCollector().incrementCounter("blocked_breaks");
        }
    }

    // Block placing blocks
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getQueueManager().isPlayerFrozen(player) ||
                plugin.getConnectionHandler().hasOngoingConnectionAttempt(playerId)) {
            event.setCancelled(true);
            plugin.getMetricsCollector().incrementCounter("blocked_places");
        }
    }

    // Block player commands during connection attempts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Block commands during connection attempts (except for admins)
        if (plugin.getConnectionHandler().hasOngoingConnectionAttempt(playerId) &&
                !player.hasPermission("shamboq.bypass")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Commands are blocked during server connection attempts.");
            plugin.getMetricsCollector().incrementCounter("blocked_commands");
        }
    }
}