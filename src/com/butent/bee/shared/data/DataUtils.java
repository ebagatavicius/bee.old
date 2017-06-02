package com.butent.bee.shared.data;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Wildcards;
import com.butent.bee.shared.utils.Wildcards.Pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Contains a set of utility functions for data management.
 */

public final class DataUtils {

  public static final String TABLE_NAMESPACE = "http://www.butent.com/table";
  public static final String VIEW_NAMESPACE = "http://www.butent.com/view";
  public static final String EXPRESSION_NAMESPACE = "http://www.butent.com/expression";
  public static final String MENU_NAMESPACE = "http://www.butent.com/menu";

  public static final String ID_TAG = "ID";
  public static final String VERSION_TAG = "VERSION";

  public static final int ID_INDEX = -2;
  public static final int VERSION_INDEX = -3;

  public static final ValueType ID_TYPE = ValueType.LONG;
  public static final ValueType VERSION_TYPE = ValueType.LONG;

  public static final long NEW_ROW_ID = 0L;
  public static final long NEW_ROW_VERSION = 0L;

  private static BeeLogger logger = LogUtils.getLogger(DataUtils.class);

  private static final Predicate<Long> IS_ID = DataUtils::isId;

  private static final char ID_LIST_SEPARATOR = ',';

  private static final Joiner ID_JOINER = Joiner.on(ID_LIST_SEPARATOR).skipNulls();
  private static final Splitter ID_SPLITTER =
      Splitter.on(ID_LIST_SEPARATOR).omitEmptyStrings().trimResults();

  private static int maxInitialRowSetSize = 50;

  public static void addNotNullLongs(Set<Long> target, BeeRowSet rowSet, String columnId) {
    Assert.notNull(target);
    Assert.notNull(rowSet);

    int index = rowSet.getColumnIndex(columnId);
    if (!BeeConst.isUndef(index) && !rowSet.isEmpty()) {
      BeeUtils.addAllNotNull(target, rowSet.getDistinctLongs(index));
    }
  }

  @SuppressWarnings("unchecked")
  public static Set<Long> asIdSet(Object obj) {
    return (obj instanceof Set) ? (Set<Long>) obj : null;
  }

  public static String buildIdList(BeeRowSet rowSet) {
    if (rowSet == null) {
      return null;
    } else {
      return buildIdList(getRowIds(rowSet));
    }
  }

  public static String buildIdList(Collection<Long> ids) {
    if (BeeUtils.isEmpty(ids)) {
      return null;
    } else {
      return ID_JOINER.join(ids.stream().filter(IS_ID).iterator());
    }
  }

  public static String buildIdList(Long... ids) {
    if (ids == null || ids.length == 0) {
      return null;
    } else {
      return buildIdList(Lists.newArrayList(ids));
    }
  }

  public static BeeColumn cloneColumn(IsColumn original) {
    Assert.notNull(original);
    if (original instanceof BeeColumn) {
      return ((BeeColumn) original).copy();
    }

    BeeColumn result = new BeeColumn(original.getType(), original.getLabel(), original.getId());

    result.setPattern(original.getPattern());
    if (original.getProperties() != null) {
      result.setProperties(original.getProperties().copy());
    }

    result.setPrecision(original.getPrecision());
    result.setScale(original.getScale());

    return result;
  }

  public static BeeRow cloneRow(IsRow original) {
    Assert.notNull(original);
    String[] arr = new String[original.getNumberOfCells()];

    for (int i = 0; i < arr.length; i++) {
      arr[i] = original.getString(i);
    }
    BeeRow result = new BeeRow(original.getId(), original.getVersion(), arr);
    result.setEditable(original.isEditable());
    result.setRemovable(original.isRemovable());

    if (!BeeUtils.isEmpty(original.getProperties())) {
      result.setProperties(original.getProperties().copy());
    }
    return result;
  }

  public static BeeRowSet cloneRowSet(BeeRowSet original) {
    Assert.notNull(original);
    BeeRowSet result = new BeeRowSet(original.getViewName(), original.getColumns());
    if (original.isEmpty()) {
      return result;
    }

    for (BeeRow row : original.getRows()) {
      result.addRow(cloneRow(row));
    }
    original.copyProperties(result);
    return result;
  }

  public static boolean contains(List<? extends IsColumn> columns, String columnId) {
    return !BeeConst.isUndef(getColumnIndex(columnId, columns));
  }

  public static boolean containsNull(BeeRowSet rowSet, String columnId) {
    if (!DataUtils.isEmpty(rowSet) && rowSet.containsColumn(columnId)) {
      int index = rowSet.getColumnIndex(columnId);
      return rowSet.getRows().stream().anyMatch(row -> row.isNull(index));

    } else {
      return false;
    }
  }

  public static BeeRow createEmptyRow(int columnCount) {
    return new BeeRow(NEW_ROW_ID, NEW_ROW_VERSION, new String[Assert.isPositive(columnCount)]);
  }

