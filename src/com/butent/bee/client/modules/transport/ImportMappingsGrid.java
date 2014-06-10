package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.LocalProvider;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
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
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.transport.TransportConstants.ImportType.ImportProperty;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;

public class ImportMappingsGrid extends AbstractGridInterceptor {

  private final ImportOptionForm form;
  private LocalProvider provider;
  private Long parentId;

  public ImportMappingsGrid(ImportOptionForm form) {
    Assert.notNull(form);
    this.form = form;
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (presenter != null && presenter.getDataProvider() instanceof LocalProvider) {
      provider = (LocalProvider) presenter.getDataProvider();
      requery();
    }
  }

  @Override
  public boolean beforeAction(Action action, GridPresenter presenter) {
    if (action != Action.REFRESH) {
      return super.beforeAction(action, presenter);
    }
    requery();
    return false;
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    addNewMapping();
    return false;
  }

  @Override
  public DeleteMode beforeDeleteRow(GridPresenter presenter, final IsRow row) {
    Queries.deleteRow(TBL_IMPORT_MAPPINGS, row.getId(), row.getVersion(), new IntCallback() {
      @Override
      public void onSuccess(Integer result) {
        requery();
      }
    });
    return DeleteMode.CANCEL;
  }

  @Override
  public DeleteMode beforeDeleteRows(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows) {

    Queries.deleteRows(TBL_IMPORT_MAPPINGS, selectedRows, new IntCallback() {
      @Override
      public void onSuccess(Integer result) {
        requery();
      }
    });
    return DeleteMode.CANCEL;
  }

  @Override
  public BeeRowSet getInitialRowSet(GridDescription gridDescription) {
    return Data.createRowSet(TBL_IMPORT_MAPPINGS);
  }

  @Override
  public GridInterceptor getInstance() {
    return new ImportMappingsGrid(form);
  }
  
  @Override
  public void onParentRow(ParentRowEvent event) {
    this.parentId = event == null ? null : event.getRowId();
    requery();
  }

  private void addNewMapping() {
    ImportProperty target = form.getMappingProperty();

    if (target == null || !DataUtils.isId(parentId)) {
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
        Queries.insert(TBL_IMPORT_MAPPINGS, Data.getColumns(TBL_IMPORT_MAPPINGS,
            Lists.newArrayList(COL_IMPORT_PROPERTY, COL_IMPORT_VALUE, COL_IMPORT_MAPPING)),
            Lists.newArrayList(BeeUtils.toString(parentId), value.getValue(), mapping.getValue()),
            null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow newRow) {
                requery();
              }
            });
      }
    }, null, getGridView().getElement(), null);
  }

  private void requery() {
    if (provider == null) {
      return;
    }
    ImportProperty target = form.getMappingProperty();
    boolean ok = target != null && DataUtils.isId(parentId);

    getGridPresenter().getMainView().setEnabled(ok);
    provider.clear();

    if (!ok) {
      return;
    }
    getGridView().getGrid()
        .setColumnLabel(COL_IMPORT_MAPPING + COL_IMPORT_VALUE, target.getCaption());

    ParameterList args = TransportHandler.createArgs(SVC_GET_IMPORT_MAPPINGS);
    args.addDataItem(COL_IMPORT_PROPERTY, parentId);
    args.addDataItem(VAR_MAPPING_TABLE, target.getRelTable());
    args.addDataItem(VAR_MAPPING_FIELD, target.getRelField());

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        Assert.notNull(response);

        if (response.hasErrors()) {
          Global.showError(Lists.newArrayList(response.getErrors()));
          return;
        }
        BeeRowSet rowset = BeeRowSet.restore(response.getResponseAsString());

        for (BeeRow row : rowset.getRows()) {
          provider.addRow(row);
        }
        provider.refresh(false);
      }
    });
  }
}
