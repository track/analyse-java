package net.analyse.paper;

import lombok.Getter;
import net.analyse.paper.config.AnalysePaperConfig;
import net.analyse.paper.listener.PlayerListener;
import net.analyse.paper.session.SessionManager;
import net.analyse.paper.task.HeartbeatTask;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.config.AnalyseConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Analyse plugin for Paper/Spigot/Bukkit servers
 */
@Getter
public class AnalysePlugin extends JavaPlugin {

  private AnalysePaperConfig pluginConfig;
  private SessionManager sessionManager;
  private AnalyseClient client;
  private BukkitTask heartbeatTask;

  @Override
  public void onEnable() {
    getLogger().info("Initializing Analyse...");

    // Load configuration
    pluginConfig = new AnalysePaperConfig(this);

    // Validate configuration
    if (!pluginConfig.isValid()) {
      getLogger().warning("Invalid configuration! Please set your API key in config.yml");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    // Initialize SDK client
    AnalyseConfig sdkConfig = new AnalyseConfig(pluginConfig.getApiKey());
    client = new AnalyseClient(sdkConfig);

    // Initialize session manager
    sessionManager = new SessionManager();

    // Register player listener
    PlayerListener playerListener = new PlayerListener(this, client);
    getServer().getPluginManager().registerEvents(playerListener, this);

    // Start heartbeat task (every 30 seconds = 600 ticks)
    heartbeatTask = getServer().getScheduler().runTaskTimerAsynchronously(
        this,
        new HeartbeatTask(this, client),
        600L,
        600L
    );

    getLogger().info("Analyse initialized successfully!");
  }

  @Override
  public void onDisable() {
    getLogger().info("Shutting down Analyse...");

    // Cancel heartbeat task
    if (heartbeatTask != null) {
      heartbeatTask.cancel();
    }

    // Shutdown SDK client
    if (client != null) {
      client.shutdown();
    }

    getLogger().info("Analyse shutdown complete");
  }
}
