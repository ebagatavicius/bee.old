package com.butent.bee.shared.data;

import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.ExtendedProperty;

import java.util.List;

/**
 * Extends {@code RowList} class, enables to extend specified table properties.
 */

public class ExtendedPropertiesData extends RowList<TableRow, TableColumn> {

  private ExtendedPropertiesData() {
    super();
  }

  public ExtendedPropertiesData(List<ExtendedProperty> data, boolean addTime) {
    super();

    int c = addTime ? ExtendedProperty.COLUMN_COUNT : ExtendedProperty.COLUMN_COUNT - 1;
    for (int i = 0; i < c; i++) {
      addColumn(ValueType.TEXT, ExtendedProperty.COLUMN_HEADERS[i]);
    }

    long rowId = 0;
    for (ExtendedProperty property : data) {
      TableRow row = new TableRow(++rowId);
      row.addCell(new TextValue(property.getName()));
      row.addCell(new TextValue(property.getSub()));
      row.addCell(new TextValue(property.getValue()));

      if (addTime) {
        row.addCell(new TextValue(property.getDate().toTimeString()));
      }

      addRow(row);
    }
  }

  @Override
  public ExtendedPropertiesData copy() {
    ExtendedPropertiesData result = new ExtendedPropertiesData();
    copyTableDescription(result);
    result.setRows(getRows());
    return result;
  }

  @Override
  public ExtendedPropertiesData create() {
    return new ExtendedPropertiesData();
  }

  @Override
  public TableColumn createColumn(ValueType type, String label, String id) {
    return new TableColumn(type, label, id);
  }

  @Override
  public TableRow createRow(long id) {
    return new TableRow(id);
  }
}
