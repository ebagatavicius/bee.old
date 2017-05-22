package com.butent.bee.shared.modules.finance.analysis;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.time.MonthRange;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class AnalysisValue implements BeeSerializable {

  public static AnalysisValue actual(long columnId, long rowId, double value) {
    AnalysisValue av = new AnalysisValue(columnId, rowId);
    av.setActualValue(value);
    return av;
  }

  public static AnalysisValue budget(long columnId, long rowId, double value) {
    AnalysisValue av = new AnalysisValue(columnId, rowId);
    av.setBudgetValue(value);
    return av;
  }

  public static AnalysisValue of(long columnId, long rowId, Double actual, Double budget) {
    AnalysisValue av = new AnalysisValue(columnId, rowId);

    if (BeeUtils.isDouble(actual)) {
      av.setActualValue(actual);
    }
    if (BeeUtils.isDouble(budget)) {
      av.setBudgetValue(budget);
    }

    return av;
  }

  public static AnalysisValue of(long columnId, long rowId, String actual, String budget) {
    AnalysisValue av = new AnalysisValue(columnId, rowId);

    if (!BeeUtils.isEmpty(actual)) {
      av.setActualValue(actual);
    }
    if (!BeeUtils.isEmpty(budget)) {
      av.setBudgetValue(budget);
    }

    return av;
  }

  public static AnalysisValue of(long columnId, long rowId,
      Map<AnalysisSplitType, AnalysisSplitValue> columnSplit,
      Map<AnalysisSplitType, AnalysisSplitValue> rowSplit,
      Double actual, Double budget) {

    AnalysisValue av = new AnalysisValue(columnId, rowId);

    if (!BeeUtils.isEmpty(columnSplit)) {
      av.columnSplit.putAll(columnSplit);
    }
    if (!BeeUtils.isEmpty(rowSplit)) {
      av.rowSplit.putAll(rowSplit);
    }

    if (BeeUtils.isDouble(actual)) {
      av.setActualValue(actual);
    }
    if (BeeUtils.isDouble(budget)) {
      av.setBudgetValue(budget);
    }

    return av;
  }

  public static AnalysisValue restore(String s) {
    AnalysisValue av = new AnalysisValue();
    av.deserialize(s);
    return av;
  }

  private static String format(double value) {
    return BeeUtils.toString(value);
  }

  private enum Serial {
    COLUMN_ID, ROW_ID, COLUMN_SPLIT, ROW_SPLIT, ACTUAL_VALUE, BUDGET_VALUE
  }

  private long columnId;
  private long rowId;

  private final Map<AnalysisSplitType, AnalysisSplitValue> columnSplit =
      new EnumMap<>(AnalysisSplitType.class);
  private final Map<AnalysisSplitType, AnalysisSplitValue> rowSplit =
      new EnumMap<>(AnalysisSplitType.class);

  private String actualValue;
  private String budgetValue;

  private AnalysisValue() {
  }

  private AnalysisValue(long columnId, long rowId) {
    this.columnId = columnId;
    this.rowId = rowId;
  }

  public void add(AnalysisValue other) {
    if (other != null) {
      if (BeeUtils.isEmpty(actualValue)) {
        setActualValue(other.actualValue);
      } else if (BeeUtils.isDouble(other.actualValue)) {
        setActualValue(getActualNumber() + other.getActualNumber());
      }

      if (BeeUtils.isEmpty(budgetValue)) {
        setBudgetValue(other.budgetValue);
      } else if (BeeUtils.isDouble(other.budgetValue)) {
        setBudgetValue(getBudgetNumber() + other.getBudgetNumber());
      }
    }
  }

  public long getColumnId() {
    return columnId;
  }

  public long getRowId() {
    return rowId;
  }

  public Map<AnalysisSplitType, AnalysisSplitValue> getColumnSplit() {
    return columnSplit;
  }

  public Set<AnalysisSplitType> getColumnSplitTypes() {
    Set<AnalysisSplitType> types = EnumSet.noneOf(AnalysisSplitType.class);
    if (!columnSplit.isEmpty()) {
      types.addAll(columnSplit.keySet());
    }
    return types;
  }

  public Map<AnalysisSplitType, AnalysisSplitValue> getRowSplit() {
    return rowSplit;
  }

  public Set<AnalysisSplitType> getRowSplitTypes() {
    Set<AnalysisSplitType> types = EnumSet.noneOf(AnalysisSplitType.class);
    if (!rowSplit.isEmpty()) {
      types.addAll(rowSplit.keySet());
    }
    return types;
  }

  public String getActualValue() {
    return actualValue;
  }

  public double getActualNumber() {
    return BeeUtils.toDouble(getActualValue());
  }

  public boolean hasActualValue() {
    return !BeeUtils.isEmpty(getActualValue());
  }

  public String getBudgetValue() {
    return budgetValue;
  }

  public double getBudgetNumber() {
    return BeeUtils.toDouble(getBudgetValue());
  }

  public boolean hasBudgetValue() {
    return !BeeUtils.isEmpty(getBudgetValue());
  }

  public void addColumnSplit(Map<AnalysisSplitType, AnalysisSplitValue> split) {
    if (!BeeUtils.isEmpty(split)) {
      columnSplit.putAll(split);
    }
  }

  public void putColumnSplit(Map<AnalysisSplitType, AnalysisSplitValue> parentSplit,
      AnalysisSplitType splitType, AnalysisSplitValue splitValue) {

    if (!BeeUtils.isEmpty(parentSplit)) {
      columnSplit.putAll(parentSplit);
    }

    if (splitType != null && splitValue != null) {
      columnSplit.put(splitType, splitValue);
    }
  }

  public boolean containsColumnSplit(Map<AnalysisSplitType, AnalysisSplitValue> split) {
    if (BeeUtils.isEmpty(split)) {
      return true;
    } else if (columnSplit.isEmpty()) {
      return false;
    } else {
      return columnSplit.entrySet().containsAll(split.entrySet());
    }
  }

  public void addRowSplit(Map<AnalysisSplitType, AnalysisSplitValue> split) {
    if (!BeeUtils.isEmpty(split)) {
      rowSplit.putAll(split);
    }
  }

  public void putRowSplit(Map<AnalysisSplitType, AnalysisSplitValue> parentSplit,
      AnalysisSplitType splitType, AnalysisSplitValue splitValue) {

    if (!BeeUtils.isEmpty(parentSplit)) {
      rowSplit.putAll(parentSplit);
    }

    if (splitType != null && splitValue != null) {
      rowSplit.put(splitType, splitValue);
    }
  }

  public boolean containsRowSplit(Map<AnalysisSplitType, AnalysisSplitValue> split) {
    if (BeeUtils.isEmpty(split)) {
      return true;
    } else if (rowSplit.isEmpty()) {
      return false;
    } else {
      return rowSplit.entrySet().containsAll(split.entrySet());
    }
  }

  public AnalysisSplitValue getSplitValue(AnalysisSplitType splitType) {
    if (splitType == null) {
      return null;

    } else if (columnSplit.containsKey(splitType)) {
      return columnSplit.get(splitType);

    } else if (rowSplit.containsKey(splitType)) {
      return rowSplit.get(splitType);

    } else {
      return null;
    }
  }

  public MonthRange getMonthRange() {
    if (columnSplit.isEmpty() && rowSplit.isEmpty()) {
      return null;
    }

    for (AnalysisSplitType splitType : AnalysisSplitType.PERIODS_INCREASING) {
      AnalysisSplitValue splitValue = getSplitValue(splitType);

      if (splitValue != null) {
        return splitType.getMonthRange(splitValue);
      }
    }
    return null;
  }

  private void setColumnId(long columnId) {
    this.columnId = columnId;
  }

  private void setRowId(long rowId) {
    this.rowId = rowId;
  }

  private void setActualValue(String actualValue) {
    this.actualValue = actualValue;
  }

  public void setActualValue(double value) {
    setActualValue(format(value));
  }

  public void updateActualValue(Double value) {
    if (BeeUtils.nonZero(value)) {
      setActualValue(value);
    } else {
      this.actualValue = null;
    }
  }

  private void setBudgetValue(String budgetValue) {
    this.budgetValue = budgetValue;
  }

  public void setBudgetValue(double value) {
    setBudgetValue(format(value));
  }

  public void updateBudgetValue(Double value) {
    if (BeeUtils.nonZero(value)) {
      setBudgetValue(value);
    } else {
      this.budgetValue = null;
    }
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, Serial.values().length);

    for (int i = 0; i < arr.length; i++) {
      String v = arr[i];

      if (!BeeUtils.isEmpty(v)) {
        switch (Serial.values()[i]) {
          case COLUMN_ID:
            setColumnId(BeeUtils.toLong(v));
            break;
          case ROW_ID:
            setRowId(BeeUtils.toLong(v));
            break;

          case COLUMN_SPLIT:
            if (!columnSplit.isEmpty()) {
              columnSplit.clear();
            }
            columnSplit.putAll(deserializeSplit(v));
            break;
          case ROW_SPLIT:
            if (!rowSplit.isEmpty()) {
              rowSplit.clear();
            }
            rowSplit.putAll(deserializeSplit(v));
            break;

          case ACTUAL_VALUE:
            setActualValue(v);
            break;
          case BUDGET_VALUE:
            setBudgetValue(v);
            break;
        }
      }
    }
  }

  private static Map<AnalysisSplitType, AnalysisSplitValue> deserializeSplit(String s) {
    Map<AnalysisSplitType, AnalysisSplitValue> split = new EnumMap<>(AnalysisSplitType.class);

    Codec.deserializeHashMap(s).forEach((st, sv) -> {
      AnalysisSplitType type = EnumUtils.getEnumByName(AnalysisSplitType.class, st);
      if (type != null) {
        split.put(type, AnalysisSplitValue.restore(sv));
      }
    });

    return split;
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[Serial.values().length];
    int i = 0;

    for (Serial member : Serial.values()) {
      switch (member) {
        case COLUMN_ID:
          arr[i++] = getColumnId();
          break;
        case ROW_ID:
          arr[i++] = getRowId();
          break;

        case COLUMN_SPLIT:
          arr[i++] = getColumnSplit();
          break;
        case ROW_SPLIT:
          arr[i++] = getRowSplit();
          break;

        case ACTUAL_VALUE:
          arr[i++] = getActualValue();
          break;
        case BUDGET_VALUE:
          arr[i++] = getBudgetValue();
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  public boolean matches(AnalysisValue other) {
    return other != null
        && columnId == other.columnId
        && rowId == other.rowId
        && columnSplit.equals(other.columnSplit)
        && rowSplit.equals(other.rowSplit);
  }

  public void round(int scale) {
    if (BeeUtils.isDouble(actualValue)) {
      setActualValue(BeeUtils.round(actualValue, scale));
    }

    if (BeeUtils.isDouble(budgetValue)) {
      setBudgetValue(BeeUtils.round(budgetValue, scale));
    }
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("c", columnId, "r", rowId,
        "cs", columnSplit.isEmpty() ? BeeConst.STRING_EMPTY : columnSplit.toString(),
        "rs", rowSplit.isEmpty() ? BeeConst.STRING_EMPTY : rowSplit.toString(),
        "a", actualValue,
        "b", budgetValue);
  }
}
