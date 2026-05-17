package net.analyse.sdk.config;

import lombok.Getter;

/**
 * Configuration for batched analytics delivery.
 */
@Getter
public class BatchConfig {

  public static final int DEFAULT_SIZE = 200;
  public static final int DEFAULT_FLUSH_INTERVAL_SECONDS = 3;
  public static final int DEFAULT_MAX_QUEUE_SIZE = 10000;
  public static final int DEFAULT_MAX_RETRIES = 3;
  public static final int API_MAX_BATCH_SIZE = 1000;

  private final int size;
  private final int flushIntervalSeconds;
  private final int maxQueueSize;
  private final int maxRetries;

  public BatchConfig() {
    this(DEFAULT_SIZE, DEFAULT_FLUSH_INTERVAL_SECONDS, DEFAULT_MAX_QUEUE_SIZE, DEFAULT_MAX_RETRIES);
  }

  public BatchConfig(int size, int flushIntervalSeconds, int maxQueueSize, int maxRetries) {
    this.size = clamp(size, 1, API_MAX_BATCH_SIZE, DEFAULT_SIZE);
    this.flushIntervalSeconds = clamp(flushIntervalSeconds, 1, 60, DEFAULT_FLUSH_INTERVAL_SECONDS);
    this.maxQueueSize = clamp(maxQueueSize, 1, Integer.MAX_VALUE, DEFAULT_MAX_QUEUE_SIZE);
    this.maxRetries = clamp(maxRetries, 0, 10, DEFAULT_MAX_RETRIES);
  }

  private static int clamp(int value, int min, int max, int defaultValue) {
    if (value < min) {
      return defaultValue;
    }

    return Math.min(value, max);
  }
}
