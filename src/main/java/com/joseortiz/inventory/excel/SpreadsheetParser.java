package com.joseortiz.inventory.excel;

import com.joseortiz.inventory.error.InvalidFileException;
import java.io.InputStream;

/**
 * Strategy for turning an uploaded file into {@link ProductRow}s.
 *
 * <p>Implementations are discovered by {@link SpreadsheetParserResolver} through the file
 * extension, so supporting a new format (CSV, legacy {@code .xls}...) only requires a new bean.
 */
public interface SpreadsheetParser {

  /**
   * @param extension lower-case file extension without the dot
   * @return whether this parser handles the extension
   */
  boolean supports(String extension);

  /**
   * Parses the file.
   *
   * @param input file content; the caller closes it
   * @param fileName original file name, used in error messages
   * @return parsed rows and formula errors
   * @throws InvalidFileException when the file is structurally unusable (unreadable, wrong headers,
   *     no data, too many rows)
   */
  ParsedSheet parse(InputStream input, String fileName);
}
