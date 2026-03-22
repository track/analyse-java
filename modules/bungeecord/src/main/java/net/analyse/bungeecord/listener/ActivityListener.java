package net.analyse.bungeecord.listener;

import net.analyse.bungeecord.AnalyseBungee;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.api.exception.AnalyseException;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.response.EventResponse;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens for common player actions on the proxy and sends built-in analytics events
 */
public class ActivityListener implements Listener {

  private final AnalyseBungee plugin;

  public ActivityListener(AnalyseBungee plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onChat(ChatEvent event) {
    if (event.isCancelled()) {
      return;
    }

    if (!(event.getSender() instanceof ProxiedPlayer)) {
      return;
    }

    ProxiedPlayer player = (ProxiedPlayer) event.getSender();
    UUID uuid = player.getUniqueId();
    String username = player.getName();
    String message = event.getMessage();

    // Determine current server for routing the event
    Server server = player.getServer();
    if (server == null) {
      return;
    }

    String serverName = server.getInfo().getName();

    if (event.isCommand()) {
      if (!isEnabled("command")) {
        return;
      }

      // Strip leading slash and extract base command
      String rawCommand = message.substring(1);
      String command = rawCommand.split(" ")[0];

      Map<String, Object> commandData = new HashMap<>();
      commandData.put("command", command);
      commandData.put("rawCommand", rawCommand);
      sendEvent(serverName, "plugin.command", uuid, username, commandData);
    } else {
      if (!isEnabled("chat")) {
        return;
      }

      Map<String, Object> chatData = new HashMap<>();
      chatData.put("message", message);
      sendEvent(serverName, "plugin.chat", uuid, username, chatData);
    }
  }

  @EventHandler
  public void onServerSwitch(ServerSwitchEvent event) {
    if (!isEnabled("serverSwitch")) {
      return;
    }

    // getFrom() is null on initial connection
    if (event.getFrom() == null) {
      return;
    }

    ProxiedPlayer player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getName();
    String from = event.getFrom().getName();

    Server currentServer = player.getServer();
    if (currentServer == null) {
      return;
    }

    String to = currentServer.getInfo().getName();

    Map<String, Object> switchData = new HashMap<>();
    switchData.put("from", from);
    switchData.put("to", to);
    sendEvent(to, "plugin.server_switch", uuid, username, switchData);
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
    if (!clientOpt.isPresent()) {
      return;
    }

    AnalyseClient client = clientOpt.get();
    EventRequest request = new EventRequest(eventName, playerUuid, playerUsername, new HashMap<>(data), null);

    client.trackEvent(request, new AnalyseCallback<EventResponse>() {
      @Override
      public void onSuccess(EventResponse response) {
        plugin.debug("Activity event '%s' sent for %s on %s", eventName, playerUsername, serverName);
      }

      @Override
      public void onError(AnalyseException exception) {
        plugin.getLogger().warning(String.format("Failed to send activity event '%s' for %s: %s",
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
