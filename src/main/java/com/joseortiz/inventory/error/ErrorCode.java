package com.joseortiz.inventory.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Catalogue of every error the API can return.
 *
 * <p>Each constant carries a stable machine-readable {@link #code()} and a human readable message
 * template. Templates may reference entries of the error's {@code info} map with {@code {key}}
 * placeholders, e.g. {@code "Row {row}: purchase date '{value}' is not a valid date."}. Adding a
 * new rule is therefore a one-line change here plus the validator that raises it.
 *
 * <p>Constants are grouped by the layer that raises them; the {@link #status()} of a top-level
 * code decides the HTTP status of the response.
 */
public enum ErrorCode {

  // ---- Top-level codes (HTTP status carriers) ------------------------------------------------

  /** One or more rows in the uploaded file failed validation. */
  VALIDATION_ERROR("The uploaded file contains invalid data.", HttpStatus.BAD_REQUEST),
  /** The uploaded file could not be accepted at all (wrong type, unreadable, bad headers...). */
  INVALID_FILE("The uploaded file could not be processed.", HttpStatus.BAD_REQUEST),
  /** A request parameter was rejected. */
  BAD_REQUEST("The request is invalid.", HttpStatus.BAD_REQUEST),
  /** The addressed resource does not exist. */
  NOT_FOUND("The requested resource was not found.", HttpStatus.NOT_FOUND),
  /** Catch-all for unexpected failures. */
  INTERNAL_ERROR("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR),

  // ---- File-level details --------------------------------------------------------------------

  MISSING_FILE("No file was provided. Upload a spreadsheet using the 'file' form field."),
  FILE_EMPTY("The uploaded file '{fileName}' is empty."),
  FILE_TOO_LARGE("The uploaded file exceeds the maximum size of {max} bytes."),
  UNSUPPORTED_FILE_TYPE(
      "File '{fileName}' has an unsupported extension. Accepted: {accepted}."),
  FILE_UNREADABLE("File '{fileName}' could not be read as a spreadsheet: {reason}"),
  MISSING_HEADER_ROW("The first sheet has no header row."),
  INVALID_HEADER("Column {column} should be '{expected}' but was '{actual}'."),
  NO_DATA_ROWS("The spreadsheet has a header but no data rows."),
  TOO_MANY_ROWS("The spreadsheet has {rows} data rows; the maximum is {max}."),

  // ---- Row / cell-level details --------------------------------------------------------------

  FORMULA_EVALUATION_ERROR("Row {row}: formula in cell {cell} could not be evaluated ({reason})."),
  REQUIRED_FIELD_MISSING("Row {row}: '{field}' is required."),
  TEXT_TOO_LONG("Row {row}: '{field}' exceeds {max} characters."),
  INVALID_DATE_FORMAT(
      "Row {row}: '{field}' value '{value}' is not a valid date. Accepted formats: {accepted}."),
  DATE_IN_FUTURE("Row {row}: '{field}' value {value} is in the future."),
  INVALID_NUMBER("Row {row}: '{field}' value '{value}' is not a valid number."),
  NEGATIVE_VALUE("Row {row}: '{field}' value {value} must not be negative."),
  NOT_AN_INTEGER("Row {row}: '{field}' value {value} must be a whole number."),

  // ---- Uniqueness details --------------------------------------------------------------------

  DUPLICATE_ENTRY_IN_FILE(
      "Row {row}: SKU '{sku}' with purchase date {purchaseDate} also appears on row {firstRow}."),
  DUPLICATE_ENTRY_EXISTS(
      "Row {row}: SKU '{sku}' with purchase date {purchaseDate} already exists in the inventory."),

  // ---- Resource details ----------------------------------------------------------------------

  PRODUCT_NOT_FOUND("Product {id} does not exist. It may have been deleted already."),

  // ---- Query details -------------------------------------------------------------------------

  INVALID_SORT_FIELD("'{value}' is not a sortable field. Sortable fields: {accepted}."),
  PAGE_SIZE_TOO_LARGE("Page size {value} exceeds the maximum of {max}.");

  private final String messageTemplate;
  private final HttpStatus status;

  ErrorCode(String messageTemplate) {
    this(messageTemplate, null);
  }

  ErrorCode(String messageTemplate, HttpStatus status) {
    this.messageTemplate = messageTemplate;
    this.status = status;
  }

  /**
   * Stable identifier sent to clients. Equals the enum constant name.
   *
   * @return the code
   */
  public String code() {
    return name();
  }

  /**
   * HTTP status associated with a top-level code.
   *
   * @return the status, or {@code null} for detail-level codes
   */
  public HttpStatus status() {
    return status;
  }

  /**
   * Renders the message template, replacing {@code {key}} placeholders with values from {@code
   * info}. Unknown placeholders are left untouched so a missing value is visible rather than
   * silently dropped.
   *
   * @param info values referenced by the template
   * @return the rendered message
   */
  public String message(Map<String, ?> info) {
    String rendered = messageTemplate;
    for (Map.Entry<String, ?> entry : info.entrySet()) {
      rendered = rendered.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
    }
    return rendered;
  }

  /**
   * Renders the template with no placeholders.
   *
   * @return the message
   */
  public String message() {
    return messageTemplate;
  }
}
