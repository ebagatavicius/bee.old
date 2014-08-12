package com.butent.bee.shared.data;

import com.google.common.collect.Lists;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.Property;

import java.util.List;
import java.util.Map;

/**
 * Enables creating, containing and cloning properties of a row list.
 */

public class PropertiesData extends RowList<StringRow, TableColumn> {

  private PropertiesData() {
    super();
  }

  public PropertiesData(List<Property> data, String... columnLabels) {
    super();
    createColumns(columnLabels);

    if (data != null) {
      long id = 0;
      for (Property property : data) {
        addRow(++id, property.getName(), property.getValue());
      }
    }
  }

  public PropertiesData(Map<String, String> data, String... columnLabels) {
    super();
    createColumns(columnLabels);

    if (data != null) {
      long id = 0;
      for (Map.Entry<String, String> entry : data.entrySet()) {
        addRow(++id, entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public PropertiesData copy() {
    PropertiesData result = new PropertiesData();
    copyTableDescription(result);
    result.setRows(getRows());
    return result;
  }

  @Override
  public PropertiesData create() {
    return new PropertiesData();
  }

  @Override
  public TableColumn createColumn(ValueType type, String label, String id) {
    return new TableColumn(type, label, id);
  }

  @Override
  public StringRow createRow(long id) {
    return new StringRow(id, BeeConst.EMPTY_IMMUTABLE_STRING_LIST);
  }

  private void addRow(long id, String name, String value) {
    List<String> values = Lists.newArrayList(name, value);
    StringRow row = new StringRow(id, values);
    addRow(row);
  }

  private void createColumns(String... columnLabels) {
    int pc = (columnLabels == null) ? 0 : columnLabels.length;
    String label;
    for (int i = 0; i < Property.HEADER_COUNT; i++) {
      label = (pc > 0 && i < pc) ? columnLabels[i] : Property.HEADERS[i];
      addColumn(ValueType.TEXT, label);
    }
  }
}
