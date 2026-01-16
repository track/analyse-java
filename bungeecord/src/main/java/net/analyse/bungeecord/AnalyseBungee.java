package net.analyse.bungeecord;

import co.aikar.commands.BungeeCommandManager;
import net.analyse.api.Analyse;
import net.analyse.api.AnalyseProvider;
import net.analyse.api.BuildConstants;
import net.analyse.api.exception.AnalyseException;
import net.analyse.api.object.builder.EventBuilder;
import net.analyse.api.platform.AnalysePlatform;
import net.analyse.bungeecord.manager.ABTestManager;
import net.analyse.bungeecord.command.AnalyseCommand;
import net.analyse.bungeecord.config.AnalyseBungeeConfig;
import net.analyse.bungeecord.listener.PlayerListener;
import net.analyse.bungeecord.manager.SessionManager;
import net.analyse.bungeecord.task.HeartbeatTask;
import net.analyse.bungeecord.update.BungeeUpdateChecker;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.response.EventResponse;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Analyse plugin for BungeeCord proxy
 */
public class AnalyseBungee extends Plugin implements AnalysePlatform {

  private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

  private AnalyseBungeeConfig pluginConfig;
  private SessionManager sessionManager;
  private ABTestManager abTestManager;
  private PlayerListener playerListener;
  private ScheduledTask heartbeatTask;
  private BungeeUpdateChecker updateChecker;

  @Override
  public void onEnable() {
    getLogger().info("Initializing Analyse...");

    // Load configuration
    if (!loadConfig()) {
      return;
    }

    // Initialize session manager
    sessionManager = new SessionManager();

    // Register player listener
    playerListener = new PlayerListener(this);
    getProxy().getPluginManager().registerListener(this, playerListener);

    // Register with the API provider if a default server is configured
    if (playerListener.getDefaultClient() != null) {
      AnalyseProvider.register(this);

      // Set up the event sender for Analyse.trackEvent()
      Analyse.setEventSender(this::sendEvent);

      // Initialize A/B test manager
      abTestManager = new ABTestManager(this);
      abTestManager.start();
    }

    // Register commands using ACF
    BungeeCommandManager commandManager = new BungeeCommandManager(this);
    commandManager.registerCommand(new AnalyseCommand(this));

    // Start heartbeat task (after playerListener is initialized)
    startHeartbeatTask();

    // Initialize update checker (only if we have a default client)
    if (playerListener.getDefaultClient() != null) {
      updateChecker = new BungeeUpdateChecker(this, BuildConstants.VERSION);
      updateChecker.start();
    }

    // Initialize sessions for players already online (in case of reload)
    initializeOnlinePlayers();

    getLogger().info(String.format("Analyse initialized with %d server(s) configured",
        pluginConfig.getServers().size()));
  }

  /**
   * Send an event via the SDK
   *
   * @param event The event builder
   * @param callback Optional callback for result
   */
  private void sendEvent(EventBuilder event, Consumer<Boolean> callback) {
    AnalyseClient client = getClient();
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

    client.trackEvent(request, new AnalyseCallback<>() {
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
      public void onError(AnalyseException exception) {
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
    getLogger().info("Shutting down Analyse...");

    // Unregister from the API provider
    AnalyseProvider.unregister();

    // Clear the event sender
    Analyse.setEventSender(null);

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

    getLogger().info("Analyse shutdown complete");
  }

  /**
   * Load configuration from file
   *
   * @return true if successful, false otherwise
   */
  private boolean loadConfig() {
    try {
      pluginConfig = AnalyseBungeeConfig.load(getDataFolder().toPath());
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

  // ========== AnalysePlatform Interface Methods ==========

  @Override
  public SessionManager getSessionManager() {
    return sessionManager;
  }

  @Override
  public ABTestManager getABTestManager() {
    return abTestManager;
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
  public AnalyseBungeeConfig getPluginConfig() {
    return pluginConfig;
  }

  /**
   * Get the SDK client (from player listener)
   *
   * @return The Analyse client, or null
   */
  public AnalyseClient getClient() {
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
