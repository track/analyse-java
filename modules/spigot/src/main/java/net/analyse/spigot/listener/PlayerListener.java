package net.analyse.spigot.listener;

import net.analyse.spigot.AnalysePlugin;
import net.analyse.spigot.manager.SessionManager;
import net.analyse.spigot.object.session.PlayerSession;
import net.analyse.sdk.util.ProtocolVersionUtil;
import net.analyse.spigot.util.SchedulerUtil;
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
import java.lang.reflect.Method;
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
  private AnalyseClient client;

  /**
   * Creates a listener that forwards player lifecycle events using the given plugin and client.
   *
   * @param plugin The Analyse plugin instance
   * @param client The SDK client, or null if not yet initialized
   */
  public PlayerListener(AnalysePlugin plugin, AnalyseClient client) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.sessionManager = plugin.getSessionManager();
    this.client = client;
  }

  /**
   * Set the SDK client. Used when reinitializing after a config reload.
   *
   * @param client The new client instance, or null to disable
   */
  public void setClient(AnalyseClient client) {
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
    if (hostname == null || hostname.trim().isEmpty()) {
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
    if (!sessionOpt.isPresent()) {
      logger.warning(String.format("No session found for player %s on join", username));
      return;
    }

    PlayerSession session = sessionOpt.get();

    // Only send to API if client is available
    if (client != null) {
      // Check if player is a Bedrock player
      boolean isBedrock = plugin.getPluginConfig().isBedrock(username);

      // Convert protocol version to "1.x.y" string for the API (Java clients only; Bedrock may still send "1.?")
      int protocolVersion = getProtocolVersion(player);
      String playerVersion = ProtocolVersionUtil.toVersionString(protocolVersion);

      // Send join event to the API
      JoinRequest request = new JoinRequest(uuid, username, session.getHostname(), session.getIp(),
          isBedrock, playerVersion);

      client.join(request, new AnalyseCallback<JoinResponse>() {
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
    }

    // Process A/B tests for this join
    boolean firstJoin = !player.hasPlayedBefore();
    if (plugin.getABTestManager() != null) {
      plugin.getABTestManager().processJoin(player, firstJoin);
    }

    // Notify admins about updates on join
    if (player.hasPermission("analyse.admin") && plugin.getUpdateChecker() != null) {
      if (plugin.getUpdateChecker().isUpdateAvailable()) {
        // Delay slightly so it appears after other join messages
        // Use entity scheduler for Folia compatibility
        SchedulerUtil.runForEntityDelayed(plugin, player, () -> {
          plugin.getUpdateChecker().sendUpdateMessage(
              player,
              plugin.getUpdateChecker().getCurrentVersion(),
              plugin.getUpdateChecker().getLatestVersion(),
              plugin.getUpdateChecker().getDownloadUrl()
          );
        }, () -> {
          // Player left before message could be sent, do nothing
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
    if (!sessionOpt.isPresent()) {
      return;
    }

    PlayerSession session = sessionOpt.get();
    if (!session.hasActiveSession()) {
      return;
    }

    // Only send to API if client is available
    if (client == null) {
      return;
    }

    // Send leave event to the API
    LeaveRequest request = new LeaveRequest(session.getSessionId());

    client.leave(request, new AnalyseCallback<LeaveResponse>() {
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
   * Resolves the player's protocol version via reflection when {@code Player#getProtocolVersion()}
   * exists (e.g. Paper); returns -1 on Spigot 1.8 or if reflection fails.
   *
   * @param player The connected player
   * @return The protocol version, or -1 if not available
   */
  private int getProtocolVersion(Player player) {
    try {
      Method method = player.getClass().getMethod("getProtocolVersion");
      Object result = method.invoke(player);
      if (result instanceof Integer) {
        return (Integer) result;
      }

      return -1;
    } catch (Exception e) {
      return -1;
    }
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
