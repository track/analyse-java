package com.serverstats.bungeecord;

import co.aikar.commands.BungeeCommandManager;
import com.serverstats.api.ServerStats;
import com.serverstats.api.ServerStatsProvider;
import com.serverstats.api.BuildConstants;
import com.serverstats.api.addon.AddonManager;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.api.object.builder.EventBuilder;
import com.serverstats.api.platform.ServerStatsPlatform;
import com.serverstats.api.messaging.ServerStatsMessaging;
import com.serverstats.bungeecord.addon.BungeeAddonManager;
import com.serverstats.bungeecord.manager.ABTestManager;
import com.serverstats.bungeecord.command.ServerStatsCommand;
import com.serverstats.bungeecord.config.ServerStatsBungeeConfig;
import com.serverstats.bungeecord.listener.PlayerListener;
import com.serverstats.bungeecord.listener.PluginMessageListener;
import com.serverstats.bungeecord.manager.SessionManager;
import com.serverstats.bungeecord.task.HeartbeatTask;
import com.serverstats.bungeecord.update.BungeeUpdateChecker;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.ServerStatsClient;
import com.serverstats.sdk.request.EventRequest;
import com.serverstats.sdk.response.EventResponse;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * ServerStats plugin for BungeeCord proxy
 */
public class ServerStatsBungee extends Plugin implements ServerStatsPlatform {

  private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

  private ServerStatsBungeeConfig pluginConfig;
  private SessionManager sessionManager;
  private ABTestManager abTestManager;
  private BungeeAddonManager addonManager;
  private PlayerListener playerListener;
  private ScheduledTask heartbeatTask;
  private BungeeUpdateChecker updateChecker;

