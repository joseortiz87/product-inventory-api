package com.joseortiz.inventory.excel;

import java.time.LocalDate;

/**
 * Type-preserving snapshot of a spreadsheet cell after formula evaluation.
 *
 * <p>Keeping the native type (number vs. date vs. text) lets the validators accept both a real
 * Excel date cell and a textual date such as {@code 2024-03-01}, and both a numeric price and a
 * formatted one such as {@code $1,250.00}.
 *
 * @param ref A1-style cell reference, e.g. {@code E7}
 * @param kind resolved cell kind
 * @param text textual content when {@link Kind#TEXT}, otherwise {@code null}
 * @param number numeric content when {@link Kind#NUMBER}, otherwise {@code null}
 * @param date date content when {@link Kind#DATE}, otherwise {@code null}
 */
public record RawCell(String ref, Kind kind, String text, Double number, LocalDate date) {

  /** Resolved kind of a cell. */
  public enum Kind {
    /** Empty cell. */
    BLANK,
    /** Text (or boolean rendered as text). */
    TEXT,
    /** Numeric value. */
    NUMBER,
    /** Numeric value formatted as a date in Excel. */
    DATE,
    /** A formula that failed to evaluate; already reported, validators skip it. */
    ERROR
  }

  /**
   * @param ref cell reference
   * @return an empty cell
   */
  public static RawCell blank(String ref) {
    return new RawCell(ref, Kind.BLANK, null, null, null);
  }

  /**
   * @param ref cell reference
   * @param text trimmed text; blank text yields a {@link Kind#BLANK} cell
   * @return a text cell
   */
  public static RawCell text(String ref, String text) {
    String trimmed = text == null ? "" : text.trim();
    return trimmed.isEmpty() ? blank(ref) : new RawCell(ref, Kind.TEXT, trimmed, null, null);
  }

  /**
   * @param ref cell reference
   * @param number numeric value
   * @return a numeric cell
   */
  public static RawCell number(String ref, double number) {
    return new RawCell(ref, Kind.NUMBER, null, number, null);
  }

  /**
   * @param ref cell reference
   * @param date date value
   * @return a date cell
   */
  public static RawCell date(String ref, LocalDate date) {
    return new RawCell(ref, Kind.DATE, null, null, date);
  }

  /**
   * @param ref cell reference
   * @return a cell whose formula failed to evaluate
   */
  public static RawCell error(String ref) {
    return new RawCell(ref, Kind.ERROR, null, null, null);
  }

  /** @return {@code true} for empty cells */
  public boolean isBlank() {
    return kind == Kind.BLANK;
  }

  /** @return {@code true} for cells whose formula failed */
  public boolean isError() {
    return kind == Kind.ERROR;
  }

  /**
   * Best-effort textual rendering used in error payloads.
   *
   * @return the display value, or {@code null} for blank/error cells
   */
  public Object displayValue() {
    return switch (kind) {
      case TEXT -> text;
      case NUMBER -> number;
      case DATE -> date.toString();
      case BLANK, ERROR -> null;
    };
  }
}
