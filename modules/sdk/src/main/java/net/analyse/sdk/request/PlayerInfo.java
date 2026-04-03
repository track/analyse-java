package net.analyse.sdk.request;

import lombok.Getter;
import java.util.UUID;

/**
 * Player information for heartbeat requests
 */
@Getter
public class PlayerInfo {

  private final String uuid;
  private final String hostname;

  /**
   * Create player info with hostname
   *
   * @param uuid     The player's UUID
   * @param hostname The hostname the player used to connect (can be null)
   */
  public PlayerInfo(UUID uuid, String hostname) {
    if (uuid == null) {
      throw new IllegalArgumentException("UUID cannot be null");
    }

    this.uuid = uuid.toString();
    this.hostname = hostname;
  }
}
