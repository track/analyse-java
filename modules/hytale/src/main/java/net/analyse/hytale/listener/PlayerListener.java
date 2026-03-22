package net.analyse.hytale.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.HostAddress;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
import io.netty.handler.codec.quic.QuicStreamChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens for player events and sends analytics data
 */
public class PlayerListener {

  private final HytalePlugin plugin;
  private final SessionManager sessionManager;
  private final AnalyseClient client;

  public PlayerListener(HytalePlugin plugin, AnalyseClient client) {
    this.plugin = plugin;
    this.sessionManager = plugin.getSessionManager();
    this.client = client;
  }

  /**
   * Handle player ready event
   *
   * @param event The ready event
   */
  public void onPlayerReady(PlayerReadyEvent event) {
    Ref<EntityStore> ref = event.getPlayerRef();
    if (!ref.isValid()) {
      return;
    }

    Store<EntityStore> store = ref.getStore();
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
    if (playerRef == null) {
      return;
    }

    UUID playerUuid = playerRef.getUuid();
    String playerName = playerRef.getUsername();
    String ip = getPlayerIp(playerRef);
    String hostname = getPlayerHostname(playerRef);

    sessionManager.createSession(playerUuid, hostname, ip);
    plugin.debug("Created session for %s (hostname: %s, ip: %s)", playerName, hostname, ip);

    // Send join event to the API
    JoinRequest request = new JoinRequest(playerUuid, playerName, hostname, ip, false);
    client.join(request, new AnalyseCallback<>() {
      @Override
      public void onSuccess(JoinResponse response) {
        sessionManager.getSession(playerUuid).ifPresent(session -> session.setSessionId(response.getSessionId()));
        plugin.debug("Join event sent for %s (sessionId: %s)", playerName, response.getSessionId());
      }

      @Override
      public void onError(AnalyseException exception) {
        plugin.logWarning(String.format("Failed to send join event for %s: %s", playerName, exception.getMessage()));
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

    client.leave(
      request,
      new AnalyseCallback<>() {
        @Override
        public void onSuccess(LeaveResponse response) {
          plugin.debug(
            "Leave event sent for %s (duration: %ds)",
            username,
            response.getDuration()
          );
        }

        @Override
        public void onError(AnalyseException exception) {
          plugin.logWarning(
            String.format(
              "Failed to send leave event for %s: %s",
              username,
              exception.getMessage()
            )
          );
        }
      }
    );
  }  

  /**
   * Get the player's IP address from the player reference
   *
   * @param playerRef The player reference
   * @return The IP address string
   */
  private String getPlayerIp(PlayerRef playerRef) {
    try {
      PacketHandler packetHandler = playerRef.getPacketHandler();
      if (packetHandler == null) {
        plugin.debug(
          String.format(
            "Failed to get player IP for %s: %s",
            playerRef.getUsername(),
            "packetHandler is null"
          )
        );
        return "unknown";
      }

      String ip = null;
      SocketAddress sa = (packetHandler.getChannel() instanceof
          QuicStreamChannel quic)
        ? quic.parent().remoteSocketAddress()
        : packetHandler.getChannel().remoteAddress();
      if (sa instanceof InetSocketAddress inet) {
        ip = inet.getAddress().getHostAddress();
      }

      return ip != null ? ip : "unknown";
    } catch (Exception e) {
      plugin.debug(
        String.format(
          "Failed to get player IP for %s: %s",
          playerRef.getUsername(),
          e.getMessage()
        )
      );
      return "unknown";
    }
  }

  /**
   * Get the player's hostname from the player reference
   *
   * @param playerRef The player reference
   * @return The IP address string
   */
  private String getPlayerHostname(PlayerRef playerRef) {
    try {
      PacketHandler packetHandler = playerRef.getPacketHandler();
      if (packetHandler == null) {
        plugin.debug(
          String.format(
            "Failed to get player hostname for %s: %s",
            playerRef.getUsername(),
            "packetHandler is null"
          )
        );
        return "unknown";
      }

      PlayerAuthentication authentication = packetHandler.getAuth();
      if (authentication == null) {
        plugin.debug(
          String.format(
            "Failed to get player hostname for %s: %s",
            playerRef.getUsername(),
            "authentication is null"
          )
        );
        return "unknown";
      }

      HostAddress referralSource = authentication.getReferralSource();
      if (referralSource == null) {
        plugin.debug(
          String.format(
            "Failed to get player hostname for %s: %s",
            playerRef.getUsername(),
            "referralSource is null"
          )
        );
        return "unknown";
      }

      return referralSource.host != null ? referralSource.host : "unknown";
    } catch (Exception e) {
      plugin.debug(
        String.format(
          "Failed to get player hostname for %s: %s",
          playerRef.getUsername(),
          e.getMessage()
        )
      );
      return "unknown";
    }
  }
}
