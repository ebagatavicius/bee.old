package com.butent.bee.client.modules.transport;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.output.PrintFormInterceptor;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

public class CargoPurchaseInvoiceForm extends PrintFormInterceptor {

  private ScheduledCommand refresher;

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    int idx = form.getDataIndex(COL_SALE);
    boolean credit = idx != BeeConst.UNDEF && DataUtils.isId(row.getLong(idx));

    form.getViewPresenter().getHeader().setCaption(credit
        ? Localized.getConstants().trCreditInvoice()
        : Localized.getConstants().trPurchaseInvoice());
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid) {
      ChildGrid grid = (ChildGrid) widget;

      if (BeeUtils.same(name, getTradeItemsName())) {
        grid.setGridInterceptor(new InvoiceItemsGrid(getRefresher()));

      } else if (BeeUtils.inListSame(name, VIEW_CARGO_PURCHASES, VIEW_CARGO_SALES)) {
        /* Kill default interceptor */
        grid.setGridInterceptor(new AbstractGridInterceptor() {
          @Override
          public GridInterceptor getInstance() {
            return null;
          }
        });
      }
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new CargoPurchaseInvoiceForm();
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new PrintInvoiceInterceptor();
  }

  protected String getTradeItemsName() {
    return TBL_PURCHASE_ITEMS;
  }

  private ScheduledCommand getRefresher() {
    if (refresher == null) {
      refresher = new ScheduledCommand() {
        @Override
        public void execute() {
          final FormView form = getFormView();

          Queries.getRow(form.getViewName(), form.getActiveRowId(), new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              RowUpdateEvent.fire(BeeKeeper.getBus(), form.getViewName(), result);
            }
          });
        }
      };
    }
    return refresher;
  }
}
