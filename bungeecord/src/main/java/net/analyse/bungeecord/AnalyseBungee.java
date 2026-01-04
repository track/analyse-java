package net.analyse.bungeecord;

import lombok.Getter;
import net.analyse.bungeecord.config.AnalyseBungeeConfig;
import net.analyse.bungeecord.listener.PlayerListener;
import net.analyse.bungeecord.session.SessionManager;
import net.analyse.bungeecord.task.HeartbeatTask;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Analyse plugin for BungeeCord proxy
 */
@Getter
public class AnalyseBungee extends Plugin {

  private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

  private AnalyseBungeeConfig pluginConfig;
  private SessionManager sessionManager;
  private PlayerListener playerListener;
  private ScheduledTask heartbeatTask;

  @Override
  public void onEnable() {
    // Test
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

    // Start heartbeat task (after playerListener is initialized)
    startHeartbeatTask();

    getLogger().info(String.format("Analyse initialized with %d server(s) configured",
        pluginConfig.getServers().size()));
  }

  @Override
  public void onDisable() {
    getLogger().info("Shutting down Analyse...");

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
}
