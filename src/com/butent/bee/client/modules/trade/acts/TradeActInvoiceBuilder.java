package com.butent.bee.client.modules.trade.acts;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.ClientDefaults;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.modules.trade.acts.TradeActTimeUnit;
import com.butent.bee.shared.modules.trade.acts.TradeActUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TradeActInvoiceBuilder extends AbstractFormInterceptor implements
    SelectorEvent.Handler {

  private static final class Act implements HasEnabled {

    private final BeeRow row;

    private final Range<DateTime> range;

    private final Long currency;
    private final String currencyName;

    private final List<Range<Integer>> invoiceDays = new ArrayList<>();

    private boolean enabled = true;

    private Act(BeeRow row, Range<DateTime> range, Long currency, String currencyName) {
      this.row = row;

      this.range = range;

      this.currency = currency;
      this.currencyName = currencyName;
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    private boolean hasInvoices(Range<Integer> days) {
      if (invoiceDays.isEmpty() || days == null) {
        return false;
      }

      for (Range<Integer> r : invoiceDays) {
        if (r.encloses(days)) {
          return true;
        }
      }
      return false;
    }

    private long id() {
      return row.getId();
    }

    private Double itemTotal() {
      String value = row.getProperty(PRP_ITEM_TOTAL);

      if (BeeUtils.isEmpty(value)) {
        return null;

      } else {
        double total = BeeUtils.toDouble(value);

        String returned = row.getProperty(PRP_RETURNED_TOTAL);
        if (!BeeUtils.isEmpty(returned)) {
          total -= BeeUtils.toDouble(returned);
        }

        return total;
      }
    }

    private JustDate lastInvoiceTo() {
      if (invoiceDays.isEmpty()) {
        return null;
      }

      int last = 0;
      for (Range<Integer> r : invoiceDays) {
        last = Math.max(last, r.upperEndpoint());
      }

      return new JustDate(last);
    }
  }

  private static final class Service {

    private final BeeRow row;

    private final Range<DateTime> range;

    private final TradeActTimeUnit timeUnit;

    private Double quantity;

    private Double tariff;
    private Double price;

    private Double factor;
    private Integer dpw;

    private Integer minTerm;

    private Double discount;
    private Double vatPercent;

    private Long currency;

    private Service(BeeRow row, Range<DateTime> range, TradeActTimeUnit timeUnit) {
      this.row = row;
      this.range = range;
      this.timeUnit = timeUnit;
    }

    private Double amount(Long toCurrency, DateTime date) {
      Double amount = TradeActUtils.serviceAmount(quantity, price, discount, timeUnit, factor);

      if (BeeUtils.nonZero(amount) && Money.canExchange(currency, toCurrency)) {
        return Money.exchange(currency, toCurrency, amount, date);
      } else {
        return amount;
      }
    }

    private long id() {
      return row.getId();
    }

    private DateTime dateFrom() {
      if (range != null && range.hasLowerBound()) {
        return range.lowerEndpoint();
      } else {
        return null;
      }
    }

    private DateTime dateTo() {
      if (range != null && range.hasUpperBound()) {
        return range.upperEndpoint();
      } else {
        return null;
      }
    }
  }

  private static final String STYLE_COMMAND_PREFIX = TradeActKeeper.STYLE_PREFIX
      + "invoice-command-";

  private static final String STYLE_COMMAND_COMPOSE = STYLE_COMMAND_PREFIX + "compose";
  private static final String STYLE_COMMAND_SAVE = STYLE_COMMAND_PREFIX + "save";
  private static final String STYLE_COMMAND_DISABLED = STYLE_COMMAND_PREFIX + "disabled";

  private static final String STYLE_ACT_PREFIX = TradeActKeeper.STYLE_PREFIX
      + "invoice-act-";

  private static final String STYLE_ACT_TABLE = STYLE_ACT_PREFIX + "table";

  private static final String STYLE_ACT_HEADER = STYLE_ACT_PREFIX + "header";
  private static final String STYLE_ACT_ROW = STYLE_ACT_PREFIX + "row";
  private static final String STYLE_ACT_SELECTED = STYLE_ACT_PREFIX + "selected";

  private static final String STYLE_ACT_TOGGLE_PREFIX = STYLE_ACT_PREFIX + "toggle-";
  private static final String STYLE_ACT_TOGGLE_WIDGET = STYLE_ACT_PREFIX + "toggle";

  private static final String STYLE_ACT_ID_PREFIX = STYLE_ACT_PREFIX + "id-";
  private static final String STYLE_ACT_NAME_PREFIX = STYLE_ACT_PREFIX + "name-";
  private static final String STYLE_ACT_DATE_PREFIX = STYLE_ACT_PREFIX + "date-";
  private static final String STYLE_ACT_UNTIL_PREFIX = STYLE_ACT_PREFIX + "until-";
  private static final String STYLE_ACT_SERIES_PREFIX = STYLE_ACT_PREFIX + "series-";
  private static final String STYLE_ACT_NUMBER_PREFIX = STYLE_ACT_PREFIX + "number-";
  private static final String STYLE_ACT_OPERATION_PREFIX = STYLE_ACT_PREFIX + "operation-";
  private static final String STYLE_ACT_STATUS_PREFIX = STYLE_ACT_PREFIX + "status-";
  private static final String STYLE_ACT_OBJECT_PREFIX = STYLE_ACT_PREFIX + "object-";
  private static final String STYLE_ACT_TOTAL_PREFIX = STYLE_ACT_PREFIX + "total-";
  private static final String STYLE_ACT_CURRENCY_PREFIX = STYLE_ACT_PREFIX + "currency-";
  private static final String STYLE_ACT_LATEST_PREFIX = STYLE_ACT_PREFIX + "latest-";

  private static final String STYLE_SVC_PREFIX = TradeActKeeper.STYLE_PREFIX
      + "invoice-service-";

  private static final String STYLE_SVC_TABLE = STYLE_SVC_PREFIX + "table";

  private static final String STYLE_SVC_HEADER = STYLE_SVC_PREFIX + "header";
  private static final String STYLE_SVC_ROW = STYLE_SVC_PREFIX + "row";
  private static final String STYLE_SVC_SELECTED = STYLE_SVC_PREFIX + "selected";
  private static final String STYLE_SVC_FOOTER = STYLE_SVC_PREFIX + "footer";

  private static final String STYLE_LABEL_CELL_SUFFIX = "label";
  private static final String STYLE_CELL_SUFFIX = "cell";
  private static final String STYLE_INPUT_SUFFIX = "input";

  private static final String STYLE_SVC_TOGGLE_PREFIX = STYLE_SVC_PREFIX + "toggle-";
  private static final String STYLE_SVC_TOGGLE_WIDGET = STYLE_SVC_PREFIX + "toggle";

  private static final String STYLE_SVC_ACT_PREFIX = STYLE_SVC_PREFIX + "act-";
  private static final String STYLE_SVC_ITEM_PREFIX = STYLE_SVC_PREFIX + "item-";
  private static final String STYLE_SVC_NAME_PREFIX = STYLE_SVC_PREFIX + "name-";
  private static final String STYLE_SVC_ARTICLE_PREFIX = STYLE_SVC_PREFIX + "article-";
  private static final String STYLE_SVC_TIME_UNIT_PREFIX = STYLE_SVC_PREFIX + "time-unit-";
  private static final String STYLE_SVC_UNIT_PREFIX = STYLE_SVC_PREFIX + "unit-";
  private static final String STYLE_SVC_FROM_PREFIX = STYLE_SVC_PREFIX + "from-";
  private static final String STYLE_SVC_TO_PREFIX = STYLE_SVC_PREFIX + "to-";
  private static final String STYLE_SVC_FACTOR_PREFIX = STYLE_SVC_PREFIX + "factor-";
  private static final String STYLE_SVC_DPW_PREFIX = STYLE_SVC_PREFIX + "dpw-";
  private static final String STYLE_SVC_MIN_TERM_PREFIX = STYLE_SVC_PREFIX + "minterm-";
  private static final String STYLE_SVC_QTY_PREFIX = STYLE_SVC_PREFIX + "qty-";
  private static final String STYLE_SVC_PRICE_PREFIX = STYLE_SVC_PREFIX + "price-";
  private static final String STYLE_SVC_CURRENCY_PREFIX = STYLE_SVC_PREFIX + "currency-";
  private static final String STYLE_SVC_DISCOUNT_PREFIX = STYLE_SVC_PREFIX + "discount-";

  private static final String STYLE_SVC_FACTOR_WIDGET = STYLE_SVC_FACTOR_PREFIX
      + STYLE_INPUT_SUFFIX;

  private static final String STYLE_SVC_AMOUNT_PREFIX = STYLE_SVC_PREFIX + "amount-";
  private static final String STYLE_SVC_AMOUNT_LABEL = STYLE_SVC_AMOUNT_PREFIX
      + STYLE_LABEL_CELL_SUFFIX;
  private static final String STYLE_SVC_AMOUNT_CELL = STYLE_SVC_AMOUNT_PREFIX + STYLE_CELL_SUFFIX;

  private static final String STYLE_SVC_TOTAL_PREFIX = STYLE_SVC_PREFIX + "total-";
  private static final String STYLE_SVC_TOTAL_CELL = STYLE_SVC_TOTAL_PREFIX + STYLE_CELL_SUFFIX;

  private static final String STORAGE_KEY_SEPARATOR = "-";

  private static void onToggle(Toggle toggle, String styleSelected) {
    TableRowElement rowElement = DomUtils.getParentRow(toggle.getElement(), false);

    if (rowElement != null) {
      if (toggle.isChecked()) {
        rowElement.addClassName(styleSelected);
      } else {
        rowElement.removeClassName(styleSelected);
      }
    }
  }

  private static String render(Double value, Integer scale) {
    if (BeeUtils.isDouble(value)) {
      if (BeeUtils.isNonNegative(scale)) {
        return BeeUtils.toString(value, scale);
      } else {
        return BeeUtils.toString(value);
      }
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  private static String render(Integer value) {
    return (value == null) ? BeeConst.STRING_EMPTY : BeeUtils.toString(value);
  }

  private static String renderAmount(Double amount) {
    if (BeeUtils.isDouble(amount)) {
      return Format.getDefaultMoneyFormat().format(amount);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  private static String renderPrice(Double price) {
    return renderAmount(price);
  }

  private IdentifiableWidget commandCompose;
  private IdentifiableWidget commandSave;

  private final List<Act> acts = new ArrayList<>();
  private final List<Service> services = new ArrayList<>();

  private Double defVatPercent;

  TradeActInvoiceBuilder() {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if ((COL_TA_COMPANY.equals(name) || COL_TA_CURRENCY.equals(name))
        && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(this);
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action == Action.REFRESH) {
      refresh(true);
      return false;

    } else {
      return super.beforeAction(action, presenter);
    }
  }

  @Override
  public void configureRelation(String name, Relation relation) {
    if (COL_TA_COMPANY.equals(name)) {
      relation.setFilter(Filter.in(Data.getIdColumn(VIEW_COMPANIES),
          VIEW_TRADE_ACTS, COL_TA_COMPANY, TradeActKind.getFilterForInvoiceBuilder()));
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeActInvoiceBuilder();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.hasRelatedView(VIEW_COMPANIES)) {
      if (event.isChanged() && event.getRelatedRow() != null) {
        refresh(true);
      }

    } else if (event.hasRelatedView(VIEW_CURRENCIES)) {
      if (event.isChanged()) {
        Element cell = Selectors.getElementByClassName(getFormView().getElement(),
            STYLE_SVC_AMOUNT_LABEL);

        if (cell != null) {
          cell.setInnerText(getAmountLabel());

          if (!services.isEmpty()) {
            refreshAmounts();
            refreshTotals();
          }
        }
      }
    }
  }

  @Override
  public void onLoad(FormView form) {
    Widget widget;

    JustDate from = BeeKeeper.getStorage().getDate(storageKey(COL_TA_SERVICE_FROM));
    if (from != null) {
      widget = form.getWidgetByName(COL_TA_SERVICE_FROM);
      if (widget instanceof InputDate) {
        ((InputDate) widget).setDate(from);
      }
    }

    JustDate to = BeeKeeper.getStorage().getDate(storageKey(COL_TA_SERVICE_TO));
    if (to != null) {
      widget = form.getWidgetByName(COL_TA_SERVICE_TO);
      if (widget instanceof InputDate) {
        ((InputDate) widget).setDate(to);
      }
    }

    String pfx = BeeKeeper.getStorage().get(storageKey(COL_TRADE_INVOICE_PREFIX));
    if (!BeeUtils.isEmpty(pfx)) {
      widget = form.getWidgetByName(COL_TRADE_INVOICE_PREFIX);
      if (widget instanceof Editor) {
        ((Editor) widget).setValue(pfx);
      }
    }

    Long currency = BeeKeeper.getStorage().getLong(storageKey(COL_TA_CURRENCY));
    if (DataUtils.isId(currency)) {
      widget = form.getWidgetByName(COL_TA_CURRENCY);
      if (widget instanceof UnboundSelector) {
        ((UnboundSelector) widget).setValue(currency, false);
      }
    }
  }

  @Override
  public void onShow(Presenter presenter) {
    HeaderView header = presenter.getHeader();

    if (header != null && !header.hasCommands()) {
      if (commandCompose == null) {
        commandCompose =
            new Button(Localized.getConstants().taInvoiceCompose(), new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                doCompose();
              }
            });

        commandCompose.addStyleName(STYLE_COMMAND_COMPOSE);
        commandCompose.addStyleName(STYLE_COMMAND_DISABLED);
      }

      header.addCommandItem(commandCompose);

      if (commandSave == null) {
        commandSave = new Button(Localized.getConstants().taInvoiceSave(), new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            doSave();
          }
        });

        commandSave.addStyleName(STYLE_COMMAND_SAVE);
        commandSave.addStyleName(STYLE_COMMAND_DISABLED);
      }

      header.addCommandItem(commandSave);
    }

    super.onShow(presenter);
  }

  private void doCompose() {
    Range<JustDate> range = getRange();
    if (range == null) {
      return;
    }

    final Range<DateTime> builderRange = TradeActUtils.convertRange(range);
    final int builderDays = TradeActUtils.countServiceDays(builderRange);

    Collection<Long> actIds = getSelectedIds(STYLE_ACT_SELECTED);
    if (actIds.isEmpty()) {
      getFormView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }

    Filter filter = Filter.and(Filter.any(COL_TRADE_ACT, actIds),
        Filter.isPositive(COL_TRADE_ITEM_QUANTITY));

    Queries.getRowSet(VIEW_TRADE_ACT_SERVICES, null, filter, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        int actIndex = result.getColumnIndex(COL_TRADE_ACT);

        int dateFromIndex = result.getColumnIndex(COL_TA_SERVICE_FROM);
        int dateToIndex = result.getColumnIndex(COL_TA_SERVICE_TO);

        int timeUnitIndex = result.getColumnIndex(COL_TIME_UNIT);
        int qtyIndex = result.getColumnIndex(COL_TRADE_ITEM_QUANTITY);

        int tariffIndex = result.getColumnIndex(COL_TA_SERVICE_TARIFF);
        int priceIndex = result.getColumnIndex(COL_TRADE_ITEM_PRICE);
        int priceScale = result.getColumn(COL_TRADE_ITEM_PRICE).getScale();

        int factorIndex = result.getColumnIndex(COL_TA_SERVICE_FACTOR);
        int factorScale = result.getColumn(COL_TA_SERVICE_FACTOR).getScale();

        int dpwIndex = result.getColumnIndex(COL_TA_SERVICE_DAYS);
        int minTermIndex = result.getColumnIndex(COL_TA_SERVICE_MIN);

        int discountIndex = result.getColumnIndex(COL_TRADE_DISCOUNT);

        int itemVatIndex = result.getColumnIndex(ALS_ITEM_VAT);
        int itemVatPercentIndex = result.getColumnIndex(ALS_ITEM_VAT_PERCENT);

        int vatIndex = result.getColumnIndex(COL_TRADE_VAT);
        int vatIsPercentIndex = result.getColumnIndex(COL_TRADE_VAT_PERC);

        Act act = null;

        services.clear();

        for (BeeRow row : result) {
          long actId = row.getLong(actIndex);
          if (act == null || act.id() != actId) {
            act = findAct(actId);
            if (act == null) {
              continue;
            }
          }

          TradeActTimeUnit tu = EnumUtils.getEnumByIndex(TradeActTimeUnit.class,
              row.getInteger(timeUnitIndex));

          Range<DateTime> serviceRange = TradeActUtils.createServiceRange(
              row.getDate(dateFromIndex), row.getDate(dateToIndex), tu, builderRange, act.range);

          if (serviceRange == null) {
            continue;
          }

          Service svc = new Service(row, serviceRange, tu);

          svc.quantity = row.getDouble(qtyIndex);

          svc.tariff = row.getDouble(tariffIndex);
          svc.price = row.getDouble(priceIndex);

          if (BeeUtils.isPositive(svc.tariff)) {
            Double p = TradeActUtils.calculateServicePrice(act.itemTotal(),
                svc.tariff, priceScale);
            if (BeeUtils.isPositive(p)) {
              svc.price = p;
            }
          }

          svc.factor = row.getDouble(factorIndex);
          svc.dpw = row.getInteger(dpwIndex);

          svc.minTerm = row.getInteger(minTermIndex);

          if (tu != null) {
            int days = TradeActUtils.countServiceDays(serviceRange);

            switch (tu) {
              case DAY:
                if (TradeActUtils.validDpw(svc.dpw) && !BeeUtils.isPositive(svc.factor)) {
                  double df = TradeActUtils.dpwToFactor(svc.dpw, days, svc.minTerm);
                  if (BeeUtils.isPositive(df)) {
                    svc.factor = df;
                  }
                }
                break;

              case MONTH:
                if (days < builderDays) {
                  double df = BeeUtils.div(days, builderDays);
                  if (BeeUtils.isPositive(svc.factor)) {
                    df *= svc.factor;
                  }

                  svc.factor = BeeUtils.round(df, factorScale);
                }
                break;
            }
          }

          svc.discount = row.getDouble(discountIndex);

          Double vat = row.getDouble(vatIndex);
          if (BeeUtils.isPositive(vat) && !row.isNull(vatIsPercentIndex)) {
            svc.vatPercent = vat;
          } else if (!row.isNull(itemVatIndex)) {
            svc.vatPercent = BeeUtils.positive(row.getDouble(itemVatPercentIndex), defVatPercent);
          }

          svc.currency = act.currency;

          services.add(svc);
        }

        renderServices();
      }
    });
  }

  private void doSave() {
    Range<JustDate> range = getRange();
    if (range == null) {
      return;
    }

    Long company = getCompany();
    if (!DataUtils.isId(company)) {
      return;
    }

    String invoicePrefix = getInvoicePrefix();
    Long currency = getCurrency();

    Collection<Long> svcIds = getSelectedIds(STYLE_SVC_SELECTED);
    if (svcIds.isEmpty() || services.isEmpty()) {
      return;
    }

    List<BeeColumn> serviceColumns = Data.getColumns(VIEW_TRADE_ACT_SERVICES);

    BeeRowSet selectedServices = new BeeRowSet(VIEW_TRADE_ACT_SERVICES, serviceColumns);
    Set<Long> actIds = new HashSet<>();

    int actIndex = selectedServices.getColumnIndex(COL_TRADE_ACT);

    for (Service svc : services) {
      if (svcIds.contains(svc.id())) {
        selectedServices.addRow(DataUtils.cloneRow(svc.row));
        actIds.add(svc.row.getLong(actIndex));
      }
    }

    if (selectedServices.isEmpty() || actIds.isEmpty()) {
      return;
    }

    BeeKeeper.getStorage().set(storageKey(COL_TA_SERVICE_FROM), range.lowerEndpoint());
    BeeKeeper.getStorage().set(storageKey(COL_TA_SERVICE_TO), range.upperEndpoint());

    BeeKeeper.getStorage().set(storageKey(COL_TRADE_INVOICE_PREFIX), BeeUtils.trim(invoicePrefix));
    BeeKeeper.getStorage().set(storageKey(COL_TA_CURRENCY), currency);

    DataInfo dataInfo = Data.getDataInfo(VIEW_SALES);
    BeeRow invoice = RowFactory.createEmptyRow(dataInfo, true);

    if (!BeeUtils.isEmpty(invoicePrefix)) {
      Data.squeezeValue(VIEW_SALES, invoice, COL_TRADE_INVOICE_PREFIX, invoicePrefix.trim());
    }

    invoice.setValue(dataInfo.getColumnIndex(COL_TRADE_CUSTOMER), company);

    if (DataUtils.isId(currency)) {
      invoice.setValue(dataInfo.getColumnIndex(COL_TRADE_CURRENCY), currency);
    }

    invoice.setValue(dataInfo.getColumnIndex(COL_TRADE_MANAGER), BeeKeeper.getUser().getUserId());

    BeeRowSet sales = DataUtils.createRowSetForInsert(dataInfo.getViewName(),
        dataInfo.getColumns(), invoice);

    BeeRowSet saleItems = createInvoiceItems(selectedServices, currency);
    BeeRowSet relations = createRelations(actIds, range.lowerEndpoint(), range.upperEndpoint());

    ParameterList params = TradeActKeeper.createArgs(SVC_CREATE_ACT_INVOICE);

    params.addDataItem(sales.getViewName(), sales.serialize());
    params.addDataItem(saleItems.getViewName(), saleItems.serialize());
    params.addDataItem(relations.getViewName(), relations.serialize());

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRow.class)) {
          BeeRow result = BeeRow.restore(response.getResponseAsString());

          RowInsertEvent.fire(BeeKeeper.getBus(), VIEW_SALES, result, null);
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_ACT_INVOICES);

          refresh(false);
          RowEditor.open(VIEW_SALES, result, Opener.MODAL);
        }
      }
    });
  }

  private static BeeRowSet createRelations(Collection<Long> actIds, JustDate start, JustDate end) {
    List<String> colNames = Lists.newArrayList(COL_TRADE_ACT, COL_SALE,
        COL_TA_INVOICE_FROM, COL_TA_INVOICE_TO);

    List<BeeColumn> columns = Data.getColumns(VIEW_TRADE_ACT_INVOICES, colNames);
    BeeRowSet relations = new BeeRowSet(VIEW_TRADE_ACT_INVOICES, columns);

    int actIndex = relations.getColumnIndex(COL_TRADE_ACT);

    int fromIndex = relations.getColumnIndex(COL_TA_INVOICE_FROM);
    int toIndex = relations.getColumnIndex(COL_TA_INVOICE_TO);

    for (Long actId : actIds) {
      BeeRow row = DataUtils.createEmptyRow(relations.getNumberOfColumns());

      row.setValue(actIndex, actId);

      row.setValue(fromIndex, start);
      row.setValue(toIndex, end);

      relations.addRow(row);
    }

    return relations;
  }

  private BeeRowSet createInvoiceItems(BeeRowSet selectedServices, Long currency) {
    List<String> colNames = Lists.newArrayList(COL_SALE, COL_ITEM, COL_TRADE_ITEM_ARTICLE,
        COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
        COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC,
        COL_TRADE_ITEM_NOTE);

    List<BeeColumn> columns = Data.getColumns(VIEW_SALE_ITEMS, colNames);

    Map<Integer, Integer> indexes = new HashMap<>();

    for (int i = 0; i < columns.size(); i++) {
      String colName = columns.get(i).getId();

      if (!COL_SALE.equals(colName)) {
        int svcIndex = selectedServices.getColumnIndex(colName);
        if (!BeeConst.isUndef(svcIndex)) {
          indexes.put(svcIndex, i);
        }
      }
    }

    BeeRowSet invoiceItems = new BeeRowSet(VIEW_SALE_ITEMS, columns);

    int priceIndex = invoiceItems.getColumnIndex(COL_TRADE_ITEM_PRICE);
    int priceScale = invoiceItems.getColumn(COL_TRADE_ITEM_PRICE).getScale();

    int vatPlusIndex = invoiceItems.getColumnIndex(COL_TRADE_VAT_PLUS);
    int vatIndex = invoiceItems.getColumnIndex(COL_TRADE_VAT);
    int vatPercentIndex = invoiceItems.getColumnIndex(COL_TRADE_VAT_PERC);

    DateTime date = TimeUtils.nowMinutes();

    for (BeeRow row : selectedServices) {
      BeeRow inv = DataUtils.createEmptyRow(invoiceItems.getNumberOfColumns());

      for (Map.Entry<Integer, Integer> entry : indexes.entrySet()) {
        if (!row.isNull(entry.getKey())) {
          inv.setValue(entry.getValue(), row.getString(entry.getKey()));
        }
      }

      Service svc = findService(row.getId());
      if (svc != null) {
        Double amount = svc.amount(currency, date);

        if (BeeUtils.isPositive(amount) && BeeUtils.isPositive(svc.quantity)) {
          inv.setValue(priceIndex, BeeUtils.round(amount / svc.quantity, priceScale));
        } else {
          inv.clearCell(priceIndex);
        }

        if (BeeUtils.isPositive(svc.vatPercent)) {
          inv.setValue(vatPlusIndex, true);
          inv.setValue(vatIndex, svc.vatPercent);
          inv.setValue(vatPercentIndex, true);
        }
      }

      invoiceItems.addRow(inv);
    }

    return invoiceItems;
  }

  private Act findAct(long id) {
    for (Act act : acts) {
      if (act.id() == id) {
        return act;
      }
    }
    return null;
  }

  private Service findService(Element element) {
    TableRowElement rowElement = DomUtils.getParentRow(element, true);
    long svcId = DomUtils.getDataIndexLong(rowElement);

    return findService(svcId);
  }

  private Service findService(long id) {
    for (Service svc : services) {
      if (svc.id() == id) {
        return svc;
      }
    }
    return null;
  }

  private JustDate getDateByWidgetName(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof InputDate) {
      return ((InputDate) widget).getDate();
    } else {
      return null;
    }
  }

  private Flow getActContainer() {
    return getContainerByWidgetName(VIEW_TRADE_ACTS);
  }

  private JustDate getDateFrom() {
    return getDateByWidgetName(COL_TA_SERVICE_FROM);
  }

  private JustDate getDateTo() {
    return getDateByWidgetName(COL_TA_SERVICE_TO);
  }

  private Flow getContainerByWidgetName(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof Flow) {
      return (Flow) widget;
    } else {
      return null;
    }
  }

  private Long getCompany() {
    return getSelectedIdByWidgetName(COL_TA_COMPANY);
  }

  private Long getCurrency() {
    Long currency = getSelectedIdByWidgetName(COL_TA_CURRENCY);
    return DataUtils.isId(currency) ? currency : ClientDefaults.getCurrency();
  }

  private String getCurrencyName() {
    String name;

    Widget widget = getFormView().getWidgetByName(COL_TA_CURRENCY);
    if (widget instanceof DataSelector) {
      name = ((DataSelector) widget).getDisplayValue();
    } else {
      name = null;
    }

    return BeeUtils.isEmpty(name) ? ClientDefaults.getCurrencyName() : name;
  }

  private String getInvoicePrefix() {
    Widget widget = getFormView().getWidgetByName(COL_TRADE_INVOICE_PREFIX);
    if (widget instanceof Editor) {
      return ((Editor) widget).getValue();
    } else {
      return null;
    }
  }

  private Range<JustDate> getRange() {
    JustDate start = getDateFrom();
    JustDate end = getDateTo();

    List<String> messages = new ArrayList<>();

    if (start == null) {
      Collections.addAll(messages, Localized.getConstants().dateFrom(),
          Localized.getConstants().valueRequired());

    } else if (end == null) {
      Collections.addAll(messages, Localized.getConstants().dateTo(),
          Localized.getConstants().valueRequired());

    } else if (TimeUtils.isMeq(start, end)) {
      Collections.addAll(messages, Localized.getConstants().invalidRange(),
          TimeUtils.renderPeriod(start, end));
    }

    if (messages.isEmpty()) {
      return Range.closedOpen(start, end);
    } else {
      getFormView().notifyWarning(ArrayUtils.toArray(messages));
      return null;
    }
  }

  private Collection<Long> getSelectedIds(String styleSelected) {
    Set<Long> ids = new HashSet<>();

    NodeList<Element> nodes = Selectors.getNodes(getFormView().getElement(),
        Selectors.classSelector(styleSelected));

    if (nodes != null) {
      for (int i = 0; i < nodes.getLength(); i++) {
        long id = DomUtils.getDataIndexLong(nodes.getItem(i));
        if (DataUtils.isId(id)) {
          ids.add(id);
        }
      }
    }

    return ids;
  }

  private Long getSelectedIdByWidgetName(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof DataSelector) {
      return ((DataSelector) widget).getRelatedId();
    } else {
      return null;
    }
  }

  private Flow getServiceContainer() {
    return getContainerByWidgetName(VIEW_TRADE_ACT_SERVICES);
  }

  private void refresh(final boolean notify) {
    Flow actContainer = getActContainer();
    if (actContainer != null && !actContainer.isEmpty()) {
      actContainer.clear();
    }

    Flow serviceContainer = getServiceContainer();
    if (serviceContainer != null && !serviceContainer.isEmpty()) {
      serviceContainer.clear();
    }

    commandCompose.addStyleName(STYLE_COMMAND_DISABLED);
    commandSave.addStyleName(STYLE_COMMAND_DISABLED);

    JustDate start = getDateFrom();
    JustDate end = getDateTo();

    Long company = getCompany();
    if (!DataUtils.isId(company)) {
      if (notify) {
        List<String> messages = Lists.newArrayList(Localized.getConstants().client(),
            Localized.getConstants().valueRequired());
        Global.showError(messages);
      }
      return;
    }

    final Range<Integer> builderDays;
    if (start == null || end == null || start.getDays() >= end.getDays()) {
      builderDays = null;
    } else {
      builderDays = Range.closedOpen(start.getDays(), end.getDays());
    }

    ParameterList params = TradeActKeeper.createArgs(SVC_GET_ACTS_FOR_INVOICE);

    if (start != null) {
      params.addQueryItem(COL_TA_SERVICE_FROM, start.getDays());
    }
    if (end != null) {
      params.addQueryItem(COL_TA_SERVICE_TO, end.getDays());
    }

    params.addQueryItem(COL_TA_COMPANY, company);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRowSet.class)) {
          BeeRowSet rowSet = BeeRowSet.restore(response.getResponseAsString());

          int dateIndex = rowSet.getColumnIndex(COL_TA_DATE);
          int untilIndex = rowSet.getColumnIndex(COL_TA_UNTIL);

          int currencyIndex = rowSet.getColumnIndex(COL_TA_CURRENCY);
          int currencyNameIndex = rowSet.getColumnIndex(ALS_CURRENCY_NAME);

          acts.clear();
          for (BeeRow row : rowSet) {
            Range<DateTime> range = TradeActUtils.createRange(row.getDateTime(dateIndex),
                row.getDateTime(untilIndex));

            Long currency = row.getLong(currencyIndex);
            String currencyName = row.getString(currencyNameIndex);

            Act act = new Act(row, range, currency, currencyName);

            String serialized = row.getProperty(TBL_TRADE_ACT_INVOICES);
            if (!BeeUtils.isEmpty(serialized)) {
              SimpleRowSet invoiceDates = SimpleRowSet.restore(serialized);

              if (!DataUtils.isEmpty(invoiceDates)) {
                for (SimpleRow sr : invoiceDates) {
                  JustDate invFrom = sr.getDate(COL_TA_INVOICE_FROM);
                  JustDate invTo = sr.getDate(COL_TA_INVOICE_TO);

                  if (invFrom != null && invTo != null && invFrom.getDays() < invTo.getDays()) {
                    act.invoiceDays.add(Range.closedOpen(invFrom.getDays(), invTo.getDays()));
                  }
                }

                if (builderDays != null && act.hasInvoices(builderDays)) {
                  act.setEnabled(false);
                }
              }
            }

            acts.add(act);
          }

          String vatPerc = rowSet.getTableProperty(PRM_VAT_PERCENT);
          if (!BeeUtils.isEmpty(vatPerc)) {
            defVatPercent = BeeUtils.toDoubleOrNull(vatPerc);
          }

          renderActs();

        } else if (notify) {
          getFormView().notifyWarning(Localized.getConstants().noData());
        }
      }
    });
  }

  private void renderActs() {
    DataInfo dataInfo = Data.getDataInfo(VIEW_TRADE_ACTS);
    List<BeeColumn> columns = dataInfo.getColumns();

    int nameIndex = dataInfo.getColumnIndex(COL_TRADE_ACT_NAME);

    int dateIndex = dataInfo.getColumnIndex(COL_TA_DATE);
    int untilIndex = dataInfo.getColumnIndex(COL_TA_UNTIL);

    int seriesNameIndex = dataInfo.getColumnIndex(COL_SERIES_NAME);
    int numberIndex = dataInfo.getColumnIndex(COL_TA_NUMBER);

    int operationNameIndex = dataInfo.getColumnIndex(COL_OPERATION_NAME);
    int statusNameIndex = dataInfo.getColumnIndex(COL_STATUS_NAME);

    int objectNameIndex = dataInfo.getColumnIndex(COL_COMPANY_OBJECT_NAME);

    boolean hasEnabledActs = false;
    for (Act act : acts) {
      if (act.isEnabled()) {
        hasEnabledActs = true;
        break;
      }
    }

    final HtmlTable table = new HtmlTable(STYLE_ACT_TABLE);

    int r = 0;
    int c = 0;

    if (hasEnabledActs) {
      Toggle toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
          STYLE_ACT_TOGGLE_WIDGET, false);

      toggle.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (event.getSource() instanceof Toggle) {
            boolean checked = ((Toggle) event.getSource()).isChecked();

            Collection<Toggle> toggles = UiHelper.getChildren(table, Toggle.class);
            for (Toggle t : toggles) {
              if (t.isChecked() != checked) {
                t.setChecked(checked);
                onToggle(t, STYLE_ACT_SELECTED);
              }
            }

            commandCompose.setStyleName(STYLE_COMMAND_DISABLED, !checked);
          }
        }
      });

      table.setWidget(r, c++, toggle, STYLE_ACT_TOGGLE_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    } else {
      c++;
    }

    table.setText(r, c++, Localized.getConstants().captionId(),
        STYLE_ACT_ID_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getLabel(columns.get(nameIndex)),
        STYLE_ACT_NAME_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(columns.get(dateIndex)),
        STYLE_ACT_DATE_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getLabel(columns.get(untilIndex)),
        STYLE_ACT_UNTIL_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Data.getColumnLabel(dataInfo.getViewName(), COL_TA_SERIES),
        STYLE_ACT_SERIES_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getLabel(columns.get(numberIndex)),
        STYLE_ACT_NUMBER_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Data.getColumnLabel(dataInfo.getViewName(), COL_TA_OPERATION),
        STYLE_ACT_OPERATION_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Data.getColumnLabel(dataInfo.getViewName(), COL_TA_STATUS),
        STYLE_ACT_STATUS_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Data.getColumnLabel(dataInfo.getViewName(), COL_TA_OBJECT),
        STYLE_ACT_OBJECT_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getConstants().goods(),
        STYLE_ACT_TOTAL_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().currencyShort(),
        STYLE_ACT_CURRENCY_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getConstants().dateTo(),
        STYLE_ACT_LATEST_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.getRowFormatter().addStyleName(r, STYLE_ACT_HEADER);

    r++;
    for (Act act : acts) {
      c = 0;

      if (act.isEnabled()) {
        Toggle toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
            STYLE_ACT_TOGGLE_WIDGET, false);

        toggle.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (event.getSource() instanceof Toggle) {
              onToggle((Toggle) event.getSource(), STYLE_ACT_SELECTED);

              commandCompose.setStyleName(STYLE_COMMAND_DISABLED,
                  !Selectors.contains(table.getElement(),
                      Selectors.classSelector(STYLE_ACT_SELECTED)));
            }
          }
        });

        table.setWidget(r, c++, toggle, STYLE_ACT_TOGGLE_PREFIX + STYLE_CELL_SUFFIX);

      } else {
        c++;
      }

      table.setText(r, c++, BeeUtils.toString(act.id()),
          STYLE_ACT_ID_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, act.row.getString(nameIndex),
          STYLE_ACT_NAME_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, TimeUtils.renderCompact(act.row.getDateTime(dateIndex)),
          STYLE_ACT_DATE_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, TimeUtils.renderCompact(act.row.getDateTime(untilIndex)),
          STYLE_ACT_UNTIL_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, act.row.getString(seriesNameIndex),
          STYLE_ACT_SERIES_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, act.row.getString(numberIndex),
          STYLE_ACT_NUMBER_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, act.row.getString(operationNameIndex),
          STYLE_ACT_OPERATION_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, act.row.getString(statusNameIndex),
          STYLE_ACT_STATUS_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, act.row.getString(objectNameIndex),
          STYLE_ACT_OBJECT_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, renderAmount(act.itemTotal()),
          STYLE_ACT_TOTAL_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, act.currencyName,
          STYLE_ACT_CURRENCY_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, TimeUtils.renderDate(act.lastInvoiceTo()),
          STYLE_ACT_LATEST_PREFIX + STYLE_CELL_SUFFIX);

      table.getRowFormatter().addStyleName(r, STYLE_ACT_ROW);
      DomUtils.setDataIndex(table.getRow(r), act.id());

      r++;
    }

    table.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Element target = EventUtils.getEventTargetElement(event);
        TableCellElement cell = DomUtils.getParentCell(target, true);

        if (cell != null
            && (cell.hasClassName(STYLE_ACT_ID_PREFIX + STYLE_CELL_SUFFIX)
            || cell.hasClassName(STYLE_ACT_NUMBER_PREFIX + STYLE_CELL_SUFFIX))) {

          long id = DomUtils.getDataIndexLong(DomUtils.getParentRow(cell, false));
          if (DataUtils.isId(id)) {
            RowEditor.open(VIEW_TRADE_ACTS, id, Opener.MODAL);
          }
        }
      }
    });

    Flow container = getActContainer();
    if (!container.isEmpty()) {
      container.clear();
    }

    container.add(table);
  }

  private void renderServices() {
    DataInfo dataInfo = Data.getDataInfo(VIEW_TRADE_ACT_SERVICES);

    final int actIndex = dataInfo.getColumnIndex(COL_TRADE_ACT);
    final int itemIndex = dataInfo.getColumnIndex(COL_TA_ITEM);

    int nameIndex = dataInfo.getColumnIndex(ALS_ITEM_NAME);
    int articleIndex = dataInfo.getColumnIndex(COL_ITEM_ARTICLE);

    int qtyScale = dataInfo.getColumnScale(COL_TRADE_ITEM_QUANTITY);
    int unitNameIndex = dataInfo.getColumnIndex(ALS_UNIT_NAME);

    int discountScale = dataInfo.getColumnScale(COL_TRADE_DISCOUNT);

    final HtmlTable table = new HtmlTable(STYLE_SVC_TABLE);

    int r = 0;
    int c = 0;

    Toggle toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
        STYLE_SVC_TOGGLE_WIDGET, false);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof Toggle) {
          boolean checked = ((Toggle) event.getSource()).isChecked();

          Collection<Toggle> toggles = UiHelper.getChildren(table, Toggle.class);
          for (Toggle t : toggles) {
            if (t.isChecked() != checked) {
              t.setChecked(checked);
              onToggle(t, STYLE_SVC_SELECTED);
            }
          }

          commandSave.setStyleName(STYLE_COMMAND_DISABLED, !checked);
        }
      }
    });

    table.setWidget(r, c++, toggle, STYLE_SVC_TOGGLE_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(dataInfo.getColumns().get(actIndex)),
        STYLE_SVC_ACT_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(dataInfo.getColumn(COL_TA_SERVICE_FROM)),
        STYLE_SVC_FROM_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getLabel(dataInfo.getColumn(COL_TA_SERVICE_TO)),
        STYLE_SVC_TO_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(dataInfo.getColumns().get(itemIndex)),
        STYLE_SVC_ITEM_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(dataInfo.getColumns().get(nameIndex)),
        STYLE_SVC_NAME_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getLabel(dataInfo.getColumns().get(articleIndex)),
        STYLE_SVC_ARTICLE_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(dataInfo.getColumn(COL_TIME_UNIT)),
        STYLE_SVC_TIME_UNIT_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(dataInfo.getColumn(COL_TRADE_ITEM_QUANTITY)),
        STYLE_SVC_QTY_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().unitShort(),
        STYLE_SVC_UNIT_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(dataInfo.getColumn(COL_TRADE_ITEM_PRICE)),
        STYLE_SVC_PRICE_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().currencyShort(),
        STYLE_SVC_CURRENCY_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getConstants().taFactorShort(),
        STYLE_SVC_FACTOR_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().taDaysPerWeekShort(),
        STYLE_SVC_DPW_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().taMinTermShort(),
        STYLE_SVC_MIN_TERM_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(dataInfo.getColumn(COL_TRADE_DISCOUNT)),
        STYLE_SVC_DISCOUNT_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    int totalCol = c;
    table.setText(r, c++, getAmountLabel(), STYLE_SVC_AMOUNT_LABEL);

    table.getRowFormatter().addStyleName(r, STYLE_SVC_HEADER);

    Act act = null;

    double total = BeeConst.DOUBLE_ZERO;

    Long currency = getCurrency();
    DateTime date = TimeUtils.nowMinutes();

    r++;
    for (Service svc : services) {
      c = 0;

      long actId = svc.row.getLong(actIndex);
      if (act == null || act.id() != actId) {
        act = findAct(actId);
      }

      toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
          STYLE_SVC_TOGGLE_WIDGET, false);

      toggle.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (event.getSource() instanceof Toggle) {
            onToggle((Toggle) event.getSource(), STYLE_SVC_SELECTED);

            commandSave.setStyleName(STYLE_COMMAND_DISABLED,
                !Selectors.contains(table.getElement(),
                    Selectors.classSelector(STYLE_SVC_SELECTED)));
          }
        }
      });

      table.setWidget(r, c++, toggle, STYLE_SVC_TOGGLE_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, BeeUtils.toString(actId),
          STYLE_SVC_ACT_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, TimeUtils.renderDate(svc.dateFrom()),
          STYLE_SVC_FROM_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, TimeUtils.renderDate(svc.dateTo()),
          STYLE_SVC_TO_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, svc.row.getString(itemIndex),
          STYLE_SVC_ITEM_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, svc.row.getString(nameIndex),
          STYLE_SVC_NAME_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, svc.row.getString(articleIndex),
          STYLE_SVC_ARTICLE_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, (svc.timeUnit == null) ? null : svc.timeUnit.getCaption(),
          STYLE_SVC_TIME_UNIT_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, render(svc.quantity, qtyScale),
          STYLE_SVC_QTY_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, svc.row.getString(unitNameIndex),
          STYLE_SVC_UNIT_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, renderPrice(svc.price),
          STYLE_SVC_PRICE_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, (act == null) ? null : act.currencyName,
          STYLE_SVC_CURRENCY_PREFIX + STYLE_CELL_SUFFIX);

      if (svc.timeUnit == null) {
        table.setText(r, c++, TimeUtils.renderDate(svc.dateFrom()),
            STYLE_SVC_FACTOR_PREFIX + STYLE_CELL_SUFFIX);
      } else {
        table.setWidget(r, c++, createFactorWidget(svc),
            STYLE_SVC_FACTOR_PREFIX + STYLE_CELL_SUFFIX);
      }

      if (svc.timeUnit == TradeActTimeUnit.DAY) {
        table.setWidget(r, c++, createDpwWidget(svc), STYLE_SVC_DPW_PREFIX + STYLE_CELL_SUFFIX);
      } else {
        table.setText(r, c++, null, STYLE_SVC_DPW_PREFIX + STYLE_CELL_SUFFIX);
      }

      table.setText(r, c++, render(svc.minTerm),
          STYLE_SVC_MIN_TERM_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, render(svc.discount, discountScale),
          STYLE_SVC_DISCOUNT_PREFIX + STYLE_CELL_SUFFIX);

      Double amount = svc.amount(currency, date);
      table.setText(r, c++, renderAmount(amount), STYLE_SVC_AMOUNT_CELL);

      if (BeeUtils.isDouble(amount)) {
        total += amount;
      }

      table.getRowFormatter().addStyleName(r, STYLE_SVC_ROW);
      DomUtils.setDataIndex(table.getRow(r), svc.id());

      r++;
    }

    c = 0;
    table.setText(r, c, Localized.getConstants().totalOf(),
        STYLE_SVC_TOTAL_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.getCellFormatter().setColSpan(r, c, totalCol);

    c++;
    table.setText(r, c, renderAmount(total), STYLE_SVC_TOTAL_CELL);

    table.getRowFormatter().addStyleName(r, STYLE_SVC_FOOTER);

    table.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Element target = EventUtils.getEventTargetElement(event);
        TableCellElement cell = DomUtils.getParentCell(target, true);

        if (cell != null
            && (cell.hasClassName(STYLE_SVC_ACT_PREFIX + STYLE_CELL_SUFFIX)
            || cell.hasClassName(STYLE_SVC_ITEM_PREFIX + STYLE_CELL_SUFFIX))) {

          Service svc = findService(cell);

          if (svc != null) {
            String viewName;
            int index;

            if (cell.hasClassName(STYLE_SVC_ITEM_PREFIX + STYLE_CELL_SUFFIX)) {
              viewName = VIEW_ITEMS;
              index = itemIndex;
            } else {
              viewName = VIEW_TRADE_ACTS;
              index = actIndex;
            }

            RowEditor.open(viewName, svc.row.getLong(index), Opener.MODAL);
          }
        }
      }
    });

    Flow container = getServiceContainer();
    if (!container.isEmpty()) {
      container.clear();
    }

    container.add(table);
  }

  private Widget createFactorWidget(Service svc) {
    InputNumber widget = new InputNumber();
    widget.addStyleName(STYLE_SVC_FACTOR_WIDGET);

    if (svc.factor != null) {
      widget.setValue(svc.factor);
    }

    widget.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        if (event.getSource() instanceof InputNumber) {
          InputNumber source = (InputNumber) event.getSource();
          Double value = source.getNumber();

          TableRowElement rowElement = DomUtils.getParentRow(source.getElement(), false);
          long svcId = DomUtils.getDataIndexLong(rowElement);
          Service service = findService(svcId);

          if (service != null && !Objects.equals(service.factor, value)) {
            service.factor = value;

            updateAmountCell(rowElement, service);
            refreshTotals();
          }
        }
      }
    });

    return widget;
  }

  private Widget createDpwWidget(Service svc) {
    ListBox widget = new ListBox();
    widget.addStyleName(STYLE_SVC_DPW_PREFIX + STYLE_INPUT_SUFFIX);

    for (int i = DPW_MIN; i <= DPW_MAX; i++) {
      widget.addItem(BeeUtils.joinWords(i, Localized.getConstants().dayShort()));
    }

    if (TradeActUtils.validDpw(svc.dpw)) {
      widget.setSelectedIndex(svc.dpw - DPW_MIN);
    } else {
      widget.deselect();
    }

    widget.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        if (event.getSource() instanceof ListBox) {
          ListBox source = (ListBox) event.getSource();

          int index = source.getSelectedIndex();
          if (index < 0) {
            return;
          }

          TableRowElement rowElement = DomUtils.getParentRow(source.getElement(), false);
          long svcId = DomUtils.getDataIndexLong(rowElement);
          Service service = findService(svcId);
          if (service == null) {
            return;
          }

          double df = TradeActUtils.dpwToFactor(DPW_MIN + index,
              TradeActUtils.countServiceDays(service.range), service.minTerm);
          if (Objects.equals(service.factor, df)) {
            return;
          }

          service.factor = df;

          Element input = Selectors.getElementByClassName(rowElement, STYLE_SVC_FACTOR_WIDGET);
          if (InputElement.is(input)) {
            InputElement.as(input).setValue(BeeUtils.toString(df));
          }

          updateAmountCell(rowElement, service);
          refreshTotals();
        }
      }
    });

    return widget;
  }

  private String getAmountLabel() {
    return BeeUtils.joinWords(Localized.getConstants().amount(), getCurrencyName());
  }

  private void refreshAmounts() {
    List<Element> cells = Selectors.getElementsByClassName(getFormView().getElement(),
        STYLE_SVC_AMOUNT_CELL);
    if (BeeUtils.isEmpty(cells)) {
      return;
    }

    Long currency = getCurrency();
    DateTime date = TimeUtils.nowMinutes();

    for (Element cell : cells) {
      Service svc = findService(cell);

      if (svc != null) {
        cell.setInnerText(renderAmount(svc.amount(currency, date)));
      }
    }
  }

  private void refreshTotals() {
    Element target = Selectors.getElementByClassName(getFormView().getElement(),
        STYLE_SVC_TOTAL_CELL);
    if (target == null) {
      return;
    }

    Long currency = getCurrency();
    DateTime date = TimeUtils.nowMinutes();

    double total = BeeConst.DOUBLE_ZERO;

    for (Service svc : services) {
      Double amount = svc.amount(currency, date);

      if (BeeUtils.nonZero(amount)) {
        total += amount;
      }
    }

    target.setInnerText(renderAmount(total));
  }

  private String storageKey(String name) {
    return BeeUtils.join(STORAGE_KEY_SEPARATOR, NameUtils.getName(this),
        BeeKeeper.getUser().getUserId(), name);
  }

  private void updateAmountCell(Element rowElement, Service svc) {
    Element cell = Selectors.getElementByClassName(rowElement, STYLE_SVC_AMOUNT_CELL);

    if (cell != null) {
      cell.setInnerText(renderAmount(svc.amount(getCurrency(), TimeUtils.nowMinutes())));
    }
  }
}
