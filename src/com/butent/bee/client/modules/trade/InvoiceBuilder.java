package com.butent.bee.client.modules.trade;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_CURRENCY;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class InvoiceBuilder extends AbstractGridInterceptor implements ClickHandler {

  private boolean isChild;

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (!isChild) {
      presenter.getHeader()
          .addCommandItem(new Button(Localized.getConstants().createPurchaseInvoice(), this));
    }
  }

  @Override
  public Map<String, Filter> getInitialParentFilters(Collection<UiOption> uiOptions) {
    isChild = BeeUtils.contains(uiOptions, UiOption.CHILD);
    Map<String, Filter> filters = super.getInitialParentFilters(uiOptions);

    if (!isChild) {
      if (filters == null) {
        filters = new HashMap<>();
      }
      filters.put(NameUtils.getClassName(this.getClass()), Filter.isNull(getRelationColumn()));
    }
    return filters;
  }

  @Override
  public void onClick(ClickEvent clickEvent) {
    final GridPresenter presenter = getGridPresenter();
    final Set<Long> ids = new HashSet<>();

    for (RowInfo row : presenter.getGridView().getSelectedRows(GridView.SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      presenter.getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }
    Queries.getRowSet(getViewName(), null, Filter.idIn(ids), new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet data) {
        int item = DataUtils.getColumnIndex(ClassifierConstants.COL_ITEM, data.getColumns(), false);
        final Pair<Boolean, Long> mainItem = Pair.of(BeeConst.isUndef(item), null);

        if (!mainItem.getA()) {
          for (BeeRow row : data) {
            if (!DataUtils.isId(row.getLong(item))) {
              mainItem.setA(true);
              break;
            }
          }
        }
        final DataInfo dataInfo = Data.getDataInfo(getTargetView());
        BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);
        Map<String, String> initialValues = getInitialValues(data);

        if (!BeeUtils.isEmpty(initialValues)) {
          for (Map.Entry<String, String> entry : initialValues.entrySet()) {
            int idx = dataInfo.getColumnIndex(entry.getKey());

            if (!BeeConst.isUndef(idx)) {
              newRow.setValue(idx, entry.getValue());
            }
          }
        }
        RowFactory.createRow(dataInfo.getNewRowForm(), dataInfo.getNewRowCaption(), dataInfo,
            newRow, null, new InvoiceForm(mainItem), new RowCallback() {
              @Override
              public void onSuccess(final BeeRow row) {
                ParameterList args = getRequestArgs();

                if (args == null) {
                  return;
                }
                args.addDataItem(getRelationColumn(), row.getId());
                args.addDataItem(COL_CURRENCY, row.getLong(dataInfo.getColumnIndex(COL_CURRENCY)));
                args.addDataItem(Service.VAR_ID, DataUtils.buildIdList(ids));

                if (DataUtils.isId(mainItem.getB())) {
                  args.addDataItem(ClassifierConstants.COL_ITEM, mainItem.getB());
                }
                BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                  @Override
                  public void onResponse(ResponseObject response) {
                    response.notify(presenter.getGridView());

                    if (!response.hasErrors()) {
                      Data.onViewChange(presenter.getViewName(), DataChangeEvent.RESET_REFRESH);
                      RowEditor.openForm(dataInfo.getEditForm(), dataInfo, row.getId(),
                          Opener.MODAL);
                    }
                  }
                });
              }
            });
      }
    });
  }

  /**
   * @param data
   */
  protected Map<String, String> getInitialValues(BeeRowSet data) {
    return null;
  }

  protected abstract String getRelationColumn();

  protected abstract ParameterList getRequestArgs();

  protected abstract String getTargetView();
}
