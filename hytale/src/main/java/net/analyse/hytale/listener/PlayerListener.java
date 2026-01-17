package net.analyse.hytale.listener;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import net.analyse.api.exception.AnalyseException;
import net.analyse.hytale.HytalePlugin;
import net.analyse.hytale.manager.SessionManager;
import net.analyse.hytale.object.session.PlayerSession;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.request.LeaveRequest;
import net.analyse.sdk.response.JoinResponse;
import net.analyse.sdk.response.LeaveResponse;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens for player events and sends analytics data
 */
public class PlayerListener {

  private final HytalePlugin plugin;
  private final SessionManager sessionManager;
  private final AnalyseClient client;

  private final Map<UUID, String> hostnameMap = new HashMap<>();

  public PlayerListener(HytalePlugin plugin, AnalyseClient client) {
    this.plugin = plugin;
    this.sessionManager = plugin.getSessionManager();
    this.client = client;
  }

  public void onPlayerSetupConnect(PlayerSetupConnectEvent event) {
    UUID uuid = event.getUuid();
    String hostname = event.getReferralSource().host;

    hostnameMap.put(uuid, hostname);

    plugin.debug("Hostname set for %s: %s", uuid, hostname);
  }

  /**
   * Handle player connect event
   *
   * @param event The connect event
   */
  public void onPlayerConnect(PlayerConnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    UUID uuid = playerRef.getUuid();
    String username = playerRef.getUsername();

    // Get player's IP address
    String ip = getPlayerIp(playerRef);

    // Get hostname (Hytale doesn't seem to expose this yet, use unknown)
    String hostname = hostnameMap.getOrDefault(uuid, "unknown");

    // Create session for this player
    sessionManager.createSession(uuid, hostname, ip);

    plugin.debug("Created session for %s (hostname: %s, ip: %s)", username, hostname, ip);

    // Send join event to the API
    JoinRequest request = new JoinRequest(uuid, username, hostname, ip, false);

    client.join(request, new AnalyseCallback<>() {
      @Override
      public void onSuccess(JoinResponse response) {
        sessionManager.getSession(uuid).ifPresent(session -> session.setSessionId(response.getSessionId()));
        plugin.debug("Join event sent for %s (sessionId: %s)", username, response.getSessionId());
      }

      @Override
      public void onError(AnalyseException exception) {
        plugin.logWarning(String.format("Failed to send join event for %s: %s", username, exception.getMessage()));
      }
    });
  }

  /**
   * Handle player disconnect event
   *
   * @param event The disconnect event
   */
  public void onPlayerDisconnect(PlayerDisconnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    UUID uuid = playerRef.getUuid();
    String username = playerRef.getUsername();

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
        plugin.logWarning(String.format("Failed to send leave event for %s: %s", username, exception.getMessage()));
      }
    });
  }

  /**
   * Get the player's IP address from the player reference
   *
   * @param playerRef The player reference
   * @return The IP address string
   */
  private String getPlayerIp(PlayerRef playerRef) {
    PacketHandler packetHandler = playerRef.getPacketHandler();
    if (packetHandler == null) {
      return "unknown";
    }

    SocketAddress address = packetHandler.getChannel().remoteAddress();
    if (!(address instanceof InetSocketAddress socketAddress)) {
      return "unknown";
    }

    String ip = socketAddress.getAddress().getHostAddress();
    return ip != null ? ip : "unknown";
  }
}
