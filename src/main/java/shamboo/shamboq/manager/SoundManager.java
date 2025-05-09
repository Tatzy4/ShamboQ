package shamboo.shamboq.manager;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import shamboo.shamboq.ShamboQ;
import shamboo.shamboq.util.LogLevel;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Class managing sounds
 */
public class SoundManager {
    private final ShamboQ plugin;
    private final Random random = new Random();
    private Sound[] randomSounds;

    public SoundManager(ShamboQ plugin) {
        this.plugin = plugin;
        loadSounds();
    }

    private void loadSounds() {
        FileConfiguration config = plugin.getConfig();

        // Default sounds
        config.addDefault("sounds", Arrays.asList(
                "ENTITY_ENDERMAN_DEATH",
                "ENTITY_ENDER_DRAGON_AMBIENT",
                "ENTITY_ENDER_DRAGON_DEATH",
                "BLOCK_END_PORTAL_FRAME_FILL",
                "BLOCK_END_PORTAL_SPAWN",
                "BLOCK_END_GATEWAY_SPAWN",
                "ENTITY_LIGHTNING_BOLT_THUNDER",
                "ENTITY_LIGHTNING_BOLT_IMPACT",
                "ENTITY_GENERIC_EXPLODE"
        ));

        config.options().copyDefaults(true);
        plugin.saveConfig();

        List<String> soundNames = config.getStringList("sounds");
        randomSounds = new Sound[soundNames.size()];

        for (int i = 0; i < soundNames.size(); i++) {
            try {
                randomSounds[i] = Sound.valueOf(soundNames.get(i));
            } catch (IllegalArgumentException e) {
                plugin.logMessage("Invalid sound name in configuration: " + soundNames.get(i), LogLevel.WARNING);
                // Use default sound for invalid name
                randomSounds[i] = Sound.ENTITY_ENDERMAN_DEATH;
            }
        }

        plugin.logMessage("Loaded " + randomSounds.length + " sounds", LogLevel.FINE);
    }

    public Sound getRandomSound() {
        if (randomSounds.length == 0) {
            return Sound.ENTITY_ENDERMAN_DEATH; // Default sound
        }
        return randomSounds[random.nextInt(randomSounds.length)];
    }

    public void playRandomSound(Player player) {
        Sound randomSound = getRandomSound();
        float volume = 0.5f + (random.nextFloat() * 0.5f); // Random volume 0.5-1.0
        float pitch = 0.8f + (random.nextFloat() * 0.4f);  // Random pitch 0.8-1.2
        player.playSound(player.getLocation(), randomSound, volume, pitch);

        // Track sounds played
        plugin.getMetricsCollector().incrementCounter("sounds_played");
    }

    public void reload() {
        loadSounds();
    }
}