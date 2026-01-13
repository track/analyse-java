package net.analyse.sdk.object.abtest;

import lombok.Getter;
import net.analyse.sdk.object.action.ActionData;
import java.util.List;

/**
 * Represents a variant in an A/B test
 */
@Getter
public class Variant {

  private String key;
  private String name;
  private int weight;
  private List<ActionData> actions;

  /**
   * Check if this variant has any actions to execute
   *
   * @return true if this variant has actions
   */
  public boolean hasActions() {
    return actions != null && !actions.isEmpty();
  }
}
