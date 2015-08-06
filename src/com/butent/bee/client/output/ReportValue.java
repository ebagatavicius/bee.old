package com.butent.bee.client.output;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

public final class ReportValue implements Comparable<ReportValue> {

  private ReportValue[] values;
  private String value;

  private String display;

  private ReportValue(String value) {
    this.value = value;
  }

  private ReportValue(ReportValue... values) {
    if (values == null) {
      this.values = new ReportValue[0];
    } else {
      this.values = values;
    }
  }

  @Override
  public int compareTo(ReportValue o) {
    if (o == null) {
      return BeeConst.COMPARE_MORE;
    }
    if (!BeeUtils.anyNotNull(values, o.values)) {
      return getValue().compareTo(o.getValue());
    }
    ReportValue[] own = values != null ? values : new ReportValue[] {this};
    ReportValue[] other = o.values != null ? o.values : new ReportValue[] {o};

    for (int i = 0; i < Math.min(own.length, other.length); i++) {
      int res = own[i].compareTo(other[i]);

      if (res != BeeConst.COMPARE_EQUAL) {
        return res;
      }
    }
    return BeeUtils.compareNullsFirst(own.length, other.length);
  }

  public static ReportValue empty() {
    return new ReportValue((String) null);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof ReportValue)) {
      return false;
    }
    return compareTo((ReportValue) obj) == BeeConst.COMPARE_EQUAL;
  }

  public String getValue() {
    return BeeUtils.nvl(value, BeeConst.STRING_EMPTY);
  }

  public ReportValue[] getValues() {
    return values;
  }

  @Override
  public int hashCode() {
    return values != null
        ? (values.length == 1 ? Objects.hashCode(values[0]) : Objects.hash((Object[]) values))
        : Objects.hashCode(value);
  }

  public static ReportValue of(String value) {
    ReportValue reportValue = new ReportValue(value);
    return reportValue;
  }

  public static ReportValue of(ReportValue... values) {
    Assert.noNulls((Object[]) values);
    ReportValue reportValue = new ReportValue(values);
    return reportValue;
  }

  public ReportValue setDisplay(String displ) {
    this.display = displ;
    return this;
  }

  @Override
  public String toString() {
    return BeeUtils.nvl(display, getValue());
  }
}
