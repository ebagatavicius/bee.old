package com.butent.bee.client.imports;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.imports.ImportProperty;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;

public class ImportMappingsGrid extends AbstractGridInterceptor {

  private final ImportOptionForm form;

  public ImportMappingsGrid(ImportOptionForm form) {
    this.form = Assert.notNull(form);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    addNewMapping();
    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return null;
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    if (getGridPresenter() == null) {
      return;
    }
    Filter filter = null;
    ImportProperty target = form.getMappingProperty();

    if (target != null) {
      filter = Filter.custom(TBL_IMPORT_MAPPINGS,
          Codec.beeSerialize(ImmutableMap.of(VAR_MAPPING_TABLE, target.getRelTable(),
              VAR_MAPPING_FIELD, target.getRelField())));

      getGridView().getGrid()
          .setColumnLabel(COL_IMPORT_MAPPING + COL_IMPORT_VALUE, target.getCaption());
    }
    getGridPresenter().getDataProvider().setUserFilter(filter);
  }

  private void addNewMapping() {
    ImportProperty target = form.getMappingProperty();

    if (target == null) {
      getGridView().notifyWarning(Localized.getConstants().actionNotAllowed());
      return;
    }
    HtmlTable table = new HtmlTable();
    table.setColumnCellClasses(0, StyleUtils.NAME_REQUIRED);
    int row = 0;

    final InputText value = new InputText();
    table.setHtml(row, 0, Localized.getConstants().trImportValue());
    table.setWidget(row, 1, value);
    row++;

    ArrayList<String> cols = Lists.newArrayList(target.getRelField());
    Relation relation = Relation.create(target.getRelTable(), cols);

    final UnboundSelector mapping = UnboundSelector.create(relation, cols);
    table.setHtml(row, 0, target.getCaption());
    table.setWidget(row, 1, mapping);
    row++;

    Global.inputWidget(Localized.getConstants().trImportNewMapping(), table, new InputCallback() {
      @Override
      public String getErrorMessage() {
        if (BeeUtils.isEmpty(value.getValue())) {
          value.setFocus(true);
        } else if (BeeUtils.isEmpty(mapping.getValue())) {
          mapping.setFocus(true);
        } else {
          return super.getErrorMessage();
        }
        return Localized.getConstants().valueRequired();
      }

      @Override
      public void onSuccess() {
        getGridView().ensureRelId(new IdCallback() {
          @Override
          public void onSuccess(Long id) {
            Queries.insert(TBL_IMPORT_MAPPINGS, Data.getColumns(TBL_IMPORT_MAPPINGS,
                Lists.newArrayList(COL_IMPORT_OPTION, COL_IMPORT_VALUE, COL_IMPORT_MAPPING)),
                Lists.newArrayList(BeeUtils.toString(id), value.getValue(), mapping.getValue()),
                null, new RowCallback() {
                  @Override
                  public void onSuccess(BeeRow result) {
                    getGridView().getGrid().insertRow(result, true);
                    getGridPresenter().refresh(true);
                  }
                });
          }
        });
      }
    }, null, getGridView().getElement(), null);
  }
}
