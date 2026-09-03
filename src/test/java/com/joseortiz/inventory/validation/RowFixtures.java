package com.joseortiz.inventory.validation;

import com.joseortiz.inventory.excel.ProductRow;
import com.joseortiz.inventory.excel.RawCell;
import com.joseortiz.inventory.excel.SpreadsheetColumn;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/** Builds {@link ProductRow}s directly, bypassing the spreadsheet parser. */
final class RowFixtures {

  private RowFixtures() {}

  /**
   * @param rowNumber Excel row number
   * @param values one value per column: String, Number, LocalDate, {@code null} for blank or a
   *     {@link RawCell} to use as-is
   * @return the row
   */
  static ProductRow row(int rowNumber, Object... values) {
    Map<SpreadsheetColumn, RawCell> cells = new EnumMap<>(SpreadsheetColumn.class);
    SpreadsheetColumn[] columns = SpreadsheetColumn.values();
    for (int i = 0; i < columns.length; i++) {
      String ref = (char) ('A' + i) + String.valueOf(rowNumber);
      Object v = i < values.length ? values[i] : null;
      RawCell cell;
      if (v == null) {
        cell = RawCell.blank(ref);
      } else if (v instanceof RawCell rc) {
        cell = rc;
      } else if (v instanceof LocalDate d) {
        cell = RawCell.date(ref, d);
      } else if (v instanceof Number n) {
        cell = RawCell.number(ref, n.doubleValue());
      } else {
        cell = RawCell.text(ref, String.valueOf(v));
      }
      cells.put(columns[i], cell);
    }
    return new ProductRow(rowNumber, cells);
  }

  /** @return a fully valid row */
  static ProductRow valid(int rowNumber) {
    return row(rowNumber, "SKU-" + rowNumber, "Name", "Category", LocalDate.of(2026, 1, 10), 19.99, 5);
  }
}
