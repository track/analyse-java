package net.analyse.hytale.object.action;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import net.analyse.hytale.HytalePlugin;
import net.analyse.sdk.object.action.Action;
import net.analyse.sdk.object.action.ActionData;

/**
 * Base class for Hytale-specific A/B test actions.
 * Analyse does not currently ship any built-in Hytale actions.
 */
public abstract class HytaleAction implements Action<PlayerRef> {

  /**
   * Create a HytaleAction from ActionData
   *
   * @param plugin The plugin instance
   * @param data   The action data from the API
   * @return The appropriate HytaleAction, or null if type is unknown
   */
  public static HytaleAction create(HytalePlugin plugin, ActionData data) {
    return null;
  }
}
