package com.joseortiz.inventory.service;

import com.joseortiz.inventory.error.NotFoundException;
import com.joseortiz.inventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write operations on existing products. Imports have their own service because they carry the
 * whole validation pipeline; this one is for the small, direct changes.
 */
@Service
@Transactional
public class ProductCommandService {

  private static final Logger log = LoggerFactory.getLogger(ProductCommandService.class);

  private final ProductRepository repository;

  /**
   * Creates the service.
   *
   * @param repository product persistence
   */
  public ProductCommandService(ProductRepository repository) {
    this.repository = repository;
  }

  /**
   * Deletes one product.
   *
   * @param id product identifier
   * @throws NotFoundException when no product has that id
   */
  public void delete(long id) {
    if (!repository.existsById(id)) {
      throw NotFoundException.product(id);
    }
    repository.deleteById(id);
    log.info("Deleted product {}", id);
  }

  /**
   * Removes every product.
   */
  public void deleteAll() {
    repository.deleteAllInBatch();
    log.info("Cleared the inventory");
  }
}
