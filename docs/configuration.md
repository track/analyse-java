# Configuration

Every platform plugin has its own config file with sensible defaults. You only ever need to set one thing to get going: your API key.

This page is the reference for every option. Jump to the section for your platform.

- [Spigot / Paper](#spigot--paper)
- [BungeeCord](#bungeecord)
- [Velocity](#velocity)
- [Hytale](#hytale)
- [Reloading](#reloading)

## Spigot / Paper

**File:** `plugins/Analyse/config.yml`

```yaml
# Enable debug logging for troubleshooting
debug: false

# Your server's API key from the Analyse dashboard
api-key: "anl_your_api_key_here"

# Bedrock player username prefix (used by Floodgate/Geyser)
# Common values: "." or "_" or "*"
# Leave empty to disable bedrock detection
bedrock-prefix: "."

# Instance ID for multi-instance setups (e.g. "survival-1", "survival-2")
# Used to identify this specific server instance in heartbeats
instance-id: "default"

# Built-in event tracking
# These events are automatically sent when players perform actions.
# High-frequency events are disabled by default.
events:
  chat: true
  command: true
  block-place: false
  block-break: false
  death: false
  kill-entity: false
```

### Options

| Option | Type | Default | What it does |
| --- | --- | --- | --- |
| `api-key` | string | `""` | Your Analyse API key. Required. Starts with `anl_`. |
| `debug` | boolean | `false` | Turns on verbose logging. Useful for troubleshooting; noisy otherwise. |
| `bedrock-prefix` | string | `"."` | Prefix used by Floodgate/Geyser. Players whose names start with it are tagged as Bedrock. Empty string disables detection. |
| `instance-id` | string | `"default"` | Distinguishes multiple instances of the same Analyse Server (e.g. `survival-1`, `survival-2`). |
| `events.chat` | boolean | `true` | Track player chat as a session-level event. |
| `events.command` | boolean | `true` | Track command usage. |
| `events.block-place` | boolean | `false` | Track every block place. High volume. |
| `events.block-break` | boolean | `false` | Track every block break. High volume. |
| `events.death` | boolean | `false` | Track every player death. |
| `events.kill-entity` | boolean | `false` | Track every entity kill. High volume. |

> [!TIP]
> Only enable the high-volume events (`block-place`, `block-break`, `kill-entity`) if you have a specific use case in mind. They can generate hundreds of events per player per minute.

## BungeeCord

**File:** `plugins/Analyse/config.yml`

```yaml
debug: false
instance-id: "default"
bedrock-prefix: "."

# Optional: server name to use when the plugin can't determine context
default-server: "lobby"

# Per-backend API keys
# The backend name must match the name you use in your BungeeCord config
servers:
  lobby:
    api-key: "anl_your_lobby_key_here"
  survival:
    api-key: "anl_your_survival_key_here"
```

### Options

| Option | Type | What it does |
| --- | --- | --- |
| `debug` | boolean | Verbose logging. |
| `instance-id` | string | Distinguishes multiple proxy instances. |
| `bedrock-prefix` | string | Floodgate/Geyser prefix (same as on Spigot). |
| `default-server` | string | Backend name used when the plugin cannot determine the current backend (e.g. for API calls from commands). |
| `servers.<name>.api-key` | string | API key for a backend called `<name>`. The name must match your BungeeCord server config. |

> [!NOTE]
> The BungeeCord plugin is only needed when you run the proxy without Analyse installed on each backend. The recommended setup is the Spigot plugin on every backend &mdash; it gives you more data.

## Velocity

**File:** `plugins/analyse/config.json`

```json
{
  "debug": false,
  "development": false,
  "bedrockPrefix": ".",
  "instanceId": "default",
  "defaultServer": null,
  "servers": {
    "lobby": { "apiKey": "anl_your_lobby_key_here" },
    "survival": { "apiKey": "anl_your_survival_key_here" }
  },
  "events": {
    "command": true,
    "serverSwitch": true
  }
}
```

### Options

| Option | Type | What it does |
| --- | --- | --- |
| `debug` | boolean | Verbose logging. |
| `bedrockPrefix` | string | Floodgate/Geyser prefix. |
| `instanceId` | string | Distinguishes multiple proxy instances. |
| `defaultServer` | string \| null | Backend used for API calls without context. |
| `servers.<name>.apiKey` | string | API key for the backend named `<name>`. |
| `events.command` | boolean | Track command usage across the proxy. |
| `events.serverSwitch` | boolean | Track when a player moves between backends. |

## Hytale

**File:** Analyse data folder &rarr; `config.json`

```json
{
  "debug": false,
  "apiKey": "",
  "instanceId": "default"
}
```

### Options

| Option | Type | What it does |
| --- | --- | --- |
| `apiKey` | string | Your Analyse API key. Required. |
| `debug` | boolean | Verbose logging. |
| `instanceId` | string | Distinguishes multiple instances of the same Server. |

## Environment variables

You can reference environment variables in your config files on platforms that support them. This is useful when your API keys live in a secrets manager rather than on disk.

```yaml
api-key: ${ANALYSE_API_KEY}
```

> [!WARNING]
> Never commit your filled-in config to a public repository. Use an environment variable or a git-ignored config when your repo is public.

## Reloading

| Platform | Command | Notes |
| --- | --- | --- |
| Spigot / Paper | `/analyse reload` | Reloads the config on the fly. |
| BungeeCord | *(restart)* | Config is loaded on proxy start. Adding a new backend requires a full restart. |
| Velocity | *(restart)* | Same as BungeeCord. |
| Hytale | *(restart)* | Same. |

## Related

- [Installation](installation.md)
- [Commands](commands.md)
- [SDK overview](sdk/README.md)
