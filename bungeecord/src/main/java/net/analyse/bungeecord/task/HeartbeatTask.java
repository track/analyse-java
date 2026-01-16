package net.analyse.bungeecord.task;

import net.analyse.bungeecord.AnalyseBungee;
import net.analyse.bungeecord.listener.PlayerListener;
import net.analyse.bungeecord.object.session.PlayerSession;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.api.exception.AnalyseException;
import net.analyse.sdk.request.HeartbeatRequest;
import net.analyse.sdk.request.PlayerInfo;
import net.analyse.sdk.request.ServerType;
import net.analyse.sdk.response.HeartbeatResponse;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Sends periodic heartbeats to the API for each configured server
 */
public class HeartbeatTask implements Runnable {

  private final AnalyseBungee plugin;
  private final Logger logger;
  private final PlayerListener playerListener;

  public HeartbeatTask(AnalyseBungee plugin, PlayerListener playerListener) {
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
    Optional<AnalyseClient> clientOpt = playerListener.getClientForServer(serverName);
    if (clientOpt.isEmpty()) {
      return;
    }

    // Get the server info
    ServerInfo serverInfo = plugin.getProxy().getServerInfo(serverName);
    if (serverInfo == null) {
      return;
    }

    AnalyseClient client = clientOpt.get();

    // Collect player info with hostnames for players on this server
    List<PlayerInfo> onlinePlayers = serverInfo.getPlayers().stream()
        .map(this::createPlayerInfo)
        .toList();

    String instanceId = plugin.getPluginConfig().getInstanceId();
    HeartbeatRequest request = new HeartbeatRequest(instanceId, ServerType.MINECRAFT, onlinePlayers);

    client.heartbeat(request, new AnalyseCallback<>() {
      @Override
      public void onSuccess(HeartbeatResponse response) {
        plugin.debug("Heartbeat sent for %s (%d players)", serverName, response.getOnlineCount());
      }

      @Override
      public void onError(AnalyseException exception) {
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
