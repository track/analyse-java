package net.analyse.paper.task;

import net.analyse.paper.AnalysePlugin;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.AnalyseException;
import net.analyse.sdk.request.HeartbeatRequest;
import net.analyse.sdk.response.HeartbeatResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Sends periodic heartbeats to the API with online players
 */
public class HeartbeatTask implements Runnable {

  private final AnalysePlugin plugin;
  private final Logger logger;
  private final AnalyseClient client;

  public HeartbeatTask(AnalysePlugin plugin, AnalyseClient client) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.client = client;
  }

  @Override
  public void run() {
    // Collect UUIDs of all online players
    List<UUID> onlinePlayers = Bukkit.getOnlinePlayers().stream()
        .map(Player::getUniqueId)
        .toList();

    HeartbeatRequest request = new HeartbeatRequest(onlinePlayers);

    client.heartbeat(request, new AnalyseCallback<>() {
      @Override
      public void onSuccess(HeartbeatResponse response) {
        logger.fine(String.format("Heartbeat sent (%d players)", response.getOnlineCount()));
      }

      @Override
      public void onError(AnalyseException exception) {
        logger.warning(String.format("Failed to send heartbeat: %s", exception.getMessage()));
      }
    });
  }
}

