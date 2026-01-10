package net.analyse.paper;

import lombok.Getter;
import net.analyse.paper.config.AnalysePaperConfig;
import net.analyse.paper.listener.PlayerListener;
import net.analyse.paper.session.SessionManager;
import net.analyse.paper.task.HeartbeatTask;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.AnalyseException;
import net.analyse.sdk.config.AnalyseConfig;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.response.JoinResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Analyse plugin for Paper/Spigot/Bukkit servers
 */
@Getter
public class AnalysePlugin extends JavaPlugin {

  private AnalysePaperConfig pluginConfig;
  private SessionManager sessionManager;
  private AnalyseClient client;
  private BukkitTask heartbeatTask;
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

    // Initialize sessions for players already online (in case of reload)
    initializeOnlinePlayers();

    getLogger().info("Analyse enabled successfully!");
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
}
