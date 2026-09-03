package com.joseortiz.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joseortiz.inventory.TestProperties;
import com.joseortiz.inventory.dto.InventorySummaryResponse;
import com.joseortiz.inventory.error.BadRequestException;
import com.joseortiz.inventory.repository.InventoryAggregate;
import com.joseortiz.inventory.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class ProductQueryServiceTest {

  private static final Clock FIXED = Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC);

  private final ProductRepository repository = mock(ProductRepository.class);
  private final ProductQueryService service =
      new ProductQueryService(repository, TestProperties.defaults(), FIXED);

  @Test
  void mapsApiFieldsToEntityPropertiesWithStableSecondaryOrder() {
    Sort sort = service.resolveSort("unitPrice,desc");
    assertThat(sort.getOrderFor("unitPrice").getDirection()).isEqualTo(Sort.Direction.DESC);
    assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  void defaultsToAscendingAndIgnoresCase() {
    assertThat(service.resolveSort("SKU").getOrderFor("sku").getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  void stockAgeSortsPurchaseDateInverted() {
    assertThat(service.resolveSort("stockAge,desc").getOrderFor("purchaseDate").getDirection())
        .isEqualTo(Sort.Direction.ASC);
    assertThat(service.resolveSort("stockAge,asc").getOrderFor("purchaseDate").getDirection())
        .isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void rejectsUnknownSortField() {
    assertThatThrownBy(() -> service.resolveSort("password,asc"))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            ex -> assertThat(ex.getDetails().get(0).code()).isEqualTo("INVALID_SORT_FIELD"));
  }

  @Test
  void rejectsOversizedPage() {
    assertThatThrownBy(() -> service.findAll(0, 201, "sku"))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            ex -> assertThat(ex.getDetails().get(0).info()).containsEntry("max", 200));
  }

  @Test
  void summaryOfEmptyInventoryHasNoAverage() {
    when(repository.aggregate()).thenReturn(new InventoryAggregate(0, BigDecimal.ZERO, null, null));
    InventorySummaryResponse summary = service.summary();
    assertThat(summary.totalProducts()).isZero();
    assertThat(summary.averageStockAgeDays()).isNull();
  }

  @Test
  void summaryAveragesStockAgeAgainstTheClock() {
    when(repository.aggregate())
        .thenReturn(new InventoryAggregate(2, new BigDecimal("150.00"), LocalDate.of(2026, 8, 24), LocalDate.of(2026, 9, 3)));
    when(repository.findAllPurchaseDates())
        .thenReturn(List.of(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 9, 3)));

    InventorySummaryResponse summary = service.summary();

    assertThat(summary.totalInventoryValue()).isEqualByComparingTo("150.00");
    assertThat(summary.averageStockAgeDays()).isEqualTo(5.0);
  }
}
