package net.analyse.sdk.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import net.analyse.api.exception.AnalyseException;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.sdk.config.BatchConfig;
import net.analyse.sdk.http.AnalyseHttpClient;
import net.analyse.sdk.request.BatchRequest;
import net.analyse.sdk.request.EventRequest;
import net.analyse.sdk.request.JoinRequest;
import net.analyse.sdk.request.LeaveRequest;
import net.analyse.sdk.response.BatchResponse;
import net.analyse.sdk.response.EventResponse;
import net.analyse.sdk.response.JoinResponse;
import net.analyse.sdk.response.LeaveResponse;

/**
 * Queues analytics items and flushes them to the batch endpoint on a background thread.
 */
public class BatchDispatcher {

  private static final int SHUTDOWN_WAIT_MILLIS = 5000;

  private final AnalyseHttpClient httpClient;
  private final BatchConfig config;
  private final String endpoint;
  private final LinkedBlockingQueue<BatchItem<?>> queue;
  private final Object monitor = new Object();
  private final Thread worker;

  private volatile boolean running = true;
  private volatile boolean flushing = false;
  private volatile boolean flushRequested = false;

  public BatchDispatcher(AnalyseHttpClient httpClient, BatchConfig config, String endpoint) {
    this.httpClient = httpClient;
    this.config = config;
    this.endpoint = endpoint;
    this.queue = new LinkedBlockingQueue<>(config.getMaxQueueSize());
    this.worker = new Thread(this::run, "analyse-batch-worker");
    this.worker.setDaemon(true);
    this.worker.start();
  }

  public void enqueueJoin(JoinRequest request, AnalyseCallback<JoinResponse> callback) {
    enqueue(BatchItem.join(request, callback));
  }

  public void enqueueLeave(LeaveRequest request, AnalyseCallback<LeaveResponse> callback) {
    enqueue(BatchItem.leave(request, callback));
  }

  public void enqueueEvent(EventRequest request, AnalyseCallback<EventResponse> callback) {
    enqueue(BatchItem.event(request, callback));
  }

  public int getQueuedItemCount() {
    return queue.size();
  }

