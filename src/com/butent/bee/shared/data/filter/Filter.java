package com.butent.bee.shared.data.filter;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowFilter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Is an abstract class for all specific filter applying classes, determines which type of filter to
 * apply.
 */

public abstract class Filter implements BeeSerializable, RowFilter {

  public static CompoundFilter and() {
    return new CompoundFilter(CompoundType.AND);
  }

  public static Filter and(Collection<Filter> filters) {
    if (filters == null || filters.isEmpty()) {
      return null;
    } else {
      Filter[] arr = filters.toArray(new Filter[0]);
      int size = arr.length;

      switch (size) {
        case 1:
          return arr[0];
        case 2:
          return and(arr[0], arr[1]);
        default:
          return new CompoundFilter(CompoundType.AND, arr);
      }
    }
  }

  public static Filter and(Filter f1, Filter f2) {
    if (f1 == null) {
      return f2;
    } else if (f2 == null) {
      return f1;
    } else {
      return new CompoundFilter(CompoundType.AND, f1, f2);
    }
  }

  public static Filter and(Filter f1, Filter f2, Filter f3) {
    return and(and(f1, f2), f3);
  }

  public static Filter any(String column, Collection<Long> values) {
    Assert.notEmpty(column);
    Assert.notNull(values);

    if (values.isEmpty()) {
      return null;
    }
    List<Value> vals = new ArrayList<>();

    for (Long value : values) {
      vals.add(new LongValue(value));
    }
    return new ColumnValueFilter(column, vals);
  }

  public static Filter anyContains(Collection<String> columns, String value) {
    Assert.notEmpty(columns);
    Assert.notEmpty(value);

    CompoundFilter filter = or();
    for (String column : columns) {
      filter.add(contains(column, value));
    }
    return filter;
  }

  public static Filter anyItemContains(String column, Class<? extends Enum<?>> clazz,
      String value) {
    Assert.notEmpty(column);
    Assert.notNull(clazz);
    Assert.notEmpty(value);

    List<Filter> filters = Lists.newArrayList();

    String item;
    for (Enum<?> constant : clazz.getEnumConstants()) {
      if (constant instanceof HasCaption) {
        item = ((HasCaption) constant).getCaption();
      } else {
        item = constant.name();
      }

      if (BeeUtils.containsSame(item, value)) {
        filters.add(isEqual(column, IntegerValue.of(constant)));
      }
    }

    return or(filters);
  }

  public static Filter compareId(long value) {
    return compareId(Operator.EQ, value);
  }

  public static Filter compareId(Operator op, long value) {
    Assert.notNull(op);
    return new IdFilter(op, value);
  }

  public static Filter compareId(Operator op, String value) {
    if (!BeeUtils.isLong(value)) {
      LogUtils.getRootLogger().warning("Not an ID value:", value);
      return null;
    }
    return compareId(op, BeeUtils.toLong(value));
  }

  public static Filter compareVersion(long value) {
    return compareVersion(Operator.EQ, value);
  }

  public static Filter compareVersion(Operator op, long value) {
    Assert.notNull(op);
    return new VersionFilter(op, value);
  }

  public static Filter compareVersion(Operator op, String value) {
    Assert.notNull(op);
    DateTime time = TimeUtils.parseDateTime(value);

    if (time == null) {
      LogUtils.getRootLogger().warning("Not a DATETIME value:", value);
      return null;
    }
    return compareVersion(op, time.getTime());
  }

  public static Filter compareWithColumn(IsColumn left, Operator op, IsColumn right) {
    Assert.noNulls(left, op, right);
    String leftColumn = left.getId();
    ValueType leftType = left.getType();
    String rightColumn = right.getId();
    ValueType rightType = right.getType();

    if (!BeeUtils.same(leftType.getGroupCode(), rightType.getGroupCode())) {
      LogUtils.getRootLogger().warning("Incompatible column types:", leftColumn,
          BeeUtils.parenthesize(leftType), "AND", rightColumn, BeeUtils.parenthesize(rightType));
      return null;
    }
    return compareWithColumn(leftColumn, op, rightColumn);
  }

  public static Filter compareWithColumn(String leftColumn, Operator op, String rightColumn) {
    Assert.notEmpty(leftColumn);
    Assert.notNull(op);
    Assert.notEmpty(rightColumn);
    return new ColumnColumnFilter(leftColumn, op, rightColumn);
  }

  public static Filter compareWithValue(IsColumn column, Operator op, String value) {
    Assert.noNulls(column, op);
    Assert.notEmpty(value);

    if (ValueType.isNumeric(column.getType()) && !BeeUtils.isDouble(value)) {
      LogUtils.getRootLogger().warning("Not a numeric value:", value);
      return null;
    }
    return compareWithValue(column.getId(), op, Value.parseValue(column.getType(), value, true));
  }

  public static Filter compareWithValue(String column, Operator op, Value value) {
    Assert.notEmpty(column);
    Assert.noNulls(op, value);
    return new ColumnValueFilter(column, op, value);
  }

  public static Filter contains(String column, String value) {
    Assert.notEmpty(value);
    return new ColumnValueFilter(column, Operator.CONTAINS, new TextValue(value));
  }

  public static Filter custom(String key) {
    Assert.notEmpty(key);
    return new CustomFilter(key);
  }

  public static Filter custom(String key, List<String> args) {
    Assert.notEmpty(key);
    return new CustomFilter(key, args);
  }

  public static Filter custom(String key, String arg) {
    Assert.notEmpty(key);
    return new CustomFilter(key, Lists.newArrayList(arg));
  }

