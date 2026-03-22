package net.analyse.hytale;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import net.analyse.api.Analyse;
import net.analyse.api.AnalyseProvider;
import net.analyse.api.addon.AddonManager;
import net.analyse.api.exception.AnalyseException;
import net.analyse.api.object.builder.EventBuilder;
import net.analyse.api.platform.AnalysePlatform;
import net.analyse.hytale.addon.HytaleAddonManager;
import net.analyse.hytale.command.AnalyseCommand;
import net.analyse.hytale.config.AnalyseHytaleConfig;
import net.analyse.hytale.listener.PlayerListener;
import net.analyse.hytale.manager.ABTestManager;
import net.analyse.hytale.manager.SessionManager;
import net.analyse.hytale.task.HeartbeatTask;
import net.analyse.hytale.update.HytaleUpdateChecker;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.config.AnalyseConfig;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.response.EventResponse;
import net.analyse.sdk.response.JoinResponse;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

/**
 * Analyse plugin for Hytale servers
 */
public class HytalePlugin extends JavaPlugin implements AnalysePlatform {

  private AnalyseHytaleConfig pluginConfig;
  private SessionManager sessionManager;
  private ABTestManager abTestManager;
  private HytaleAddonManager addonManager;
  private HytaleUpdateChecker updateChecker;
  private AnalyseClient client;
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> heartbeatTask;
  private boolean configValid = false;
  private String version;

  public HytalePlugin(@Nonnull JavaPluginInit init) {
    super(init);
    this.version = init.getPluginManifest().getVersion().toString();
  }

  @Override
  protected void setup() {
    super.setup();
    getLogger().atInfo().log("Loading Analyse...");

    // Migrate old "ServerStats" data folder if it exists
    migrateDataFolder();

    // Load configuration
    pluginConfig = AnalyseHytaleConfig.load(this);

    // Validate configuration
    if (!pluginConfig.isValid()) {
      getLogger()
        .atWarning()
        .log("Invalid configuration! Please set your API key in config.json");
      return;
    }

    configValid = true;
    getLogger().atInfo().log("Configuration loaded successfully!");
  }

  /**
   * Migrate the data folder from the old "ServerStats" name to "Analyse".
   * Automatically renames the old folder if it exists.
   */
  private void migrateDataFolder() {
    File dataDir = getDataDirectory().toFile();
    File oldFolder = new File(dataDir.getParentFile(), "ServerStats");
    if (oldFolder.exists() && oldFolder.isDirectory() && !dataDir.exists()) {
      if (oldFolder.renameTo(dataDir)) {
        getLogger().atInfo().log("Migrated data folder from ServerStats to Analyse");
      } else {
        getLogger().atWarning().log("Failed to migrate data folder from ServerStats to Analyse. Please rename it manually.");
      }
    }
  }

  @Override
  protected void start() {
    super.start();

    // Check if config was valid
    if (!configValid) {
      getLogger()
        .atSevere()
        .log("Cannot enable Analyse - invalid configuration!");
      return;
    }

    getLogger().atInfo().log("Enabling Analyse...");

    // Initialize SDK client
    AnalyseConfig sdkConfig = new AnalyseConfig(
      pluginConfig.getApiKey(),
      pluginConfig.isDevelopment()
    );
    client = new AnalyseClient(sdkConfig);

    // Register with the API provider so other plugins can use Analyse.get()
    AnalyseProvider.register(this);

    // Set up the event sender for Analyse.trackEvent()
    Analyse.setEventSender(this::sendEvent);

    // Initialize session manager
    sessionManager = new SessionManager();

    // Register player listener
    PlayerListener playerListener = new PlayerListener(this, client);
    getEventRegistry()
      .registerGlobal(PlayerReadyEvent.class, playerListener::onPlayerReady);
    getEventRegistry()
      .registerGlobal(
        PlayerDisconnectEvent.class,
        playerListener::onPlayerDisconnect
      );

    // Register commands
    getCommandRegistry().registerCommand(new AnalyseCommand(this));

    // Start heartbeat task (every 30 seconds)
    scheduler = Executors.newScheduledThreadPool(3);
    heartbeatTask = scheduler.scheduleAtFixedRate(
      new HeartbeatTask(this, client),
      30,
      30,
      TimeUnit.SECONDS
    );

    // Initialize A/B test manager
    abTestManager = new ABTestManager(this, scheduler);
    abTestManager.start();

    // Initialize update checker
    updateChecker = new HytaleUpdateChecker(this, getVersion(), scheduler);
    updateChecker.start();

    // Initialize sessions for players already online (in case of reload)
    initializeOnlinePlayers();

    // Initialize addon manager and load addons
    addonManager = new HytaleAddonManager(this, getDataDirectory());
    addonManager.loadAddons();
    addonManager.enableAddons();

    getLogger().atInfo().log("Analyse enabled successfully!");
  }

