package net.analyse.sdk.object.action;

import lombok.Getter;
import java.util.Map;

/**
 * Raw action data from the API.
 * This is deserialized from JSON and used to create platform-specific Action instances.
 */
@Getter
public class ActionData {

  private ActionType type;
  private Map<String, Object> data;

  /**
   * Get a string value from the action data
   *
   * @param key The data key
   * @return The string value, or null if not found
   */
  public String getString(String key) {
    if (data == null) {
      return null;
    }

    Object value = data.get(key);
    return value != null ? value.toString() : null;
  }

  /**
   * Get a boolean value from the action data
   *
   * @param key          The data key
   * @param defaultValue Default value if not found
   * @return The boolean value
   */
  public boolean getBoolean(String key, boolean defaultValue) {
    if (data == null) {
      return defaultValue;
    }

    Object value = data.get(key);
    if (value instanceof Boolean bool) {
      return bool;
    }

    return defaultValue;
  }

  /**
   * Get an integer value from the action data
   *
   * @param key          The data key
   * @param defaultValue Default value if not found
   * @return The integer value
   */
  public int getInt(String key, int defaultValue) {
    if (data == null) {
      return defaultValue;
    }

    Object value = data.get(key);
    if (value instanceof Number number) {
      return number.intValue();
    }

    if (value instanceof String str) {
      try {
        return Integer.parseInt(str);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }

    return defaultValue;
  }
}
