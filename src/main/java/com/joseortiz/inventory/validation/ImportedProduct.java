package com.joseortiz.inventory.validation;

import com.joseortiz.inventory.domain.Product;

/**
 * A product built from a spreadsheet row, keeping the row number for error reporting.
 *
 * @param rowNumber Excel row the product came from
 * @param product the mapped entity, not yet persisted
 */
public record ImportedProduct(int rowNumber, Product product) {}
