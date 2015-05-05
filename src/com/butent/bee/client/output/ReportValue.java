package com.butent.bee.client.output;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

public final class ReportValue implements Comparable<ReportValue> {

  private String value;
  private String display;

  private ReportValue(String value, String display) {
    this.value = value;
    this.display = display;
  }

  @Override
  public int compareTo(ReportValue o) {
    if (o == null) {
      return BeeConst.COMPARE_MORE;
    }
    return getValue().compareTo(o.getValue());
  }

  public static ReportValue empty() {
    return new ReportValue(null, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof ReportValue)) {
      return false;
    }
    return getValue().equals(((ReportValue) obj).getValue());
  }

  public String getValue() {
    return BeeUtils.nvl(value, BeeConst.STRING_EMPTY);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getValue());
  }

  public static ReportValue of(String value) {
    return new ReportValue(value, null);
  }

  public static ReportValue of(String value, String display) {
    return new ReportValue(value, display);
  }

  @Override
  public String toString() {
    return BeeUtils.nvl(display, getValue());
  }
}
