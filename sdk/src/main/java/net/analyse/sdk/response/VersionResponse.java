package net.analyse.sdk.response;

import lombok.Getter;
import java.util.Map;

/**
 * Response containing version information for plugin updates
 */
@Getter
public class VersionResponse {

  private boolean success;
  private String version;
  private String buildNumber;
  private Map<String, PlatformInfo> platforms;

  /**
   * Platform-specific version information
   */
  @Getter
  public static class PlatformInfo {

    private String version;
    private String fileName;
    private String downloadUrl;
    private Long size;
  }
}
