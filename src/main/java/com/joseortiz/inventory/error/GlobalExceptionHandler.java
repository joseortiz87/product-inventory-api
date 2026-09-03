package com.joseortiz.inventory.error;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Translates exceptions into the uniform {@link ApiErrorResponse} envelope.
 *
 * <p>Domain exceptions already carry their code and details. Framework exceptions that a client can
 * trigger (missing multipart, oversized upload) are mapped to the same shape so the front end has a
 * single error contract to implement. Anything else becomes {@link ErrorCode#INTERNAL_ERROR}
 * without leaking internals.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles domain exceptions.
   *
   * @param ex the exception
   * @return the response with the status of the top-level code
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
    log.debug("API error {}: {}", ex.getCode(), ex.getDetails());
    return ResponseEntity.status(ex.getCode().status()).body(ex.toResponse());
  }

  /**
   * Handles uploads that exceed the configured limit.
   *
   * @param ex the exception
   * @return a 400 response
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleTooLarge(MaxUploadSizeExceededException ex) {
    ErrorDetail detail =
        ErrorDetail.withInfo().put("max", ex.getMaxUploadSize()).build(ErrorCode.FILE_TOO_LARGE);
    return ResponseEntity.status(ErrorCode.INVALID_FILE.status())
        .body(ApiErrorResponse.of(ErrorCode.INVALID_FILE, List.of(detail)));
  }

  /**
   * Handles requests to the import endpoint that carry no {@code file} part.
   *
   * @param ex the exception
   * @return a 400 response
   */
  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
    ErrorDetail detail = ErrorDetail.of(ErrorCode.MISSING_FILE);
    return ResponseEntity.status(ErrorCode.INVALID_FILE.status())
        .body(ApiErrorResponse.of(ErrorCode.INVALID_FILE, List.of(detail)));
  }

  /**
   * Last-resort handler.
   *
   * @param ex the exception
   * @return a 500 response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
        .body(ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, List.of()));
  }
}
