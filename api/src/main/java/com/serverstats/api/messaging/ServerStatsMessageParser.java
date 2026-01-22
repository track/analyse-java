package com.serverstats.api.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

/**
 * Parser for ServerStats plugin messages.
 * Used by proxy plugins to parse incoming messages from backend servers.
 */
public final class ServerStatsMessageParser {

  private static final Gson GSON = new GsonBuilder().create();
  private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

  private ServerStatsMessageParser() {
  }

  /**
   * Parse a plugin message
   *
   * @param data The raw message bytes
   * @return The parsed message
   * @throws IOException if parsing fails
   */
  public static ParsedMessage parse(byte[] data) throws IOException {
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

    String type = in.readUTF();

    if (ServerStatsMessaging.TYPE_EVENT.equals(type)) {
      return parseEventMessage(in);
    } else if (ServerStatsMessaging.TYPE_CONVERSION.equals(type)) {
      return parseConversionMessage(in);
    } else {
      throw new IOException("Unknown message type: " + type);
    }
  }

  /**
   * Parse an event message
   *
   * @param in The data input stream
   * @return The parsed event message
   * @throws IOException if parsing fails
   */
  private static EventMessage parseEventMessage(DataInputStream in) throws IOException {
    String eventName = in.readUTF();
    UUID playerUuid = UUID.fromString(in.readUTF());
    String playerUsername = in.readUTF();
    if (playerUsername.isEmpty()) {
      playerUsername = null;
    }

    String dataJson = in.readUTF();
    Map<String, Object> data = null;
    if (dataJson != null && !dataJson.equals("{}")) {
      data = GSON.fromJson(dataJson, MAP_TYPE);
    }

    Double value = null;
    if (in.readBoolean()) {
      value = in.readDouble();
    }

    return new EventMessage(eventName, playerUuid, playerUsername, data, value);
  }

  /**
   * Parse a conversion message
   *
   * @param in The data input stream
   * @return The parsed conversion message
   * @throws IOException if parsing fails
   */
  private static ConversionMessage parseConversionMessage(DataInputStream in) throws IOException {
    UUID playerUuid = UUID.fromString(in.readUTF());
    String playerUsername = in.readUTF();
    if (playerUsername.isEmpty()) {
      playerUsername = null;
    }
    String testKey = in.readUTF();
    String conversionEvent = in.readUTF();

    return new ConversionMessage(playerUuid, playerUsername, testKey, conversionEvent);
  }

  /**
   * Base class for parsed messages
   */
  public abstract static class ParsedMessage {

    private final String type;

    protected ParsedMessage(String type) {
      this.type = type;
    }

    /**
     * Get the message type
     *
     * @return The message type
     */
    public String getType() {
      return type;
    }

    /**
     * Check if this is an event message
     *
     * @return true if event message
     */
    public boolean isEvent() {
      return ServerStatsMessaging.TYPE_EVENT.equals(type);
    }

    /**
     * Check if this is a conversion message
     *
     * @return true if conversion message
     */
    public boolean isConversion() {
      return ServerStatsMessaging.TYPE_CONVERSION.equals(type);
    }
  }

  /**
   * Represents a parsed event message
   */
  public static class EventMessage extends ParsedMessage {

    private final String eventName;
    private final UUID playerUuid;
    private final String playerUsername;
    private final Map<String, Object> data;
    private final Double value;

    /**
     * Create a new event message
     *
     * @param eventName The event name
     * @param playerUuid The player UUID
     * @param playerUsername The player username
     * @param data The event data
     * @param value The event value
     */
    public EventMessage(
        String eventName,
        UUID playerUuid,
        String playerUsername,
        Map<String, Object> data,
        Double value
    ) {
      super(ServerStatsMessaging.TYPE_EVENT);
      this.eventName = eventName;
      this.playerUuid = playerUuid;
      this.playerUsername = playerUsername;
      this.data = data;
      this.value = value;
    }

    public String getEventName() {
      return eventName;
    }

    public UUID getPlayerUuid() {
      return playerUuid;
    }

    public String getPlayerUsername() {
      return playerUsername;
    }

    public Map<String, Object> getData() {
      return data;
    }

    public Double getValue() {
      return value;
    }
  }

  /**
   * Represents a parsed conversion message
   */
  public static class ConversionMessage extends ParsedMessage {

    private final UUID playerUuid;
    private final String playerUsername;
    private final String testKey;
    private final String conversionEvent;

    /**
     * Create a new conversion message
     *
     * @param playerUuid The player UUID
     * @param playerUsername The player username
     * @param testKey The A/B test key
     * @param conversionEvent The conversion event name
     */
    public ConversionMessage(
        UUID playerUuid,
        String playerUsername,
        String testKey,
        String conversionEvent
    ) {
      super(ServerStatsMessaging.TYPE_CONVERSION);
      this.playerUuid = playerUuid;
      this.playerUsername = playerUsername;
      this.testKey = testKey;
      this.conversionEvent = conversionEvent;
    }

    public UUID getPlayerUuid() {
      return playerUuid;
    }

    public String getPlayerUsername() {
      return playerUsername;
    }

    public String getTestKey() {
      return testKey;
    }

    public String getConversionEvent() {
      return conversionEvent;
    }
  }
}
