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
import com.serverstats.api.addon.LoadedAddon;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.api.object.builder.EventBuilder;
import com.serverstats.paper.ServerStatsPlugin;
import com.serverstats.paper.object.session.PlayerSession;
import com.serverstats.paper.util.ComponentUtil;
import com.serverstats.paper.util.SchedulerUtil;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.request.PlayerInfoRequest;
import com.serverstats.sdk.response.PlayerInfoResponse;
import com.serverstats.sdk.response.ServerInfoResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
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
    boolean initialized = plugin.isInitialized();
    boolean apiConnected = ServerStats.isConnected();
    int trackedPlayers = plugin.getSessionManager().getSessionCount();
    boolean debugEnabled = plugin.isDebugEnabled();

    StringBuilder message = new StringBuilder();
    message.append("#3498db&l「 ServerStats &r&fv").append(BuildConstants.VERSION).append(" #3498db&l」&r\n");

    if (!initialized) {
      message.append(" #5dade2┃ &fStatus: &c● Not Initialized&r\n");
      message.append(" #5dade2┃ &7Set a valid API key and run &f/serverstats reload&r\n");
    } else {
      message.append(" #5dade2┃ &fStatus: ").append(apiConnected ? "&a● Connected" : "&c● Disconnected").append("&r\n");
      if (!apiConnected && ServerStats.getLastConnectionError() != null) {
        message.append(" #5dade2┃ &fError: &c").append(ServerStats.getLastConnectionError()).append("&r\n");
      }
      message.append(" #5dade2┃ &fAPI: &7api.serverstats.com&r\n");
      message.append(" #5dade2┃ &fPlayers Tracked: &7").append(trackedPlayers).append("&r\n");
    }

    message.append(" #5dade2┃ &fDebug: ").append(debugEnabled ? "&aEnabled" : "&7Disabled").append("&r\n");
    send(sender, message.toString());
  }

  @Subcommand("reload")
  @Description("Reload configuration")
  @CommandPermission("serverstats.command.reload")
  public void onReload(CommandSender sender) {
    // Reload the Bukkit config file
    plugin.reloadConfig();

    // Reload the plugin configuration object
    plugin.getPluginConfig().reload();

    // Check if the config is now valid
    if (plugin.getPluginConfig().isValid()) {
      // Reinitialize ServerStats with the new config
      plugin.initializeServerStats();
      send(sender, "&aServerStats configuration reloaded and initialized successfully.");
    } else {
      send(sender, "&cConfiguration reloaded, but API key is invalid. Please set a valid API key in config.yml");
    }
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
        SchedulerUtil.runSync(plugin, () -> {
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
        SchedulerUtil.runSync(plugin, () -> {
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
        SchedulerUtil.runSync(plugin, () -> {
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
        SchedulerUtil.runSync(plugin, () -> {
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
        SchedulerUtil.runSync(plugin, () -> {
          send(sender, buildPlayerInfoMessage(player.getName(), session, response));
        });
      }

      @Override
      public void onError(ServerStatsException exception) {
        SchedulerUtil.runSync(plugin, () -> {
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

  @Subcommand("addons")
  @Description("List all loaded addons")
  @CommandPermission("serverstats.command.addons")
  public void onAddons(CommandSender sender) {
    Collection<LoadedAddon> addons = plugin.getAddonManager().getLoadedAddons();

    StringBuilder message = new StringBuilder();
    message.append("#3498db&l「 ServerStats Addons 」&r\n");

    if (addons.isEmpty()) {
      message.append(" &7No addons loaded.&r\n");
      message.append(" &7Place addon JARs in &fplugins/ServerStats/addons/&r\n");
    } else {
      for (LoadedAddon addon : addons) {
        String status = addon.isEnabled() ? "&a● Enabled" : "&c● Disabled";
        message.append(" #5dade2┃ &f").append(addon.getName());
        message.append(" &7v").append(addon.getVersion());
        message.append(" ").append(status).append("&r\n");
        if (!addon.getAuthor().isEmpty()) {
          message.append("    &7by ").append(addon.getAuthor()).append("&r\n");
        }
      }
      message.append("&r\n");
      message.append(" &7Total: &f").append(addons.size()).append(" addon(s)&r\n");
    }

    send(sender, message.toString());
  }

  @Subcommand("addons reload")
  @Description("Reload all addons or a specific addon")
  @CommandPermission("serverstats.command.addons.reload")
  @Syntax("[addon]")
  public void onAddonsReload(CommandSender sender, String[] args) {
    if (args.length == 0) {
      // Reload all addons
      send(sender, "&7Reloading all addons...");
      plugin.getAddonManager().reloadAddons();
      send(sender, "&aAll addons reloaded.");
    } else {
      // Reload specific addon
      String addonId = args[0];
      if (!plugin.getAddonManager().isAddonLoaded(addonId)) {
        send(sender, "&cAddon '" + addonId + "' is not loaded.");
        return;
      }

      send(sender, "&7Reloading addon '" + addonId + "'...");
      if (plugin.getAddonManager().reloadAddon(addonId)) {
        send(sender, "&aAddon '" + addonId + "' reloaded.");
      } else {
        send(sender, "&cFailed to reload addon '" + addonId + "'.");
      }
    }
  }

  @Subcommand("addons enable")
  @Description("Enable an addon")
  @CommandPermission("serverstats.command.addons.enable")
  @Syntax("<addon>")
  public void onAddonsEnable(CommandSender sender, String addonId) {
    if (!plugin.getAddonManager().isAddonLoaded(addonId)) {
      send(sender, "&cAddon '" + addonId + "' is not loaded.");
      return;
    }

    if (plugin.getAddonManager().isAddonEnabled(addonId)) {
      send(sender, "&7Addon '" + addonId + "' is already enabled.");
      return;
    }

    if (plugin.getAddonManager().enableAddon(addonId)) {
      send(sender, "&aAddon '" + addonId + "' enabled.");
    } else {
      send(sender, "&cFailed to enable addon '" + addonId + "'.");
    }
  }

  @Subcommand("addons disable")
  @Description("Disable an addon")
  @CommandPermission("serverstats.command.addons.disable")
  @Syntax("<addon>")
  public void onAddonsDisable(CommandSender sender, String addonId) {
    if (!plugin.getAddonManager().isAddonLoaded(addonId)) {
      send(sender, "&cAddon '" + addonId + "' is not loaded.");
      return;
    }

    if (!plugin.getAddonManager().isAddonEnabled(addonId)) {
      send(sender, "&7Addon '" + addonId + "' is already disabled.");
      return;
    }

    if (plugin.getAddonManager().disableAddon(addonId)) {
      send(sender, "&aAddon '" + addonId + "' disabled.");
    } else {
      send(sender, "&cFailed to disable addon '" + addonId + "'.");
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
    message.append(" #5dade2┃ &f/serverstats addons &7- List loaded addons&r\n");
    message.append(" #5dade2┃ &f/serverstats addons reload [id] &7- Reload addons&r\n");
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
