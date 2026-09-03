package com.joseortiz.inventory.validation;

import com.joseortiz.inventory.error.ErrorCode;
import com.joseortiz.inventory.error.ErrorDetail;
import com.joseortiz.inventory.repository.ProductKey;
import com.joseortiz.inventory.repository.ProductRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Enforces the business rule "SKU + purchase date must be unique".
 *
 * <p>Two checks are performed on the whole batch:
 *
 * <ol>
 *   <li>within the file: a key appearing twice is reported on the later row, pointing at the first;
 *   <li>against the database: keys already stored are fetched with one query for all SKUs in the
 *       file.
 * </ol>
 *
 * The database unique constraint remains as a safety net for concurrent imports.
 */
@Component
public class DuplicateRuleValidator implements ValidationRule<List<ImportedProduct>> {

  private final ProductRepository repository;

  /**
   * Creates the validator.
   *
   * @param repository product persistence
   */
  public DuplicateRuleValidator(ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ErrorDetail> validate(List<ImportedProduct> products) {
    List<ErrorDetail> errors = new ArrayList<>();
    if (products.isEmpty()) {
      return errors;
    }

    Map<ProductKey, Integer> firstRowByKey = new HashMap<>();
    for (ImportedProduct item : products) {
      ProductKey key = keyOf(item);
      Integer firstRow = firstRowByKey.putIfAbsent(key, item.rowNumber());
      if (firstRow != null) {
        errors.add(
            base(item, key).put("firstRow", firstRow).build(ErrorCode.DUPLICATE_ENTRY_IN_FILE));
      }
    }

    Set<String> skus =
        products.stream().map(p -> p.product().getSku()).collect(Collectors.toSet());
    Set<ProductKey> existing = new HashSet<>(repository.findExistingKeys(skus));
    if (existing.isEmpty()) {
      return errors;
    }
    for (ImportedProduct item : products) {
      ProductKey key = keyOf(item);
      if (existing.contains(key)) {
        errors.add(base(item, key).build(ErrorCode.DUPLICATE_ENTRY_EXISTS));
      }
    }
    return errors;
  }

  private static ProductKey keyOf(ImportedProduct item) {
    return new ProductKey(item.product().getSku(), item.product().getPurchaseDate());
  }

  private static ErrorDetail.InfoBuilder base(ImportedProduct item, ProductKey key) {
    return ErrorDetail.withInfo()
        .put("row", item.rowNumber())
        .put("field", "sku")
        .put("sku", key.sku())
        .put("purchaseDate", key.purchaseDate().toString());
  }
}
