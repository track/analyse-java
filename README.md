# Analyse Server Plugins

Analytics tracking plugins for Minecraft servers. Supports Spigot 1.8+, Paper, Velocity, and BungeeCord.

## Features

- **Automatic Session Tracking** - Player joins, leaves, and playtime
- **Heartbeat System** - Regular server health checks
- **Custom Events API** - Fluent API for other plugins to track analytics
- **Multi-Platform** - Spigot 1.8+, Paper, BungeeCord, and Velocity support
- **ACF Commands** - Clean command system using [Annotation Command Framework](https://github.com/aikar/commands)

## Documentation

📖 **[Full Documentation](docs/README.md)**

- [Installation Guide](docs/installation.md)
- [Configuration](docs/configuration.md)
- [Commands](docs/commands.md)
- [Developer API](docs/api.md)

## Quick Start

### 1. Download

Download the appropriate plugin for your server:

| Platform | Download |
|----------|----------|
| Spigot/Paper | `analyse-spigot-x.x.x.jar` |
| BungeeCord | `analyse-bungeecord-x.x.x.jar` |
| Velocity | `analyse-velocity-x.x.x.jar` |
| Hytale | `analyse-hytale-x.x.x.jar` |

### 2. Configure

Add your API key from [analyse.net](https://analyse.net):

```yaml
# Spigot/Paper - plugins/Analyse/config.yml
api-key: "your-api-key-here"
```

### 3. Verify

Run `/analyse status` to check the connection.

## For Developers

Track custom events from your plugin:

```java
import net.analyse.api.Analyse;

// Simple event
Analyse.trackEvent("shop_purchase")
    .withPlayer(player.getUniqueId(), player.getName())
    .withData("item", "diamond_sword")
    .withValue(500.0)
    .send();
```

See the [Developer API Documentation](docs/api.md) for more examples.

## Modules

| Module | Description |
|--------|-------------|
| `sdk` | Core SDK with HTTP client and data models |
| `api` | Public API for other plugins to track events |
| `spigot` | Plugin for Spigot 1.8+ and Paper servers |
| `velocity` | Plugin for Velocity proxies |
| `bungeecord` | Plugin for BungeeCord proxies |
| `hytale` | Plugin for Hytale servers |

## Building

```bash
./gradlew clean build
```

Output JARs:

- `modules/spigot/build/libs/analyse-spigot-*.jar`
- `modules/velocity/build/libs/analyse-velocity-*.jar`
- `modules/bungeecord/build/libs/analyse-bungeecord-*.jar`
- `modules/hytale/build/libs/analyse-hytale-*.jar`

## Requirements

- **Java**: 21+
- **Spigot/Paper**: Spigot 1.8+ or Paper (see module for tested API level)
- **Velocity**: 3.4.0+
- **BungeeCord**: 1.21+

## Commands

```
/analyse              - Show plugin status
/analyse status       - Show plugin status  
/analyse reload       - Reload configuration (Spigot/Paper only)
/analyse debug        - Toggle debug mode
/analyse event <name> - Send a custom event
/analyse help         - Show help
```

See [Commands Documentation](docs/commands.md) for details.

## Support

- Website: [analyse.net](https://analyse.net)
- API: [api.analyse.net](https://api.analyse.net)

## License

Proprietary - All rights reserved.
