package net.analyse.api;

import java.util.Map;
import java.util.UUID;

/**
 * Main entry point for the Analyse API.
 * Use this class to track custom analytics events from your plugin.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Track a player completing a quest
 * Analyse.trackEvent("quest_completed")
 *     .withPlayer(player.getUniqueId(), player.getName())
 *     .withData("quest_id", "dragon_slayer")
 *     .withData("time_taken_seconds", 3600)
 *     .withValue(500.0)
 *     .send();
 *
 * // Simple event with just a name
 * Analyse.trackEvent("server_restart").send();
 *
 * // Track with callback
 * Analyse.trackEvent("purchase")
 *     .withPlayer(player.getUniqueId(), player.getName())
 *     .withData("item", "diamond_sword")
 *     .withValue(1000.0)
 *     .send(response -> {
 *         if (response != null && response.isSuccess()) {
 *             // Event tracked successfully
 *         }
 *     });
 * }</pre>
 */
public final class Analyse {

  private Analyse() {
    // Prevent instantiation
  }

  /**
   * Create a new event builder to track a custom event.
   *
   * @param name The event name (lowercase with underscores recommended, e.g., "quest_completed")
   * @return An EventBuilder for configuring and sending the event
   * @throws IllegalArgumentException if the name is null or blank
   */
  public static EventBuilder trackEvent(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Event name cannot be null or blank");
    }

    return new EventBuilder(name);
  }

  /**
   * Check if Analyse is available and ready to track events
   *
   * @return true if Analyse is initialized and ready
   */
  public static boolean isAvailable() {
    return AnalyseProvider.isRegistered() && AnalyseProvider.getPlatform().getClient() != null;
  }

  /**
   * Convenience method to quickly track an event with a player
   *
   * @param name     The event name
   * @param playerUuid The player's UUID
   * @param playerUsername The player's username
   * @return An EventBuilder for further configuration
   */
  public static EventBuilder trackEvent(String name, UUID playerUuid, String playerUsername) {
    return trackEvent(name).withPlayer(playerUuid, playerUsername);
  }

  /**
   * Convenience method to quickly track an event with data
   *
   * @param name The event name
   * @param data The event data
   * @return An EventBuilder for further configuration
   */
  public static EventBuilder trackEvent(String name, Map<String, Object> data) {
    return trackEvent(name).withData(data);
  }
}
