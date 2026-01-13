package net.analyse.api.manager;

import net.analyse.api.session.PlayerSession;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages player analytics sessions
 */
public interface SessionManager {

  /**
   * Get a player's session by UUID
   *
   * @param uuid The player's UUID
   * @return The session, or empty if not found
   */
  Optional<? extends PlayerSession> getSession(UUID uuid);

  /**
   * Check if a player has an active session
   *
   * @param uuid The player's UUID
   * @return true if the player has a session
   */
  default boolean hasSession(UUID uuid) {
    return getSession(uuid).isPresent();
  }

  /**
   * Get all active sessions
   *
   * @return Collection of all sessions
   */
  Collection<? extends PlayerSession> getAllSessions();

  /**
   * Get the count of active sessions
   *
   * @return The session count
   */
  default int getSessionCount() {
    return getAllSessions().size();
  }
}
