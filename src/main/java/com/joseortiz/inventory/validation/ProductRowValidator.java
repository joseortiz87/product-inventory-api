package com.joseortiz.inventory.validation;

import com.joseortiz.inventory.domain.Product;
import com.joseortiz.inventory.error.ErrorCode;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.excel.ProductRow;
import com.joseortiz.inventory.excel.RawCell;
import com.joseortiz.inventory.excel.SpreadsheetColumn;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Validates the values of a single spreadsheet row.
 *
 * <p>Rules:
 *
 * <ul>
 *   <li>SKU, name and category are required text of at most {@link Product#TEXT_MAX_LENGTH}
 *       characters.
 *   <li>Purchase date is required, must parse (Excel date or accepted text pattern) and must not be
 *       in the future.
 *   <li>Unit price is required, numeric and not negative.
 *   <li>Quantity is required, a whole number and not negative.
 * </ul>
 *
 * Cells whose formula failed are skipped: the parser has already reported them.
 */
@Component
public class ProductRowValidator implements ValidationRule<ProductRow> {

  private final ValueParsers parsers;
  private final Clock clock;

  /**
   * Creates the validator.
   *
   * @param parsers value conversions
   * @param clock source of "today", injectable for tests
   */
  public ProductRowValidator(ValueParsers parsers, Clock clock) {
    this.parsers = parsers;
    this.clock = clock;
  }

  @Override
  public List<ErrorDetail> validate(ProductRow row) {
    List<ErrorDetail> errors = new ArrayList<>();
    validateText(row, SpreadsheetColumn.SKU, errors);
    validateText(row, SpreadsheetColumn.NAME, errors);
    validateText(row, SpreadsheetColumn.CATEGORY, errors);
    validateDate(row, errors);
    validatePrice(row, errors);
    validateQuantity(row, errors);
    return errors;
  }

  private void validateText(ProductRow row, SpreadsheetColumn column, List<ErrorDetail> errors) {
    RawCell cell = row.cell(column);
    if (cell.isError()) {
      return;
    }
    if (cell.isBlank()) {
      errors.add(required(row, column, cell));
      return;
    }
    String value = String.valueOf(cell.displayValue());
    if (value.length() > Product.TEXT_MAX_LENGTH) {
      errors.add(
          base(row, column, cell).put("max", Product.TEXT_MAX_LENGTH).build(ErrorCode.TEXT_TOO_LONG));
    }
  }

  private void validateDate(ProductRow row, List<ErrorDetail> errors) {
    SpreadsheetColumn column = SpreadsheetColumn.PURCHASE_DATE;
    RawCell cell = row.cell(column);
    if (cell.isError()) {
      return;
    }
    if (cell.isBlank()) {
      errors.add(required(row, column, cell));
      return;
    }
    Optional<LocalDate> date = parsers.parseDate(cell);
    if (date.isEmpty()) {
      errors.add(
          base(row, column, cell)
              .put("accepted", String.join(", ", parsers.datePatterns()))
              .build(ErrorCode.INVALID_DATE_FORMAT));
      return;
    }
    LocalDate today = LocalDate.now(clock);
    if (date.get().isAfter(today)) {
      errors.add(
          base(row, column, cell)
              .put("value", date.get().toString())
              .put("max", today.toString())
              .build(ErrorCode.DATE_IN_FUTURE));
    }
  }

  private void validatePrice(ProductRow row, List<ErrorDetail> errors) {
    SpreadsheetColumn column = SpreadsheetColumn.UNIT_PRICE;
    RawCell cell = row.cell(column);
    if (cell.isError()) {
      return;
    }
    if (cell.isBlank()) {
      errors.add(required(row, column, cell));
      return;
    }
    Optional<BigDecimal> price = parsers.parseDecimal(cell);
    if (price.isEmpty()) {
      errors.add(base(row, column, cell).build(ErrorCode.INVALID_NUMBER));
    } else if (price.get().signum() < 0) {
      errors.add(base(row, column, cell).put("min", 0).build(ErrorCode.NEGATIVE_VALUE));
    }
  }

  private void validateQuantity(ProductRow row, List<ErrorDetail> errors) {
    SpreadsheetColumn column = SpreadsheetColumn.QUANTITY;
    RawCell cell = row.cell(column);
    if (cell.isError()) {
      return;
    }
    if (cell.isBlank()) {
      errors.add(required(row, column, cell));
      return;
    }
    Optional<BigDecimal> quantity = parsers.parseDecimal(cell);
    if (quantity.isEmpty()) {
      errors.add(base(row, column, cell).build(ErrorCode.INVALID_NUMBER));
      return;
    }
    if (!ValueParsers.isWhole(quantity.get())) {
      errors.add(base(row, column, cell).build(ErrorCode.NOT_AN_INTEGER));
    }
    if (quantity.get().signum() < 0) {
      errors.add(base(row, column, cell).put("min", 0).build(ErrorCode.NEGATIVE_VALUE));
    }
  }

  private static ErrorDetail required(ProductRow row, SpreadsheetColumn column, RawCell cell) {
    return base(row, column, cell).build(ErrorCode.REQUIRED_FIELD_MISSING);
  }

  private static ErrorDetail.InfoBuilder base(ProductRow row, SpreadsheetColumn column, RawCell cell) {
    return ErrorDetail.withInfo()
        .put("row", row.rowNumber())
        .put("cell", cell.ref())
        .put("field", column.field())
        .put("value", cell.displayValue());
  }
}
