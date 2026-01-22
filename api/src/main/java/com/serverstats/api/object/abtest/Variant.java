package com.serverstats.api.object.abtest;

/**
 * Represents a variant in an A/B test
 */
public interface Variant {

  /**
   * Get the variant key
   *
   * @return The variant key
   */
  String getKey();

  /**
   * Get the variant name
   *
   * @return The variant name
   */
  String getName();

  /**
   * Get the variant weight (percentage)
   *
   * @return The weight (0-100)
   */
  int getWeight();

  /**
   * Check if this variant has any actions to execute
   *
   * @return true if this variant has actions
   */
  boolean hasActions();
}
