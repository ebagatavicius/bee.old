package com.butent.bee.client.imports;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.OptionElement;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.imports.ImportProperty;
import com.butent.bee.shared.imports.ImportType;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ImportPropertiesGrid extends AbstractGridInterceptor {

  private final ImportOptionForm form;
  private Map<String, String> propMap = new LinkedHashMap<>();

  public ImportPropertiesGrid(ImportOptionForm form) {
    this.form = Assert.notNull(form);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    addProperties();
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
          String value = BeeUtils.notEmpty(propMap.get(name), name);
          Long relId = Data.getLong(TBL_IMPORT_PROPERTIES, row, COL_IMPORT_RELATION_OPTION);

          if (DataUtils.isId(relId)) {
            value += " " + new FaLabel(FontAwesome.SIGN_IN, true).getElement().getString();
          }
          return value;
        }
      };
    }
    return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
  }

  @Override
  public void onActiveRowChange(ActiveRowChangeEvent event) {
    Long relId = null;

    if (event.getRowValue() != null) {
      relId = event.getRowValue().getLong(getDataIndex(COL_IMPORT_RELATION_OPTION));
    }
    form.showMappings(DataUtils.isId(relId));
    super.onActiveRowChange(event);
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    IsRow row = event.getRowValue();
    Long relId = row.getLong(getDataIndex(COL_IMPORT_RELATION_OPTION));

    if (DataUtils.isId(relId)) {
      event.consume();

      if (BeeUtils.same(event.getColumnId(), COL_IMPORT_VALUE)) {
        GridView gridView = getGridView();
        String ov = row.getString(getDataIndex(COL_IMPORT_VALUE));
        String nv = BeeUtils.isEmpty(ov) ? "1" : null;

        Queries.update(getViewName(), row.getId(), row.getVersion(),
            Lists.newArrayList(DataUtils.getColumn(COL_IMPORT_VALUE, gridView.getDataColumns())),
            Queries.asList(ov), Queries.asList(nv), null, new RowUpdateCallback(getViewName()));
      } else {
        RowEditor.open(TBL_IMPORT_OPTIONS, relId, Opener.MODAL);
      }
    } else {
      super.onEditStart(event);
    }
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    propMap.clear();
    boolean isSubOption = form.isSubOption();

    for (ImportProperty prop : form.getProperties()) {
      if (isSubOption && !prop.isDataProperty()) {
        continue;
      }
      propMap.put(prop.getName(), prop.getCaption());
    }
    super.onParentRow(event);
  }

  private void addProperties() {
    Set<String> exists = new HashSet<>();
    int propIdx = getDataIndex(COL_IMPORT_PROPERTY);

    for (IsRow row : getGridView().getRowData()) {
      exists.add(row.getString(propIdx));
    }
    final ListBox listBox = new ListBox(true);

    for (Entry<String, String> entry : propMap.entrySet()) {
      if (!exists.contains(entry.getKey())) {
        listBox.addItem(entry.getValue(), entry.getKey());
      }
    }
    if (listBox.isEmpty()) {
      Global.showInfo(Localized.getConstants().noData());
      return;
    } else if (listBox.getItemCount() > 30) {
      listBox.setVisibleItemCount(30);
    } else {
      listBox.setAllVisible();
    }
    Global.inputWidget(Localized.getConstants().trImportNewProperty(), listBox,
        new InputCallback() {
          @Override
          public void onSuccess() {
            final Map<String, Long> props = new HashMap<>();

            for (int i = 0; i < listBox.getItemCount(); i++) {
              OptionElement optionElement = listBox.getOptionElement(i);

              if (optionElement.isSelected()) {
                props.put(optionElement.getValue(), null);
              }
            }
            if (BeeUtils.isEmpty(props)) {
              return;
            }
            final BiConsumer<String, Long> consumer = new BiConsumer<String, Long>() {
              private int cnt;

              @Override
              public void accept(String property, Long option) {
                props.put(property, option);

                if (++cnt == props.size()) {
                  getGridView().ensureRelId(new IdCallback() {
                    @Override
                    public void onSuccess(Long id) {
                      String view = getViewName();

                      BeeRowSet rowSet = new BeeRowSet(view,
                          Data.getColumns(view, Lists.newArrayList(COL_IMPORT_OPTION,
                              COL_IMPORT_PROPERTY, COL_IMPORT_RELATION_OPTION)));

                      for (Entry<String, Long> entry : props.entrySet()) {
                        rowSet.addRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION,
                            Queries.asList(id, entry.getKey(), entry.getValue()));
                      }
                      Queries.insertRows(rowSet, new Callback<RowInfoList>() {
                        @Override
                        public void onSuccess(RowInfoList result) {
                          getGridPresenter().refresh(false);
                        }
                      });
                    }
                  });
                }
              }
            };
            for (final String prop : props.keySet()) {
              String relation = form.getProperty(prop).getRelTable();

              if (!BeeUtils.isEmpty(relation)) {
                Queries.insert(TBL_IMPORT_OPTIONS, Data.getColumns(TBL_IMPORT_OPTIONS,
                    Lists.newArrayList(COL_IMPORT_TYPE, COL_IMPORT_DATA)),
                    Queries.asList(ImportType.DATA.ordinal(), relation), null, new RowCallback() {
                      @Override
                      public void onSuccess(BeeRow result) {
                        consumer.accept(prop, result.getId());
                      }
                    });
              } else {
                consumer.accept(prop, null);
              }
            }
          }
        });
  }
}
