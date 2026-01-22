package com.serverstats.hytale.manager;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.api.object.abtest.ABTest;
import com.serverstats.hytale.HytalePlugin;
import com.serverstats.hytale.object.action.HytaleAction;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.request.ConversionRequest;
import com.serverstats.sdk.response.ABTestsResponse;
import com.serverstats.sdk.response.ConversionResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages A/B tests for the Hytale plugin.
 * Handles fetching, caching, and executing tests.
 */
public class ABTestManager implements com.serverstats.api.manager.ABTestManager {

  private static final long SYNC_INTERVAL_MINUTES = 5;

  private final HytalePlugin plugin;
  private final ScheduledExecutorService scheduler;
  private final Map<String, com.serverstats.sdk.object.abtest.ABTest> testsCache = new ConcurrentHashMap<>();
  private ScheduledFuture<?> syncTask;

  public ABTestManager(HytalePlugin plugin, ScheduledExecutorService scheduler) {
    this.plugin = plugin;
    this.scheduler = scheduler;
  }

  /**
   * Start the A/B test manager
   */
  public void start() {
    // Initial sync
    syncTests();

    // Schedule periodic sync
    syncTask = scheduler.scheduleAtFixedRate(
        this::syncTests,
        SYNC_INTERVAL_MINUTES,
        SYNC_INTERVAL_MINUTES,
        TimeUnit.MINUTES
    );

    plugin.logInfo("A/B Test manager started");
  }

  /**
   * Stop the A/B test manager
   */
  public void stop() {
    if (syncTask != null) {
      syncTask.cancel(false);
      syncTask = null;
    }

    testsCache.clear();
  }

  /**
   * Sync tests from the API
   */
  private void syncTests() {
    if (plugin.getClient() == null) {
      return;
    }

    plugin.getClient().getABTests(new ServerStatsCallback<>() {
      @Override
      public void onSuccess(ABTestsResponse response) {
        if (response.isSuccess() && response.getTests() != null) {
          testsCache.clear();
          for (com.serverstats.sdk.object.abtest.ABTest test : response.getTests()) {
            if (test.isActive()) {
              testsCache.put(test.getKey(), test);
            }
          }

          if (plugin.isDebugEnabled()) {
            plugin.logInfo(String.format("[DEBUG] Synced %d active A/B tests", testsCache.size()));
          }
        }
      }

      @Override
      public void onError(ServerStatsException exception) {
        plugin.logWarning(String.format("Failed to sync A/B tests: %s", exception.getMessage()));
      }
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<com.serverstats.sdk.object.abtest.ABTest> getActiveTests() {
    return List.copyOf(testsCache.values());
  }

  @Override
  public ABTest getTest(String testKey) {
    return testsCache.get(testKey);
  }

  @Override
  public String getVariant(UUID playerUuid, String testKey) {
    com.serverstats.sdk.object.abtest.ABTest test = testsCache.get(testKey);
    if (test == null) {
      return null;
    }

    var variant = test.assignVariant(playerUuid);
    return variant != null ? variant.getKey() : null;
  }

  @Override
  public boolean isTestActive(String testKey) {
    ABTest test = testsCache.get(testKey);
    return test != null && test.isActive();
  }

  /**
   * Process a player join event for A/B tests
   *
   * @param playerRef The player who joined
   * @param firstJoin Whether this is the player's first join
   */
  public void processJoin(PlayerRef playerRef, boolean firstJoin) {
    ABTest.Trigger trigger = firstJoin ? ABTest.Trigger.FIRST_JOIN : ABTest.Trigger.EVERY_JOIN;

    for (com.serverstats.sdk.object.abtest.ABTest test : testsCache.values()) {
      if (!test.matchesTrigger(trigger)) {
        continue;
      }

      // Assign variant and execute actions
      var variant = test.assignVariant(playerRef.getUuid());
      if (variant != null && variant.hasActions()) {
        executeActions(playerRef, test, (com.serverstats.sdk.object.abtest.Variant) variant);
      }
    }
  }

  /**
   * Process a custom event for A/B tests
   *
   * @param playerRef The player who triggered the event
   * @param eventName The event name
   */
  public void processEvent(PlayerRef playerRef, String eventName) {
    for (com.serverstats.sdk.object.abtest.ABTest test : testsCache.values()) {
      if (!test.matchesEvent(eventName)) {
        continue;
      }

      // Assign variant and execute actions
      var variant = test.assignVariant(playerRef.getUuid());
      if (variant != null && variant.hasActions()) {
        executeActions(playerRef, test, (com.serverstats.sdk.object.abtest.Variant) variant);
      }
    }
  }

  /**
   * Execute actions for a variant
   *
   * @param playerRef The player
   * @param test The A/B test
   * @param variant The assigned variant
   */
  private void executeActions(PlayerRef playerRef, com.serverstats.sdk.object.abtest.ABTest test,
                              com.serverstats.sdk.object.abtest.Variant variant) {
    if (plugin.isDebugEnabled()) {
      plugin.logInfo(String.format("[DEBUG] Player %s assigned to variant '%s' for test '%s'",
          playerRef.getUsername(), variant.getKey(), test.getKey()));
    }

    for (ActionData actionData : variant.getActions()) {
      HytaleAction action = HytaleAction.create(plugin, actionData);
      if (action != null) {
        action.execute(playerRef);
      }
    }
  }

  @Override
  public void trackConversion(UUID playerUuid, String playerUsername, String testKey, String eventName) {
    com.serverstats.sdk.object.abtest.ABTest test = testsCache.get(testKey);
    if (test == null) {
      return;
    }

    var variant = test.assignVariant(playerUuid);
    String variantKey = variant != null ? variant.getKey() : null;

    ConversionRequest request = new ConversionRequest(
        testKey, variantKey, playerUuid, playerUsername, eventName, null
    );

    plugin.getClient().trackConversion(request, new ServerStatsCallback<>() {
      @Override
      public void onSuccess(ConversionResponse response) {
        if (plugin.isDebugEnabled()) {
          plugin.logInfo(String.format("[DEBUG] Conversion tracked: test=%s, event=%s, player=%s",
              testKey, eventName, playerUsername));
        }
      }

      @Override
      public void onError(ServerStatsException exception) {
        plugin.logWarning(String.format("Failed to track conversion: %s", exception.getMessage()));
      }
    });
  }
}
