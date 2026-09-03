package com.joseortiz.inventory.validation;

import com.joseortiz.inventory.error.ErrorDetail;
import java.util.List;

/**
 * A validation rule producing zero or more {@link ErrorDetail}s.
 *
 * <p>Rules never throw: the import service aggregates the details of every rule and raises a single
 * {@link com.joseortiz.inventory.error.ValidationException} so the user sees all problems at once.
 *
 * @param <T> type being validated (a row, a list of products...)
 */
@FunctionalInterface
public interface ValidationRule<T> {

  /**
   * Validates the target.
   *
   * @param target value to check
   * @return problems found, empty when valid
   */
  List<ErrorDetail> validate(T target);
}
