package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.transport.TransportConstants.ImportType.ImportProperty;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ImportPropertiesGrid extends AbstractGridInterceptor {

  private final ImportOptionForm form;

  public ImportPropertiesGrid(ImportOptionForm form) {
    Assert.notNull(form);
    this.form = form;
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    addNewProperty();
    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new ImportPropertiesGrid(form);
  }
  
  @Override
  public AbstractCellRenderer getRenderer(final String columnName,
      List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
      CellSource cellSource) {

    if (BeeUtils.same(columnName, COL_IMPORT_PROPERTY)) {
      return new AbstractCellRenderer(null) {
        @Override
        public String render(IsRow row) {
          String name = Data.getString(TBL_IMPORT_PROPERTIES, row, columnName);
          ImportProperty prop = form.getProperty(name);

          if (prop != null) {
            return prop.getCaption();
          }
          return name;
        }
      };
    }
    return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
  }

  private void addNewProperty() {
    Collection<ImportProperty> props = form.getProperties();
    Map<String, String> map = Maps.newLinkedHashMap();

    if (!BeeUtils.isEmpty(props)) {
      for (ImportProperty prop : props) {
        map.put(prop.getName(), prop.getCaption());
      }
      int propIdx = getGridView().getDataIndex(COL_IMPORT_PROPERTY);

      for (IsRow row : getGridView().getRowData()) {
        map.remove(row.getString(propIdx));
      }
    }
    if (BeeUtils.isEmpty(map)) {
      getGridView().notifyWarning(Localized.getConstants().noData());
      return;
    }
    HtmlTable table = new HtmlTable();
    int row = 0;

    final ListBox property = new ListBox();
    table.setHtml(row, 0, Localized.getConstants().trImportProperty(), StyleUtils.NAME_REQUIRED);
    table.setWidget(row, 1, property);
    row++;

    for (Entry<String, String> idx : map.entrySet()) {
      property.addItem(idx.getValue(), idx.getKey());
    }
    final InputText value = new InputText();
    table.setHtml(row, 0, Localized.getConstants().trImportValue());
    table.setWidget(row, 1, value);
    row++;

    Global.inputWidget(Localized.getConstants().trImportNewProperty(), table, new InputCallback() {
      @Override
      public String getErrorMessage() {
        if (BeeUtils.isEmpty(property.getValue())) {
          property.setFocus(true);
        } else {
          return super.getErrorMessage();
        }
        return Localized.getConstants().valueRequired();
      }

      @Override
      public void onSuccess() {
        final GridView grid = getGridView();

        grid.ensureRelId(new IdCallback() {
          @Override
          public void onSuccess(Long id) {
            Queries.insert(getViewName(), grid.getDataColumns(),
                Lists.newArrayList(BeeUtils.toString(id), property.getValue(), value.getValue()),
                null, new RowInsertCallback(getViewName(), grid.getId()) {
                  @Override
                  public void onSuccess(BeeRow newRow) {
                    super.onSuccess(newRow);
                    grid.getGrid().insertRow(newRow, true);
                  }
                });
          }
        });
      }
    }, null, getGridView().getElement(), null);
  }
}
