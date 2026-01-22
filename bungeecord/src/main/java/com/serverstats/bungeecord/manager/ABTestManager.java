package com.serverstats.bungeecord.manager;

import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.api.object.abtest.ABTest;
import com.serverstats.bungeecord.ServerStatsBungee;
import com.serverstats.bungeecord.object.action.BungeeAction;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.ServerStatsClient;
import com.serverstats.sdk.object.abtest.Variant;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.request.ConversionRequest;
import com.serverstats.sdk.response.ABTestsResponse;
import com.serverstats.sdk.response.ConversionResponse;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages A/B tests for the BungeeCord plugin.
 * Supports SEND_MESSAGE and RUN_COMMAND actions on proxies.
 */
public class ABTestManager implements com.serverstats.api.manager.ABTestManager {

  private static final long SYNC_INTERVAL_MINUTES = 5;

  private final ServerStatsBungee plugin;
  private final Map<String, com.serverstats.sdk.object.abtest.ABTest> testsCache = new ConcurrentHashMap<>();

  public ABTestManager(ServerStatsBungee plugin) {
    this.plugin = plugin;
  }

  /**
   * Start the A/B test manager
   */
  public void start() {
    // Initial sync
    syncTests();

    // Schedule periodic sync
    plugin.getProxy().getScheduler().schedule(
        plugin,
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
    testsCache.clear();
  }

  /**
   * Sync tests from the API
   */
  private void syncTests() {
    ServerStatsClient client = plugin.getClient();
    if (client == null) {
      return;
    }

    client.getABTests(new ServerStatsCallback<>() {
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

    com.serverstats.api.object.abtest.Variant variant = test.assignVariant(playerUuid);
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
   * @param player The player who joined
   * @param firstJoin Whether this is the player's first join
   */
  public void processJoin(ProxiedPlayer player, boolean firstJoin) {
    ABTest.Trigger trigger = firstJoin ? ABTest.Trigger.FIRST_JOIN : ABTest.Trigger.EVERY_JOIN;

    for (com.serverstats.sdk.object.abtest.ABTest test : testsCache.values()) {
      if (!test.matchesTrigger(trigger)) {
        continue;
      }

      // Assign variant and execute actions
      var variant = test.assignVariant(player.getUniqueId());
      if (variant != null && variant.hasActions()) {
        executeActions(player, test, (com.serverstats.sdk.object.abtest.Variant) variant);
      }
    }
  }

  /**
   * Process a custom event for A/B tests
   *
   * @param player The player who triggered the event
   * @param eventName The event name
   */
  public void processEvent(ProxiedPlayer player, String eventName) {
    for (com.serverstats.sdk.object.abtest.ABTest test : testsCache.values()) {
      if (!test.matchesEvent(eventName)) {
        continue;
      }

      // Assign variant and execute actions
      var variant = test.assignVariant(player.getUniqueId());
      if (variant != null && variant.hasActions()) {
        executeActions(player, test, (com.serverstats.sdk.object.abtest.Variant) variant);
      }
    }
  }

  /**
   * Execute actions for a variant
   *
   * @param player The player
   * @param test The A/B test
   * @param variant The assigned variant
   */
  private void executeActions(ProxiedPlayer player, com.serverstats.sdk.object.abtest.ABTest test,
                              com.serverstats.sdk.object.abtest.Variant variant) {
    if (plugin.isDebugEnabled()) {
      plugin.logInfo(String.format("[DEBUG] Player %s assigned to variant '%s' for test '%s'",
          player.getName(), variant.getKey(), test.getKey()));
    }

    for (ActionData actionData : variant.getActions()) {
      BungeeAction action = BungeeAction.create(plugin, actionData);
      if (action != null) {
        action.execute(player);
      }
    }
  }

  @Override
  public void trackConversion(UUID playerUuid, String playerUsername, String testKey, String eventName) {
    com.serverstats.sdk.object.abtest.ABTest test = testsCache.get(testKey);
    if (test == null) {
      return;
    }

    ServerStatsClient client = plugin.getClient();
    if (client == null) {
      return;
    }

    var variant = test.assignVariant(playerUuid);
    String variantKey = variant != null ? variant.getKey() : null;

    ConversionRequest request = new ConversionRequest(
        testKey, variantKey, playerUuid, playerUsername, eventName, null
    );

    client.trackConversion(request, new ServerStatsCallback<>() {
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
