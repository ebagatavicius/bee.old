package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.transport.TransportConstants.ImportType;
import com.butent.bee.shared.modules.transport.TransportConstants.ImportType.ImportProperty;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;

public class ImportOptionForm extends AbstractFormInterceptor {

  private GridInterceptor properties;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid) {
      GridInterceptor interceptor = null;

      if (BeeUtils.same(name, TBL_IMPORT_PROPERTIES)) {
        interceptor = new ImportPropertiesGrid(this);
        this.properties = interceptor;

      } else if (BeeUtils.same(name, TBL_IMPORT_MAPPINGS)) {
        interceptor = new ImportMappingsGrid(this);
      }
      if (interceptor != null) {
        ((ChildGrid) widget).setGridInterceptor(interceptor);
      }
    }
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    getHeaderView().setCaption(BeeUtils.join(": ", form.getCaption(),
        EnumUtils.getEnumByIndex(ImportType.class,
            row.getInteger(form.getDataIndex(COL_IMPORT_TYPE))).getCaption()));
  }

  @Override
  public FormInterceptor getInstance() {
    return new ImportOptionForm();
  }

  public ImportProperty getMappingProperty() {
    ImportProperty target = null;

    if (properties.getGridView() != null && properties.getGridView().getActiveRow() != null) {
      ImportType type = getImportType();

      if (type != null) {
        target = type.getProperty(properties.getGridView().getActiveRow()
            .getString(properties.getDataIndex(COL_IMPORT_PROPERTY)));

        if (target != null && BeeUtils.isEmpty(target.getRelTable())) {
          target = null;
        }
      }
    }
    return target;
  }

  public Collection<ImportProperty> getProperties() {
    ImportType type = getImportType();

    if (type != null) {
      return type.getProperties();
    }
    return null;
  }

  public ImportProperty getProperty(String name) {
    ImportType type = getImportType();

    if (type != null) {
      return type.getProperty(name);
    }
    return null;
  }

  private ImportType getImportType() {
    FormView formView = getFormView();
    IsRow row = formView.getActiveRow();

    if (row != null) {
      return EnumUtils.getEnumByIndex(ImportType.class,
          row.getInteger(formView.getDataIndex(COL_IMPORT_TYPE)));
    }
    return null;
  }
}
