package com.joseortiz.inventory.excel;

import com.joseortiz.inventory.config.InventoryProperties;
import com.joseortiz.inventory.error.ErrorCode;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.error.InvalidFileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Parses Office Open XML workbooks ({@code .xlsx}) with Apache POI.
 *
 * <p>Formulas are evaluated with POI's {@link FormulaEvaluator}; a cell whose formula cannot be
 * evaluated, or that already holds an Excel error such as {@code #DIV/0!}, produces a {@link
 * ErrorCode#FORMULA_EVALUATION_ERROR} detail while parsing continues, so the client receives every
 * problem in one response.
 */
@Component
public class XlsxSpreadsheetParser implements SpreadsheetParser {

  private static final Logger log = LoggerFactory.getLogger(XlsxSpreadsheetParser.class);
  private static final String EXTENSION = "xlsx";

  private final InventoryProperties properties;

  /**
   * Creates the parser.
   *
   * @param properties import limits
   */
  public XlsxSpreadsheetParser(InventoryProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean supports(String extension) {
    return EXTENSION.equals(extension);
  }

  @Override
  public ParsedSheet parse(InputStream input, String fileName) {
    try (Workbook workbook = WorkbookFactory.create(input)) {
      Sheet sheet = workbook.getSheetAt(0);
      validateHeader(sheet);
      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
      return readRows(sheet, evaluator);
    } catch (InvalidFileException e) {
      throw e;
    } catch (IOException | RuntimeException e) {
      // POI throws a variety of runtime exceptions for corrupt, encrypted or non-OOXML content.
      log.debug("Unreadable workbook '{}'", fileName, e);
      throw new InvalidFileException(
          ErrorDetail.withInfo()
              .put("fileName", fileName)
              .put("reason", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
              .build(ErrorCode.FILE_UNREADABLE));
    }
  }

  private void validateHeader(Sheet sheet) {
    Row header = sheet.getRow(sheet.getFirstRowNum());
    if (sheet.getPhysicalNumberOfRows() == 0 || header == null) {
      throw new InvalidFileException(ErrorDetail.of(ErrorCode.MISSING_HEADER_ROW));
    }
    List<ErrorDetail> mismatches = new ArrayList<>();
    for (SpreadsheetColumn column : SpreadsheetColumn.values()) {
      Cell cell = header.getCell(column.index(), MissingCellPolicy.RETURN_BLANK_AS_NULL);
      String actual = cell == null ? "" : cell.toString();
      if (!column.matches(actual)) {
        mismatches.add(
            ErrorDetail.withInfo()
                .put("column", CellReference.convertNumToColString(column.index()))
                .put("expected", column.header())
                .put("actual", actual.trim())
                .build(ErrorCode.INVALID_HEADER));
      }
    }
    if (!mismatches.isEmpty()) {
      throw new InvalidFileException(mismatches);
    }
  }

  private ParsedSheet readRows(Sheet sheet, FormulaEvaluator evaluator) {
    List<ProductRow> rows = new ArrayList<>();
    List<ErrorDetail> cellErrors = new ArrayList<>();
    int maxRows = properties.importSettings().maxRows();

    for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null || isBlank(row)) {
        continue;
      }
      int excelRowNumber = i + 1;
      Map<SpreadsheetColumn, RawCell> cells = new EnumMap<>(SpreadsheetColumn.class);
      for (SpreadsheetColumn column : SpreadsheetColumn.values()) {
        Cell cell = row.getCell(column.index(), MissingCellPolicy.RETURN_BLANK_AS_NULL);
        cells.put(column, readCell(cell, column, excelRowNumber, evaluator, cellErrors));
      }
      rows.add(new ProductRow(excelRowNumber, cells));
      if (rows.size() > maxRows) {
        throw new InvalidFileException(
            ErrorDetail.withInfo()
                .put("rows", countDataRows(sheet))
                .put("max", maxRows)
                .build(ErrorCode.TOO_MANY_ROWS));
      }
    }
    if (rows.isEmpty()) {
      throw new InvalidFileException(ErrorDetail.of(ErrorCode.NO_DATA_ROWS));
    }
    return new ParsedSheet(rows, cellErrors);
  }

  private RawCell readCell(
      Cell cell,
      SpreadsheetColumn column,
      int rowNumber,
      FormulaEvaluator evaluator,
      List<ErrorDetail> cellErrors) {
    String ref = CellReference.convertNumToColString(column.index()) + rowNumber;
    if (cell == null) {
      return RawCell.blank(ref);
    }
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      try {
        CellValue value = evaluator.evaluate(cell);
        return fromValue(cell, value, ref, rowNumber, cellErrors);
      } catch (RuntimeException e) {
        log.debug("Formula evaluation failed at {}", ref, e);
        cellErrors.add(formulaError(ref, rowNumber, column, e.getMessage()));
        return RawCell.error(ref);
      }
    }
    return switch (type) {
      case STRING -> RawCell.text(ref, cell.getStringCellValue());
      case BOOLEAN -> RawCell.text(ref, String.valueOf(cell.getBooleanCellValue()));
      case NUMERIC ->
          DateUtil.isCellDateFormatted(cell)
              ? RawCell.date(ref, cell.getLocalDateTimeCellValue().toLocalDate())
              : RawCell.number(ref, cell.getNumericCellValue());
      case ERROR -> {
        cellErrors.add(
            formulaError(ref, rowNumber, column, FormulaError.forInt(cell.getErrorCellValue()).getString()));
        yield RawCell.error(ref);
      }
      default -> RawCell.blank(ref);
    };
  }

  private RawCell fromValue(
      Cell cell, CellValue value, String ref, int rowNumber, List<ErrorDetail> cellErrors) {
    return switch (value.getCellType()) {
      case STRING -> RawCell.text(ref, value.getStringValue());
      case BOOLEAN -> RawCell.text(ref, String.valueOf(value.getBooleanValue()));
      case NUMERIC ->
          DateUtil.isCellDateFormatted(cell)
              ? RawCell.date(ref, DateUtil.getLocalDateTime(value.getNumberValue()).toLocalDate())
              : RawCell.number(ref, value.getNumberValue());
      case ERROR -> {
        cellErrors.add(
            formulaError(
                ref,
                rowNumber,
                SpreadsheetColumn.values()[cell.getColumnIndex()],
                FormulaError.forInt(value.getErrorValue()).getString()));
        yield RawCell.error(ref);
      }
      default -> RawCell.blank(ref);
    };
  }

  private static ErrorDetail formulaError(
      String ref, int rowNumber, SpreadsheetColumn column, String reason) {
    return ErrorDetail.withInfo()
        .put("row", rowNumber)
        .put("cell", ref)
        .put("field", column.field())
        .put("reason", reason == null ? "unknown error" : reason)
        .build(ErrorCode.FORMULA_EVALUATION_ERROR);
  }

  private static boolean isBlank(Row row) {
    for (SpreadsheetColumn column : SpreadsheetColumn.values()) {
      Cell cell = row.getCell(column.index(), MissingCellPolicy.RETURN_BLANK_AS_NULL);
      if (cell != null && cell.getCellType() != CellType.BLANK) {
        return false;
      }
    }
    return true;
  }

  private static int countDataRows(Sheet sheet) {
    int count = 0;
    for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row != null && !isBlank(row)) {
        count++;
      }
    }
    return count;
  }
}
