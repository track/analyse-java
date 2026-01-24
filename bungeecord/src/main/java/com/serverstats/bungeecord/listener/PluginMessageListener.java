package com.serverstats.bungeecord.listener;

import com.serverstats.api.ServerStats;
import com.serverstats.api.messaging.ServerStatsMessageParser;
import com.serverstats.api.messaging.ServerStatsMessageParser.ConversionMessage;
import com.serverstats.api.messaging.ServerStatsMessageParser.EventMessage;
import com.serverstats.api.messaging.ServerStatsMessageParser.ParsedMessage;
import com.serverstats.api.messaging.ServerStatsMessaging;
import com.serverstats.bungeecord.ServerStatsBungee;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import java.io.IOException;

/**
 * Listens for plugin messages from backend servers.
 * Allows backend servers to send events through the proxy.
 */
public class PluginMessageListener implements Listener {

  private final ServerStatsBungee plugin;

  /**
   * Create a new plugin message listener
   *
   * @param plugin The plugin instance
   */
  public PluginMessageListener(ServerStatsBungee plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onPluginMessage(PluginMessageEvent event) {
    // Only handle messages from backend servers
    if (!(event.getSender() instanceof Server)) {
      return;
    }

    // Only handle our channel
    if (!event.getTag().equals(ServerStatsMessaging.CHANNEL)) {
      return;
    }

    // Mark as cancelled so it doesn't get forwarded
    event.setCancelled(true);

    Server server = (Server) event.getSender();
    ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
    String serverName = server.getInfo().getName();

    plugin.debug("Received plugin message from %s on server %s", player.getName(), serverName);

    try {
      ParsedMessage message = ServerStatsMessageParser.parse(event.getData());
      handleMessage(message, player, serverName);
    } catch (IOException e) {
      plugin.getLogger().warning(String.format("Failed to parse plugin message from %s: %s",
          player.getName(), e.getMessage()));
    }
  }

  /**
   * Handle a parsed plugin message
   *
   * @param message The parsed message
   * @param player The player associated with the connection
   * @param serverName The server the message came from
   */
  private void handleMessage(ParsedMessage message, ProxiedPlayer player, String serverName) {
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
      plugin.getLogger().warning("Received event message but ServerStats is not available");
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
        plugin.getLogger().warning(String.format("Failed to send event '%s' from backend server %s",
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
      plugin.getLogger().warning("Received conversion message but ServerStats is not available");
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
