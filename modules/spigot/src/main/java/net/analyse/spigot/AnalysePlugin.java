package net.analyse.spigot;

import co.aikar.commands.BukkitCommandManager;
import net.analyse.api.Analyse;
import net.analyse.api.AnalyseProvider;
import net.analyse.api.BuildConstants;
import net.analyse.api.addon.AddonManager;
import net.analyse.api.exception.AnalyseException;
import net.analyse.api.object.builder.EventBuilder;
import net.analyse.api.platform.AnalysePlatform;
import net.analyse.spigot.addon.SpigotAddonManager;
import net.analyse.spigot.manager.ABTestManager;
import net.analyse.spigot.command.AnalyseCommand;
import net.analyse.spigot.config.AnalyseSpigotConfig;
import net.analyse.spigot.listener.ActivityListener;
import net.analyse.spigot.listener.PlayerListener;
import net.analyse.spigot.manager.SessionManager;
import net.analyse.spigot.task.HeartbeatTask;
import net.analyse.spigot.update.SpigotUpdateChecker;
import net.analyse.spigot.util.MessageUtil;
import net.analyse.spigot.util.SchedulerUtil;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.config.AnalyseConfig;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.response.EventResponse;
import net.analyse.sdk.response.JoinResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Analyse plugin for Paper/Spigot/Bukkit servers
 */
public class AnalysePlugin extends JavaPlugin implements AnalysePlatform {

  private AnalyseSpigotConfig pluginConfig;
  private SessionManager sessionManager;
  private ABTestManager abTestManager;
  private SpigotAddonManager addonManager;
  private AnalyseClient client;
  private SchedulerUtil.CancellableTask heartbeatTask;
  private SpigotUpdateChecker updateChecker;
  private PlayerListener playerListener;
  private boolean initialized = false;

  @Override
  public void onLoad() {
    getLogger().info("Loading Analyse...");

    // Migrate old "ServerStats" data folder if it exists
    migrateDataFolder();

    // Load configuration
    pluginConfig = new AnalyseSpigotConfig(this);

    // Validate configuration
    if (!pluginConfig.isValid()) {
      getLogger().warning("Invalid configuration! Please set your API key in config.yml and run /analyse reload");
    } else {
      getLogger().info("Configuration loaded successfully!");
    }
  }

  /**
   * Migrate the data folder from the old "ServerStats" name to "Analyse".
   * Automatically renames plugins/ServerStats to plugins/Analyse if it exists.
   */
  private void migrateDataFolder() {
    File oldFolder = new File(getDataFolder().getParentFile(), "ServerStats");
    if (oldFolder.exists() && oldFolder.isDirectory() && !getDataFolder().exists()) {
      if (oldFolder.renameTo(getDataFolder())) {
        getLogger().info("Migrated data folder from ServerStats to Analyse");
      } else {
        getLogger().warning("Failed to migrate data folder from ServerStats to Analyse. Please rename it manually.");
      }
    }
  }

  @Override
  public void onEnable() {
    getLogger().info("Enabling Analyse...");

    // Initialize the cross-version messaging system
    MessageUtil.init(this);

    // Register commands using ACF (always available so reload command works)
    BukkitCommandManager commandManager = new BukkitCommandManager(this);
    commandManager.registerCommand(new AnalyseCommand(this));

    // Initialize session manager (always needed)
    sessionManager = new SessionManager();

    // Register player listener (always needed, but it checks if client is available)
    playerListener = new PlayerListener(this, null);
    getServer().getPluginManager().registerEvents(playerListener, this);

    // Register activity listener for built-in event tracking
    getServer().getPluginManager().registerEvents(new ActivityListener(this), this);

    // Try to initialize if config is valid
    if (pluginConfig.isValid()) {
      initializeAnalyse();
    } else {
      getLogger().warning("Analyse is running in limited mode. Set a valid API key and run /analyse reload");
    }
  }

