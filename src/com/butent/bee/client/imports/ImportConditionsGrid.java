package com.butent.bee.client.imports;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.imports.ImportProperty;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportConditionsGrid extends AbstractGridInterceptor {

  private Long optionId;
  private String optionData;
  private final Table<String, String, String> captions = HashBasedTable.create();

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    addNewCondition(optionId, optionData);
    return false;
  }

  @Override
  public AbstractCellRenderer getRenderer(final String columnName,
      List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
      CellSource cellSource) {

    if (BeeUtils.same(columnName, COL_IMPORT_PROPERTY)) {
      return new AbstractCellRenderer(null) {
        @Override
        public String render(IsRow row) {
          String data = Data.getString(getViewName(), row, COL_IMPORT_DATA);

          if (!captions.containsRow(data)) {
            for (ImportProperty prop : ImportDataForm.getDataProperties(data).values()) {
              captions.put(data, prop.getName(), prop.getCaption());
            }
          }
          return captions.get(data, Data.getString(getViewName(), row,
              COL_IMPORT_PROPERTY + COL_IMPORT_VALUE));
        }
      };
    }
    return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
  }

  @Override
  public GridInterceptor getInstance() {
    return null;
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    if (!DataUtils.isId(optionId)) {
      if (event.getRow() != null) {
        optionId = Data.getLong(event.getViewName(), event.getRow(), COL_IMPORT_OPTION);
        Queries.getValue(TBL_IMPORT_OPTIONS, optionId, COL_IMPORT_DATA, new RpcCallback<String>() {
          @Override
          public void onSuccess(String result) {
            optionData = result;
          }
        });
      }
    }
    super.onParentRow(event);
  }

  private void addNewCondition(Long option, final String data) {
    Queries.getRowSet(TBL_IMPORT_PROPERTIES,
        Arrays.asList(COL_IMPORT_PROPERTY, COL_IMPORT_RELATION_OPTION),
        Filter.equals(COL_IMPORT_OPTION, option), new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            Map<String, ImportProperty> props = ImportDataForm.getDataProperties(data);
            final Map<Long, String> choices = new LinkedHashMap<>();
            final Map<Long, Pair<Long, String>> relations = new HashMap<>();

            Set<Long> exists = new HashSet<>();
            int propIdx = getDataIndex(COL_IMPORT_PROPERTY);

            for (IsRow row : getGridView().getRowData()) {
              exists.add(row.getLong(propIdx));
            }
            for (int i = 0; i < result.getNumberOfRows(); i++) {
              Long propId = result.getRow(i).getId();

              if (!exists.contains(propId)) {
                ImportProperty prop = props.get(result.getString(i, COL_IMPORT_PROPERTY));
                choices.put(propId, prop.getCaption());

                if (!BeeUtils.isEmpty(prop.getRelation())) {
                  choices.put(propId, choices.get(propId) + "...");
                  relations.put(propId,
                      Pair.of(result.getLong(i, COL_IMPORT_RELATION_OPTION), prop.getRelation()));
                }
              }
            }
            if (choices.isEmpty()) {
              getGridView().notifyWarning(Localized.getConstants().noData());
              return;
            }
            Global.choice(Data.getViewCaption(data), Localized.getConstants().trImportProperty(),
                new ArrayList<>(choices.values()), new ChoiceCallback() {
                  @Override
                  public void onSuccess(int value) {
                    final Long propId = new ArrayList<>(choices.keySet()).get(value);
                    Pair<Long, String> pair = relations.get(propId);

                    if (pair != null) {
                      addNewCondition(pair.getA(), pair.getB());
                    } else {
                      getGridView().ensureRelId(new IdCallback() {
                        @Override
                        public void onSuccess(Long id) {
                          Queries.insert(getViewName(), Data.getColumns(getViewName(),
                              Arrays.asList(COL_IMPORT_MAPPING, COL_IMPORT_PROPERTY)),
                              Queries.asList(id, propId), null, new RowCallback() {
                                @Override
                                public void onSuccess(BeeRow row) {
                                  getGridView().getGrid().insertRow(row, true);
                                }
                              });
                        }
                      });
                    }
                  }
                });
          }
        });
  }
}
