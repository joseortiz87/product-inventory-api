package com.joseortiz.inventory;

import com.joseortiz.inventory.config.InventoryProperties;
import java.util.List;
import org.springframework.util.unit.DataSize;

/** Ready-made {@link InventoryProperties} for unit tests, mirroring application.yml. */
public final class TestProperties {

  private TestProperties() {}

  /** @return default properties */
  public static InventoryProperties defaults() {
    return new InventoryProperties(
        new InventoryProperties.Cors(List.of("http://localhost:4200")),
        new InventoryProperties.Import(
            DataSize.ofMegabytes(5),
            10_000,
            List.of("xlsx"),
            List.of("yyyy-MM-dd", "MM/dd/yyyy", "M/d/yyyy", "dd-MMM-yyyy")),
        new InventoryProperties.Query(200));
  }

  /**
   * @param maxRows row limit
   * @return properties with a custom row limit
   */
  public static InventoryProperties withMaxRows(int maxRows) {
    InventoryProperties d = defaults();
    return new InventoryProperties(
        d.cors(),
        new InventoryProperties.Import(
            d.importSettings().maxFileSize(),
            maxRows,
            d.importSettings().acceptedExtensions(),
            d.importSettings().datePatterns()),
        d.query());
  }
}
