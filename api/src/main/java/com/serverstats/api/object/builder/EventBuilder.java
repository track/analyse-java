package com.serverstats.api.object.builder;

import com.serverstats.api.ServerStats;
import com.serverstats.api.ServerStatsProvider;
import com.serverstats.api.platform.ServerStatsPlatform;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Fluent builder for constructing and sending custom events.
 * Use {@link ServerStats#trackEvent(String)} to create a new builder.
 *
 * <p>Example:</p>
 * <pre>{@code
 * ServerStats.trackEvent("purchase")
 *     .withPlayer(player.getUniqueId(), player.getName())
 *     .withData("item", "diamond_sword")
 *     .withData("price", 100)
 *     .withValue(100.0)
 *     .send();
 * }</pre>
 */
public class EventBuilder {

  private final String name;
  private UUID playerUuid;
  private String playerUsername;
  private Map<String, Object> data;
  private Double value;

  /**
   * Create a new event builder
   *
   * @param name The event name
   */
  public EventBuilder(String name) {
    this.name = name;
  }

  /**
   * Associate this event with a player using UUID and username
   *
   * @param uuid The player's UUID
   * @param username The player's username
   * @return This builder for chaining
   */
  public EventBuilder withPlayer(UUID uuid, String username) {
    this.playerUuid = uuid;
    this.playerUsername = username;
    return this;
  }

  /**
   * Associate this event with a player using only UUID
   *
   * @param uuid The player's UUID
   * @return This builder for chaining
   */
  public EventBuilder withPlayer(UUID uuid) {
    this.playerUuid = uuid;
    return this;
  }

  /**
   * Add a data field to this event
   *
   * @param key The field key
   * @param value The field value
   * @return This builder for chaining
   */
  public EventBuilder withData(String key, Object value) {
    if (this.data == null) {
      this.data = new HashMap<>();
    }

    this.data.put(key, value);
    return this;
  }

  /**
   * Add multiple data fields to this event
   *
   * @param data The data map to add
   * @return This builder for chaining
   */
  public EventBuilder withData(Map<String, Object> data) {
    if (this.data == null) {
      this.data = new HashMap<>();
    }

    this.data.putAll(data);
    return this;
  }

  /**
   * Set a numeric value for this event (useful for aggregations like sum/avg)
   *
   * @param value The numeric value
   * @return This builder for chaining
   */
  public EventBuilder withValue(double value) {
    this.value = value;
    return this;
  }

  /**
   * Get the event name
   *
   * @return The event name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the player UUID
   *
   * @return The player UUID, or null if not set
   */
  public UUID getPlayerUuid() {
    return playerUuid;
  }

  /**
   * Get the player username
   *
   * @return The player username, or null if not set
   */
  public String getPlayerUsername() {
    return playerUsername;
  }

  /**
   * Get the event data
   *
   * @return The data map, or null if not set
   */
  public Map<String, Object> getData() {
    return data;
  }

  /**
   * Get the event value
   *
   * @return The value, or null if not set
   */
  public Double getValue() {
    return value;
  }

  /**
   * Send the event (fire and forget)
   */
  public void send() {
    send(null);
  }

  /**
   * Send the event with a callback for success/failure
   *
   * @param callback Optional callback to receive success (true) or failure (false)
   */
  public void send(Consumer<Boolean> callback) {
    ServerStatsPlatform platform = ServerStatsProvider.getPlatform();
    if (platform == null) {
      throw new IllegalStateException("ServerStats is not initialized. Make sure the ServerStats plugin is enabled.");
    }

    // Delegate to internal sender - this will be set by the platform implementation
    EventSender sender = ServerStats.getEventSender();
    if (sender == null) {
      throw new IllegalStateException("ServerStats event sender is not available. Check your configuration.");
    }

    sender.send(this, callback);
  }

  /**
   * Internal interface for sending events - implemented by platform plugins
   */
  public interface EventSender {

    /**
     * Send an event
     *
     * @param event The event builder with all data
     * @param callback Optional callback for result
     */
    void send(EventBuilder event, Consumer<Boolean> callback);
  }
}
