package com.rex.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageResponseTest {

  @Test
  @DisplayName("mutating the source list after construction does not change the response")
  void copiesOnConstruction() {
    List<String> source = new ArrayList<>(List.of("a", "b"));
    PageResponse<String> response = new PageResponse<>(source, 0, 2, 2, 1);

    source.add("c");

    assertThat(response.items()).containsExactly("a", "b");
  }

  @Test
  @DisplayName("the returned list cannot be modified by a caller")
  void returnsAnImmutableView() {
    PageResponse<String> response = new PageResponse<>(List.of("a"), 0, 1, 1, 1);

    assertThatThrownBy(() -> response.items().add("b"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("an empty page is valid")
  void emptyPage() {
    PageResponse<String> response = new PageResponse<>(List.of(), 0, 20, 0, 0);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalElements()).isZero();
  }
}
