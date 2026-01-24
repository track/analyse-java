package com.serverstats.hytale.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;

import com.serverstats.api.ServerStats;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.api.object.builder.EventBuilder;
import com.serverstats.hytale.HytalePlugin;
import com.serverstats.hytale.object.session.PlayerSession;
import com.serverstats.hytale.util.ComponentUtil;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.request.PlayerInfoRequest;
import com.serverstats.sdk.response.PlayerInfoResponse;
import com.serverstats.sdk.response.ServerInfoResponse;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main command handler for the ServerStats plugin
 */
public class ServerStatsCommand extends AbstractCommand {

  private final HytalePlugin plugin;

  public ServerStatsCommand(HytalePlugin plugin) {
    super("serverstats", "ServerStats analytics plugin command");
    addAliases("analyse", "ss");
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
      onDefault(sender);
    } else {
      String subcommand = args[0].toLowerCase();
      String[] subArgs = new String[args.length - 1];
      if (args.length > 1) {
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
      }

      switch (subcommand) {
        case "status" -> onStatus(sender);
        case "info" -> onInfo(sender, subArgs);
        case "debug" -> onDebug(sender);
        case "event" -> onEvent(sender, subArgs);
        case "help" -> onHelp(sender);
        default -> onDefault(sender);
      }
    }

