# ServerStats Server Plugins

Analytics tracking plugins for Minecraft servers. Supports Paper, Velocity, and BungeeCord.

## Features

- **Automatic Session Tracking** - Player joins, leaves, and playtime
- **Heartbeat System** - Regular server health checks
- **Custom Events API** - Fluent API for other plugins to track analytics
- **Multi-Platform** - Paper, BungeeCord, and Velocity support
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
| Paper/Spigot | `serverstats-paper-x.x.x.jar` |
| BungeeCord | `serverstats-bungeecord-x.x.x.jar` |
| Velocity | `serverstats-velocity-x.x.x.jar` |

### 2. Configure

Add your API key from [serverstats.net](https://serverstats.net):

```yaml
# Paper - plugins/ServerStats/config.yml
api-key: "your-api-key-here"
```

### 3. Verify

Run `/serverstats status` to check the connection.

## For Developers

Track custom events from your plugin:

```java
import net.serverstats.api.ServerStats;

// Simple event
ServerStats.trackEvent("shop_purchase")
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
| `paper` | Plugin for Paper/Spigot servers |
| `velocity` | Plugin for Velocity proxies |
| `bungeecord` | Plugin for BungeeCord proxies |

## Building

```bash
./gradlew clean build
```

Output JARs:
- `paper/build/libs/serverstats-paper-*.jar`
- `velocity/build/libs/serverstats-velocity-*.jar`
- `bungeecord/build/libs/serverstats-bungeecord-*.jar`

## Requirements

- **Java**: 21+
- **Paper**: 1.21.4+
- **Velocity**: 3.4.0+
- **BungeeCord**: 1.21+

## Commands

```
/serverstats              - Show plugin status
/serverstats status       - Show plugin status  
/serverstats reload       - Reload configuration
/serverstats debug        - Toggle debug mode
/serverstats event <name> - Send a custom event
/serverstats help         - Show help
```

See [Commands Documentation](docs/commands.md) for details.

## Support

- Website: [serverstats.net](https://serverstats.net)
- API: [api.serverstats.net](https://api.serverstats.net)

## License

Proprietary - All rights reserved.
