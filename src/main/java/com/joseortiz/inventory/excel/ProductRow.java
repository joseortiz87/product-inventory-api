package com.joseortiz.inventory.excel;

import java.util.EnumMap;
import java.util.Map;

/**
 * One data row of the spreadsheet, before validation.
 *
 * @param rowNumber 1-based row number as displayed in Excel (header is row 1)
 * @param cells cell snapshot per column
 */
public record ProductRow(int rowNumber, Map<SpreadsheetColumn, RawCell> cells) {

  /**
   * Creates a row, defensively copying the cells.
   *
   * @param rowNumber Excel row number
   * @param cells cells by column
   */
  public ProductRow {
    cells = Map.copyOf(new EnumMap<>(cells));
  }

  /**
   * @param column column to read
   * @return the cell; blank when the column was absent
   */
  public RawCell cell(SpreadsheetColumn column) {
    RawCell cell = cells.get(column);
    return cell != null ? cell : RawCell.blank(refOf(column));
  }

  private String refOf(SpreadsheetColumn column) {
    return (char) ('A' + column.index()) + String.valueOf(rowNumber);
  }
}
