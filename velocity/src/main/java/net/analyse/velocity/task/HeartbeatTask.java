package net.analyse.velocity.task;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.api.exception.AnalyseException;
import net.analyse.sdk.request.HeartbeatRequest;
import net.analyse.sdk.request.PlayerInfo;
import net.analyse.sdk.request.ServerType;
import net.analyse.sdk.response.HeartbeatResponse;
import net.analyse.velocity.AnalyseVelocity;
import net.analyse.velocity.listener.PlayerListener;
import net.analyse.velocity.object.session.PlayerSession;
import org.slf4j.Logger;
import java.util.List;
import java.util.Optional;

/**
 * Sends periodic heartbeats to the API for each configured server
 */
public class HeartbeatTask implements Runnable {

  private final AnalyseVelocity plugin;
  private final Logger logger;
  private final PlayerListener playerListener;

  public HeartbeatTask(AnalyseVelocity plugin, PlayerListener playerListener) {
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

    // Get the registered server
    Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(serverName);
    if (serverOpt.isEmpty()) {
      return;
    }

    RegisteredServer server = serverOpt.get();
    AnalyseClient client = clientOpt.get();

    // Collect player info with hostnames for players on this server
    List<PlayerInfo> onlinePlayers = server.getPlayersConnected().stream()
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
        logger.warn(String.format("Failed to send heartbeat for %s: %s",
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
  private PlayerInfo createPlayerInfo(Player player) {
    // Try to get hostname from session
    Optional<PlayerSession> sessionOpt = plugin.getSessionManager().getSession(player.getUniqueId());
    String hostname = sessionOpt.map(PlayerSession::getHostname).orElse(null);
    return new PlayerInfo(player.getUniqueId(), hostname);
  }
}
