package com.serverstats.sdk.object.action;

/**
 * Interface for executable A/B test actions.
 * Platform implementations (Paper, BungeeCord, Velocity) provide concrete implementations.
 *
 * @param <TPlayer> The platform-specific player type
 */
public interface Action<TPlayer> {

  /**
   * Get the action type
   *
   * @return The action type
   */
  ActionType getType();

  /**
   * Execute this action for a player
   *
   * @param player The platform-specific player object
   */
  void execute(TPlayer player);
}
