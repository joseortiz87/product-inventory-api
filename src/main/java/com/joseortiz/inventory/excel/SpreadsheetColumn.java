package com.joseortiz.inventory.excel;

/**
 * The columns an inventory spreadsheet must contain, in order.
 *
 * <p>{@link #ordinal()} is the zero-based column index in the sheet; {@link #header()} is the exact
 * (case-insensitive) header text expected on the first row; {@link #field()} is the name used in
 * error payloads and matches the JSON property of the product resource.
 */
public enum SpreadsheetColumn {
  SKU("Product SKU", "sku"),
  NAME("Product Name", "name"),
  CATEGORY("Category", "category"),
  PURCHASE_DATE("Purchase Date", "purchaseDate"),
  UNIT_PRICE("Unit Price", "unitPrice"),
  QUANTITY("Quantity", "quantity");

  private final String header;
  private final String field;

  SpreadsheetColumn(String header, String field) {
    this.header = header;
    this.field = field;
  }

  /** @return expected header text */
  public String header() {
    return header;
  }

  /** @return field name used in API payloads */
  public String field() {
    return field;
  }

  /** @return zero-based column index in the sheet */
  public int index() {
    return ordinal();
  }

  /**
   * Whether the header text matches this column (trimmed, case-insensitive).
   *
   * @param text header cell text
   * @return {@code true} when it matches
   */
  public boolean matches(String text) {
    return text != null && header.equalsIgnoreCase(text.trim());
  }
}
