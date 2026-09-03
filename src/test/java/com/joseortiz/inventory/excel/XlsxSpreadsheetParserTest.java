package com.joseortiz.inventory.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joseortiz.inventory.TestProperties;
import com.joseortiz.inventory.error.ErrorCode;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.error.InvalidFileException;
import com.joseortiz.inventory.excel.WorkbookFixtures.Formula;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class XlsxSpreadsheetParserTest {

  private final XlsxSpreadsheetParser parser = new XlsxSpreadsheetParser(TestProperties.defaults());

  private ParsedSheet parse(WorkbookFixtures fixture) {
    return parser.parse(new ByteArrayInputStream(fixture.bytes()), "test.xlsx");
  }

  @Test
  void readsNativeTypesAndEvaluatesFormulas() {
    ParsedSheet sheet =
        parse(
            WorkbookFixtures.standard()
                .row("SKU-1", "Laptop", "Electronics", LocalDate.of(2026, 1, 15), 1200.5, 4)
                .row("SKU-2", "Mouse", "Accessories", "2026-02-01", new Formula("10*2"), new Formula("D2-D2+3")));

    assertThat(sheet.cellErrors()).isEmpty();
    assertThat(sheet.rows()).hasSize(2);

    ProductRow first = sheet.rows().get(0);
    assertThat(first.rowNumber()).isEqualTo(2);
    assertThat(first.cell(SpreadsheetColumn.PURCHASE_DATE).kind()).isEqualTo(RawCell.Kind.DATE);
    assertThat(first.cell(SpreadsheetColumn.PURCHASE_DATE).date()).isEqualTo(LocalDate.of(2026, 1, 15));
    assertThat(first.cell(SpreadsheetColumn.UNIT_PRICE).number()).isEqualTo(1200.5);

    ProductRow second = sheet.rows().get(1);
    assertThat(second.cell(SpreadsheetColumn.PURCHASE_DATE).kind()).isEqualTo(RawCell.Kind.TEXT);
    assertThat(second.cell(SpreadsheetColumn.UNIT_PRICE).number()).isEqualTo(20.0);
    assertThat(second.cell(SpreadsheetColumn.QUANTITY).number()).isEqualTo(3.0);
  }

  @Test
  void reportsFormulaErrorsWithoutAborting() {
    ParsedSheet sheet =
        parse(
            WorkbookFixtures.standard()
                .row("SKU-1", "Laptop", "Electronics", LocalDate.of(2026, 1, 15), new Formula("1/0"), 4)
                .row("SKU-2", "Mouse", "Accessories", LocalDate.of(2026, 1, 15), 5, new Formula("B3*2")));

    assertThat(sheet.rows()).hasSize(2);
    assertThat(sheet.rows().get(0).cell(SpreadsheetColumn.UNIT_PRICE).isError()).isTrue();
    assertThat(sheet.cellErrors())
        .extracting(ErrorDetail::code)
        .containsOnly(ErrorCode.FORMULA_EVALUATION_ERROR.code());
    assertThat(sheet.cellErrors().get(0).info())
        .containsEntry("row", 2)
        .containsEntry("cell", "E2")
        .containsEntry("field", "unitPrice")
        .containsEntry("reason", "#DIV/0!");
    assertThat(sheet.cellErrors().get(1).info()).containsEntry("cell", "F3");
  }

  @Test
  void skipsBlankRows() {
    ParsedSheet sheet =
        parse(
            WorkbookFixtures.standard()
                .product("A", LocalDate.of(2026, 1, 1))
                .row()
                .row(null, null, null, null, null, null)
                .product("B", LocalDate.of(2026, 1, 2)));
    assertThat(sheet.rows()).extracting(ProductRow::rowNumber).containsExactly(2, 5);
  }

  @Test
  void rejectsWrongHeaders() {
    WorkbookFixtures fixture =
        WorkbookFixtures.withHeaders("SKU", "Product Name", "Category", "Purchase Date", "Price", "Quantity")
            .product("A", LocalDate.of(2026, 1, 1));

    assertThatThrownBy(() -> parse(fixture))
        .isInstanceOfSatisfying(
            InvalidFileException.class,
            ex -> {
              assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_FILE);
              assertThat(ex.getDetails()).hasSize(2);
              assertThat(ex.getDetails().get(0).info())
                  .containsEntry("column", "A")
                  .containsEntry("expected", "Product SKU")
                  .containsEntry("actual", "SKU");
              assertThat(ex.getDetails().get(1).info()).containsEntry("column", "E");
            });
  }

  @Test
  void acceptsHeadersIgnoringCaseAndWhitespace() {
    ParsedSheet sheet =
        parse(
            WorkbookFixtures.withHeaders(
                    " product sku ", "PRODUCT NAME", "category", "Purchase date", "unit PRICE", "Quantity ")
                .product("A", LocalDate.of(2026, 1, 1)));
    assertThat(sheet.rows()).hasSize(1);
  }

  @Test
  void rejectsSheetWithoutDataRows() {
    assertThatThrownBy(() -> parse(WorkbookFixtures.standard()))
        .isInstanceOfSatisfying(
            InvalidFileException.class,
            ex -> assertThat(ex.getDetails()).extracting(ErrorDetail::code).containsExactly("NO_DATA_ROWS"));
  }

  @Test
  void rejectsTooManyRows() {
    XlsxSpreadsheetParser limited = new XlsxSpreadsheetParser(TestProperties.withMaxRows(2));
    WorkbookFixtures fixture =
        WorkbookFixtures.standard()
            .product("A", LocalDate.of(2026, 1, 1))
            .product("B", LocalDate.of(2026, 1, 1))
            .product("C", LocalDate.of(2026, 1, 1));

    assertThatThrownBy(() -> limited.parse(new ByteArrayInputStream(fixture.bytes()), "big.xlsx"))
        .isInstanceOfSatisfying(
            InvalidFileException.class,
            ex -> assertThat(ex.getDetails().get(0).info()).containsEntry("rows", 3).containsEntry("max", 2));
  }

  @Test
  void rejectsUnreadableContent() {
    byte[] garbage = "definitely not a workbook".getBytes();
    assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(garbage), "broken.xlsx"))
        .isInstanceOfSatisfying(
            InvalidFileException.class,
            ex -> assertThat(ex.getDetails()).extracting(ErrorDetail::code).containsExactly("FILE_UNREADABLE"));
  }
}
