package com.butent.bee.client.modules.trade;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.finance.FinanceConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.TabbedPages;
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
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.finance.PrepaymentKind;
import com.butent.bee.shared.modules.trade.DebtKind;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

class PaymentForm extends AbstractFormInterceptor {

  private static final BeeLogger logger = LogUtils.getLogger(PaymentForm.class);

  private static final String STYLE_PREFIX = TradeKeeper.STYLE_PREFIX + "payment-";
  private static final String STYLE_DEBT_SUMMARY = STYLE_PREFIX + "debt-summary";
  private static final String STYLE_PREPAYMENT_SUMMARY = STYLE_PREFIX + "prepayment-summary";
  private static final String STYLE_SUMMARY_AMOUNT = STYLE_PREFIX + "summary-amount";
  private static final String STYLE_SUMMARY_CURRENCY = STYLE_PREFIX + "summary-currency";

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
          break;

        case NAME_PAGES:
          if (widget instanceof TabbedPages) {
            TabbedPages pages = (TabbedPages) widget;

            pages.setSummaryRenderer(GRID_TRADE_PAYABLES, summary ->
                summarizeDebts(DebtKind.PAYABLE, GRID_TRADE_PAYABLES));
            pages.setSummaryRenderer(GRID_TRADE_RECEIVABLES, summary ->
                summarizeDebts(DebtKind.RECEIVABLE, GRID_TRADE_RECEIVABLES));

            pages.setSummaryRenderer(GRID_OUTSTANDING_PREPAYMENT_GIVEN, summary ->
                summarizePrepayments(PrepaymentKind.SUPPLIERS, GRID_OUTSTANDING_PREPAYMENT_GIVEN));
            pages.setSummaryRenderer(GRID_OUTSTANDING_PREPAYMENT_RECEIVED, summary ->
                summarizePrepayments(PrepaymentKind.CUSTOMERS,
                    GRID_OUTSTANDING_PREPAYMENT_RECEIVED));
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

  private void dischargeDebts(GridView gridView) {
    logger.debug("debts", gridView.getGridName());
  }

  private void dischargePrepayments(GridView gridView, PrepaymentKind kind) {
    logger.debug(gridView.getGridName(), kind);
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

  private void onPay(GridView gridView) {
    logger.debug("pay", gridView.getGridName());
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

  private String summarizeDebts(DebtKind kind, String gridName) {
    Map<String, Double> totals = new TreeMap<>();

    GridView gridView = ViewHelper.getChildGrid(getFormView(), gridName);

    if (gridView != null && !gridView.isEmpty()) {
      boolean hasSelection = gridView.hasSelection();
      int currencyIndex = gridView.getDataIndex(ALS_CURRENCY_NAME);

      for (IsRow row : gridView.getRowData()) {
        if (!hasSelection || gridView.isRowSelected(row.getId())) {
          String currencyName = row.getString(currencyIndex);
          Double debt = row.getPropertyDouble(PROP_TD_DEBT);

          if (!BeeUtils.isEmpty(currencyName) && BeeUtils.nonZero(debt)) {
            totals.merge(currencyName, debt, Double::sum);
          }
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

    GridView gridView = ViewHelper.getChildGrid(getFormView(), gridName);

    if (gridView != null && !gridView.isEmpty()) {
      boolean hasSelection = gridView.hasSelection();

      int amountIndex = gridView.getDataIndex(COL_FIN_AMOUNT);
      int currencyIndex = gridView.getDataIndex(ALS_CURRENCY_NAME);

      for (IsRow row : gridView.getRowData()) {
        if (!hasSelection || gridView.isRowSelected(row.getId())) {
          double debt = BeeUtils.unbox(row.getDouble(amountIndex))
              - BeeUtils.unbox(row.getPropertyDouble(PROP_PREPAYMENT_USED));
          String currencyName = row.getString(currencyIndex);

          if (!BeeUtils.isEmpty(currencyName) && BeeUtils.nonZero(debt)) {
            totals.merge(currencyName, debt, Double::sum);
          }
        }
      }
    }

    if (totals.isEmpty()) {
      return BeeConst.STRING_EMPTY;

    } else {
      HtmlTable table = new HtmlTable(STYLE_PREPAYMENT_SUMMARY);
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
}
