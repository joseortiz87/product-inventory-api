package com.joseortiz.inventory.repository;

import com.joseortiz.inventory.domain.Product;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence operations for {@link Product}.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

  /**
   * Finds the natural keys, among the given SKUs, that already exist in the database.
   *
   * <p>Used by the duplicate validator to detect collisions with previously imported data in a
   * single round trip instead of one query per row.
   *
   * @param skus SKUs present in the file being imported
   * @return existing (sku, purchaseDate) pairs for those SKUs
   */
  @Query(
      "select new com.joseortiz.inventory.repository.ProductKey(p.sku, p.purchaseDate) "
          + "from Product p where p.sku in :skus")
  List<ProductKey> findExistingKeys(@Param("skus") Collection<String> skus);

  /**
   * Aggregates the whole inventory in a single query.
   *
   * @return count, total value and average purchase date (as epoch day) of all products
   */
  @Query(
      "select new com.joseortiz.inventory.repository.InventoryAggregate("
          + "count(p), coalesce(sum(p.unitPrice * p.quantity), 0), min(p.purchaseDate), "
          + "max(p.purchaseDate)) from Product p")
  InventoryAggregate aggregate();

  /**
   * Streams purchase dates only, used to compute the average stock age.
   *
   * @return every purchase date in the table
   */
  @Query("select p.purchaseDate from Product p")
  List<LocalDate> findAllPurchaseDates();
}
