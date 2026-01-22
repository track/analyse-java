package com.serverstats.paper.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.serverstats.api.ServerStats;
import com.serverstats.api.BuildConstants;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.api.object.builder.EventBuilder;
import com.serverstats.paper.ServerStatsPlugin;
import com.serverstats.paper.object.session.PlayerSession;
import com.serverstats.paper.util.ComponentUtil;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.request.PlayerInfoRequest;
import com.serverstats.sdk.response.PlayerInfoResponse;
import com.serverstats.sdk.response.ServerInfoResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Main command handler for the ServerStats plugin using ACF
 */
@CommandAlias("serverstats|analyse|ss")
public class ServerStatsCommand extends BaseCommand {

  private final ServerStatsPlugin plugin;

  public ServerStatsCommand(ServerStatsPlugin plugin) {
    this.plugin = plugin;
  }

  @Default
  @Description("Show plugin info")
  public void onDefault(CommandSender sender) {
    // Check if user has admin permission
    if (sender.hasPermission("serverstats.command.status")) {
      showStatus(sender);
    } else {
      showPublicInfo(sender);
    }
  }

  @Subcommand("status")
  @Description("Show plugin status")
  @CommandPermission("serverstats.command.status")
  public void onStatus(CommandSender sender) {
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
    message.append("#3498db&l「 ServerStats &r&fv").append(BuildConstants.VERSION).append(" #3498db&l」&r\n");
    message.append(" #5dade2┃ &fStatus: ").append(connected ? "&a● Connected" : "&c● Disconnected").append("&r\n");
    message.append(" #5dade2┃ &fAPI: &7api.serverstats.com&r\n");
    message.append(" #5dade2┃ &fPlayers Tracked: &7").append(trackedPlayers).append("&r\n");
    message.append(" #5dade2┃ &fDebug: ").append(debugEnabled ? "&aEnabled" : "&7Disabled").append("&r\n");
    send(sender, message.toString());
  }

  @Subcommand("reload")
  @Description("Reload configuration")
  @CommandPermission("serverstats.command.reload")
  public void onReload(CommandSender sender) {
    plugin.reloadConfig();
    send(sender, "&aServerStats configuration reloaded.");
  }

  @Subcommand("debug")
  @Description("Toggle debug mode")
  @CommandPermission("serverstats.command.debug")
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
  @CommandPermission("serverstats.command.event")
  @Syntax("<name> [--player <player>] [--value <number>] [--data <key=value>...]")
  @CommandCompletion("test_event|custom_event @players")
  public void onEvent(CommandSender sender, String[] args) {
    if (args.length == 0) {
      send(sender, "&cUsage: /serverstats event <name> [--player <name>] [--value <number>] [--data key=value...]");
      return;
    }

    if (!ServerStats.isAvailable()) {
      send(sender, "&cServerStats is not connected. Cannot send events.");
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
    EventBuilder builder = ServerStats.trackEvent(eventName);

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

  @Subcommand("info")
  @Description("View server or player analytics")
  @CommandPermission("serverstats.command.info")
  @Syntax("[player]")
  @CommandCompletion("@players")
  public void onInfo(CommandSender sender, String[] args) {
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
    int onlinePlayers = Bukkit.getOnlinePlayers().size();

    // Fetch additional data from API
    plugin.getClient().getServerInfo(new ServerStatsCallback<>() {
      @Override
      public void onSuccess(ServerInfoResponse response) {
        Bukkit.getScheduler().runTask(plugin, () -> {
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
        });
      }

      @Override
      public void onError(ServerStatsException exception) {
        Bukkit.getScheduler().runTask(plugin, () -> {
          StringBuilder message = new StringBuilder();
          message.append("#3498db&l「 Server Analytics 」&r\n");
          message.append(" #5dade2┃ &fOnline Players: &7").append(onlinePlayers).append("&r\n");
          message.append(" #5dade2┃ &fTracked Sessions: &7").append(trackedSessions).append("&r\n");
          send(sender, message.toString());
        });
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
    Player player = Bukkit.getPlayer(playerName);
    if (player == null) {
      send(sender, "&cPlayer '" + playerName + "' is not online.");
      return;
    }

    // Get local session data
    PlayerSession session = plugin.getSessionManager().getSession(player.getUniqueId()).orElse(null);

    // Fetch additional data from API
    plugin.getClient().getPlayerInfo(new PlayerInfoRequest(player.getUniqueId()), new ServerStatsCallback<>() {
      @Override
      public void onSuccess(PlayerInfoResponse response) {
        Bukkit.getScheduler().runTask(plugin, () -> {
          send(sender, buildPlayerInfoMessage(player.getName(), session, response));
        });
      }

      @Override
      public void onError(ServerStatsException exception) {
        Bukkit.getScheduler().runTask(plugin, () -> {
          send(sender, buildPlayerInfoMessage(player.getName(), session, null));
        });
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

  @Subcommand("help")
  @Description("Show help information")
  @CommandPermission("serverstats.command.help")
  public void onHelp(CommandSender sender) {
    StringBuilder message = new StringBuilder();
    message.append("#3498db&l「 ServerStats Commands 」&r\n");
    message.append(" #5dade2┃ &f/serverstats &7- Show plugin info&r\n");
    message.append(" #5dade2┃ &f/serverstats status &7- Show plugin status&r\n");
    message.append(" #5dade2┃ &f/serverstats info &7- View server analytics&r\n");
    message.append(" #5dade2┃ &f/serverstats info <player> &7- View player analytics&r\n");
    message.append(" #5dade2┃ &f/serverstats reload &7- Reload configuration&r\n");
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
    sender.sendMessage(ComponentUtil.parse(message));
  }
}
