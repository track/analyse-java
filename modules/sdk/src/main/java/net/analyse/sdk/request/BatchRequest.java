package net.analyse.sdk.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * Request payload for the batch endpoint.
 */
@Getter
public class BatchRequest {

  private final List<JoinRequest> joins;
  private final List<LeaveRequest> leaves;
  private final List<EventRequest> events;

  public BatchRequest(List<JoinRequest> joins, List<LeaveRequest> leaves, List<EventRequest> events) {
    this.joins = copy(joins);
    this.leaves = copy(leaves);
    this.events = copy(events);

    if (getTotalCount() == 0) {
      throw new IllegalArgumentException("Batch must contain at least one item");
    }

    if (getTotalCount() > 1000) {
      throw new IllegalArgumentException("Batch cannot contain more than 1000 items");
    }
  }

  public int getTotalCount() {
    return joins.size() + leaves.size() + events.size();
  }

  private static <T> List<T> copy(List<T> items) {
    if (items == null || items.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(new ArrayList<>(items));
  }
}
