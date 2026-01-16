package net.analyse.hytale.task;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.List;
import java.util.Optional;
import net.analyse.api.exception.AnalyseException;
import net.analyse.hytale.HytalePlugin;
import net.analyse.hytale.object.session.PlayerSession;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.request.HeartbeatRequest;
import net.analyse.sdk.request.PlayerInfo;
import net.analyse.sdk.request.ServerType;
import net.analyse.sdk.response.HeartbeatResponse;

/**
 * Sends periodic heartbeats to the API with online players
 */
public class HeartbeatTask implements Runnable {

  private final HytalePlugin plugin;
  private final AnalyseClient client;

  public HeartbeatTask(HytalePlugin plugin, AnalyseClient client) {
    this.plugin = plugin;
    this.client = client;
  }

  @Override
  public void run() {
    // Collect player info with hostnames for all online players
    Universe universe = plugin.getUniverse();
    List<PlayerInfo> onlinePlayers = universe
      .getPlayers()
      .stream()
      .map(this::createPlayerInfo)
      .toList();

    String instanceId = plugin.getPluginConfig().getInstanceId();
    HeartbeatRequest request = new HeartbeatRequest(instanceId, ServerType.HYTALE, onlinePlayers);

    client.heartbeat(
      request,
      new AnalyseCallback<>() {
        @Override
        public void onSuccess(HeartbeatResponse response) {
          plugin.debug(
            "Heartbeat sent (%d players)",
            response.getOnlineCount()
          );
        }

        @Override
        public void onError(AnalyseException exception) {
          plugin.logWarning(
            String.format(
              "Failed to send heartbeat: %s",
              exception.getMessage()
            )
          );
        }
      }
    );
  }

  /**
   * Create player info from a player, inclsuding their hostname from session
   *
   * @param playerRef The player reference
   * @return PlayerInfo with UUID and hostname
   */
  private PlayerInfo createPlayerInfo(PlayerRef playerRef) {
    // Try to get hostname from session
    Optional<PlayerSession> sessionOpt = plugin
      .getSessionManager()
      .getSession(playerRef.getUuid());
    String hostname = sessionOpt.map(PlayerSession::getHostname).orElse(null);
    return new PlayerInfo(playerRef.getUuid(), hostname);
  }
}
