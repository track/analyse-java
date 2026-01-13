package net.analyse.api.exception;

/**
 * Exception thrown when an Analyse API operation fails
 */
public class AnalyseException extends Exception {

  private final int statusCode;
  private final ErrorType errorType;

  /**
   * Create a new exception with a status code and message
   *
   * @param statusCode The HTTP status code
   * @param message The error message
   */
  public AnalyseException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
    this.errorType = ErrorType.fromStatusCode(statusCode);
  }

  /**
   * Create a new exception from a throwable
   *
   * @param message The error message
   * @param cause The underlying cause
   */
  public AnalyseException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = 0;
    this.errorType = ErrorType.NETWORK_ERROR;
  }

  /**
   * Get the HTTP status code
   *
   * @return The status code, or 0 if not applicable
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Get the error type
   *
   * @return The error type
   */
  public ErrorType getErrorType() {
    return errorType;
  }

  /**
   * Error types based on HTTP status codes
   */
  public enum ErrorType {
    /**
     * Invalid request body (missing/invalid fields)
     */
    BAD_REQUEST,

    /**
     * Invalid or missing API key
     */
    UNAUTHORIZED,

    /**
     * Session or player not found
     */
    NOT_FOUND,

    /**
     * Rate limited
     */
    RATE_LIMITED,

    /**
     * Server error
     */
    SERVER_ERROR,

    /**
     * Network or connection error
     */
    NETWORK_ERROR,

    /**
     * Unknown error
     */
    UNKNOWN;

    /**
     * Get the error type from an HTTP status code
     *
     * @param statusCode The HTTP status code
     * @return The corresponding error type
     */
    public static ErrorType fromStatusCode(int statusCode) {
      return switch (statusCode) {
        case 400 -> BAD_REQUEST;
        case 401 -> UNAUTHORIZED;
        case 404 -> NOT_FOUND;
        case 429 -> RATE_LIMITED;
        case 500, 502, 503, 504 -> SERVER_ERROR;
        default -> UNKNOWN;
      };
    }
  }
}
