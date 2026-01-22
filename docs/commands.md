# Commands

The ServerStats plugin provides commands to manage the plugin and send test events.

## Base Command

```
/serverstats
```

**Aliases**: `/analytics`, `/anl`

**Permission**: `serverstats.command`

## Subcommands

### Status

Shows the current plugin status and connection information.

```
/serverstats status
```

**Permission**: `serverstats.command`

**Output**:
```
──────────────────────────────
  ServerStats v0.1.0
──────────────────────────────
  Status: ● Connected
  API: api.serverstats.net
  Players Tracked: 5
  Debug: Disabled
──────────────────────────────
```

On proxy servers (BungeeCord/Velocity), additional info is shown:
```
  Servers Configured: 3
```

### Reload (Paper only)

Reloads the plugin configuration from disk.

```
/serverstats reload
```

**Permission**: `serverstats.command.reload`

### Debug

Toggles debug mode on/off. Debug mode provides verbose logging for troubleshooting.

```
/serverstats debug
```

**Permission**: `serverstats.command.debug`

### Event

Sends a custom event to the ServerStats API. Useful for testing.

```
/serverstats event <name> [options]
```

**Permission**: `serverstats.command.event`

#### Options

| Option | Description | Example |
|--------|-------------|---------|
| `--player <name>` | Associate event with an online player | `--player Steve` |
| `--value <number>` | Set a numeric value for the event | `--value 100.50` |
| `--data <key=value>` | Add custom data (can be repeated) | `--data item=diamond` |

#### Examples

```bash
# Simple test event
/serverstats event test_event

# Event with player
/serverstats event player_action --player Steve

# Event with value
/serverstats event purchase --value 500.00

# Event with custom data
/serverstats event shop_purchase --player Steve --value 100 --data item=diamond_sword --data quantity=1

# Complex event
/serverstats event quest_completed --player Steve --data quest_id=dragon_slayer --data difficulty=hard --value 1000
```

### Help

Shows the help menu with all available commands.

```
/serverstats help
```

**Permission**: `serverstats.command`

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `serverstats.command` | Base permission for all commands | OP |
| `serverstats.command.reload` | Permission to reload configuration (Paper only) | OP |
| `serverstats.command.debug` | Permission to toggle debug mode | OP |
| `serverstats.command.event` | Permission to send custom events | OP |

## Tab Completion

The plugin provides intelligent tab completion powered by ACF (Annotation Command Framework) for:
- Subcommand names
- Player names (for `--player` option)
- Example event names
- Option flags

## Console Usage

All commands work from the console without the leading slash:

```
serverstats status
serverstats event server_restart --data reason=update
```
