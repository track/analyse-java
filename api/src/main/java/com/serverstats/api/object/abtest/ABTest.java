package com.serverstats.api.object.abtest;

import java.util.List;
import java.util.UUID;

/**
 * Represents an A/B test configuration
 */
public interface ABTest {

  /**
   * Test trigger types
   */
  enum Trigger {
    FIRST_JOIN,
    EVERY_JOIN,
    ON_COMMAND,
    ON_EVENT
  }

  /**
   * Test status
   */
  enum Status {
    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED
  }

  /**
   * Get the test ID
   *
   * @return The test ID
   */
  String getId();

  /**
   * Get the test key
   *
   * @return The test key
   */
  String getKey();

  /**
   * Get the test name
   *
   * @return The test name
   */
  String getName();

  /**
   * Get the trigger type
   *
   * @return The trigger type
   */
  Trigger getTrigger();

  /**
   * Get the trigger command (if trigger is ON_COMMAND)
   *
   * @return The trigger command, or null
   */
  String getTriggerCommand();

  /**
   * Get the trigger event (if trigger is ON_EVENT)
   *
   * @return The trigger event, or null
   */
  String getTriggerEvent();

  /**
   * Check if the command should be cancelled when triggered
   *
   * @return true if the command should be cancelled
   */
  boolean isCancelCommand();

  /**
   * Get the test status
   *
   * @return The test status
   */
  Status getStatus();

  /**
   * Get all variants in this test
   *
   * @return List of variants
   */
  List<? extends Variant> getVariants();

  /**
   * Get the primary metric for this test
   *
   * @return The primary metric name
   */
  String getPrimaryMetric();

  /**
   * Check if this test is active
   *
   * @return true if the test is active
   */
  default boolean isActive() {
    return getStatus() == Status.ACTIVE;
  }

  /**
   * Check if this test matches a trigger type
   *
   * @param trigger The trigger to check
   * @return true if this test uses the given trigger
   */
  default boolean matchesTrigger(Trigger trigger) {
    return getTrigger() == trigger;
  }

  /**
   * Check if this test is triggered by a specific command
   *
   * @param command The command to check (without leading slash)
   * @return true if this test is triggered by the command
   */
  default boolean matchesCommand(String command) {
    if (getTrigger() != Trigger.ON_COMMAND || getTriggerCommand() == null) {
      return false;
    }

    String normalizedCommand = command.startsWith("/") ? command.substring(1) : command;
    String normalizedTrigger = getTriggerCommand().startsWith("/")
        ? getTriggerCommand().substring(1)
        : getTriggerCommand();

    return normalizedCommand.equalsIgnoreCase(normalizedTrigger) ||
        normalizedCommand.toLowerCase().startsWith(normalizedTrigger.toLowerCase() + " ");
  }

  /**
   * Check if this test is triggered by a specific event
   *
   * @param eventName The event name to check
   * @return true if this test is triggered by the event
   */
  default boolean matchesEvent(String eventName) {
    if (getTrigger() != Trigger.ON_EVENT || getTriggerEvent() == null) {
      return false;
    }

    return getTriggerEvent().equalsIgnoreCase(eventName);
  }

  /**
   * Assign a variant to a player based on their UUID.
   * This is deterministic - the same player always gets the same variant.
   *
   * @param playerUuid The player's UUID
   * @return The assigned variant, or null if no variants exist
   */
  Variant assignVariant(UUID playerUuid);

  /**
   * Get a variant by its key
   *
   * @param variantKey The variant key
   * @return The variant, or null if not found
   */
  Variant getVariant(String variantKey);
}
