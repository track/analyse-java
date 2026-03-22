package net.analyse.spigot.object.session;

import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores session data for a connected player
 */
@Getter
public class PlayerSession implements net.analyse.api.session.PlayerSession {

  private final UUID playerUuid;
  private final String hostname;
  private final String ip;
  private final Instant joinTime;
  private String sessionId;

  /**
   * Create a new player session
   *
   * @param playerUuid The player's UUID
   * @param hostname The hostname the player used to connect
   * @param ip The player's IP address
   */
  public PlayerSession(UUID playerUuid, String hostname, String ip) {
    this.playerUuid = playerUuid;
    this.hostname = hostname;
    this.ip = ip;
    this.joinTime = Instant.now();
  }

  /**
   * Set the session ID after a successful join
   *
   * @param sessionId The session ID from the API response
   */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public boolean hasActiveSession() {
    return sessionId != null && !sessionId.trim().isEmpty();
  }
}
