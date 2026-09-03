package com.joseortiz.inventory.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Stable pagination envelope, independent of Spring Data's {@code Page} serialisation.
 *
 * @param content items of the current page
 * @param page zero-based page index
 * @param size requested page size
 * @param totalElements total number of items across all pages
 * @param totalPages total number of pages
 * @param sort applied sort, as {@code field,direction}
 * @param <T> item type
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages, String sort) {

  /**
   * Converts a Spring Data page.
   *
   * @param page source page
   * @param mapper entity to DTO mapping
   * @param sort sort description echoed to the client
   * @param <E> entity type
   * @param <T> DTO type
   * @return the response
   */
  public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper, String sort) {
    return new PageResponse<>(
        page.getContent().stream().map(mapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        sort);
  }
}
