package net.analyse.sdk.response;

import lombok.Getter;

/**
 * Response from the plugin purchase endpoint
 */
@Getter
public class PurchaseResponse {

  private boolean success;
  private String purchaseId;
  private String transactionId;
  private boolean isFirstPurchase;
}
