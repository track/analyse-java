package net.analyse.velocity.object.action;

import com.velocitypowered.api.proxy.Player;
import net.analyse.sdk.object.action.Action;
import net.analyse.sdk.object.action.ActionData;
import net.analyse.velocity.AnalyseVelocity;

/**
 * Base class for Velocity-specific A/B test actions.
 * Analyse does not currently ship any built-in Velocity actions.
 */
public abstract class VelocityAction implements Action<Player> {

  /**
   * Create a VelocityAction from ActionData
   *
   * @param plugin The plugin instance
   * @param data   The action data from the API
   * @return The appropriate VelocityAction, or null if type is unsupported
   */
  public static VelocityAction create(AnalyseVelocity plugin, ActionData data) {
    return null;
  }
}
