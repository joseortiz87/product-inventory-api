package com.joseortiz.inventory.controller;

import com.joseortiz.inventory.dto.ImportResultResponse;
import com.joseortiz.inventory.dto.InventorySummaryResponse;
import com.joseortiz.inventory.dto.PageResponse;
import com.joseortiz.inventory.dto.ProductResponse;
import com.joseortiz.inventory.error.ApiErrorResponse;
import com.joseortiz.inventory.service.ProductImportService;
import com.joseortiz.inventory.service.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST resource for the product inventory.
 */
@RestController
@RequestMapping(path = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Products", description = "Import and query the product inventory")
public class ProductController {

  private final ProductImportService importService;
  private final ProductQueryService queryService;

  /**
   * Creates the controller.
   *
   * @param importService import use case
   * @param queryService read use cases
   */
  public ProductController(ProductImportService importService, ProductQueryService queryService) {
    this.importService = importService;
    this.queryService = queryService;
  }

  /**
   * Imports a spreadsheet.
   *
   * @param file the {@code .xlsx} upload in the {@code file} form field
   * @return import counts
   */
  @Operation(
      summary = "Import an inventory spreadsheet",
      description =
          "Accepts an .xlsx file with the columns Product SKU, Product Name, Category, Purchase Date,"
              + " Unit Price and Quantity. The import is atomic: any invalid or duplicated row"
              + " rejects the whole file and every problem is listed in the response.")
  @ApiResponse(responseCode = "201", description = "Rows imported")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid file or invalid rows",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ImportResultResponse importFile(
      @Parameter(description = "Spreadsheet to import") @RequestPart("file") MultipartFile file) {
    return importService.importFile(file);
  }

  /**
   * Lists products with server-side pagination and sorting.
   *
   * @param page zero-based page index
   * @param size page size
   * @param sort {@code field,direction}; sortable fields: sku, name, category, purchaseDate,
   *     unitPrice, quantity, stockAge
   * @return the page
   */
  @Operation(summary = "List products", description = "Paginated and sortable product listing.")
  @ApiResponse(responseCode = "200", description = "Page of products")
  @ApiResponse(
      responseCode = "400",
      description = "Unknown sort field or page size too large",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @GetMapping
  public PageResponse<ProductResponse> list(
      @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort as field,direction e.g. stockAge,desc")
          @RequestParam(defaultValue = ProductQueryService.DEFAULT_SORT)
          String sort) {
    return queryService.findAll(page, size, sort);
  }

  /**
   * Returns the dashboard summary.
   *
   * @return totals and average stock age
   */
  @Operation(summary = "Inventory summary", description = "Totals shown on the dashboard cards.")
  @GetMapping("/summary")
  public InventorySummaryResponse summary() {
    return queryService.summary();
  }

  /**
   * Deletes every product so a file can be imported again.
   */
  @Operation(summary = "Clear the inventory", description = "Deletes all products. Intended for demos.")
  @ApiResponse(responseCode = "204", description = "Inventory cleared")
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clear() {
    queryService.deleteAll();
  }
}