  /**
   * Send an event via the SDK
   *
   * @param event The event builder
   * @param callback Optional callback for result
   */
  private void sendEvent(EventBuilder event, Consumer<Boolean> callback) {
    // Send to API
    EventRequest request = new EventRequest(
      event.getName(),
      event.getPlayerUuid(),
      event.getPlayerUsername(),
      event.getData(),
      event.getValue()
    );

    client.trackEvent(
      request,
      new AnalyseCallback<>() {
        @Override
        public void onSuccess(EventResponse response) {
          if (isDebugEnabled()) {
            logInfo(
              String.format(
                "[DEBUG] Event '%s' tracked successfully (id: %s)",
                event.getName(),
                response.getEventId()
              )
            );
          }

          if (callback != null) {
            callback.accept(true);
          }
        }

        @Override
        public void onError(AnalyseException exception) {
          logWarning(
            String.format(
              "Failed to track event '%s': %s",
              event.getName(),
              exception.getMessage()
            )
          );

          if (callback != null) {
            callback.accept(false);
          }
        }
      }
    );
  }

  /**
   * Initialize sessions for players already online (handles plugin reload scenario)
   */
  private void initializeOnlinePlayers() {
    int count = getUniverse().getPlayers().size();
    if (count == 0) {
      return;
    }

    getLogger()
      .atInfo()
      .log("Initializing sessions for %d online player(s)...", count);

    for (PlayerRef playerRef : getUniverse().getPlayers()) {
      String username = playerRef.getUsername();
      UUID uuid = playerRef.getUuid();

      // Get hostname (Hytale doesn't seem to expose this yet)
      String hostname = "unknown";

      // Get player's IP address
      String ip = getPlayerIp(playerRef);

      // Create session
      sessionManager.createSession(uuid, hostname, ip);
      debug(
        "Created session for existing player %s (hostname: %s, ip: %s)",
        username,
        hostname,
        ip
      );

      // Send join event to the API
      JoinRequest request = new JoinRequest(
        uuid,
        username,
        hostname,
        ip,
        false
      );

      client.join(
        request,
        new AnalyseCallback<>() {
          @Override
          public void onSuccess(JoinResponse response) {
            sessionManager
              .getSession(uuid)
              .ifPresent(session ->
                session.setSessionId(response.getSessionId())
              );
            debug(
              "Join event sent for existing player %s (sessionId: %s)",
              username,
              response.getSessionId()
            );
          }

          @Override
          public void onError(AnalyseException exception) {
            getLogger()
              .atWarning()
              .log(
                "Failed to send join event for existing player %s: %s",
                username,
                exception.getMessage()
              );
          }
        }
      );
    }
  }

  /**
   * Get a player's IP address
   *
   * @param playerRef The player reference
   * @return The IP address string
   */
  private String getPlayerIp(PlayerRef playerRef) {
    if (playerRef.getPacketHandler() == null) {
      return "unknown";
    }

    SocketAddress address = playerRef
      .getPacketHandler()
      .getChannel()
      .remoteAddress();
    if (!(address instanceof InetSocketAddress socketAddress)) {
      return "unknown";
    }

    String ip = socketAddress.getAddress().getHostAddress();
    return ip != null ? ip : "unknown";
  }

  @Override
  protected void shutdown() {
    getLogger().atInfo().log("Disabling Analyse...");

    // Disable all addons first
    if (addonManager != null) {
      addonManager.disableAddons();
    }

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
      heartbeatTask.cancel(false);
    }

    // Shutdown scheduler
    if (scheduler != null) {
      scheduler.shutdown();
    }

    // Shutdown SDK client
    if (client != null) {
      client.shutdown();
    }

    getLogger().atInfo().log("Analyse disabled");
    super.shutdown();
  }

  /**
   * Log a debug message (only if debug mode is enabled)
   *
   * @param message The message to log
   */
  public void debug(String message) {
    if (pluginConfig != null && pluginConfig.isDebug()) {
      getLogger().atInfo().log("[DEBUG] %s", message);
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
      getLogger().atInfo().log("[DEBUG] %s", String.format(format, args));
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
  public AddonManager getAddonManager() {
    return addonManager;
  }

  @Override
  public boolean isDebugEnabled() {
    return pluginConfig != null && pluginConfig.isDebug();
  }

  @Override
  public void logInfo(String message) {
    getLogger().atInfo().log("%s", message);
  }

  @Override
  public void logWarning(String message) {
    getLogger().atWarning().log("%s", message);
  }

  /**
   * Log an error message
   *
   * @param message The message to log
   */
  public void logError(String message) {
    getLogger().atSevere().log("%s", message);
  }

  /**
   * Log an error message with an exception
   *
   * @param message The message to log
   * @param throwable The exception to log
   */
  public void logError(String message, Throwable throwable) {
    if (throwable != null) {
      getLogger().atSevere().withCause(throwable).log("%s", message);
    } else {
      getLogger().atSevere().log("%s", message);
    }
  }

  @Override
  public String getVersion() {
    return version;
  }

  // ========== Internal Getters ==========

  /**
   * Get the plugin configuration
   *
   * @return The plugin config
   */
  public AnalyseHytaleConfig getPluginConfig() {
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
  public HytaleUpdateChecker getUpdateChecker() {
    return updateChecker;
  }

  /**
   * Get the universe
   *
   * @return The universe
   */
  public Universe getUniverse() {
    return Universe.get();
  }
}