  public static Filter custom(String key, String arg1, String arg2) {
    Assert.notEmpty(key);
    return new CustomFilter(key, Lists.newArrayList(arg1, arg2));
  }

  public static Filter equals(String column, Long value) {
    return compareWithValue(column, Operator.EQ, new LongValue(value));
  }

  public static Filter idIn(Collection<Long> values) {
    Assert.notNull(values);

    if (values.isEmpty()) {
      return null;
    }
    return new IdFilter(values);
  }

  public static Filter idNotIn(Collection<Long> values) {
    Filter flt = idIn(values);

    if (flt != null) {
      flt = isNot(flt);
    }
    return flt;
  }

  public static Filter in(String column, String inView, String inColumn) {
    return in(column, inView, inColumn, null);
  }

  public static Filter in(String column, String inView, String inColumn, Filter inFilter) {
    Assert.notEmpty(column);
    Assert.notEmpty(inView);
    Assert.notEmpty(inColumn);

    return new ColumnInFilter(column, inView, inColumn, inFilter);
  }

  public static Filter isEqual(String column, Value value) {
    return compareWithValue(column, Operator.EQ, value);
  }

  public static Filter isFalse() {
    return new IsFalseFilter();
  }

  public static Filter isLess(String column, Value value) {
    return compareWithValue(column, Operator.LT, value);
  }

  public static Filter isLessEqual(String column, Value value) {
    return compareWithValue(column, Operator.LE, value);
  }

  public static Filter isMore(String column, Value value) {
    return compareWithValue(column, Operator.GT, value);
  }

  public static Filter isMoreEqual(String column, Value value) {
    return compareWithValue(column, Operator.GE, value);
  }

  public static Filter isNot(Filter filter) {
    Assert.notNull(filter);
    return new CompoundFilter(CompoundType.NOT, filter);
  }

  public static Filter isNotEqual(String column, Value value) {
    return compareWithValue(column, Operator.NE, value);
  }

  public static Filter isNull(String column) {
    Assert.notEmpty(column);
    return new ColumnIsNullFilter(column);
  }

  public static Filter isTrue() {
    return new IsTrueFilter();
  }

  public static Filter notEquals(String column, Long value) {
    return compareWithValue(column, Operator.NE, new LongValue(value));
  }

  public static Filter notNull(String column) {
    Assert.notEmpty(column);
    return new ColumnNotNullFilter(column);
  }

  public static CompoundFilter or() {
    return new CompoundFilter(CompoundType.OR);
  }

  public static Filter or(Collection<Filter> filters) {
    if (filters == null || filters.isEmpty()) {
      return null;
    } else {
      Filter[] arr = filters.toArray(new Filter[0]);
      int size = arr.length;

      switch (size) {
        case 1:
          return arr[0];
        case 2:
          return or(arr[0], arr[1]);
        default:
          return new CompoundFilter(CompoundType.OR, arr);
      }
    }
  }

  public static Filter or(Filter f1, Filter f2) {
    if (f1 == null) {
      return f2;
    } else if (f2 == null) {
      return f1;
    } else {
      return new CompoundFilter(CompoundType.OR, f1, f2);
    }
  }

  public static Filter restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);
    String clazz = arr[0];
    String data = arr[1];

    Filter flt = getFilter(clazz);
    flt.deserialize(data);
    return flt;
  }

  private static Filter getFilter(String clazz) {
    Filter flt = null;

    if (NameUtils.getClassName(ColumnValueFilter.class).equals(clazz)) {
      flt = new ColumnValueFilter();

    } else if (NameUtils.getClassName(ColumnColumnFilter.class).equals(clazz)) {
      flt = new ColumnColumnFilter();

    } else if (NameUtils.getClassName(ColumnIsNullFilter.class).equals(clazz)) {
      flt = new ColumnIsNullFilter();

    } else if (NameUtils.getClassName(ColumnNotNullFilter.class).equals(clazz)) {
      flt = new ColumnNotNullFilter();

    } else if (NameUtils.getClassName(CompoundFilter.class).equals(clazz)) {
      flt = new CompoundFilter();

    } else if (NameUtils.getClassName(IdFilter.class).equals(clazz)) {
      flt = new IdFilter();

    } else if (NameUtils.getClassName(VersionFilter.class).equals(clazz)) {
      flt = new VersionFilter();

    } else if (NameUtils.getClassName(IsFalseFilter.class).equals(clazz)) {
      flt = new IsFalseFilter();

    } else if (NameUtils.getClassName(IsTrueFilter.class).equals(clazz)) {
      flt = new IsTrueFilter();

    } else if (NameUtils.getClassName(ColumnInFilter.class).equals(clazz)) {
      flt = new ColumnInFilter();

    } else if (NameUtils.getClassName(CustomFilter.class).equals(clazz)) {
      flt = new CustomFilter();

    } else {
      Assert.unsupported("Unsupported class name: " + clazz);
    }
    return flt;
  }

  private boolean safe = true;

  protected Filter() {
    this.safe = false;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof Filter) {
      return toString().equals(obj.toString());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  public abstract boolean involvesColumn(String colName);

  @Override
  public abstract boolean isMatch(List<? extends IsColumn> columns, IsRow row);

  protected int getColumnIndex(String colName, List<? extends IsColumn> columns) {
    return DataUtils.getColumnIndex(colName, columns);
  }

  protected String serialize(Object obj) {
    return Codec.beeSerialize(new Object[] {NameUtils.getClassName(this.getClass()), obj});
  }

  protected void setSafe() {
    Assert.isFalse(safe);
    this.safe = true;
  }
}
