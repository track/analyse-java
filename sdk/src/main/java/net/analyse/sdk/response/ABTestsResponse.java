package net.analyse.sdk.response;

import lombok.Getter;
import net.analyse.sdk.object.abtest.ABTest;
import java.util.List;

/**
 * Response containing active A/B tests
 */
@Getter
public class ABTestsResponse {

  private boolean success;
  private List<ABTest> tests;

  /**
   * Get an A/B test by its key
   *
   * @param key The test key
   * @return The test, or null if not found
   */
  public ABTest getTest(String key) {
    if (tests == null) {
      return null;
    }

    for (ABTest test : tests) {
      if (test.getKey().equals(key)) {
        return test;
      }
    }

    return null;
  }
}