  @Override
  public void onEnable() {
    getLogger().info("Initializing ServerStats...");

    // Load configuration
    if (!loadConfig()) {
      return;
    }

    // Initialize session manager
    sessionManager = new SessionManager();

    // Register player listener
    playerListener = new PlayerListener(this);
    getProxy().getPluginManager().registerListener(this, playerListener);

    // Register plugin message channel for backend server communication
    getProxy().registerChannel(ServerStatsMessaging.CHANNEL);
    getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));
    getLogger().info("Registered plugin message channel: " + ServerStatsMessaging.CHANNEL);

    // Register with the API provider if a default server is configured
    if (playerListener.getDefaultClient() != null) {
      ServerStatsProvider.register(this);

      // Set up the event sender for ServerStats.trackEvent()
      ServerStats.setEventSender(this::sendEvent);

      // Initialize A/B test manager
      abTestManager = new ABTestManager(this);
      abTestManager.start();
    }

    // Register commands using ACF
    BungeeCommandManager commandManager = new BungeeCommandManager(this);
    commandManager.registerCommand(new ServerStatsCommand(this));

    // Start heartbeat task (after playerListener is initialized)
    startHeartbeatTask();

    // Initialize update checker (only if we have a default client)
    if (playerListener.getDefaultClient() != null) {
      updateChecker = new BungeeUpdateChecker(this, BuildConstants.VERSION);
      updateChecker.start();
    }

    // Initialize sessions for players already online (in case of reload)
    initializeOnlinePlayers();

    // Initialize addon manager and load addons
    addonManager = new BungeeAddonManager(this);
    addonManager.loadAddons();
    addonManager.enableAddons();

    getLogger().info(String.format("ServerStats initialized with %d server(s) configured",
        pluginConfig.getServers().size()));
  }

  /**
   * Send an event via the SDK
   *
   * @param event The event builder
   * @param callback Optional callback for result
   */
  private void sendEvent(EventBuilder event, Consumer<Boolean> callback) {
    ServerStatsClient client = getClient();
    if (client == null) {
      if (callback != null) {
        callback.accept(false);
      }
      return;
    }

    // Process A/B test ON_EVENT triggers if player is associated
    if (event.getPlayerUuid() != null && abTestManager != null) {
      ProxiedPlayer player = getProxy().getPlayer(event.getPlayerUuid());
      if (player != null) {
        abTestManager.processEvent(player, event.getName());
      }
    }

    // Send to API
    EventRequest request = new EventRequest(
        event.getName(),
        event.getPlayerUuid(),
        event.getPlayerUsername(),
        event.getData(),
        event.getValue()
    );

    client.trackEvent(request, new ServerStatsCallback<>() {
      @Override
      public void onSuccess(EventResponse response) {
        if (isDebugEnabled()) {
          logInfo(String.format("[DEBUG] Event '%s' tracked successfully (id: %s)",
              event.getName(), response.getEventId()));
        }

        if (callback != null) {
          callback.accept(true);
        }
      }

      @Override
      public void onError(ServerStatsException exception) {
        logWarning(String.format("Failed to track event '%s': %s",
            event.getName(), exception.getMessage()));

        if (callback != null) {
          callback.accept(false);
        }
      }
    });
  }

  /**
   * Initialize sessions for players already online (handles proxy reload scenario)
   */
  private void initializeOnlinePlayers() {
    int count = getProxy().getOnlineCount();
    if (count == 0) {
      return;
    }

    getLogger().info(String.format("Initializing sessions for %d online player(s)...", count));

    for (ProxiedPlayer player : getProxy().getPlayers()) {
      String username = player.getName();

      // Get hostname from virtual host
      String hostname = "unknown";
      InetSocketAddress virtualHost = player.getPendingConnection().getVirtualHost();
      if (virtualHost != null) {
        hostname = virtualHost.getHostString();
      }

      // Get player's IP address
      String ip = player.getAddress().getAddress().getHostAddress();

      // Create session
      sessionManager.createSession(player.getUniqueId(), hostname, ip);
      debug("Created session for existing player %s (hostname: %s, ip: %s)", username, hostname, ip);

      // If player is already on a server, send join event
      if (player.getServer() != null) {
        String serverName = player.getServer().getInfo().getName();
        playerListener.sendJoinEventForExistingPlayer(player, serverName);
      }
    }
  }

  @Override
  public void onDisable() {
    getLogger().info("Shutting down ServerStats...");

    // Disable all addons first
    if (addonManager != null) {
      addonManager.disableAddons();
    }

    // Unregister from the API provider
    ServerStatsProvider.unregister();

    // Clear the event sender
    ServerStats.setEventSender(null);

    // Stop A/B test manager
    if (abTestManager != null) {
      abTestManager.stop();
    }

    // Cancel heartbeat task
    if (heartbeatTask != null) {
      heartbeatTask.cancel();
    }

    // Shutdown player listener (closes API clients)
    if (playerListener != null) {
      playerListener.shutdown();
    }

    getLogger().info("ServerStats shutdown complete");
  }

  /**
   * Load configuration from file
   *
   * @return true if successful, false otherwise
   */
  private boolean loadConfig() {
    try {
      pluginConfig = ServerStatsBungeeConfig.load(getDataFolder().toPath());
    } catch (IOException e) {
      getLogger().severe(String.format("Failed to load configuration: %s", e.getMessage()));
      return false;
    }

    // Check if any servers are configured
    if (pluginConfig.getServers().isEmpty()) {
      getLogger().warning("No servers configured! Please edit the config.json file.");
      return false;
    }

    return true;
  }

  /**
   * Start the heartbeat task (runs every 30 seconds)
   */
  private void startHeartbeatTask() {
    heartbeatTask = getProxy().getScheduler().schedule(
        this,
        new HeartbeatTask(this, playerListener),
        HEARTBEAT_INTERVAL_SECONDS,
        HEARTBEAT_INTERVAL_SECONDS,
        TimeUnit.SECONDS
    );
  }

  /**
   * Log a debug message (only if debug mode is enabled)
   *
   * @param message The message to log
   */
  public void debug(String message) {
    if (pluginConfig != null && pluginConfig.isDebug()) {
      getLogger().info("[DEBUG] " + message);
    }
  }

  /**
   * Log a formatted debug message (only if debug mode is enabled)
   *
   * @param format The format string
   * @param args The arguments
   */
  public void debug(String format, Object... args) {
    if (pluginConfig != null && pluginConfig.isDebug()) {
      getLogger().info("[DEBUG] " + String.format(format, args));
    }
  }

  // ========== ServerStatsPlatform Interface Methods ==========

  @Override
  public SessionManager getSessionManager() {
    return sessionManager;
  }

  @Override
  public ABTestManager getABTestManager() {
    return abTestManager;
  }

  @Override
  public AddonManager getAddonManager() {
    return addonManager;
  }

  @Override
  public boolean isDebugEnabled() {
    return pluginConfig != null && pluginConfig.isDebug();
  }

  @Override
  public void logInfo(String message) {
    getLogger().info(message);
  }

  @Override
  public void logWarning(String message) {
    getLogger().warning(message);
  }

  @Override
  public String getVersion() {
    return BuildConstants.VERSION;
  }

  // ========== Internal Getters ==========

  /**
   * Get the plugin configuration
   *
   * @return The plugin config
   */
  public ServerStatsBungeeConfig getPluginConfig() {
    return pluginConfig;
  }

  /**
   * Get the SDK client (from player listener)
   *
   * @return The ServerStats client, or null
   */
  public ServerStatsClient getClient() {
    return playerListener != null ? playerListener.getDefaultClient() : null;
  }

  /**
   * Get the player listener
   *
   * @return The player listener
   */
  public PlayerListener getPlayerListener() {
    return playerListener;
  }

  /**
   * Get the update checker
   *
   * @return The update checker
   */
  public BungeeUpdateChecker getUpdateChecker() {
    return updateChecker;
  }
}
