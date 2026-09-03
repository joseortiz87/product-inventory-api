package com.joseortiz.inventory.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.joseortiz.inventory.TestProperties;
import com.joseortiz.inventory.excel.RawCell;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValueParsersTest {

  private final ValueParsers parsers = new ValueParsers(TestProperties.defaults());

  @ParameterizedTest
  @CsvSource({"2026-03-05, 2026-03-05", "03/05/2026, 2026-03-05", "3/5/2026, 2026-03-05", "05-Mar-2026, 2026-03-05"})
  void parsesConfiguredTextDatePatterns(String text, LocalDate expected) {
    assertThat(parsers.parseDate(RawCell.text("D2", text))).contains(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-13-01", "31/31/2026", "yesterday", "2026/03/05"})
  void rejectsUnparseableDates(String text) {
    assertThat(parsers.parseDate(RawCell.text("D2", text))).isEmpty();
  }

  @Test
  void acceptsNativeDateCells() {
    assertThat(parsers.parseDate(RawCell.date("D2", LocalDate.of(2026, 1, 1)))).contains(LocalDate.of(2026, 1, 1));
    assertThat(parsers.parseDate(RawCell.number("D2", 45000))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"'$1,250.50', 1250.50", "'12', 12", "'-4.5', -4.5", "'€ 99.99', 99.99"})
  void parsesFormattedAmounts(String text, BigDecimal expected) {
    assertThat(parsers.parseDecimal(RawCell.text("E2", text))).contains(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"twelve", "1.2.3", "12abc", ""})
  void rejectsNonNumericText(String text) {
    assertThat(parsers.parseDecimal(RawCell.text("E2", text))).isEmpty();
  }

  @Test
  void detectsWholeNumbers() {
    assertThat(ValueParsers.isWhole(new BigDecimal("12"))).isTrue();
    assertThat(ValueParsers.isWhole(new BigDecimal("12.000"))).isTrue();
    assertThat(ValueParsers.isWhole(new BigDecimal("12.5"))).isFalse();
  }
}
