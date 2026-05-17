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

# Controls how Analyse sends joins, leaves, and custom events to the API.
# SINGLE sends each item immediately using the existing individual endpoints.
# BATCH queues items in memory and sends them together using /v1/plugin/batch.
# BATCH reduces request volume during spikes, but queued items can be lost if the server is killed before flush.
send-mode: "SINGLE"

# Batch sending settings. Only used when send-mode is BATCH.
batch:
  # Number of queued items that triggers an immediate flush.
  # Recommended: 100-250. Maximum API batch size is 1000.
  size: 200

  # How often queued items are flushed, in seconds.
  # Recommended: 2-5 seconds.
  flush-interval-seconds: 3

  # Maximum queued items kept in memory before new items are rejected.
  max-queue-size: 10000

  # Number of retry attempts for temporary HTTP/network failures.
  max-retries: 3

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
| `send-mode` | enum | `SINGLE` | `SINGLE` sends joins, leaves, and custom events immediately. `BATCH` queues them in memory and sends them through `/v1/plugin/batch`. |
| `batch.size` | integer | `200` | Number of queued items that triggers an immediate batch flush. Recommended: `100-250`. Maximum sent per API request is `1000`. |
| `batch.flush-interval-seconds` | integer | `3` | How often queued batch items are flushed. Recommended: `2-5` seconds. |
| `batch.max-queue-size` | integer | `10000` | Maximum queued items kept in memory before new batch items are rejected. |
| `batch.max-retries` | integer | `3` | Retry attempts for temporary batch HTTP/network failures. |
| `events.chat` | boolean | `true` | Track player chat as a session-level event. |
| `events.command` | boolean | `true` | Track command usage. |
| `events.block-place` | boolean | `false` | Track every block place. High volume. |
| `events.block-break` | boolean | `false` | Track every block break. High volume. |
| `events.death` | boolean | `false` | Track every player death. |
| `events.kill-entity` | boolean | `false` | Track every entity kill. High volume. |

> [!TIP]
> Only enable the high-volume events (`block-place`, `block-break`, `kill-entity`) if you have a specific use case in mind. They can generate hundreds of events per player per minute.

> [!NOTE]
> `BATCH` reduces request volume during spikes, but the queue is in memory only. Queued analytics can be lost if the server process is killed before the next flush or shutdown flush completes.

## BungeeCord

**File:** `plugins/Analyse/config.json`

```json
{
  "debug": false,
  "development": false,
  "bedrockPrefix": ".",
  "instanceId": "default",
  "sendMode": "SINGLE",
  "batch": {
    "size": 200,
    "flushIntervalSeconds": 3,
    "maxQueueSize": 10000,
    "maxRetries": 3
  },
  "defaultServer": "lobby",
  "servers": {
    "lobby": { "apiKey": "anl_your_lobby_key_here" },
    "survival": { "apiKey": "anl_your_survival_key_here" }
  },
  "events": {
    "command": true,
    "chat": true,
    "serverSwitch": true
  }
}
```

### Options

| Option | Type | What it does |
| --- | --- | --- |
| `debug` | boolean | Verbose logging. |
| `instanceId` | string | Distinguishes multiple proxy instances. |
| `bedrockPrefix` | string | Floodgate/Geyser prefix (same as on Spigot). |
| `defaultServer` | string | Backend name used when the plugin cannot determine the current backend (e.g. for API calls from commands). |
| `sendMode` | enum | `SINGLE` preserves current per-item requests. `BATCH` queues joins, leaves, and custom events for `/v1/plugin/batch`. |
| `batch.size` | integer | Defaults to `200`. Recommended: `100-250`. |
| `batch.flushIntervalSeconds` | integer | Defaults to `3`. Recommended: `2-5`. |
| `batch.maxQueueSize` | integer | Defaults to `10000`. |
| `batch.maxRetries` | integer | Defaults to `3`. |
| `servers.<name>.apiKey` | string | API key for a backend called `<name>`. The name must match your BungeeCord server config. |

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
  "sendMode": "SINGLE",
  "batch": {
    "size": 200,
    "flushIntervalSeconds": 3,
    "maxQueueSize": 10000,
    "maxRetries": 3
  },
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
| `sendMode` | enum | `SINGLE` preserves current per-item requests. `BATCH` queues joins, leaves, and custom events for `/v1/plugin/batch`. |
| `batch.size` | integer | Defaults to `200`. Recommended: `100-250`. |
| `batch.flushIntervalSeconds` | integer | Defaults to `3`. Recommended: `2-5`. |
| `batch.maxQueueSize` | integer | Defaults to `10000`. |
| `batch.maxRetries` | integer | Defaults to `3`. |
| `defaultServer` | string \| null | Backend used for API calls without context. |
| `servers.<name>.apiKey` | string | API key for the backend named `<name>`. |
| `events.command` | boolean | Track command usage across the proxy. |
| `events.serverSwitch` | boolean | Track when a player moves between backends. |

## Hytale

**File:** Analyse data folder &rarr; `config.json`

```json
{
  "debug": false,
  "development": false,
  "apiKey": "",
  "instanceId": "default",
  "sendMode": "SINGLE",
  "batch": {
    "size": 200,
    "flushIntervalSeconds": 3,
    "maxQueueSize": 10000,
    "maxRetries": 3
  }
}
```

### Options

| Option | Type | What it does |
| --- | --- | --- |
| `apiKey` | string | Your Analyse API key. Required. |
| `debug` | boolean | Verbose logging. |
| `instanceId` | string | Distinguishes multiple instances of the same Server. |
| `sendMode` | enum | `SINGLE` preserves current per-item requests. `BATCH` queues joins, leaves, and custom events for `/v1/plugin/batch`. |
| `batch.size` | integer | Defaults to `200`. Recommended: `100-250`. |
| `batch.flushIntervalSeconds` | integer | Defaults to `3`. Recommended: `2-5`. |
| `batch.maxQueueSize` | integer | Defaults to `10000`. |
| `batch.maxRetries` | integer | Defaults to `3`. |

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
