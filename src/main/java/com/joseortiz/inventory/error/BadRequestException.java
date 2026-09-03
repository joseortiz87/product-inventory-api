package com.joseortiz.inventory.error;

import java.util.List;

/**
 * Raised when a query parameter is rejected (unknown sort field, page size too large...).
 */
public class BadRequestException extends ApiException {

  /**
   * Creates the exception.
   *
   * @param detail the problem
   */
  public BadRequestException(ErrorDetail detail) {
    super(ErrorCode.BAD_REQUEST, List.of(detail));
  }
}
