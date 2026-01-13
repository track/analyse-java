package net.analyse.api;

import net.analyse.sdk.object.abtest.ABTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main entry point for the Analyse API.
 * Use this class to track custom analytics events and interact with A/B tests.
 *
 * <p>Event tracking example:</p>
 * <pre>{@code
 * // Track a player completing a quest
 * Analyse.trackEvent("quest_completed")
 *     .withPlayer(player.getUniqueId(), player.getName())
 *     .withData("quest_id", "dragon_slayer")
 *     .withData("time_taken_seconds", 3600)
 *     .withValue(500.0)
 *     .send();
 * }</pre>
 *
 * <p>A/B testing example:</p>
 * <pre>{@code
 * // Get the variant assigned to a player
 * String variant = Analyse.getVariant(player.getUniqueId(), "welcome-rewards");
 * if ("diamonds".equals(variant)) {
 *     giveWelcomeDiamonds(player);
 * }
 *
 * // Track a conversion event
 * Analyse.trackConversion(player.getUniqueId(), player.getName(), "welcome-rewards", "first_purchase");
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

  // ========== A/B Testing Methods ==========

  /**
   * Get the variant assigned to a player for a specific A/B test.
   * This is deterministic - the same player always gets the same variant.
   *
   * <p>Example:</p>
   * <pre>{@code
   * String variant = Analyse.getVariant(player.getUniqueId(), "welcome-rewards");
   * switch (variant) {
   *     case "control" -> { }
   *     case "diamonds" -> giveWelcomeDiamonds(player);
   *     case "tutorial" -> startTutorial(player);
   * }
   * }</pre>
   *
   * @param playerUuid The player's UUID
   * @param testKey    The A/B test key
   * @return The assigned variant key, or null if test not found or inactive
   */
  public static String getVariant(UUID playerUuid, String testKey) {
    if (!isAvailable()) {
      return null;
    }

    return AnalyseProvider.getPlatform().getVariant(playerUuid, testKey);
  }

  /**
   * Check if an A/B test is currently active
   *
   * @param testKey The A/B test key
   * @return true if the test exists and is active
   */
  public static boolean isTestActive(String testKey) {
    if (!isAvailable()) {
      return false;
    }

    return AnalyseProvider.getPlatform().isTestActive(testKey);
  }

  /**
   * Get all active A/B tests
   *
   * @return List of active A/B tests, or empty list if none
   */
  public static List<ABTest> getActiveTests() {
    if (!isAvailable()) {
      return List.of();
    }

    return AnalyseProvider.getPlatform().getActiveTests();
  }

  /**
   * Get an A/B test by its key
   *
   * @param testKey The test key
   * @return The A/B test, or null if not found
   */
  public static ABTest getTest(String testKey) {
    if (!isAvailable()) {
      return null;
    }

    return AnalyseProvider.getPlatform().getTest(testKey);
  }

  /**
   * Track a conversion event for an A/B test.
   * This records that a player completed a desired action.
   *
   * <p>Example:</p>
   * <pre>{@code
   * // Track when a player makes their first purchase
   * Analyse.trackConversion(player.getUniqueId(), player.getName(), "welcome-rewards", "first_purchase");
   * }</pre>
   *
   * @param playerUuid     The player's UUID
   * @param playerUsername The player's username
   * @param testKey        The A/B test key
   * @param eventName      The conversion event name
   */
  public static void trackConversion(UUID playerUuid, String playerUsername, String testKey, String eventName) {
    if (!isAvailable()) {
      return;
    }

    AnalyseProvider.getPlatform().trackConversion(playerUuid, playerUsername, testKey, eventName);
  }
}