  public static BeeRowSet createRowSetForInsert(String viewName, List<BeeColumn> columns,
      List<String> values) {

    if (BeeUtils.isEmpty(columns)) {
      return null;
    }
    BeeRow row = createEmptyRow(columns.size());
    row.setValues(Assert.notNull(values));

    return createRowSetForInsert(viewName, columns, row, null, false);
  }

  public static BeeRowSet createRowSetForInsert(String viewName, List<BeeColumn> columns,
      IsRow row) {
    return createRowSetForInsert(viewName, columns, row, null, false);
  }

  public static BeeRowSet createRowSetForInsert(String viewName, List<BeeColumn> columns,
      IsRow row, Collection<String> alwaysInclude, boolean addProperties) {

    if (BeeUtils.isEmpty(columns)) {
      return null;
    }
    Assert.notNull(row);

    List<BeeColumn> newColumns = new ArrayList<>();
    List<String> values = new ArrayList<>();

    for (int i = 0; i < columns.size(); i++) {
      BeeColumn column = columns.get(i);
      if (!column.isEditable()) {
        continue;
      }

      String value = row.getString(i);
      if (!BeeUtils.isEmpty(value)
          || alwaysInclude != null && alwaysInclude.contains(column.getId())) {
        newColumns.add(column);
        values.add(value);
      }
    }
    if (newColumns.isEmpty()) {
      return null;
    }

    BeeRow newRow = new BeeRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION, values);
    if (addProperties && row.getProperties() != null) {
      newRow.setProperties(row.getProperties().copy());
    }

