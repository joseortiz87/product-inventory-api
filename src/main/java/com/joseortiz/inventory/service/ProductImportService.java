package com.joseortiz.inventory.service;

import com.joseortiz.inventory.dto.ImportResultResponse;
import com.joseortiz.inventory.error.ErrorCode;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.error.InvalidFileException;
import com.joseortiz.inventory.error.ValidationException;
import com.joseortiz.inventory.excel.ParsedSheet;
import com.joseortiz.inventory.excel.ProductRow;
import com.joseortiz.inventory.excel.SpreadsheetParser;
import com.joseortiz.inventory.excel.SpreadsheetParserResolver;
import com.joseortiz.inventory.repository.ProductRepository;
import com.joseortiz.inventory.validation.ImportedProduct;
import com.joseortiz.inventory.validation.ValidationRule;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates a spreadsheet import: parse, validate every row, check uniqueness, persist.
 *
 * <p>The import is atomic: if any row is invalid nothing is written and a {@link
 * ValidationException} listing every problem is raised. Re-uploading a corrected file is cheap,
 * whereas a partially imported file is hard to reason about for the user.
 */
@Service
public class ProductImportService {

  private static final Logger log = LoggerFactory.getLogger(ProductImportService.class);

  private final SpreadsheetParserResolver parserResolver;
  private final ValidationRule<ProductRow> rowValidator;
  private final ValidationRule<List<ImportedProduct>> duplicateValidator;
  private final ProductRowMapper mapper;
  private final ProductRepository repository;

  /**
   * Creates the service.
   *
   * @param parserResolver selects the parser for the uploaded file type
   * @param rowValidator value rules applied to each row
   * @param duplicateValidator uniqueness rules applied to the batch
   * @param mapper row to entity mapping
   * @param repository product persistence
   */
  public ProductImportService(
      SpreadsheetParserResolver parserResolver,
      ValidationRule<ProductRow> rowValidator,
      ValidationRule<List<ImportedProduct>> duplicateValidator,
      ProductRowMapper mapper,
      ProductRepository repository) {
    this.parserResolver = parserResolver;
    this.rowValidator = rowValidator;
    this.duplicateValidator = duplicateValidator;
    this.mapper = mapper;
    this.repository = repository;
  }

  /**
   * Imports the uploaded spreadsheet.
   *
   * @param file multipart upload
   * @return counts of the import
   * @throws InvalidFileException when the file cannot be processed
   * @throws ValidationException when one or more rows are invalid or duplicated
   */
  @Transactional
  public ImportResultResponse importFile(MultipartFile file) {
    String fileName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
    if (file.isEmpty()) {
      throw new InvalidFileException(
          ErrorDetail.withInfo().put("fileName", fileName).build(ErrorCode.FILE_EMPTY));
    }

    SpreadsheetParser parser = parserResolver.resolve(fileName);
    ParsedSheet sheet = parse(parser, file, fileName);

    List<ErrorDetail> errors = new ArrayList<>(sheet.cellErrors());
    List<ImportedProduct> candidates = new ArrayList<>();
    for (ProductRow row : sheet.rows()) {
      List<ErrorDetail> rowErrors = rowValidator.validate(row);
      if (rowErrors.isEmpty() && !hasCellError(row)) {
        candidates.add(mapper.toProduct(row));
      } else {
        errors.addAll(rowErrors);
      }
    }
    errors.addAll(duplicateValidator.validate(candidates));

    if (!errors.isEmpty()) {
      errors.sort(Comparator.comparingInt(ProductImportService::rowOf));
      log.info("Import of '{}' rejected with {} error(s)", fileName, errors.size());
      throw new ValidationException(errors);
    }

    repository.saveAll(candidates.stream().map(ImportedProduct::product).toList());
    long total = repository.count();
    log.info("Imported {} row(s) from '{}'; inventory now holds {} product(s)", candidates.size(), fileName, total);
    return new ImportResultResponse(fileName, candidates.size(), total);
  }

  private static ParsedSheet parse(SpreadsheetParser parser, MultipartFile file, String fileName) {
    try (InputStream input = file.getInputStream()) {
      return parser.parse(input, fileName);
    } catch (IOException e) {
      throw new InvalidFileException(
          ErrorDetail.withInfo()
              .put("fileName", fileName)
              .put("reason", e.getMessage())
              .build(ErrorCode.FILE_UNREADABLE));
    }
  }

  private static boolean hasCellError(ProductRow row) {
    return row.cells().values().stream().anyMatch(cell -> cell.isError());
  }

  private static int rowOf(ErrorDetail detail) {
    Object row = detail.info().get("row");
    return row instanceof Integer i ? i : 0;
  }
}
