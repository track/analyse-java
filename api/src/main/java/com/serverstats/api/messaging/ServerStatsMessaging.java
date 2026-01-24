package com.serverstats.api.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for sending ServerStats events via plugin messaging.
 * Use this on backend servers to send events through the proxy.
 *
 * <p>Example usage on a Paper backend server:</p>
 * <pre>{@code
 * // Send an event through the proxy
 * byte[] message = ServerStatsMessaging.createEventMessage(
 *     "crate_opened",
 *     player.getUniqueId(),
 *     player.getName(),
 *     Map.of("crate_type", "legendary"),
 *     100.0
 * );
 *
 * player.sendPluginMessage(plugin, ServerStatsMessaging.CHANNEL, message);
 * }</pre>
 */
public final class ServerStatsMessaging {

  /**
   * The plugin messaging channel for ServerStats events
   */
  public static final String CHANNEL = "serverstats:events";

  /**
   * Message type for custom events
   */
  public static final String TYPE_EVENT = "event";

  /**
   * Message type for A/B test conversions
   */
  public static final String TYPE_CONVERSION = "conversion";

  private static final Gson GSON = new GsonBuilder().create();

  private ServerStatsMessaging() {
  }

  /**
   * Create a plugin message for tracking a custom event
   *
   * @param eventName The event name
   * @param playerUuid The player's UUID
   * @param playerUsername The player's username
   * @param data Optional event data (can be null)
   * @param value Optional numeric value (can be null)
   * @return The encoded message bytes
   * @throws IOException if encoding fails
   */
  public static byte[] createEventMessage(
      String eventName,
      UUID playerUuid,
      String playerUsername,
      Map<String, Object> data,
      Double value
  ) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bytes);

    out.writeUTF(TYPE_EVENT);
    out.writeUTF(eventName);
    out.writeUTF(playerUuid.toString());
    out.writeUTF(playerUsername != null ? playerUsername : "");
    out.writeUTF(data != null ? GSON.toJson(data) : "{}");
    out.writeBoolean(value != null);
    if (value != null) {
      out.writeDouble(value);
    }

    return bytes.toByteArray();
  }

  /**
   * Create a plugin message for tracking an A/B test conversion
   *
   * @param playerUuid The player's UUID
   * @param playerUsername The player's username
   * @param testKey The A/B test key
   * @param conversionEvent The conversion event name
   * @return The encoded message bytes
   * @throws IOException if encoding fails
   */
  public static byte[] createConversionMessage(
      UUID playerUuid,
      String playerUsername,
      String testKey,
      String conversionEvent
  ) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bytes);

    out.writeUTF(TYPE_CONVERSION);
    out.writeUTF(playerUuid.toString());
    out.writeUTF(playerUsername != null ? playerUsername : "");
    out.writeUTF(testKey);
    out.writeUTF(conversionEvent);

    return bytes.toByteArray();
  }

  /**
   * Builder for creating event messages with a fluent API
   */
  public static class EventMessageBuilder {

    private final String eventName;
    private UUID playerUuid;
    private String playerUsername;
    private Map<String, Object> data;
    private Double value;

    /**
     * Create a new event message builder
     *
     * @param eventName The event name
     */
    public EventMessageBuilder(String eventName) {
      this.eventName = eventName;
    }

    /**
     * Associate this event with a player
     *
     * @param uuid The player's UUID
     * @param username The player's username
     * @return This builder
     */
    public EventMessageBuilder withPlayer(UUID uuid, String username) {
      this.playerUuid = uuid;
      this.playerUsername = username;
      return this;
    }

    /**
     * Add a data field
     *
     * @param key The field key
     * @param value The field value
     * @return This builder
     */
    public EventMessageBuilder withData(String key, Object value) {
      if (this.data == null) {
        this.data = new HashMap<>();
      }
      this.data.put(key, value);
      return this;
    }

    /**
     * Set a numeric value
     *
     * @param value The value
     * @return This builder
     */
    public EventMessageBuilder withValue(double value) {
      this.value = value;
      return this;
    }

    /**
     * Build the message bytes
     *
     * @return The encoded message
     * @throws IOException if encoding fails
     * @throws IllegalStateException if player is not set
     */
    public byte[] build() throws IOException {
      if (playerUuid == null) {
        throw new IllegalStateException("Player UUID is required");
      }
      return createEventMessage(eventName, playerUuid, playerUsername, data, value);
    }
  }

  /**
   * Create a new event message builder
   *
   * @param eventName The event name
   * @return A new builder
   */
  public static EventMessageBuilder event(String eventName) {
    return new EventMessageBuilder(eventName);
  }
}
