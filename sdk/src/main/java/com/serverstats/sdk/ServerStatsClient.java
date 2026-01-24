package com.serverstats.sdk;

import com.serverstats.sdk.config.ServerStatsConfig;
import com.serverstats.sdk.http.ServerStatsHttpClient;
import com.serverstats.sdk.request.ConversionRequest;
import com.serverstats.sdk.request.EventRequest;
import com.serverstats.sdk.request.HeartbeatRequest;
import com.serverstats.sdk.request.JoinRequest;
import com.serverstats.sdk.request.LeaveRequest;
import com.serverstats.sdk.request.PlayerInfoRequest;
import com.serverstats.sdk.response.ABTestsResponse;
import com.serverstats.sdk.response.ConversionResponse;
import com.serverstats.sdk.response.EventResponse;
import com.serverstats.sdk.response.HeartbeatResponse;
import com.serverstats.sdk.response.JoinResponse;
import com.serverstats.sdk.response.LeaveResponse;
import com.serverstats.sdk.response.PlayerInfoResponse;
import com.serverstats.sdk.response.ServerInfoResponse;
import com.serverstats.sdk.response.VersionResponse;

/**
 * Main client for interacting with the ServerStats API
 */
public class ServerStatsClient {

  private static final String ENDPOINT_JOIN = "/v1/plugin/join";
  private static final String ENDPOINT_LEAVE = "/v1/plugin/leave";
  private static final String ENDPOINT_HEARTBEAT = "/v1/plugin/heartbeat";
  private static final String ENDPOINT_EVENT = "/v1/plugin/event";
  private static final String ENDPOINT_AB_TESTS = "/v1/plugin/ab-tests";
  private static final String ENDPOINT_CONVERSION = "/v1/plugin/conversion";
  private static final String ENDPOINT_VERSION = "/v1/plugin/version";
  private static final String ENDPOINT_SERVER_INFO = "/v1/plugin/info";
  private static final String ENDPOINT_PLAYER_INFO = "/v1/plugin/player";

  private final ServerStatsHttpClient httpClient;

  /**
   * Create a new client with the given configuration
   *
   * @param config The SDK configuration
   */
  public ServerStatsClient(ServerStatsConfig config) {
    this.httpClient = new ServerStatsHttpClient(config);
  }

  /**
   * Send a player join event to the API
   *
   * @param request  The join request containing player details
   * @param callback The callback to invoke on success or failure
   */
  public void join(JoinRequest request, ServerStatsCallback<JoinResponse> callback) {
    httpClient.post(ENDPOINT_JOIN, request, JoinResponse.class, callback);
  }

  /**
   * Send a player leave event to the API
   *
   * @param request  The leave request containing the session ID
   * @param callback The callback to invoke on success or failure
   */
  public void leave(LeaveRequest request, ServerStatsCallback<LeaveResponse> callback) {
    httpClient.post(ENDPOINT_LEAVE, request, LeaveResponse.class, callback);
  }

  /**
   * Send a heartbeat to the API with current online players
   *
   * @param request  The heartbeat request containing online player UUIDs
   * @param callback The callback to invoke on success or failure
   */
  public void heartbeat(HeartbeatRequest request, ServerStatsCallback<HeartbeatResponse> callback) {
    httpClient.post(ENDPOINT_HEARTBEAT, request, HeartbeatResponse.class, callback);
  }

  /**
   * Send a custom event to the API
   *
   * @param request  The event request containing event details
   * @param callback The callback to invoke on success or failure
   */
  public void trackEvent(EventRequest request, ServerStatsCallback<EventResponse> callback) {
    httpClient.post(ENDPOINT_EVENT, request, EventResponse.class, callback);
  }

  /**
   * Fetch active A/B tests from the API
   *
   * @param callback The callback to invoke on success or failure
   */
  public void getABTests(ServerStatsCallback<ABTestsResponse> callback) {
    httpClient.get(ENDPOINT_AB_TESTS, ABTestsResponse.class, callback);
  }

  /**
   * Track a conversion event for an A/B test
   *
   * @param request  The conversion request containing test and event details
   * @param callback The callback to invoke on success or failure
   */
  public void trackConversion(ConversionRequest request, ServerStatsCallback<ConversionResponse> callback) {
    httpClient.post(ENDPOINT_CONVERSION, request, ConversionResponse.class, callback);
  }

  /**
   * Check for plugin updates
   *
   * @param callback The callback to invoke on success or failure
   */
  public void checkVersion(ServerStatsCallback<VersionResponse> callback) {
    httpClient.get(ENDPOINT_VERSION, VersionResponse.class, callback);
  }

  /**
   * Get real-time server analytics information
   *
   * @param callback The callback to invoke on success or failure
   */
  public void getServerInfo(ServerStatsCallback<ServerInfoResponse> callback) {
    httpClient.get(ENDPOINT_SERVER_INFO, ServerInfoResponse.class, callback);
  }

  /**
   * Get detailed analytics information for a specific player
   *
   * @param request  The player info request containing the player UUID
   * @param callback The callback to invoke on success or failure
   */
  public void getPlayerInfo(PlayerInfoRequest request, ServerStatsCallback<PlayerInfoResponse> callback) {
    String endpoint = ENDPOINT_PLAYER_INFO + "/" + request.getUuid();
    httpClient.get(endpoint, PlayerInfoResponse.class, callback);
  }

  /**
   * Shutdown the client and release resources.
   * Call this when the plugin is disabled.
   */
  public void shutdown() {
    httpClient.shutdown();
  }
}

