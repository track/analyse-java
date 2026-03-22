package net.analyse.spigot.task;

import net.analyse.api.Analyse;
import net.analyse.spigot.AnalysePlugin;
import net.analyse.spigot.object.session.PlayerSession;
import net.analyse.spigot.util.SchedulerUtil;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.api.exception.AnalyseException;
import net.analyse.sdk.request.HeartbeatRequest;
import net.analyse.sdk.request.PlayerInfo;
import net.analyse.sdk.request.ServerType;
import net.analyse.sdk.response.HeartbeatResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Sends periodic heartbeats to the API with online players.
 * Folia-compatible: collects player data on sync thread, sends request async.
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
    // Collect player data on the global region thread (required for Folia)
    SchedulerUtil.runSync(plugin, this::collectAndSendHeartbeat);
  }

  /**
   * Collect player data and send heartbeat.
   * Must be called from the main/global thread.
   */
  private void collectAndSendHeartbeat() {
    // Collect player info with hostnames for all online players
    List<PlayerInfo> onlinePlayers = new ArrayList<PlayerInfo>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      onlinePlayers.add(createPlayerInfo(player));
    }

    String instanceId = plugin.getPluginConfig().getInstanceId();
    HeartbeatRequest request = new HeartbeatRequest(instanceId, ServerType.MINECRAFT, onlinePlayers);

    // Send the API request asynchronously
    SchedulerUtil.runAsync(plugin, () -> {
      client.heartbeat(request, new AnalyseCallback<HeartbeatResponse>() {
        @Override
        public void onSuccess(HeartbeatResponse response) {
          Analyse.setConnectionStatus(true, null);
          plugin.debug("Heartbeat sent (%d players)", response.getOnlineCount());
        }

        @Override
        public void onError(AnalyseException exception) {
          Analyse.setConnectionStatus(false, exception.getMessage());
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
