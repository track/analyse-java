package net.analyse.paper.manager;

import net.analyse.paper.AnalysePlugin;
import net.analyse.paper.object.action.PaperAction;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.AnalyseException;
import net.analyse.sdk.object.abtest.ABTest;
import net.analyse.sdk.object.abtest.Variant;
import net.analyse.sdk.object.action.ActionData;
import net.analyse.sdk.request.ConversionRequest;
import net.analyse.sdk.response.ABTestsResponse;
import net.analyse.sdk.response.ConversionResponse;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages A/B tests for the Paper plugin.
 * Handles fetching, caching, and executing tests.
 */
public class ABTestManager {

  private static final long SYNC_INTERVAL_TICKS = 20 * 60 * 5; // 5 minutes

  private final AnalysePlugin plugin;
  private final Map<String, ABTest> testsCache = new ConcurrentHashMap<>();
  private BukkitTask syncTask;

  public ABTestManager(AnalysePlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Start the A/B test manager
   */
  public void start() {
    // Initial sync
    syncTests();

    // Schedule periodic sync
    syncTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
        plugin,
        this::syncTests,
        SYNC_INTERVAL_TICKS,
        SYNC_INTERVAL_TICKS
    );

    plugin.logInfo("A/B Test manager started");
  }

  /**
   * Stop the A/B test manager
   */
  public void stop() {
    if (syncTask != null) {
      syncTask.cancel();
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

    plugin.getClient().getABTests(new AnalyseCallback<>() {
      @Override
      public void onSuccess(ABTestsResponse response) {
        if (response.isSuccess() && response.getTests() != null) {
          testsCache.clear();
          for (ABTest test : response.getTests()) {
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

  /**
   * Get all active tests
   *
   * @return List of active tests
   */
  public List<ABTest> getActiveTests() {
    return List.copyOf(testsCache.values());
  }

  /**
   * Get a test by key
   *
   * @param testKey The test key
   * @return The test, or null if not found
   */
  public ABTest getTest(String testKey) {
    return testsCache.get(testKey);
  }

  /**
   * Get the variant assigned to a player for a test
   *
   * @param playerUuid The player's UUID
   * @param testKey    The test key
   * @return The variant key, or null if test not found
   */
  public String getVariant(UUID playerUuid, String testKey) {
    ABTest test = testsCache.get(testKey);
    if (test == null) {
      return null;
    }

    Variant variant = test.assignVariant(playerUuid);
    return variant != null ? variant.getKey() : null;
  }

  /**
   * Check if a test is active
   *
   * @param testKey The test key
   * @return true if test exists and is active
   */
  public boolean isTestActive(String testKey) {
    ABTest test = testsCache.get(testKey);
    return test != null && test.isActive();
  }

  /**
   * Process a player join event for A/B tests
   *
   * @param player    The player who joined
   * @param firstJoin Whether this is the player's first join
   */
  public void processJoin(Player player, boolean firstJoin) {
    ABTest.Trigger trigger = firstJoin ? ABTest.Trigger.FIRST_JOIN : ABTest.Trigger.EVERY_JOIN;

    for (ABTest test : testsCache.values()) {
      if (!test.matchesTrigger(trigger)) {
        continue;
      }

      // Assign variant and execute actions
      Variant variant = test.assignVariant(player.getUniqueId());
      if (variant != null && variant.hasActions()) {
        executeActions(player, test, variant);
      }
    }
  }

  /**
   * Process a command for A/B tests
   *
   * @param player  The player who ran the command
   * @param command The command (without leading slash)
   * @return true if the command should be cancelled
   */
  public boolean processCommand(Player player, String command) {
    for (ABTest test : testsCache.values()) {
      if (!test.matchesCommand(command)) {
        continue;
      }

      // Assign variant and execute actions
      Variant variant = test.assignVariant(player.getUniqueId());
      if (variant != null && variant.hasActions()) {
        executeActions(player, test, variant);
      }

      // Return whether to cancel the original command
      return test.isCancelCommand();
    }

    return false;
  }

  /**
   * Execute actions for a variant
   *
   * @param player  The player
   * @param test    The A/B test
   * @param variant The assigned variant
   */
  private void executeActions(Player player, ABTest test, Variant variant) {
    if (plugin.isDebugEnabled()) {
      plugin.logInfo(String.format("[DEBUG] Player %s assigned to variant '%s' for test '%s'",
          player.getName(), variant.getKey(), test.getKey()));
    }

    for (ActionData actionData : variant.getActions()) {
      PaperAction action = PaperAction.create(plugin, actionData);
      if (action != null) {
        action.execute(player);
      }
    }
  }

  /**
   * Track a conversion event
   *
   * @param playerUuid     The player's UUID
   * @param playerUsername The player's username
   * @param testKey        The test key
   * @param eventName      The conversion event name
   */
  public void trackConversion(UUID playerUuid, String playerUsername, String testKey, String eventName) {
    ABTest test = testsCache.get(testKey);
    if (test == null) {
      return;
    }

    Variant variant = test.assignVariant(playerUuid);
    String variantKey = variant != null ? variant.getKey() : null;

    ConversionRequest request = new ConversionRequest(
        testKey, variantKey, playerUuid, playerUsername, eventName, null
    );

    plugin.getClient().trackConversion(request, new AnalyseCallback<>() {
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
