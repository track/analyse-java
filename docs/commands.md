# Commands

The Analyse plugin provides commands to manage the plugin and send test events.

## Base Command

```
/analyse
```

**Aliases**: `/ss` (where registered)

**Permission**: `analyse.command.status` (default command / status view)

## Subcommands

### Status

Shows the current plugin status and connection information.

```
/analyse status
```

**Permission**: `analyse.command.status`

**Output**:

```
──────────────────────────────
  Analyse v0.7.3
──────────────────────────────
  Status: ● Connected
  API: api.analyse.net
  Players Tracked: 5
  Debug: Disabled
──────────────────────────────
```

On proxy servers (BungeeCord/Velocity), additional info is shown:

```
  Servers Configured: 3
```

### Reload (Spigot/Paper only)

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

**Permission**: `analyse.command.help`

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `analyse.command.status` | View status and default `/analyse` output | OP |
| `analyse.command.reload` | Reload configuration (Spigot/Paper only) | OP |
| `analyse.command.debug` | Toggle debug mode | OP |
| `analyse.command.event` | Send custom events | OP |
| `analyse.command.help` | Show help | OP |

## Tab Completion

The plugin provides intelligent tab completion powered by ACF (Annotation Command Framework) for:

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
