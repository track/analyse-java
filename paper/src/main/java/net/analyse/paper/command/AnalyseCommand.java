package net.analyse.paper.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.analyse.api.Analyse;
import net.analyse.api.object.builder.EventBuilder;
import net.analyse.paper.AnalysePlugin;
import net.analyse.paper.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

/**
 * Main command handler for the Analyse plugin using ACF
 */
@CommandAlias("analyse|analytics|anl")
@CommandPermission("analyse.command")
public class AnalyseCommand extends BaseCommand {

  private final AnalysePlugin plugin;

  public AnalyseCommand(AnalysePlugin plugin) {
    this.plugin = plugin;
  }

  @Default
  @Subcommand("status")
  @Description("Show plugin status")
  public void onStatus(CommandSender sender) {
    boolean connected = Analyse.isAvailable();
    int trackedPlayers = plugin.getSessionManager().getSessionCount();
    boolean debugEnabled = plugin.isDebugEnabled();

    send(sender, "&8&m                              ");
    send(sender, "  &b&lAnalyse &7v" + plugin.getDescription().getVersion());
    send(sender, "&8&m                              ");
    send(sender, "  &7Status: " + (connected ? "&a● Connected" : "&c● Disconnected"));
    send(sender, "  &7API: &fapi.analyse.net");
    send(sender, "  &7Players Tracked: &f" + trackedPlayers);
    send(sender, "  &7Debug: " + (debugEnabled ? "&aEnabled" : "&7Disabled"));
    send(sender, "&8&m                              ");
  }

  @Subcommand("reload")
  @Description("Reload configuration")
  @CommandPermission("analyse.command.reload")
  public void onReload(CommandSender sender) {
    plugin.reloadConfig();
    send(sender, "&aAnalyse configuration reloaded.");
  }

  @Subcommand("debug")
  @Description("Toggle debug mode")
  @CommandPermission("analyse.command.debug")
  public void onDebug(CommandSender sender) {
    boolean newState = !plugin.getPluginConfig().isDebug();
    plugin.getPluginConfig().setDebug(newState);

    if (newState) {
      send(sender, "&aDebug mode enabled.");
    } else {
      send(sender, "&7Debug mode disabled.");
    }
  }

  @Subcommand("event")
  @Description("Send a custom event")
  @CommandPermission("analyse.command.event")
  @Syntax("<name> [--player <player>] [--value <number>] [--data <key=value>...]")
  @CommandCompletion("test_event|custom_event @players")
  public void onEvent(CommandSender sender, String[] args) {
    if (args.length == 0) {
      send(sender, "&cUsage: /analyse event <name> [--player <name>] [--value <number>] [--data key=value...]");
      return;
    }

    if (!Analyse.isAvailable()) {
      send(sender, "&cAnalyse is not connected. Cannot send events.");
      return;
    }

    // Parse arguments
    String eventName = args[0];
    String playerName = null;
    Double value = null;
    Map<String, Object> data = new HashMap<>();

    for (int i = 1; i < args.length; i++) {
      String arg = args[i];

      if (arg.equals("--player") && i + 1 < args.length) {
        playerName = args[++i];
      } else if (arg.equals("--value") && i + 1 < args.length) {
        try {
          value = Double.parseDouble(args[++i]);
        } catch (NumberFormatException e) {
          send(sender, "&cInvalid value. Must be a number.");
          return;
        }
      } else if (arg.equals("--data") && i + 1 < args.length) {
        String dataArg = args[++i];
        int eqIndex = dataArg.indexOf('=');
        if (eqIndex > 0) {
          String key = dataArg.substring(0, eqIndex);
          String val = dataArg.substring(eqIndex + 1);
          // Try to parse as number
          try {
            data.put(key, Double.parseDouble(val));
          } catch (NumberFormatException e) {
            data.put(key, val);
          }
        }
      }
    }

    // Build the event
    EventBuilder builder = Analyse.trackEvent(eventName);

    // Add player if specified
    if (playerName != null) {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        builder.withPlayer(player.getUniqueId(), player.getName());
      } else {
        send(sender, "&cPlayer '" + playerName + "' not found online.");
        return;
      }
    }

    // Add value if specified
    if (value != null) {
      builder.withValue(value);
    }

    // Add data if specified
    if (!data.isEmpty()) {
      builder.withData(data);
    }

    // Send the event
    String finalPlayerName = playerName;
    Double finalValue = value;
    builder.send(success -> {
      if (Boolean.TRUE.equals(success)) {
        Bukkit.getScheduler().runTask(plugin, () -> {
          send(sender, "&a✓ Event '&f" + eventName + "&a' sent successfully");
          if (finalPlayerName != null) {
            send(sender, "  &7Player: &f" + finalPlayerName);
          }
          if (finalValue != null) {
            send(sender, "  &7Value: &f" + finalValue);
          }
          if (!data.isEmpty()) {
            send(sender, "  &7Data: &f" + data);
          }
        });
      } else {
        Bukkit.getScheduler().runTask(plugin, () -> {
          send(sender, "&c✗ Failed to send event '&f" + eventName + "&c'");
        });
      }
    });
  }

  @Subcommand("help")
  @Description("Show help information")
  public void onHelp(CommandSender sender) {
    send(sender, "&8&m                              ");
    send(sender, "  &b&lAnalyse Commands");
    send(sender, "&8&m                              ");
    send(sender, "  &e/analyse &7- Show plugin status");
    send(sender, "  &e/analyse status &7- Show plugin status");
    send(sender, "  &e/analyse reload &7- Reload configuration");
    send(sender, "  &e/analyse debug &7- Toggle debug mode");
    send(sender, "  &e/analyse event <name> &7- Send custom event");
    send(sender, "    &7Options:");
    send(sender, "    &f--player <name> &7- Associate with player");
    send(sender, "    &f--value <number> &7- Set numeric value");
    send(sender, "    &f--data <key=value> &7- Add data (repeatable)");
    send(sender, "  &e/analyse help &7- Show this help");
    send(sender, "&8&m                              ");
  }

  /**
   * Send a colored message to a command sender
   *
   * @param sender  The command sender
   * @param message The message with color codes
   */
  private void send(CommandSender sender, String message) {
    sender.sendMessage(ComponentUtil.parse(message));
  }
}
