package com.serverstats.bungeecord.listener;

import com.serverstats.bungeecord.ServerStatsBungee;
import com.serverstats.bungeecord.manager.SessionManager;
import com.serverstats.bungeecord.object.session.PlayerSession;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.ServerStatsClient;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.sdk.config.ServerStatsConfig;
import com.serverstats.sdk.request.JoinRequest;
import com.serverstats.sdk.request.LeaveRequest;
import com.serverstats.sdk.util.ProtocolVersionUtil;
import com.serverstats.sdk.response.JoinResponse;
import com.serverstats.sdk.response.LeaveResponse;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Listens for player events and sends analytics data
 */
public class PlayerListener implements Listener {

  private final ServerStatsBungee plugin;
  private final Logger logger;
  private final SessionManager sessionManager;
  private final Map<String, ServerStatsClient> serverClients = new HashMap<>();

  public PlayerListener(ServerStatsBungee plugin) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.sessionManager = plugin.getSessionManager();

    // Initialize API clients for each configured server
    initializeClients();
  }

  /**
   * Initialize API clients for each configured server
   */
  private void initializeClients() {
    plugin.getPluginConfig().getServers().forEach((serverName, serverConfig) -> {
      String apiKey = serverConfig.getApiKey();
      if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("anl_your_")) {
        ServerStatsConfig config = new ServerStatsConfig(apiKey);
        serverClients.put(serverName, new ServerStatsClient(config));
        logger.info(String.format("Initialized analytics client for server: %s", serverName));
      }
    });
  }

  /**
   * Get the API client for a server
   *
   * @param serverName The server name
   * @return The client, or empty if not configured
   */
  public Optional<ServerStatsClient> getClientForServer(String serverName) {
    return Optional.ofNullable(serverClients.get(serverName));
  }

  /**
   * Get the default API client (for the configured default server)
   *
   * @return The default client, or null if not configured
   */
  public ServerStatsClient getDefaultClient() {
    String defaultServer = plugin.getPluginConfig().getDefaultServer();
    if (defaultServer == null || defaultServer.isBlank()) {
      return null;
    }

    return serverClients.get(defaultServer);
  }

  @EventHandler
  public void onPostLogin(PostLoginEvent event) {
    ProxiedPlayer player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    // Get hostname from virtual host
    String hostname = getPlayerHostname(player);

    // Get player's IP address
    String ip = getPlayerIp(player);

    // Create session for this player
    sessionManager.createSession(uuid, hostname, ip);
    plugin.debug("Created session for %s (hostname: %s, ip: %s)",
        player.getName(), hostname, ip);
  }

  @EventHandler
  public void onServerConnected(ServerConnectedEvent event) {
    ProxiedPlayer player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getName();
    String newServerName = event.getServer().getInfo().getName();

    Optional<PlayerSession> sessionOpt = sessionManager.getSession(uuid);
    if (sessionOpt.isEmpty()) {
      logger.warning(String.format("No session found for player %s on server connect", username));
      return;
    }

    PlayerSession session = sessionOpt.get();

    // Handle leaving the previous server
    if (session.hasActiveSession()) {
      String previousServerName = session.getCurrentServer();
      sendLeaveEvent(uuid, username, session, previousServerName);
    }

    // Send join event to the new server (include client protocol version)
    int protocolVersion = player.getPendingConnection().getVersion();
    sendJoinEvent(uuid, username, session, newServerName, protocolVersion);
  }

  @EventHandler
  public void onPlayerDisconnect(PlayerDisconnectEvent event) {
    ProxiedPlayer player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getName();

    Optional<PlayerSession> sessionOpt = sessionManager.removeSession(uuid);
    if (sessionOpt.isEmpty()) {
      return;
    }

    PlayerSession session = sessionOpt.get();
    if (!session.hasActiveSession()) {
      return;
    }

    // Send leave event to the current server
    String currentServer = session.getCurrentServer();
    sendLeaveEvent(uuid, username, session, currentServer);
  }

  /**
   * Send a join event to the API
   *
   * @param uuid         Player UUID
   * @param username     Player username
   * @param session      Player session
   * @param serverName   Server the player connected to
   * @param protocolVersion Client protocol version (e.g. from getPendingConnection().getVersion()), or -1
   */
  private void sendJoinEvent(UUID uuid, String username, PlayerSession session, String serverName,
      int protocolVersion) {
    Optional<ServerStatsClient> clientOpt = getClientForServer(serverName);
    if (clientOpt.isEmpty()) {
      plugin.debug("Server %s not configured, skipping join event for %s", serverName, username);
      return;
    }

    ServerStatsClient client = clientOpt.get();

    // Check if player is a Bedrock player
    boolean isBedrock = plugin.getPluginConfig().isBedrock(username);

    // Convert protocol to "1.x.y" for the API
    String playerVersion = ProtocolVersionUtil.toVersionString(protocolVersion);

    JoinRequest request = new JoinRequest(uuid, username, session.getHostname(), session.getIp(),
        isBedrock, playerVersion);

    client.join(request, new ServerStatsCallback<>() {
      @Override
      public void onSuccess(JoinResponse response) {
        session.setCurrentSession(serverName, response.getSessionId());
        plugin.debug("Join event sent for %s on %s (sessionId: %s, bedrock: %s)",
            username, serverName, response.getSessionId(), isBedrock);
      }

      @Override
      public void onError(ServerStatsException exception) {
        logger.warning(String.format("Failed to send join event for %s on %s: %s",
            username, serverName, exception.getMessage()));
      }
    });
  }

  /**
   * Send a leave event to the API
   */
  private void sendLeaveEvent(UUID uuid, String username, PlayerSession session, String serverName) {
    if (!session.hasActiveSession()) {
      return;
    }

    Optional<ServerStatsClient> clientOpt = getClientForServer(serverName);
    if (clientOpt.isEmpty()) {
      return;
    }

    ServerStatsClient client = clientOpt.get();
    LeaveRequest request = new LeaveRequest(session.getSessionId());

    client.leave(request, new ServerStatsCallback<>() {
      @Override
      public void onSuccess(LeaveResponse response) {
        plugin.debug("Leave event sent for %s on %s (duration: %ds)",
            username, serverName, response.getDuration());
      }

      @Override
      public void onError(ServerStatsException exception) {
        logger.warning(String.format("Failed to send leave event for %s on %s: %s",
            username, serverName, exception.getMessage()));
      }
    });

    session.clearSession();
  }

  /**
   * Get the player's hostname they used to connect
   *
   * @param player The player
   * @return The hostname string
   */
  private String getPlayerHostname(ProxiedPlayer player) {
    InetSocketAddress virtualHost = player.getPendingConnection().getVirtualHost();
    if (virtualHost == null) {
      return "unknown";
    }

    return virtualHost.getHostString();
  }

  /**
   * Get the player's IP address
   *
   * @param player The player
   * @return The IP address string
   */
  private String getPlayerIp(ProxiedPlayer player) {
    return player.getAddress().getAddress().getHostAddress();
  }

  /**
   * Send a join event for an existing player (used during plugin reload)
   *
   * @param player     The player
   * @param serverName The server name
   */
  public void sendJoinEventForExistingPlayer(ProxiedPlayer player, String serverName) {
    UUID uuid = player.getUniqueId();
    String username = player.getName();

    Optional<PlayerSession> sessionOpt = sessionManager.getSession(uuid);
    if (sessionOpt.isEmpty()) {
      logger.warning(String.format("No session found for existing player %s", username));
      return;
    }

    PlayerSession session = sessionOpt.get();
    int protocolVersion = player.getPendingConnection().getVersion();
    sendJoinEvent(uuid, username, session, serverName, protocolVersion);
  }

  /**
   * Reinitialize API clients after a config reload
   */
  public void reinitialize() {
    initializeClients();
  }

  /**
   * Shutdown all API clients
   */
  public void shutdown() {
    serverClients.values().forEach(ServerStatsClient::shutdown);
    serverClients.clear();
  }
}
