# Commands

The plugin registers a single top-level command, `/analyse`, with subcommands for status, configuration, custom events, analytics lookups, and addon management.

Every subcommand has its own permission node (e.g. `analyse.status`, `analyse.event`). Grant `analyse.*` on Spigot/Paper to give a role access to all of them, or `analyse.addons.*` for the addon management subset.

## The base command

| Command | Alias | Permission | What it does |
| --- | --- | --- | --- |
| `/analyse` | `/ss` | *(none)* | Shows a short info banner. Same output as `/analyse help`. |

## Subcommands

### `status`

Prints the current plugin status and connection health.

```
/analyse status
```

**Permission:** `analyse.status`

**Example output:**

```
──────────────────────────────
  Analyse v1.0.0
──────────────────────────────
  Status: Connected
  API: api.analyse.net
  Players Tracked: 5
  Debug: Disabled
──────────────────────────────
```

On a proxy you'll also see the number of configured backends.

### `reload` &nbsp;*(Spigot/Paper only)*

Reloads the config from disk without restarting the server.

```
/analyse reload
```

**Permission:** `analyse.reload`

Proxy plugins don't support hot reload &mdash; see the [configuration guide](configuration.md#reloading).

### `debug`

Toggles debug mode. Debug mode logs every outgoing API request and every event payload to the server console. It's noisy; leave it off unless you're troubleshooting.

```
/analyse debug
```

**Permission:** `analyse.debug`

### `event`

Sends a custom event to the Analyse API. Handy for smoke-testing your dashboards and confirming that events make it through.

```
/analyse event <name> [--player <player>] [--value <number>] [--data <key=value>...]
```

**Permission:** `analyse.event`

**Options:**

| Option | What it does | Example |
| --- | --- | --- |
| `--player <name>` | Associate the event with an online player | `--player Steve` |
| `--value <number>` | Set the numeric "main value" of the event | `--value 100.50` |
| `--data <key=value>` | Add a custom property. Can be repeated. | `--data item=diamond` |

**Examples:**

```bash
# Simple event
/analyse event test_event

# Event with a player
/analyse event player_action --player Steve

# Event with a numeric value
/analyse event purchase --value 500.00

# Event with data
/analyse event shop_purchase --player Steve --value 100 --data item=diamond_sword --data quantity=1

# A richer event with several data fields
/analyse event quest_completed --player Steve --data quest_id=dragon_slayer --data difficulty=hard --value 1000
```

### `info`

Looks up analytics for your server or a specific player.

```
/analyse info [player]
```

**Permission:** `analyse.info`

Without arguments, it prints recent server-level stats. With a player name it prints that player's summary.

### `addons`

Lists every loaded Analyse addon.

```
/analyse addons
```

**Permission:** `analyse.addons`

### `addons reload [addon]`

Reloads every loaded addon, or a single one by name.

```
/analyse addons reload
/analyse addons reload MyAddon
```

**Permission:** `analyse.addons.reload`

### `addons enable <addon>`

Enables a disabled addon.

```
/analyse addons enable MyAddon
```

**Permission:** `analyse.addons.enable`

### `addons disable <addon>`

Disables a running addon.

```
/analyse addons disable MyAddon
```

**Permission:** `analyse.addons.disable`

### `help`

Prints the help menu with every subcommand and its description.

```
/analyse help
```

**Permission:** `analyse.help`

## Permissions summary

| Permission | Default | What it grants |
| --- | --- | --- |
| `analyse.status` | OP | `/analyse status` |
| `analyse.reload` | OP | `/analyse reload` *(Spigot/Paper only)* |
| `analyse.debug` | OP | `/analyse debug` |
| `analyse.event` | OP | `/analyse event ...` |
| `analyse.info` | OP | `/analyse info ...` |
| `analyse.addons` | OP | `/analyse addons` |
| `analyse.addons.reload` | OP | `/analyse addons reload ...` |
| `analyse.addons.enable` | OP | `/analyse addons enable ...` |
| `analyse.addons.disable` | OP | `/analyse addons disable ...` |
| `analyse.help` | OP | `/analyse help` |

## Tab completion

All commands ship with ACF-powered tab completion, including:

- Subcommand names
- Player names for `--player`
- Example event names
- Option flags

## Console usage

Every command works from the console exactly like in-game, minus the leading slash:

```
analyse status
analyse event server_restart --data reason=update
```
