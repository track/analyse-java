package com.serverstats.sdk.object.abtest;

import lombok.Getter;
import com.serverstats.sdk.object.action.ActionData;
import java.util.List;

/**
 * Concrete implementation of a variant in an A/B test
 */
@Getter
public class Variant implements com.serverstats.api.object.abtest.Variant {

  private String key;
  private String name;
  private int weight;
  private List<ActionData> actions;

  @Override
  public boolean hasActions() {
    return actions != null && !actions.isEmpty();
  }
}
