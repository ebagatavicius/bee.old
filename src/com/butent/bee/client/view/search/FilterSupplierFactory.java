package com.butent.bee.client.view.search;

import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.ui.FilterSupplierType;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public final class FilterSupplierFactory {

  public static AbstractFilterSupplier getSupplier(String viewName, List<BeeColumn> dataColumns,
      String idColumnName, String versionColumnName, String columnId, String label,
      List<String> searchBy, ValueType valueType, FilterSupplierType supplierType,
      List<String> renderColumns, List<String> orderColumns,
      String enumKey, Relation relation, String options) {

    BeeColumn sourceColumn = DataUtils.getColumn(columnId, dataColumns);

    List<BeeColumn> searchColumns = new ArrayList<>();
    if (!BeeUtils.isEmpty(searchBy)) {
      for (String by : searchBy) {
        BeeColumn column = DataUtils.getColumn(by, dataColumns);

        if (column != null) {
          searchColumns.add(column);
        } else if (BeeUtils.same(by, idColumnName)) {
          searchColumns.add(BeeColumn.forRowId(idColumnName));
        } else if (BeeUtils.same(by, versionColumnName)) {
          searchColumns.add(BeeColumn.forRowVersion(versionColumnName));
        } else {
          searchColumns.add(new BeeColumn(BeeUtils.nvl(valueType, ValueType.TEXT), by));
        }
      }
    }

    BeeColumn filterColumn = BeeUtils.isEmpty(searchColumns)
        ? sourceColumn : searchColumns.get(0);

    AbstractFilterSupplier supplier = null;

    if (supplierType != null) {
      switch (supplierType) {
        case VALUE:
          supplier = new ValueFilterSupplier(viewName, dataColumns, idColumnName, versionColumnName,
              filterColumn, label, searchColumns, options);
          break;

        case RANGE:
          supplier = new RangeFilterSupplier(viewName, filterColumn, label, options);
          break;

        case LIST:
          supplier = new ListFilterSupplier(viewName, sourceColumn, filterColumn, label,
              renderColumns, orderColumns, relation, options);
          break;

        case ENUM:
          supplier = new EnumFilterSupplier(viewName, filterColumn, options, label, enumKey);
          break;

        case ID:
          supplier = new IdFilterSupplier(viewName, BeeColumn.forRowId(idColumnName), label,
              options);
          break;

        case VERSION:
          supplier = new VersionFilterSupplier(viewName, BeeColumn.forRowVersion(versionColumnName),
              label, options);
          break;

        case CUSTOM:
          supplier = new CustomFilterSupplier(viewName, dataColumns, idColumnName,
              versionColumnName, columnId, valueType, label, searchColumns, options);
          break;

        case CUSTOM_DATE_TIME:
          supplier = new CustomDateTimeFilterSupplier(viewName, filterColumn, columnId, valueType,
              label, options);
          break;
      }
    }

    if (supplier == null && !BeeUtils.isEmpty(enumKey)) {
      supplier = new EnumFilterSupplier(viewName, filterColumn, options, label, enumKey);
    }

    if (supplier == null && filterColumn != null) {
      switch (filterColumn.getType()) {
        case BOOLEAN:
          supplier = new BooleanFilterSupplier(viewName, filterColumn, label, options);
          break;

        case DATE:
          supplier = new DateFilterSupplier(viewName, filterColumn, label, options);
          break;

        case DATE_TIME:
          supplier = new DateTimeFilterSupplier(viewName, filterColumn, label, options);
          break;

        case DECIMAL:
        case INTEGER:
        case LONG:
        case NUMBER:
          supplier = new RangeFilterSupplier(viewName, filterColumn, label, options);
          break;

        case TEXT:
        case BLOB:
        case TIME_OF_DAY:
          supplier =
              new ValueFilterSupplier(viewName, dataColumns, idColumnName, versionColumnName,
                  filterColumn, label, searchColumns, options);
          break;
      }
    }

    return supplier;
  }

  private FilterSupplierFactory() {
  }
}
