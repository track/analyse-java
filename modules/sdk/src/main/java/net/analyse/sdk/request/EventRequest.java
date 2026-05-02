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
  private final String instanceId;

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
    this(name, playerUuid, playerUsername, data, value, null);
  }

  /**
   * Create a new event request
   *
   * @param name           The event name (lowercase with underscores)
   * @param playerUuid     The player's UUID (optional)
   * @param playerUsername The player's username (optional)
   * @param data           Additional event data (optional)
   * @param value          Numeric value for aggregations (optional)
   * @param instanceId     The instance identifier (optional, defaults to "default" if null/blank)
   */
  public EventRequest(String name, UUID playerUuid, String playerUsername, Map<String, Object> data, Double value,
      String instanceId) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Event name cannot be null or blank");
    }

    this.name = name;
    this.playerUuid = playerUuid != null ? playerUuid.toString() : null;
    this.playerUsername = playerUsername;
    this.data = data;
    this.value = value;
    this.instanceId = (instanceId == null || instanceId.trim().isEmpty()) ? "default" : instanceId;
  }
}
