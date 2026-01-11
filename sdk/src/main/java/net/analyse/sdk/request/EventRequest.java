package net.analyse.sdk.request;

import lombok.Getter;
import java.util.Map;
import java.util.UUID;

/**
 * Request payload for the custom event endpoint
 */
@Getter
public class EventRequest {

  private final String name;
  private final String playerUuid;
  private final String playerUsername;
  private final Map<String, Object> data;
  private final Double value;

  /**
   * Create a new event request
   *
   * @param name           The event name (lowercase with underscores)
   * @param playerUuid     The player's UUID (optional)
   * @param playerUsername The player's username (optional)
   * @param data           Additional event data (optional)
   * @param value          Numeric value for aggregations (optional)
   */
  public EventRequest(String name, UUID playerUuid, String playerUsername, Map<String, Object> data, Double value) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Event name cannot be null or blank");
    }

    this.name = name;
    this.playerUuid = playerUuid != null ? playerUuid.toString() : null;
    this.playerUsername = playerUsername;
    this.data = data;
    this.value = value;
  }
}
