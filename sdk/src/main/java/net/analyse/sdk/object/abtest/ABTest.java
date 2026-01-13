package net.analyse.sdk.object.abtest;

import lombok.Getter;
import java.util.List;
import java.util.UUID;

/**
 * Concrete implementation of an A/B test configuration
 */
@Getter
public class ABTest implements net.analyse.api.object.abtest.ABTest {

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

  @Override
  public List<Variant> getVariants() {
    return variants;
  }

  @Override
  public net.analyse.api.object.abtest.Variant assignVariant(UUID playerUuid) {
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

  @Override
  public net.analyse.api.object.abtest.Variant getVariant(String variantKey) {
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
