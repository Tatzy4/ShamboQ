# ShamboQ

[![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/username/ShamboQ/releases)
[![Spigot](https://img.shields.io/badge/spigot-1.16--1.21-orange.svg)](https://www.spigotmc.org/resources/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

ShamboQ is an advanced server queue system for Minecraft servers (Bukkit/Spigot/Paper) with extensive optimization features. It helps manage player flow between your lobby server and main gameplay server in a BungeeCord/Velocity proxy network.

## Features

- 🚀 **Optimized** queue system to reduce server resource usage
- 🔄 **BungeeCord/Velocity** integration for seamless server transitions
- ⏱️ **Customizable queue timers** with countdown and notifications
- 🎵 **Sound effects** for immersive queue experience
- 🛡️ **Connection error handling** with automatic retry system
- 🔧 **Extended optimization** options for high-load scenarios
- 📊 **Metrics collection** for performance monitoring
- 🎮 **Player visibility management** for improved experience

## Installation

1. Download the latest version of ShamboQ from [GitHub releases](https://github.com/Tatzy4/ShamboQ/releases/tag/Release)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin by editing the `config.yaml` file in the `plugins/ShamboQ` directory

## Configuration

ShamboQ's configuration is stored in `config.yaml`. Here's a breakdown of all configuration options:

### Basic Queue Configuration

```yaml
queue:
  # Enable or disable the queue system
  enabled: true
  
  # Time in seconds that players spend in the queue before transfer
  time: 10
  
  # The name of the target server in your BungeeCord/Velocity configuration
  smp-server: "smp"
  
  # Message to display when the queue is disabled
  disabled-message: "Queue is currently disabled"
  
  # Whether to show queue disabled notification to players
  show-disabled-message: true
  
  # Interval in seconds between queue disabled notifications
  notification-interval: 5

# Spawn location in the queue world (The End dimension)
spawn:
  x: -1.5
  y: 64
  z: 0.5
  
# List of sound effects to play during queue
sounds:
  - "ENTITY_ENDERMAN_DEATH"
  - "ENTITY_ENDER_DRAGON_AMBIENT"
  - "ENTITY_ENDER_DRAGON_DEATH"
  - "BLOCK_END_PORTAL_FRAME_FILL"
  - "BLOCK_END_PORTAL_SPAWN"
  - "BLOCK_END_GATEWAY_SPAWN"
  - "ENTITY_LIGHTNING_BOLT_THUNDER"
  - "ENTITY_LIGHTNING_BOLT_IMPACT"
  - "ENTITY_GENERIC_EXPLODE"
```

### Optimization Settings

```yaml
optimization:
  # Manages chunk loading/unloading to reduce memory usage
  chunk-management: true
  
  # Disables mob spawning and AI in the queue area
  disable-mobs: true
  
  # Reduces view distance for players in queue
  reduce-view-distance: true
  
  # Sets up a no-tick zone around the queue area (Paper servers only)
  no-tick-zone: true
  
  # The view distance for players in queue (lower = better performance)
  queue-view-distance: 2
  
  # Disables terrain generation completely (Paper servers only)
  disable-terrain-generation: true
  
  # Disables player tick processing while in queue
  disable-player-ticks: true
  
  # Uses spectator mode for players in queue to reduce server load
  spectator-mode: true
  
  # Uses a dedicated thread pool for queue processing
  dedicated-thread-pool: true
  
  # Size of the dedicated thread pool
  thread-pool-size: 1
  
  # Aggressively manages chunks to minimize loaded chunks
  aggressive-chunk-management: true
  
  # Maximum number of chunks to keep loaded around the queue area
  max-loaded-chunks: 9
```

### Connection Settings

```yaml
connection:
  # Maximum number of retry attempts for server connections
  max-retries: 3
  
  # Delay in seconds between retry attempts
  retry-delay: 5
```

### Messages

```yaml
messages:
  queue_enabled: "&aQueue has been enabled!"
  queue_disabled: "&cQueue has been disabled!"
  # ... and many more (see default config)
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/shamboq` | Shows help information | `shamboq.admin` |
| `/shamboq toggle` | Enables or disables the queue | `shamboq.admin` |
| `/shamboq settime <seconds>` | Sets the queue time | `shamboq.admin` |
| `/shamboq message <text>` | Sets the queue disabled message | `shamboq.admin` |
| `/shamboq notify` | Toggles queue disabled notifications | `shamboq.admin` |
| `/shamboq send <player>` | Sends a player to the SMP server | `shamboq.admin` |
| `/shamboq reload` | Reloads the configuration | `shamboq.admin` |
| `/shamboq status` | Shows the current status | `shamboq.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `shamboq.admin` | Allows access to all ShamboQ commands | `op` |
| `shamboq.bypass` | Allows players to bypass the queue | `op` |

## Platform Support

ShamboQ is optimized for different server platforms with automatic detection:

- **Paper** - Full support with all optimization features
- **Spigot** - Good support with most features except Paper-specific optimizations
- **Bukkit** - Basic support with fallback methods for compatibility

## Development

### Building from Source

ShamboQ uses Gradle as its build system. To build from source:

1. Clone the repository:
   ```
   git clone https://github.com/username/ShamboQ.git
   ```

2. Navigate to the project directory:
   ```
   cd ShamboQ
   ```

3. Build with Gradle:
   ```
   ./gradlew build
   ```

4. Find the built JAR in the `build/libs` directory

### Project Structure

The project is organized as follows:

- `shamboo.shamboq` - Main package
  - `command` - Command implementations
  - `config` - Configuration handling
  - `connection` - BungeeCord/Velocity connection logic
  - `event` - Event listeners
  - `manager` - Manager classes for different subsystems
  - `platform` - Platform-specific adapters
  - `util` - Utility classes

## Performance Tuning

For best performance, consider these tips:

1. **Use Paper server** for the queue to enable all optimization features
2. **Lower the `queue-view-distance`** to 1-2 for minimal resource usage
3. **Enable `spectator-mode`** to drastically reduce player processing
4. **Use The End dimension** for the queue world (already the default)
5. **Keep `max-loaded-chunks`** as low as possible (9 is the default)
6. **Enable `aggressive-chunk-management`** for high-traffic servers

## Troubleshooting

### Common Issues

1. **Players getting stuck in queue**:
   - Check BungeeCord/Velocity configuration
   - Ensure `smp-server` matches your BungeeCord server name
   - Verify server is accessible from the proxy

2. **High CPU usage**:
   - Enable all optimization options
   - Reduce `max-loaded-chunks` value
   - Set `queue-view-distance` to 1

3. **Commands not working**:
   - Check permissions
   - Verify you're using the correct command syntax

4. **Connection failures**:
   - Check network connectivity between servers
   - Verify BungeeCord/Velocity plugin messaging channels are enabled

## License

ShamboQ is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact & Support

- **Website**: [https://szam.boo/](https://szam.boo/)
- **Issues**: [GitHub Issues](https://github.com/username/ShamboQ/issues)
- **Discord**: [Join our Discord](https://discord.gg/example)

---
Sponsored by Shambonoor Ent.