    BeeRowSet rs = new BeeRowSet(viewName, newColumns);
    rs.addRow(newRow);
    return rs;
  }

  public static BeeRowSet createRowSetForInsert(BeeRowSet input) {
    if (input == null) {
      return null;
    }

    List<BeeColumn> newColumns = new ArrayList<>();
    List<Integer> indexes = new ArrayList<>();

    for (int i = 0; i < input.getNumberOfColumns(); i++) {
      BeeColumn column = input.getColumn(i);

      if (column.isEditable()) {
        newColumns.add(column);
        indexes.add(i);
      }
    }

    if (newColumns.isEmpty()) {
      return null;
    }

    BeeRowSet result = new BeeRowSet(input.getViewName(), newColumns);

    for (BeeRow oldRow : input) {
      List<String> values = new ArrayList<>();
      for (int index : indexes) {
        values.add(oldRow.getString(index));
      }

      BeeRow newRow = new BeeRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION, values);
      result.addRow(newRow);
    }

    return result;
  }

  public static String defaultColumnId(int index) {
    if (BeeUtils.betweenExclusive(index, 0, 1000)) {
      return "col" + BeeUtils.toLeadingZeroes(index, 3);
    } else {
      return "col" + index;
    }
  }

  public static String defaultColumnLabel(int index) {
    return "Column " + index;
  }

  public static BeeRowSet emptyToNull(BeeRowSet rowSet) {
    return isEmpty(rowSet) ? null : rowSet;
  }

  public static List<BeeRow> filterRows(Collection<BeeRow> rows, Collection<Long> ids) {
    List<BeeRow> result = new ArrayList<>();

    for (BeeRow row : rows) {
      if (ids.contains(row.getId())) {
        result.add(row);
      }
    }
    return result;
  }

  public static List<BeeRow> filterRows(BeeRowSet rowSet, String columnId, Long value) {
    List<BeeRow> result = new ArrayList<>();
    int index = rowSet.getColumnIndex(columnId);

    for (BeeRow row : rowSet) {
      if (Objects.equals(row.getLong(index), value)) {
        result.add(row);
      }
    }
    return result;
  }

  public static List<BeeRow> filterRows(BeeRowSet rowSet, String columnId, String value) {
    List<BeeRow> result = new ArrayList<>();
    int index = rowSet.getColumnIndex(columnId);

    for (BeeRow row : rowSet) {
      if (BeeUtils.equalsTrim(row.getString(index), value)) {
        result.add(row);
      }
    }
    return result;
  }

  public static Boolean getBoolean(BeeRowSet rowSet, IsRow row, String columnId) {
    return getBoolean(rowSet.getColumns(), row, columnId);
  }

  public static Boolean getBoolean(List<? extends IsColumn> columns, IsRow row, String columnId) {
    return row.getBoolean(getColumnIndex(columnId, columns));
  }

  public static <T extends IsColumn> T getColumn(String columnId, List<T> columns) {
    int index = getColumnIndex(columnId, columns);

    if (index >= 0) {
      return columns.get(index);
    } else {
      return null;
    }
  }

  /**
   * Finds column place in the list.
   *
   * @param columnId the name of target column
   * @param columns a list of columns
   * @return an index of target column in columns list
   */
  public static int getColumnIndex(String columnId, List<? extends IsColumn> columns) {
    return getColumnIndex(columnId, columns, false);
  }

  /**
   * Finds column place in the list with the warning.
   *
   * @param columnId the name of target column
   * @param columns a list of columns
   * @param warn value to show or not the warning
   * @return an index of target column in columns list
   */
  public static int getColumnIndex(String columnId, List<? extends IsColumn> columns,
      boolean warn) {

    if (columns != null) {
      for (int i = 0; i < columns.size(); i++) {
        if (BeeUtils.same(columns.get(i).getId(), columnId)) {
          return i;
        }
      }
    }

    if (warn) {
      logger.warning("column not found", columnId);
    }
    return BeeConst.UNDEF;
  }

  public static String getColumnName(String input, List<? extends IsColumn> columns,
      String idColumnName, String versionColumnName) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    if (!BeeUtils.isEmpty(columns)) {
      IsColumn column = getColumn(input, columns);

      if (column != null) {
        return column.getId();
      }
    }

    if (BeeUtils.same(input, idColumnName)) {
      return idColumnName;
    }
    if (BeeUtils.same(input, versionColumnName)) {
      return versionColumnName;
    }
    return null;
  }

  public static List<String> getColumnNames(List<? extends IsColumn> columns) {
    return getColumnNames(columns, null, null);
  }

  public static List<String> getColumnNames(List<? extends IsColumn> columns,
      String idColumnName, String versionColumnName) {
    List<String> names = new ArrayList<>();

    if (!BeeUtils.isEmpty(columns)) {
      for (IsColumn column : columns) {
        names.add(column.getId());
      }
    }

    if (!BeeUtils.isEmpty(idColumnName)) {
      names.add(idColumnName);
    }
    if (!BeeUtils.isEmpty(versionColumnName)) {
      names.add(versionColumnName);
    }
    return names;
  }

  public static int getColumnPrecision(String columnId, List<? extends IsColumn> columns) {
    IsColumn column = getColumn(columnId, columns);
    return (column == null) ? BeeConst.UNDEF : column.getPrecision();
  }

  public static List<BeeColumn> getColumns(List<BeeColumn> columns, int... indexes) {
    if (indexes == null) {
      return columns;
    }

    List<BeeColumn> result = new ArrayList<>();
    for (int index : indexes) {
      if (index >= 0 && index < columns.size()) {
        result.add(columns.get(index));
      }
    }
    return result;
  }

  public static List<BeeColumn> getColumns(List<BeeColumn> columns, List<String> input) {
    List<BeeColumn> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(input)) {
      for (String s : input) {
        BeeColumn column = getColumn(s, columns);
        if (column != null) {
          result.add(column);
        }
      }
    }
    return result;
  }

  public static ValueType getColumnType(String columnId, List<? extends IsColumn> columns) {
    ValueType type = null;
    IsColumn column = getColumn(columnId, columns);

    if (column != null) {
      type = column.getType();
    }
    return type;
  }

  public static JustDate getDate(BeeRowSet rowSet, IsRow row, String columnId) {
    return getDate(rowSet.getColumns(), row, columnId);
  }

  public static JustDate getDate(List<? extends IsColumn> columns, IsRow row, String columnId) {
    return row.getDate(getColumnIndex(columnId, columns));
  }

  public static DateTime getDateTime(BeeRowSet rowSet, IsRow row, String columnId) {
    return getDateTime(rowSet.getColumns(), row, columnId);
  }

  public static DateTime getDateTime(List<? extends IsColumn> columns, IsRow row, String columnId) {
    return row.getDateTime(getColumnIndex(columnId, columns));
  }

  public static DateTime getDateTimeQuietly(IsRow row, int index) {
    if (row == null) {
      return null;

    } else if (index == VERSION_INDEX) {
      return new DateTime(row.getVersion());

    } else if (row.isIndex(index)) {
      return row.getDateTime(index);

    } else {
      return null;
    }
  }

  public static List<Long> getDistinct(BeeRowSet rowSet, String columnId) {
    return getDistinct(rowSet.getRows(), rowSet.getColumnIndex(columnId));
  }

  public static List<Long> getDistinct(Collection<? extends IsRow> rows, int index) {
    return getDistinct(rows, index, null);
  }

  public static List<Long> getDistinct(Collection<? extends IsRow> rows, int index, Long exclude) {
    List<Long> result = new ArrayList<>();
    if (BeeUtils.isEmpty(rows)) {
      return result;
    }

    for (IsRow row : rows) {
      Long value = row.getLong(index);
      if (value != null && !value.equals(exclude) && !result.contains(value)) {
        result.add(value);
      }
    }
    return result;
  }

  public static Double getDouble(BeeRowSet rowSet, IsRow row, String columnId) {
    return getDouble(rowSet.getColumns(), row, columnId);
  }

  public static Double getDouble(List<? extends IsColumn> columns, IsRow row, String columnId) {
    return row.getDouble(getColumnIndex(columnId, columns));
  }

  public static Double getDoubleQuietly(IsRow row, int index) {
    if (row == null) {
      return null;

    } else if (index == ID_INDEX) {
      return (double) row.getId();

    } else if (index == VERSION_INDEX) {
      return (double) row.getVersion();

    } else if (row.isIndex(index)) {
      return row.getDouble(index);

    } else {
      return null;
    }
  }

  public static long getId(IsRow row) {
    return (row == null) ? BeeConst.LONG_UNDEF : row.getId();
  }

  public static Set<Long> getIdSetDifference(String s1, String s2) {
    Set<Long> difference = parseIdSet(s1);
    if (!difference.isEmpty() || !BeeUtils.isEmpty(s2)) {
      difference.removeAll(parseIdSet(s2));
    }
    return difference;
  }

  public static Integer getInteger(BeeRowSet rowSet, IsRow row, String columnId) {
    return getInteger(rowSet.getColumns(), row, columnId);
  }

  public static Integer getInteger(List<? extends IsColumn> columns, IsRow row, String columnId) {
    return row.getInteger(getColumnIndex(columnId, columns));
  }

  public static Integer getIntegerQuietly(IsRow row, int index) {
    if (row == null) {
      return null;

    } else if (row.isIndex(index)) {
      return row.getInteger(index);

    } else {
      return null;
    }
  }

  public static Long getLong(BeeRowSet rowSet, IsRow row, String columnId) {
    return getLong(rowSet.getColumns(), row, columnId);
  }

  public static Long getLong(List<? extends IsColumn> columns, IsRow row, String columnId) {
    return row.getLong(getColumnIndex(columnId, columns));
  }

  public static Long getLongQuietly(List<? extends IsColumn> columns, IsRow row, String columnId) {
    return getLongQuietly(row, getColumnIndex(columnId, columns));
  }

  public static Long getLongQuietly(IsRow row, int index) {
    if (row == null) {
      return null;

    } else if (index == ID_INDEX) {
      return row.getId();

    } else if (index == VERSION_INDEX) {
      return row.getVersion();

    } else if (row.isIndex(index)) {
      return row.getLong(index);

    } else {
      return null;
    }
  }

  public static int getMaxInitialRowSetSize() {
    return maxInitialRowSetSize;
  }

  public static String getMaxValue(IsColumn column) {
    if (column == null) {
      return null;
    }

    ValueType type = column.getType();
    if (type == null) {
      return null;
    }

    String value;
    switch (type) {
      case INTEGER:
        value = BeeUtils.toString(Integer.MAX_VALUE);
        break;

      case LONG:
        value = BeeUtils.toString(Long.MAX_VALUE);
        break;

      case DECIMAL:
        int precision = column.getPrecision();
        int scale = column.getScale();

        if (precision > 0 && scale <= 0) {
          value = BeeUtils.toString(Math.pow(10, precision) - 1);
        } else if (precision > 0 && scale < precision) {
          value = BeeUtils.toString(Math.pow(10, precision - scale) - Math.pow(10, -scale));
        } else {
          value = null;
        }
        break;

      default:
        value = null;
    }
    return value;
  }

  public static String getMinValue(IsColumn column) {
    if (column == null) {
      return null;
    }

    ValueType type = column.getType();
    if (type == null) {
      return null;
    }

    String value;
    switch (type) {
      case INTEGER:
        value = BeeUtils.toString(Integer.MIN_VALUE);
        break;

      case LONG:
        value = BeeUtils.toString(Long.MIN_VALUE);
        break;

      case DECIMAL:
        int precision = column.getPrecision();
        int scale = column.getScale();

        if (precision > 0 && scale <= 0) {
          value = BeeUtils.toString(-Math.pow(10, precision) + 1);
        } else if (precision > 0 && scale < precision) {
          value = BeeUtils.toString(-Math.pow(10, precision - scale) + Math.pow(10, -scale));
        } else {
          value = null;
        }
        break;

      default:
        value = null;
    }
    return value;
  }

  public static int getNumberOfRows(BeeRowSet rowSet) {
    return (rowSet == null) ? 0 : rowSet.getNumberOfRows();
  }

  public static String getRowCaption(DataInfo dataInfo, IsRow row,
      Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    Assert.notNull(dataInfo);
    if (row == null) {
      return null;
    }

    List<String> colNames = new ArrayList<>();
    if (!BeeUtils.isEmpty(dataInfo.getRowCaption())) {
      colNames.addAll(dataInfo.parseColumns(dataInfo.getRowCaption()));
    }

    if (colNames.isEmpty()) {
      for (BeeColumn column : dataInfo.getColumns()) {
        if (column.isCharacter() && column.isEditable() && !column.isNullable()) {
          colNames.add(column.getId());
        }
      }
    }

    if (colNames.isEmpty()) {
      return null;
    } else {
      return join(dataInfo, row, colNames, BeeConst.STRING_SPACE, dateRenderer, dateTimeRenderer);
    }
  }

  public static List<Long> getRowIds(BeeRowSet rowSet) {
    List<Long> result = new ArrayList<>();
    if (!isEmpty(rowSet)) {
      for (BeeRow row : rowSet.getRows()) {
        result.add(row.getId());
      }
    }
    return result;
  }

  public static List<Long> getRowIds(Collection<? extends IsRow> rows) {
    List<Long> result = new ArrayList<>();
    if (!BeeUtils.isEmpty(rows)) {
      for (IsRow row : rows) {
        if (hasId(row)) {
          result.add(row.getId());
        }
      }
    }
    return result;
  }

  public static String getString(BeeRowSet rowSet, IsRow row, String columnId) {
    return getString(rowSet.getColumns(), row, columnId);
  }

  public static String getString(List<? extends IsColumn> columns, IsRow row, String columnId) {
    return row.getString(getColumnIndex(columnId, columns));
  }

  public static String getStringQuietly(List<? extends IsColumn> columns, IsRow row,
      String columnId) {

    return getStringQuietly(row, getColumnIndex(columnId, columns));
  }

  public static String getStringQuietly(IsRow row, int index) {
    if (row == null) {
      return null;

    } else if (index == ID_INDEX) {
      return BeeUtils.toString(row.getId());

    } else if (index == VERSION_INDEX) {
      return BeeUtils.toString(row.getVersion());

    } else if (row.isIndex(index)) {
      return row.getString(index);

    } else {
      return null;
    }
  }

  public static String getTranslation(BeeRowSet rowSet, IsRow row, String columnId,
      String language) {
    return getTranslation(rowSet.getColumns(), row, columnId, language);
  }

  public static String getTranslation(List<? extends IsColumn> columns, IsRow row, String columnId,
      String language) {

    if (!BeeUtils.isEmpty(language)) {
      int index = getColumnIndex(Localized.column(columnId, language), columns);

      if (!BeeConst.isUndef(index)) {
        String value = row.getString(index);
        if (!BeeUtils.isEmpty(value)) {
          return value;
        }
      }
    }

    return getString(columns, row, columnId);
  }

  public static BeeRowSet getUpdated(String viewName, List<BeeColumn> columns, IsRow oldRow,
      IsRow newRow, Collection<RowChildren> children) {

    Assert.notNull(oldRow);
    Assert.notNull(newRow);

    List<String> oldValues = new ArrayList<>();
    List<String> newValues = new ArrayList<>();

    for (int i = 0; i < oldRow.getNumberOfCells(); i++) {
      oldValues.add(oldRow.getString(i));
    }
    for (int i = 0; i < newRow.getNumberOfCells(); i++) {
      newValues.add(newRow.getString(i));
    }
    return getUpdated(viewName, oldRow.getId(), oldRow.getVersion(),
        columns, oldValues, newValues, children);
  }

  public static BeeRowSet getUpdated(String viewName, long rowId, long rowVersion,
      BeeColumn column, String oldValue, String newValue) {

    Assert.notNull(column);

    return getUpdated(viewName, rowId, rowVersion, Collections.singletonList(column),
        Collections.singletonList(oldValue), Collections.singletonList(newValue), null);
  }

  public static BeeRowSet getUpdated(String viewName, long rowId, long rowVersion,
      List<BeeColumn> columns, List<String> oldValues, List<String> newValues,
      Collection<RowChildren> children) {

    if (BeeUtils.isEmpty(columns)) {
      return null;
    }

    Assert.notEmpty(viewName);
    Assert.notNull(oldValues);
    Assert.notNull(newValues);

    int cc = columns.size();
    Assert.isTrue(cc == oldValues.size());
    Assert.isTrue(cc == newValues.size());

    List<BeeColumn> updatedColumns = new ArrayList<>();
    List<String> updatedOldValues = new ArrayList<>();
    List<String> updatedNewValues = new ArrayList<>();

    for (int i = 0; i < cc; i++) {
      BeeColumn column = columns.get(i);
      if (!column.isEditable()) {
        continue;
      }
      String oldValue = oldValues.get(i);
      String newValue = newValues.get(i);

      if (!BeeUtils.equalsTrimRight(oldValue, newValue)) {
        updatedColumns.add(column);
        updatedOldValues.add(oldValue);
        updatedNewValues.add(newValue);
      }
    }
    if (updatedColumns.isEmpty()) {
      return null;
    }

    BeeRowSet rowSet = new BeeRowSet(viewName, updatedColumns);
    rowSet.addRow(rowId, rowVersion, updatedOldValues);

    for (int i = 0; i < updatedColumns.size(); i++) {
      rowSet.getRow(0).preliminaryUpdate(i, updatedNewValues.get(i));
    }
    if (!BeeUtils.isEmpty(children)) {
      rowSet.getRow(0).setChildren(children);
    }
    return rowSet;
  }

  /**
   * Checks if row has id.
   *
   * @param row data entry
   * @return true if a row has id value, otherwise false.
   */
  public static boolean hasId(IsRow row) {
    return row != null && isId(row.getId());
  }

  /**
   * Checks if the id of the row and specific id are equal.
   *
   * @param row data entry
   * @param id specific id value
   * @return true if both id are equal, otherwise false.
   */
  public static boolean idEquals(IsRow row, Long id) {
    return row != null && id != null && id.equals(row.getId());
  }

  /**
   * Checks if the rowSet is empty or null.
   *
   * @param rowSet data set (BeeRowSet object)
   * @return True if rowSet is empty, otherwise false.
   */
  public static boolean isEmpty(BeeRowSet rowSet) {
    return rowSet == null || rowSet.isEmpty();
  }

  /**
   * Checks if the rowSet is empty or null.
   *
   * @param rowSet data set (SimpleRowSet object)
   * @return True if rowSet is empty, otherwise false.
   */
  public static boolean isEmpty(SimpleRowSet rowSet) {
    return rowSet == null || rowSet.isEmpty();
  }

  /**
   * Checks if the id is truly correct.
   *
   * @param id target id value (Long object)
   * @return True if the {@code id} is positive, otherwise false.
   */
  public static boolean isId(Long id) {
    return id != null && id > 0;
  }

  /**
   * Checks if the id is truly correct.
   *
   * @param s target String value
   * @return True if the {@code s} is positive, otherwise false.
   */
  public static boolean isId(String s) {
    return s != null && BeeUtils.isDigit(s.trim()) && isId(BeeUtils.toLongOrNull(s));
  }

  /**
   * Checks if the target row is a new row.
   *
   * @param row data entry
   * @return True if id of the {@code row} equals 0, otherwise false.
   */
  public static boolean isNewRow(IsRow row) {
    return row != null && row.getId() == NEW_ROW_ID;
  }

  /**
   * Checks if the value at {@code row} {@code columnId} is {@code null}.
   *
   * @param rowSet data set (BeeRowSet object)
   * @param row data entry
   * @param columnId the name of target column
   * @return True if the cell at {@code row} {@code columnId} equals {@code null}, otherwise false.
   */
  public static boolean isNull(BeeRowSet rowSet, IsRow row, String columnId) {
    return row.isNull(getColumnIndex(columnId, rowSet.getColumns()));
  }

  public static String join(DataInfo dataInfo, IsRow row, List<String> colNames, String separator,
      Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    Assert.notNull(dataInfo);
    Assert.notNull(row);
    Assert.notEmpty(colNames);

    StringBuilder sb = new StringBuilder();
    String sep = BeeUtils.nvl(separator, BeeConst.DEFAULT_LIST_SEPARATOR);

    for (String colName : colNames) {
      int i = dataInfo.getColumnIndex(colName);
      if (BeeConst.isUndef(i)) {
        logger.warning(dataInfo.getViewName(), "column not found", colName);
        continue;
      }

      String value = render((i >= 0) ? dataInfo.getColumns().get(i) : null, row, i,
          dateRenderer, dateTimeRenderer);

      if (!BeeUtils.isEmpty(value)) {
        if (sb.length() > 0) {
          sb.append(sep);
        }
        sb.append(value.trim());
      }
    }
    return sb.toString();
  }

  public static String join(DataInfo dataInfo, IsRow row, String separator,
      Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    Assert.notNull(dataInfo);
    Assert.notNull(row);

    StringBuilder sb = new StringBuilder();
    String sep = BeeUtils.nvl(separator, BeeConst.DEFAULT_LIST_SEPARATOR);

    for (int i = 0; i < dataInfo.getColumnCount(); i++) {
      BeeColumn column = dataInfo.getColumns().get(i);
      if (dataInfo.hasRelation(column.getId())) {
        continue;
      }

      String value = render(column, row, i, dateRenderer, dateTimeRenderer);
      if (!BeeUtils.isEmpty(value)) {
        if (sb.length() > 0) {
          sb.append(sep);
        }
        sb.append(value.trim());
      }
    }
    return sb.toString();
  }

  public static String join(IsRow row, List<Integer> indexes,
      Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    return join(null, row, indexes, BeeConst.DEFAULT_LIST_SEPARATOR, dateRenderer,
        dateTimeRenderer);
  }

  public static String join(List<BeeColumn> columns, IsRow row, List<Integer> indexes,
      String separator, Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    Assert.notNull(row);
    Assert.notEmpty(indexes);

    StringBuilder sb = new StringBuilder();
    String sep = BeeUtils.nvl(separator, BeeConst.DEFAULT_LIST_SEPARATOR);

    for (int index : indexes) {
      String value = render(BeeUtils.getQuietly(columns, index), row,
          index, dateRenderer, dateTimeRenderer);

      if (!BeeUtils.isEmpty(value)) {
        if (sb.length() > 0) {
          sb.append(sep);
        }
        sb.append(value.trim());
      }
    }
    return sb.toString();
  }

  public static List<String> parseColumns(List<String> input, List<? extends IsColumn> columns) {
    return parseColumns(input, columns, null, null);
  }

  public static List<String> parseColumns(List<String> input, List<? extends IsColumn> columns,
      String idName, String versionName) {

    List<String> result = new ArrayList<>();
    if (BeeUtils.isEmpty(input)) {
      return result;
    }
    Assert.notEmpty(columns);

    boolean hasWildcards = false;
    boolean hasExclusions = false;

    for (String item : input) {
      if (!hasWildcards) {
        hasWildcards = Wildcards.hasDefaultWildcards(item);
      }
      if (!hasExclusions) {
        hasExclusions = BeeUtils.isPrefixOrSuffix(item, BeeConst.CHAR_MINUS);
      }
    }

    if (hasWildcards || hasExclusions) {
      Set<Pattern> include = new HashSet<>();
      Set<Pattern> exclude = new HashSet<>();

      for (String item : input) {
        if (hasExclusions && BeeUtils.isPrefixOrSuffix(item, BeeConst.CHAR_MINUS)) {
          String expr = BeeUtils.removePrefixAndSuffix(item, BeeConst.CHAR_MINUS);
          if (!BeeUtils.isEmpty(expr)) {
            exclude.add(Wildcards.getDefaultPattern(BeeUtils.trim(expr)));
          }
        } else if (!BeeUtils.isEmpty(item)) {
          include.add(Wildcards.getDefaultPattern(BeeUtils.trim(item)));
        }
      }

      List<String> colNames = new ArrayList<>();

      if (!include.isEmpty()) {
        if (!BeeUtils.isEmpty(idName) && Wildcards.contains(include, idName)) {
          colNames.add(idName);
        }
        if (!BeeUtils.isEmpty(versionName) && Wildcards.contains(include, versionName)) {
          colNames.add(versionName);
        }

        for (IsColumn column : columns) {
          if (Wildcards.contains(include, column.getId())) {
            colNames.add(column.getId());
          }
        }

      } else if (!exclude.isEmpty()) {
        if (!BeeUtils.isEmpty(idName)) {
          colNames.add(idName);
        }
        if (!BeeUtils.isEmpty(versionName)) {
          colNames.add(versionName);
        }

        for (IsColumn column : columns) {
          colNames.add(column.getId());
        }
      }

      if (exclude.isEmpty()) {
        if (!colNames.isEmpty()) {
          result.addAll(colNames);
        }

      } else if (!colNames.isEmpty()) {
        for (String colName : colNames) {
          if (!Wildcards.contains(exclude, colName)) {
            result.add(colName);
          }
        }
      }

    } else {
      for (String item : input) {
        String colName = getColumnName(item, columns, idName, versionName);
        if (!BeeUtils.isEmpty(colName) && !result.contains(colName)) {
          result.add(colName);
        }
      }
    }

    return result;
  }

  public static List<String> parseColumns(String input, List<? extends IsColumn> columns) {
    return parseColumns(input, columns, null, null);
  }

  public static List<String> parseColumns(String input, List<? extends IsColumn> columns,
      String idColumnName, String versionColumnName) {
    if (BeeUtils.isEmpty(input)) {
      return new ArrayList<>();
    } else {
      return parseColumns(NameUtils.NAME_SPLITTER.splitToList(input), columns,
          idColumnName, versionColumnName);
    }
  }

  public static List<Long> parseIdList(String input) {
    List<Long> result = new ArrayList<>();
    if (BeeUtils.isEmpty(input)) {
      return result;
    }

    for (String s : ID_SPLITTER.split(input)) {
      Long id = BeeUtils.toLongOrNull(s);
      if (isId(id)) {
        result.add(id);
      }
    }
    return result;
  }

  public static Set<Long> parseIdSet(String input) {
    Set<Long> result = new HashSet<>();
    if (BeeUtils.isEmpty(input)) {
      return result;
    }

    for (String s : ID_SPLITTER.split(input)) {
      Long id = BeeUtils.toLongOrNull(s);
      if (isId(id)) {
        result.add(id);
      }
    }
    return result;
  }

  public static Order parseOrder(String input, DataInfo.Provider provider, String viewName) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }
    Assert.notNull(provider);

    DataInfo dataInfo = provider.getDataInfo(viewName, true);
    return (dataInfo == null) ? null : dataInfo.parseOrder(input);
  }

  public static String render(DataInfo dataInfo, IsRow row, BeeColumn column, int index,
      Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    if (dataInfo == null || row == null || column == null) {
      return null;
    }

    if (dataInfo.hasRelation(column.getId())) {
      List<String> columns = RelationUtils.getRenderColumns(dataInfo, column.getId());
      if (!columns.isEmpty()) {
        return join(dataInfo, row, columns, BeeConst.STRING_SPACE, dateRenderer, dateTimeRenderer);
      }
    }

    return render(column, row, index, dateRenderer, dateTimeRenderer);
  }

  public static String render(IsColumn column, IsRow row, int index,
      Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    if (row == null) {
      return null;

    } else if (index == ID_INDEX) {
      return BeeUtils.toString(row.getId());

    } else if (index == VERSION_INDEX) {
      return dateTimeRenderer.apply(new DateTime(row.getVersion()));

    } else if (row.isNull(index)) {
      return null;

    } else if (column == null || ValueType.isString(column.getType())) {
      return row.getString(index);

    } else if (ValueType.DATE == column.getType()) {
      return dateRenderer.apply(row.getDate(index));

    } else if (ValueType.DATE_TIME == column.getType()) {
      return dateTimeRenderer.apply(row.getDateTime(index));

    } else if (!BeeUtils.isEmpty(column.getEnumKey())) {
      return EnumUtils.getCaption(column.getEnumKey(), row.getInteger(index));

    } else {
      return row.getValue(index, column.getType()).render(dateRenderer, dateTimeRenderer);
    }
  }

  public static List<BeeRow> restoreRows(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    List<BeeRow> result = new ArrayList<>();
    String[] arr = Codec.beeDeserializeCollection(s);

    if (arr != null) {
      for (String r : arr) {
        result.add(BeeRow.restore(r));
      }
    }
    return result;
  }

  /**
   * Checks if the rows have same id.
   *
   * @param r1 first row
   * @param r2 second row
   * @return true if the rows have the same id, otherwise false.
   */
  public static boolean sameId(IsRow r1, IsRow r2) {
    return r1 != null && r2 != null && r1.getId() == r2.getId();
  }

  /**
   * Checks if the rows have same id and version.
   *
   * @param r1 first row
   * @param r2 second row
   * @return true if the rows have the same id and same version, otherwise false.
   */
  public static boolean sameIdAndVersion(IsRow r1, IsRow r2) {
    return r1 != null && r2 != null
        && r1.getId() == r2.getId() && r1.getVersion() == r2.getVersion();
  }

  public static boolean sameIdSet(String s, Collection<Long> col) {
    Set<Long> set = parseIdSet(s);
    if (col == null) {
      return set.isEmpty();
    } else {
      return set.size() == col.size() && set.containsAll(col);
    }
  }

  public static boolean sameIdSet(String s1, String s2) {
    return sameIdSet(s1, parseIdSet(s2));
  }

  public static boolean sameRows(List<? extends IsRow> c1, List<? extends IsRow> c2) {
    if (BeeUtils.isEmpty(c1)) {
      return BeeUtils.isEmpty(c2);

    } else if (BeeUtils.isEmpty(c2)) {
      return BeeUtils.isEmpty(c1);

    } else if (c1.size() == c2.size()) {
      for (int i = 0; i < c1.size(); i++) {
        IsRow r1 = c1.get(i);
        IsRow r2 = c2.get(i);

        if (r1 == null) {
          if (r2 != null) {
            return false;
          }

        } else if (!r1.deepEquals(r2)) {
          return false;
        }
      }
      return true;

    } else {
      return false;
    }
  }

  public static int setDefaults(IsRow row, Collection<String> colNames, List<BeeColumn> columns,
      Defaults defaults) {
    int result = 0;
    if (row == null || BeeUtils.isEmpty(colNames) || BeeUtils.isEmpty(columns)
        || defaults == null) {
      return result;
    }

    for (String colName : colNames) {
      int index = getColumnIndex(colName, columns);
      if (BeeConst.isUndef(index)) {
        continue;
      }

      BeeColumn column = columns.get(index);
      if (!column.hasDefaults()) {
        continue;
      }

      Object value = defaults.getValue(column.getDefaults().getA(), column.getDefaults().getB());
      if (value == null) {
        continue;
      }

      row.setValue(index, Value.getValue(value));
      result++;
    }
    return result;
  }

  public static void setValue(BeeRowSet rowSet, IsRow row, String columnId, String value) {
    row.setValue(getColumnIndex(columnId, rowSet.getColumns()), value);
  }

  public static List<String> translate(List<String> input, List<? extends IsColumn> columns,
      IsRow row) {
    List<String> result = new ArrayList<>();

    if (BeeUtils.isEmpty(input) || BeeUtils.isEmpty(columns)) {
      BeeUtils.overwrite(result, input);
      return result;
    }

    for (String expr : input) {
      int index = getColumnIndex(expr, columns);

      if (BeeConst.isUndef(index)) {
        result.add(expr);
      } else if (row == null) {
        result.add(BeeConst.STRING_EMPTY);
      } else {
        result.add(row.getString(index));
      }
    }
    return result;
  }

  public static void updateRow(IsRow target, IsRow source) {
    Assert.notNull(target);
    Assert.notNull(source);

    target.setId(source.getId());
    target.setVersion(source.getVersion());

    for (int i = 0; i < target.getNumberOfCells(); i++) {
      target.setValue(i, source.getString(i));
    }
  }

  private DataUtils() {
  }
}
