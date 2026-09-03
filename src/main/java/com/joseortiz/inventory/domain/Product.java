package com.joseortiz.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A product lot in the inventory.
 *
 * <p>The natural key is the combination of {@link #getSku() SKU} and {@link #getPurchaseDate()
 * purchase date}: the same SKU may be purchased several times, but never twice on the same day.
 * That rule is enforced both by the validation layer (to produce friendly errors) and by a
 * database unique constraint (as a last line of defence).
 */
@Entity
@Table(
    name = "products",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_products_sku_purchase_date", columnNames = {"sku", "purchase_date"}),
    indexes = {
      @Index(name = "ix_products_category", columnList = "category"),
      @Index(name = "ix_products_purchase_date", columnList = "purchase_date")
    })
public class Product {

  /** Maximum length accepted for free-text columns. */
  public static final int TEXT_MAX_LENGTH = 255;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = TEXT_MAX_LENGTH)
  private String sku;

  @Column(nullable = false, length = TEXT_MAX_LENGTH)
  private String name;

  @Column(nullable = false, length = TEXT_MAX_LENGTH)
  private String category;

  @Column(name = "purchase_date", nullable = false)
  private LocalDate purchaseDate;

  @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private Integer quantity;

  /** Required by JPA. */
  protected Product() {}

  /**
   * Creates a new, unsaved product.
   *
   * @param sku stock keeping unit
   * @param name human readable product name
   * @param category product category
   * @param purchaseDate date the lot was purchased
   * @param unitPrice price of a single unit
   * @param quantity number of units purchased
   */
  public Product(
      String sku,
      String name,
      String category,
      LocalDate purchaseDate,
      BigDecimal unitPrice,
      Integer quantity) {
    this.sku = Objects.requireNonNull(sku, "sku");
    this.name = Objects.requireNonNull(name, "name");
    this.category = Objects.requireNonNull(category, "category");
    this.purchaseDate = Objects.requireNonNull(purchaseDate, "purchaseDate");
    this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
    this.quantity = Objects.requireNonNull(quantity, "quantity");
  }

  /** @return database identifier, {@code null} until persisted */
  public Long getId() {
    return id;
  }

  /** @return stock keeping unit */
  public String getSku() {
    return sku;
  }

  /** @return product name */
  public String getName() {
    return name;
  }

  /** @return product category */
  public String getCategory() {
    return category;
  }

  /** @return purchase date of the lot */
  public LocalDate getPurchaseDate() {
    return purchaseDate;
  }

  /** @return price of one unit */
  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  /** @return number of units */
  public Integer getQuantity() {
    return quantity;
  }

  /**
   * Total value of the lot.
   *
   * @return {@code unitPrice * quantity}
   */
  public BigDecimal getTotalValue() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }
}
