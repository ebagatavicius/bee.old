package com.butent.bee.shared.data.view;

import com.google.common.collect.ImmutableList;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColumnMapper implements HasExtendedInfo {

  private static final BeeLogger logger = LogUtils.getLogger(ColumnMapper.class);

  private final ColumnNamesProvider columnListProvider;

  private final Map<String, ImmutableList<String>> map = new HashMap<>();

  public ColumnMapper(ColumnNamesProvider columnListProvider) {
    super();
    this.columnListProvider = Assert.notNull(columnListProvider);
  }

  public void clear() {
    map.clear();
  }

  public void clearCell(String viewName, IsRow row, String colName) {
    row.clearCell(getIndex(viewName, colName));
  }

  public boolean contains(String viewName) {
    return map.containsKey(viewName);
  }

  public boolean contains(String viewName, String colName) {
    ImmutableList<String> columnNames = map.get(viewName);
    return (columnNames == null) ? null : columnNames.contains(colName);
  }

  public Boolean getBoolean(String viewName, IsRow row, String colName) {
    return row.getBoolean(getIndex(viewName, colName));
  }

  public JustDate getDate(String viewName, IsRow row, String colName) {
    return row.getDate(getIndex(viewName, colName));
  }

  public DateTime getDateTime(String viewName, IsRow row, String colName) {
    return row.getDateTime(getIndex(viewName, colName));
  }

  public BigDecimal getDecimal(String viewName, IsRow row, String colName) {
    return row.getDecimal(getIndex(viewName, colName));
  }

  public Double getDouble(String viewName, IsRow row, String colName) {
    return row.getDouble(getIndex(viewName, colName));
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = new ArrayList<>();
    info.add(new ExtendedProperty("Views", BeeUtils.bracket(map.size())));

    int idx = 0;
    for (Map.Entry<String, ImmutableList<String>> entry : map.entrySet()) {
      String viewName = entry.getKey();
      ImmutableList<String> columnNames = entry.getValue();

      info.add(new ExtendedProperty(viewName, BeeUtils.progress(++idx, map.size()),
          BeeUtils.bracket(columnNames.size())));
      for (int i = 0; i < columnNames.size(); i++) {
        info.add(new ExtendedProperty(viewName, columnNames.get(i), BeeUtils.toString(i)));
      }
    }
    return info;
  }

  public int getIndex(String viewName, String colName) {
    ImmutableList<String> columnNames = map.get(viewName);

    if (columnNames == null) {
      columnNames = columnListProvider.getColumnNames(viewName);
      if (BeeUtils.isEmpty(columnNames)) {
        logError("view", viewName, "columns not available");
        return BeeConst.UNDEF;
      }
      map.put(viewName, columnNames);
    }

    int index = columnNames.indexOf(colName);
    if (index < 0) {
      logError("view", viewName, "column", colName, "not found");
    }
    return index;
  }

  public Integer getInteger(String viewName, IsRow row, String colName) {
    return row.getInteger(getIndex(viewName, colName));
  }

  public Long getLong(String viewName, IsRow row, String colName) {
    return row.getLong(getIndex(viewName, colName));
  }

  public String getString(String viewName, IsRow row, String colName) {
    return row.getString(getIndex(viewName, colName));
  }

  public boolean isNull(String viewName, IsRow row, String colName) {
    return row.isNull(getIndex(viewName, colName));
  }

  public boolean isTrue(String viewName, IsRow row, String colName) {
    return row.isTrue(getIndex(viewName, colName));
  }

  public ImmutableList<String> remove(String viewName) {
    return map.remove(viewName);
  }

  public void setValue(String viewName, IsRow row, String colName, BigDecimal value) {
    row.setValue(getIndex(viewName, colName), value);
  }

  public void setValue(String viewName, IsRow row, String colName, Boolean value) {
    row.setValue(getIndex(viewName, colName), value);
  }

  public void setValue(String viewName, IsRow row, String colName, DateTime value) {
    row.setValue(getIndex(viewName, colName), value);
  }

  public void setValue(String viewName, IsRow row, String colName, Double value) {
    row.setValue(getIndex(viewName, colName), value);
  }

  public void setValue(String viewName, IsRow row, String colName, Integer value) {
    row.setValue(getIndex(viewName, colName), value);
  }

  public void setValue(String viewName, IsRow row, String colName, JustDate value) {
    row.setValue(getIndex(viewName, colName), value);
  }

  public void setValue(String viewName, IsRow row, String colName, Long value) {
    row.setValue(getIndex(viewName, colName), value);
  }

  public void setValue(String viewName, IsRow row, String colName, String value) {
    row.setValue(getIndex(viewName, colName), value);
  }

  private void logError(Object... obj) {
    logger.severe(NameUtils.getName(this));
    logger.severe(obj);
  }
}
