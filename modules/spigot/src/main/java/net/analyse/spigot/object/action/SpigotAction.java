package net.analyse.spigot.object.action;

import net.analyse.spigot.AnalysePlugin;
import net.analyse.sdk.object.action.Action;
import net.analyse.sdk.object.action.ActionData;
import org.bukkit.entity.Player;

/**
 * Base class for Paper-specific A/B test actions.
 * Provides common functionality for all Paper actions.
 */
public abstract class SpigotAction implements Action<Player> {

  protected final AnalysePlugin plugin;
  protected final ActionData data;

  protected SpigotAction(AnalysePlugin plugin, ActionData data) {
    this.plugin = plugin;
    this.data = data;
  }

  /**
   * Create a SpigotAction from ActionData
   *
   * @param plugin The plugin instance
   * @param data   The action data from the API
   * @return The appropriate SpigotAction, or null if type is unknown
   */
  public static SpigotAction create(AnalysePlugin plugin, ActionData data) {
    if (data == null || data.getType() == null) {
      return null;
    }

    switch (data.getType()) {
      case SEND_MESSAGE:
        return new SendMessageAction(plugin, data);
      case RUN_COMMAND:
        return new RunCommandAction(plugin, data);
      default:
        return null;
    }
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
