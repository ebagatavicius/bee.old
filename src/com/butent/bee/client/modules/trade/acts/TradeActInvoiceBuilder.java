package com.butent.bee.client.modules.trade.acts;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.HeaderView;
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
import java.util.function.Consumer;

public class TradeActInvoiceBuilder extends AbstractFormInterceptor implements
    SelectorEvent.Handler {

  private static final class Act implements HasEnabled {

    private final BeeRow row;

    private final Range<DateTime> range;

    private final Long currency;
    private final String currencyName;

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

    private long id() {
      return row.getId();
    }

    private Double itemTotal() {
      return BeeUtils.toDoubleOrNull(row.getProperty(PRP_ITEM_TOTAL));
    }
  }

  private static final class Service {

    private final BeeRow row;

    private final List<Range<DateTime>> ranges = new ArrayList<>();

    private final List<Double> factors = new ArrayList<>();
    private final List<Integer> dpws = new ArrayList<>();
    private final List<Double> discounts = new ArrayList<>();

    private final TradeActTimeUnit timeUnit;

    private Double quantity;

    private Double tariff;
    private Double price;

    private Integer minTerm;
    private DateTime minTermStart;

    private Double vatPercent;

    private Long currency;

    private Service(BeeRow row, TradeActTimeUnit timeUnit) {
      this.row = row;
      this.timeUnit = timeUnit;
    }

    private void add(Range<DateTime> range, Double factor, Integer dpw, Double discount) {
      ranges.add(range);
      factors.add(factor);
      dpws.add(dpw);
      discounts.add(discount);
    }

    private Double amount(int index, Long toCurrency, DateTime date) {
      Double amount = TradeActUtils.serviceAmount(quantity, price, discounts.get(index), timeUnit,
          factors.get(index));

      if (BeeUtils.nonZero(amount) && Money.canExchange(currency, toCurrency)) {
        return Money.exchange(currency, toCurrency, amount, date);
      } else {
        return amount;
      }
    }

    private JustDate dateFrom(int index) {
      return ranges.get(index).lowerEndpoint().getDate();
    }

    private JustDate dateTo(int index) {
      return ranges.get(index).upperEndpoint().getDate();
    }

    private long id() {
      return row.getId();
    }

    private boolean minTermWarn(int index, Collection<Integer> holidays) {
      if (minTermStart == null || !BeeUtils.isPositive(minTerm)) {
        return false;
      } else {
        Range<DateTime> range = TradeActUtils.createRange(minTermStart, dateTo(index));
        int days = TradeActUtils.countServiceDays(range, holidays, dpws.get(index));
        return minTerm > days;
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
  private static final String STYLE_SVC_DISCOUNT_WIDGET = STYLE_SVC_DISCOUNT_PREFIX
      + STYLE_INPUT_SUFFIX;

  private static final String STYLE_SVC_MIN_TERM_CELL = STYLE_SVC_MIN_TERM_PREFIX
      + STYLE_CELL_SUFFIX;
  private static final String STYLE_SVC_MIN_TERM_WARN = STYLE_SVC_MIN_TERM_PREFIX + "warn";

  private static final String STYLE_SVC_AMOUNT_PREFIX = STYLE_SVC_PREFIX + "amount-";
  private static final String STYLE_SVC_AMOUNT_LABEL = STYLE_SVC_AMOUNT_PREFIX
      + STYLE_LABEL_CELL_SUFFIX;
  private static final String STYLE_SVC_AMOUNT_CELL = STYLE_SVC_AMOUNT_PREFIX + STYLE_CELL_SUFFIX;

  private static final String STYLE_SVC_TOTAL_PREFIX = STYLE_SVC_PREFIX + "total-";
  private static final String STYLE_SVC_TOTAL_CELL = STYLE_SVC_TOTAL_PREFIX + STYLE_CELL_SUFFIX;

  private static final String STORAGE_KEY_SEPARATOR = "-";

  private static final String KEY_SERVICE_ID = "service";
  private static final String KEY_RANGE_INDEX = "range";

  private static List<Range<DateTime>> buildRanges(Range<DateTime> serviceRange,
      Collection<Range<DateTime>> invoiceRanges, TradeActTimeUnit timeUnit) {

    List<Range<DateTime>> result = new ArrayList<>();
    if (serviceRange == null) {
      return result;
    }

    if (BeeUtils.isEmpty(invoiceRanges)) {
      result.add(serviceRange);
      return result;
    } else if (timeUnit == null) {
      return result;
    }

    List<Range<Integer>> invoiceDays = new ArrayList<>();

    for (Range<DateTime> range : invoiceRanges) {
      int lower = range.lowerEndpoint().getDate().getDays();

      int upper = range.upperEndpoint().getDate().getDays();
      if (upper <= lower) {
        upper = lower + 1;
      }

      invoiceDays.add(Range.closedOpen(lower, upper));
    }

    List<Integer> serviceDays = new ArrayList<>();

    int from = serviceRange.lowerEndpoint().getDate().getDays();
    int to = serviceRange.upperEndpoint().getDate().getDays();
    if (to <= from) {
      to = from + 1;
    }

    for (int d = from; d < to; d++) {
      boolean ok = true;

      for (Range<Integer> range : invoiceDays) {
        if (range.contains(d)) {
          ok = false;
          break;
        }
      }

      if (ok) {
        serviceDays.add(d);
      }
    }

    if (serviceDays.isEmpty()) {
      return result;
    }

    from = serviceDays.get(0);
    to = serviceDays.get(serviceDays.size() - 1) + 1;

    if (from + serviceDays.size() == to) {
      result.add(TradeActUtils.createRange(new JustDate(from), new JustDate(to)));
      return result;
    }

    int p = 0;

    for (int i = 1; i < serviceDays.size(); i++) {
      int d = serviceDays.get(i);
      if (from + i - p < d) {
        result.add(TradeActUtils.createRange(new JustDate(from), new JustDate(from + i - p)));
        from = d;
        p = i;
      }
    }

    result.add(TradeActUtils.createRange(new JustDate(from), new JustDate(to)));

    return result;
  }

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
      Set<Long> activeStatuses = TradeActKeeper.getActiveStatuses();

      Filter statusFilter;
      if (activeStatuses.isEmpty()) {
        statusFilter = Filter.isNull(COL_TA_STATUS);
      } else {
        statusFilter = Filter.or(Filter.isNull(COL_TA_STATUS),
            Filter.any(COL_TA_STATUS, activeStatuses));
      }

      Filter actFilter = Filter.and(TradeActKind.getFilterForInvoiceBuilder(), statusFilter);

      relation.setFilter(Filter.in(Data.getIdColumn(VIEW_COMPANIES),
          VIEW_TRADE_ACTS, COL_TA_COMPANY, actFilter));
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeActInvoiceBuilder();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.hasRelatedView(VIEW_COMPANIES)) {
      if (event.isOpened()) {
        event.getSelector().getOracle().clearData();
      } else if (event.isChanged() && event.getRelatedRow() != null) {
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

    Long seriesId =
        BeeUtils.toLongOrNull(BeeKeeper.getStorage().get(storageKey(COL_TRADE_SALE_SERIES)));
    if (DataUtils.isId(seriesId)) {
      widget = form.getWidgetByName(COL_TRADE_SALE_SERIES);
      if (widget instanceof UnboundSelector) {
        ((UnboundSelector) widget).setValue(seriesId, true);
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
  public void afterCreatePresenter(Presenter presenter) {
    HeaderView header = presenter.getHeader();

    if (header != null && !header.hasCommands()) {
      if (commandCompose == null) {
        commandCompose =
            new Button(Localized.dictionary().taInvoiceCompose(), new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                ClassifierKeeper.getHolidays(new Consumer<Set<Integer>>() {
                  @Override
                  public void accept(Set<Integer> input) {
                    doCompose(input);
                  }
                });
              }
            });

        commandCompose.addStyleName(STYLE_COMMAND_COMPOSE);
        commandCompose.addStyleName(STYLE_COMMAND_DISABLED);
      }

      header.addCommandItem(commandCompose);

      if (commandSave == null) {
        commandSave = new Button(Localized.dictionary().taInvoiceSave(), new ClickHandler() {
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

    super.afterCreatePresenter(presenter);
  }

  private void doCompose(final Collection<Integer> holidays) {
    Range<JustDate> range = getRange();
    if (range == null) {
      return;
    }

    final Range<DateTime> builderRange = TradeActUtils.convertRange(range);
    final int builderDays = TradeActUtils.countServiceDays(builderRange, holidays);
    if (builderDays <= 0) {
      getFormView().notifyWarning(Localized.dictionary().holidays());
      return;
    }

    Collection<Long> actIds = getSelectedActs(STYLE_ACT_SELECTED);
    if (actIds.isEmpty()) {
      getFormView().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }

    commandCompose.addStyleName(STYLE_COMMAND_DISABLED);
    commandSave.addStyleName(STYLE_COMMAND_DISABLED);

    ParameterList params = TradeActKeeper.createArgs(SVC_GET_SERVICES_FOR_INVOICE);
    params.addQueryItem(COL_TRADE_ACT, DataUtils.buildIdList(actIds));

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        services.clear();

        if (response.hasResponse(BeeRowSet.class)) {
          BeeRowSet result = BeeRowSet.restore(response.getResponseAsString());
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

            JustDate dateFrom = row.getDate(dateFromIndex);
            JustDate dateTo = row.getDate(dateToIndex);

            Range<DateTime> serviceRange = TradeActUtils.createServiceRange(dateFrom, dateTo, tu,
                builderRange, act.range);

            if (serviceRange == null) {
              continue;
            }

            List<Range<DateTime>> invoiceRanges = new ArrayList<>();

            List<Integer> periods = BeeUtils.toInts(row.getProperty(PRP_INVOICE_PERIODS));
            if (periods.size() >= 2) {
              for (int i = 0; i < periods.size() - 1; i += 2) {
                JustDate from = new JustDate(periods.get(i));
                JustDate to = new JustDate(periods.get(i + 1));

                invoiceRanges.add(TradeActUtils.createRange(from, to));
              }
            }

            List<Range<DateTime>> ranges = buildRanges(serviceRange, invoiceRanges, tu);
            if (ranges.isEmpty()) {
              continue;
            }

            Service svc = new Service(row, tu);

            svc.quantity = row.getDouble(qtyIndex);

            svc.tariff = row.getDouble(tariffIndex);
            svc.price = row.getDouble(priceIndex);

            if (BeeUtils.isPositive(svc.tariff)) {
              Double p = TradeActUtils.calculateServicePrice(svc.price, dateTo, act.itemTotal(),
                  svc.tariff, priceScale);
              if (BeeUtils.isPositive(p)) {
                svc.price = p;
              }
            }

            Double factor = row.getDouble(factorIndex);
            Integer dpw = row.getInteger(dpwIndex);

            svc.minTerm = row.getInteger(minTermIndex);

            if (tu == TradeActTimeUnit.DAY && BeeUtils.isPositive(svc.minTerm)) {
              if (dateFrom == null) {
                svc.minTermStart = TradeActUtils.getLower(act.range);
              } else {
                svc.minTermStart = TimeUtils.startOfDay(dateFrom);
              }
            }

            Double discount = row.getDouble(discountIndex);

            for (Range<DateTime> r : ranges) {
              if (tu == null) {
                svc.add(r, factor, dpw, discount);

              } else {
                switch (tu) {
                  case DAY:
                    if (TradeActUtils.validDpw(dpw)) {
                      int days = TradeActUtils.countServiceDays(r, holidays, dpw);

                      if (days > 0) {
                        double df = days;
                        if (BeeUtils.isPositive(factor)) {
                          df *= factor;
                          df = BeeUtils.round(df, factorScale);
                        }

                        svc.add(r, df, dpw, discount);
                      }

                    } else {
                      svc.add(r, factor, dpw, discount);
                    }
                    break;

                  case MONTH:
                    double mf = TradeActUtils.getMonthFactor(r, holidays);

                    if (BeeUtils.isPositive(mf)) {
                      if (BeeUtils.isPositive(factor)) {
                        mf *= factor;
                      }
                      mf = BeeUtils.round(mf, factorScale);

                      svc.add(r, mf, dpw, discount);
                    }
                    break;
                }
              }
            }

            if (svc.ranges.isEmpty()) {
              continue;
            }

            Double vat = row.getDouble(vatIndex);
            if (BeeUtils.isPositive(vat) && !row.isNull(vatIsPercentIndex)) {
              svc.vatPercent = vat;
            } else if (!row.isNull(itemVatIndex)) {
              svc.vatPercent = BeeUtils.positive(row.getDouble(itemVatPercentIndex), defVatPercent);
            }

            svc.currency = act.currency;

            services.add(svc);
          }

          renderServices(holidays);

        } else {
          getServiceContainer().clear();
          getFormView().notifyWarning(Localized.dictionary().noData());
        }
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

    Long seriesId = getSeriesId();
    Long currency = getCurrency();

    Multimap<Long, Integer> ss = getSelectedServices(STYLE_SVC_SELECTED);
    if (ss.isEmpty() || services.isEmpty()) {
      return;
    }

    List<BeeColumn> serviceColumns = Data.getColumns(VIEW_TRADE_ACT_SERVICES);

    BeeRowSet selectedServices = new BeeRowSet(VIEW_TRADE_ACT_SERVICES, serviceColumns);

    List<String> notes = new ArrayList<>();

    int actIndex = selectedServices.getColumnIndex(COL_TRADE_ACT);

    int itemNameIndex = selectedServices.getColumnIndex(ALS_ITEM_NAME);
    int objectNameIndex = Data.getColumnIndex(VIEW_TRADE_ACTS, COL_COMPANY_OBJECT_NAME);
    int objectAddressIndex = Data.getColumnIndex(VIEW_TRADE_ACTS, COL_COMPANY_OBJECT_ADDRESS);

    for (Service svc : services) {
      if (ss.containsKey(svc.id())) {
        BeeRow row = DataUtils.cloneRow(svc.row);
        selectedServices.addRow(row);

        String itemName = svc.row.getString(itemNameIndex);

        Long actId = svc.row.getLong(actIndex);
        Act act = findAct(actId);

        String objectInfo = (act == null) ? null
            : BeeUtils.joinWords(act.row.getString(objectNameIndex),
                act.row.getString(objectAddressIndex));

        for (Integer idx : ss.get(svc.id())) {
          Range<DateTime> r = svc.ranges.get(idx);
          String period = Format.renderPeriod(r.lowerEndpoint(), r.upperEndpoint());
          notes.add(BeeUtils.joinItems(itemName, period, objectInfo));
        }
      }
    }

    if (selectedServices.isEmpty()) {
      return;
    }

    BeeKeeper.getStorage().set(storageKey(COL_TA_SERVICE_FROM), range.lowerEndpoint());
    BeeKeeper.getStorage().set(storageKey(COL_TA_SERVICE_TO), range.upperEndpoint());

    BeeKeeper.getStorage().set(storageKey(COL_TRADE_SALE_SERIES), BeeUtils.toString(seriesId));
    BeeKeeper.getStorage().set(storageKey(COL_TA_CURRENCY), currency);

    DataInfo dataInfo = Data.getDataInfo(VIEW_SALES);
    BeeRow invoice = RowFactory.createEmptyRow(dataInfo, true);

    if (DataUtils.isId(seriesId)) {
      invoice.setValue(dataInfo.getColumnIndex(COL_TRADE_SALE_SERIES), seriesId);
    }

    invoice.setValue(dataInfo.getColumnIndex(COL_TRADE_CUSTOMER), company);

    if (DataUtils.isId(currency)) {
      invoice.setValue(dataInfo.getColumnIndex(COL_TRADE_CURRENCY), currency);
    }

    invoice.setValue(dataInfo.getColumnIndex(COL_TRADE_MANAGER), BeeKeeper.getUser().getUserId());

    if (!notes.isEmpty()) {
      invoice.setValue(dataInfo.getColumnIndex(COL_TRADE_NOTES), BeeUtils.buildLines(notes));
    }

    BeeRowSet sales = DataUtils.createRowSetForInsert(dataInfo.getViewName(),
        dataInfo.getColumns(), invoice);

    BeeRowSet saleItems = createInvoiceItems(selectedServices, ss, currency);

    ParameterList params = TradeActKeeper.createArgs(SVC_CREATE_ACT_INVOICE);

    params.addDataItem(sales.getViewName(), sales.serialize());
    params.addDataItem(saleItems.getViewName(), saleItems.serialize());

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

  private BeeRowSet createInvoiceItems(BeeRowSet selectedServices,
      Multimap<Long, Integer> selectedRanges, Long currency) {

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
      Service svc = findService(row.getId());

      if (svc != null) {
        for (Integer idx : selectedRanges.get(row.getId())) {
          BeeRow inv = DataUtils.createEmptyRow(invoiceItems.getNumberOfColumns());

          for (Map.Entry<Integer, Integer> entry : indexes.entrySet()) {
            if (!row.isNull(entry.getKey())) {
              inv.setValue(entry.getValue(), row.getString(entry.getKey()));
            }
          }

          inv.setProperty(COL_TA_INVOICE_SERVICE, BeeUtils.toString(row.getId()));

          Double amount = svc.amount(idx, currency, date);

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

          JustDate from = svc.dateFrom(idx);
          JustDate to = svc.dateTo(idx);

          if (from != null) {
            inv.setProperty(PRP_TA_SERVICE_FROM, BeeUtils.toString(from.getDays()));
          }
          if (to != null) {
            inv.setProperty(PRP_TA_SERVICE_TO, BeeUtils.toString(to.getDays()));
          }

          invoiceItems.addRow(inv);
        }
      }
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
    Long svcId = DomUtils.getDataPropertyLong(rowElement, KEY_SERVICE_ID);

    return DataUtils.isId(svcId) ? findService(svcId) : null;
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

  private Long getSeriesId() {
    Widget widget = getFormView().getWidgetByName(COL_TRADE_SALE_SERIES);
    if (widget instanceof UnboundSelector) {
      return BeeUtils.toLongOrNull(((UnboundSelector) widget).getValue());
    } else {
      return null;
    }
  }

  private Range<JustDate> getRange() {
    JustDate start = getDateFrom();
    JustDate end = getDateTo();

    List<String> messages = new ArrayList<>();

    if (start == null) {
      Collections.addAll(messages, Localized.dictionary()
          .fieldRequired(Localized.dictionary().dateFrom()));

    } else if (end == null) {
      Collections.addAll(messages, Localized.dictionary()
          .fieldRequired(Localized.dictionary().dateTo()));

    } else if (TimeUtils.isMeq(start, end)) {
      Collections.addAll(messages, Localized.dictionary().invalidRange(),
          TimeUtils.renderPeriod(Format.renderDate(start), Format.renderDate(end)));
    }

    if (messages.isEmpty()) {
      return Range.closedOpen(start, end);
    } else {
      getFormView().notifyWarning(ArrayUtils.toArray(messages));
      return null;
    }
  }

  private Collection<Long> getSelectedActs(String styleSelected) {
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

  private Multimap<Long, Integer> getSelectedServices(String styleSelected) {
    Multimap<Long, Integer> result = ArrayListMultimap.create();

    NodeList<Element> nodes = Selectors.getNodes(getFormView().getElement(),
        Selectors.classSelector(styleSelected));

    if (nodes != null) {
      for (int i = 0; i < nodes.getLength(); i++) {
        Element node = nodes.getItem(i);
        Long svcId = DomUtils.getDataPropertyLong(node, KEY_SERVICE_ID);
        Integer idx = DomUtils.getDataPropertyInt(node, KEY_RANGE_INDEX);

        if (DataUtils.isId(svcId) && idx != null) {
          result.put(svcId, idx);
        }
      }
    }

    return result;
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
        Global.showError(Localized.dictionary().fieldRequired(Localized.dictionary().client()));
      }
      return;
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
            acts.add(act);
          }

          String vatPerc = rowSet.getTableProperty(PRM_VAT_PERCENT);
          if (!BeeUtils.isEmpty(vatPerc)) {
            defVatPercent = BeeUtils.toDoubleOrNull(vatPerc);
          }

          renderActs();

        } else if (notify) {
          getFormView().notifyWarning(Localized.dictionary().noData());
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

    table.setText(r, c++, Localized.dictionary().captionId(),
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

    table.setText(r, c++, Localized.dictionary().goods(),
        STYLE_ACT_TOTAL_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.dictionary().currencyShort(),
        STYLE_ACT_CURRENCY_PREFIX + STYLE_LABEL_CELL_SUFFIX);

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

      table.setText(r, c++, Format.renderDateTime(act.row.getDateTime(dateIndex)),
          STYLE_ACT_DATE_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, Format.renderDateTime(act.row.getDateTime(untilIndex)),
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

  private void renderServices(Collection<Integer> holidays) {
    DataInfo dataInfo = Data.getDataInfo(VIEW_TRADE_ACT_SERVICES);

    final int actIndex = dataInfo.getColumnIndex(COL_TRADE_ACT);
    final int itemIndex = dataInfo.getColumnIndex(COL_TA_ITEM);

    int nameIndex = dataInfo.getColumnIndex(ALS_ITEM_NAME);
    int articleIndex = dataInfo.getColumnIndex(COL_ITEM_ARTICLE);

    int qtyScale = dataInfo.getColumnScale(COL_TRADE_ITEM_QUANTITY);
    int unitNameIndex = dataInfo.getColumnIndex(ALS_UNIT_NAME);

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
    table.setText(r, c++, Localized.dictionary().unitShort(),
        STYLE_SVC_UNIT_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.getLabel(dataInfo.getColumn(COL_TRADE_ITEM_PRICE)),
        STYLE_SVC_PRICE_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.dictionary().currencyShort(),
        STYLE_SVC_CURRENCY_PREFIX + STYLE_LABEL_CELL_SUFFIX);

    table.setText(r, c++, Localized.dictionary().taFactorShort(),
        STYLE_SVC_FACTOR_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.dictionary().taDaysPerWeekShort(),
        STYLE_SVC_DPW_PREFIX + STYLE_LABEL_CELL_SUFFIX);
    table.setText(r, c++, Localized.dictionary().taMinTermShort(),
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
      for (int idx = 0; idx < svc.ranges.size(); idx++) {
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

        table.setText(r, c++, Format.renderDate(svc.dateFrom(idx)),
            STYLE_SVC_FROM_PREFIX + STYLE_CELL_SUFFIX);
        table.setText(r, c++, Format.renderDate(svc.dateTo(idx)),
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
          table.setText(r, c++, Format.renderDate(svc.dateFrom(idx)),
              STYLE_SVC_FACTOR_PREFIX + STYLE_CELL_SUFFIX);
        } else {
          table.setWidget(r, c++, createFactorWidget(svc.factors.get(idx)),
              STYLE_SVC_FACTOR_PREFIX + STYLE_CELL_SUFFIX);
        }

        if (svc.timeUnit == TradeActTimeUnit.DAY) {
          table.setWidget(r, c++, createDpwWidget(svc.dpws.get(idx), holidays),
              STYLE_SVC_DPW_PREFIX + STYLE_CELL_SUFFIX);
        } else {
          table.setText(r, c++, null, STYLE_SVC_DPW_PREFIX + STYLE_CELL_SUFFIX);
        }

        table.setText(r, c++, render(svc.minTerm), STYLE_SVC_MIN_TERM_CELL,
            svc.minTermWarn(idx, holidays) ? STYLE_SVC_MIN_TERM_WARN : null);

        table.setWidget(r, c++, createDiscountWidget(svc.discounts.get(idx)),
            STYLE_SVC_DISCOUNT_PREFIX + STYLE_CELL_SUFFIX);

        Double amount = svc.amount(idx, currency, date);
        table.setText(r, c++, renderAmount(amount), STYLE_SVC_AMOUNT_CELL);

        if (BeeUtils.isDouble(amount)) {
          total += amount;
        }

        Element rowElement = table.getRow(r);
        rowElement.addClassName(STYLE_SVC_ROW);

        DomUtils.setDataProperty(rowElement, KEY_SERVICE_ID, svc.id());
        DomUtils.setDataProperty(rowElement, KEY_RANGE_INDEX, idx);

        r++;
      }
    }

    c = 0;
    table.setText(r, c, Localized.dictionary().totalOf(),
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

  private Widget createDiscountWidget(Double initialValue) {
    InputNumber widget = new InputNumber();
    widget.addStyleName(STYLE_SVC_DISCOUNT_WIDGET);

    if (initialValue != null) {
      widget.setValue(initialValue);
    }

    widget.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        if (event.getSource() instanceof InputNumber) {
          InputNumber source = (InputNumber) event.getSource();
          Double value = source.getNumber();

          TableRowElement rowElement = DomUtils.getParentRow(source.getElement(), false);
          Long svcId = DomUtils.getDataPropertyLong(rowElement, KEY_SERVICE_ID);
          Integer idx = DomUtils.getDataPropertyInt(rowElement, KEY_RANGE_INDEX);

          if (DataUtils.isId(svcId) && idx != null) {
            Service service = findService(svcId);

            if (service != null && !Objects.equals(service.discounts.get(idx), value)) {
              service.discounts.set(idx, value);

              updateAmountCell(rowElement, service, idx);
              refreshTotals();
            }
          }
        }
      }
    });

    return widget;
  }

  private Widget createDpwWidget(Integer initialValue, final Collection<Integer> holidays) {
    ListBox widget = new ListBox();
    widget.addStyleName(STYLE_SVC_DPW_PREFIX + STYLE_INPUT_SUFFIX);

    for (int i = DPW_MIN; i <= DPW_MAX; i++) {
      widget.addItem(BeeUtils.joinWords(i, Localized.dictionary().dayShort()));
    }

    if (TradeActUtils.validDpw(initialValue)) {
      widget.setSelectedIndex(initialValue - DPW_MIN);
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
          Long svcId = DomUtils.getDataPropertyLong(rowElement, KEY_SERVICE_ID);
          Integer idx = DomUtils.getDataPropertyInt(rowElement, KEY_RANGE_INDEX);

          if (!DataUtils.isId(svcId) || idx == null) {
            return;
          }

          Service service = findService(svcId);
          if (service == null) {
            return;
          }

          int dpw = DPW_MIN + index;
          if (Objects.equals(service.dpws.get(idx), dpw)) {
            return;
          }

          double df = TradeActUtils.countServiceDays(service.ranges.get(idx), holidays, dpw);

          service.dpws.set(idx, dpw);
          service.factors.set(idx, df);

          Element mtc = Selectors.getElementByClassName(rowElement, STYLE_SVC_MIN_TERM_CELL);
          if (mtc != null) {
            if (service.minTermWarn(idx, holidays)) {
              mtc.addClassName(STYLE_SVC_MIN_TERM_WARN);
            } else {
              mtc.removeClassName(STYLE_SVC_MIN_TERM_WARN);
            }
          }

          Element input = Selectors.getElementByClassName(rowElement, STYLE_SVC_FACTOR_WIDGET);
          if (InputElement.is(input)) {
            InputElement.as(input).setValue(BeeUtils.toString(df));
          }

          updateAmountCell(rowElement, service, idx);
          refreshTotals();
        }
      }
    });

    return widget;
  }

  private Widget createFactorWidget(Double initialValue) {
    InputNumber widget = new InputNumber();
    widget.addStyleName(STYLE_SVC_FACTOR_WIDGET);

    if (initialValue != null) {
      widget.setValue(initialValue);
    }

    widget.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        if (event.getSource() instanceof InputNumber) {
          InputNumber source = (InputNumber) event.getSource();
          Double value = source.getNumber();

          TableRowElement rowElement = DomUtils.getParentRow(source.getElement(), false);
          Long svcId = DomUtils.getDataPropertyLong(rowElement, KEY_SERVICE_ID);
          Integer idx = DomUtils.getDataPropertyInt(rowElement, KEY_RANGE_INDEX);

          if (DataUtils.isId(svcId) && idx != null) {
            Service service = findService(svcId);

            if (service != null && !Objects.equals(service.factors.get(idx), value)) {
              service.factors.set(idx, value);

              updateAmountCell(rowElement, service, idx);
              refreshTotals();
            }
          }
        }
      }
    });

    return widget;
  }

  private String getAmountLabel() {
    return BeeUtils.joinWords(Localized.dictionary().amount(), getCurrencyName());
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
      TableRowElement rowElement = DomUtils.getParentRow(cell, true);

      Long svcId = DomUtils.getDataPropertyLong(rowElement, KEY_SERVICE_ID);
      Integer idx = DomUtils.getDataPropertyInt(rowElement, KEY_RANGE_INDEX);

      if (DataUtils.isId(svcId) && idx != null) {
        Service svc = findService(svcId);
        if (svc != null) {
          cell.setInnerText(renderAmount(svc.amount(idx, currency, date)));
        }
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
      for (int idx = 0; idx < svc.ranges.size(); idx++) {
        Double amount = svc.amount(idx, currency, date);

        if (BeeUtils.nonZero(amount)) {
          total += amount;
        }
      }
    }

    target.setInnerText(renderAmount(total));
  }

  private String storageKey(String name) {
    return BeeUtils.join(STORAGE_KEY_SEPARATOR, NameUtils.getName(this),
        BeeKeeper.getUser().getUserId(), name);
  }

  private void updateAmountCell(Element rowElement, Service svc, int idx) {
    Element cell = Selectors.getElementByClassName(rowElement, STYLE_SVC_AMOUNT_CELL);

    if (cell != null) {
      cell.setInnerText(renderAmount(svc.amount(idx, getCurrency(), TimeUtils.nowMinutes())));
    }
  }
}
