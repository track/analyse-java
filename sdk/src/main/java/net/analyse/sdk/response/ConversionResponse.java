package net.analyse.sdk.response;

import lombok.Getter;

/**
 * Response after tracking a conversion event
 */
@Getter
public class ConversionResponse {

  private boolean success;
  private String conversionId;
}
