package net.analyse.hytale.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;

import net.analyse.api.Analyse;
import net.analyse.api.object.builder.EventBuilder;
import net.analyse.hytale.HytalePlugin;
import net.analyse.hytale.util.ComponentUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main command handler for the Analyse plugin
 */
public class AnalyseCommand extends AbstractCommand {

  private final HytalePlugin plugin;

  public AnalyseCommand(HytalePlugin plugin) {
    super("analyse", "Analyse analytics plugin command");
    addAliases("analytics", "anl");
    setAllowsExtraArguments(true);
    this.plugin = plugin;
  }

  @Override
  public @Nullable CompletableFuture<Void> acceptCall(
      @Nonnull CommandSender sender,
      @Nonnull ParserContext parserContext,
      @Nonnull ParseResult parseResult
  ) {
    String input = parserContext.getInputString().trim();
    String[] parts = input.split("\\s+");

    // Remove command name from args
    String[] args = new String[0];
    if (parts.length > 1) {
      args = new String[parts.length - 1];
      System.arraycopy(parts, 1, args, 0, args.length);
    }

    // Route to subcommand
    if (args.length == 0) {
      onStatus(sender);
    } else {
      String subcommand = args[0].toLowerCase();
      String[] subArgs = new String[args.length - 1];
      if (args.length > 1) {
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
      }

      switch (subcommand) {
        case "status" -> onStatus(sender);
        case "debug" -> onDebug(sender);
        case "event" -> onEvent(sender, subArgs);
        case "help" -> onHelp(sender);
        default -> onHelp(sender);
      }
    }

    return CompletableFuture.completedFuture(null);
  }

  @Override
  protected @Nullable CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
    throw new UnsupportedOperationException();
  }

  /**
   * Show plugin status
   *
   * @param sender The command sender
   */
  private void onStatus(CommandSender sender) {
    boolean connected = Analyse.isAvailable();
    int trackedPlayers = plugin.getSessionManager().getSessionCount();
    boolean debugEnabled = plugin.isDebugEnabled();

    send(sender, "&8&m                              ");
    send(sender, "  &b&lAnalyse &7v" + plugin.getVersion());
    send(sender, "&8&m                              ");
    send(sender, "  &7Status: " + (connected ? "&a● Connected" : "&c● Disconnected"));
    send(sender, "  &7API: &fapi.analyse.net");
    send(sender, "  &7Players Tracked: &f" + trackedPlayers);
    send(sender, "  &7Debug: " + (debugEnabled ? "&aEnabled" : "&7Disabled"));
    send(sender, "&8&m                              ");
  }

  /**
   * Toggle debug mode
   *
   * @param sender The command sender
   */
  private void onDebug(CommandSender sender) {
    boolean newState = !plugin.getPluginConfig().isDebug();
    plugin.getPluginConfig().setDebug(newState);

    if (newState) {
      send(sender, "&aDebug mode enabled.");
    } else {
      send(sender, "&7Debug mode disabled.");
    }
  }

  /**
   * Send a custom event
   *
   * @param sender The command sender
   * @param args The arguments
   */
  private void onEvent(CommandSender sender, String[] args) {
    if (args.length == 0) {
      send(sender, "&cUsage: /analyse event <name> [--value <number>] [--data key=value...]");
      return;
    }

    if (!Analyse.isAvailable()) {
      send(sender, "&cAnalyse is not connected. Cannot send events.");
      return;
    }

    // Parse arguments
    String eventName = args[0];
    Double value = null;
    Map<String, Object> data = new HashMap<>();

    for (int i = 1; i < args.length; i++) {
      String arg = args[i];

      if (arg.equals("--value") && i + 1 < args.length) {
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

    // Add value if specified
    if (value != null) {
      builder.withValue(value);
    }

    // Add data if specified
    if (!data.isEmpty()) {
      builder.withData(data);
    }

    // Send the event
    Double finalValue = value;
    builder.send(success -> {
      if (Boolean.TRUE.equals(success)) {
        send(sender, "&a✓ Event '&f" + eventName + "&a' sent successfully");
        if (finalValue != null) {
          send(sender, "  &7Value: &f" + finalValue);
        }
        if (!data.isEmpty()) {
          send(sender, "  &7Data: &f" + data);
        }
      } else {
        send(sender, "&c✗ Failed to send event '&f" + eventName + "&c'");
      }
    });
  }

  /**
   * Show help information
   *
   * @param sender The command sender
   */
  private void onHelp(CommandSender sender) {
    send(sender, "&8&m                              ");
    send(sender, "  &b&lAnalyse Commands");
    send(sender, "&8&m                              ");
    send(sender, "  &e/analyse &7- Show plugin status");
    send(sender, "  &e/analyse status &7- Show plugin status");
    send(sender, "  &e/analyse debug &7- Toggle debug mode");
    send(sender, "  &e/analyse event <name> &7- Send custom event");
    send(sender, "    &7Options:");
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
    Message hytaleMessage = ComponentUtil.parseHytaleMessage(message);
    
    sender.sendMessage(hytaleMessage);
  }
}
