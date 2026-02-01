package com.serverstats.paper;

import co.aikar.commands.PaperCommandManager;
import com.serverstats.api.ServerStats;
import com.serverstats.api.ServerStatsProvider;
import com.serverstats.api.BuildConstants;
import com.serverstats.api.addon.AddonManager;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.api.object.builder.EventBuilder;
import com.serverstats.api.platform.ServerStatsPlatform;
import com.serverstats.paper.addon.PaperAddonManager;
import com.serverstats.paper.manager.ABTestManager;
import com.serverstats.paper.command.ServerStatsCommand;
import com.serverstats.paper.config.ServerStatsPaperConfig;
import com.serverstats.paper.listener.PlayerListener;
import com.serverstats.paper.manager.SessionManager;
import com.serverstats.paper.task.HeartbeatTask;
import com.serverstats.paper.update.PaperUpdateChecker;
import com.serverstats.paper.util.SchedulerUtil;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.ServerStatsClient;
import com.serverstats.sdk.config.ServerStatsConfig;
import com.serverstats.sdk.request.EventRequest;
import com.serverstats.sdk.request.JoinRequest;
import com.serverstats.sdk.response.EventResponse;
import com.serverstats.sdk.response.JoinResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * ServerStats plugin for Paper/Spigot/Bukkit servers
 */
public class ServerStatsPlugin extends JavaPlugin implements ServerStatsPlatform {

  private ServerStatsPaperConfig pluginConfig;
  private SessionManager sessionManager;
  private ABTestManager abTestManager;
  private PaperAddonManager addonManager;
  private ServerStatsClient client;
  private SchedulerUtil.CancellableTask heartbeatTask;
  private PaperUpdateChecker updateChecker;
  private boolean configValid = false;

  @Override
  public void onLoad() {
    getLogger().info("Loading ServerStats...");

    // Load configuration
    pluginConfig = new ServerStatsPaperConfig(this);

    // Validate configuration
    if (!pluginConfig.isValid()) {
      getLogger().warning("Invalid configuration! Please set your API key in config.yml");
      return;
    }

    configValid = true;
    getLogger().info("Configuration loaded successfully!");
  }

  @Override
  public void onEnable() {
    // Check if config was valid
    if (!configValid) {
      getLogger().severe("Cannot enable ServerStats - invalid configuration!");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    getLogger().info("Enabling ServerStats...");

    // Initialize SDK client
    ServerStatsConfig sdkConfig = new ServerStatsConfig(pluginConfig.getApiKey());
    client = new ServerStatsClient(sdkConfig);

    // Register with the API provider so other plugins can use ServerStats.get()
    ServerStatsProvider.register(this);

    // Set up the event sender for ServerStats.trackEvent()
    ServerStats.setEventSender(this::sendEvent);

    // Initialize session manager
    sessionManager = new SessionManager();

    // Register player listener
    PlayerListener playerListener = new PlayerListener(this, client);
    getServer().getPluginManager().registerEvents(playerListener, this);

    // Register commands using ACF
    PaperCommandManager commandManager = new PaperCommandManager(this);
    commandManager.registerCommand(new ServerStatsCommand(this));

    // Start heartbeat task (every 30 seconds = 600 ticks)
    // Uses sync timer because HeartbeatTask collects player data on main thread, then sends async
    heartbeatTask = SchedulerUtil.runSyncTimer(
        this,
        new HeartbeatTask(this, client),
        600L,
        600L
    );

    // Initialize A/B test manager
    abTestManager = new ABTestManager(this);
    abTestManager.start();

    // Initialize update checker
    updateChecker = new PaperUpdateChecker(this, BuildConstants.VERSION);
    updateChecker.start();

    // Initialize sessions for players already online (in case of reload)
    initializeOnlinePlayers();

    // Initialize addon manager and load addons
    addonManager = new PaperAddonManager(this);
    addonManager.loadAddons();
    addonManager.enableAddons();

    getLogger().info("ServerStats enabled successfully!");
  }

  /**
   * Send an event via the SDK.
   * Thread-safe: can be called from any thread.
   *
   * @param event The event builder
   * @param callback Optional callback for result
   */
  private void sendEvent(EventBuilder event, Consumer<Boolean> callback) {
    // Process A/B test ON_EVENT triggers if player is associated
    // Must run on sync thread for Folia compatibility
    if (event.getPlayerUuid() != null && abTestManager != null) {
      SchedulerUtil.runSync(this, () -> {
        Player player = getServer().getPlayer(event.getPlayerUuid());
        if (player != null) {
          abTestManager.processEvent(player, event.getName());
        }
      });
    }

    // Send to API (async-safe operation)
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
   * Initialize sessions for players already online (handles plugin reload scenario)
   */
  private void initializeOnlinePlayers() {
    int count = getServer().getOnlinePlayers().size();
    if (count == 0) {
      return;
    }

    getLogger().info(String.format("Initializing sessions for %d online player(s)...", count));

    for (Player player : getServer().getOnlinePlayers()) {
      String username = player.getName();
      UUID uuid = player.getUniqueId();

      // Get hostname from player's virtual host (may be null after reload)
      String hostname = "unknown";
      InetSocketAddress virtualHost = player.getVirtualHost();
      if (virtualHost != null) {
        hostname = virtualHost.getHostName();
      }

      // Get player's IP address
      String ip = "unknown";
      InetSocketAddress address = player.getAddress();
      if (address != null && address.getAddress() != null) {
        ip = address.getAddress().getHostAddress();
      }

      // Create session
      sessionManager.createSession(uuid, hostname, ip);
      debug("Created session for existing player %s (hostname: %s, ip: %s)", username, hostname, ip);

      // Check if player is a Bedrock player
      boolean isBedrock = pluginConfig.isBedrock(username);

      // Send join event to the API
      JoinRequest request = new JoinRequest(uuid, username, hostname, ip, isBedrock);

      client.join(request, new ServerStatsCallback<>() {
        @Override
        public void onSuccess(JoinResponse response) {
          sessionManager.getSession(uuid).ifPresent(session -> session.setSessionId(response.getSessionId()));
          debug("Join event sent for existing player %s (sessionId: %s, bedrock: %s)",
              username, response.getSessionId(), isBedrock);
        }

        @Override
        public void onError(ServerStatsException exception) {
          getLogger().warning(String.format("Failed to send join event for existing player %s: %s",
              username, exception.getMessage()));
        }
      });
    }
  }

  @Override
  public void onDisable() {
    getLogger().info("Disabling ServerStats...");

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

    // Stop update checker
    if (updateChecker != null) {
      updateChecker.stop();
    }

    // Cancel heartbeat task
    if (heartbeatTask != null) {
      heartbeatTask.cancel();
    }

    // Shutdown SDK client
    if (client != null) {
      client.shutdown();
    }

    getLogger().info("ServerStats disabled");
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
  public ServerStatsPaperConfig getPluginConfig() {
    return pluginConfig;
  }

  /**
   * Get the SDK client
   *
   * @return The ServerStats client
   */
  public ServerStatsClient getClient() {
    return client;
  }

  /**
   * Get the update checker
   *
   * @return The update checker
   */
  public PaperUpdateChecker getUpdateChecker() {
    return updateChecker;
  }
}
