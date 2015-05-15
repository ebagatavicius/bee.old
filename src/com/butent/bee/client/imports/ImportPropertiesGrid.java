package com.butent.bee.client.imports;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.imports.ImportProperty;
import com.butent.bee.shared.imports.ImportType;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportPropertiesGrid extends AbstractGridInterceptor {

  private Map<String, ImportProperty> propMap = new LinkedHashMap<>();

  @Override
  public void afterDeleteRow(long rowId) {
    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), TBL_IMPORT_CONDITIONS);
    super.afterDeleteRow(rowId);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    addNewProperty();
    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return null;
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName,
      List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
      CellSource cellSource) {

    if (BeeUtils.same(columnName, COL_IMPORT_PROPERTY)) {
      return new AbstractCellRenderer(cellSource) {
        @Override
        public String render(IsRow row) {
          String name = getString(row);
          String value = propMap.containsKey(name) ? propMap.get(name).getCaption() : name;
          Long relId = Data.getLong(getViewName(), row, COL_IMPORT_RELATION_OPTION);

          if (DataUtils.isId(relId)) {
            value += BeeConst.CHAR_NBSP
                + new FaLabel(FontAwesome.SIGN_IN, true).getElement().getString();
          }
          return value;
        }
      };
    }
    return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    IsRow row = event.getRowValue();
    Long relId = row.getLong(getDataIndex(COL_IMPORT_RELATION_OPTION));

    if (DataUtils.isId(relId)) {
      event.consume();

      if (BeeUtils.same(event.getColumnId(), COL_IMPORT_VALUE)) {
        String ov = row.getString(getDataIndex(COL_IMPORT_VALUE));
        String nv = BeeUtils.isEmpty(ov) ? "1" : null;

        Queries.update(getViewName(), row.getId(), row.getVersion(),
            DataUtils.getColumns(getGridView().getDataColumns(),
                Collections.singletonList(COL_IMPORT_VALUE)), Queries.asList(ov),
            Queries.asList(nv), null, new RowUpdateCallback(getViewName()));
      } else {
        Queries.getRow(TBL_IMPORT_OPTIONS, relId, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            RowEditor.openForm(FORM_IMPORT_DATA, Data.getDataInfo(TBL_IMPORT_OPTIONS), result,
                Opener.MODAL, null,
                new ImportDataForm(Data.getString(TBL_IMPORT_OPTIONS, result, COL_IMPORT_DATA)));
          }
        });
      }
    } else {
      super.onEditStart(event);
    }
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    propMap.clear();
    String parentView = event.getViewName();
    IsRow parentRow = event.getRow();

    ImportType type = EnumUtils.getEnumByIndex(ImportType.class,
        Data.getInteger(parentView, parentRow, COL_IMPORT_TYPE));

    if (type == null) {
      return;
    }
    boolean isRoot = BeeUtils.isEmpty(Data.getString(parentView, parentRow,
        COL_IMPORT_RELATION_OPTION));

    for (ImportProperty prop : type.getProperties()) {
      if (prop.isDataProperty() || isRoot) {
        propMap.put(prop.getName(), prop);
      }
    }
    String viewName = Data.getString(parentView, parentRow, COL_IMPORT_DATA);

    if (!BeeUtils.isEmpty(viewName)) {
      propMap.putAll(ImportDataForm.getDataProperties(viewName));
    }
    super.onParentRow(event);
  }

  private void addNewProperty() {
    Set<String> exists = new HashSet<>();
    int propIdx = getDataIndex(COL_IMPORT_PROPERTY);

    for (IsRow row : getGridView().getRowData()) {
      exists.add(row.getString(propIdx));
    }
    final Map<String, String> choices = new LinkedHashMap<>();

    for (ImportProperty prop : propMap.values()) {
      if (!exists.contains(prop.getName())) {
        choices.put(prop.getName(), prop.getCaption());
      }
    }
    if (choices.isEmpty()) {
      getGridView().notifyWarning(Localized.getConstants().noData());
      return;
    }
    Global.choice(Localized.getConstants().trImportNewProperty(), null,
        new ArrayList<>(choices.values()), new ChoiceCallback() {
          @Override
          public void onSuccess(int value) {
            final ImportProperty prop = propMap.get(new ArrayList<>(choices.keySet()).get(value));

            final Consumer<Long> consumer = new Consumer<Long>() {
              @Override
              public void accept(final Long dataOption) {
                getGridView().ensureRelId(new IdCallback() {
                  @Override
                  public void onSuccess(Long id) {
                    Queries.insert(getViewName(),
                        Data.getColumns(getViewName(), Arrays.asList(COL_IMPORT_OPTION,
                            COL_IMPORT_PROPERTY, COL_IMPORT_RELATION_OPTION)),
                        Queries.asList(id, prop.getName(), dataOption), null,
                        new RowCallback() {
                          @Override
                          public void onSuccess(BeeRow result) {
                            getGridView().getGrid().insertRow(result, true);
                          }
                        });
                  }
                });
              }
            };
            if (!BeeUtils.isEmpty(prop.getRelation())) {
              Queries.insert(TBL_IMPORT_OPTIONS, Data.getColumns(TBL_IMPORT_OPTIONS,
                      Arrays.asList(COL_IMPORT_TYPE, COL_IMPORT_DATA)),
                  Queries.asList(ImportType.DATA.ordinal(), prop.getRelation()), null,
                  new RowCallback() {
                    @Override
                    public void onSuccess(BeeRow result) {
                      consumer.accept(result.getId());
                    }
                  });
            } else {
              consumer.accept(null);
            }
          }
        });
  }
}
