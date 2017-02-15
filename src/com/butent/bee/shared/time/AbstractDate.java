package com.butent.bee.shared.time;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Date;

/**
 * Implements {@code HasDateValue} interface and enables assertions and transformations for data
 * types.
 */

public abstract class AbstractDate implements HasDateValue {

  public static AbstractDate fromJava(Date date, ValueType type) {
    if (date == null) {
      return null;
    }
    assertType(type);

    switch (type) {
      case DATE:
        return new JustDate(date);
      case DATE_TIME:
        return new DateTime(date);
      default:
        Assert.untouchable();
        return null;
    }
  }

  public static AbstractDate restore(String s, ValueType type) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    assertType(type);

    switch (type) {
      case DATE:
        return TimeUtils.toDateOrNull(s);
      case DATE_TIME:
        return TimeUtils.toDateTimeOrNull(s);
      default:
        Assert.untouchable();
        return null;
    }
  }

  private static void assertType(ValueType type) {
    Assert.notNull(type);
    Assert.isTrue(type.equals(ValueType.DATE) || type.equals(ValueType.DATE_TIME));
  }

  @Override
  public HasDateValue fromDate(JustDate justDate) {
    if (justDate == null) {
      return null;
    }
    ValueType type = getType();
    assertType(type);

    switch (type) {
      case DATE:
        return new JustDate(justDate.getDays());
      case DATE_TIME:
        return new DateTime(justDate);
      default:
        Assert.untouchable();
        return null;
    }
  }

  @Override
  public HasDateValue fromDateTime(DateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    ValueType type = getType();
    assertType(type);

    switch (type) {
      case DATE:
        return new JustDate(dateTime);
      case DATE_TIME:
        return new DateTime(dateTime.getTime());
      default:
        Assert.untouchable();
        return null;
    }
  }

  @Override
  public HasDateValue fromJava(Date date) {
    return fromJava(date, getType());
  }

  @Override
  public int getHour() {
    return 0;
  }

  @Override
  public int getMillis() {
    return 0;
  }

  @Override
  public int getMinute() {
    return 0;
  }

  @Override
  public int getSecond() {
    return 0;
  }

  @Override
  public int getTimezoneOffset() {
    return 0;
  }

  public abstract ValueType getType();
}
