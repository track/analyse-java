package net.analyse.bungeecord.session;

import lombok.Getter;

/**
 * Stores session data for a connected player
 */
@Getter
public class PlayerSession {

  private final String hostname;
  private final String ip;
  private String currentServer;
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
   * Update the current server and session ID after a join
   *
   * @param serverName The name of the server the player joined
   * @param sessionId  The session ID from the API response
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

  /**
   * Check if the player has an active session
   *
   * @return true if the player has a session ID
   */
  public boolean hasActiveSession() {
    return sessionId != null && !sessionId.isBlank();
  }
}

