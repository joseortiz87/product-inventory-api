package com.joseortiz.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Product Inventory Management API.
 *
 * <p>The application imports product inventory spreadsheets, validates them and exposes the
 * resulting catalogue through a small REST API consumed by the Angular dashboard.
 */
@SpringBootApplication
public class InventoryApplication {

  /**
   * Boots the Spring application.
   *
   * @param args command line arguments forwarded to Spring Boot
   */
  public static void main(String[] args) {
    SpringApplication.run(InventoryApplication.class, args);
  }
}
