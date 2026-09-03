package com.joseortiz.inventory.service;

import com.joseortiz.inventory.config.InventoryProperties;
import com.joseortiz.inventory.dto.InventorySummaryResponse;
import com.joseortiz.inventory.dto.PageResponse;
import com.joseortiz.inventory.dto.ProductResponse;
import com.joseortiz.inventory.error.BadRequestException;
import com.joseortiz.inventory.error.ErrorCode;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.repository.InventoryAggregate;
import com.joseortiz.inventory.repository.ProductRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the inventory: paginated listing and summary figures.
 *
 * <p>Sorting is done in the database. Clients sort by API field names; the service maps them to
 * entity properties through an allow-list, which both prevents sorting by arbitrary columns and
 * lets derived fields be sortable: {@code stockAge} is the inverse of {@code purchaseDate}.
 */
@Service
@Transactional(readOnly = true)
public class ProductQueryService {

  /** Default sort applied when the client does not specify one. */
  public static final String DEFAULT_SORT = "purchaseDate,desc";

  /** API sort field to entity property. */
  private static final Map<String, String> SORTABLE =
      Map.of(
          "sku", "sku",
          "name", "name",
          "category", "category",
          "purchasedate", "purchaseDate",
          "unitprice", "unitPrice",
          "quantity", "quantity",
          "stockage", "purchaseDate");

  /** Sort fields whose direction must be flipped because they are inversely derived. */
  private static final List<String> INVERTED = List.of("stockage");

  private final ProductRepository repository;
  private final InventoryProperties properties;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param repository product persistence
   * @param properties query limits
   * @param clock source of "today" for stock age
   */
  public ProductQueryService(ProductRepository repository, InventoryProperties properties, Clock clock) {
    this.repository = repository;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Lists products.
   *
   * @param page zero-based page index
   * @param size page size, capped by {@code inventory.query.max-page-size}
   * @param sort {@code field} or {@code field,asc|desc}; see {@link #SORTABLE}
   * @return the page
   * @throws BadRequestException on unknown sort field or oversized page
   */
  public PageResponse<ProductResponse> findAll(int page, int size, String sort) {
    int maxPageSize = properties.query().maxPageSize();
    if (size > maxPageSize) {
      throw new BadRequestException(
          ErrorDetail.withInfo()
              .put("field", "size")
              .put("value", size)
              .put("max", maxPageSize)
              .build(ErrorCode.PAGE_SIZE_TOO_LARGE));
    }
    Sort resolvedSort = resolveSort(sort == null || sort.isBlank() ? DEFAULT_SORT : sort);
    Page<com.joseortiz.inventory.domain.Product> result =
        repository.findAll(PageRequest.of(Math.max(page, 0), Math.max(size, 1), resolvedSort));
    LocalDate today = LocalDate.now(clock);
    return PageResponse.from(result, p -> ProductResponse.from(p, today), sort == null ? DEFAULT_SORT : sort);
  }

  /**
   * Computes the dashboard summary.
   *
   * @return totals and average stock age
   */
  public InventorySummaryResponse summary() {
    InventoryAggregate aggregate = repository.aggregate();
    Double averageAge = null;
    if (aggregate.productCount() > 0) {
      LocalDate today = LocalDate.now(clock);
      averageAge =
          repository.findAllPurchaseDates().stream()
              .mapToLong(date -> ChronoUnit.DAYS.between(date, today))
              .average()
              .orElse(0);
    }
    return new InventorySummaryResponse(
        aggregate.productCount(),
        aggregate.totalValue(),
        averageAge,
        aggregate.oldestPurchaseDate(),
        aggregate.newestPurchaseDate());
  }

  /**
   * Removes every product. Exposed for demos and tests.
   */
  @Transactional
  public void deleteAll() {
    repository.deleteAllInBatch();
  }

  /**
   * Translates the client sort expression into a Spring Data sort.
   *
   * @param sort {@code field} or {@code field,direction}
   * @return the sort
   */
  Sort resolveSort(String sort) {
    String[] parts = sort.split(",", 2);
    String field = parts[0].trim().toLowerCase(Locale.ROOT);
    String property = SORTABLE.get(field);
    if (property == null) {
      throw new BadRequestException(
          ErrorDetail.withInfo()
              .put("field", "sort")
              .put("value", parts[0].trim())
              .put("accepted", "sku, name, category, purchaseDate, unitPrice, quantity, stockAge")
              .build(ErrorCode.INVALID_SORT_FIELD));
    }
    Sort.Direction direction =
        parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
    if (INVERTED.contains(field)) {
      direction = direction.isAscending() ? Sort.Direction.DESC : Sort.Direction.ASC;
    }
    // Secondary sort keeps pagination stable when the primary key has ties.
    return Sort.by(new Sort.Order(direction, property)).and(Sort.by(Sort.Direction.ASC, "id"));
  }
}
