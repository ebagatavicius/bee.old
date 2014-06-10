package com.butent.bee.shared.data.filter;

import com.google.common.collect.Lists;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Enables to compare column data against given value (for example quantity > 10).
 */
public class ColumnValueFilter extends ComparisonFilter {

  protected ColumnValueFilter() {
    super();
  }

  protected ColumnValueFilter(String column, Operator operator, Value value) {
    super(column,
        operator.isStringOperator() && !ValueType.isString(value.getType())
            ? Operator.EQ
            : operator,
        Lists.newArrayList(value));
  }

  protected ColumnValueFilter(String column, List<Value> values) {
    super(column, Operator.IN, values);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Value> getValue() {
    return (List<Value>) super.getValue();
  }

  @Override
  public boolean involvesColumn(String colName) {
    return BeeUtils.same(colName, getColumn());
  }

  @Override
  public boolean isMatch(List<? extends IsColumn> columns, IsRow row) {
    int columnIndex = getColumnIndex(getColumn(), columns);
    Value columnValue = row.getValue(columnIndex, columns.get(columnIndex).getType());
    boolean ok = false;

    for (Value value : getValue()) {
      ok = isOperatorMatch(columnValue, value);

      if (EnumSet.of(Operator.IN, Operator.EQ).contains(getOperator()) == ok) {
        break;
      }
    }
    return ok;
  }

  @Override
  public String toString() {
    List<String> values = new ArrayList<>();

    for (Value val : getValue()) {
      String value = val.getString();

      if (ValueType.isString(val.getType())) {
        value = "\"" + value + "\"";
      }
      values.add(value);
    }
    return BeeUtils.join(BeeConst.STRING_EMPTY, getColumn(), getOperator().toTextString(), values);
  }

  @Override
  protected List<Value> restoreValue(String s) {
    List<Value> values = new ArrayList<>();

    for (String value : Codec.beeDeserializeCollection(s)) {
      values.add(Value.restore(value));
    }
    return values;
  }
}