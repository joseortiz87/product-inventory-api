package com.joseortiz.inventory.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joseortiz.inventory.domain.Product;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.repository.ProductKey;
import com.joseortiz.inventory.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuplicateRuleValidatorTest {

  private final ProductRepository repository = mock(ProductRepository.class);
  private final DuplicateRuleValidator validator = new DuplicateRuleValidator(repository);

  private static ImportedProduct item(int row, String sku, LocalDate date) {
    return new ImportedProduct(row, new Product(sku, "n", "c", date, BigDecimal.ONE, 1));
  }

  @Test
  void emptyBatchSkipsDatabase() {
    assertThat(validator.validate(List.of())).isEmpty();
    verify(repository, never()).findExistingKeys(anyCollection());
  }

  @Test
  void sameSkuOnDifferentDatesIsAllowed() {
    when(repository.findExistingKeys(anyCollection())).thenReturn(List.of());
    List<ErrorDetail> errors =
        validator.validate(List.of(item(2, "A", LocalDate.of(2026, 1, 1)), item(3, "A", LocalDate.of(2026, 1, 2))));
    assertThat(errors).isEmpty();
  }

  @Test
  void reportsInFileDuplicatesPointingAtFirstOccurrence() {
    when(repository.findExistingKeys(anyCollection())).thenReturn(List.of());
    LocalDate date = LocalDate.of(2026, 1, 1);
    List<ErrorDetail> errors =
        validator.validate(List.of(item(2, "A", date), item(3, "B", date), item(4, "A", date)));

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).code()).isEqualTo("DUPLICATE_ENTRY_IN_FILE");
    assertThat(errors.get(0).info())
        .containsEntry("row", 4)
        .containsEntry("firstRow", 2)
        .containsEntry("sku", "A")
        .containsEntry("purchaseDate", "2026-01-01");
    assertThat(errors.get(0).message())
        .isEqualTo("Row 4: SKU 'A' with purchase date 2026-01-01 also appears on row 2.");
  }

  @Test
  void reportsCollisionsWithExistingRowsUsingOneQuery() {
    LocalDate date = LocalDate.of(2026, 1, 1);
    when(repository.findExistingKeys(anyCollection())).thenReturn(List.of(new ProductKey("B", date)));

    List<ErrorDetail> errors = validator.validate(List.of(item(2, "A", date), item(3, "B", date)));

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).code()).isEqualTo("DUPLICATE_ENTRY_EXISTS");
    assertThat(errors.get(0).info()).containsEntry("row", 3).containsEntry("sku", "B");
    verify(repository).findExistingKeys(anyCollection());
  }
}
