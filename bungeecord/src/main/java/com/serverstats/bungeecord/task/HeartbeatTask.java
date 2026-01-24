package com.serverstats.bungeecord.task;

import com.serverstats.bungeecord.ServerStatsBungee;
import com.serverstats.bungeecord.listener.PlayerListener;
import com.serverstats.bungeecord.object.session.PlayerSession;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.ServerStatsClient;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.sdk.request.HeartbeatRequest;
import com.serverstats.sdk.request.PlayerInfo;
import com.serverstats.sdk.request.ServerType;
import com.serverstats.sdk.response.HeartbeatResponse;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Sends periodic heartbeats to the API for each configured server
 */
public class HeartbeatTask implements Runnable {

  private final ServerStatsBungee plugin;
  private final Logger logger;
  private final PlayerListener playerListener;

  public HeartbeatTask(ServerStatsBungee plugin, PlayerListener playerListener) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.playerListener = playerListener;
  }

  @Override
  public void run() {
    // Send heartbeat for each configured server
    plugin.getPluginConfig().getServers().keySet().forEach(this::sendHeartbeatForServer);
  }

  /**
   * Send a heartbeat for a specific server
   *
   * @param serverName The server name
   */
  private void sendHeartbeatForServer(String serverName) {
    Optional<ServerStatsClient> clientOpt = playerListener.getClientForServer(serverName);
    if (clientOpt.isEmpty()) {
      return;
    }

    // Get the server info
    ServerInfo serverInfo = plugin.getProxy().getServerInfo(serverName);
    if (serverInfo == null) {
      return;
    }

    ServerStatsClient client = clientOpt.get();

    // Collect player info with hostnames for players on this server
    List<PlayerInfo> onlinePlayers = serverInfo.getPlayers().stream()
        .map(this::createPlayerInfo)
        .toList();

    String instanceId = plugin.getPluginConfig().getInstanceId();
    HeartbeatRequest request = new HeartbeatRequest(instanceId, ServerType.MINECRAFT, onlinePlayers);

    client.heartbeat(request, new ServerStatsCallback<>() {
      @Override
      public void onSuccess(HeartbeatResponse response) {
        plugin.debug("Heartbeat sent for %s (%d players)", serverName, response.getOnlineCount());
      }

      @Override
      public void onError(ServerStatsException exception) {
        logger.warning(String.format("Failed to send heartbeat for %s: %s",
            serverName, exception.getMessage()));
      }
    });
  }

  /**
   * Create player info from a player, including their hostname from session
   *
   * @param player The player
   * @return PlayerInfo with UUID and hostname
   */
  private PlayerInfo createPlayerInfo(ProxiedPlayer player) {
    // Try to get hostname from session
    Optional<PlayerSession> sessionOpt = plugin.getSessionManager().getSession(player.getUniqueId());
    String hostname = sessionOpt.map(PlayerSession::getHostname).orElse(null);
    return new PlayerInfo(player.getUniqueId(), hostname);
  }
}
