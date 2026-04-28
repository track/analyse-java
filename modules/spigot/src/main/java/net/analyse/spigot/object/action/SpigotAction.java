package net.analyse.spigot.object.action;

import net.analyse.spigot.AnalysePlugin;
import net.analyse.sdk.object.action.Action;
import net.analyse.sdk.object.action.ActionData;
import org.bukkit.entity.Player;

/**
 * Base class for Paper-specific A/B test actions.
 * Analyse does not currently ship any built-in Paper actions.
 */
public abstract class SpigotAction implements Action<Player> {

  /**
   * Create a SpigotAction from ActionData
   *
   * @param plugin The plugin instance
   * @param data   The action data from the API
   * @return The appropriate SpigotAction, or null if type is unknown
   */
  public static SpigotAction create(AnalysePlugin plugin, ActionData data) {
    return null;
  }
}
