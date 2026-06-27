package net.analyse.sdk.util;

import java.util.UUID;

/**
 * Helpers for identifying Bedrock players.
 */
public final class BedrockUtil {

  private BedrockUtil() {
  }

  /**
   * Check whether a UUID belongs to a Bedrock player.
   *
   * <p>Floodgate assigns Bedrock players a UUID built from their XUID in the
   * least-significant 64 bits, leaving the most-significant 64 bits all zero.
   * The result is a UUID of the form {@code 00000000-0000-0000-xxxx-xxxxxxxxxxxx}.
   *
   * @param uuid The player's UUID
   * @return true if the UUID is a Floodgate-style Bedrock UUID
   */
  public static boolean isBedrockUuid(UUID uuid) {
    return uuid != null && uuid.getMostSignificantBits() == 0L;
  }
}
