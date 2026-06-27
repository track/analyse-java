package net.analyse.sdk.config;

/**
 * Controls how Bedrock players are detected.
 *
 * <p>NAME matches the player's username against a configured prefix (e.g. "."),
 * which is how Floodgate/Geyser rename Bedrock players by default. UUID inspects
 * the player's UUID instead, which Floodgate generates in the
 * {@code 00000000-0000-0000-xxxx-xxxxxxxxxxxx} form (the entire high 64 bits are
 * zero). UUID detection is more reliable because it cannot be fooled by Java
 * players whose names happen to start with the prefix.
 */
public enum BedrockMode {
  NAME,
  UUID;

  /**
   * Parse a bedrock detection mode from configuration.
   *
   * @param value The configured value
   * @return The parsed mode, or NAME if missing/invalid
   */
  public static BedrockMode fromConfig(String value) {
    if (value == null || value.trim().isEmpty()) {
      return NAME;
    }

    try {
      return BedrockMode.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ignored) {
      return NAME;
    }
  }
}
