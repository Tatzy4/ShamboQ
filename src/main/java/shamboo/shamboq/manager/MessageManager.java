package shamboo.shamboq.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.util.LogLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Class managing messages
 */
public class MessageManager {
    private final ShamboQ plugin;
    private Map<String, String> messages = new HashMap<>();

    public MessageManager(ShamboQ plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        FileConfiguration config = plugin.getConfig();

        // Default messages
        config.addDefault("messages.queue_enabled", "&aQueue has been enabled!");
        config.addDefault("messages.queue_disabled", "&cQueue has been disabled!");
        config.addDefault("messages.no_permission", "&cYou don't have permission to use this command!");
        config.addDefault("messages.queue_time_set", "&aQueue time set to &e%d &aseconds!");
        config.addDefault("messages.invalid_number", "&cInvalid number: &e%s");
        config.addDefault("messages.message_set", "&aQueue disabled message set to: &e%s");
        config.addDefault("messages.notifications_enabled", "&aQueue disabled notifications have been enabled!");
        config.addDefault("messages.notifications_disabled", "&cQueue disabled notifications have been disabled!");
        config.addDefault("messages.player_not_found", "&cPlayer not found: &e%s");
        config.addDefault("messages.sending_player", "&aSending &e%s &ato &e%s&a!");
        config.addDefault("messages.config_reloaded", "&aShamboQ configuration reloaded!");
        config.addDefault("messages.welcome_title", "&6Welcome to SHARTY!");
        config.addDefault("messages.welcome_subtitle", "&eYou will be transferred to the SMP server in &6%d &eseconds");
        config.addDefault("messages.countdown", "&eTransfer in &6%d &eseconds...");
        config.addDefault("messages.queue_disabled_title", "&cQueue Disabled");
        config.addDefault("messages.usage_settime", "&cUsage: /shamboq settime <seconds>");
        config.addDefault("messages.usage_message", "&cUsage: /shamboq message <message>");
        config.addDefault("messages.usage_send", "&cUsage: /shamboq send <player>");
        config.addDefault("messages.queue_disabled_player", "&cQueue system has been disabled. You are no longer in queue.");
        config.addDefault("messages.frozen_player", "&cYou are currently frozen in the lobby.");

        // Add new messages for connection errors
        config.addDefault("messages.connection_failed", "&cConnection to SMP server failed. Retrying...");
        config.addDefault("messages.connection_error", "&cCould not connect to SMP server. &eError: %s");
        config.addDefault("messages.connection_timeout", "&cConnection timed out. Retrying in %d seconds...");
        config.addDefault("messages.max_retries_reached", "&cCould not connect after %d attempts. Please try again later.");
        config.addDefault("messages.back_in_queue", "&eYou've been placed back in the queue due to connection issues.");

        config.options().copyDefaults(true);
        plugin.saveConfig();

        // Load messages
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                String messageText = config.getString("messages." + key, "Missing message: " + key);
                messages.put(key, ChatColor.translateAlternateColorCodes('&', messageText));
            }
        }

        plugin.logMessage("Loaded " + messages.size() + " messages", LogLevel.FINE);
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "Missing message: " + key);
    }

    public String getMessage(String key, Object... args) {
        return String.format(getMessage(key), args);
    }

    public void reload() {
        loadMessages();
    }
}