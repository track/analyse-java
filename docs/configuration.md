# Configuration

This guide covers all configuration options for each platform.

## Spigot / Paper configuration

Location: `plugins/Analyse/config.yml` (see `plugin.yml` in the Spigot/Paper module)

```yaml
# Your Analyse API key
# Get this from your dashboard at https://analyse.net
api-key: "your-api-key-here"

# Enable debug mode for verbose logging
# Useful for troubleshooting API communication
debug: false

# Unique identifier for this server instance
# Auto-generated on first run, usually leave as-is
instance-id: "auto-generated-uuid"
```

### Configuration options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `api-key` | String | `""` | Your Analyse API key (required) |
| `debug` | Boolean | `false` | Enable verbose debug logging |
| `instance-id` | String | Auto | Unique identifier for this server |

## BungeeCord configuration

Location: `plugins/Analyse/config.yml`

```yaml
# Enable debug mode
debug: false

# Unique identifier for this proxy instance
instance-id: "auto-generated-uuid"

# Default server for API calls (optional)
# Used for the static API when no specific server context
default-server: "lobby"

# Per-server API keys
# Each backend server can have its own API key
servers:
  lobby:
    api-key: "lobby-api-key"
  survival:
    api-key: "survival-api-key"
  creative:
    api-key: "creative-api-key"
```

### BungeeCord-specific options

| Option | Type | Description |
|--------|------|-------------|
| `default-server` | String | Server to use for static API calls |
| `servers` | Map | Per-server API key configuration |

## Velocity configuration

Location: `plugins/analyse/config.yml` (Velocity plugin id is `analyse`)

```yaml
# Enable debug mode
debug: false

# Unique identifier for this proxy instance
instance-id: "auto-generated-uuid"

# Default server for API calls (optional)
default-server: "lobby"

# Per-server API keys
servers:
  lobby:
    api-key: "lobby-api-key"
  survival:
    api-key: "survival-api-key"
```

## Proxy server mapping

When using BungeeCord or Velocity, the plugin tracks which backend server each player is connected to. Configure an API key for each server you want to track:

```yaml
servers:
  # Server name must match the name in your proxy config
  lobby:
    api-key: "key-for-lobby-server"
  
  skyblock:
    api-key: "key-for-skyblock-server"
  
  factions:
    api-key: "key-for-factions-server"
```

If a player connects to a server without an API key configured, their session won't be tracked for that server.

## Environment variables

You can use environment variables for sensitive configuration:

```yaml
api-key: ${ANALYSE_API_KEY}
```

## Reloading configuration

After making changes, reload the configuration:

```
/analyse reload
```

Note: Config reload is available on **Spigot/Paper** only. On proxies, some changes (like adding new server entries) may require a full restart.
