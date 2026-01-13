package net.analyse.sdk;

import net.analyse.sdk.config.AnalyseConfig;
import net.analyse.sdk.http.AnalyseHttpClient;
import net.analyse.sdk.request.ConversionRequest;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.request.HeartbeatRequest;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.request.LeaveRequest;
import net.analyse.sdk.response.ABTestsResponse;
import net.analyse.sdk.response.ConversionResponse;
import net.analyse.sdk.response.EventResponse;
import net.analyse.sdk.response.HeartbeatResponse;
import net.analyse.sdk.response.JoinResponse;
import net.analyse.sdk.response.LeaveResponse;
import net.analyse.sdk.response.VersionResponse;

/**
 * Main client for interacting with the Analyse API
 */
public class AnalyseClient {

  private static final String ENDPOINT_JOIN = "/v1/plugin/join";
  private static final String ENDPOINT_LEAVE = "/v1/plugin/leave";
  private static final String ENDPOINT_HEARTBEAT = "/v1/plugin/heartbeat";
  private static final String ENDPOINT_EVENT = "/v1/plugin/event";
  private static final String ENDPOINT_AB_TESTS = "/v1/plugin/ab-tests";
  private static final String ENDPOINT_CONVERSION = "/v1/plugin/conversion";
  private static final String ENDPOINT_VERSION = "/v1/plugin/version";

  private final AnalyseHttpClient httpClient;

  /**
   * Create a new client with the given configuration
   *
   * @param config The SDK configuration
   */
  public AnalyseClient(AnalyseConfig config) {
    this.httpClient = new AnalyseHttpClient(config);
  }

  /**
   * Send a player join event to the API
   *
   * @param request  The join request containing player details
   * @param callback The callback to invoke on success or failure
   */
  public void join(JoinRequest request, AnalyseCallback<JoinResponse> callback) {
    httpClient.post(ENDPOINT_JOIN, request, JoinResponse.class, callback);
  }

  /**
   * Send a player leave event to the API
   *
   * @param request  The leave request containing the session ID
   * @param callback The callback to invoke on success or failure
   */
  public void leave(LeaveRequest request, AnalyseCallback<LeaveResponse> callback) {
    httpClient.post(ENDPOINT_LEAVE, request, LeaveResponse.class, callback);
  }

  /**
   * Send a heartbeat to the API with current online players
   *
   * @param request  The heartbeat request containing online player UUIDs
   * @param callback The callback to invoke on success or failure
   */
  public void heartbeat(HeartbeatRequest request, AnalyseCallback<HeartbeatResponse> callback) {
    httpClient.post(ENDPOINT_HEARTBEAT, request, HeartbeatResponse.class, callback);
  }

  /**
   * Send a custom event to the API
   *
   * @param request  The event request containing event details
   * @param callback The callback to invoke on success or failure
   */
  public void trackEvent(EventRequest request, AnalyseCallback<EventResponse> callback) {
    httpClient.post(ENDPOINT_EVENT, request, EventResponse.class, callback);
  }

  /**
   * Fetch active A/B tests from the API
   *
   * @param callback The callback to invoke on success or failure
   */
  public void getABTests(AnalyseCallback<ABTestsResponse> callback) {
    httpClient.get(ENDPOINT_AB_TESTS, ABTestsResponse.class, callback);
  }

  /**
   * Track a conversion event for an A/B test
   *
   * @param request  The conversion request containing test and event details
   * @param callback The callback to invoke on success or failure
   */
  public void trackConversion(ConversionRequest request, AnalyseCallback<ConversionResponse> callback) {
    httpClient.post(ENDPOINT_CONVERSION, request, ConversionResponse.class, callback);
  }

  /**
   * Check for plugin updates
   *
   * @param callback The callback to invoke on success or failure
   */
  public void checkVersion(AnalyseCallback<VersionResponse> callback) {
    httpClient.get(ENDPOINT_VERSION, VersionResponse.class, callback);
  }

  /**
   * Shutdown the client and release resources.
   * Call this when the plugin is disabled.
   */
  public void shutdown() {
    httpClient.shutdown();
  }
}

