package com.serverstats.hytale.task;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.List;
import java.util.Optional;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.hytale.HytalePlugin;
import com.serverstats.hytale.object.session.PlayerSession;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.ServerStatsClient;
import com.serverstats.sdk.request.HeartbeatRequest;
import com.serverstats.sdk.request.PlayerInfo;
import com.serverstats.sdk.request.ServerType;
import com.serverstats.sdk.response.HeartbeatResponse;

/**
 * Sends periodic heartbeats to the API with online players
 */
public class HeartbeatTask implements Runnable {

  private final HytalePlugin plugin;
  private final ServerStatsClient client;

  public HeartbeatTask(HytalePlugin plugin, ServerStatsClient client) {
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
      new ServerStatsCallback<>() {
        @Override
        public void onSuccess(HeartbeatResponse response) {
          plugin.debug(
            "Heartbeat sent (%d players)",
            response.getOnlineCount()
          );
        }

        @Override
        public void onError(ServerStatsException exception) {
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
