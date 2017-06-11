package com.butent.bee.shared.data;

import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface RowPredicate extends Predicate<IsRow> {

  static RowPredicate and(RowPredicate p1, RowPredicate p2) {
    if (p1 == null) {
      return p2;
    } else if (p2 == null) {
      return p1;
    } else {
      return row -> p1.test(row) && p2.test(row);
    }
  }

  static RowPredicate equals(int index, Long value) {
    return row -> row != null && Objects.equals(row.getLong(index), value);
  }

  static RowPredicate isNull(int index) {
    return row -> row == null || row.isNull(index);
  }

  static RowPredicate isTrue(int index) {
    return row -> row != null && row.isTrue(index);
  }

  static RowPredicate notNull(int index) {
    return row -> row != null && !row.isNull(index);
  }
}
