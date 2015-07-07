package com.butent.bee.client.modules.transport;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.modules.transport.TransportHandler.Profit;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class TransportationOrderForm extends AbstractFormInterceptor implements ClickHandler {

  @Override
  public FormInterceptor getInstance() {
    return new TransportationOrderForm();
  }

  @Override
  public void onClick(ClickEvent event) {
    final FormView form = getFormView();
    long orderId = form.getActiveRowId();

    if (!DataUtils.isId(orderId)) {
      return;
    }
    ParameterList args = TransportHandler.createArgs(SVC_GET_UNASSIGNED_CARGOS);
    args.addDataItem(COL_ORDER, orderId);

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          response.notify(form);
          return;
        }
        String[] cargos = Codec.beeDeserializeCollection(response.getResponseAsString());

        if (ArrayUtils.isEmpty(cargos)) {
          form.notifyWarning(Localized.getMessages()
              .dataNotAvailable(Localized.getConstants().cargos()));
          return;
        }
        TripSelector.select(cargos, null, form.getElement());
      }
    });
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    Long customer = null;
    List<BeeColumn> cols = event.getColumns();

    for (int i = 0; i < cols.size(); i++) {
      if (BeeUtils.inListSame(cols.get(i).getId(), COL_CUSTOMER, COL_PAYER)) {
        customer = BeeUtils.toLongOrNull(event.getValues().get(i));

        if (BeeUtils.same(cols.get(i).getId(), COL_PAYER)) {
          break;
        }
      }
    }
    if (DataUtils.isId(customer)) {
      event.consume();
      checkCreditInfo(listener, event, customer);
    }
  }

  @Override
  public void onSaveChanges(HasHandlers listener, final SaveChangesEvent event) {
    Long customer = null;
    int custIdx = -1;
    int payerIdx = -1;
    List<BeeColumn> cols = event.getColumns();

    for (int i = 0; i < cols.size(); i++) {
      if (BeeUtils.same(cols.get(i).getId(), COL_CUSTOMER)) {
        custIdx = i;
      } else if (BeeUtils.same(cols.get(i).getId(), COL_PAYER)) {
        payerIdx = i;
      }
    }
    Long cust = Data.getLong(getViewName(), event.getNewRow(), COL_CUSTOMER);
    Long payer = Data.getLong(getViewName(), event.getNewRow(), COL_PAYER);

    if (BeeUtils.isNonNegative(payerIdx)
        || !DataUtils.isId(payer) && BeeUtils.isNonNegative(custIdx)) {
      customer = BeeUtils.nvl(payer, cust);
    }
    if (DataUtils.isId(customer)) {
      event.consume();
      checkCreditInfo(listener, event, customer);
    }
  }

  @Override
  public boolean onStartEdit(FormView form, final IsRow row, ScheduledCommand focusCommand) {
    HeaderView hdr = form.getViewPresenter().getHeader();
    hdr.clearCommandPanel();

    if (Data.isViewEditable(VIEW_CARGO_INVOICES)) {
      hdr.addCommandItem(new InvoiceCreator(VIEW_CARGO_SALES,
          Filter.equals(COL_ORDER, row.getId())));
    }
    if (Data.isViewEditable(VIEW_CARGO_TRIPS)) {
      Image button = new Image(Global.getImages().silverTruck());
      button.setTitle(Localized.getConstants().trAssignTrip());
      button.setAlt(button.getTitle());
      button.addClickHandler(this);

      hdr.addCommandItem(button);
    }
    hdr.addCommandItem(new Profit(COL_ORDER, row.getId()));

    return true;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    form.getViewPresenter().getHeader().clearCommandPanel();
  }

  private void checkCreditInfo(final HasHandlers listener, final GwtEvent<?> event, Long customer) {
    ParameterList args = TransportHandler.createArgs(SVC_GET_CREDIT_INFO);
    args.addDataItem(ClassifierConstants.COL_COMPANY, customer);

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getFormView());

        if (response.hasErrors()) {
          return;
        }
        Map<String, String> result = Codec.deserializeMap(response.getResponseAsString());

        double limit = BeeUtils.toDouble(result.get(ClassifierConstants.COL_COMPANY_CREDIT_LIMIT));
        double debt = BeeUtils.toDouble(result.get(TradeConstants.VAR_DEBT));
        double overdue = BeeUtils.toDouble(result.get(TradeConstants.VAR_OVERDUE));
        double income = BeeUtils.toDouble(result.get(VAR_INCOME));

        if (overdue > 0 || (debt + income) > limit) {
          String cap = result.get(ClassifierConstants.COL_COMPANY_NAME);
          List<String> msgs = new ArrayList<>();

          msgs.add(BeeUtils.join(": ", Localized.getConstants().creditLimit(),
              BeeUtils.joinWords(limit, result.get(AdministrationConstants.COL_CURRENCY))));
          msgs.add(BeeUtils.join(": ", Localized.getConstants().trdDebt(), debt));

          if (overdue > 0) {
            msgs.add(BeeUtils.join(": ", Localized.getConstants().trdOverdue(), overdue));
          }
          if (income > 0) {
            msgs.add(BeeUtils.join(": ", Localized.getConstants().trOrders(), income));
          }
          Global.confirm(cap, null, msgs, Localized.getConstants().ok(),
              Localized.getConstants().cancel(), new ConfirmationCallback() {
                @Override
                public void onConfirm() {
                  listener.fireEvent(event);
                }
              });
        } else {
          listener.fireEvent(event);
        }
      }
    });
  }
}