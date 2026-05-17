package net.analyse.sdk.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.analyse.api.exception.AnalyseException;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.request.BatchRequest;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.request.LeaveRequest;
import net.analyse.sdk.response.EventResponse;
import net.analyse.sdk.response.JoinResponse;
import net.analyse.sdk.response.LeaveResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AnalyseClientSendModeTest {

  private MockWebServer server;

  @AfterEach
  void tearDown() throws IOException {
    if (server != null) {
      server.shutdown();
    }
  }

  @Test
  void parsesSendModeFromConfig() {
    assertEquals(SendMode.SINGLE, SendMode.fromConfig("SINGLE"));
    assertEquals(SendMode.BATCH, SendMode.fromConfig("BATCH"));
    assertEquals(SendMode.SINGLE, SendMode.fromConfig("invalid"));
    assertEquals(SendMode.SINGLE, SendMode.fromConfig(null));
  }

  @Test
  void joinRequestKeepsCompatibilityAndSupportsInstanceId() {
    UUID uuid = UUID.randomUUID();

    JoinRequest defaultInstance = new JoinRequest(uuid, "Player", "play.example.com", "127.0.0.1", false);
    JoinRequest explicitInstance = new JoinRequest(uuid, "Player", "play.example.com", "127.0.0.1", false,
        "1.20.4", "survival-1");

    assertEquals("default", defaultInstance.getInstanceId());
    assertEquals("survival-1", explicitInstance.getInstanceId());
  }

  @Test
  void leaveRequestAcceptsSessionIdOrPlayerUuid() {
    UUID uuid = UUID.randomUUID();

    LeaveRequest sessionLeave = new LeaveRequest("session-id");
    LeaveRequest fallbackLeave = new LeaveRequest(uuid, "survival-1");

    assertEquals("session-id", sessionLeave.getSessionId());
    assertEquals("default", sessionLeave.getInstanceId());
    assertEquals(uuid.toString(), fallbackLeave.getPlayerUuid());
    assertEquals("survival-1", fallbackLeave.getInstanceId());
    assertThrows(IllegalArgumentException.class, () -> new LeaveRequest(null, null, "default"));
  }

  @Test
  void batchRequestGroupsItemsAndRejectsInvalidCounts() {
    UUID uuid = UUID.randomUUID();
    JoinRequest join = new JoinRequest(uuid, "Player", "play.example.com", "127.0.0.1", false);
    LeaveRequest leave = new LeaveRequest("session-id");
    EventRequest event = new EventRequest("plugin.test", uuid, "Player", Collections.emptyMap(), 1.0, "default");

    BatchRequest request = new BatchRequest(
        Collections.singletonList(join),
        Collections.singletonList(leave),
        Collections.singletonList(event)
    );

    assertEquals(1, request.getJoins().size());
    assertEquals(1, request.getLeaves().size());
    assertEquals(1, request.getEvents().size());
    assertEquals(3, request.getTotalCount());
    assertThrows(IllegalArgumentException.class,
        () -> new BatchRequest(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
  }

  @Test
  void batchModeSendsOneBatchAndMapsCallbacks() throws Exception {
    server = new MockWebServer();
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("{\"success\":true,\"accepted\":3,\"failed\":0,\"results\":{"
            + "\"joins\":[{\"success\":true,\"sessionId\":\"session-1\",\"instanceId\":\"survival-1\","
            + "\"playerId\":\"player-1\",\"isFirstJoin\":false,\"attributed\":false}],"
            + "\"leaves\":[{\"success\":true,\"duration\":12,\"instanceId\":\"survival-1\","
            + "\"sessionId\":\"session-1\"}],"
            + "\"events\":[{\"success\":true,\"event\":\"plugin.test\",\"instanceId\":\"survival-1\"}]"
            + "},\"counts\":{\"joins\":1,\"leaves\":1,\"events\":1}}"));
    server.start();

    AnalyseClient client = new AnalyseClient(new AnalyseConfig(
        "anl_test",
        baseUrl(),
        SendMode.BATCH,
        new BatchConfig(3, 10, 100, 0)
    ));

    CallbackProbe<JoinResponse> joinProbe = new CallbackProbe<>();
    CallbackProbe<LeaveResponse> leaveProbe = new CallbackProbe<>();
    CallbackProbe<EventResponse> eventProbe = new CallbackProbe<>();
    UUID uuid = UUID.randomUUID();

    client.join(new JoinRequest(uuid, "Player", "play.example.com", "127.0.0.1", false, null, "survival-1"),
        joinProbe);
    client.leave(new LeaveRequest("session-1", uuid, "survival-1"), leaveProbe);
    client.trackEvent(new EventRequest("plugin.test", uuid, "Player", Collections.emptyMap(), 1.0, "survival-1"),
        eventProbe);

    assertTrue(joinProbe.await());
    assertTrue(leaveProbe.await());
    assertTrue(eventProbe.await());

    RecordedRequest recorded = server.takeRequest(1, TimeUnit.SECONDS);
    assertEquals("/v1/plugin/batch", recorded.getPath());
    assertEquals("session-1", joinProbe.response.getSessionId());
    assertEquals(12, leaveProbe.response.getDuration());
    assertEquals("plugin.test", eventProbe.response.getEvent());

    client.shutdown();
  }

  @Test
  void batchModeRetriesTemporaryFailures() throws Exception {
    server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(500).setBody("temporary"));
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("{\"success\":true,\"accepted\":1,\"failed\":0,\"results\":{"
            + "\"joins\":[{\"success\":true,\"sessionId\":\"session-2\",\"instanceId\":\"default\"}],"
            + "\"leaves\":[],\"events\":[]},\"counts\":{\"joins\":1,\"leaves\":0,\"events\":0}}"));
    server.start();

    AnalyseClient client = new AnalyseClient(new AnalyseConfig(
        "anl_test",
        baseUrl(),
        SendMode.BATCH,
        new BatchConfig(1, 10, 100, 1)
    ));

    CallbackProbe<JoinResponse> probe = new CallbackProbe<>();
    client.join(new JoinRequest(UUID.randomUUID(), "Player", "play.example.com", "127.0.0.1", false), probe);

    assertTrue(probe.await(4, TimeUnit.SECONDS));
    assertEquals("session-2", probe.response.getSessionId());
    assertEquals(2, server.getRequestCount());

    client.shutdown();
  }

  @Test
  void batchModeCallsCallbackWithErrorAfterFinalFailure() throws Exception {
    server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(500).setBody("temporary"));
    server.start();

    AnalyseClient client = new AnalyseClient(new AnalyseConfig(
        "anl_test",
        baseUrl(),
        SendMode.BATCH,
        new BatchConfig(1, 10, 100, 0)
    ));

    CallbackProbe<JoinResponse> probe = new CallbackProbe<>();
    client.join(new JoinRequest(UUID.randomUUID(), "Player", "play.example.com", "127.0.0.1", false), probe);

    assertTrue(probe.awaitAllowingError());
    assertEquals(500, probe.error.getStatusCode());

    client.shutdown();
  }

  @Test
  void singleModeUsesIndividualEndpoints() throws Exception {
    server = new MockWebServer();
    server.enqueue(json("{\"success\":true,\"sessionId\":\"session-1\"}"));
    server.enqueue(json("{\"success\":true,\"duration\":5}"));
    server.enqueue(json("{\"success\":true,\"eventId\":\"event-1\"}"));
    server.start();

    AnalyseClient client = new AnalyseClient(new AnalyseConfig(
        "anl_test",
        baseUrl(),
        SendMode.SINGLE,
        new BatchConfig()
    ));

    UUID uuid = UUID.randomUUID();
    CallbackProbe<JoinResponse> joinProbe = new CallbackProbe<>();
    CallbackProbe<LeaveResponse> leaveProbe = new CallbackProbe<>();
    CallbackProbe<EventResponse> eventProbe = new CallbackProbe<>();

    client.join(new JoinRequest(uuid, "Player", "play.example.com", "127.0.0.1", false), joinProbe);
    client.leave(new LeaveRequest("session-1"), leaveProbe);
    client.trackEvent(new EventRequest("plugin.test", uuid, "Player", Collections.emptyMap(), null, "default"),
        eventProbe);

    assertTrue(joinProbe.await());
    assertTrue(leaveProbe.await());
    assertTrue(eventProbe.await());

    Set<String> paths = new HashSet<>();
    paths.add(server.takeRequest(1, TimeUnit.SECONDS).getPath());
    paths.add(server.takeRequest(1, TimeUnit.SECONDS).getPath());
    paths.add(server.takeRequest(1, TimeUnit.SECONDS).getPath());
    assertTrue(paths.contains("/v1/plugin/join"));
    assertTrue(paths.contains("/v1/plugin/leave"));
    assertTrue(paths.contains("/v1/plugin/event"));
    assertFalse(client.isBatchMode());

    client.shutdown();
  }

  private MockResponse json(String body) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body);
  }

  private String baseUrl() {
    String url = server.url("/").toString();
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private static class CallbackProbe<T> implements AnalyseCallback<T> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private T response;
    private AnalyseException error;

    @Override
    public void onSuccess(T response) {
      this.response = response;
      latch.countDown();
    }

    @Override
    public void onError(AnalyseException exception) {
      this.error = exception;
      latch.countDown();
    }

    private boolean await() throws InterruptedException {
      return await(2, TimeUnit.SECONDS);
    }

    private boolean await(long timeout, TimeUnit unit) throws InterruptedException {
      boolean completed = latch.await(timeout, unit);
      if (error != null) {
        throw new AssertionError("Callback failed", error);
      }
      return completed;
    }

    private boolean awaitAllowingError() throws InterruptedException {
      return latch.await(2, TimeUnit.SECONDS);
    }
  }
}
