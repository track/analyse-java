package com.serverstats.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.serverstats.api.ServerStats;
import com.serverstats.api.messaging.ServerStatsMessageParser;
import com.serverstats.api.messaging.ServerStatsMessageParser.ConversionMessage;
import com.serverstats.api.messaging.ServerStatsMessageParser.EventMessage;
import com.serverstats.api.messaging.ServerStatsMessageParser.ParsedMessage;
import com.serverstats.api.messaging.ServerStatsMessaging;
import com.serverstats.velocity.ServerStatsVelocity;
import java.io.IOException;

/**
 * Listens for plugin messages from backend servers.
 * Allows backend servers to send events through the proxy.
 */
public class PluginMessageListener {

  private final ServerStatsVelocity plugin;

  /**
   * Create a new plugin message listener
   *
   * @param plugin The plugin instance
   */
  public PluginMessageListener(ServerStatsVelocity plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onPluginMessage(PluginMessageEvent event) {
    // Only handle messages from backend servers
    if (!(event.getSource() instanceof ServerConnection)) {
      return;
    }

    // Only handle our channel
    if (!event.getIdentifier().getId().equals(ServerStatsMessaging.CHANNEL)) {
      return;
    }

    // Mark as handled so it doesn't get forwarded
    event.setResult(PluginMessageEvent.ForwardResult.handled());

    ServerConnection connection = (ServerConnection) event.getSource();
    Player player = connection.getPlayer();
    String serverName = connection.getServerInfo().getName();

    plugin.debug("Received plugin message from %s on server %s", player.getUsername(), serverName);

    try {
      ParsedMessage message = ServerStatsMessageParser.parse(event.getData());
      handleMessage(message, player, serverName);
    } catch (IOException e) {
      plugin.getLogger().warn(String.format("Failed to parse plugin message from %s: %s",
          player.getUsername(), e.getMessage()));
    }
  }

  /**
   * Handle a parsed plugin message
   *
   * @param message The parsed message
   * @param player The player associated with the connection
   * @param serverName The server the message came from
   */
  private void handleMessage(ParsedMessage message, Player player, String serverName) {
    if (message.isEvent()) {
      handleEventMessage((EventMessage) message, serverName);
    } else if (message.isConversion()) {
      handleConversionMessage((ConversionMessage) message, serverName);
    }
  }

  /**
   * Handle an event message
   *
   * @param message The event message
   * @param serverName The source server
   */
  private void handleEventMessage(EventMessage message, String serverName) {
    if (!ServerStats.isAvailable()) {
      plugin.getLogger().warn("Received event message but ServerStats is not available");
      return;
    }

    plugin.debug("Processing event '%s' for player %s from server %s",
        message.getEventName(), message.getPlayerUsername(), serverName);

    // Build and send the event
    var builder = ServerStats.trackEvent(message.getEventName())
        .withPlayer(message.getPlayerUuid(), message.getPlayerUsername());

    if (message.getData() != null) {
      builder.withData(message.getData());
    }

    if (message.getValue() != null) {
      builder.withValue(message.getValue());
    }

    // Add server context
    builder.withData("_source_server", serverName);

    builder.send(success -> {
      if (success) {
        plugin.debug("Event '%s' from backend server %s sent successfully",
            message.getEventName(), serverName);
      } else {
        plugin.getLogger().warn(String.format("Failed to send event '%s' from backend server %s",
            message.getEventName(), serverName));
      }
    });
  }

  /**
   * Handle a conversion message
   *
   * @param message The conversion message
   * @param serverName The source server
   */
  private void handleConversionMessage(ConversionMessage message, String serverName) {
    if (!ServerStats.isAvailable()) {
      plugin.getLogger().warn("Received conversion message but ServerStats is not available");
      return;
    }

    plugin.debug("Processing conversion for test '%s' event '%s' for player %s from server %s",
        message.getTestKey(), message.getConversionEvent(),
        message.getPlayerUsername(), serverName);

    ServerStats.trackConversion(
        message.getPlayerUuid(),
        message.getPlayerUsername(),
        message.getTestKey(),
        message.getConversionEvent()
    );
  }
}
