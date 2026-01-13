package net.analyse.paper.listener;

import net.analyse.paper.AnalysePlugin;
import net.analyse.paper.manager.SessionManager;
import net.analyse.paper.object.session.PlayerSession;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.api.exception.AnalyseException;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.request.LeaveRequest;
import net.analyse.sdk.response.JoinResponse;
import net.analyse.sdk.response.LeaveResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Listens for player events and sends analytics data
 */
public class PlayerListener implements Listener {

  private final AnalysePlugin plugin;
  private final Logger logger;
  private final SessionManager sessionManager;
  private final AnalyseClient client;

  public PlayerListener(AnalysePlugin plugin, AnalyseClient client) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.sessionManager = plugin.getSessionManager();
    this.client = client;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerLogin(PlayerLoginEvent event) {
    if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
      return;
    }

    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    // Get hostname from the connection
    String hostname = event.getHostname();
    if (hostname == null || hostname.isBlank()) {
      hostname = "unknown";
    }

    // Remove port from hostname if present
    if (hostname.contains(":")) {
      hostname = hostname.split(":")[0];
    }

    // Get player's IP address
    String ip = getPlayerIp(event);

    // Create session for this player
    sessionManager.createSession(uuid, hostname, ip);
    plugin.debug("Created session for %s (hostname: %s, ip: %s)",
        player.getName(), hostname, ip);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getName();

    Optional<PlayerSession> sessionOpt = sessionManager.getSession(uuid);
    if (sessionOpt.isEmpty()) {
      logger.warning(String.format("No session found for player %s on join", username));
      return;
    }

    PlayerSession session = sessionOpt.get();

    // Check if player is a Bedrock player
    boolean isBedrock = plugin.getPluginConfig().isBedrock(username);

    // Send join event to the API
    JoinRequest request = new JoinRequest(uuid, username, session.getHostname(), session.getIp(), isBedrock);

    client.join(request, new AnalyseCallback<>() {
      @Override
      public void onSuccess(JoinResponse response) {
        session.setSessionId(response.getSessionId());
        plugin.debug("Join event sent for %s (sessionId: %s, bedrock: %s)",
            username, response.getSessionId(), isBedrock);
      }

      @Override
      public void onError(AnalyseException exception) {
        logger.warning(String.format("Failed to send join event for %s: %s",
            username, exception.getMessage()));
      }
    });

    // Process A/B tests for this join
    boolean firstJoin = !player.hasPlayedBefore();
    if (plugin.getABTestManager() != null) {
      plugin.getABTestManager().processJoin(player, firstJoin);
    }

    // Notify admins about updates on join
    if (player.hasPermission("analyse.admin") && plugin.getUpdateChecker() != null) {
      if (plugin.getUpdateChecker().isUpdateAvailable()) {
        // Delay slightly so it appears after other join messages
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
          plugin.getUpdateChecker().sendUpdateMessage(
              player,
              plugin.getUpdateChecker().getCurrentVersion(),
              plugin.getUpdateChecker().getLatestVersion(),
              plugin.getUpdateChecker().getDownloadUrl()
          );
        }, 40L);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getName();

    Optional<PlayerSession> sessionOpt = sessionManager.removeSession(uuid);
    if (sessionOpt.isEmpty()) {
      return;
    }

    PlayerSession session = sessionOpt.get();
    if (!session.hasActiveSession()) {
      return;
    }

    // Send leave event to the API
    LeaveRequest request = new LeaveRequest(session.getSessionId());

    client.leave(request, new AnalyseCallback<>() {
      @Override
      public void onSuccess(LeaveResponse response) {
        plugin.debug("Leave event sent for %s (duration: %ds)", username, response.getDuration());
      }

      @Override
      public void onError(AnalyseException exception) {
        logger.warning(String.format("Failed to send leave event for %s: %s",
            username, exception.getMessage()));
      }
    });
  }

  /**
   * Get the player's IP address from the login event
   *
   * @param event The login event
   * @return The IP address string
   */
  private String getPlayerIp(PlayerLoginEvent event) {
    // Try to get from event.getAddress() first
    if (event.getAddress() != null) {
      return event.getAddress().getHostAddress();
    }

    // Fall back to getRealAddress()
    java.net.InetAddress realAddress = event.getRealAddress();
    if (realAddress != null) {
      return realAddress.getHostAddress();
    }

    return "unknown";
  }
}
