package net.analyse.velocity.object.session;

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
  private String currentServer;
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
   * Update the current server and session ID after a join
   *
   * @param serverName The name of the server the player joined
   * @param sessionId The session ID from the API response
   */
  public void setCurrentSession(String serverName, String sessionId) {
    this.currentServer = serverName;
    this.sessionId = sessionId;
  }

  /**
   * Clear the session data when the player leaves a server
   */
  public void clearSession() {
    this.currentServer = null;
    this.sessionId = null;
  }

  @Override
  public boolean hasActiveSession() {
    return sessionId != null && !sessionId.isBlank();
  }
}
