package com.butent.bee.client.modules.trade;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.finance.FinanceConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.modules.finance.FinanceKeeper;
import com.butent.bee.client.modules.finance.OutstandingPrepaymentGrid;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.finance.PrepaymentKind;
import com.butent.bee.shared.modules.trade.DebtKind;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasStringValue;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

class PaymentForm extends AbstractFormInterceptor {

  private static final BeeLogger logger = LogUtils.getLogger(PaymentForm.class);

  private static final String STYLE_PREFIX = TradeKeeper.STYLE_PREFIX + "payment-";
  private static final String STYLE_DEBT_SUMMARY = STYLE_PREFIX + "debt-summary";
  private static final String STYLE_PREPAYMENT_SUMMARY = STYLE_PREFIX + "prepayment-summary";
  private static final String STYLE_SUMMARY_AMOUNT = STYLE_PREFIX + "summary-amount";
  private static final String STYLE_SUMMARY_CURRENCY = STYLE_PREFIX + "summary-currency";
  private static final String STYLE_UPDATING = STYLE_PREFIX + "updating";

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

  private static final String NAME_PAGES = "pages";

  private final DebtKind debtKind;
  private final boolean financeEnabled = Module.FINANCE.isEnabled();

  private final Map<String, DateTime> dateTimeValues = new HashMap<>();

  private State state;

  PaymentForm(DebtKind debtKind) {
    this.debtKind = debtKind;
  }

  @Override
  public FormInterceptor getInstance() {
    return new PaymentForm(debtKind);
  }

