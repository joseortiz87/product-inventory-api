package com.joseortiz.inventory.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.joseortiz.inventory.TestProperties;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.excel.RawCell;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductRowValidatorTest {

  private static final Clock FIXED = Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC);

  private final ProductRowValidator validator =
      new ProductRowValidator(new ValueParsers(TestProperties.defaults()), FIXED);

  @Test
  void validRowHasNoErrors() {
    assertThat(validator.validate(RowFixtures.valid(2))).isEmpty();
  }

  @Test
  void reportsEveryMissingRequiredField() {
    List<ErrorDetail> errors = validator.validate(RowFixtures.row(4));
    assertThat(errors).hasSize(6).allMatch(e -> e.code().equals("REQUIRED_FIELD_MISSING"));
    assertThat(errors).extracting(e -> e.info().get("field"))
        .containsExactly("sku", "name", "category", "purchaseDate", "unitPrice", "quantity");
    assertThat(errors.get(0).info()).containsEntry("row", 4).containsEntry("cell", "A4");
    assertThat(errors.get(0).message()).isEqualTo("Row 4: 'sku' is required.");
  }

  @Test
  void reportsInvalidDateFormatWithAcceptedPatterns() {
    List<ErrorDetail> errors =
        validator.validate(RowFixtures.row(3, "S", "N", "C", "31/31/2026", 1, 1));
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).code()).isEqualTo("INVALID_DATE_FORMAT");
    assertThat(errors.get(0).info())
        .containsEntry("value", "31/31/2026")
        .containsEntry("accepted", "yyyy-MM-dd, MM/dd/yyyy, M/d/yyyy, dd-MMM-yyyy");
  }

  @Test
  void rejectsFutureDates() {
    List<ErrorDetail> errors =
        validator.validate(RowFixtures.row(3, "S", "N", "C", LocalDate.of(2026, 9, 4), 1, 1));
    assertThat(errors).extracting(ErrorDetail::code).containsExactly("DATE_IN_FUTURE");
    assertThat(errors.get(0).info()).containsEntry("max", "2026-09-03");
  }

  @Test
  void acceptsToday() {
    assertThat(validator.validate(RowFixtures.row(3, "S", "N", "C", LocalDate.of(2026, 9, 3), 1, 1))).isEmpty();
  }

  @Test
  void validatesPriceAndQuantity() {
    List<ErrorDetail> errors =
        validator.validate(RowFixtures.row(5, "S", "N", "C", "2026-01-01", -4.5, 2.5));
    assertThat(errors).extracting(ErrorDetail::code).containsExactly("NEGATIVE_VALUE", "NOT_AN_INTEGER");
    assertThat(errors.get(0).info()).containsEntry("field", "unitPrice").containsEntry("value", -4.5).containsEntry("min", 0);
    assertThat(errors.get(1).message()).isEqualTo("Row 5: 'quantity' value 2.5 must be a whole number.");
  }

  @Test
  void rejectsNonNumericText() {
    List<ErrorDetail> errors =
        validator.validate(RowFixtures.row(5, "S", "N", "C", "2026-01-01", "twelve", "many"));
    assertThat(errors).extracting(ErrorDetail::code).containsExactly("INVALID_NUMBER", "INVALID_NUMBER");
  }

  @Test
  void acceptsFormattedCurrencyText() {
    assertThat(validator.validate(RowFixtures.row(5, "S", "N", "C", "2026-01-01", "$1,250.00", "12"))).isEmpty();
  }

  @Test
  void rejectsOverlongText() {
    String longText = "x".repeat(256);
    List<ErrorDetail> errors =
        validator.validate(RowFixtures.row(6, longText, "N", "C", "2026-01-01", 1, 1));
    assertThat(errors).extracting(ErrorDetail::code).containsExactly("TEXT_TOO_LONG");
    assertThat(errors.get(0).info()).containsEntry("max", 255);
  }

  @Test
  void skipsCellsAlreadyReportedAsFormulaErrors() {
    List<ErrorDetail> errors =
        validator.validate(
            RowFixtures.row(7, "S", "N", "C", "2026-01-01", RawCell.error("E7"), RawCell.error("F7")));
    assertThat(errors).isEmpty();
  }
}
