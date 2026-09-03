package com.joseortiz.inventory.error;

import java.util.List;

/**
 * Raised when the content of an uploaded file fails one or more validation rules.
 *
 * <p>All detected problems are reported together so the user can fix the spreadsheet in one pass.
 */
public class ValidationException extends ApiException {

  /**
   * Creates the exception.
   *
   * @param details every failed rule, must not be empty
   */
  public ValidationException(List<ErrorDetail> details) {
    super(ErrorCode.VALIDATION_ERROR, details);
  }
}
