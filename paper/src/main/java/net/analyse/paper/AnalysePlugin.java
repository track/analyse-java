package net.analyse.paper;

import co.aikar.commands.PaperCommandManager;
import net.analyse.api.Analyse;
import net.analyse.api.AnalyseProvider;
import net.analyse.api.BuildConstants;
import net.analyse.api.exception.AnalyseException;
import net.analyse.api.object.builder.EventBuilder;
import net.analyse.api.platform.AnalysePlatform;
import net.analyse.paper.manager.ABTestManager;
import net.analyse.paper.command.AnalyseCommand;
import net.analyse.paper.config.AnalysePaperConfig;
import net.analyse.paper.listener.PlayerListener;
import net.analyse.paper.manager.SessionManager;
import net.analyse.paper.task.HeartbeatTask;
import net.analyse.paper.update.PaperUpdateChecker;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.config.AnalyseConfig;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.response.EventResponse;
import net.analyse.sdk.response.JoinResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Analyse plugin for Paper/Spigot/Bukkit servers
 */
public class AnalysePlugin extends JavaPlugin implements AnalysePlatform {

  private AnalysePaperConfig pluginConfig;
  private SessionManager sessionManager;
  private ABTestManager abTestManager;
  private AnalyseClient client;
  private BukkitTask heartbeatTask;
  private PaperUpdateChecker updateChecker;
  private boolean configValid = false;

  @Override
  public void onLoad() {
    getLogger().info("Loading Analyse...");

    // Load configuration
    pluginConfig = new AnalysePaperConfig(this);

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
      getLogger().severe("Cannot enable Analyse - invalid configuration!");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    getLogger().info("Enabling Analyse...");

    // Initialize SDK client
    AnalyseConfig sdkConfig = new AnalyseConfig(pluginConfig.getApiKey());
    client = new AnalyseClient(sdkConfig);

    // Register with the API provider so other plugins can use Analyse.get()
    AnalyseProvider.register(this);

    // Set up the event sender for Analyse.trackEvent()
    Analyse.setEventSender(this::sendEvent);

    // Initialize session manager
    sessionManager = new SessionManager();

    // Register player listener
    PlayerListener playerListener = new PlayerListener(this, client);
    getServer().getPluginManager().registerEvents(playerListener, this);

    // Register commands using ACF
    PaperCommandManager commandManager = new PaperCommandManager(this);
    commandManager.registerCommand(new AnalyseCommand(this));

    // Start heartbeat task (every 30 seconds = 600 ticks)
    heartbeatTask = getServer().getScheduler().runTaskTimerAsynchronously(
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

    getLogger().info("Analyse enabled successfully!");
  }

  /**
   * Send an event via the SDK
   *
   * @param event The event builder
   * @param callback Optional callback for result
   */
  private void sendEvent(EventBuilder event, Consumer<Boolean> callback) {
    // Process A/B test ON_EVENT triggers if player is associated
    if (event.getPlayerUuid() != null && abTestManager != null) {
      Player player = getServer().getPlayer(event.getPlayerUuid());
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

      client.join(request, new AnalyseCallback<>() {
        @Override
        public void onSuccess(JoinResponse response) {
          sessionManager.getSession(uuid).ifPresent(session -> session.setSessionId(response.getSessionId()));
          debug("Join event sent for existing player %s (sessionId: %s, bedrock: %s)",
              username, response.getSessionId(), isBedrock);
        }

        @Override
        public void onError(AnalyseException exception) {
          getLogger().warning(String.format("Failed to send join event for existing player %s: %s",
              username, exception.getMessage()));
        }
      });
    }
  }

  @Override
  public void onDisable() {
    getLogger().info("Disabling Analyse...");

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

    // Shutdown SDK client
    if (client != null) {
      client.shutdown();
    }

    getLogger().info("Analyse disabled");
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
  public AnalysePaperConfig getPluginConfig() {
    return pluginConfig;
  }

  /**
   * Get the SDK client
   *
   * @return The Analyse client
   */
  public AnalyseClient getClient() {
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
