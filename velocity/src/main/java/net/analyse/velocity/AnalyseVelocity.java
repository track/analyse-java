package net.analyse.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import co.aikar.commands.VelocityCommandManager;
import net.analyse.api.Analyse;
import net.analyse.api.AnalyseProvider;
import net.analyse.api.BuildConstants;
import net.analyse.api.exception.AnalyseException;
import net.analyse.api.object.builder.EventBuilder;
import net.analyse.api.platform.AnalysePlatform;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.response.EventResponse;
import net.analyse.velocity.command.AnalyseCommand;
import net.analyse.velocity.config.AnalyseVelocityConfig;
import net.analyse.velocity.listener.PlayerListener;
import net.analyse.velocity.manager.ABTestManager;
import net.analyse.velocity.manager.SessionManager;
import net.analyse.velocity.task.HeartbeatTask;
import net.analyse.velocity.update.VelocityUpdateChecker;
import org.slf4j.Logger;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Analyse plugin for Velocity proxy
 */
@Plugin(
    id = "analyse",
    name = "Analyse",
    version = BuildConstants.VERSION,
    description = "Analytics tracking plugin for Minecraft servers",
    authors = {"VertCode"}
)
public class AnalyseVelocity implements AnalysePlatform {

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;

  private AnalyseVelocityConfig pluginConfig;
  private SessionManager sessionManager;
  private ABTestManager abTestManager;
  private PlayerListener playerListener;
  private ScheduledTask heartbeatTask;
  private VelocityUpdateChecker updateChecker;

  @Inject
  public AnalyseVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    logger.info("Initializing Analyse...");

    // Load configuration
    if (!loadConfig()) {
      return;
    }

    // Initialize session manager
    sessionManager = new SessionManager();

    // Register player listener
    playerListener = new PlayerListener(this);
    server.getEventManager().register(this, playerListener);

    // Register with the API provider if at least one server is configured
    if (playerListener.hasAnyClient()) {
      AnalyseProvider.register(this);

      // Set up the event sender for Analyse.trackEvent()
      Analyse.setEventSender(this::sendEvent);

      // Initialize A/B test manager
      abTestManager = new ABTestManager(this);
      abTestManager.start();
    }

    // Register commands using ACF
    VelocityCommandManager commandManager = new VelocityCommandManager(server, this);
    commandManager.registerCommand(new AnalyseCommand(this));

    // Start heartbeat task (after playerListener is initialized)
    startHeartbeatTask();

    // Initialize update checker (only if we have any client)
    if (playerListener.hasAnyClient()) {
      updateChecker = new VelocityUpdateChecker(this, BuildConstants.VERSION);
      updateChecker.start();
    }

    // Initialize sessions for players already online (in case of reload)
    initializeOnlinePlayers();

    logger.info(String.format("Analyse initialized with %d server(s) configured",
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
      server.getPlayer(event.getPlayerUuid()).ifPresent(player -> {
        abTestManager.processEvent(player, event.getName());
      });
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
    int count = server.getPlayerCount();
    if (count == 0) {
      return;
    }

    logger.info(String.format("Initializing sessions for %d online player(s)...", count));

    for (Player player : server.getAllPlayers()) {
      String username = player.getUsername();

      // Get hostname from virtual host
      String hostname = player.getVirtualHost()
          .map(InetSocketAddress::getHostString)
          .orElse("unknown");

      // Get player's IP address
      String ip = player.getRemoteAddress().getAddress().getHostAddress();

      // Create session
      sessionManager.createSession(player.getUniqueId(), hostname, ip);
      debug("Created session for existing player %s (hostname: %s, ip: %s)", username, hostname, ip);

      // If player is already on a server, send join event
      player.getCurrentServer().ifPresent(serverConnection -> {
        String serverName = serverConnection.getServerInfo().getName();
        playerListener.sendJoinEventForExistingPlayer(player, serverName);
      });
    }
  }

  @Subscribe
  public void onProxyReload(ProxyReloadEvent event) {
    logger.info("Reloading Analyse configuration...");

    // Cancel existing heartbeat task
    if (heartbeatTask != null) {
      heartbeatTask.cancel();
    }

    // Shutdown existing clients
    if (playerListener != null) {
      playerListener.shutdown();
    }

    // Reload configuration
    if (!loadConfig()) {
      logger.error("Failed to reload configuration!");
      return;
    }

    // Reinitialize player listener clients
    if (playerListener != null) {
      playerListener.reinitialize();
    }

    // Restart heartbeat task
    startHeartbeatTask();

    logger.info(String.format("Analyse reloaded with %d server(s) configured",
        pluginConfig.getServers().size()));
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    logger.info("Shutting down Analyse...");

    // Unregister from the API provider
    AnalyseProvider.unregister();

    // Clear the event sender
    Analyse.setEventSender(null);

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

    // Shutdown player listener (closes API clients)
    if (playerListener != null) {
      playerListener.shutdown();
    }

    logger.info("Analyse shutdown complete");
  }

  /**
   * Load configuration from file
   *
   * @return true if successful, false otherwise
   */
  private boolean loadConfig() {
    try {
      pluginConfig = AnalyseVelocityConfig.load(dataDirectory);
    } catch (IOException e) {
      logger.error("Failed to load configuration", e);
      return false;
    }

    // Check if any servers are configured
    if (pluginConfig.getServers().isEmpty()) {
      logger.warn("No servers configured! Please edit the config.json file.");
      return false;
    }

    return true;
  }

  /**
   * Start the heartbeat task (runs every 30 seconds)
   */
  private void startHeartbeatTask() {
    heartbeatTask = server.getScheduler()
        .buildTask(this, new HeartbeatTask(this, playerListener))
        .repeat(30, TimeUnit.SECONDS)
        .schedule();
  }

  /**
   * Log a debug message (only if debug mode is enabled)
   *
   * @param message The message to log
   */
  public void debug(String message) {
    if (pluginConfig != null && pluginConfig.isDebug()) {
      logger.info("[DEBUG] " + message);
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
      logger.info("[DEBUG] " + String.format(format, args));
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
    logger.info(message);
  }

  @Override
  public void logWarning(String message) {
    logger.warn(message);
  }

  @Override
  public String getVersion() {
    return BuildConstants.VERSION;
  }

  // ========== Internal Getters ==========

  /**
   * Get the proxy server
   *
   * @return The proxy server
   */
  public ProxyServer getServer() {
    return server;
  }

  /**
   * Get the logger
   *
   * @return The logger
   */
  public Logger getLogger() {
    return logger;
  }

  /**
   * Get the plugin configuration
   *
   * @return The plugin config
   */
  public AnalyseVelocityConfig getPluginConfig() {
    return pluginConfig;
  }

  /**
   * Get an SDK client for API operations.
   * Uses the configured default server if set, otherwise returns any available client.
   *
   * @return The Analyse client, or null if none available
   */
  public AnalyseClient getClient() {
    return playerListener != null ? playerListener.getAvailableClient() : null;
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
  public VelocityUpdateChecker getUpdateChecker() {
    return updateChecker;
  }
}
