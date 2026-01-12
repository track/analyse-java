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
import lombok.Getter;
import net.analyse.api.AnalyseProvider;
import net.analyse.api.platform.AnalysePlatform;
import net.analyse.sdk.AnalyseClient;
import net.analyse.velocity.command.AnalyseCommand;
import net.analyse.velocity.config.AnalyseVelocityConfig;
import net.analyse.velocity.listener.PlayerListener;
import net.analyse.velocity.session.SessionManager;
import net.analyse.velocity.task.HeartbeatTask;
import org.slf4j.Logger;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Analyse plugin for Velocity proxy
 */
@Plugin(
    id = "analyse",
    name = "Analyse",
    version = "1.0.0-SNAPSHOT",
    description = "Analytics tracking plugin for Minecraft servers",
    authors = {"VertCode"}
)
@Getter
public class AnalyseVelocity implements AnalysePlatform {

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;

  private AnalyseVelocityConfig pluginConfig;
  private SessionManager sessionManager;
  private PlayerListener playerListener;
  private ScheduledTask heartbeatTask;

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

    // Register with the API provider if a default server is configured
    if (playerListener.getDefaultClient() != null) {
      AnalyseProvider.register(this);
    }

    // Register commands using ACF
    VelocityCommandManager commandManager = new VelocityCommandManager(server, this);
    commandManager.registerCommand(new AnalyseCommand(this));

    // Start heartbeat task (after playerListener is initialized)
    startHeartbeatTask();

    // Initialize sessions for players already online (in case of reload)
    initializeOnlinePlayers();

    logger.info(String.format("Analyse initialized with %d server(s) configured",
        pluginConfig.getServers().size()));
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

  @Override
  public AnalyseClient getClient() {
    return playerListener != null ? playerListener.getDefaultClient() : null;
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
}
