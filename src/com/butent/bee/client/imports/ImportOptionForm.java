package com.butent.bee.client.imports;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.imports.ImportProperty;
import com.butent.bee.shared.imports.ImportType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class ImportOptionForm extends AbstractFormInterceptor {

  private GridInterceptor properties;
  private Widget mappings;

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
        this.mappings = widget.asWidget();
      }
      if (interceptor != null) {
        ((ChildGrid) widget).setGridInterceptor(interceptor);
      }
    }
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    ImportType type = getImportType();
    String cap = type.getCaption();

    if (Objects.equals(type, ImportType.DATA)) {
      cap = BeeUtils.joinWords(cap,
          BeeUtils.parenthesize(Data.getViewCaption(getStringValue(COL_IMPORT_DATA))));
    }
    getHeaderView().setCaption(BeeUtils.join(": ", form.getCaption(), cap));
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

    Collection<ImportProperty> props = new ArrayList<>(type.getProperties());

    if (Objects.equals(type, ImportType.DATA)) {
      String viewName = getStringValue(COL_IMPORT_DATA);

      for (ViewColumn col : Data.getDataInfo(viewName).getViewColumns()) {
        if (col.isReadOnly() || !BeeUtils.unbox(col.getEditable())
            && BeeUtils.isPositive(col.getLevel())) {
          continue;
        }
        String name = col.getName();
        ImportProperty prop = new ImportProperty(name, Data.getColumnLabel(viewName, name), true);

        if (!BeeUtils.isEmpty(col.getRelation())) {
          prop.setRelTable(col.getRelation());
        }
        props.add(prop);
      }
    }
    return props;
  }

  public ImportProperty getProperty(String name) {
    for (ImportProperty prop : getProperties()) {
      if (BeeUtils.same(name, prop.getName())) {
        return prop;
      }
    }
    return null;
  }

  public boolean isSubOption() {
    return DataUtils.isId(getLongValue(COL_IMPORT_RELATION_OPTION));
  }

  public void showMappings(boolean show) {
    if (mappings != null) {
      mappings.setVisible(show);
    }
  }

  private ImportType getImportType() {
    return EnumUtils.getEnumByIndex(ImportType.class, getIntegerValue(COL_IMPORT_TYPE));
  }
}