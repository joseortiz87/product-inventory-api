package com.joseortiz.inventory.error;

import java.util.List;

/**
 * Raised when an upload cannot be processed as an inventory spreadsheet at all: missing file,
 * unsupported type, unreadable content or unexpected column headers.
 */
public class InvalidFileException extends ApiException {

  /**
   * Creates the exception with a single detail.
   *
   * @param detail the problem
   */
  public InvalidFileException(ErrorDetail detail) {
    super(ErrorCode.INVALID_FILE, List.of(detail));
  }

  /**
   * Creates the exception with several details (e.g. multiple wrong headers).
   *
   * @param details the problems
   */
  public InvalidFileException(List<ErrorDetail> details) {
    super(ErrorCode.INVALID_FILE, details);
  }
}
