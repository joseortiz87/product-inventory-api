package com.joseortiz.inventory.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single problem inside an {@link ApiErrorResponse}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * {
 *   "code": "DUPLICATE_ENTRY_IN_FILE",
 *   "message": "Row 7: SKU 'ABC-1' with purchase date 2024-01-05 also appears on row 3.",
 *   "info": { "row": 7, "sku": "ABC-1", "purchaseDate": "2024-01-05", "firstRow": 3 }
 * }
 * }</pre>
 *
 * @param code machine readable identifier, see {@link ErrorCode}
 * @param message human readable explanation
 * @param info structured context (field, value, row, limits...) a client can use for highlighting
 */
public record ErrorDetail(String code, String message, Map<String, Object> info) {

  /**
   * Builds a detail from a code and its context; the message is rendered from the code's template.
   *
   * @param code error code
   * @param info context values, also used to fill the message placeholders
   * @return the detail
   */
  public static ErrorDetail of(ErrorCode code, Map<String, ?> info) {
    Map<String, Object> copy = new LinkedHashMap<>(info);
    return new ErrorDetail(code.code(), code.message(copy), Collections.unmodifiableMap(copy));
  }

  /**
   * Builds a detail without context.
   *
   * @param code error code
   * @return the detail
   */
  public static ErrorDetail of(ErrorCode code) {
    return new ErrorDetail(code.code(), code.message(), Map.of());
  }

  /**
   * Fluent helper to build the {@code info} map in a fixed key order.
   *
   * @return a new builder
   */
  public static InfoBuilder withInfo() {
    return new InfoBuilder();
  }

  /** Ordered map builder for the {@code info} payload. */
  public static final class InfoBuilder {
    private final Map<String, Object> values = new LinkedHashMap<>();

    private InfoBuilder() {}

    /**
     * Adds an entry; {@code null} values are stored so the client sees the field was inspected.
     *
     * @param key info key
     * @param value info value
     * @return this builder
     */
    public InfoBuilder put(String key, Object value) {
      values.put(key, value);
      return this;
    }

    /**
     * Creates the detail.
     *
     * @param code error code
     * @return the rendered detail
     */
    public ErrorDetail build(ErrorCode code) {
      return ErrorDetail.of(code, values);
    }
  }
}
