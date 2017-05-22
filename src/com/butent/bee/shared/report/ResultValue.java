package com.butent.bee.shared.report;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.Objects;

public final class ResultValue implements BeeSerializable, Comparable<ResultValue> {

  private enum Serial {
    VALUES, VALUE, DISPLAY
  }

  private ResultValue[] values;
  private String value;
  private String display;

  private ResultValue(String value) {
    this.value = value;
  }

  private ResultValue(ResultValue... values) {
    if (values == null) {
      this.values = new ResultValue[0];
    } else {
      this.values = values;
    }
  }

  @Override
  public int compareTo(ResultValue o) {
    if (o == null) {
      return BeeConst.COMPARE_MORE;
    }
    if (!BeeUtils.anyNotNull(values, o.values)) {
      return getValue().compareTo(o.getValue());
    }
    ResultValue[] own = values != null ? values : new ResultValue[] {this};
    ResultValue[] other = o.values != null ? o.values : new ResultValue[] {o};

    for (int i = 0; i < Math.min(own.length, other.length); i++) {
      int res = own[i].compareTo(other[i]);

      if (res != BeeConst.COMPARE_EQUAL) {
        return res;
      }
    }
    return BeeUtils.compareNullsFirst(own.length, other.length);
  }

  @Override
  public void deserialize(String data) {
    processMembers(Serial.class, data, (serial, val) -> {
      switch (serial) {
        case VALUES:
          String[] arr = Codec.beeDeserializeCollection(val);

          if (Objects.nonNull(arr)) {
            values = Arrays.stream(arr).map(ResultValue::restore).toArray(ResultValue[]::new);
          }
          break;

        case VALUE:
          value = val;
          break;

        case DISPLAY:
          display = val;
          break;
      }
    });
  }

  public static ResultValue empty() {
    return new ResultValue((String) null);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof ResultValue)) {
      return false;
    }
    return compareTo((ResultValue) obj) == BeeConst.COMPARE_EQUAL;
  }

  public String getValue() {
    return BeeUtils.nvl(value, BeeConst.STRING_EMPTY);
  }

  public ResultValue[] getValues() {
    return values;
  }

  @Override
  public int hashCode() {
    return values != null
        ? (values.length == 1 ? Objects.hashCode(values[0]) : Objects.hash((Object[]) values))
        : Objects.hashCode(value);
  }

  public static ResultValue of(String value) {
    return new ResultValue(value);
  }

  public static ResultValue of(ResultValue... values) {
    Assert.noNulls((Object[]) values);
    return new ResultValue(values);
  }

  public static ResultValue restore(String s) {
    return BeeSerializable.restore(s, () -> new ResultValue((String) null));
  }

  public ResultValue setDisplay(String displ) {
    this.display = displ;
    return this;
  }

  @Override
  public String serialize() {
    return serializeMembers(Serial.class, serial -> {
      Object val = null;

      switch (serial) {
        case VALUES:
          val = values;
          break;

        case VALUE:
          val = value;
          break;

        case DISPLAY:
          val = display;
          break;
      }
      return val;
    });
  }

  @Override
  public String toString() {
    return BeeUtils.nvl(display, getValue());
  }
}
