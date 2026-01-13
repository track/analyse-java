package net.analyse.api.manager;

import net.analyse.api.object.abtest.ABTest;
import java.util.List;
import java.util.UUID;

/**
 * Manages A/B tests and variant assignments
 */
public interface ABTestManager {

  /**
   * Get all active A/B tests
   *
   * @return List of active tests
   */
  List<? extends ABTest> getActiveTests();

  /**
   * Get an A/B test by its key
   *
   * @param testKey The test key
   * @return The test, or null if not found
   */
  ABTest getTest(String testKey);

  /**
   * Get the variant assigned to a player for a specific test.
   * This is deterministic - the same player always gets the same variant.
   *
   * @param playerUuid The player's UUID
   * @param testKey The test key
   * @return The assigned variant key, or null if test not found/inactive
   */
  String getVariant(UUID playerUuid, String testKey);

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
   * @param playerUuid The player's UUID
   * @param playerUsername The player's username
   * @param testKey The test key
   * @param eventName The conversion event name
   */
  void trackConversion(UUID playerUuid, String playerUsername, String testKey, String eventName);
}
