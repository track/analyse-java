package net.analyse.api.platform;

import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.object.abtest.ABTest;
import net.analyse.sdk.object.abtest.Variant;
import java.util.List;
import java.util.UUID;

/**
 * Interface for platform-specific plugin implementations.
 * Each platform (Paper, BungeeCord, Velocity) implements this to provide
 * access to the SDK client and logging.
 */
public interface AnalysePlatform {

  /**
   * Get the SDK client for API communication
   *
   * @return The Analyse SDK client
   */
  AnalyseClient getClient();

  /**
   * Check if debug mode is enabled
   *
   * @return true if debug mode is enabled
   */
  boolean isDebugEnabled();

  /**
   * Log an info message
   *
   * @param message The message to log
   */
  void logInfo(String message);

  /**
   * Log a warning message
   *
   * @param message The message to log
   */
  void logWarning(String message);

  /**
   * Get all active A/B tests
   *
   * @return List of active A/B tests, or empty list if none
   */
  default List<ABTest> getActiveTests() {
    return List.of();
  }

  /**
   * Get an A/B test by its key
   *
   * @param testKey The test key
   * @return The A/B test, or null if not found
   */
  default ABTest getTest(String testKey) {
    return null;
  }

  /**
   * Get the variant assigned to a player for a specific test.
   * This is deterministic - the same player always gets the same variant.
   *
   * @param playerUuid The player's UUID
   * @param testKey    The test key
   * @return The assigned variant key, or null if test not found/inactive
   */
  default String getVariant(UUID playerUuid, String testKey) {
    ABTest test = getTest(testKey);
    if (test == null || !test.isActive()) {
      return null;
    }

    Variant variant = test.assignVariant(playerUuid);
    return variant != null ? variant.getKey() : null;
  }

  /**
   * Check if a test is active
   *
   * @param testKey The test key
   * @return true if the test exists and is active
   */
  default boolean isTestActive(String testKey) {
    ABTest test = getTest(testKey);
    return test != null && test.isActive();
  }

  /**
   * Track a conversion event for an A/B test
   *
   * @param playerUuid     The player's UUID
   * @param playerUsername The player's username
   * @param testKey        The test key
   * @param eventName      The conversion event name
   */
  default void trackConversion(UUID playerUuid, String playerUsername, String testKey, String eventName) {
    // Default implementation does nothing - platforms override this
  }
}