  @Override
  public boolean beforeCreateWidget(String name, Element description) {
    if (name != null) {
      switch (name) {
        case GRID_OUTSTANDING_PREPAYMENT_GIVEN:
        case GRID_OUTSTANDING_PREPAYMENT_RECEIVED:
          if (!financeEnabled) {
            return false;
          }
          break;
      }
    }

    return super.beforeCreateWidget(name, description);
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

        case NAME_DATE:
          if (widget instanceof InputDateTime) {
            ((InputDateTime) widget).setDate(TimeUtils.today());
          }
          break;

        case NAME_DATE_TO:
        case NAME_TERM_TO:
          if (widget instanceof Editor) {
            ((Editor) widget).addEditStopHandler(event -> maybeUpdateDateTime(name));
            ((Editor) widget).addBlurHandler(event -> maybeUpdateDateTime(name));
          }
          break;

        case NAME_PAGES:
          if (widget instanceof TabbedPages) {
            TabbedPages pages = (TabbedPages) widget;

            pages.setSummaryRenderer(GRID_TRADE_PAYABLES, summary ->
                summarizeDebts(DebtKind.PAYABLE, GRID_TRADE_PAYABLES));
            pages.setSummaryRenderer(GRID_TRADE_RECEIVABLES, summary ->
                summarizeDebts(DebtKind.RECEIVABLE, GRID_TRADE_RECEIVABLES));

            if (financeEnabled) {
              pages.setSummaryRenderer(GRID_OUTSTANDING_PREPAYMENT_GIVEN, summary ->
                  summarizePrepayments(PrepaymentKind.SUPPLIERS,
                      GRID_OUTSTANDING_PREPAYMENT_GIVEN));
              pages.setSummaryRenderer(GRID_OUTSTANDING_PREPAYMENT_RECEIVED, summary ->
                  summarizePrepayments(PrepaymentKind.CUSTOMERS,
                      GRID_OUTSTANDING_PREPAYMENT_RECEIVED));
            }
          }
          break;

        case GRID_OUTSTANDING_PREPAYMENT_GIVEN:
        case GRID_OUTSTANDING_PREPAYMENT_RECEIVED:
          if (widget instanceof GridPanel) {
            GridInterceptor interceptor = ((GridPanel) widget).getGridInterceptor();
            if (interceptor instanceof OutstandingPrepaymentGrid) {
              ((OutstandingPrepaymentGrid) interceptor).setDischarger(this::dischargePrepayments);
            }
          }
          break;

        case GRID_TRADE_PAYABLES:
        case GRID_TRADE_RECEIVABLES:
          if (widget instanceof GridPanel) {
            GridInterceptor interceptor = ((GridPanel) widget).getGridInterceptor();
            if (interceptor instanceof TradeDebtsGrid) {
              TradeDebtsGrid debtsGrid = (TradeDebtsGrid) interceptor;

              if (debtsGrid.getDebtKind() == debtKind) {
                debtsGrid.initDischarger(Localized.dictionary().pay(), this::onPay);
              } else {
                debtsGrid.initDischarger(Localized.dictionary().paymentDischargeDebt(),
                    this::dischargeDebts);
              }
            }
          }
          break;
      }
    }

    super.afterCreateWidget(name, widget, callback);
  }

  private void clearValue(String name) {
    Widget widget = getWidgetByName(name);
    if (widget instanceof Editor) {
      ((Editor) widget).clearValue();
    }
  }

  private void dischargeDebts(GridView gridView) {
    logger.debug("debts", gridView.getGridName());
  }

  private void dischargePrepayments(GridView gridView, PrepaymentKind kind) {
    logger.debug(gridView.getGridName(), kind);
  }

  private static List<? extends IsRow> filterRows(List<? extends IsRow> rows,
      int index, Long value) {

    return rows.stream()
        .filter(row -> Objects.equals(row.getLong(index), value))
        .collect(Collectors.toList());
  }

  private Double getAmount() {
    Widget widget = getWidgetByName(NAME_AMOUNT);

    if (widget instanceof InputNumber) {
      Double value = ((InputNumber) widget).getNumber();
      return BeeUtils.isDouble(value) ? normalize(value) : null;
    } else {
      return null;
    }
  }

  private DateTime getDateTime(String name) {
    Widget widget = getWidgetByName(name);

    if (widget instanceof InputDateTime) {
      return ((InputDateTime) widget).getDateTime();
    } else {
      return null;
    }
  }

  private GridView getGrid(String gridName) {
    return ViewHelper.getChildGrid(getFormView(), gridName);
  }

  private static List<? extends IsRow> getRows(GridView gridView) {
    if (gridView == null || gridView.isEmpty()) {
      return new ArrayList<>();

    } else if (gridView.hasSelection()) {
      List<IsRow> rows = new ArrayList<>();

      for (IsRow row : gridView.getRowData()) {
        if (gridView.isRowSelected(row.getId())) {
          rows.add(row);
        }
      }

      return rows;

    } else {
      return gridView.getRowData();
    }
  }

  private Long getSelectorValue(String name) {
    Widget widget = getWidgetByName(name);

    if (widget instanceof DataSelector) {
      return ((DataSelector) widget).getRelatedId();
    } else {
      return null;
    }
  }

  private String getString(String name) {
    Widget widget = getWidgetByName(name);

    if (widget instanceof HasStringValue) {
      return BeeUtils.trim(((HasStringValue) widget).getValue());
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

  private static double normalize(Double value) {
    return Localized.normalizeMoney(value);
  }

  private void onPay(GridView gridView) {
    if (isUpdating()) {
      return;
    }

    if (gridView == null || gridView.isEmpty()) {
      getFormView().notifyWarning(Localized.dictionary().noData());
      return;
    }

    final Long payer = getSelectorValue(NAME_PAYER);
    if (!DataUtils.isId(payer)) {
      notifyRequired(debtKind.getPayerLabel(Localized.dictionary()));
      focusName(NAME_PAYER);
      return;
    }

    final DateTime date = getDateTime(NAME_DATE);
    if (date == null) {
      notifyRequired(Localized.dictionary().date());
      focusName(NAME_DATE);
      return;
    }

    final Double amount = getAmount();
    if (!BeeUtils.isPositive(amount)) {
      notifyRequired(Localized.dictionary().amount());
      focusName(NAME_AMOUNT);
      return;
    }

    int currencyIndex = gridView.getDataIndex(COL_TRADE_CURRENCY);
    Long currency = getSelectorValue(NAME_CURRENCY);

    final List<? extends IsRow> rows;
    if (DataUtils.isId(currency)) {
      rows = filterRows(getRows(gridView), currencyIndex, currency);

    } else {
      rows = getRows(gridView);

      Set<Long> currencies = rows.stream()
          .map(row -> row.getLong(currencyIndex))
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());

      if (currencies.size() == 1) {
        currency = currencies.stream().findFirst().get();

      } else {
        notifyRequired(Localized.dictionary().currency());
        focusName(NAME_CURRENCY);
        return;
      }
    }

    final Long account = getSelectorValue(NAME_ACCOUNT);
    final Long paymentType = getSelectorValue(NAME_PAYMENT_TYPE);

    if (!DataUtils.isId(account) && !DataUtils.isId(paymentType)) {
      getFormView().notifyWarning(Localized.dictionary().paymentEnterAccountOrType());
      focusName(NAME_ACCOUNT);
      return;
    }

    final String series = getString(NAME_SERIES);
    final String number = getString(NAME_NUMBER);

    double debt = totalDebt(rows);

    List<String> messages = new ArrayList<>();

    if (amount > debt) {
      messages.add(BeeUtils.joinWords(Localized.dictionary().debt(),
          TradeUtils.formatAmount(debt)));
      if (financeEnabled) {
        messages.add(BeeUtils.joinWords(Localized.dictionary().prepayment(),
            TradeUtils.formatAmount(amount - debt)));
      }

      messages.add(BeeConst.STRING_EMPTY);
    }
    messages.add(Localized.dictionary().paymentSubmitQuestion());

    final Long pc = currency;

    Global.confirm(Localized.dictionary().newPayment(), Icon.QUESTION, messages,
        () -> doPay(rows, payer, date, amount, pc, account, paymentType, series, number));
  }

  private void doPay(List<? extends IsRow> rows, Long payer, DateTime date, double amount,
      Long currency, Long account, Long paymentType, String series, String number) {

    final Map<Long, Double> payments = new HashMap<>();
    double sum = BeeConst.DOUBLE_ZERO;

    for (IsRow row : rows) {
      double debt = normalize(row.getPropertyDouble(PROP_TD_DEBT));

      if (BeeUtils.isPositive(debt)) {
        debt = normalize(Math.min(debt, amount - sum));
        payments.put(row.getId(), debt);

        sum = normalize(sum + debt);
        if (sum >= amount) {
          break;
        }
      }
    }

    final double prepayment = normalize(amount - sum);

    ParameterList parameters = TradeKeeper.createArgs(SVC_SUBMIT_PAYMENT);
    if (!payments.isEmpty()) {
      parameters.addDataItem(VAR_PAYMENTS, Codec.beeSerialize(payments));
    }

    parameters.addDataItem(COL_TRADE_PAYMENT_DATE, date.getTime());

    if (DataUtils.isId(account)) {
      parameters.addDataItem(COL_TRADE_PAYMENT_ACCOUNT, account);
    }
    if (DataUtils.isId(paymentType)) {
      parameters.addDataItem(COL_TRADE_PAYMENT_TYPE, paymentType);
    }

    if (!BeeUtils.isEmpty(series)) {
      parameters.addDataItem(COL_TRADE_PAYMENT_SERIES, series);
    }
    if (!BeeUtils.isEmpty(number)) {
      parameters.addDataItem(COL_TRADE_PAYMENT_NUMBER, number);
    }

    if (financeEnabled && BeeUtils.isPositive(prepayment)) {
      parameters.addDataItem(VAR_PREPAYMENT, prepayment);
      parameters.addDataItem(VAR_KIND, debtKind.ordinal());
      parameters.addDataItem(COL_TRADE_PAYER, payer);
      parameters.addDataItem(COL_TRADE_CURRENCY, currency);
    }

    setUpdating(true);

    BeeKeeper.getRpc().makeRequest(parameters, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        setUpdating(false);

        if (!response.hasErrors()) {
          clearValue(NAME_AMOUNT);

          GridView mainDebts = getGrid(debtKind.tradeDebtsMainGrid());
          if (mainDebts != null) {
            mainDebts.getGrid().clearSelection();
            ViewHelper.refresh(mainDebts);
          }

          if (financeEnabled && BeeUtils.isPositive(prepayment)) {
            ViewHelper.refresh(getGrid(debtKind.getPrepaymentKind().tradePaymentsMainGrid()));
          }

          if (response.hasMessages()) {
            response.notify(getFormView());
          }

          if (financeEnabled && !payments.isEmpty()) {
            FinanceKeeper.postTradeDocuments(payments.keySet(), null);
          }
        }
      }
    });
  }

  private void refreshChildren() {
    Long payer = getSelectorValue(NAME_PAYER);
    Long currency = getSelectorValue(NAME_CURRENCY);

    refreshMainDebts(payer, currency);
    refreshDebts(debtKind.tradeDebtsOtherGrid(), payer, currency, null, null);

    refreshPrepayments(GRID_OUTSTANDING_PREPAYMENT_GIVEN, payer, currency);
    refreshPrepayments(GRID_OUTSTANDING_PREPAYMENT_RECEIVED, payer, currency);
  }

  private void refreshDebts(String gridName, Long payer, Long currency,
      DateTime dateTo, DateTime termTo) {

    GridView gridView = getGrid(gridName);

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
    if (financeEnabled) {
      GridView gridView = getGrid(gridName);

      if (gridView != null && gridView.getGridInterceptor() instanceof OutstandingPrepaymentGrid) {
        ((OutstandingPrepaymentGrid) gridView.getGridInterceptor()).onParentChange(payer, currency);
      }
    }
  }

  private String summarizeDebts(DebtKind kind, String gridName) {
    Map<String, Double> totals = new TreeMap<>();

    GridView gridView = getGrid(gridName);

    if (gridView != null && !gridView.isEmpty()) {
      int currencyIndex = gridView.getDataIndex(ALS_CURRENCY_NAME);

      for (IsRow row : getRows(gridView)) {
        String currencyName = row.getString(currencyIndex);
        double debt = normalize(row.getPropertyDouble(PROP_TD_DEBT));

        if (!BeeUtils.isEmpty(currencyName) && BeeUtils.nonZero(debt)) {
          totals.merge(currencyName, debt, Double::sum);
        }
      }
    }

    if (totals.isEmpty()) {
      return BeeConst.STRING_EMPTY;

    } else {
      HtmlTable table = new HtmlTable(STYLE_DEBT_SUMMARY);
      table.addStyleName(StyleUtils.joinName(STYLE_DEBT_SUMMARY, kind.getStyleSuffix()));

      int r = 0;
      for (Map.Entry<String, Double> entry : totals.entrySet()) {
        table.setText(r, 0, TradeUtils.formatAmount(entry.getValue()), STYLE_SUMMARY_AMOUNT);
        table.setText(r, 1, entry.getKey(), STYLE_SUMMARY_CURRENCY);

        r++;
      }

      return DomUtils.getOuterHtml(table.getElement());
    }
  }

  private String summarizePrepayments(PrepaymentKind kind, String gridName) {
    Map<String, Double> totals = new TreeMap<>();

    GridView gridView = getGrid(gridName);

    if (gridView != null && !gridView.isEmpty()) {
      int amountIndex = gridView.getDataIndex(COL_FIN_AMOUNT);
      int currencyIndex = gridView.getDataIndex(ALS_CURRENCY_NAME);

      for (IsRow row : getRows(gridView)) {
        double balance = normalize(BeeUtils.unbox(row.getDouble(amountIndex))
            - BeeUtils.unbox(row.getPropertyDouble(PROP_PREPAYMENT_USED)));
        String currencyName = row.getString(currencyIndex);

        if (!BeeUtils.isEmpty(currencyName) && BeeUtils.nonZero(balance)) {
          totals.merge(currencyName, balance, Double::sum);
        }
      }
    }

    if (totals.isEmpty()) {
      return BeeConst.STRING_EMPTY;

    } else {
      HtmlTable table = new HtmlTable(STYLE_PREPAYMENT_SUMMARY);
      table.addStyleName(StyleUtils.joinName(STYLE_DEBT_SUMMARY, kind.styleSuffix()));

      int r = 0;
      for (Map.Entry<String, Double> entry : totals.entrySet()) {
        table.setText(r, 0, TradeUtils.formatAmount(entry.getValue()), STYLE_SUMMARY_AMOUNT);
        table.setText(r, 1, entry.getKey(), STYLE_SUMMARY_CURRENCY);

        r++;
      }

      return DomUtils.getOuterHtml(table.getElement());
    }
  }

  private static double totalDebt(List<? extends IsRow> rows) {
    return normalize(rows.stream()
        .mapToDouble(row -> normalize(row.getPropertyDouble(PROP_TD_DEBT)))
        .filter(BeeUtils::isPositive)
        .sum());
  }

  private boolean isUpdating() {
    return state == State.UPDATING;
  }

  private void setUpdating(boolean updating) {
    this.state = updating ? State.UPDATING : null;
    getFormView().setStyleName(STYLE_UPDATING, updating);
  }
}
