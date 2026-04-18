package net.analyse.sdk.request;

import lombok.Getter;
import java.util.Map;
import java.util.UUID;

/**
 * Request to track a conversion event for an A/B test
 */
@Getter
public class ConversionRequest {

  private final String testKey;
  private final String variantKey;
  private final String playerUuid;
  private final String playerUsername;
  private final String eventName;
  private final Map<String, Object> data;

  /**
   * Create a new conversion request
   *
   * @param testKey        The A/B test key
   * @param variantKey     The variant key the player was assigned
   * @param playerUuid     The player's UUID
   * @param playerUsername The player's username
   * @param eventName      The conversion event name
   * @param data           Optional additional data
   */
  public ConversionRequest(String testKey, String variantKey, UUID playerUuid,
      String playerUsername, String eventName, Map<String, Object> data) {
    if (testKey == null || testKey.trim().isEmpty()) {
      throw new IllegalArgumentException("Test key cannot be null or blank");
    }
    if (eventName == null || eventName.trim().isEmpty()) {
      throw new IllegalArgumentException("Event name cannot be null or blank");
    }

    this.testKey = testKey;
    this.variantKey = variantKey;
    this.playerUuid = playerUuid != null ? playerUuid.toString() : null;
    this.playerUsername = playerUsername;
    this.eventName = eventName;
    this.data = data;
  }
}
