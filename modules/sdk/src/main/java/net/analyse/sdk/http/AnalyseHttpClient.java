package net.analyse.sdk.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.analyse.sdk.AnalyseCallback;
import net.analyse.api.exception.AnalyseException;
import net.analyse.sdk.config.AnalyseConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for making async requests to the Analyse API
 */
public class AnalyseHttpClient {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private static final int TIMEOUT_SECONDS = 10;

  private final OkHttpClient httpClient;
  private final Gson gson;
  private final AnalyseConfig config;

  /**
   * Create a new HTTP client with the given configuration
   *
   * @param config The SDK configuration
   */
  public AnalyseHttpClient(AnalyseConfig config) {
    this.config = config;
    this.gson = new GsonBuilder().create();
    this.httpClient = new OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build();
  }

  /**
   * Make an async POST request to the API
   *
   * @param endpoint      The API endpoint (e.g., "/api/plugin/join")
   * @param requestBody   The request body object
   * @param responseClass The class to deserialize the response into
   * @param callback      The callback to invoke on success or failure
   * @param <T>           The response type
   */
  public <T> void post(String endpoint, Object requestBody, Class<T> responseClass, AnalyseCallback<T> callback) {
    String url = config.getApiUrl() + endpoint;
    String json = gson.toJson(requestBody);
    String apiKey = config.getApiKey();

    // Validate API key before making request
    if (apiKey == null || apiKey.trim().isEmpty()) {
      callback.onError(new AnalyseException(401, "API key is not configured"));
      return;
    }

    Request request = new Request.Builder()
        .url(url)
        .addHeader("X-Api-Key", apiKey)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")
        .post(RequestBody.create(json, JSON))
        .build();

    httpClient.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(@NotNull Call call, @NotNull IOException e) {
        callback.onError(new AnalyseException("Network error: " + e.getMessage(), e));
      }

      @Override
      public void onResponse(@NotNull Call call, @NotNull Response response) {
        try (ResponseBody body = response.body()) {
          if (!response.isSuccessful()) {
            String errorMessage = body != null ? body.string() : "Unknown error";
            callback.onError(new AnalyseException(response.code(), errorMessage));
            return;
          }

          if (body == null) {
            callback.onError(new AnalyseException(500, "Empty response body"));
            return;
          }

          String responseJson = body.string();
          T result = gson.fromJson(responseJson, responseClass);
          callback.onSuccess(result);
        } catch (IOException e) {
          callback.onError(new AnalyseException("Failed to read response: " + e.getMessage(), e));
        } catch (Exception e) {
          callback.onError(new AnalyseException("Failed to parse response: " + e.getMessage(), e));
        }
      }
    });
  }

  /**
   * Make an async GET request to the API
   *
   * @param endpoint      The API endpoint
   * @param responseClass The class to deserialize the response into
   * @param callback      The callback to invoke on success or failure
   * @param <T>           The response type
   */
  public <T> void get(String endpoint, Class<T> responseClass, AnalyseCallback<T> callback) {
    String url = config.getApiUrl() + endpoint;
    String apiKey = config.getApiKey();

    // Validate API key before making request
    if (apiKey == null || apiKey.trim().isEmpty()) {
      callback.onError(new AnalyseException(401, "API key is not configured"));
      return;
    }

    Request request = new Request.Builder()
        .url(url)
        .addHeader("X-Api-Key", apiKey)
        .addHeader("Accept", "application/json")
        .get()
        .build();

    httpClient.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(@NotNull Call call, @NotNull IOException e) {
        callback.onError(new AnalyseException("Network error: " + e.getMessage(), e));
      }

      @Override
      public void onResponse(@NotNull Call call, @NotNull Response response) {
        try (ResponseBody body = response.body()) {
          if (!response.isSuccessful()) {
            String errorMessage = body != null ? body.string() : "Unknown error";
            callback.onError(new AnalyseException(response.code(), errorMessage));
            return;
          }

          if (body == null) {
            callback.onError(new AnalyseException(500, "Empty response body"));
            return;
          }

          String responseJson = body.string();
          T result = gson.fromJson(responseJson, responseClass);
          callback.onSuccess(result);
        } catch (IOException e) {
          callback.onError(new AnalyseException("Failed to read response: " + e.getMessage(), e));
        } catch (Exception e) {
          callback.onError(new AnalyseException("Failed to parse response: " + e.getMessage(), e));
        }
      }
    });
  }

  /**
   * Shutdown the HTTP client and release resources
   */
  public void shutdown() {
    httpClient.dispatcher().executorService().shutdown();
    httpClient.connectionPool().evictAll();
  }
}

