package com.serverstats.sdk;

import com.serverstats.api.exception.ServerStatsException;

/**
 * Callback interface for async API responses
 *
 * @param <T> The response type
 */
public interface ServerStatsCallback<T> {

  /**
   * Called when the API request succeeds
   *
   * @param response The response from the API
   */
  void onSuccess(T response);

  /**
   * Called when the API request fails
   *
   * @param exception The exception that occurred
   */
  void onError(ServerStatsException exception);
}

