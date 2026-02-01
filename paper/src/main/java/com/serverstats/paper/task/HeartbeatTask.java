package com.serverstats.paper.task;

import com.serverstats.paper.ServerStatsPlugin;
import com.serverstats.paper.object.session.PlayerSession;
import com.serverstats.paper.util.SchedulerUtil;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.ServerStatsClient;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.sdk.request.HeartbeatRequest;
import com.serverstats.sdk.request.PlayerInfo;
import com.serverstats.sdk.request.ServerType;
import com.serverstats.sdk.response.HeartbeatResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Sends periodic heartbeats to the API with online players.
 * Folia-compatible: collects player data on sync thread, sends request async.
 */
public class HeartbeatTask implements Runnable {

  private final ServerStatsPlugin plugin;
  private final Logger logger;
  private final ServerStatsClient client;

  public HeartbeatTask(ServerStatsPlugin plugin, ServerStatsClient client) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.client = client;
  }

  @Override
  public void run() {
    // Collect player data on the global region thread (required for Folia)
    SchedulerUtil.runSync(plugin, this::collectAndSendHeartbeat);
  }

  /**
   * Collect player data and send heartbeat.
   * Must be called from the main/global thread.
   */
  private void collectAndSendHeartbeat() {
    // Collect player info with hostnames for all online players
    List<PlayerInfo> onlinePlayers = Bukkit.getOnlinePlayers().stream()
        .map(this::createPlayerInfo)
        .toList();

    String instanceId = plugin.getPluginConfig().getInstanceId();
    HeartbeatRequest request = new HeartbeatRequest(instanceId, ServerType.MINECRAFT, onlinePlayers);

    // Send the API request asynchronously
    SchedulerUtil.runAsync(plugin, () -> {
      client.heartbeat(request, new ServerStatsCallback<>() {
        @Override
        public void onSuccess(HeartbeatResponse response) {
          plugin.debug("Heartbeat sent (%d players)", response.getOnlineCount());
        }

        @Override
        public void onError(ServerStatsException exception) {
          logger.warning(String.format("Failed to send heartbeat: %s", exception.getMessage()));
        }
      });
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
