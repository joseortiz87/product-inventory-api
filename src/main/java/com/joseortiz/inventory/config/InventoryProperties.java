package com.joseortiz.inventory.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly typed view of the {@code inventory.*} section of {@code application.yml}.
 *
 * <p>Grouping tunables here keeps magic numbers out of the services and allows every value to be
 * overridden by environment variables when the application runs in a container.
 *
 * @param cors browser origins allowed to call the API
 * @param importSettings limits and formats applied when importing spreadsheets
 * @param query limits applied to read endpoints
 */
@Validated
@ConfigurationProperties(prefix = "inventory")
public record InventoryProperties(
    Cors cors, @Name("import") Import importSettings, Query query) {

  /**
   * Cross-origin settings.
   *
   * @param allowedOrigins origins (scheme + host + port) allowed to call the API from a browser
   */
  public record Cors(@NotEmpty List<String> allowedOrigins) {}

  /**
   * Import limits and accepted formats.
   *
   * @param maxFileSize maximum accepted upload size
   * @param maxRows maximum number of data rows accepted in a single file
   * @param acceptedExtensions lower-case file extensions the parser layer can handle
   * @param datePatterns {@link java.time.format.DateTimeFormatter} patterns tried, in order, when a
   *     purchase date is provided as text
   */
  public record Import(
      DataSize maxFileSize,
      @Min(1) int maxRows,
      @NotEmpty List<String> acceptedExtensions,
      @NotEmpty List<String> datePatterns) {}

  /**
   * Read endpoint limits.
   *
   * @param maxPageSize largest page size a client may request
   */
  public record Query(@Min(1) int maxPageSize) {}
}
