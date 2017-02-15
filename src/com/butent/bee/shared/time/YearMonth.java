package com.butent.bee.shared.time;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class YearMonth implements Comparable<YearMonth>, BeeSerializable, HasYearMonth {

  public static final char SEPARATOR = '-';

  public static YearMonth copyOf(YearMonth original) {
    if (original == null) {
      return null;
    } else {
      return new YearMonth(original.getYear(), original.getMonth());
    }
  }

  public static YearMonth of(HasYearMonth ref) {
    if (ref == null) {
      return null;
    } else {
      return new YearMonth(ref);
    }
  }

  public static YearMonth of(Integer year, Integer month) {
    if (TimeUtils.isYear(year) && TimeUtils.isMonth(month)) {
      return new YearMonth(year, month);
    } else {
      return null;
    }
  }

  public static YearMonth parse(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    List<Integer> fields = TimeUtils.splitFields(s);
    if (!BeeUtils.isPositive(BeeUtils.max(fields))) {
      return null;
    } else {
      return new YearMonth(TimeUtils.normalizeYear(TimeUtils.getField(fields, 0)),
          TimeUtils.getField(fields, 1));
    }
  }

  public static YearMonth parse(String ys, String ms) {
    Integer y = TimeUtils.parseYear(ys);
    Integer m = TimeUtils.parseMonth(ms);

    if (TimeUtils.isYear(y) && TimeUtils.isMonth(m)) {
      return new YearMonth(y, m);
    } else {
      return null;
    }
  }

  private int year;
  private int month;

  public YearMonth(HasYearMonth ref) {
    this(ref.getYear(), ref.getMonth());
  }

  public YearMonth(int year, int month) {
    super();
    checkMonth(month);
    this.year = year;
    this.month = month;
  }

  @Override
  public int compareTo(YearMonth other) {
    int result = BeeUtils.compareNullsFirst(getYear(), other.getYear());
    if (result == BeeConst.COMPARE_EQUAL) {
      result = BeeUtils.compareNullsFirst(getMonth(), other.getMonth());
    }
    return result;
  }

  @Override
  public void deserialize(String s) {
    YearMonth ym = parse(s);
    Assert.notNull(ym);
    setYearMonth(ym);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj == null) {
      return false;
    } else if (getClass() != obj.getClass()) {
      return false;
    } else {
      return year == ((YearMonth) obj).year && month == ((YearMonth) obj).month;
    }
  }

  @Override
  public JustDate getDate() {
    return new JustDate(getYear(), getMonth(), 1);
  }

  public JustDate getLast() {
    return new JustDate(getYear(), getMonth(), getLength());
  }

  public int getLength() {
    return Grego.monthLength(getYear(), getMonth());
  }

  @Override
  public int getMonth() {
    return month;
  }

  public DateRange getRange() {
    return DateRange.closed(getDate(), getLast());
  }

  @Override
  public int getYear() {
    return year;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + month;
    result = prime * result + year;
    return result;
  }

  public YearMonth nextMonth() {
    return nextMonth(1);
  }

  public YearMonth nextMonth(int increment) {
    return copyOf(this).shiftMonth(increment);
  }

  public YearMonth nextYear() {
    return nextYear(1);
  }

  public YearMonth nextYear(int increment) {
    return copyOf(this).shiftYear(increment);
  }

  public YearMonth previousMonth() {
    return nextMonth(-1);
  }

  public YearMonth previousMonth(int increment) {
    return nextMonth(-increment);
  }

  public YearMonth previousYear() {
    return nextYear(-1);
  }

  public YearMonth previousYear(int increment) {
    return nextYear(-increment);
  }

  @Override
  public String serialize() {
    return toString();
  }

  @Override
  public void setMonth(int month) {
    checkMonth(month);
    this.month = month;
  }

  @Override
  public void setYear(int year) {
    this.year = year;
  }

  public void setYearMonth(int y, int m) {
    setYear(y);
    setMonth(m);
  }

  public void setYearMonth(YearMonth source) {
    Assert.notNull(source);
    setYearMonth(source.getYear(), source.getMonth());
  }

  public YearMonth shiftMonth(int deltaMonths) {
    increment(deltaMonths);
    return this;
  }

  public YearMonth shiftYear(int deltaYears) {
    setYear(getYear() + deltaYears);
    return this;
  }

  public YearMonth shiftYearMonth(int deltaYears, int deltaMonths) {
    return shiftYear(deltaYears).shiftMonth(deltaMonths);
  }

  @Override
  public String toString() {
    return TimeUtils.yearToString(getYear()) + SEPARATOR + TimeUtils.monthToString(getMonth());
  }

  private static void checkMonth(int value) {
    Assert.betweenInclusive(value, 1, 12, "invalid month");
  }

  private void increment(int value) {
    if (value == 0) {
      return;
    }
    int z = getMonth() + value;

    if (z < 1) {
      setYear(getYear() + z / 12 - 1);
      setMonth(z % 12 + 12);
    } else if (z > 12) {
      setYear(getYear() + (z - 1) / 12);
      setMonth((z - 1) % 12 + 1);
    } else {
      setMonth(z);
    }
  }
}
