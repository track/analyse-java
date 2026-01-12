package net.analyse.velocity.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.analyse.api.Analyse;
import net.analyse.api.EventBuilder;
import net.analyse.velocity.AnalyseVelocity;
import net.kyori.adventure.text.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Main command handler for the Analyse plugin using ACF
 */
@CommandAlias("analyse|analytics|anl")
@CommandPermission("analyse.command")
public class AnalyseCommand extends BaseCommand {

  private final AnalyseVelocity plugin;

  public AnalyseCommand(AnalyseVelocity plugin) {
    this.plugin = plugin;
  }

  @Default
  @Subcommand("status")
  @Description("Show plugin status")
  public void onStatus(CommandSource sender) {
    boolean connected = Analyse.isAvailable();
    int trackedPlayers = plugin.getSessionManager().getSessionCount();
    boolean debugEnabled = plugin.isDebugEnabled();
    int configuredServers = plugin.getPluginConfig().getServers().size();

    sender.sendMessage(Component.text("§8§m                              "));
    sender.sendMessage(Component.text("  §b§lAnalyse §7v1.0.0 §8(Velocity)"));
    sender.sendMessage(Component.text("§8§m                              "));
    sender.sendMessage(Component.text("  §7Status: " + (connected ? "§a● Connected" : "§c● Disconnected")));
    sender.sendMessage(Component.text("  §7API: §fapi.analyse.net"));
    sender.sendMessage(Component.text("  §7Servers Configured: §f" + configuredServers));
    sender.sendMessage(Component.text("  §7Players Tracked: §f" + trackedPlayers));
    sender.sendMessage(Component.text("  §7Debug: " + (debugEnabled ? "§aEnabled" : "§7Disabled")));
    sender.sendMessage(Component.text("§8§m                              "));
  }

  @Subcommand("debug")
  @Description("Toggle debug mode")
  @CommandPermission("analyse.command.debug")
  public void onDebug(CommandSource sender) {
    boolean newState = !plugin.getPluginConfig().isDebug();
    plugin.getPluginConfig().setDebug(newState);

    if (newState) {
      sender.sendMessage(Component.text("§aDebug mode enabled."));
    } else {
      sender.sendMessage(Component.text("§7Debug mode disabled."));
    }
  }

  @Subcommand("event")
  @Description("Send a custom event")
  @CommandPermission("analyse.command.event")
  @Syntax("<name> [--player <player>] [--value <number>] [--data <key=value>...]")
  @CommandCompletion("test_event|custom_event @players")
  public void onEvent(CommandSource sender, String[] args) {
    if (args.length == 0) {
      sender.sendMessage(Component.text("§cUsage: /analyse event <name> [--player <name>] [--value <number>] [--data key=value...]"));
      return;
    }

    if (!Analyse.isAvailable()) {
      sender.sendMessage(Component.text("§cAnalyse is not connected. Make sure a default server is configured."));
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
          sender.sendMessage(Component.text("§cInvalid value. Must be a number."));
          return;
        }
      } else if (arg.equals("--data") && i + 1 < args.length) {
        String dataArg = args[++i];
        int eqIndex = dataArg.indexOf('=');
        if (eqIndex > 0) {
          String key = dataArg.substring(0, eqIndex);
          String val = dataArg.substring(eqIndex + 1);
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
      Player player = plugin.getServer().getPlayer(playerName).orElse(null);
      if (player != null) {
        builder.withPlayer(player.getUniqueId(), player.getUsername());
      } else {
        sender.sendMessage(Component.text("§cPlayer '" + playerName + "' not found online."));
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
    builder.send(response -> {
      if (response != null && response.isSuccess()) {
        sender.sendMessage(Component.text("§a✓ Event '§f" + eventName + "§a' sent successfully"));
        if (finalPlayerName != null) {
          sender.sendMessage(Component.text("  §7Player: §f" + finalPlayerName));
        }
        if (finalValue != null) {
          sender.sendMessage(Component.text("  §7Value: §f" + finalValue));
        }
        if (!data.isEmpty()) {
          sender.sendMessage(Component.text("  §7Data: §f" + data));
        }
      } else {
        sender.sendMessage(Component.text("§c✗ Failed to send event '§f" + eventName + "§c'"));
      }
    });
  }

  @Subcommand("help")
  @Description("Show help information")
  public void onHelp(CommandSource sender) {
    sender.sendMessage(Component.text("§8§m                              "));
    sender.sendMessage(Component.text("  §b§lAnalyse Commands"));
    sender.sendMessage(Component.text("§8§m                              "));
    sender.sendMessage(Component.text("  §e/analyse §7- Show plugin status"));
    sender.sendMessage(Component.text("  §e/analyse status §7- Show plugin status"));
    sender.sendMessage(Component.text("  §e/analyse debug §7- Toggle debug mode"));
    sender.sendMessage(Component.text("  §e/analyse event <name> §7- Send custom event"));
    sender.sendMessage(Component.text("    §7Options:"));
    sender.sendMessage(Component.text("    §f--player <name> §7- Associate with player"));
    sender.sendMessage(Component.text("    §f--value <number> §7- Set numeric value"));
    sender.sendMessage(Component.text("    §f--data <key=value> §7- Add data (repeatable)"));
    sender.sendMessage(Component.text("  §e/analyse help §7- Show this help"));
    sender.sendMessage(Component.text("§8§m                              "));
  }
}
