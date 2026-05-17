package net.analyse.sdk.batch;

import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.request.LeaveRequest;
import net.analyse.sdk.response.EventResponse;
import net.analyse.sdk.response.JoinResponse;
import net.analyse.sdk.response.LeaveResponse;

/**
 * A queued analytics item waiting to be sent through the batch endpoint.
 */
public class BatchItem<T> {

  public enum Type {
    JOIN,
    LEAVE,
    EVENT
  }

  private final Type type;
  private final Object request;
  private final AnalyseCallback<T> callback;

  private BatchItem(Type type, Object request, AnalyseCallback<T> callback) {
    this.type = type;
    this.request = request;
    this.callback = callback;
  }

  public static BatchItem<JoinResponse> join(JoinRequest request, AnalyseCallback<JoinResponse> callback) {
    return new BatchItem<>(Type.JOIN, request, callback);
  }

  public static BatchItem<LeaveResponse> leave(LeaveRequest request, AnalyseCallback<LeaveResponse> callback) {
    return new BatchItem<>(Type.LEAVE, request, callback);
  }

  public static BatchItem<EventResponse> event(EventRequest request, AnalyseCallback<EventResponse> callback) {
    return new BatchItem<>(Type.EVENT, request, callback);
  }

  public Type getType() {
    return type;
  }

  public Object getRequest() {
    return request;
  }

  public AnalyseCallback<T> getCallback() {
    return callback;
  }
}
