package com.rex.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envelope for paginated collections, so clients never depend on Spring's Page shape.
 *
 * <p>The item list is copied on construction and on read. A record field holding a caller supplied
 * collection is otherwise shared mutable state, and a response object that can change after it is
 * built is a source of surprising bugs.
 */
public record PageResponse<T>(
    List<T> items, int page, int size, long totalElements, int totalPages) {

  public PageResponse {
    items = List.copyOf(items);
  }

  @Override
  public List<T> items() {
    return List.copyOf(items);
  }

  public static <S, T> PageResponse<T> from(Page<S> page, List<T> items) {
    return new PageResponse<>(
        items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }
}
