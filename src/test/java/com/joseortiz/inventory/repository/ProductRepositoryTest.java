package com.joseortiz.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joseortiz.inventory.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ProductRepositoryTest {

  @Autowired private ProductRepository repository;

  private static Product product(String sku, LocalDate date, String price, int qty) {
    return new Product(sku, "Name", "Category", date, new BigDecimal(price), qty);
  }

  @Test
  void uniqueConstraintIsTheLastLineOfDefence() {
    repository.saveAndFlush(product("A", LocalDate.of(2026, 1, 1), "1.00", 1));
    assertThatThrownBy(() -> repository.saveAndFlush(product("A", LocalDate.of(2026, 1, 1), "2.00", 1)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void sameSkuOnAnotherDateIsAllowed() {
    repository.saveAndFlush(product("A", LocalDate.of(2026, 1, 1), "1.00", 1));
    repository.saveAndFlush(product("A", LocalDate.of(2026, 1, 2), "1.00", 1));
    assertThat(repository.count()).isEqualTo(2);
  }

  @Test
  void findsExistingKeysForGivenSkus() {
    repository.saveAndFlush(product("A", LocalDate.of(2026, 1, 1), "1.00", 1));
    repository.saveAndFlush(product("B", LocalDate.of(2026, 1, 2), "1.00", 1));

    assertThat(repository.findExistingKeys(List.of("A", "Z")))
        .containsExactly(new ProductKey("A", LocalDate.of(2026, 1, 1)));
  }

  @Test
  void aggregatesTotals() {
    repository.saveAndFlush(product("A", LocalDate.of(2026, 1, 1), "10.50", 2));
    repository.saveAndFlush(product("B", LocalDate.of(2026, 3, 1), "1.25", 4));

    InventoryAggregate aggregate = repository.aggregate();

    assertThat(aggregate.productCount()).isEqualTo(2);
    assertThat(aggregate.totalValue()).isEqualByComparingTo("26.00");
    assertThat(aggregate.oldestPurchaseDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(aggregate.newestPurchaseDate()).isEqualTo(LocalDate.of(2026, 3, 1));
  }

  @Test
  void aggregateOfEmptyTableIsZero() {
    InventoryAggregate aggregate = repository.aggregate();
    assertThat(aggregate.productCount()).isZero();
    assertThat(aggregate.totalValue()).isEqualByComparingTo("0");
    assertThat(aggregate.oldestPurchaseDate()).isNull();
  }
}
