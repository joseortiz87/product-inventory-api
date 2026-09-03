package com.joseortiz.inventory.error;

import java.util.List;

/**
 * Envelope of every error response produced by the API.
 *
 * <pre>{@code
 * { "error": { "code": "VALIDATION_ERROR", "details": [ ... ] } }
 * }</pre>
 *
 * @param error the error body
 */
public record ApiErrorResponse(ErrorBody error) {

  /**
   * Error body.
   *
   * @param code top-level {@link ErrorCode}
   * @param message human readable summary
   * @param details individual problems, possibly empty
   */
  public record ErrorBody(String code, String message, List<ErrorDetail> details) {}

  /**
   * Builds a response.
   *
   * @param code top-level code
   * @param details individual problems
   * @return the response
   */
  public static ApiErrorResponse of(ErrorCode code, List<ErrorDetail> details) {
    return new ApiErrorResponse(new ErrorBody(code.code(), code.message(), List.copyOf(details)));
  }
}
