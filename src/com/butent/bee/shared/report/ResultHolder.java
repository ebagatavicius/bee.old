package com.butent.bee.shared.report;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeMultimap;

import com.butent.bee.client.output.ReportItem;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class ResultHolder implements BeeSerializable {

  private enum Serial {
    ROW_GROUPS, COL_GROUPS, VALUES
  }

  public enum ResultLevel implements HasLocalizedCaption {
    CELL {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.resultLevelCell();
      }
    },
    COL {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.resultLevelCol();
      }
    },
    GROUP {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.resultLevelGroup();
      }
    },
    GROUP_COL {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.resultLevelGroupCol();
      }
    },
    ROW {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.resultLevelRow();
      }
    },
    TOTAL {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.resultLevelTotal();
      }
    }
  }

  private final Multimap<ResultValue, ResultValue> rowGroups = TreeMultimap.create();
  private final Set<ResultValue> colGroups = new TreeSet<>();
  private final Table<ResultValue, String, Object> values = HashBasedTable.create();

  public void addValues(ResultValue rowGroup, ResultValue[] rowValues, ResultValue colGroup,
      ReportInfoItem infoItem, ResultValue value) {

    ResultValue rowValue = ResultValue.of(rowValues);

    ResultValue key = getKey(ResultLevel.CELL, rowGroup, rowValue, colGroup);
    ReportItem item = infoItem.getItem();
    String col = item.getName();
    Object calc = item.calculate(getValue(key, col), value, infoItem.getFunction());

    if (calc == null) {
      return;
    }
    if (!rowGroups.containsEntry(rowGroup, rowValue)) {
      rowGroups.put(rowGroup, rowValue);
    }
    if (!colGroups.contains(colGroup)) {
      colGroups.add(colGroup);
    }
    putValue(key, col, calc);

    if (infoItem.isRowSummary() || infoItem.isSorted()) {
      key = getKey(ResultLevel.ROW, rowGroup, rowValue);
      putValue(key, col, item.calculate(getValue(key, col), value, infoItem.getFunction()));
    }
    if (infoItem.isColSummary()) {
      if (infoItem.isRowSummary()) {
        key = getKey(ResultLevel.TOTAL);
        putValue(key, col, item.calculate(getValue(key, col), value, infoItem.getFunction()));
      }
      key = getKey(ResultLevel.COL, colGroup);
      putValue(key, col, item.calculate(getValue(key, col), value, infoItem.getFunction()));
    }
    if (infoItem.isGroupSummary()) {
      if (infoItem.isRowSummary() || infoItem.isSorted()) {
        key = getKey(ResultLevel.GROUP, rowGroup);
        putValue(key, col, item.calculate(getValue(key, col), value, infoItem.getFunction()));
      }
      key = getKey(ResultLevel.GROUP_COL, rowGroup, colGroup);
      putValue(key, col, item.calculate(getValue(key, col), value, infoItem.getFunction()));
    }
  }

  @Override
  public void deserialize(String data) {
    processMembers(Serial.class, data, (serial, value) -> {
      switch (serial) {
        case ROW_GROUPS:
          rowGroups.clear();
          Codec.deserializeLinkedHashMap(value)
              .forEach((k, collection) -> rowGroups.putAll(ResultValue.restore(k),
                  Codec.deserializeList(collection).stream().map(ResultValue::restore)
                      .collect(Collectors.toList())));
          break;

        case COL_GROUPS:
          colGroups.clear();
          colGroups.addAll(Codec.deserializeList(value).stream().map(ResultValue::restore)
              .collect(Collectors.toList()));
          break;

        case VALUES:
          values.clear();
          Codec.deserializeLinkedHashMap(value)
              .forEach((c, map) -> Codec.deserializeLinkedHashMap(map)
                  .forEach((r, v) -> values.put(ResultValue.restore(r), c, v)));
          break;
      }
    });
  }

  public Object getCellValue(ResultValue rowGroup, ResultValue[] row, ResultValue colGroup,
      String col) {
    return getValue(getKey(ResultLevel.CELL, rowGroup, ResultValue.of(row), colGroup), col);
  }

  public Collection<ResultValue> getColGroups() {
    return colGroups;
  }

  public Object getColTotal(ResultValue colGroup, String col) {
    return getValue(getKey(ResultLevel.COL, colGroup), col);
  }

  public Object getGroupTotal(ResultValue rowGroup, String col) {
    return getValue(getKey(ResultLevel.GROUP, rowGroup), col);
  }

  public Object getGroupValue(ResultValue rowGroup, ResultValue colGroup, String col) {
    return getValue(getKey(ResultLevel.GROUP_COL, rowGroup, colGroup), col);
  }

  public Collection<ResultValue> getRowGroups(ReportInfoItem sortedItem) {
    List<ResultValue> result = new ArrayList<>(rowGroups.keySet());

    if (sortedItem != null) {
      Map<ResultValue, Object> items = new HashMap<>();

      for (ResultValue rowGroup : result) {
        items.put(rowGroup, getGroupTotal(rowGroup, sortedItem.getItem().getName()));
      }
      sort(result, items, sortedItem.getDescending());
    }
    return result;
  }

  public Collection<ResultValue[]> getRows(ResultValue rowGroup, ReportInfoItem sortedItem) {
    List<ResultValue> rows = new ArrayList<>(rowGroups.get(rowGroup));

    if (sortedItem != null) {
      Map<ResultValue, Object> items = new HashMap<>();

      for (ResultValue row : rows) {
        items.put(row, getRowTotal(rowGroup, row.getValues(), sortedItem.getItem().getName()));
      }
      sort(rows, items, sortedItem.getDescending());
    }
    List<ResultValue[]> result = new ArrayList<>();

    for (ResultValue row : rows) {
      result.add(row.getValues());
    }
    return result;
  }

  public Object getRowTotal(ResultValue rowGroup, ResultValue[] row, String col) {
    return getValue(getKey(ResultLevel.ROW, rowGroup, ResultValue.of(row)), col);
  }

  public Object getTotal(String col) {
    return getValue(getKey(ResultLevel.TOTAL), col);
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  @Override
  public String serialize() {
    return serializeMembers(Serial.class, serial -> {
      Object value = null;

      switch (serial) {
        case ROW_GROUPS:
          value = rowGroups.asMap();
          break;

        case COL_GROUPS:
          value = colGroups;
          break;

        case VALUES:
          Map<String, Map<ResultValue, Object>> m = new LinkedHashMap<>();

          values.columnMap().forEach((c, map) -> {
            Map<ResultValue, Object> tmp = new LinkedHashMap<>();
            map.forEach((r, v) -> tmp.put(r, Objects.isNull(v) ? null : v.toString()));
            m.put(c, tmp);
          });
          value = m;
          break;
      }
      return value;
    });
  }

  private static ResultValue getKey(ResultLevel level, ResultValue... prm) {
    int l = ArrayUtils.length(prm);
    ResultValue[] params = new ResultValue[l + 1];
    params[0] = ResultValue.of(level.name());

    System.arraycopy(prm, 0, params, 1, l);

    return ResultValue.of(params);
  }

  private Object getValue(ResultValue row, String col) {
    return values.get(row, col);
  }

  private void putValue(ResultValue row, String col, Object value) {
    if (value != null) {
      values.put(row, col, value);
    }
  }

  private static void sort(List<ResultValue> result, Map<ResultValue, Object> items, boolean desc) {
    result.sort((value1, value2) -> {
      Object item1;
      Object item2;

      if (desc) {
        item1 = items.get(value2);
        item2 = items.get(value1);
      } else {
        item1 = items.get(value1);
        item2 = items.get(value2);
      }

      if (item1 == null) {
        if (item2 == null) {
          return BeeConst.COMPARE_EQUAL;
        } else {
          return BeeConst.COMPARE_LESS;
        }
      } else if (item2 == null) {
        return BeeConst.COMPARE_MORE;
      } else if (item1 instanceof Comparable) {
        @SuppressWarnings("unchecked") int res = ((Comparable<Object>) item1).compareTo(item2);
        return res;
      } else {
        return BeeConst.COMPARE_EQUAL;
      }
    });
  }
}
