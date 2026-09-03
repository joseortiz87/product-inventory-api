package com.joseortiz.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joseortiz.inventory.dto.ImportResultResponse;
import com.joseortiz.inventory.error.BadRequestException;
import com.joseortiz.inventory.error.ErrorCode;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.error.NotFoundException;
import com.joseortiz.inventory.error.ValidationException;
import com.joseortiz.inventory.service.ProductCommandService;
import com.joseortiz.inventory.service.ProductImportService;
import com.joseortiz.inventory.service.ProductQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies the HTTP contract, in particular the error envelope shape. */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

  @Autowired private MockMvc mvc;
  @MockitoBean private ProductImportService importService;
  @MockitoBean private ProductQueryService queryService;
  @MockitoBean private ProductCommandService commandService;

  private static MockMultipartFile upload() {
    return new MockMultipartFile("file", "inventory.xlsx", "application/octet-stream", new byte[] {1});
  }

  @Test
  void importReturns201WithCounts() throws Exception {
    when(importService.importFile(any())).thenReturn(new ImportResultResponse("inventory.xlsx", 3, 10));

    mvc.perform(multipart("/api/products/import").file(upload()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fileName").value("inventory.xlsx"))
        .andExpect(jsonPath("$.importedRows").value(3))
        .andExpect(jsonPath("$.totalProducts").value(10));
  }

  @Test
  void validationErrorsUseTheDocumentedEnvelope() throws Exception {
    ErrorDetail detail =
        ErrorDetail.withInfo()
            .put("row", 7)
            .put("sku", "ABC-1")
            .put("purchaseDate", "2026-01-05")
            .put("firstRow", 3)
            .build(ErrorCode.DUPLICATE_ENTRY_IN_FILE);
    when(importService.importFile(any())).thenThrow(new ValidationException(List.of(detail)));

    mvc.perform(multipart("/api/products/import").file(upload()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.details.length()").value(1))
        .andExpect(jsonPath("$.error.details[0].code").value("DUPLICATE_ENTRY_IN_FILE"))
        .andExpect(
            jsonPath("$.error.details[0].message")
                .value("Row 7: SKU 'ABC-1' with purchase date 2026-01-05 also appears on row 3."))
        .andExpect(jsonPath("$.error.details[0].info.row").value(7))
        .andExpect(jsonPath("$.error.details[0].info.firstRow").value(3));
  }

  @Test
  void missingFilePartIsA400WithTheSameEnvelope() throws Exception {
    mvc.perform(multipart("/api/products/import"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_FILE"))
        .andExpect(jsonPath("$.error.details[0].code").value("MISSING_FILE"));
  }

  @Test
  void invalidSortIsA400() throws Exception {
    when(queryService.findAll(anyInt(), anyInt(), anyString()))
        .thenThrow(
            new BadRequestException(
                ErrorDetail.withInfo().put("field", "sort").put("value", "x").put("accepted", "sku").build(ErrorCode.INVALID_SORT_FIELD)));

    mvc.perform(get("/api/products").param("sort", "x"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.error.details[0].code").value("INVALID_SORT_FIELD"))
        .andExpect(jsonPath("$.error.details[0].info.field").value("sort"));
  }

  @Test
  void unexpectedFailuresDoNotLeakInternals() throws Exception {
    when(queryService.summary()).thenThrow(new IllegalStateException("boom: jdbc://secret"));

    mvc.perform(get("/api/products/summary"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.error.details").isEmpty());
  }

  @Test
  void clearReturns204() throws Exception {
    mvc.perform(delete("/api/products")).andExpect(status().isNoContent());
  }

  @Test
  void deleteReturns204() throws Exception {
    mvc.perform(delete("/api/products/7")).andExpect(status().isNoContent());
  }

  @Test
  void deletingAMissingProductIsA404InTheSameEnvelope() throws Exception {
    doThrow(NotFoundException.product(7)).when(commandService).delete(7);

    mvc.perform(delete("/api/products/7"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.error.details[0].code").value("PRODUCT_NOT_FOUND"))
        .andExpect(jsonPath("$.error.details[0].info.id").value(7))
        .andExpect(
            jsonPath("$.error.details[0].message")
                .value("Product 7 does not exist. It may have been deleted already."));
  }
}
