package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;

public abstract class RowToDouble implements RowFunction<Double> {

  public static RowToDouble at(final int index) {
    return new RowToDouble() {
      @Override
      public Double apply(IsRow input) {
        return (input == null) ? null : input.getDouble(index);
      }
    };
  }

  public RowToDouble minus(final RowToDouble other) {
    Assert.notNull(other);

    return new RowToDouble() {
      @Override
      public Double apply(IsRow input) {
        Double v1 = RowToDouble.this.apply(input);
        Double v2 = other.apply(input);

        if (v1 == null) {
          return v2;
        } else if (v2 == null) {
          return v1;
        } else {
          return v1 - v2;
        }
      }
    };
  }

  public RowToDouble plus(final RowToDouble other) {
    Assert.notNull(other);

    return new RowToDouble() {
      @Override
      public Double apply(IsRow input) {
        Double v1 = RowToDouble.this.apply(input);
        Double v2 = other.apply(input);

        if (v1 == null) {
          return v2;
        } else if (v2 == null) {
          return v1;
        } else {
          return v1 + v2;
        }
      }
    };
  }

  public RowToDouble times(final RowToDouble other) {
    Assert.notNull(other);

    return new RowToDouble() {
      @Override
      public Double apply(IsRow input) {
        Double v1 = RowToDouble.this.apply(input);
        Double v2 = other.apply(input);

        return (v1 == null || v2 == null) ? null : (v1 * v2);
      }
    };
  }
}
