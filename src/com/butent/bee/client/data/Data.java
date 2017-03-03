package com.butent.bee.client.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.xml.client.Document;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.ColumnMapper;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class Data {

  private static final BeeLogger logger = LogUtils.getLogger(Data.class);

  private static final DataInfoProvider DATA_INFO_PROVIDER = new DataInfoProvider();

  private static final ColumnMapper COLUMN_MAPPER = new ColumnMapper(DATA_INFO_PROVIDER);

  private static final Set<String> visibleViews = new HashSet<>();
  private static final Set<String> hiddenViews = new HashSet<>();

  private static final Set<String> editableViews = new HashSet<>();
  private static final Set<String> readOnlyViews = new HashSet<>();

  private static final Multimap<String, String> readOnlyColumns = HashMultimap.create();

  private static final Map<String, Integer> approximateSizes = new HashMap<>();
  private static final Multimap<String, Consumer<Integer>> sizeConsumers = HashMultimap.create();

  public static String clamp(String viewName, String colName, String value) {
    if (BeeUtils.isEmpty(value)) {
      return null;
    } else {
      Integer precision = getColumnPrecision(viewName, colName);
      if (BeeUtils.isPositive(precision) && value.length() > precision) {
        return BeeUtils.left(value.trim(), precision);
      } else {
        return value;
      }
    }
  }

  public static void clearApproximateSizes() {
    approximateSizes.clear();
  }

  public static void clearCell(String viewName, IsRow row, String colName) {
    COLUMN_MAPPER.clearCell(viewName, row, colName);
  }

  public static boolean containsColumn(String viewName, String colName) {
    DataInfo dataInfo = getDataInfo(viewName);
    return dataInfo != null && dataInfo.containsColumn(colName);
  }

  public static BeeRowSet createRowSet(String viewName) {
    return new BeeRowSet(viewName, new ArrayList<>(getColumns(viewName)));
  }

  public static boolean equals(String viewName, IsRow row, String colName, Long value) {
    return Objects.equals(getLong(viewName, row, colName), value);
  }

  public static void estimateSize(final String viewName, Consumer<Integer> consumer) {
    Assert.notEmpty(viewName);
    Assert.notNull(consumer);

    Integer size = approximateSizes.get(viewName);

    if (size == null) {
      boolean pending = sizeConsumers.containsKey(viewName);
      sizeConsumers.put(viewName, consumer);

      if (!pending) {
        Queries.getRowCount(viewName, null, new Queries.IntCallback() {
          @Override
          public void onFailure(String... reason) {
            consumeSize(BeeConst.UNDEF);
            super.onFailure(reason);
          }

          @Override
          public void onSuccess(Integer result) {
            approximateSizes.put(viewName, result);
            consumeSize(result);
          }

          private void consumeSize(Integer result) {
            Collection<Consumer<Integer>> consumers = sizeConsumers.removeAll(viewName);

            if (!BeeUtils.isEmpty(consumers)) {
              for (Consumer<Integer> c : consumers) {
                c.accept(result);
              }
            }
          }
        });
      }

    } else {
      consumer.accept(size);
    }
  }

  public static Map<String, Integer> getApproximateSizes() {
    return approximateSizes;
  }

  public static Boolean getBoolean(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.getBoolean(viewName, row, colName);
  }

  public static BeeColumn getColumn(String viewName, String colName) {
    BeeColumn column = getDataInfo(viewName).getColumn(colName);
    if (column == null) {
      logger.severe(viewName, "column", colName, "not found");
    }
    return column;
  }

  public static int getColumnIndex(String viewName, String colName) {
    return COLUMN_MAPPER.getIndex(viewName, colName);
  }

  public static String getColumnLabel(String viewName, String colName) {
    return Localized.getLabel(getColumn(viewName, colName));
  }

  public static List<String> getColumnLabels(String viewName, List<String> colNames) {
    List<String> result = new ArrayList<>();

    for (BeeColumn column : getColumns(viewName, colNames)) {
      result.add(Localized.getLabel(column));
    }

    return result;
  }

  public static ColumnMapper getColumnMapper() {
    return COLUMN_MAPPER;
  }

  public static Integer getColumnPrecision(String viewName, String colName) {
    return getDataInfo(viewName).getColumnPrecision(colName);
  }

  public static String getColumnRelation(String viewName, String colName) {
    return getDataInfo(viewName).getRelation(colName);
  }

  public static List<BeeColumn> getColumns(String viewName) {
    return getDataInfo(viewName).getColumns();
  }

  public static List<BeeColumn> getColumns(String viewName, List<String> colNames) {
    List<BeeColumn> result = new ArrayList<>();
    DataInfo dataInfo = getDataInfo(viewName);

    for (String colName : colNames) {
      BeeColumn column = dataInfo.getColumn(colName);
      if (column == null) {
        logger.severe(viewName, "column", colName, "not found");
      } else {
        result.add(column);
      }
    }
    return result;
  }

  public static List<BeeColumn> getColumns(String viewName, String col1, String col2) {
    return getColumns(viewName, Arrays.asList(col1, col2));
  }

  public static Integer getColumnScale(String viewName, String colName) {
    return getDataInfo(viewName).getColumnScale(colName);
  }

  public static ValueType getColumnType(String viewName, String colName) {
    return getDataInfo(viewName).getColumnType(colName);
  }

  public static DataInfo getDataInfo(String viewName) {
    return getDataInfo(viewName, true);
  }

  public static DataInfo getDataInfo(String viewName, boolean warn) {
    return DATA_INFO_PROVIDER.getDataInfo(viewName, warn);
  }

  public static DataInfoProvider getDataInfoProvider() {
    return DATA_INFO_PROVIDER;
  }

  public static JustDate getDate(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.getDate(viewName, row, colName);
  }

  public static DateTime getDateTime(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.getDateTime(viewName, row, colName);
  }

  public static BigDecimal getDecimal(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.getDecimal(viewName, row, colName);
  }

  public static Double getDouble(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.getDouble(viewName, row, colName);
  }

  public static <E extends Enum<?>> E getEnum(String viewName, IsRow row, String colName,
      Class<E> clazz) {

    return EnumUtils.getEnumByIndex(clazz, getInteger(viewName, row, colName));
  }

  public static String getIdColumn(String viewName) {
    return getDataInfo(viewName).getIdColumn();
  }

  public static Integer getInteger(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.getInteger(viewName, row, colName);
  }

  public static Long getLong(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.getLong(viewName, row, colName);
  }

  public static Relation getRelation(String viewName) {
    Relation relation = null;
    String relationInfo = getDataInfo(viewName).getRelationInfo();

    if (!BeeUtils.isEmpty(relationInfo)) {
      Document doc = XmlUtils.parse(relationInfo);

      if (doc != null) {
        Map<String, String> attributes = XmlUtils.getAttributes(doc.getDocumentElement());
        attributes.put(UiConstants.ATTR_VIEW_NAME, viewName);
        relation = FormWidget.createRelation(null, attributes,
            XmlUtils.getChildrenElements(doc.getDocumentElement()), Relation.RenderMode.SOURCE);
      }
    }
    return relation;
  }

  public static String getString(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.getString(viewName, row, colName);
  }

  public static String getViewCaption(String viewName) {
    DataInfo dataInfo = getDataInfo(viewName);
    return BeeUtils.notEmpty(Localized.maybeTranslate(dataInfo.getCaption()), viewName);
  }

  public static String getViewTable(String viewName) {
    DataInfo dataInfo = getDataInfo(viewName);
    return (dataInfo == null) ? null : dataInfo.getTableName();
  }

  public static boolean isColumnReadOnly(String viewName, BeeColumn column) {
    return column.isReadOnly() || readOnlyColumns.containsEntry(viewName, column.getId())
        || !BeeKeeper.getUser().canEditColumn(viewName, column.getId());
  }

  public static boolean isNull(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.isNull(viewName, row, colName);
  }

  public static boolean isTrue(String viewName, IsRow row, String colName) {
    return COLUMN_MAPPER.isTrue(viewName, row, colName);
  }

  public static boolean isViewEditable(String viewName) {
    if (BeeUtils.isEmpty(viewName) || !BeeKeeper.getUser().canEditData(viewName)) {
      return false;
    } else if (!editableViews.isEmpty()) {
      return editableViews.contains(viewName);
    } else if (readOnlyViews.isEmpty()) {
      return true;
    } else {
      return !readOnlyViews.contains(viewName);
    }
  }

  public static boolean isViewVisible(String viewName) {
    if (BeeUtils.isEmpty(viewName) || !BeeKeeper.getUser().isDataVisible(viewName)) {
      return false;
    } else if (!visibleViews.isEmpty()) {
      return visibleViews.contains(viewName);
    } else if (hiddenViews.isEmpty()) {
      return true;
    } else {
      return !hiddenViews.contains(viewName);
    }
  }

  public static void onTableChange(String tableName, EnumSet<DataChangeEvent.Effect> effects) {
    Collection<String> viewNames = DATA_INFO_PROVIDER.getViewNames(tableName);
    for (String viewName : viewNames) {
      DataChangeEvent.fire(BeeKeeper.getBus(), viewName, effects);
    }
  }

  public static void onViewChange(String viewName, EnumSet<DataChangeEvent.Effect> effects) {
    onTableChange(getDataInfo(viewName).getTableName(), effects);
  }

  public static Double round(String viewName, String colName, Double value) {
    if (BeeUtils.nonZero(value)) {
      Integer scale = getColumnScale(viewName, colName);
      if (BeeUtils.isNonNegative(scale)) {
        return BeeUtils.round(value, scale);
      }
    }
    return value;
  }

  public static boolean sameTable(String v1, String v2) {
    String t1 = getViewTable(v1);
    if (BeeUtils.isEmpty(t1)) {
      return false;
    } else {
      return v1.equals(v2) || t1.equals(getViewTable(v2));
    }
  }

  public static void setColumnReadOnly(String viewName, String colName) {
    Assert.notEmpty(viewName);
    Assert.notEmpty(colName);

    readOnlyColumns.put(viewName, colName);
  }

  public static void setEditableViews(Collection<String> views) {
    BeeUtils.overwrite(editableViews, views);
  }

  public static void setHiddenViews(Collection<String> views) {
    BeeUtils.overwrite(hiddenViews, views);
  }

  public static void setReadOnlyViews(Collection<String> views) {
    BeeUtils.overwrite(readOnlyViews, views);
  }

  public static void setValue(String viewName, IsRow row, String colName, BigDecimal value) {
    COLUMN_MAPPER.setValue(viewName, row, colName, value);
  }

  public static void setValue(String viewName, IsRow row, String colName, Boolean value) {
    COLUMN_MAPPER.setValue(viewName, row, colName, value);
  }

  public static void setValue(String viewName, IsRow row, String colName, DateTime value) {
    COLUMN_MAPPER.setValue(viewName, row, colName, value);
  }

  public static void setValue(String viewName, IsRow row, String colName, Double value) {
    COLUMN_MAPPER.setValue(viewName, row, colName, value);
  }

  public static void setValue(String viewName, IsRow row, String colName, Integer value) {
    COLUMN_MAPPER.setValue(viewName, row, colName, value);
  }

  public static void setValue(String viewName, IsRow row, String colName, JustDate value) {
    COLUMN_MAPPER.setValue(viewName, row, colName, value);
  }

  public static void setValue(String viewName, IsRow row, String colName, Long value) {
    COLUMN_MAPPER.setValue(viewName, row, colName, value);
  }

  public static void setValue(String viewName, IsRow row, String colName, String value) {
    COLUMN_MAPPER.setValue(viewName, row, colName, value);
  }

  public static void setVisibleViews(Collection<String> views) {
    BeeUtils.overwrite(visibleViews, views);
  }

  public static void squeezeValue(String viewName, IsRow row, String colName, String value) {
    COLUMN_MAPPER.setValue(viewName, row, colName, clamp(viewName, colName, value));
  }

  private Data() {
    super();
  }
}