    return CompletableFuture.completedFuture(null);
  }

  @Override
  protected @Nullable CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
    throw new UnsupportedOperationException();
  }

  /**
   * Default command handler - shows public info or status based on permission
   *
   * @param sender The command sender
   */
  private void onDefault(CommandSender sender) {
    // For Hytale, we'll show status to all for now (permission system may differ)
    // TODO: Add proper permission check when Hytale permission API is available
    showPublicInfo(sender);
  }

  /**
   * Show plugin status (admin only)
   *
   * @param sender The command sender
   */
  private void onStatus(CommandSender sender) {
    showStatus(sender);
  }

  /**
   * Show public info message for users without permission
   *
   * @param sender The command sender
   */
  private void showPublicInfo(CommandSender sender) {
    StringBuilder message = new StringBuilder();
    message.append("#3498db&l「 ServerStats 」&r\n");
    message.append(" #5dade2┃ &7This server uses &fServerStats &7to track\n");
    message.append(" #5dade2┃ &7player analytics and sessions.&r\n");
    message.append("&r\n");
    message.append(" &7→ &fserverstats.com&r\n");
    send(sender, message.toString());
  }

  /**
   * Show full status for admins
   *
   * @param sender The command sender
   */
  private void showStatus(CommandSender sender) {
    boolean connected = ServerStats.isAvailable();
    int trackedPlayers = plugin.getSessionManager().getSessionCount();
    boolean debugEnabled = plugin.isDebugEnabled();

    StringBuilder message = new StringBuilder();
    message.append("#3498db&l「 ServerStats &r&fv").append(plugin.getVersion()).append(" #3498db&l」&r\n");
    message.append(" #5dade2┃ &fStatus: ").append(connected ? "&a● Connected" : "&c● Disconnected").append("&r\n");
    message.append(" #5dade2┃ &fAPI: &7api.serverstats.com&r\n");
    message.append(" #5dade2┃ &fPlayers Tracked: &7").append(trackedPlayers).append("&r\n");
    message.append(" #5dade2┃ &fDebug: ").append(debugEnabled ? "&aEnabled" : "&7Disabled").append("&r\n");
    send(sender, message.toString());
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
      send(sender, "&cUsage: /serverstats event <name> [--value <number>] [--data key=value...]");
      return;
    }

    if (!ServerStats.isAvailable()) {
      send(sender, "&cServerStats is not connected. Cannot send events.");
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
    EventBuilder builder = ServerStats.trackEvent(eventName);

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
   * View server or player analytics
   *
   * @param sender The command sender
   * @param args The command arguments
   */
  private void onInfo(CommandSender sender, String[] args) {
    if (!ServerStats.isAvailable()) {
      send(sender, "&cServerStats is not connected.");
      return;
    }

    if (args.length == 0) {
      // Show server info
      showServerInfo(sender);
    } else {
      // Show player info
      showPlayerInfo(sender, args[0]);
    }
  }

  /**
   * Display server analytics information
   *
   * @param sender The command sender
   */
  private void showServerInfo(CommandSender sender) {
    int trackedSessions = plugin.getSessionManager().getSessionCount();
    int onlinePlayers = plugin.getUniverse().getPlayers().size();

    // Fetch additional data from API
    plugin.getClient().getServerInfo(new ServerStatsCallback<>() {
      @Override
public void onSuccess(ServerInfoResponse response) {
          StringBuilder message = new StringBuilder();
          message.append("#3498db&l「 Server Analytics 」&r\n");
          message.append(" #5dade2┃ &fOnline Players: &7").append(onlinePlayers).append("&r\n");
        message.append(" #5dade2┃ &fTracked Sessions: &7").append(trackedSessions).append("&r\n");
        if (response.getPeakToday() > 0) {
          message.append(" #5dade2┃ &fPeak Today: &7").append(response.getPeakToday()).append("&r\n");
        }
        if (response.getTotalJoinsToday() > 0) {
          message.append(" #5dade2┃ &fTotal Joins Today: &7").append(response.getTotalJoinsToday()).append("&r\n");
        }
        if (response.getUniquePlayersToday() > 0) {
          message.append(" #5dade2┃ &fUnique Players Today: &7").append(response.getUniquePlayersToday()).append("&r\n");
        }
        send(sender, message.toString());
      }

      @Override
public void onError(ServerStatsException exception) {
          StringBuilder message = new StringBuilder();
          message.append("#3498db&l「 Server Analytics 」&r\n");
          message.append(" #5dade2┃ &fOnline Players: &7").append(onlinePlayers).append("&r\n");
        message.append(" #5dade2┃ &fTracked Sessions: &7").append(trackedSessions).append("&r\n");
        send(sender, message.toString());
      }
    });
  }

  /**
   * Display player analytics information
   *
   * @param sender The command sender
   * @param playerName The player name to look up
   */
  private void showPlayerInfo(CommandSender sender, String playerName) {
    // Find player by name
    PlayerRef player = null;
    for (PlayerRef p : plugin.getUniverse().getPlayers()) {
      if (p.getUsername().equalsIgnoreCase(playerName)) {
        player = p;
        break;
      }
    }

    if (player == null) {
      send(sender, "&cPlayer '" + playerName + "' is not online.");
      return;
    }

    // Get local session data
    PlayerSession session = plugin.getSessionManager().getSession(player.getUuid()).orElse(null);
    String username = player.getUsername();

    // Fetch additional data from API
    plugin.getClient().getPlayerInfo(new PlayerInfoRequest(player.getUuid()), new ServerStatsCallback<>() {
      @Override
      public void onSuccess(PlayerInfoResponse response) {
        send(sender, buildPlayerInfoMessage(username, session, response));
      }

      @Override
      public void onError(ServerStatsException exception) {
        send(sender, buildPlayerInfoMessage(username, session, null));
      }
    });
  }

  /**
   * Build the player info message string
   *
   * @param playerName The player's name
   * @param session The local session data (may be null)
   * @param response The API response (may be null)
   * @return The formatted message string
   */
  private String buildPlayerInfoMessage(String playerName, PlayerSession session, PlayerInfoResponse response) {
    StringBuilder message = new StringBuilder();
    message.append("#3498db&l「 Player: &r&f").append(playerName).append(" #3498db&l」&r\n");
    message.append(" #5dade2┃ &fStatus: &a● Online&r\n");

    if (session != null) {
      Duration currentSession = Duration.between(session.getJoinTime(), Instant.now());
      message.append(" #5dade2┃ &fCurrent Session: &7").append(formatDuration(currentSession)).append("&r\n");
      message.append(" #5dade2┃ &fHostname: &7").append(session.getHostname()).append("&r\n");
    }

    if (response != null) {
      // Add statistics section header if we have API data
      boolean hasStats = response.getTotalPlaytimeSeconds() > 0 || response.getTotalSessions() > 0 
          || (response.getFirstSeen() != null && !response.getFirstSeen().isEmpty())
          || (response.getCampaign() != null && !response.getCampaign().isEmpty())
          || (response.getCountry() != null && !response.getCountry().isEmpty());

      if (hasStats) {
        message.append("&r\n");
        message.append("#3498db&l「 Statistics 」&r\n");
      }

      if (response.getTotalPlaytimeSeconds() > 0) {
        message.append(" #5dade2┃ &fTotal Playtime: &7").append(formatDuration(Duration.ofSeconds(response.getTotalPlaytimeSeconds()))).append("&r\n");
      }
      if (response.getTotalSessions() > 0) {
        message.append(" #5dade2┃ &fTotal Sessions: &7").append(response.getTotalSessions()).append("&r\n");
      }
      if (response.getFirstSeen() != null && !response.getFirstSeen().isEmpty()) {
        message.append(" #5dade2┃ &fFirst Seen: &7").append(response.getFirstSeen()).append("&r\n");
      }
      if (response.getCampaign() != null && !response.getCampaign().isEmpty()) {
        message.append(" #5dade2┃ &fCampaign: &e").append(response.getCampaign()).append("&r\n");
      }
      if (response.getCountry() != null && !response.getCountry().isEmpty()) {
        message.append(" #5dade2┃ &fCountry: &7").append(response.getCountry()).append("&r\n");
      }
    }

    return message.toString();
  }

  /**
   * Format a duration into a human-readable string
   *
   * @param duration The duration to format
   * @return The formatted string (e.g., "2h 34m" or "5m 12s")
   */
  private String formatDuration(Duration duration) {
    long totalSeconds = duration.getSeconds();
    long days = totalSeconds / 86400;
    long hours = (totalSeconds % 86400) / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;

    if (days > 0) {
      return String.format("%dd %dh %dm", days, hours, minutes);
    } else if (hours > 0) {
      return String.format("%dh %dm", hours, minutes);
    } else if (minutes > 0) {
      return String.format("%dm %ds", minutes, seconds);
    } else {
      return String.format("%ds", seconds);
    }
  }

  /**
   * Show help information
   *
   * @param sender The command sender
   */
  private void onHelp(CommandSender sender) {
    StringBuilder message = new StringBuilder();
    message.append("#3498db&l「 ServerStats Commands 」&r\n");
    message.append(" #5dade2┃ &f/serverstats &7- Show plugin info&r\n");
    message.append(" #5dade2┃ &f/serverstats status &7- Show plugin status&r\n");
    message.append(" #5dade2┃ &f/serverstats info &7- View server analytics&r\n");
    message.append(" #5dade2┃ &f/serverstats info <player> &7- View player analytics&r\n");
    message.append(" #5dade2┃ &f/serverstats debug &7- Toggle debug mode&r\n");
    message.append(" #5dade2┃ &f/serverstats event <name> &7- Send custom event&r\n");
    message.append(" #5dade2┃ &f/serverstats help &7- Show this help&r\n");
    send(sender, message.toString());
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
