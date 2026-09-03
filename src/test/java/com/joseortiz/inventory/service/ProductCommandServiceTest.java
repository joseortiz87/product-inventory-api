package com.joseortiz.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joseortiz.inventory.error.NotFoundException;
import com.joseortiz.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;

class ProductCommandServiceTest {

  private final ProductRepository repository = mock(ProductRepository.class);
  private final ProductCommandService service = new ProductCommandService(repository);

  @Test
  void deletesExistingProduct() {
    when(repository.existsById(7L)).thenReturn(true);
    service.delete(7L);
    verify(repository).deleteById(7L);
  }

  @Test
  void missingProductIsA404WithItsId() {
    when(repository.existsById(7L)).thenReturn(false);
    assertThatThrownBy(() -> service.delete(7L))
        .isInstanceOfSatisfying(
            NotFoundException.class,
            ex -> {
              assertThat(ex.getDetails().get(0).code()).isEqualTo("PRODUCT_NOT_FOUND");
              assertThat(ex.getDetails().get(0).info()).containsEntry("id", 7L);
            });
    verify(repository, never()).deleteById(7L);
  }
}
