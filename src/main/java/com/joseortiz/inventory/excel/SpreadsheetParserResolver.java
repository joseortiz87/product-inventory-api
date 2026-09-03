package com.joseortiz.inventory.excel;

import com.joseortiz.inventory.config.InventoryProperties;
import com.joseortiz.inventory.error.ErrorCode;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.error.InvalidFileException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Picks the {@link SpreadsheetParser} able to handle an uploaded file.
 *
 * <p>The accepted extensions are configured in {@code inventory.import.accepted-extensions}, which
 * lets an operator disable a format without a code change even if a parser bean exists for it.
 */
@Component
public class SpreadsheetParserResolver {

  private final List<SpreadsheetParser> parsers;
  private final InventoryProperties properties;

  /**
   * Creates the resolver.
   *
   * @param parsers every parser bean in the context
   * @param properties import settings with the accepted extensions
   */
  public SpreadsheetParserResolver(List<SpreadsheetParser> parsers, InventoryProperties properties) {
    this.parsers = List.copyOf(parsers);
    this.properties = properties;
  }

  /**
   * Resolves the parser for a file name.
   *
   * @param fileName original file name
   * @return the parser
   * @throws InvalidFileException when the extension is not accepted or no parser supports it
   */
  public SpreadsheetParser resolve(String fileName) {
    String extension = extensionOf(fileName);
    List<String> accepted = properties.importSettings().acceptedExtensions();
    if (accepted.contains(extension)) {
      for (SpreadsheetParser parser : parsers) {
        if (parser.supports(extension)) {
          return parser;
        }
      }
    }
    throw new InvalidFileException(
        ErrorDetail.withInfo()
            .put("fileName", fileName)
            .put("accepted", String.join(", ", accepted))
            .build(ErrorCode.UNSUPPORTED_FILE_TYPE));
  }

  /**
   * Extracts a lower-case extension.
   *
   * @param fileName file name, may be {@code null}
   * @return the extension without the dot, or an empty string
   */
  static String extensionOf(String fileName) {
    if (fileName == null) {
      return "";
    }
    int dot = fileName.lastIndexOf('.');
    return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
  }
}
