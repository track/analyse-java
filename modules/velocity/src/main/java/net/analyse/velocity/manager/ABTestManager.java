package net.analyse.velocity.manager;

import com.velocitypowered.api.proxy.Player;
import net.analyse.api.exception.AnalyseException;
import net.analyse.api.object.abtest.ABTest;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseClient;
import net.analyse.sdk.object.action.ActionData;
import net.analyse.sdk.request.ConversionRequest;
import net.analyse.sdk.response.ABTestsResponse;
import net.analyse.sdk.response.ConversionResponse;
import net.analyse.velocity.AnalyseVelocity;
import net.analyse.velocity.object.action.VelocityAction;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages A/B tests for the Velocity plugin.
 * Does not execute built-in variant actions.
 */
public class ABTestManager implements net.analyse.api.manager.ABTestManager {

  private static final long SYNC_INTERVAL_MINUTES = 5;

  private final AnalyseVelocity plugin;
  private final Map<String, net.analyse.sdk.object.abtest.ABTest> testsCache = new ConcurrentHashMap<>();

  public ABTestManager(AnalyseVelocity plugin) {
    this.plugin = plugin;
  }

  /**
   * Start the A/B test manager
   */
  public void start() {
    // Initial sync
    syncTests();

    // Schedule periodic sync
    plugin.getServer().getScheduler()
        .buildTask(plugin, this::syncTests)
        .repeat(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
        .schedule();

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
    AnalyseClient client = plugin.getClient();
    if (client == null) {
      return;
    }

    client.getABTests(new AnalyseCallback<>() {
      @Override
      public void onSuccess(ABTestsResponse response) {
        if (response.isSuccess() && response.getTests() != null) {
          testsCache.clear();
          for (net.analyse.sdk.object.abtest.ABTest test : response.getTests()) {
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
      public void onError(AnalyseException exception) {
        plugin.logWarning(String.format("Failed to sync A/B tests: %s", exception.getMessage()));
      }
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<net.analyse.sdk.object.abtest.ABTest> getActiveTests() {
    return List.copyOf(testsCache.values());
  }

  @Override
  public ABTest getTest(String testKey) {
    return testsCache.get(testKey);
  }

  @Override
  public String getVariant(UUID playerUuid, String testKey) {
    net.analyse.sdk.object.abtest.ABTest test = testsCache.get(testKey);
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
   * @param player The player who joined
   * @param firstJoin Whether this is the player's first join
   */
  public void processJoin(Player player, boolean firstJoin) {
    ABTest.Trigger trigger = firstJoin ? ABTest.Trigger.FIRST_JOIN : ABTest.Trigger.EVERY_JOIN;

    for (net.analyse.sdk.object.abtest.ABTest test : testsCache.values()) {
      if (!test.matchesTrigger(trigger)) {
        continue;
      }

      // Assign variant and execute actions
      var variant = test.assignVariant(player.getUniqueId());
      if (variant != null && variant.hasActions()) {
        executeActions(player, test, (net.analyse.sdk.object.abtest.Variant) variant);
      }
    }
  }

  /**
   * Process a custom event for A/B tests
   *
   * @param player The player who triggered the event
   * @param eventName The event name
   */
  public void processEvent(Player player, String eventName) {
    for (net.analyse.sdk.object.abtest.ABTest test : testsCache.values()) {
      if (!test.matchesEvent(eventName)) {
        continue;
      }

      // Assign variant and execute actions
      var variant = test.assignVariant(player.getUniqueId());
      if (variant != null && variant.hasActions()) {
        executeActions(player, test, (net.analyse.sdk.object.abtest.Variant) variant);
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
  private void executeActions(Player player, net.analyse.sdk.object.abtest.ABTest test,
                              net.analyse.sdk.object.abtest.Variant variant) {
    if (plugin.isDebugEnabled()) {
      plugin.logInfo(String.format("[DEBUG] Player %s assigned to variant '%s' for test '%s'",
          player.getUsername(), variant.getKey(), test.getKey()));
    }

    for (ActionData actionData : variant.getActions()) {
      VelocityAction action = VelocityAction.create(plugin, actionData);
      if (action != null) {
        action.execute(player);
      }
    }
  }

  @Override
  public void trackConversion(UUID playerUuid, String playerUsername, String testKey, String eventName) {
    net.analyse.sdk.object.abtest.ABTest test = testsCache.get(testKey);
    if (test == null) {
      return;
    }

    AnalyseClient client = plugin.getClient();
    if (client == null) {
      return;
    }

    var variant = test.assignVariant(playerUuid);
    String variantKey = variant != null ? variant.getKey() : null;

    ConversionRequest request = new ConversionRequest(
        testKey, variantKey, playerUuid, playerUsername, eventName, null
    );

    client.trackConversion(request, new AnalyseCallback<>() {
      @Override
      public void onSuccess(ConversionResponse response) {
        if (plugin.isDebugEnabled()) {
          plugin.logInfo(String.format("[DEBUG] Conversion tracked: test=%s, event=%s, player=%s",
              testKey, eventName, playerUsername));
        }
      }

      @Override
      public void onError(AnalyseException exception) {
        plugin.logWarning(String.format("Failed to track conversion: %s", exception.getMessage()));
      }
    });
  }
}
