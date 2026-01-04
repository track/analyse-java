package net.analyse.velocity.task;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.AnalyseException;
import net.analyse.sdk.request.HeartbeatRequest;
import net.analyse.sdk.response.HeartbeatResponse;
import net.analyse.velocity.AnalyseVelocity;
import net.analyse.velocity.listener.PlayerListener;
import org.slf4j.Logger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    // Collect UUIDs of players on this server
    List<UUID> onlinePlayers = server.getPlayersConnected().stream()
        .map(Player::getUniqueId)
        .toList();

    String instanceId = plugin.getPluginConfig().getInstanceId();
    HeartbeatRequest request = new HeartbeatRequest(instanceId, onlinePlayers);

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
}

