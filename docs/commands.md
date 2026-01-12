# Commands

The Analyse plugin provides commands to manage the plugin and send test events.

## Base Command

```
/analyse
```

**Aliases**: `/analytics`, `/anl`

**Permission**: `analyse.command`

## Subcommands

### Status

Shows the current plugin status and connection information.

```
/analyse status
```

**Permission**: `analyse.command`

**Output**:
```
▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
  Analyse v1.0.0
▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
  Status: ● Connected
  API: api.analyse.net
  Players Tracked: 5
  Debug: Disabled
▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
```

### Reload

Reloads the plugin configuration from disk.

```
/analyse reload
```

**Permission**: `analyse.command.reload`

### Debug

Toggles debug mode on/off. Debug mode provides verbose logging for troubleshooting.

```
/analyse debug
```

**Permission**: `analyse.command.debug`

### Event

Sends a custom event to the Analyse API. Useful for testing.

```
/analyse event <name> [options]
```

**Permission**: `analyse.command.event`

#### Options

| Option | Description | Example |
|--------|-------------|---------|
| `--player <name>` | Associate event with an online player | `--player Steve` |
| `--value <number>` | Set a numeric value for the event | `--value 100.50` |
| `--data <key=value>` | Add custom data (can be repeated) | `--data item=diamond` |

#### Examples

```bash
# Simple test event
/analyse event test_event

# Event with player
/analyse event player_action --player Steve

# Event with value
/analyse event purchase --value 500.00

# Event with custom data
/analyse event shop_purchase --player Steve --value 100 --data item=diamond_sword --data quantity=1

# Complex event
/analyse event quest_completed --player Steve --data quest_id=dragon_slayer --data difficulty=hard --value 1000
```

### Help

Shows the help menu with all available commands.

```
/analyse help
```

**Permission**: `analyse.command`

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `analyse.command` | Base permission for all commands | OP |
| `analyse.command.reload` | Permission to reload configuration | OP |
| `analyse.command.debug` | Permission to toggle debug mode | OP |
| `analyse.command.event` | Permission to send custom events | OP |

## Tab Completion

The plugin provides intelligent tab completion for:
- Subcommand names
- Player names (for `--player` option)
- Example event names
- Option flags

## Console Usage

All commands work from the console without the leading slash:

```
analyse status
analyse event server_restart --data reason=update
```
