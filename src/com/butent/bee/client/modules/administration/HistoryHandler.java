package com.butent.bee.client.modules.administration;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.LocalProvider;
import com.butent.bee.client.dialog.ModalGrid;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

public class HistoryHandler extends AbstractGridInterceptor implements ClickHandler {

  private LocalProvider provider;
  private final String viewName;
  private final Collection<Long> ids;

  public HistoryHandler(String viewName, Collection<Long> ids) {
    Assert.notEmpty(viewName);
    this.viewName = viewName;
    this.ids = ids;
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.same(columnName, AUDIT_FLD_VALUE)) {
      column.getCell().addClickHandler(this);
    }
    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (presenter != null && presenter.getDataProvider() instanceof LocalProvider) {
      provider = (LocalProvider) presenter.getDataProvider();
      requery();
    }
  }

  @Override
  public String getCaption() {
    return BeeUtils.joinWords(Localized.getConstants().actionAudit(),
        BeeUtils.parenthesize(Data.getViewCaption(viewName)));
  }

  @Override
  public BeeRowSet getInitialRowSet(GridDescription gridDescription) {
    return new BeeRowSet(HISTORY_COLUMNS);
  }

  @Override
  public GridInterceptor getInstance() {
    return new HistoryHandler(viewName, ids);
  }

  @Override
  public void onClick(ClickEvent event) {
    if (event.getSource() instanceof AbstractCell<?>) {
      CellContext context = ((AbstractCell<?>) event.getSource()).getEventContext();
      IsRow row = context.getRow();
      String relation = row.getString(provider.getColumnIndex(COL_RELATION));

      if (!BeeUtils.isEmpty(relation)) {
        Long id = row.getLong(provider.getColumnIndex(AUDIT_FLD_VALUE));

        if (DataUtils.isId(id)) {
          GridFactory.openGrid(GRID_HISTORY,
              new HistoryHandler(relation, Lists.newArrayList(id)),
              null, ModalGrid.opener(500, 500));
        }
      }
    }
  }

  private void requery() {
    if (provider == null) {
      return;
    }
    provider.clear();

    ParameterList args = AdministrationKeeper.createArgs(SVC_GET_HISTORY);
    args.addDataItem(VAR_HISTORY_VIEW, viewName);

    if (!BeeUtils.isEmpty(ids)) {
      args.addDataItem(VAR_HISTORY_IDS, DataUtils.buildIdList(ids));
    }
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
