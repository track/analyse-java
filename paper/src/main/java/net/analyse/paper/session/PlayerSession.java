package net.analyse.paper.session;

import lombok.Getter;

/**
 * Stores session data for a connected player
 */
@Getter
public class PlayerSession {

  private final String hostname;
  private final String ip;
  private String sessionId;

  /**
   * Create a new player session
   *
   * @param hostname The hostname the player used to connect
   * @param ip       The player's IP address
   */
  public PlayerSession(String hostname, String ip) {
    this.hostname = hostname;
    this.ip = ip;
  }

  /**
   * Set the session ID after a successful join
   *
   * @param sessionId The session ID from the API response
   */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  /**
   * Check if the player has an active session
   *
   * @return true if the player has a session ID
   */
  public boolean hasActiveSession() {
    return sessionId != null && !sessionId.isBlank();
  }
}

