package com.serverstats.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.ServerStatsClient;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.sdk.config.ServerStatsConfig;
import com.serverstats.sdk.request.JoinRequest;
import com.serverstats.sdk.request.LeaveRequest;
import com.serverstats.sdk.util.ProtocolVersionUtil;
import com.serverstats.sdk.response.JoinResponse;
import com.serverstats.sdk.response.LeaveResponse;
import com.serverstats.velocity.ServerStatsVelocity;
import com.serverstats.velocity.manager.SessionManager;
import com.serverstats.velocity.object.session.PlayerSession;
import org.slf4j.Logger;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens for player events and sends analytics data
 */
public class PlayerListener {

  private final ServerStatsVelocity plugin;
  private final Logger logger;
  private final SessionManager sessionManager;
  private final Map<String, ServerStatsClient> serverClients = new HashMap<>();

  public PlayerListener(ServerStatsVelocity plugin) {
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
   * Get an API client for use with the ServerStats API.
   * Returns the configured default server's client if set, otherwise returns the first available client.
   *
   * @return An available client, or null if none configured
   */
  public ServerStatsClient getAvailableClient() {
    // Try configured default server first
    String defaultServer = plugin.getPluginConfig().getDefaultServer();
    if (defaultServer != null && !defaultServer.isBlank()) {
      ServerStatsClient client = serverClients.get(defaultServer);
      if (client != null) {
        return client;
      }
    }

    // Fall back to any available client
    return serverClients.values().stream().findFirst().orElse(null);
  }

  /**
   * Check if any API clients are configured and available
   *
   * @return true if at least one client is available
   */
  public boolean hasAnyClient() {
    return !serverClients.isEmpty();
  }

  @Subscribe
  public void onLogin(LoginEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    // Get hostname from virtual host
    String hostname = player.getVirtualHost()
        .map(InetSocketAddress::getHostString)
        .orElse("unknown");

    // Get player's IP address
    String ip = getPlayerIp(player);

    // Create session for this player
    sessionManager.createSession(uuid, hostname, ip);
    plugin.debug("Created session for %s (hostname: %s, ip: %s)",
        player.getUsername(), hostname, ip);
  }

  @Subscribe
  public void onServerConnected(ServerConnectedEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getUsername();
    String newServerName = event.getServer().getServerInfo().getName();

    Optional<PlayerSession> sessionOpt = sessionManager.getSession(uuid);
    if (sessionOpt.isEmpty()) {
      logger.warn(String.format("No session found for player %s on server connect", username));
      return;
    }

    PlayerSession session = sessionOpt.get();

    // Handle leaving the previous server
    Optional<RegisteredServer> previousServer = event.getPreviousServer();
    if (previousServer.isPresent()) {
      String previousServerName = previousServer.get().getServerInfo().getName();
      sendLeaveEvent(uuid, username, session, previousServerName);
    }

    // Send join event to the new server (include client protocol version)
    int protocolVersion = player.getProtocolVersion().getProtocol();
    sendJoinEvent(uuid, username, session, newServerName, protocolVersion);
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getUsername();

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
   * @param uuid            Player UUID
   * @param username        Player username
   * @param session         Player session
   * @param serverName      Server the player connected to
   * @param protocolVersion Client protocol version (e.g. from getProtocolVersion().getProtocol()), or -1
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
        logger.warn(String.format("Failed to send join event for %s on %s: %s",
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
        logger.warn(String.format("Failed to send leave event for %s on %s: %s",
            username, serverName, exception.getMessage()));
      }
    });

    session.clearSession();
  }

  /**
   * Get the player's IP address
   *
   * @param player The player
   * @return The IP address string
   */
  private String getPlayerIp(Player player) {
    return player.getRemoteAddress().getAddress().getHostAddress();
  }

  /**
   * Send a join event for an existing player (used during plugin reload)
   *
   * @param player     The player
   * @param serverName The server name
   */
  public void sendJoinEventForExistingPlayer(Player player, String serverName) {
    UUID uuid = player.getUniqueId();
    String username = player.getUsername();

    Optional<PlayerSession> sessionOpt = sessionManager.getSession(uuid);
    if (sessionOpt.isEmpty()) {
      logger.warn(String.format("No session found for existing player %s", username));
      return;
    }

    PlayerSession session = sessionOpt.get();
    int protocolVersion = player.getProtocolVersion().getProtocol();
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
