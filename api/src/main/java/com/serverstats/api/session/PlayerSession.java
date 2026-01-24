package com.serverstats.api.session;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a player's analytics session
 */
public interface PlayerSession {

  /**
   * Get the player's UUID
   *
   * @return The player UUID
   */
  UUID getPlayerUuid();

  /**
   * Get the session ID from the API
   *
   * @return The session ID, or null if not yet assigned
   */
  String getSessionId();

  /**
   * Get the hostname the player connected with
   *
   * @return The hostname
   */
  String getHostname();

  /**
   * Get the player's IP address
   *
   * @return The IP address
   */
  String getIp();

  /**
   * Get when the player joined
   *
   * @return The join timestamp
   */
  Instant getJoinTime();

  /**
   * Check if this session has an active API session ID
   *
   * @return true if the session has been registered with the API
   */
  default boolean hasActiveSession() {
    return getSessionId() != null;
  }
}
