package com.joseortiz.inventory.error;

import java.util.List;

/**
 * Base class of all exceptions translated into an {@link ApiErrorResponse}.
 *
 * <p>Subclasses fix the top-level {@link ErrorCode}; the details describe the individual problems.
 */
public abstract class ApiException extends RuntimeException {

  private final ErrorCode code;
  private final transient List<ErrorDetail> details;

  /**
   * Creates the exception.
   *
   * @param code top-level code, must carry an HTTP status
   * @param details individual problems
   */
  protected ApiException(ErrorCode code, List<ErrorDetail> details) {
    super(code.message() + " (" + details.size() + " detail(s))");
    if (code.status() == null) {
      throw new IllegalArgumentException(code + " is not a top-level error code");
    }
    this.code = code;
    this.details = List.copyOf(details);
  }

  /** @return top-level error code */
  public ErrorCode getCode() {
    return code;
  }

  /** @return individual problems */
  public List<ErrorDetail> getDetails() {
    return details;
  }

  /**
   * Converts the exception to its wire representation.
   *
   * @return the response body
   */
  public ApiErrorResponse toResponse() {
    return ApiErrorResponse.of(code, details);
  }
}
