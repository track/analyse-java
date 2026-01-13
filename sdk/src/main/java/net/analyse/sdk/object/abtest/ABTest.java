package net.analyse.sdk.object.abtest;

import lombok.Getter;
import java.util.List;
import java.util.UUID;

/**
 * Represents an A/B test configuration
 */
@Getter
public class ABTest {

  /**
   * Test trigger types
   */
  public enum Trigger {
    FIRST_JOIN,
    EVERY_JOIN,
    ON_COMMAND,
    ON_EVENT
  }

  /**
   * Test status
   */
  public enum Status {
    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED
  }

  private String id;
  private String key;
  private String name;
  private Trigger trigger;
  private String triggerCommand;
  private String triggerEvent;
  private boolean cancelCommand;
  private Status status;
  private List<Variant> variants;
  private String primaryMetric;

  /**
   * Check if this test is active
   *
   * @return true if the test is active
   */
  public boolean isActive() {
    return status == Status.ACTIVE;
  }

  /**
   * Check if this test matches a trigger type
   *
   * @param trigger The trigger to check
   * @return true if this test uses the given trigger
   */
  public boolean matchesTrigger(Trigger trigger) {
    return this.trigger == trigger;
  }

  /**
   * Check if this test is triggered by a specific command
   *
   * @param command The command to check (without leading slash)
   * @return true if this test is triggered by the command
   */
  public boolean matchesCommand(String command) {
    if (trigger != Trigger.ON_COMMAND || triggerCommand == null) {
      return false;
    }

    // Remove leading slash if present
    String normalizedCommand = command.startsWith("/") ? command.substring(1) : command;
    String normalizedTrigger = triggerCommand.startsWith("/") ? triggerCommand.substring(1) : triggerCommand;

    return normalizedCommand.equalsIgnoreCase(normalizedTrigger) ||
        normalizedCommand.toLowerCase().startsWith(normalizedTrigger.toLowerCase() + " ");
  }

  /**
   * Check if this test is triggered by a specific event
   *
   * @param eventName The event name to check
   * @return true if this test is triggered by the event
   */
  public boolean matchesEvent(String eventName) {
    if (trigger != Trigger.ON_EVENT || triggerEvent == null) {
      return false;
    }

    return triggerEvent.equalsIgnoreCase(eventName);
  }

  /**
   * Assign a variant to a player based on their UUID.
   * This is deterministic - the same player always gets the same variant.
   *
   * @param playerUuid The player's UUID
   * @return The assigned variant, or null if no variants exist
   */
  public Variant assignVariant(UUID playerUuid) {
    if (variants == null || variants.isEmpty()) {
      return null;
    }

    // Deterministic hash based on player UUID and test key
    String seed = playerUuid.toString() + ":" + key;
    int bucket = Math.abs(seed.hashCode() % 100);

    // Find which variant this bucket falls into
    int cumulative = 0;
    for (Variant variant : variants) {
      cumulative += variant.getWeight();
      if (bucket < cumulative) {
        return variant;
      }
    }

    // Fallback to first variant
    return variants.get(0);
  }

  /**
   * Get a variant by its key
   *
   * @param variantKey The variant key
   * @return The variant, or null if not found
   */
  public Variant getVariant(String variantKey) {
    if (variants == null) {
      return null;
    }

    for (Variant variant : variants) {
      if (variant.getKey().equals(variantKey)) {
        return variant;
      }
    }

    return null;
  }
}
