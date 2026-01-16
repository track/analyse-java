package net.analyse.hytale.manager;

import net.analyse.hytale.object.session.PlayerSession;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for player sessions
 */
public class SessionManager implements net.analyse.api.manager.SessionManager {

  private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

  /**
   * Create a new session for a player
   *
   * @param uuid The player's UUID
   * @param hostname The hostname the player used to connect
   * @param ip The player's IP address
   * @return The created session
   */
  public PlayerSession createSession(UUID uuid, String hostname, String ip) {
    PlayerSession session = new PlayerSession(uuid, hostname, ip);
    sessions.put(uuid, session);
    return session;
  }

  @Override
  public Optional<PlayerSession> getSession(UUID uuid) {
    return Optional.ofNullable(sessions.get(uuid));
  }

  /**
   * Remove a player's session
   *
   * @param uuid The player's UUID
   * @return The removed session, or empty if not found
   */
  public Optional<PlayerSession> removeSession(UUID uuid) {
    return Optional.ofNullable(sessions.remove(uuid));
  }

  @Override
  public boolean hasSession(UUID uuid) {
    return sessions.containsKey(uuid);
  }

  @Override
  public Collection<PlayerSession> getAllSessions() {
    return sessions.values();
  }

  @Override
  public int getSessionCount() {
    return sessions.size();
  }

  /**
   * Get all active sessions as a map
   *
   * @return Map of UUID to PlayerSession
   */
  public Map<UUID, PlayerSession> getActiveSessions() {
    return sessions;
  }
}
