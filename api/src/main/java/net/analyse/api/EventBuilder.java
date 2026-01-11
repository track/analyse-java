package net.analyse.api;

import net.analyse.api.platform.AnalysePlatform;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseException;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.response.EventResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Fluent builder for constructing and sending custom events.
 * Use {@link Analyse#trackEvent(String)} to create a new builder.
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
  EventBuilder(String name) {
    this.name = name;
  }

  /**
   * Associate this event with a player using UUID and username
   *
   * @param uuid     The player's UUID
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
   * @param key   The field key
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
   * Send the event (fire and forget)
   */
  public void send() {
    send(null);
  }

  /**
   * Send the event with a callback for the response
   *
   * @param callback Optional callback to receive the response
   */
  public void send(Consumer<EventResponse> callback) {
    AnalysePlatform platform = AnalyseProvider.getPlatform();
    if (platform == null) {
      throw new IllegalStateException("Analyse is not initialized. Make sure the Analyse plugin is enabled.");
    }

    if (platform.getClient() == null) {
      throw new IllegalStateException("Analyse client is not available. Check your configuration.");
    }

    EventRequest request = new EventRequest(name, playerUuid, playerUsername, data, value);

    platform.getClient().trackEvent(request, new AnalyseCallback<>() {
      @Override
      public void onSuccess(EventResponse response) {
        if (platform.isDebugEnabled()) {
          platform.logInfo(String.format("[DEBUG] Event '%s' tracked successfully (id: %s)",
              name, response.getEventId()));
        }

        if (callback != null) {
          callback.accept(response);
        }
      }

      @Override
      public void onError(AnalyseException exception) {
        platform.logWarning(String.format("Failed to track event '%s': %s",
            name, exception.getMessage()));

        if (callback != null) {
          callback.accept(null);
        }
      }
    });
  }
}
