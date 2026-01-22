package com.serverstats.velocity.object.action;

import com.velocitypowered.api.proxy.Player;
import com.serverstats.sdk.object.action.Action;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.velocity.ServerStatsVelocity;

/**
 * Base class for Velocity-specific A/B test actions.
 * Note: Only SEND_MESSAGE and RUN_COMMAND are supported on proxies.
 */
public abstract class VelocityAction implements Action<Player> {

  protected final ServerStatsVelocity plugin;
  protected final ActionData data;

  protected VelocityAction(ServerStatsVelocity plugin, ActionData data) {
    this.plugin = plugin;
    this.data = data;
  }

  /**
   * Create a VelocityAction from ActionData
   *
   * @param plugin The plugin instance
   * @param data   The action data from the API
   * @return The appropriate VelocityAction, or null if type is unsupported
   */
  public static VelocityAction create(ServerStatsVelocity plugin, ActionData data) {
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
  protected String replacePlaceholders(String text, Player player) {
    return text
        .replace("%player%", player.getUsername())
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
