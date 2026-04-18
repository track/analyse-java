package net.analyse.api;

import net.analyse.api.manager.ABTestManager;
import net.analyse.api.manager.SessionManager;
import net.analyse.api.object.abtest.ABTest;
import net.analyse.api.object.builder.EventBuilder;
import net.analyse.api.platform.AnalysePlatform;
import java.util.Collections;
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
 *
 * <p>Accessing managers:</p>
 * <pre>{@code
 * // Get the platform for advanced usage
 * AnalysePlatform platform = Analyse.get();
 * SessionManager sessions = platform.getSessionManager();
 * ABTestManager abTests = platform.getABTestManager();
 * }</pre>
 */
public final class Analyse {

  private static EventBuilder.EventSender eventSender;
  private static boolean apiConnected = false;
  private static String lastConnectionError = null;

  private Analyse() {
  }

  /**
   * Get the Analyse platform instance for advanced usage.
   * Use this to access managers and platform-specific functionality.
   *
   * @return The platform instance
   * @throws IllegalStateException if Analyse is not initialized
   */
  public static AnalysePlatform get() {
    AnalysePlatform platform = AnalyseProvider.getPlatform();
    if (platform == null) {
      throw new IllegalStateException("Analyse is not initialized. Make sure the Analyse plugin is enabled.");
    }

    return platform;
  }

  /**
   * Check if Analyse is available and ready to use.
   * This checks if the plugin is initialized, not if the API connection is working.
   *
   * @return true if Analyse is initialized and ready
   */
  public static boolean isAvailable() {
    return AnalyseProvider.isRegistered();
  }

  /**
   * Check if the API connection is working (based on last heartbeat/API call)
   *
   * @return true if the last API call was successful
   */
  public static boolean isConnected() {
    return isAvailable() && apiConnected;
  }

  /**
   * Get the last connection error message
   *
   * @return The error message, or null if connected
   */
  public static String getLastConnectionError() {
    return lastConnectionError;
  }

  /**
   * Set the API connection status. Called by platform implementations.
   *
   * @param connected Whether the last API call was successful
   * @param errorMessage The error message if failed, or null if successful
   */
  public static void setConnectionStatus(boolean connected, String errorMessage) {
    apiConnected = connected;
    lastConnectionError = connected ? null : errorMessage;
  }

  /**
   * Create a new event builder to track a custom event.
   *
   * @param name The event name (lowercase with underscores recommended, e.g., "quest_completed")
   * @return An EventBuilder for configuring and sending the event
   * @throws IllegalArgumentException if the name is null or blank
   */
  public static EventBuilder trackEvent(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Event name cannot be null or blank");
    }

    return new EventBuilder(name);
  }

  /**
   * Convenience method to quickly track an event with a player
   *
   * @param name The event name
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
   * @param playerUuid The player's UUID
   * @param testKey The A/B test key
   * @return The assigned variant key, or null if test not found or inactive
   */
  public static String getVariant(UUID playerUuid, String testKey) {
    if (!isAvailable()) {
      return null;
    }

    ABTestManager manager = get().getABTestManager();
    return manager != null ? manager.getVariant(playerUuid, testKey) : null;
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

    ABTestManager manager = get().getABTestManager();
    return manager != null && manager.isTestActive(testKey);
  }

  /**
   * Get all active A/B tests
   *
   * @return List of active A/B tests, or empty list if none
   */
  public static List<? extends ABTest> getActiveTests() {
    if (!isAvailable()) {
      return Collections.emptyList();
    }

    ABTestManager manager = get().getABTestManager();
    return manager != null ? manager.getActiveTests() : Collections.emptyList();
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

    ABTestManager manager = get().getABTestManager();
    return manager != null ? manager.getTest(testKey) : null;
  }

  /**
   * Track a conversion event for an A/B test.
   * This records that a player completed a desired action.
   *
   * @param playerUuid The player's UUID
   * @param playerUsername The player's username
   * @param testKey The A/B test key
   * @param eventName The conversion event name
   */
  public static void trackConversion(UUID playerUuid, String playerUsername, String testKey, String eventName) {
    if (!isAvailable()) {
      return;
    }

    ABTestManager manager = get().getABTestManager();
    if (manager != null) {
      manager.trackConversion(playerUuid, playerUsername, testKey, eventName);
    }
  }

  // ========== Manager Shortcuts ==========

  /**
   * Get the session manager
   *
   * @return The session manager
   * @throws IllegalStateException if Analyse is not initialized
   */
  public static SessionManager sessions() {
    return get().getSessionManager();
  }

  /**
   * Get the A/B test manager
   *
   * @return The A/B test manager, or null if not available
   * @throws IllegalStateException if Analyse is not initialized
   */
  public static ABTestManager abTests() {
    return get().getABTestManager();
  }

  // ========== Internal Methods (for platform implementations) ==========

  /**
   * Set the event sender. Called by platform implementations.
   *
   * @param sender The event sender
   */
  public static void setEventSender(EventBuilder.EventSender sender) {
    eventSender = sender;
  }

  /**
   * Get the event sender
   *
   * @return The event sender, or null if not set
   */
  public static EventBuilder.EventSender getEventSender() {
    return eventSender;
  }
}
