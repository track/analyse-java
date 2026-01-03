package net.analyse.sdk;

/**
 * Callback interface for async API responses
 *
 * @param <T> The response type
 */
public interface AnalyseCallback<T> {

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
  void onError(AnalyseException exception);
}

