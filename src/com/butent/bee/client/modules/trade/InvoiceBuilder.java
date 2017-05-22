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
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
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
import java.util.function.BiConsumer;

public abstract class InvoiceBuilder extends AbstractGridInterceptor
    implements ClickHandler, BiConsumer<BeeRowSet, BeeRow> {

  private boolean isChild;

  @Override
  public void accept(final BeeRowSet data, BeeRow newRow) {
    final DataInfo dataInfo = Data.getDataInfo(getTargetView());
    int item = DataUtils.getColumnIndex(ClassifierConstants.COL_ITEM, data.getColumns());
    boolean itemAbsent = BeeConst.isUndef(item);

    if (!itemAbsent) {
      for (BeeRow row : data) {
        if (!DataUtils.isId(row.getLong(item))) {
          itemAbsent = true;
          break;
        }
      }
    }
    final Holder<Long> mainItem;

    if (itemAbsent) {
      mainItem = Holder.absent();
    } else {
      mainItem = null;
    }
    RowFactory.createRow(dataInfo.getNewRowForm(), dataInfo.getNewRowCaption(), dataInfo,
        newRow, Modality.ENABLED, null, new InvoiceForm(mainItem), null, new RowCallback() {
          @Override
          public void onSuccess(final BeeRow row) {
            ParameterList args = getRequestArgs();

            if (args != null) {
              Map<String, String> params = new HashMap<>();

              params.put(Service.VAR_TABLE, Data.getViewTable(getViewName()));
              params.put(getRelationColumn(), BeeUtils.toString(row.getId()));
              params.put(Service.VAR_DATA, DataUtils.buildIdList(data.getRowIds()));
              params.put(COL_CURRENCY, row.getString(dataInfo.getColumnIndex(COL_CURRENCY)));

              if (mainItem != null && DataUtils.isId(mainItem.get())) {
                params.put(ClassifierConstants.COL_ITEM, BeeUtils.toString(mainItem.get()));
              }
              for (String prm : params.keySet()) {
                if (!args.hasParameter(prm)) {
                  args.addDataItem(prm, params.get(prm));
                }
              }
              BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  response.notify(getGridView());

                  if (!response.hasErrors()) {
                    Popup popup = UiHelper.getParentPopup(getGridView().getGrid());

                    if (popup != null) {
                      popup.close();
                    }
                    Data.resetLocal(getViewName());
                    RowEditor.openForm(dataInfo.getEditForm(), dataInfo,
                        Filter.compareId(row.getId()), Opener.MODAL);
                  }
                }
              });
            }
          }
        });
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (!isChild) {
      presenter.getHeader().addCommandItem(new Button(Localized.dictionary().createInvoice(),
          this));
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
    Set<Long> ids = new HashSet<>();

    for (RowInfo row : getGridView().getSelectedRows(GridView.SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      getGridView().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }
    Queries.getRowSet(getViewName(), null, Filter.idIn(ids), new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet data) {
        createInvoice(data, InvoiceBuilder.this);
      }
    });
  }

  protected void createInvoice(BeeRowSet data, BiConsumer<BeeRowSet, BeeRow> consumer) {
    consumer.accept(data, RowFactory.createEmptyRow(Data.getDataInfo(getTargetView()), true));
  }

  protected abstract String getRelationColumn();

  protected abstract ParameterList getRequestArgs();

  protected abstract String getTargetView();
}