  public void flush() {
    synchronized (monitor) {
      flushRequested = true;
      monitor.notifyAll();
      long deadline = System.currentTimeMillis() + SHUTDOWN_WAIT_MILLIS;
      while ((!queue.isEmpty() || flushing) && System.currentTimeMillis() < deadline) {
        try {
          monitor.wait(Math.max(1L, deadline - System.currentTimeMillis()));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  public void shutdown() {
    running = false;
    synchronized (monitor) {
      monitor.notifyAll();
    }
    try {
      worker.join(SHUTDOWN_WAIT_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void enqueue(BatchItem<?> item) {
    if (!queue.offer(item)) {
      notifyError(item, new AnalyseException(429, "Analyse batch queue is full"));
      return;
    }

    if (queue.size() >= config.getSize()) {
      synchronized (monitor) {
        monitor.notifyAll();
      }
    }
  }

  private void run() {
    while (running || !queue.isEmpty()) {
      waitForFlushTrigger();
      drainAndSend();
    }
  }

  private void waitForFlushTrigger() {
    synchronized (monitor) {
      if (!running || flushRequested || queue.size() >= config.getSize()) {
        return;
      }

      try {
        monitor.wait(TimeUnit.SECONDS.toMillis(config.getFlushIntervalSeconds()));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void drainAndSend() {
    while (!queue.isEmpty()) {
      List<BatchItem<?>> items = new ArrayList<>();
      queue.drainTo(items, Math.min(config.getSize(), BatchConfig.API_MAX_BATCH_SIZE));
      if (items.isEmpty()) {
        flushRequested = false;
        return;
      }

      flushing = true;
      try {
        sendWithRetry(items);
      } finally {
        flushing = false;
        synchronized (monitor) {
          monitor.notifyAll();
        }
      }

      if (queue.isEmpty()) {
        flushRequested = false;
      }

      if (running && !flushRequested && queue.size() < config.getSize()) {
        return;
      }
    }
  }

  private void sendWithRetry(List<BatchItem<?>> items) {
    AnalyseException lastError = null;
    for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
      try {
        BatchResponse response = httpClient.postSync(endpoint, buildRequest(items), BatchResponse.class);
        notifySuccess(items, response);
        return;
      } catch (AnalyseException e) {
        lastError = e;
        if (!isRetryable(e) || attempt >= config.getMaxRetries()) {
          break;
        }
        sleepBackoff(attempt);
      }
    }

    AnalyseException error = lastError != null ? lastError : new AnalyseException(500, "Batch request failed");
    for (BatchItem<?> item : items) {
      notifyError(item, error);
    }
  }

  private BatchRequest buildRequest(List<BatchItem<?>> items) {
    List<JoinRequest> joins = new ArrayList<>();
    List<LeaveRequest> leaves = new ArrayList<>();
    List<EventRequest> events = new ArrayList<>();

    for (BatchItem<?> item : items) {
      switch (item.getType()) {
        case JOIN:
          joins.add((JoinRequest) item.getRequest());
          break;
        case LEAVE:
          leaves.add((LeaveRequest) item.getRequest());
          break;
        case EVENT:
          events.add((EventRequest) item.getRequest());
          break;
        default:
          break;
      }
    }

    return new BatchRequest(joins, leaves, events);
  }

  private void notifySuccess(List<BatchItem<?>> items, BatchResponse response) {
    List<BatchItem<?>> joins = filter(items, BatchItem.Type.JOIN);
    List<BatchItem<?>> leaves = filter(items, BatchItem.Type.LEAVE);
    List<BatchItem<?>> events = filter(items, BatchItem.Type.EVENT);

    for (int i = 0; i < joins.size(); i++) {
      BatchResponse.JoinResult result = i < response.getJoinResults().size() ? response.getJoinResults().get(i) : null;
      if (result != null && result.isSuccess()) {
        notifyJoinSuccess(joins.get(i), result.toResponse());
      } else {
        notifyError(joins.get(i), perItemError("join", result != null ? result.getError() : null));
      }
    }

    for (int i = 0; i < leaves.size(); i++) {
      BatchResponse.LeaveResult result = i < response.getLeaveResults().size() ? response.getLeaveResults().get(i) : null;
      if (result != null && result.isSuccess()) {
        notifyLeaveSuccess(leaves.get(i), result.toResponse());
      } else {
        notifyError(leaves.get(i), perItemError("leave", result != null ? result.getError() : null));
      }
    }

    for (int i = 0; i < events.size(); i++) {
      BatchResponse.EventResult result = i < response.getEventResults().size() ? response.getEventResults().get(i) : null;
      if (result != null && result.isSuccess()) {
        notifyEventSuccess(events.get(i), result.toResponse());
      } else {
        notifyError(events.get(i), perItemError("event", result != null ? result.getError() : null));
      }
    }
  }

  private List<BatchItem<?>> filter(List<BatchItem<?>> items, BatchItem.Type type) {
    List<BatchItem<?>> filtered = new ArrayList<>();
    for (BatchItem<?> item : items) {
      if (item.getType() == type) {
        filtered.add(item);
      }
    }
    return filtered;
  }

  @SuppressWarnings("unchecked")
  private void notifyJoinSuccess(BatchItem<?> item, JoinResponse response) {
    AnalyseCallback<JoinResponse> callback = (AnalyseCallback<JoinResponse>) item.getCallback();
    if (callback != null) {
      callback.onSuccess(response);
    }
  }

  @SuppressWarnings("unchecked")
  private void notifyLeaveSuccess(BatchItem<?> item, LeaveResponse response) {
    AnalyseCallback<LeaveResponse> callback = (AnalyseCallback<LeaveResponse>) item.getCallback();
    if (callback != null) {
      callback.onSuccess(response);
    }
  }

  @SuppressWarnings("unchecked")
  private void notifyEventSuccess(BatchItem<?> item, EventResponse response) {
    AnalyseCallback<EventResponse> callback = (AnalyseCallback<EventResponse>) item.getCallback();
    if (callback != null) {
      callback.onSuccess(response);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void notifyError(BatchItem<?> item, AnalyseException error) {
    AnalyseCallback callback = item.getCallback();
    if (callback != null) {
      callback.onError(error);
    }
  }

  private AnalyseException perItemError(String type, String message) {
    return new AnalyseException(400, "Batch " + type + " failed: " + (message != null ? message : "unknown error"));
  }

  private boolean isRetryable(AnalyseException e) {
    int status = e.getStatusCode();
    return status == 0 || status == 429 || status >= 500;
  }

  private void sleepBackoff(int attempt) {
    long delayMillis = TimeUnit.SECONDS.toMillis(1L << Math.min(attempt, 2));
    try {
      Thread.sleep(delayMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
