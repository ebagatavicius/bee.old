package com.butent.bee.shared.modules.finance.analysis;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.NonNullList;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.time.MonthRange;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class AnalysisResults implements BeeSerializable {

  public static AnalysisResults restore(String s) {
    AnalysisResults ar = new AnalysisResults();
    ar.deserialize(s);
    return ar;
  }

  private enum Serial {
    HEADER_INDEXES, HEADER, COLUMN_INDEXES, COLUMNS, ROW_INDEXES, ROWS,
    COLUMN_SPLIT_TYPES, ROW_SPLIT_TYPES, COLUMN_SPLIT_VALUES, ROW_SPLIT_VALUES, VALUES,
    INIT_START, VALIDATE_START, COMPUTE_START, COMPUTE_END, QUERY_COUNT, QUERY_DURATION
  }

  private static final List<AnalysisSplitType> EMPTY_SPLIT_TYPES = Collections.emptyList();

  private long initStart;
  private long validateStart;
  private long computeStart;
  private long computeEnd;

  private long queryCount;
  private long queryDuration;

  private final Map<String, Integer> headerIndexes = new HashMap<>();
  private BeeRow header;

  private final Map<String, Integer> columnIndexes = new HashMap<>();
  private final List<BeeRow> columns = new ArrayList<>();

  private final Map<String, Integer> rowIndexes = new HashMap<>();
  private final List<BeeRow> rows = new ArrayList<>();

  private final Map<Long, List<AnalysisSplitType>> columnSplitTypes = new HashMap<>();
  private final Map<Long, List<AnalysisSplitType>> rowSplitTypes = new HashMap<>();

  private final Map<Long, Map<AnalysisSplitType, List<AnalysisSplitValue>>> columnSplitValues =
      new HashMap<>();
  private final Map<Long, Map<AnalysisSplitType, List<AnalysisSplitValue>>> rowSplitValues =
      new HashMap<>();

  private final List<AnalysisValue> values = new ArrayList<>();

  private AnalysisResults() {
  }

  public AnalysisResults(Map<String, Integer> headerIndexes, BeeRow header,
      Map<String, Integer> columnIndexes, List<BeeRow> columns,
      Map<String, Integer> rowIndexes, List<BeeRow> rows) {

    this.headerIndexes.putAll(headerIndexes);
    this.header = header;

    this.columnIndexes.putAll(columnIndexes);
    this.columns.addAll(columns);

    this.rowIndexes.putAll(rowIndexes);
    this.rows.addAll(rows);
  }

  public void addValue(AnalysisValue value) {
    if (value != null) {
      values.add(value);
    }
  }

  public void addValues(Collection<AnalysisValue> collection) {
    if (collection != null) {
      collection.forEach(this::addValue);
    }
  }

  public void updateBudget(AnalysisValue value) {
    if (value != null && value.hasBudgetValue()) {
      for (AnalysisValue av : values) {
        if (av.matches(value)) {
          av.setBudgetValue(value.getBudgetNumber());
          return;
        }
      }

      addValue(value);
    }
  }

  public void updateBudget(Collection<AnalysisValue> collection) {
    if (collection != null) {
      collection.forEach(this::updateBudget);
    }
  }

  public boolean containsValues(long columnId, long rowId) {
    for (AnalysisValue av : values) {
      if (av.getColumnId() == columnId && av.getRowId() == rowId) {
        return true;
      }
    }
    return false;
  }

  public List<AnalysisValue> getValues(long columnId, long rowId) {
    List<AnalysisValue> result = new ArrayList<>();

    for (AnalysisValue av : values) {
      if (av.getColumnId() == columnId && av.getRowId() == rowId) {
        result.add(av);
      }
    }

    return result;
  }

  public Multimap<Long, AnalysisValue> getColumnValuesByRow(long columnId,
      Collection<Long> rowIds, Predicate<AnalysisValue> predicate) {

    Multimap<Long, AnalysisValue> result = ArrayListMultimap.create();

    if (!BeeUtils.isEmpty(rowIds)) {
      for (long rowId : rowIds) {
        List<AnalysisValue> list = getValues(columnId, rowId);

        if (!list.isEmpty()) {
          if (predicate == null) {
            result.putAll(rowId, list);

          } else {
            for (AnalysisValue analysisValue : list) {
              if (predicate.test(analysisValue)) {
                result.put(rowId, analysisValue);
              }
            }
          }
        }
      }
    }

    return result;
  }

  public Multimap<Long, AnalysisValue> getRowValuesByColumn(long rowId,
      Collection<Long> columnIds, Predicate<AnalysisValue> predicate) {

    Multimap<Long, AnalysisValue> result = ArrayListMultimap.create();

    if (!BeeUtils.isEmpty(columnIds)) {
      for (long columnId : columnIds) {
        List<AnalysisValue> list = getValues(columnId, rowId);

        if (!list.isEmpty()) {
          if (predicate == null) {
            result.putAll(columnId, list);

          } else {
            for (AnalysisValue analysisValue : list) {
              if (predicate.test(analysisValue)) {
                result.put(columnId, analysisValue);
              }
            }
          }
        }
      }
    }

    return result;
  }

  public void addColumnSplitTypes(long columnId, List<AnalysisSplitType> splitTypes) {
    if (!BeeUtils.isEmpty(splitTypes)) {
      columnSplitTypes.put(columnId, splitTypes);
    }
  }

  public void addColumnSplitValues(long columnId, AnalysisSplitType splitType,
      List<AnalysisSplitValue> splitValues) {

    if (!BeeUtils.isEmpty(splitValues)) {
      if (columnSplitValues.containsKey(columnId)) {
        AnalysisSplitValue.putSplitValues(columnSplitValues.get(columnId), splitType, splitValues);

      } else {
        Map<AnalysisSplitType, List<AnalysisSplitValue>> map = new HashMap<>();
        map.put(splitType, splitValues);

        columnSplitValues.put(columnId, map);
      }
    }
  }

  public void addColumnSplitValues(long columnId,
      Map<AnalysisSplitType, List<AnalysisSplitValue>> map) {

    if (!BeeUtils.isEmpty(map)) {
      map.forEach((type, splitValues) -> addColumnSplitValues(columnId, type, splitValues));
    }
  }

  public void addRowSplitTypes(long rowId, List<AnalysisSplitType> splitTypes) {
    if (!BeeUtils.isEmpty(splitTypes)) {
      rowSplitTypes.put(rowId, splitTypes);
    }
  }

  public void addRowSplitValues(long rowId, AnalysisSplitType splitType,
      List<AnalysisSplitValue> splitValues) {

    if (!BeeUtils.isEmpty(splitValues)) {
      if (rowSplitValues.containsKey(rowId)) {
        AnalysisSplitValue.putSplitValues(rowSplitValues.get(rowId), splitType, splitValues);

      } else {
        Map<AnalysisSplitType, List<AnalysisSplitValue>> map = new HashMap<>();
        map.put(splitType, splitValues);

        rowSplitValues.put(rowId, map);
      }
    }
  }

  public void addRowSplitValues(long rowId, Map<AnalysisSplitType, List<AnalysisSplitValue>> map) {
    if (!BeeUtils.isEmpty(map)) {
      map.forEach((type, splitValues) -> addRowSplitValues(rowId, type, splitValues));
    }
  }

  public List<AnalysisSplitType> getColumnSplitTypes(long columnId) {
    return columnSplitTypes.getOrDefault(columnId, EMPTY_SPLIT_TYPES);
  }

  public Map<AnalysisSplitType, List<AnalysisSplitValue>> getColumnSplitValues(long columnId) {
    return columnSplitValues.get(columnId);
  }

  public List<Map<AnalysisSplitType, List<AnalysisSplitValue>>> getColumnSplitValues(
      Collection<Long> columnIds) {

    List<Map<AnalysisSplitType, List<AnalysisSplitValue>>> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(columnIds)) {
      for (long columnId : columnIds) {
        Map<AnalysisSplitType, List<AnalysisSplitValue>> map = getColumnSplitValues(columnId);
        if (map != null) {
          result.add(map);
        }
      }
    }
    return result;
  }

  public List<AnalysisSplitType> getRowSplitTypes(long rowId) {
    return rowSplitTypes.getOrDefault(rowId, EMPTY_SPLIT_TYPES);
  }

  public Map<AnalysisSplitType, List<AnalysisSplitValue>> getRowSplitValues(long rowId) {
    return rowSplitValues.get(rowId);
  }

  public List<Map<AnalysisSplitType, List<AnalysisSplitValue>>> getRowSplitValues(
      Collection<Long> rowIds) {

    List<Map<AnalysisSplitType, List<AnalysisSplitValue>>> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(rowIds)) {
      for (long rowId : rowIds) {
        Map<AnalysisSplitType, List<AnalysisSplitValue>> map = getRowSplitValues(rowId);
        if (map != null) {
          result.add(map);
        }
      }
    }
    return result;
  }

  public List<AnalysisValue> getValues() {
    return values;
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, Serial.values().length);

    String[] items;

    for (int i = 0; i < arr.length; i++) {
      String v = arr[i];

      if (!BeeUtils.isEmpty(v)) {
        switch (Serial.values()[i]) {
          case HEADER_INDEXES:
            if (!headerIndexes.isEmpty()) {
              headerIndexes.clear();
            }

            headerIndexes.putAll(deserializeIndexes(v));
            break;

          case HEADER:
            setHeader(BeeRow.restore(v));
            break;

          case COLUMN_INDEXES:
            if (!columnIndexes.isEmpty()) {
              columnIndexes.clear();
            }

            columnIndexes.putAll(deserializeIndexes(v));
            break;

          case COLUMNS:
            if (!columns.isEmpty()) {
              columns.clear();
            }

            items = Codec.beeDeserializeCollection(v);
            if (items != null) {
              for (String item : items) {
                columns.add(BeeRow.restore(item));
              }
            }
            break;

          case ROW_INDEXES:
            if (!rowIndexes.isEmpty()) {
              rowIndexes.clear();
            }

            rowIndexes.putAll(deserializeIndexes(v));
            break;

          case ROWS:
            if (!rows.isEmpty()) {
              rows.clear();
            }

            items = Codec.beeDeserializeCollection(v);
            if (items != null) {
              for (String item : items) {
                rows.add(BeeRow.restore(item));
              }
            }
            break;

          case COLUMN_SPLIT_TYPES:
            if (!columnSplitTypes.isEmpty()) {
              columnSplitTypes.clear();
            }

            columnSplitTypes.putAll(deserializeSplitTypes(v));
            break;

          case ROW_SPLIT_TYPES:
            if (!rowSplitTypes.isEmpty()) {
              rowSplitTypes.clear();
            }

            rowSplitTypes.putAll(deserializeSplitTypes(v));
            break;

          case COLUMN_SPLIT_VALUES:
            if (!columnSplitValues.isEmpty()) {
              columnSplitValues.clear();
            }

            columnSplitValues.putAll(deserializeSplitValues(v));
            break;

          case ROW_SPLIT_VALUES:
            if (!rowSplitValues.isEmpty()) {
              rowSplitValues.clear();
            }

            rowSplitValues.putAll(deserializeSplitValues(v));
            break;

          case VALUES:
            if (!values.isEmpty()) {
              values.clear();
            }

            items = Codec.beeDeserializeCollection(v);
            if (items != null) {
              for (String item : items) {
                addValue(AnalysisValue.restore(item));
              }
            }
            break;

          case INIT_START:
            setInitStart(BeeUtils.toLong(v));
            break;
          case VALIDATE_START:
            setValidateStart(BeeUtils.toLong(v));
            break;
          case COMPUTE_START:
            setComputeStart(BeeUtils.toLong(v));
            break;
          case COMPUTE_END:
            setComputeEnd(BeeUtils.toLong(v));
            break;

          case QUERY_COUNT:
            setQueryCount(BeeUtils.toLong(v));
            break;
          case QUERY_DURATION:
            setQueryDuration(BeeUtils.toLong(v));
            break;
        }
      }
    }
  }

  private static Map<String, Integer> deserializeIndexes(String s) {
    Map<String, Integer> indexes = new HashMap<>();

    Codec.deserializeHashMap(s).forEach((k, v) -> {
      Integer index = BeeUtils.toIntOrNull(v);
      if (index != null) {
        indexes.put(k, index);
      }
    });

    return indexes;
  }

  private static Map<Long, List<AnalysisSplitType>> deserializeSplitTypes(String s) {
    Map<Long, List<AnalysisSplitType>> result = new HashMap<>();

    Codec.deserializeHashMap(s).forEach((k, v) -> {
      Long id = BeeUtils.toLongOrNull(k);
      List<AnalysisSplitType> types = new ArrayList<>();

      String[] items = Codec.beeDeserializeCollection(v);
      if (items != null) {
        for (String item : items) {
          AnalysisSplitType type = EnumUtils.getEnumByName(AnalysisSplitType.class, item);
          if (type != null) {
            types.add(type);
          }
        }
      }

      if (DataUtils.isId(id) && !types.isEmpty()) {
        result.put(id, types);
      }
    });

    return result;
  }

  private static Map<Long, Map<AnalysisSplitType, List<AnalysisSplitValue>>> deserializeSplitValues(
      String s) {

    Map<Long, Map<AnalysisSplitType, List<AnalysisSplitValue>>> result = new HashMap<>();

    Codec.deserializeHashMap(s).forEach((k, v) -> {
      Long id = BeeUtils.toLongOrNull(k);

      if (DataUtils.isId(id)) {
        Map<AnalysisSplitType, List<AnalysisSplitValue>> map = new HashMap<>();

        Codec.deserializeHashMap(v).forEach((ast, asvList) -> {
          AnalysisSplitType type = EnumUtils.getEnumByName(AnalysisSplitType.class, ast);
          List<AnalysisSplitValue> splitValues = new ArrayList<>();

          String[] items = Codec.beeDeserializeCollection(asvList);
          if (items != null) {
            for (String item : items) {
              splitValues.add(AnalysisSplitValue.restore(item));
            }
          }

          if (type != null && !splitValues.isEmpty()) {
            map.put(type, splitValues);
          }
        });

        if (!map.isEmpty()) {
          result.put(id, map);
        }
      }
    });

    return result;
  }

  private void setHeader(BeeRow header) {
    this.header = header;
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[Serial.values().length];
    int i = 0;

    for (Serial member : Serial.values()) {
      switch (member) {
        case HEADER_INDEXES:
          arr[i++] = headerIndexes;
          break;
        case HEADER:
          arr[i++] = header;
          break;

        case COLUMN_INDEXES:
          arr[i++] = columnIndexes;
          break;
        case COLUMNS:
          arr[i++] = columns;
          break;

        case ROW_INDEXES:
          arr[i++] = rowIndexes;
          break;
        case ROWS:
          arr[i++] = rows;
          break;

        case COLUMN_SPLIT_TYPES:
          arr[i++] = columnSplitTypes;
          break;
        case ROW_SPLIT_TYPES:
          arr[i++] = rowSplitTypes;
          break;

        case COLUMN_SPLIT_VALUES:
          arr[i++] = columnSplitValues;
          break;
        case ROW_SPLIT_VALUES:
          arr[i++] = rowSplitValues;
          break;

        case VALUES:
          arr[i++] = values;
          break;

        case INIT_START:
          arr[i++] = initStart;
          break;
        case VALIDATE_START:
          arr[i++] = validateStart;
          break;
        case COMPUTE_START:
          arr[i++] = computeStart;
          break;
        case COMPUTE_END:
          arr[i++] = computeEnd;
          break;

        case QUERY_COUNT:
          arr[i++] = queryCount;
          break;
        case QUERY_DURATION:
          arr[i++] = queryDuration;
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  public long getInitStart() {
    return initStart;
  }

  public void setInitStart(long initStart) {
    this.initStart = initStart;
  }

  public long getValidateStart() {
    return validateStart;
  }

  public void setValidateStart(long validateStart) {
    this.validateStart = validateStart;
  }

  public long getComputeStart() {
    return computeStart;
  }

  public void setComputeStart(long computeStart) {
    this.computeStart = computeStart;
  }

  public long getComputeEnd() {
    return computeEnd;
  }

  public void setComputeEnd(long computeEnd) {
    this.computeEnd = computeEnd;
  }

  public long getQueryCount() {
    return queryCount;
  }

  public void setQueryCount(long queryCount) {
    this.queryCount = queryCount;
  }

  public long getQueryDuration() {
    return queryDuration;
  }

  public void setQueryDuration(long queryDuration) {
    this.queryDuration = queryDuration;
  }

  public long getHeaderId() {
    return header.getId();
  }

  private Integer getHeaderInteger(String key) {
    return header.getInteger(headerIndexes.get(key));
  }

  public List<AnalysisLabel> getHeaderLabels(String period, boolean includeName) {
    List<AnalysisLabel> labels = new NonNullList<>();

    if (includeName) {
      labels.add(new AnalysisLabel(COL_ANALYSIS_NAME, getHeaderString(COL_ANALYSIS_NAME),
          getHeaderString(COL_ANALYSIS_HEADER_BACKGROUND),
          getHeaderString(COL_ANALYSIS_HEADER_FOREGROUND)));
    }

    for (int ordinal = 1; ordinal <= Dimensions.getObserved(); ordinal++) {
      labels.add(AnalysisLabel.dimension(header, headerIndexes, ordinal));
    }

    labels.add(AnalysisLabel.employee(header, headerIndexes, COL_ANALYSIS_HEADER_EMPLOYEE));
    labels.add(AnalysisLabel.budgetType(header, headerIndexes, COL_ANALYSIS_HEADER_BUDGET_TYPE));
    labels.add(AnalysisLabel.period(period));
    labels.add(AnalysisLabel.currency(header, headerIndexes, COL_ANALYSIS_HEADER_CURRENCY));

    return labels;
  }

  public MonthRange getHeaderRange() {
    Integer yearFrom = getHeaderInteger(COL_ANALYSIS_HEADER_YEAR_FROM);
    Integer monthFrom = getHeaderInteger(COL_ANALYSIS_HEADER_MONTH_FROM);
    Integer yearUntil = getHeaderInteger(COL_ANALYSIS_HEADER_YEAR_UNTIL);
    Integer monthUntil = getHeaderInteger(COL_ANALYSIS_HEADER_MONTH_UNTIL);

    return AnalysisUtils.getRange(yearFrom, monthFrom, yearUntil, monthUntil);
  }

  public String getHeaderString(String key) {
    return header.getString(headerIndexes.get(key));
  }

  private boolean isHeaderTrue(String key) {
    return header.isTrue(headerIndexes.get(key));
  }

  private Integer getColumnInteger(BeeRow column, String key) {
    return column.getInteger(columnIndexes.get(key));
  }

  public List<AnalysisLabel> getColumnLabels(BeeRow column, String period) {
    List<AnalysisLabel> labels = new NonNullList<>();

    labels.add(AnalysisLabel.value(column, columnIndexes, COL_ANALYSIS_COLUMN_NAME,
        COL_ANALYSIS_COLUMN_BACKGROUND, COL_ANALYSIS_COLUMN_FOREGROUND));
    labels.add(AnalysisLabel.value(column, columnIndexes, COL_ANALYSIS_COLUMN_ABBREVIATION));

    labels.add(AnalysisLabel.indicator(column, columnIndexes, COL_ANALYSIS_COLUMN_INDICATOR));
    labels.add(AnalysisLabel.turnoverOrBalance(column, columnIndexes,
        COL_ANALYSIS_COLUMN_TURNOVER_OR_BALANCE));

    labels.add(AnalysisLabel.budgetType(column, columnIndexes, COL_ANALYSIS_COLUMN_BUDGET_TYPE));

    for (int ordinal = 1; ordinal <= Dimensions.getObserved(); ordinal++) {
      if (isHeaderTrue(colAnalysisShowColumnDimension(ordinal))) {
        labels.add(AnalysisLabel.dimension(column, columnIndexes, ordinal));
      }
    }

    if (isHeaderTrue(COL_ANALYSIS_SHOW_COLUMN_EMPLOYEE)) {
      labels.add(AnalysisLabel.employee(column, columnIndexes, COL_ANALYSIS_COLUMN_EMPLOYEE));
    }

    labels.add(AnalysisLabel.period(period));

    return labels;
  }

  public MonthRange getColumnRange(BeeRow column) {
    Integer yearFrom = getColumnInteger(column, COL_ANALYSIS_COLUMN_YEAR_FROM);
    Integer monthFrom = getColumnInteger(column, COL_ANALYSIS_COLUMN_MONTH_FROM);
    Integer yearUntil = getColumnInteger(column, COL_ANALYSIS_COLUMN_YEAR_UNTIL);
    Integer monthUntil = getColumnInteger(column, COL_ANALYSIS_COLUMN_MONTH_UNTIL);

    return AnalysisUtils.getRange(yearFrom, monthFrom, yearUntil, monthUntil);
  }

  public String getColumnString(BeeRow column, String key) {
    return column.getString(columnIndexes.get(key));
  }

  public List<AnalysisCellType> getColumnCellTypes(BeeRow column) {
    return AnalysisCellType.normalize(getColumnString(column, COL_ANALYSIS_COLUMN_VALUES));
  }

  public boolean isColumnVisible(BeeRow column) {
    return column.isTrue(columnIndexes.get(COL_ANALYSIS_COLUMN_SELECTED));
  }

  private Integer getRowInteger(BeeRow row, String key) {
    return row.getInteger(rowIndexes.get(key));
  }

  public List<AnalysisLabel> getRowLabels(BeeRow row, String period) {
    List<AnalysisLabel> labels = new NonNullList<>();

    labels.add(AnalysisLabel.value(row, rowIndexes, COL_ANALYSIS_ROW_NAME,
        COL_ANALYSIS_ROW_BACKGROUND, COL_ANALYSIS_ROW_FOREGROUND));
    labels.add(AnalysisLabel.value(row, rowIndexes, COL_ANALYSIS_ROW_ABBREVIATION));

    labels.add(AnalysisLabel.indicator(row, rowIndexes, COL_ANALYSIS_ROW_INDICATOR));
    labels.add(AnalysisLabel.turnoverOrBalance(row, rowIndexes,
        COL_ANALYSIS_ROW_TURNOVER_OR_BALANCE));

    labels.add(AnalysisLabel.budgetType(row, rowIndexes, COL_ANALYSIS_ROW_BUDGET_TYPE));

    for (int ordinal = 1; ordinal <= Dimensions.getObserved(); ordinal++) {
      if (isHeaderTrue(colAnalysisShowRowDimension(ordinal))) {
        labels.add(AnalysisLabel.dimension(row, rowIndexes, ordinal));
      }
    }

    if (isHeaderTrue(COL_ANALYSIS_SHOW_ROW_EMPLOYEE)) {
      labels.add(AnalysisLabel.employee(row, rowIndexes, COL_ANALYSIS_ROW_EMPLOYEE));
    }

    labels.add(AnalysisLabel.period(period));

    return labels;
  }

  public MonthRange getRowRange(BeeRow row) {
    Integer yearFrom = getRowInteger(row, COL_ANALYSIS_ROW_YEAR_FROM);
    Integer monthFrom = getRowInteger(row, COL_ANALYSIS_ROW_MONTH_FROM);
    Integer yearUntil = getRowInteger(row, COL_ANALYSIS_ROW_YEAR_UNTIL);
    Integer monthUntil = getRowInteger(row, COL_ANALYSIS_ROW_MONTH_UNTIL);

    return AnalysisUtils.getRange(yearFrom, monthFrom, yearUntil, monthUntil);
  }

  public String getRowString(BeeRow row, String key) {
    return row.getString(rowIndexes.get(key));
  }

  public List<AnalysisCellType> getRowCellTypes(BeeRow row) {
    return AnalysisCellType.normalize(getRowString(row, COL_ANALYSIS_ROW_VALUES));
  }

  public boolean isRowVisible(BeeRow row) {
    return row.isTrue(rowIndexes.get(COL_ANALYSIS_ROW_SELECTED));
  }

  public List<BeeRow> getColumns() {
    return columns;
  }

  public List<BeeRow> getRows() {
    return rows;
  }
}
