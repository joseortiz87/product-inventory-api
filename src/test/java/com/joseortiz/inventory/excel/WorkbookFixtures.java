package com.joseortiz.inventory.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Builds small in-memory .xlsx files for tests. */
public final class WorkbookFixtures {

  /** Marker for a formula cell. */
  public record Formula(String expression) {}

  private final List<String> headers = new ArrayList<>();
  private final List<Object[]> rows = new ArrayList<>();

  private WorkbookFixtures() {}

  /** @return a workbook with the standard header row */
  public static WorkbookFixtures standard() {
    WorkbookFixtures f = new WorkbookFixtures();
    for (SpreadsheetColumn c : SpreadsheetColumn.values()) {
      f.headers.add(c.header());
    }
    return f;
  }

  /**
   * @param headers custom header texts
   * @return a workbook with the given header row
   */
  public static WorkbookFixtures withHeaders(String... headers) {
    WorkbookFixtures f = new WorkbookFixtures();
    f.headers.addAll(Arrays.asList(headers));
    return f;
  }

  /**
   * Adds a data row. Values may be String, Number, LocalDate, {@link Formula} or {@code null}.
   *
   * @param values cell values in column order
   * @return this
   */
  public WorkbookFixtures row(Object... values) {
    rows.add(values);
    return this;
  }

  /**
   * Adds a valid product row.
   *
   * @param sku sku
   * @param date purchase date
   * @return this
   */
  public WorkbookFixtures product(String sku, LocalDate date) {
    return row(sku, "Name " + sku, "Category", date, 10.5, 3);
  }

  /** @return the .xlsx bytes */
  public byte[] bytes() {
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("Inventory");
      CellStyle dateStyle = wb.createCellStyle();
      dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
      Row header = sheet.createRow(0);
      for (int i = 0; i < headers.size(); i++) {
        header.createCell(i).setCellValue(headers.get(i));
      }
      for (int r = 0; r < rows.size(); r++) {
        Row row = sheet.createRow(r + 1);
        Object[] values = rows.get(r);
        for (int c = 0; c < values.length; c++) {
          Object v = values[c];
          if (v == null) {
            continue;
          }
          Cell cell = row.createCell(c);
          if (v instanceof Formula f) {
            cell.setCellFormula(f.expression());
          } else if (v instanceof LocalDate d) {
            cell.setCellValue(d);
            cell.setCellStyle(dateStyle);
          } else if (v instanceof Number n) {
            cell.setCellValue(n.doubleValue());
          } else {
            cell.setCellValue(String.valueOf(v));
          }
        }
      }
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
