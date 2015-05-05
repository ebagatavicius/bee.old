package com.butent.bee.client.imports;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.imports.ImportProperty;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImportDataForm extends AbstractFormInterceptor {

  private final String viewName;

  public ImportDataForm(String viewName) {
    this.viewName = viewName;
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid) {
      GridInterceptor interceptor = null;

      if (BeeUtils.same(name, TBL_IMPORT_PROPERTIES)) {
        interceptor = new ImportPropertiesGrid();

      } else if (BeeUtils.same(name, TBL_IMPORT_MAPPINGS)) {
        interceptor = new ImportMappingsGrid(viewName);

      } else if (BeeUtils.same(name, TBL_IMPORT_CONDITIONS)) {
        interceptor = new ImportConditionsGrid();
      }
      if (interceptor != null) {
        ((ChildGrid) widget).setGridInterceptor(interceptor);
      }
    }
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    getHeaderView().setCaption(BeeUtils.join(": ", form.getCaption(),
        Data.getViewCaption(getStringValue(COL_IMPORT_DATA))));
  }

  @Override
  public FormInterceptor getInstance() {
    return null;
  }

  public static Map<String, ImportProperty> getDataProperties(String viewName) {
    Map<String, ImportProperty> props = new LinkedHashMap<>();

    for (ViewColumn col : Data.getDataInfo(viewName).getViewColumns()) {
      if (col.isReadOnly() || !BeeUtils.unbox(col.getEditable())
          && BeeUtils.isPositive(col.getLevel())) {
        continue;
      }
      String name = col.getName();
      ImportProperty prop = new ImportProperty(name, Data.getColumnLabel(viewName, name), true);

      if (!BeeUtils.isEmpty(col.getRelation())) {
        prop.setRelation(col.getRelation());
      }
      props.put(prop.getName(), prop);
    }
    return props;
  }
}