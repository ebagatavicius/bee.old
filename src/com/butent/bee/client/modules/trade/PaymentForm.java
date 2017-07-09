package com.butent.bee.client.modules.trade;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.ALS_CURRENCY_NAME;
import static com.butent.bee.shared.modules.finance.FinanceConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
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
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.State;
import com.butent.bee.shared.Triplet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowPredicate;
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
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
              ((OutstandingPrepaymentGrid) interceptor).setDischarger(this::onDischargePrepayments);
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
                debtsGrid.initDischarger(Localized.dictionary().paymentDischargeDebtCommand(),
                    this::onDischargeDebts);
              }
            }
          }
          break;
      }
    }

    super.afterCreateWidget(name, widget, callback);
  }

  private DateTime checkDate() {
    DateTime date = getDateTime(NAME_DATE);

    if (date == null) {
      notifyRequired(Localized.dictionary().date());
      focusName(NAME_DATE);
    }
    return date;
  }

  private Long checkPayer() {
    Long payer = getSelectorValue(NAME_PAYER);

    if (!DataUtils.isId(payer)) {
      notifyRequired(debtKind.getPayerLabel(Localized.dictionary()));
      focusName(NAME_PAYER);
    }
    return payer;
  }

  private void clearValue(String name) {
    Widget widget = getWidgetByName(name);
    if (widget instanceof Editor) {
      ((Editor) widget).clearValue();
    }
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

  private Pair<Long, List<IsRow>> getCurrencyAndRows(GridView gridView,
      String currencyColumn, RowPredicate predicate) {

    Long currency = getSelectorValue(NAME_CURRENCY);
    List<IsRow> rows;

    int currencyIndex = gridView.getDataIndex(currencyColumn);

    if (DataUtils.isId(currency)) {
      rows = getRows(gridView,
          RowPredicate.and(predicate, RowPredicate.equals(currencyIndex, currency)));

    } else {
      rows = getRows(gridView, predicate);

      Set<Long> currencies = rows.stream()
          .map(row -> row.getLong(currencyIndex))
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());

      if (currencies.size() == 1) {
        currency = currencies.stream().findFirst().get();

      } else {
        notifyRequired(Localized.dictionary().currency());
        focusName(NAME_CURRENCY);
        return null;
      }
    }

    return Pair.of(currency, rows);
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

  private static List<IsRow> getRows(GridView gridView, RowPredicate predicate) {
    if (gridView == null || gridView.isEmpty()) {
      return new ArrayList<>();
    }

    boolean hasSelection = gridView.hasSelection();

    if (hasSelection || predicate != null) {
      RowPredicate rp = RowPredicate.and(
          hasSelection ? row -> gridView.isRowSelected(row.getId()) : null, predicate);

      return gridView.getRowData().stream().filter(rp).collect(Collectors.toList());

    } else {
      return new ArrayList<>(gridView.getRowData());
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
      warn(Localized.dictionary().noData());
      return;
    }

    Long payer = checkPayer();
    if (!DataUtils.isId(payer)) {
      return;
    }

    DateTime date = checkDate();
    if (date == null) {
      return;
    }

    Double amount = getAmount();
    if (!BeeUtils.isPositive(amount)) {
      notifyRequired(Localized.dictionary().amount());
      focusName(NAME_AMOUNT);
      return;
    }

    Pair<Long, List<IsRow>> cr = getCurrencyAndRows(gridView, COL_TRADE_CURRENCY, hasDebt());
    if (cr == null) {
      return;
    }

    Long currency = cr.getA();
    List<IsRow> rows = cr.getB();

    Long account = getSelectorValue(NAME_ACCOUNT);
    Long paymentType = getSelectorValue(NAME_PAYMENT_TYPE);

    if (!DataUtils.isId(account) && !DataUtils.isId(paymentType)) {
      warn(Localized.dictionary().paymentEnterAccountOrType());
      focusName(NAME_ACCOUNT);
      return;
    }

    String series = getString(NAME_SERIES);
    String number = getString(NAME_NUMBER);

    double debt = totalDebt(rows);
    String currencyName = getCurrencyName(rows, gridView.getDataIndex(ALS_CURRENCY_NAME));

    List<String> messages = new ArrayList<>();

    if (amount > debt) {
      messages.add(message(Localized.dictionary().debt(), debt, currencyName));
      if (financeEnabled) {
        messages.add(message(Localized.dictionary().prepayment(), amount - debt, currencyName));
      }

    } else {
      messages.add(message(Localized.dictionary().amount(), amount, currencyName));
    }

    messages.add(BeeConst.STRING_EMPTY);
    messages.add(Localized.dictionary().paymentSubmitQuestion());

    Global.confirm(Localized.dictionary().newPayment(), Icon.QUESTION, messages,
        () -> doPay(rows, payer, date, amount, currency, account, paymentType, series, number));
  }

  private void doPay(List<IsRow> rows, Long payer, DateTime date, double amount,
      Long currency, Long account, Long paymentType, String series, String number) {

    Map<Long, Double> payments = new HashMap<>();
    double sum = BeeConst.DOUBLE_ZERO;

    for (IsRow row : rows) {
      double debt = getDebt(row);

      if (BeeUtils.isPositive(debt)) {
        debt = normalize(Math.min(debt, amount - sum));
        payments.put(row.getId(), debt);

        sum = normalize(sum + debt);
        if (sum >= amount) {
          break;
        }
      }
    }

    double prepayment = normalize(amount - sum);

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

    BeeKeeper.getRpc().makeRequest(parameters, response -> {
      setUpdating(false);

      if (response.hasMessages()) {
        response.notify(getFormView());
      }

      if (!response.hasErrors()) {
        clearValue(NAME_AMOUNT);

        resetGrid(debtKind.tradeDebtsMainGrid());

        if (financeEnabled && BeeUtils.isPositive(prepayment)) {
          ViewHelper.refresh(getGrid(debtKind.getPrepaymentKind().tradePaymentsGrid()));
        }

        if (financeEnabled && !payments.isEmpty()) {
          FinanceKeeper.postTradeDocuments(payments.keySet(), null);
        }
      }
    });
  }

  private void onDischargeDebts(GridView otherGrid) {
    if (isUpdating()) {
      return;
    }

    if (otherGrid == null || otherGrid.isEmpty()) {
      warn(debtKind.opposite().getCaption(), Localized.dictionary().noData());
      return;
    }

    GridView mainGrid = getGrid(debtKind.tradeDebtsMainGrid());
    if (mainGrid == null || mainGrid.isEmpty()) {
      warn(debtKind.getCaption(), Localized.dictionary().noData());
      return;
    }

    if (!DataUtils.isId(checkPayer())) {
      return;
    }

    DateTime date = checkDate();
    if (date == null) {
      return;
    }

    Pair<Long, List<IsRow>> cr = getCurrencyAndRows(otherGrid, COL_TRADE_CURRENCY, hasDebt());
    if (cr == null) {
      return;
    }

    Long currency = cr.getA();
    List<IsRow> otherRows = cr.getB();

    if (BeeUtils.isEmpty(otherRows)) {
      warn(debtKind.opposite().getCaption(), Localized.dictionary().noData());
      return;
    }

    String currencyName = getCurrencyName(otherRows, otherGrid.getDataIndex(ALS_CURRENCY_NAME));

    List<IsRow> mainRows = getRows(mainGrid, RowPredicate.and(hasDebt(),
        RowPredicate.equals(mainGrid.getDataIndex(COL_TRADE_CURRENCY), currency)));
    if (BeeUtils.isEmpty(mainRows)) {
      warn(debtKind.getCaption(), currencyName, Localized.dictionary().noData());
      return;
    }

    double mainDebt = totalDebt(mainRows);
    double otherDebt = totalDebt(otherRows);

    Double amount = getAmount();
    double discharge;

    if (BeeUtils.isPositive(amount)) {
      discharge = Math.min(amount, Math.min(mainDebt, otherDebt));
    } else {
      discharge = Math.min(mainDebt, otherDebt);
    }

    String series = getString(NAME_SERIES);
    String number = getString(NAME_NUMBER);

    String mainMessage = message(debtKind.getCaption(), mainDebt, currencyName);
    String otherMessage = message(debtKind.opposite().getCaption(), otherDebt, currencyName);
    String amountMessage = message(Localized.dictionary().paymentDischargeAmount(),
        discharge, currencyName);

    if (!BeeUtils.isPositive(discharge)) {
      warn(mainMessage, otherMessage, amountMessage);
      return;
    }

    List<String> messages = new ArrayList<>();

    messages.add(mainMessage);
    messages.add(otherMessage);
    messages.add(BeeConst.STRING_EMPTY);
    messages.add(amountMessage);

    messages.add(BeeConst.STRING_EMPTY);
    messages.add(Localized.dictionary().paymentDischargeDebtQuestion());

    Global.confirm(Localized.dictionary().paymentDischargeDebtCaption(), Icon.QUESTION, messages,
        () -> doDischargeDebts(date, discharge, series, number, mainRows, otherRows));
  }

  private void doDischargeDebts(DateTime date, double amount, String series, String number,
      List<IsRow> mainRows, List<IsRow> otherRows) {

    List<Triplet<Long, Long, Double>> discharges = new ArrayList<>();

    Map<Long, Double> mainDebts = new HashMap<>();
    mainRows.forEach(row -> mainDebts.put(row.getId(), getDebt(row)));

    double remaining = amount;
    int mainIndex = 0;

    for (IsRow otherRow : otherRows) {
      double otherDebt = getDebt(otherRow);

      while (BeeUtils.isPositive(otherDebt) && BeeUtils.isPositive(remaining)
          && BeeUtils.isIndex(mainRows, mainIndex)) {

        long mainId = mainRows.get(mainIndex).getId();
        double mainDebt = mainDebts.get(mainId);

        if (BeeUtils.isPositive(mainDebt)) {
          double x = Math.min(remaining, Math.min(mainDebt, otherDebt));

          discharges.add(Triplet.of(mainId, otherRow.getId(), x));

          remaining = normalize(remaining - x);
          otherDebt = normalize(otherDebt - x);

          mainDebt = normalize(mainDebt - x);
          mainDebts.put(mainId, mainDebt);

          if (!BeeUtils.isPositive(mainDebt)) {
            mainIndex++;
          }

        } else {
          mainIndex++;
        }
      }

      if (!BeeUtils.isPositive(remaining) || !BeeUtils.isIndex(mainRows, mainIndex)) {
        break;
      }
    }

    if (discharges.isEmpty()) {
      String message = "cannot discharge debts";
      logger.severe(message, amount);
      logger.severe(mainRows);
      logger.severe(otherRows);

      warn(message);
      return;
    }

    ParameterList parameters = TradeKeeper.createArgs(SVC_DISCHARGE_DEBT);

    parameters.addDataItem(VAR_PAYMENTS, Codec.beeSerialize(discharges));
    parameters.addDataItem(COL_TRADE_PAYMENT_DATE, date.getTime());

    if (!BeeUtils.isEmpty(series)) {
      parameters.addDataItem(COL_TRADE_PAYMENT_SERIES, series);
    }
    if (!BeeUtils.isEmpty(number)) {
      parameters.addDataItem(COL_TRADE_PAYMENT_NUMBER, number);
    }

    setUpdating(true);

    BeeKeeper.getRpc().makeRequest(parameters, response -> {
      setUpdating(false);

      if (response.hasMessages()) {
        response.notify(getFormView());
      }

      if (!response.hasErrors()) {
        clearValue(NAME_AMOUNT);

        resetGrid(debtKind.tradeDebtsMainGrid());
        resetGrid(debtKind.tradeDebtsOtherGrid());

        Set<Long> docIds = new HashSet<>();
        discharges.forEach(triplet -> {
          docIds.add(triplet.getA());
          docIds.add(triplet.getB());
        });

        FinanceKeeper.postTradeDocuments(docIds, null);
      }
    });
  }

  private void onDischargePrepayments(GridView prepaymentGrid, PrepaymentKind prepaymentKind) {
    if (isUpdating()) {
      return;
    }

    if (prepaymentGrid == null || prepaymentGrid.isEmpty()) {
      warn(prepaymentKind.getFullCaption(Localized.dictionary()), Localized.dictionary().noData());
      return;
    }
    if (prepaymentKind == null) {
      warn(Localized.dictionary().parameterNotFound(NameUtils.getClassName(PrepaymentKind.class)));
    }

    GridView debtGrid = getGrid(prepaymentKind.getDebtKInd().tradeDebtsMainGrid());
    if (debtGrid == null || debtGrid.isEmpty()) {
      warn(prepaymentKind.getDebtKInd().getCaption(), Localized.dictionary().noData());
      return;
    }

    if (!DataUtils.isId(checkPayer())) {
      return;
    }

    DateTime date = checkDate();
    if (date == null) {
      return;
    }

    Pair<Long, List<IsRow>> cr = getCurrencyAndRows(prepaymentGrid, COL_FIN_CURRENCY,
        isOutstanding(prepaymentGrid));
    if (cr == null) {
      return;
    }

    Long currency = cr.getA();
    List<IsRow> prepaymentRows = cr.getB();

    if (BeeUtils.isEmpty(prepaymentRows)) {
      warn(prepaymentKind.getFullCaption(Localized.dictionary()), Localized.dictionary().noData());
      return;
    }

    String currencyName = getCurrencyName(prepaymentRows,
        prepaymentGrid.getDataIndex(ALS_CURRENCY_NAME));

    List<IsRow> debtRows = getRows(debtGrid, RowPredicate.and(hasDebt(),
        RowPredicate.equals(debtGrid.getDataIndex(COL_TRADE_CURRENCY), currency)));
    if (BeeUtils.isEmpty(debtRows)) {
      warn(prepaymentKind.getDebtKInd().getCaption(), currencyName,
          Localized.dictionary().noData());
      return;
    }

    int amountIndex = prepaymentGrid.getDataIndex(COL_FIN_AMOUNT);

    double prepayment = totalPrepayment(prepaymentRows, amountIndex);
    double debt = totalDebt(debtRows);

    Double amount = getAmount();
    double discharge;

    if (BeeUtils.isPositive(amount)) {
      discharge = Math.min(amount, Math.min(prepayment, debt));
    } else {
      discharge = Math.min(prepayment, debt);
    }

    String prepaymentMessage = message(Localized.dictionary().prepayment(), prepayment,
        currencyName);
    String debtMessage = message(Localized.dictionary().debt(), debt, currencyName);
    String amountMessage = message(Localized.dictionary().paymentDischargeAmount(),
        discharge, currencyName);

    if (!BeeUtils.isPositive(discharge)) {
      warn(prepaymentMessage, debtMessage, amountMessage);
      return;
    }

    List<String> messages = new ArrayList<>();

    messages.add(prepaymentMessage);
    messages.add(debtMessage);
    messages.add(BeeConst.STRING_EMPTY);
    messages.add(amountMessage);

    messages.add(BeeConst.STRING_EMPTY);
    messages.add(Localized.dictionary().paymentDischargePrepaymentQuestion());

    Global.confirm(prepaymentKind.getFullCaption(Localized.dictionary()), Icon.QUESTION, messages,
        () -> doDischargePrepayments(prepaymentKind, date, discharge,
            prepaymentGrid, prepaymentRows, amountIndex, debtGrid, debtRows));
  }

  private void doDischargePrepayments(PrepaymentKind prepaymentKind, DateTime date, double amount,
      GridView prepaymentGrid, List<IsRow> prepaymentRows, int amountIndex,
      GridView debtGrid, List<IsRow> debtRows) {

    List<Triplet<Long, Long, Double>> discharges = new ArrayList<>();

    Map<Long, Double> debts = new HashMap<>();
    debtRows.forEach(row -> debts.put(row.getId(), getDebt(row)));

    double remaining = amount;
    int debtIndex = 0;

    for (IsRow prepaymentRow : prepaymentRows) {
      double prepayment = getPrepaymentBalance(prepaymentRow, amountIndex);

      while (BeeUtils.isPositive(prepayment) && BeeUtils.isPositive(remaining)
          && BeeUtils.isIndex(debtRows, debtIndex)) {

        long debtId = debtRows.get(debtIndex).getId();
        double debt = debts.get(debtId);

        if (BeeUtils.isPositive(debt)) {
          double x = Math.min(remaining, Math.min(prepayment, debt));

          discharges.add(Triplet.of(prepaymentRow.getId(), debtId, x));

          remaining = normalize(remaining - x);
          prepayment = normalize(prepayment - x);

          debt = normalize(debt - x);
          debts.put(debtId, debt);

          if (!BeeUtils.isPositive(debt)) {
            debtIndex++;
          }

        } else {
          debtIndex++;
        }
      }

      if (!BeeUtils.isPositive(remaining) || !BeeUtils.isIndex(debtRows, debtIndex)) {
        break;
      }
    }

    if (discharges.isEmpty()) {
      String message = "cannot discharge prepayments";
      logger.severe(message, amount);
      logger.severe(prepaymentRows);
      logger.severe(debtRows);

      warn(message);
      return;
    }

    ParameterList parameters = TradeKeeper.createArgs(SVC_DISCHARGE_PREPAYMENT);

    parameters.addDataItem(VAR_KIND, prepaymentKind.ordinal());
    parameters.addDataItem(VAR_PREPAYMENT, Codec.beeSerialize(discharges));
    parameters.addDataItem(COL_TRADE_PAYMENT_DATE, date.getTime());

    setUpdating(true);

    BeeKeeper.getRpc().makeRequest(parameters, response -> {
      setUpdating(false);

      if (response.hasMessages()) {
        response.notify(getFormView());
      }

      if (!response.hasErrors()) {
        clearValue(NAME_AMOUNT);

        prepaymentGrid.getGrid().clearSelection();
        ViewHelper.refresh(prepaymentGrid);

        debtGrid.getGrid().clearSelection();
        ViewHelper.refresh(debtGrid);

        Set<Long> docIds = discharges.stream().map(Triplet::getB).collect(Collectors.toSet());
        FinanceKeeper.postTradeDocuments(docIds, null);
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

      for (IsRow row : getRows(gridView, null)) {
        String currencyName = row.getString(currencyIndex);
        double debt = getDebt(row);

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
        table.setText(r, 0, format(entry.getValue()), STYLE_SUMMARY_AMOUNT);
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

      for (IsRow row : getRows(gridView, null)) {
        double balance = getPrepaymentBalance(row, amountIndex);
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
        table.setText(r, 0, format(entry.getValue()), STYLE_SUMMARY_AMOUNT);
        table.setText(r, 1, entry.getKey(), STYLE_SUMMARY_CURRENCY);

        r++;
      }

      return DomUtils.getOuterHtml(table.getElement());
    }
  }

  private static double totalDebt(List<IsRow> rows) {
    return normalize(rows.stream()
        .mapToDouble(PaymentForm::getDebt)
        .filter(BeeUtils::isPositive)
        .sum());
  }

  private static double totalPrepayment(List<IsRow> rows, int amountIndex) {
    return normalize(rows.stream()
        .mapToDouble(row -> getPrepaymentBalance(row, amountIndex))
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

  private static RowPredicate hasDebt() {
    return row -> BeeUtils.isPositive(getDebt(row));
  }

  private static RowPredicate isOutstanding(GridView gridView) {
    int amountIndex = gridView.getDataIndex(COL_FIN_AMOUNT);
    return row -> BeeUtils.isPositive(getPrepaymentBalance(row, amountIndex));
  }

  private static String getCurrencyName(List<IsRow> rows, int index) {
    if (BeeUtils.isEmpty(rows)) {
      return null;
    } else {
      return rows.stream().findFirst().get().getString(index);
    }
  }

  private static double getDebt(IsRow row) {
    return normalize(row.getPropertyDouble(PROP_TD_DEBT));
  }

  private static double getPrepaymentBalance(IsRow row, int amountIndex) {
    return normalize(BeeUtils.unbox(row.getDouble(amountIndex))
        - BeeUtils.unbox(row.getPropertyDouble(PROP_PREPAYMENT_USED)));
  }

  private void warn(String... messages) {
    if (getFormView() != null) {
      getFormView().notifyWarning(messages);
    }
  }

  private static String format(Double value) {
    return BeeUtils.isDouble(value)
        ? Format.getDefaultMoneyFormat().format(value) : BeeConst.STRING_ZERO;
  }

  private static String message(String label, Double amount, String currency) {
    return BeeUtils.joinWords(label + BeeConst.STRING_COLON, format(amount), currency);
  }

  private void resetGrid(String gridName) {
    GridView gridView = getGrid(gridName);

    if (gridView != null) {
      gridView.getGrid().clearSelection();
      ViewHelper.refresh(gridView);
    }
  }
}
