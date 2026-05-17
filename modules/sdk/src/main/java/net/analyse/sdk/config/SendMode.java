package net.analyse.sdk.config;

/**
 * Controls how analytics items are sent to the Analyse API.
 */
public enum SendMode {
  SINGLE,
  BATCH;

  /**
   * Parse a send mode value from configuration.
   *
   * @param value The configured value
   * @return The parsed send mode, or SINGLE if missing/invalid
   */
  public static SendMode fromConfig(String value) {
    if (value == null || value.trim().isEmpty()) {
      return SINGLE;
    }

    try {
      return SendMode.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ignored) {
      return SINGLE;
    }
  }
}
