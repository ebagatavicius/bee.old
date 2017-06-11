package com.butent.bee.shared.data;

@FunctionalInterface
public interface RowToLong extends RowFunction<Long> {

  static RowToLong at(final int index) {
    return input -> (input == null) ? null : input.getLong(index);
  }
}
