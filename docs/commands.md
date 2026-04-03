# Commands

The Analyse plugin provides commands to manage the plugin and send test events.

## Base Command

```
/analyse
```

**Aliases**: `/ss` (where registered)

**Permission**: `analyse.netmand.status` (default command / status view)

## Subcommands

### Status

Shows the current plugin status and connection information.

```
/analyse status
```

**Permission**: `analyse.netmand.status`

**Output**:

```
──────────────────────────────
  Analyse v1.0.0
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

**Permission**: `analyse.netmand.reload`

### Debug

Toggles debug mode on/off. Debug mode provides verbose logging for troubleshooting.

```
/analyse debug
```

**Permission**: `analyse.netmand.debug`

### Event

Sends a custom event to the Analyse API. Useful for testing.

```
/analyse event <name> [options]
```

**Permission**: `analyse.netmand.event`

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

**Permission**: `analyse.netmand.help`

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `analyse.netmand.status` | View status and default `/analyse` output | OP |
| `analyse.netmand.reload` | Reload configuration (Spigot/Paper only) | OP |
| `analyse.netmand.debug` | Toggle debug mode | OP |
| `analyse.netmand.event` | Send custom events | OP |
| `analyse.netmand.help` | Show help | OP |

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
