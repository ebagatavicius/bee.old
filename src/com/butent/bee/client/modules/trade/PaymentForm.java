package com.butent.bee.client.modules.trade;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.modules.finance.OutstandingPrepaymentGrid;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.finance.FinanceConstants;
import com.butent.bee.shared.modules.trade.DebtKind;
import com.butent.bee.shared.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class PaymentForm extends AbstractFormInterceptor {

  private static final String NAME_PAYER_LABEL = "PayerLabel";
  private static final String NAME_PAYER = "Payer";
  private static final String NAME_DATE_TO = "DateTo";
  private static final String NAME_TERM_TO = "TermTo";

  private static final String NAME_DATE = "Date";
  private static final String NAME_AMOUNT = "Amount";
  private static final String NAME_CURRENCY = "Currency";
  private static final String NAME_ACCOUNT = "Account";
  private static final String NAME_PAYMENT_TYPE = "PaymentType";
  private static final String NAME_SERIES = "Series";
  private static final String NAME_NUMBER = "Number";

  private final DebtKind debtKind;

  private final Map<String, DateTime> dateTimeValues = new HashMap<>();

  PaymentForm(DebtKind debtKind) {
    this.debtKind = debtKind;
  }

  @Override
  public FormInterceptor getInstance() {
    return new PaymentForm(debtKind);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (name != null) {
      switch (name) {
        case NAME_PAYER_LABEL:
          if (widget instanceof HasHtml) {
            ((HasHtml) widget).setText(debtKind.getPayerLabel(Localized.dictionary()));
          }
          break;

        case NAME_PAYER:
        case NAME_CURRENCY:
          if (widget instanceof DataSelector) {
            ((DataSelector) widget).addSelectorHandler(event -> {
              if (event.isChanged()) {
                refreshChildren();
              }
            });
          }
          break;

        case NAME_DATE_TO:
        case NAME_TERM_TO:
          if (widget instanceof Editor) {
            ((Editor) widget).addEditStopHandler(event -> maybeUpdateDateTime(name));
            ((Editor) widget).addBlurHandler(event -> maybeUpdateDateTime(name));
          }
      }
    }

    super.afterCreateWidget(name, widget, callback);
  }

  private DateTime getDateTime(String name) {
    Widget widget = getFormView().getWidgetByName(name);

    if (widget instanceof InputDateTime) {
      return ((InputDateTime) widget).getDateTime();
    } else {
      return null;
    }
  }

  private Long getSelectorValue(String name) {
    Widget widget = getFormView().getWidgetByName(name);

    if (widget instanceof DataSelector) {
      return ((DataSelector) widget).getRelatedId();
    } else {
      return null;
    }
  }

  private void maybeUpdateDateTime(String name) {
    DateTime oldValue = dateTimeValues.get(name);
    DateTime newValue = getDateTime(name);

    if (!Objects.equals(oldValue, newValue)) {
      dateTimeValues.put(name, newValue);

      Long payer = getSelectorValue(NAME_PAYER);
      Long currency = getSelectorValue(NAME_CURRENCY);

      refreshMainDebts(payer, currency);
    }
  }

  private void refreshChildren() {
    Long payer = getSelectorValue(NAME_PAYER);
    Long currency = getSelectorValue(NAME_CURRENCY);

    refreshMainDebts(payer, currency);
    refreshDebts(debtKind.tradeDebtsOtherGrid(), payer, currency, null, null);

    refreshPrepayments(FinanceConstants.GRID_OUTSTANDING_PREPAYMENT_GIVEN, payer, currency);
    refreshPrepayments(FinanceConstants.GRID_OUTSTANDING_PREPAYMENT_RECEIVED, payer, currency);
  }

  private void refreshDebts(String gridName, Long payer, Long currency,
      DateTime dateTo, DateTime termTo) {

    GridView gridView = ViewHelper.getChildGrid(getFormView(), gridName);

    if (gridView != null && gridView.getGridInterceptor() instanceof TradeDebtsGrid) {
      ((TradeDebtsGrid) gridView.getGridInterceptor()).onParentChange(payer, currency,
          dateTo, termTo);
    }
  }

  private void refreshMainDebts(Long payer, Long currency) {
    DateTime dateTo = getDateTime(NAME_DATE_TO);
    DateTime termTo = getDateTime(NAME_TERM_TO);

    refreshDebts(debtKind.tradeDebtsMainGrid(), payer, currency, dateTo, termTo);
  }

  private void refreshPrepayments(String gridName, Long payer, Long currency) {
    GridView gridView = ViewHelper.getChildGrid(getFormView(), gridName);

    if (gridView != null && gridView.getGridInterceptor() instanceof OutstandingPrepaymentGrid) {
      ((OutstandingPrepaymentGrid) gridView.getGridInterceptor()).onParentChange(payer, currency);
    }
  }
}
