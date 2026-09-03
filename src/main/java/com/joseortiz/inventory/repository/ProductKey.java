package com.joseortiz.inventory.repository;

import java.time.LocalDate;

/**
 * Natural key of a {@link com.joseortiz.inventory.domain.Product}.
 *
 * @param sku stock keeping unit
 * @param purchaseDate purchase date
 */
public record ProductKey(String sku, LocalDate purchaseDate) {}
