package com.joseortiz.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Figures shown on the dashboard summary cards.
 *
 * @param totalProducts number of product rows
 * @param totalInventoryValue sum of {@code unitPrice * quantity}
 * @param averageStockAgeDays mean stock age in days, {@code null} when the inventory is empty
 * @param oldestPurchaseDate earliest purchase date, {@code null} when empty
 * @param newestPurchaseDate latest purchase date, {@code null} when empty
 */
public record InventorySummaryResponse(
    long totalProducts,
    BigDecimal totalInventoryValue,
    Double averageStockAgeDays,
    LocalDate oldestPurchaseDate,
    LocalDate newestPurchaseDate) {}
