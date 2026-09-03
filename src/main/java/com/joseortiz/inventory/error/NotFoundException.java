package com.joseortiz.inventory.error;

import java.util.List;

/**
 * Raised when a request addresses a resource that does not exist.
 */
public class NotFoundException extends ApiException {

  /**
   * Creates the exception.
   *
   * @param detail which resource was missing
   */
  public NotFoundException(ErrorDetail detail) {
    super(ErrorCode.NOT_FOUND, List.of(detail));
  }

  /**
   * Convenience factory for a missing product.
   *
   * @param id requested identifier
   * @return the exception
   */
  public static NotFoundException product(long id) {
    return new NotFoundException(
        ErrorDetail.withInfo().put("field", "id").put("id", id).build(ErrorCode.PRODUCT_NOT_FOUND));
  }
}
