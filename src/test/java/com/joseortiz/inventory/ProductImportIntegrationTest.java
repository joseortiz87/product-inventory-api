package com.joseortiz.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joseortiz.inventory.excel.WorkbookFixtures;
import com.joseortiz.inventory.excel.WorkbookFixtures.Formula;
import com.joseortiz.inventory.repository.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end flow through the real parser, validators, H2 and the JSON contract. */
@SpringBootTest
@AutoConfigureMockMvc
class ProductImportIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ProductRepository repository;

  @BeforeEach
  void clean() {
    repository.deleteAll();
  }

  private static MockMultipartFile file(String name, byte[] bytes) {
    return new MockMultipartFile("file", name, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
  }

  @Test
  void importsTheShippedSampleThenRejectsItAsDuplicate() throws Exception {
    byte[] sample = Files.readAllBytes(Path.of("samples/inventory-sample.xlsx"));

    mvc.perform(multipart("/api/products/import").file(file("inventory-sample.xlsx", sample)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.importedRows").value(16))
        .andExpect(jsonPath("$.totalProducts").value(16));

    mvc.perform(get("/api/products").param("size", "5").param("sort", "stockAge,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(16))
        .andExpect(jsonPath("$.totalPages").value(4))
        .andExpect(jsonPath("$.content.length()").value(5))
        .andExpect(jsonPath("$.content[0].purchaseDate").value("2025-04-18"))
        .andExpect(jsonPath("$.content[0].stockAgeDays").isNumber());

    mvc.perform(get("/api/products/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalProducts").value(16))
        .andExpect(jsonPath("$.totalInventoryValue").isNumber())
        .andExpect(jsonPath("$.averageStockAgeDays").isNumber());

    mvc.perform(multipart("/api/products/import").file(file("inventory-sample.xlsx", sample)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.details.length()").value(16))
        .andExpect(jsonPath("$.error.details[*].code", hasItem("DUPLICATE_ENTRY_EXISTS")));
    assertThat(repository.count()).isEqualTo(16);

    mvc.perform(delete("/api/products")).andExpect(status().isNoContent());
    assertThat(repository.count()).isZero();
  }

  @Test
  void rejectsInvalidFileAtomicallyAndReportsEveryProblem() throws Exception {
    byte[] bytes =
        WorkbookFixtures.standard()
            .product("OK-1", LocalDate.of(2026, 1, 10))
            .product("OK-1", LocalDate.of(2026, 1, 10))
            .row("BAD-1", "Bad values", "Test", "31/31/2026", -4.5, 2.5)
            .row("BAD-2", null, null, "2026-02-01", "twelve", 3)
            .row("BAD-3", "Formulas", "Test", LocalDate.of(2026, 3, 1), new Formula("1/0"), new Formula("B6*2"))
            .row("BAD-4", "Future", "Test", LocalDate.of(2099, 1, 1), 1, 1)
            .bytes();

    mvc.perform(multipart("/api/products/import").file(file("errors.xlsx", bytes)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath(
                "$.error.details[*].code",
                containsInAnyOrder(
                    "DUPLICATE_ENTRY_IN_FILE",
                    "INVALID_DATE_FORMAT",
                    "NEGATIVE_VALUE",
                    "NOT_AN_INTEGER",
                    "REQUIRED_FIELD_MISSING",
                    "REQUIRED_FIELD_MISSING",
                    "INVALID_NUMBER",
                    "FORMULA_EVALUATION_ERROR",
                    "FORMULA_EVALUATION_ERROR",
                    "DATE_IN_FUTURE")))
        .andExpect(jsonPath("$.error.details[0].info.row").value(3));

    assertThat(repository.count()).as("nothing is written when any row fails").isZero();
  }

  @Test
  void rejectsWrongHeadersAsInvalidFile() throws Exception {
    byte[] bytes = WorkbookFixtures.withHeaders("SKU", "Name").row("A", "B").bytes();

    mvc.perform(multipart("/api/products/import").file(file("wrong.xlsx", bytes)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_FILE"))
        .andExpect(jsonPath("$.error.details[0].code").value("INVALID_HEADER"));
  }

  @Test
  void rejectsUnsupportedExtension() throws Exception {
    mvc.perform(multipart("/api/products/import").file(file("inventory.csv", "a,b".getBytes())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details[0].code").value("UNSUPPORTED_FILE_TYPE"));
  }

  @Test
  void rejectsEmptyUpload() throws Exception {
    mvc.perform(multipart("/api/products/import").file(file("empty.xlsx", new byte[0])))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details[0].code").value("FILE_EMPTY"));
  }
}
