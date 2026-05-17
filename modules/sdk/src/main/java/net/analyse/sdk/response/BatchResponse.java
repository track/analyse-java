package net.analyse.sdk.response;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * Response from the batch endpoint.
 */
@Getter
public class BatchResponse {

  private boolean success;
  private int accepted;
  private int failed;
  private Results results;
  private Counts counts;

  public List<JoinResult> getJoinResults() {
    return results != null && results.joins != null ? results.joins : Collections.emptyList();
  }

  public List<LeaveResult> getLeaveResults() {
    return results != null && results.leaves != null ? results.leaves : Collections.emptyList();
  }

  public List<EventResult> getEventResults() {
    return results != null && results.events != null ? results.events : Collections.emptyList();
  }

  @Getter
  public static class Results {
    private List<JoinResult> joins;
    private List<LeaveResult> leaves;
    private List<EventResult> events;
  }

  @Getter
  public static class JoinResult {
    private boolean success;
    private String sessionId;
    private String instanceId;
    private String playerId;
    private boolean isFirstJoin;
    private boolean attributed;
    private String error;

    public JoinResponse toResponse() {
      return new JoinResponse(success, sessionId, instanceId, playerId, isFirstJoin, attributed);
    }
  }

  @Getter
  public static class LeaveResult {
    private boolean success;
    private int duration;
    private String instanceId;
    private String sessionId;
    private String error;

    public LeaveResponse toResponse() {
      return new LeaveResponse(success, duration, instanceId, sessionId);
    }
  }

  @Getter
  public static class EventResult {
    private boolean success;
    private String event;
    private String instanceId;
    private String error;

    public EventResponse toResponse() {
      return new EventResponse(success, null, event, instanceId);
    }
  }

  @Getter
  public static class Counts {
    private int joins;
    private int leaves;
    private int events;
    private int clickhouseSessionRows;
    private int clickhouseEventRows;
  }
}