  /**
   * Initialize or reinitialize the Analyse functionality.
   * Called on startup if config is valid, and on reload when config becomes valid.
   */
  public void initializeAnalyse() {
    // Shutdown existing components if reinitializing
    shutdownAnalyse();

    // Initialize SDK client
    AnalyseConfig sdkConfig = new AnalyseConfig(pluginConfig.getApiKey(), pluginConfig.isDevelopment());
    client = new AnalyseClient(sdkConfig);

    // Update the player listener with the new client
    playerListener.setClient(client);

    // Register with the API provider so other plugins can use Analyse.get()
    AnalyseProvider.register(this);

    // Set up the event sender for Analyse.trackEvent()
    Analyse.setEventSender(this::sendEvent);

    // Start heartbeat task (every 30 seconds = 600 ticks)
    // Uses sync timer because HeartbeatTask collects player data on main thread, then sends async
    // Initial delay of 1 tick to run immediately after initialization
    heartbeatTask = SchedulerUtil.runSyncTimer(
        this,
        new HeartbeatTask(this, client),
        1L,
        600L
    );

    // Initialize A/B test manager
    abTestManager = new ABTestManager(this);
    abTestManager.start();

    // Initialize update checker
    updateChecker = new SpigotUpdateChecker(this, BuildConstants.VERSION);
    updateChecker.start();

    // Initialize sessions for players already online (in case of reload)
    initializeOnlinePlayers();

    // Initialize addon manager and load addons
    addonManager = new SpigotAddonManager(this);
    addonManager.loadAddons();
    addonManager.enableAddons();

    initialized = true;
    getLogger().info("Analyse initialized successfully!");
  }

  /**
   * Shutdown Analyse functionality without disabling the plugin.
   * Called before reinitializing or when the plugin is disabled.
   */
  private void shutdownAnalyse() {
    if (!initialized) {
      return;
    }

    // Disable all addons first
    if (addonManager != null) {
      addonManager.disableAddons();
      addonManager = null;
    }

    // Unregister from the API provider
    AnalyseProvider.unregister();

    // Clear the event sender
    Analyse.setEventSender(null);

    // Stop A/B test manager
    if (abTestManager != null) {
      abTestManager.stop();
      abTestManager = null;
    }

    // Stop update checker
    if (updateChecker != null) {
      updateChecker.stop();
      updateChecker = null;
    }

    // Cancel heartbeat task
    if (heartbeatTask != null) {
      heartbeatTask.cancel();
      heartbeatTask = null;
    }

    // Shutdown SDK client
    if (client != null) {
      client.shutdown();
      client = null;
    }

    // Clear client from player listener
    if (playerListener != null) {
      playerListener.setClient(null);
    }

    initialized = false;
  }

  /**
   * Check if Analyse is fully initialized and functional
   *
   * @return true if initialized with a valid API key
   */
  public boolean isAnalyseReady() {
    return initialized;
  }

  /**
   * Send an event via the SDK.
   * Thread-safe: can be called from any thread.
   *
   * @param event The event builder
   * @param callback Optional callback for result
   */
  private void sendEvent(EventBuilder event, Consumer<Boolean> callback) {
    // Can't send events if not initialized
    if (client == null) {
      if (callback != null) {
        callback.accept(false);
      }
      return;
    }

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
        event.getValue(),
        pluginConfig.getInstanceId()
    );

    client.trackEvent(request, new AnalyseCallback<EventResponse>() {
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
      // Uses reflection since getVirtualHost() is Paper-only
      String hostname = "unknown";
      try {
        java.lang.reflect.Method method = player.getClass().getMethod("getVirtualHost");
        InetSocketAddress virtualHost = (InetSocketAddress) method.invoke(player);
        if (virtualHost != null) {
          hostname = virtualHost.getHostName();
        }
      } catch (Exception ignored) {
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

      client.join(request, new AnalyseCallback<JoinResponse>() {
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
    shutdownAnalyse();
    MessageUtil.close();
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
  public AnalyseSpigotConfig getPluginConfig() {
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
  public SpigotUpdateChecker getUpdateChecker() {
    return updateChecker;
  }
}
