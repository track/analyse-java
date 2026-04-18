package net.analyse.sdk.request;

import lombok.Getter;
import java.util.UUID;

/**
 * Request payload for the plugin purchase endpoint
 */
@Getter
public class PurchaseRequest {

  private final String playerUuid;
  private final double purchaseValue;
  private final String productName;

  /**
   * Create a new purchase request
   *
   * @param playerUuid    The player's Minecraft UUID
   * @param purchaseValue The value of the purchase (must be non-negative)
   * @param productName   The name of the product that was purchased
   */
  public PurchaseRequest(UUID playerUuid, double purchaseValue, String productName) {
    if (playerUuid == null) {
      throw new IllegalArgumentException("Player UUID cannot be null");
    }

    if (Double.isNaN(purchaseValue) || Double.isInfinite(purchaseValue)) {
      throw new IllegalArgumentException("Purchase value must be a finite number");
    }

    if (purchaseValue < 0) {
      throw new IllegalArgumentException("Purchase value cannot be negative");
    }

    if (productName == null || productName.trim().isEmpty()) {
      throw new IllegalArgumentException("Product name cannot be null or blank");
    }

    this.playerUuid = playerUuid.toString();
    this.purchaseValue = purchaseValue;
    this.productName = productName;
  }
}
