package net.analyse.hytale.object.action;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import net.analyse.hytale.HytalePlugin;
import net.analyse.sdk.object.action.Action;
import net.analyse.sdk.object.action.ActionData;

/**
 * Base class for Hytale-specific A/B test actions.
 * Provides common functionality for all Hytale actions.
 */
public abstract class HytaleAction implements Action<PlayerRef> {

  protected final HytalePlugin plugin;
  protected final ActionData data;

  protected HytaleAction(HytalePlugin plugin, ActionData data) {
    this.plugin = plugin;
    this.data = data;
  }

  /**
   * Create a HytaleAction from ActionData
   *
   * @param plugin The plugin instance
   * @param data   The action data from the API
   * @return The appropriate HytaleAction, or null if type is unknown
   */
  public static HytaleAction create(HytalePlugin plugin, ActionData data) {
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
   * @param playerRef The player
   * @return Text with placeholders replaced
   */
  protected String replacePlaceholders(String text, PlayerRef playerRef) {
    return text
        .replace("%player%", playerRef.getUsername())
        .replace("%uuid%", playerRef.getUuid().toString());
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
