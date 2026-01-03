# Analyse Server Plugins

Analytics tracking plugins for Minecraft servers. Supports Paper, Velocity, and BungeeCord.

## Modules

| Module | Description |
|--------|-------------|
| `sdk` | Core SDK with API client and data models |
| `paper` | Plugin for Paper/Spigot/Bukkit servers |
| `velocity` | Plugin for Velocity proxies (multi-server) |
| `bungeecord` | Plugin for BungeeCord proxies (multi-server) |

## Building

```bash
./gradlew clean build
```

Output JARs will be in:
- `paper/build/libs/analyse-paper-*.jar`
- `velocity/build/libs/analyse-velocity-*.jar`
- `bungeecord/build/libs/analyse-bungeecord-*.jar`

## Configuration

### Paper

Location: `plugins/Analyse/config.yml`

```yaml
# Your server's API key from the Analyse dashboard
api-key: "anl_your_api_key_here"

# Bedrock player username prefix (used by Floodgate/Geyser)
# Common values: "." or "_" or "*"
# Leave empty to disable bedrock detection
bedrock-prefix: "."
```

### Velocity / BungeeCord

Location: `plugins/analyse/config.json`

```json
{
  "bedrockPrefix": ".",
  "servers": {
    "lobby": {
      "apiKey": "anl_your_lobby_api_key"
    },
    "survival": {
      "apiKey": "anl_your_survival_api_key"
    }
  }
}
```

Each backend server can have its own API key for separate analytics tracking.

## Features

- **Player Join/Leave Tracking**: Sends events when players join and leave
- **Heartbeat**: Reports online players every 30 seconds
- **Bedrock Detection**: Identifies Bedrock players by username prefix
- **Multi-Server Support**: Velocity/BungeeCord track players across backend servers
- **Hostname Tracking**: Records which domain players used to connect

## API Endpoints

The plugins communicate with the following endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/plugin/join` | POST | Player joined the server |
| `/api/plugin/leave` | POST | Player left the server |
| `/api/plugin/heartbeat` | POST | Periodic player count update |

## Requirements

- Java 21+
- Paper 1.21.4+ / Velocity 3.4.0+ / BungeeCord 1.21+

## License

Proprietary - All rights reserved.

