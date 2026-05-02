package net.analyse.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.api.exception.AnalyseException;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.response.EventResponse;
import net.analyse.velocity.AnalyseVelocity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens for common player actions on the proxy and sends built-in analytics events
 */
public class ActivityListener {

  private final AnalyseVelocity plugin;

  public ActivityListener(AnalyseVelocity plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onCommand(CommandExecuteEvent event) {
    if (!isEnabled("command")) {
      return;
    }

    if (!(event.getCommandSource() instanceof Player player)) {
      return;
    }

    UUID uuid = player.getUniqueId();
    String username = player.getUsername();

    String rawCommand = event.getCommand();
    String command = rawCommand.split(" ")[0];

    // Send to the player's current server client
    player.getCurrentServer().ifPresent(serverConnection -> {
      String serverName = serverConnection.getServerInfo().getName();
      sendEvent(serverName, "plugin.command", uuid, username, Map.of(
          "command", command,
          "rawCommand", rawCommand
      ));
    });
  }

  @Subscribe
  public void onServerConnected(ServerConnectedEvent event) {
    if (!isEnabled("serverSwitch")) {
      return;
    }

    // Only track switches, not initial connections
    Optional<RegisteredServer> previousServer = event.getPreviousServer();
    if (previousServer.isEmpty()) {
      return;
    }

    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getUsername();
    String from = previousServer.get().getServerInfo().getName();
    String to = event.getServer().getServerInfo().getName();

    sendEvent(to, "plugin.server_switch", uuid, username, Map.of(
        "from", from,
        "to", to
    ));
  }

  /**
   * Send an event via the per-server SDK client
   *
   * @param serverName The server to send the event to
   * @param eventName The event name
   * @param playerUuid The player's UUID
   * @param playerUsername The player's username
   * @param data The event properties
   */
  private void sendEvent(String serverName, String eventName, UUID playerUuid, String playerUsername,
      Map<String, Object> data) {
    Optional<AnalyseClient> clientOpt = plugin.getPlayerListener().getClientForServer(serverName);
    if (clientOpt.isEmpty()) {
      return;
    }

    AnalyseClient client = clientOpt.get();
    String instanceId = plugin.getPluginConfig().getInstanceId();
    EventRequest request = new EventRequest(eventName, playerUuid, playerUsername, new HashMap<>(data), null, instanceId);

    client.trackEvent(request, new AnalyseCallback<>() {
      @Override
      public void onSuccess(EventResponse response) {
        plugin.debug("Activity event '%s' sent for %s on %s", eventName, playerUsername, serverName);
      }

      @Override
      public void onError(AnalyseException exception) {
        plugin.getLogger().warn(String.format("Failed to send activity event '%s' for %s: %s",
            eventName, playerUsername, exception.getMessage()));
      }
    });
  }

  /**
   * Check if a built-in event type is enabled
   *
   * @param key The event config key
   * @return true if the event should be tracked
   */
  private boolean isEnabled(String key) {
    return plugin.getPluginConfig() != null && plugin.getPluginConfig().isEventEnabled(key);
  }
}
