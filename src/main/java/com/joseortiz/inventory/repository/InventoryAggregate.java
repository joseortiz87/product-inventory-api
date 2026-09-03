package com.joseortiz.inventory.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Result of the single aggregate query over the products table.
 *
 * @param productCount number of product rows
 * @param totalValue sum of {@code unitPrice * quantity}
 * @param oldestPurchaseDate earliest purchase date, {@code null} when the table is empty
 * @param newestPurchaseDate latest purchase date, {@code null} when the table is empty
 */
public record InventoryAggregate(
    long productCount,
    BigDecimal totalValue,
    LocalDate oldestPurchaseDate,
    LocalDate newestPurchaseDate) {}
