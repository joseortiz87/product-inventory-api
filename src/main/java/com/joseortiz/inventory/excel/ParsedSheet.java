package com.joseortiz.inventory.excel;

import com.joseortiz.inventory.error.ErrorDetail;
import java.util.List;

/**
 * Outcome of parsing a spreadsheet.
 *
 * <p>Cell-level problems found while evaluating formulas are collected rather than thrown so they
 * can be reported together with the validation errors of other rows.
 *
 * @param rows data rows in sheet order, blank rows skipped
 * @param cellErrors formula evaluation failures
 */
public record ParsedSheet(List<ProductRow> rows, List<ErrorDetail> cellErrors) {

  /**
   * Creates the result with immutable copies.
   *
   * @param rows data rows
   * @param cellErrors formula errors
   */
  public ParsedSheet {
    rows = List.copyOf(rows);
    cellErrors = List.copyOf(cellErrors);
  }
}
