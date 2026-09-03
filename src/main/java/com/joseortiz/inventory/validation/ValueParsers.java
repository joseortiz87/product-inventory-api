package com.joseortiz.inventory.validation;

import com.joseortiz.inventory.config.InventoryProperties;
import com.joseortiz.inventory.excel.RawCell;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Lenient-but-safe conversions from {@link RawCell}s to domain types.
 *
 * <p>Shared by the row validator (to decide whether a value is acceptable) and the row mapper (to
 * build the entity), so both always agree on what a valid value is.
 */
@Component
public class ValueParsers {

  /** Characters allowed in a currency/number after stripping symbols and grouping separators. */
  private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");

  /** Symbols removed before parsing a textual amount: currency signs, spaces, grouping commas. */
  private static final Pattern NOISE = Pattern.compile("[\\s,$€£¥]");

  private final List<DateTimeFormatter> dateFormatters;
  private final List<String> datePatterns;

  /**
   * Creates the parsers from the configured date patterns.
   *
   * @param properties import settings
   */
  public ValueParsers(InventoryProperties properties) {
    this.datePatterns = List.copyOf(properties.importSettings().datePatterns());
    this.dateFormatters =
        datePatterns.stream()
            .map(p -> DateTimeFormatter.ofPattern(p, Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART))
            .toList();
  }

  /** @return the accepted textual date patterns, for error messages */
  public List<String> datePatterns() {
    return datePatterns;
  }

  /**
   * Reads a date from a native Excel date cell or from text matching one of the configured
   * patterns.
   *
   * @param cell the cell
   * @return the date, empty when the cell is blank or unparseable
   */
  public Optional<LocalDate> parseDate(RawCell cell) {
    return switch (cell.kind()) {
      case DATE -> Optional.of(cell.date());
      case TEXT -> parseTextDate(cell.text());
      default -> Optional.empty();
    };
  }

  private Optional<LocalDate> parseTextDate(String text) {
    for (DateTimeFormatter formatter : dateFormatters) {
      try {
        return Optional.of(LocalDate.parse(text, formatter));
      } catch (DateTimeParseException ignored) {
        // try the next pattern
      }
    }
    return Optional.empty();
  }

  /**
   * Reads a decimal amount from a numeric cell or formatted text such as {@code $1,250.50}.
   *
   * @param cell the cell
   * @return the amount, empty when blank or not numeric
   */
  public Optional<BigDecimal> parseDecimal(RawCell cell) {
    return switch (cell.kind()) {
      case NUMBER -> Optional.of(BigDecimal.valueOf(cell.number()));
      case TEXT -> parseTextDecimal(cell.text());
      default -> Optional.empty();
    };
  }

  private Optional<BigDecimal> parseTextDecimal(String text) {
    String cleaned = NOISE.matcher(text).replaceAll("");
    if (!DECIMAL.matcher(cleaned).matches()) {
      return Optional.empty();
    }
    return Optional.of(new BigDecimal(cleaned));
  }

  /**
   * Whether a decimal has no fractional part (e.g. {@code 12} or {@code 12.0}).
   *
   * @param value the value
   * @return {@code true} for whole numbers
   */
  public static boolean isWhole(BigDecimal value) {
    return value.stripTrailingZeros().scale() <= 0;
  }
}
