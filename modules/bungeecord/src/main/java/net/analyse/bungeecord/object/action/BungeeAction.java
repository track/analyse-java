package net.analyse.bungeecord.object.action;

import net.analyse.bungeecord.AnalyseBungee;
import net.analyse.sdk.object.action.Action;
import net.analyse.sdk.object.action.ActionData;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Base class for BungeeCord-specific A/B test actions.
 * Analyse does not currently ship any built-in BungeeCord actions.
 */
public abstract class BungeeAction implements Action<ProxiedPlayer> {

  /**
   * Create a BungeeAction from ActionData
   *
   * @param plugin The plugin instance
   * @param data   The action data from the API
   * @return The appropriate BungeeAction, or null if type is unsupported
   */
  public static BungeeAction create(AnalyseBungee plugin, ActionData data) {
    return null;
  }
}
