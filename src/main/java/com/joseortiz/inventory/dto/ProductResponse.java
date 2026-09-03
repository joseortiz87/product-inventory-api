package com.joseortiz.inventory.dto;

import com.joseortiz.inventory.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Product as exposed by the API, enriched with derived metrics.
 *
 * @param id identifier
 * @param sku stock keeping unit
 * @param name product name
 * @param category category
 * @param purchaseDate purchase date
 * @param unitPrice price per unit
 * @param quantity units in stock
 * @param totalValue {@code unitPrice * quantity}
 * @param stockAgeDays days elapsed between the purchase date and today
 */
public record ProductResponse(
    Long id,
    String sku,
    String name,
    String category,
    LocalDate purchaseDate,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal totalValue,
    long stockAgeDays) {

  /**
   * Maps an entity.
   *
   * @param product the entity
   * @param today reference date for the stock age
   * @return the response
   */
  public static ProductResponse from(Product product, LocalDate today) {
    return new ProductResponse(
        product.getId(),
        product.getSku(),
        product.getName(),
        product.getCategory(),
        product.getPurchaseDate(),
        product.getUnitPrice(),
        product.getQuantity(),
        product.getTotalValue(),
        ChronoUnit.DAYS.between(product.getPurchaseDate(), today));
  }
}
