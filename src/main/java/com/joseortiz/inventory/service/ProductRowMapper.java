package com.joseortiz.inventory.service;

import com.joseortiz.inventory.domain.Product;
import com.joseortiz.inventory.excel.ProductRow;
import com.joseortiz.inventory.excel.SpreadsheetColumn;
import com.joseortiz.inventory.validation.ImportedProduct;
import com.joseortiz.inventory.validation.ValueParsers;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Builds {@link Product} entities from rows that already passed {@link
 * com.joseortiz.inventory.validation.ProductRowValidator}.
 */
@Component
public class ProductRowMapper {

  private final ValueParsers parsers;

  /**
   * Creates the mapper.
   *
   * @param parsers value conversions shared with the validator
   */
  public ProductRowMapper(ValueParsers parsers) {
    this.parsers = parsers;
  }

  /**
   * Maps a validated row.
   *
   * @param row a row for which the validator returned no errors
   * @return the product with its row number
   * @throws IllegalStateException if the row was not validated first
   */
  public ImportedProduct toProduct(ProductRow row) {
    Product product =
        new Product(
            text(row, SpreadsheetColumn.SKU),
            text(row, SpreadsheetColumn.NAME),
            text(row, SpreadsheetColumn.CATEGORY),
            parsers
                .parseDate(row.cell(SpreadsheetColumn.PURCHASE_DATE))
                .orElseThrow(() -> unvalidated(row, SpreadsheetColumn.PURCHASE_DATE)),
            parsers
                .parseDecimal(row.cell(SpreadsheetColumn.UNIT_PRICE))
                .orElseThrow(() -> unvalidated(row, SpreadsheetColumn.UNIT_PRICE))
                .setScale(2, RoundingMode.HALF_UP),
            parsers
                .parseDecimal(row.cell(SpreadsheetColumn.QUANTITY))
                .orElseThrow(() -> unvalidated(row, SpreadsheetColumn.QUANTITY))
                .intValueExact());
    return new ImportedProduct(row.rowNumber(), product);
  }

  private static String text(ProductRow row, SpreadsheetColumn column) {
    Object value = row.cell(column).displayValue();
    if (value == null) {
      throw unvalidated(row, column);
    }
    return String.valueOf(value);
  }

  private static IllegalStateException unvalidated(ProductRow row, SpreadsheetColumn column) {
    return new IllegalStateException(
        "Row " + row.rowNumber() + " column " + column.field() + " was not validated before mapping");
  }
}
