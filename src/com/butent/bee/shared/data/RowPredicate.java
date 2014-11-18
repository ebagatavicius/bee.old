package com.butent.bee.shared.data;

import com.google.common.base.Predicate;

import com.butent.bee.shared.utils.BeeUtils;

public abstract class RowPredicate implements Predicate<IsRow> {

  public static RowPredicate isNull(final int index) {
    return new RowPredicate() {
      @Override
      public boolean apply(IsRow input) {
        return input == null || input.isNull(index);
      }
    };
  }

  public static RowPredicate isTrue(final int index) {
    return new RowPredicate() {
      @Override
      public boolean apply(IsRow input) {
        return input != null && BeeUtils.isTrue(input.getBoolean(index));
      }
    };
  }

  public static RowPredicate notNull(final int index) {
    return new RowPredicate() {
      @Override
      public boolean apply(IsRow input) {
        return input != null && !input.isNull(index);
      }
    };
  }
}
