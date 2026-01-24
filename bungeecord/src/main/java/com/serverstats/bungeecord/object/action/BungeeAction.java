package com.serverstats.bungeecord.object.action;

import com.serverstats.bungeecord.ServerStatsBungee;
import com.serverstats.sdk.object.action.Action;
import com.serverstats.sdk.object.action.ActionData;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Base class for BungeeCord-specific A/B test actions.
 * Note: Only SEND_MESSAGE and RUN_COMMAND are supported on proxies.
 */
public abstract class BungeeAction implements Action<ProxiedPlayer> {

  protected final ServerStatsBungee plugin;
  protected final ActionData data;

  protected BungeeAction(ServerStatsBungee plugin, ActionData data) {
    this.plugin = plugin;
    this.data = data;
  }

  /**
   * Create a BungeeAction from ActionData
   *
   * @param plugin The plugin instance
   * @param data   The action data from the API
   * @return The appropriate BungeeAction, or null if type is unsupported
   */
  public static BungeeAction create(ServerStatsBungee plugin, ActionData data) {
    if (data == null || data.getType() == null) {
      return null;
    }

    return switch (data.getType()) {
      case SEND_MESSAGE -> new SendMessageAction(plugin, data);
      case RUN_COMMAND -> new RunCommandAction(plugin, data);
    };
  }

  /**
   * Replace common placeholders in text
   *
   * @param text   The text with placeholders
   * @param player The player
   * @return Text with placeholders replaced
   */
  protected String replacePlaceholders(String text, ProxiedPlayer player) {
    return text
        .replace("%player%", player.getName())
        .replace("%uuid%", player.getUniqueId().toString());
  }

  /**
   * Log a debug message if debug mode is enabled
   *
   * @param message The message format
   * @param args    Format arguments
   */
  protected void debug(String message, Object... args) {
    if (plugin.isDebugEnabled()) {
      plugin.logInfo(String.format("[DEBUG] " + message, args));
    }
  }
}
