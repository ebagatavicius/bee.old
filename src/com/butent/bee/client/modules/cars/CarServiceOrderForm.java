package com.butent.bee.client.modules.cars;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.COL_EMPLOYEE;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.ChildSelector;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.administration.Stage;
import com.butent.bee.client.modules.trade.TradeDocumentsGrid;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HasStages;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.client.widget.CustomAction;
import com.butent.bee.client.widget.DoubleLabel;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.FontStyle;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.cars.CarsConstants;
import com.butent.bee.shared.modules.trade.ItemQuantities;
import com.butent.bee.shared.modules.trade.Totalizer;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocument;
import com.butent.bee.shared.modules.trade.TradeDocumentItem;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CarServiceOrderForm extends PrintFormInterceptor implements HasStages,
    SelectorEvent.Handler {

  private HasWidgets stageContainer;
  private List<Stage> orderStages;

  private CustomAction createInvoice = new CustomAction(FontAwesome.FILE_TEXT_O,
      clickEvent -> selectServicesAndJobs());

  private CustomAction copyAction = new CustomAction(FontAwesome.COPY,
      ev -> Global.confirm(Localized.dictionary().trCopyOrder(), this::copyServiceOrder));

  Widget customerWarning;
  List<String> customerMessages = new ArrayList<>();
  Widget carWarning;
  List<String> carMessages = new ArrayList<>();

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (Objects.equals(editableWidget.getColumnId(), COL_CAR) && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(this);
    }
    if (Objects.equals(editableWidget.getColumnId(), COL_CUSTOMER)
        && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(event -> {
        if (event.isChanged()) {
          showCustomerWarning();
        }
      });
    }
    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreatePresenter(Presenter presenter) {
    HeaderView hdr = presenter.getHeader();

    if (Data.isViewEditable(TBL_TRADE_DOCUMENTS)) {
      createInvoice.setTitle(Localized.dictionary().createInvoice());
      hdr.addCommandItem(createInvoice);
    }
    copyAction.setTitle(Localized.dictionary().actionCopy());
    hdr.addCommandItem(copyAction);

    super.afterCreatePresenter(presenter);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid) {
      switch (name) {
        case TBL_SERVICE_ORDER_ITEMS:
          ((ChildGrid) widget).setGridInterceptor(new CarServiceItemsGrid());
          break;
        case GRID_SERVICE_ORDER_JOBS:
          ((ChildGrid) widget).setGridInterceptor(new CarServiceJobsGrid());
          break;
        case TBL_SERVICE_EVENTS:
          ((ChildGrid) widget).setGridInterceptor(new CarServiceEventsGrid());
          break;
        case TBL_SERVICE_JOB_PROGRESS:
          ((ChildGrid) widget).setGridInterceptor(new CarJobProgressGrid());
          break;
      }
    }
    if (Objects.equals(name, VIEW_TRADE_DOCUMENTS) && widget instanceof GridPanel) {
      ((GridPanel) widget).setGridInterceptor(new TradeDocumentsGrid().setFilterSupplier(() ->
          Filter.custom(FILTER_CAR_SERVICE_DOCUMENTS, getActiveRowId())));
    }
    if (Objects.equals(name, COL_CUSTOMER + "Warning") && widget instanceof HasClickHandlers) {
      customerWarning = widget.asWidget();
      ((HasClickHandlers) customerWarning).addClickHandler(clickEvent ->
          Global.showInfo(Localized.dictionary().serviceOrders(), customerMessages));
    }
    if (Objects.equals(name, COL_CAR + "Warning") && widget instanceof HasClickHandlers) {
      carWarning = widget.asWidget();
      ((HasClickHandlers) carWarning).addClickHandler(clickEvent ->
          Global.showInfo(Localized.dictionary().recalls(), carMessages));
    }
    if (Objects.equals(name, TBL_SERVICE_SYMPTOMS) && widget instanceof ChildSelector) {
      ((ChildSelector) widget).addSelectorHandler(event -> {
        if (event.isOpened()) {
          Filter filter = Filter.isNull(COL_MODEL);

          if (DataUtils.isId(getLongValue(COL_MODEL))) {
            filter = Filter.or(filter, Filter.equals(COL_MODEL, getLongValue(COL_MODEL)));
          }
          event.getSelector().setAdditionalFilter(filter);
        }
      });
    }
    if (Objects.equals(name, TBL_STAGES) && widget instanceof HasWidgets) {
      stageContainer = (HasWidgets) widget;
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    refreshStages();

    showCustomerWarning();
    showCarWarning();

    Stream.of(createInvoice, copyAction).forEach(w -> w.setVisible(!DataUtils.isNewRow(row)));

    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new CarServiceOrderForm();
  }

  @Override
  public HasWidgets getStageContainer() {
    return stageContainer;
  }

  @Override
  public List<Stage> getStages() {
    return orderStages;
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    DataInfo eventInfo = Data.getDataInfo(getViewName());
    DataInfo carInfo = Data.getDataInfo(event.getRelatedViewName());
    Long owner = getLongValue(COL_CUSTOMER);

    if (event.isNewRow()) {
      RelationUtils.copyWithDescendants(eventInfo, COL_CUSTOMER, getActiveRow(),
          carInfo, COL_OWNER, event.getNewRow());

    } else if (event.isOpened()) {
      event.getSelector().setAdditionalFilter(Objects.isNull(owner) ? null
          : Filter.equals(COL_OWNER, owner));

    } else if (event.isChanged()) {
      if (Objects.isNull(owner)) {
        RelationUtils.copyWithDescendants(carInfo, COL_OWNER, event.getRelatedRow(),
            eventInfo, COL_CUSTOMER, getActiveRow());
        getFormView().refresh();
      } else {
        showCarWarning();
      }
    }
  }

  @Override
  public void onSourceChange(IsRow row, String source, String value) {
    if (!DataUtils.isNewRow(row) && !BeeUtils.isEmpty(source)) {
      switch (source) {
        case COL_TRADE_DOCUMENT_VAT_MODE:
          BeeRowSet rs = DataUtils.getUpdated(getViewName(), row.getId(), row.getVersion(),
              DataUtils.getColumn(source, getDataColumns()),
              getFormView().getOldRow().getString(getDataIndex(source)), value);

          if (!DataUtils.isEmpty(rs)) {
            Queries.updateRow(rs, RowUpdateCallback.refreshRow(getViewName(), true));
          }
          break;
      }
    }
    super.onSourceChange(row, source, value);
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    Global.getParameterRelation(PRM_SERVICE_WAREHOUSE, (id, text) -> {
      if (DataUtils.isId(id)) {
        newRow.setValue(getDataIndex(COL_WAREHOUSE), id);
        newRow.setValue(Data.getColumnIndex(TBL_SERVICE_ORDERS, ALS_WAREHOUSE_CODE), text);
        form.refreshBySource(COL_WAREHOUSE);
      }
    });
    super.onStartNewRow(form, oldRow, newRow);
  }

  @Override
  public void setStages(List<Stage> stages) {
    orderStages = stages;
  }

  private void copyServiceOrder() {
    IsRow order = getActiveRow();

    if (order == null) {
      return;
    }
    copyAction.running();

    DataInfo orderInfo = Data.getDataInfo(getViewName());
    BeeRow orderClone = RowFactory.createEmptyRow(orderInfo, true);

    orderInfo.getColumnNames(false).stream()
        .filter(col -> !BeeUtils.inList(col, COL_DATE, COL_ORDER_NO, COL_STAGE, COL_STAGE_NAME))
        .forEach(col -> {
          int idx = orderInfo.getColumnIndex(col);

          if (!BeeConst.isUndef(idx)) {
            orderClone.setValue(idx, order.getString(idx));
          }
        });
    Queries.insertRow(DataUtils.createRowSetForInsert(orderInfo.getViewName(),
        orderInfo.getColumns(), orderClone), new RowCallback() {
      @Override
      public void onSuccess(BeeRow newOrder) {
        Queries.getRowSet(TBL_SERVICE_ORDER_ITEMS, null,
            Filter.equals(COL_SERVICE_ORDER, order.getId()), new Queries.RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet rowSet) {
                Runnable finalizer = () -> {
                  copyAction.idle();
                  RowEditor.open(getViewName(), newOrder.getId(), Opener.MODAL);
                };
                if (!DataUtils.isEmpty(rowSet)) {
                  BeeRowSet newRowSet = DataUtils.createRowSetForInsert(rowSet);
                  int serviceOrderIdx = newRowSet.getColumnIndex(COL_SERVICE_ORDER);

                  for (BeeRow row : newRowSet) {
                    row.setValue(serviceOrderIdx, newOrder.getId());
                  }
                  Queries.insertRows(newRowSet, new RpcCallback<RowInfoList>() {
                    @Override
                    public void onSuccess(RowInfoList res) {
                      finalizer.run();
                    }
                  });
                } else {
                  finalizer.run();
                }
              }
            });
      }
    });
  }

  private void renderInvoiceTable(BeeRowSet rs, Map<Long, Pair<Double, Double>> quantities) {
    Map<IsRow, InputNumber> items = new HashMap<>();
    DoubleLabel itemsTotal = new DoubleLabel(true);
    Map<IsRow, InputNumber> jobs = new HashMap<>();
    DoubleLabel jobsTotal = new DoubleLabel(true);
    String view = rs.getViewName();

    HashMap<Long, Double> stocks = new HashMap<>();
    quantities.forEach((item, pair) ->
        stocks.put(item, Double.max(pair.getA() - pair.getB(), BeeConst.DOUBLE_ZERO)));

    Consumer<Map<IsRow, InputNumber>> totalizer = map -> {
      Totalizer tot = new Totalizer(Data.getColumns(view));
      tot.setQuantityFunction(row -> map.get(row).getNumber());

      (Objects.equals(map, jobs) ? jobsTotal : itemsTotal).setValue(map.keySet().stream()
          .mapToDouble(row -> BeeUtils.round(BeeUtils.unbox(tot.getTotal(row)), 2)).sum());
    };
    rs.forEach(row -> {
      double qty = BeeUtils.unbox(row.getDouble(rs.getColumnIndex(COL_TRADE_ITEM_QUANTITY)))
          - BeeUtils.unbox(row.getDouble(rs.getColumnIndex(ALS_COMPLETED)));

      if (BeeUtils.isPositive(qty)) {
        Map<IsRow, InputNumber> map = row.isNull(rs.getColumnIndex(COL_JOB)) ? items : jobs;
        row.setProperty(COL_RESERVE, qty);
        Long item = row.getLong(rs.getColumnIndex(COL_ITEM));

        double stock = stocks.getOrDefault(item,
            row.isTrue(rs.getColumnIndex(COL_ITEM_IS_SERVICE)) ? Double.MAX_VALUE
                : BeeConst.DOUBLE_ZERO);
        qty = BeeUtils.min(qty, stock);
        stocks.put(item, stock - qty);

        InputNumber input = new InputNumber();
        input.addInputHandler(event -> totalizer.accept(map));
        input.setWidth("80px");
        input.setMinValue("0");

        if (BeeUtils.isPositive(qty)) {
          input.setValue(qty);
        }
        map.put(row, input);
      }
    });
    Dictionary d = Localized.dictionary();
    Latch rNo = new Latch(0);

    HtmlTable table = new HtmlTable(StyleUtils.NAME_INFO_TABLE);
    table.getRowFormatter().addStyleName(rNo.get(), StyleUtils.className(FontWeight.BOLD));
    table.getRowFormatter().addStyleName(rNo.get(), StyleUtils.className(TextAlign.CENTER));

    Flow container = new Flow();
    container.add(new InlineLabel(d.quantity() + BeeConst.STRING_SPACE));
    FaLabel clear = new FaLabel(FontAwesome.TIMES, true);
    StyleUtils.setColor(clear, "red");
    clear.addClickHandler(event -> {
      items.values().forEach(InputText::clearValue);
      totalizer.accept(items);
      jobs.values().forEach(InputText::clearValue);
      totalizer.accept(jobs);
    });
    container.add(clear);
    int c = 0;
    table.setText(rNo.get(), c++, d.name());
    table.setText(rNo.get(), c++, d.article());
    table.setText(rNo.get(), c++, d.ordUncompleted());
    table.setText(rNo.get(), c++, d.unitShort());
    table.setWidget(rNo.get(), c++, container);
    table.setText(rNo.get(), c++, d.price());
    table.setText(rNo.get(), c++, d.discount());
    table.setText(rNo.get(), c, d.vat());

    Stream.of(2, 5, 6, 7).forEach(col ->
        table.setColumnCellClasses(col, StyleUtils.className(TextAlign.RIGHT)));

    Stream.of(items, jobs).filter(map -> !map.isEmpty()).forEach(map -> {
      boolean isJobs = Objects.equals(map, jobs);
      rNo.increment();
      table.getRowFormatter().addStyleName(rNo.get(), StyleUtils.className(FontStyle.ITALIC));
      table.getCellFormatter().setColSpan(rNo.get(), 0, 8);
      Flow flow = new Flow(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);
      flow.add(new Label(isJobs ? Localized.dictionary().serviceJobs()
          : Localized.dictionary().productsServices()));
      flow.getWidget(0).addStyleName(StyleUtils.NAME_FLEXIBLE);
      flow.add(new Label(d.total() + ": "));
      flow.add(isJobs ? jobsTotal : itemsTotal);
      table.setWidget(rNo.get(), 0, flow);

      map.forEach((row, input) -> {
        rNo.increment();
        int cNo = 0;
        table.setText(rNo.get(), cNo++, Data.getString(view, row, ALS_ITEM_NAME));
        table.setText(rNo.get(), cNo++, Data.getString(view, row, COL_ITEM_ARTICLE));
        table.setText(rNo.get(), cNo++, row.getProperty(COL_RESERVE));
        table.setText(rNo.get(), cNo++, Data.getString(view, row, ALS_UNIT_NAME));

        Flow cont = new Flow(StyleUtils.NAME_FLEX_BOX_HORIZONTAL + "-center");
        cont.add(input);

        FaLabel info = new FaLabel(FontAwesome.INFO_CIRCLE);
        info.addClickHandler(clickEvent ->
            TradeKeeper.getReservationsInfo(getLongValue(COL_WAREHOUSE),
                Data.getLong(view, row, COL_ITEM), Data.isTrue(view, row, COL_RESERVE)
                    ? getDateTimeValue(COL_DATE) : null, reservations ->
                    showReservations(BeeUtils.joinWords(Data.getString(view, row,
                        ALS_ITEM_NAME), BeeUtils.parenthesize(Localized.dictionary()
                            .trdStock() + ": " + quantities.getOrDefault(Data.getLong(view,
                        row, COL_ITEM), Pair.of(BeeConst.DOUBLE_ZERO, null)).getA())),
                        reservations)));

        if (Data.isTrue(view, row, COL_ITEM_IS_SERVICE)) {
          info.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);
        }
        cont.add(info);
        table.setWidget(rNo.get(), cNo++, cont);

        table.setText(rNo.get(), cNo++, Data.getString(view, row, COL_PRICE));

        table.setText(rNo.get(), cNo++, BeeUtils.join("", Data.getString(view, row,
            COL_TRADE_DOCUMENT_ITEM_DISCOUNT), Data.isNull(view, row,
            COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT) ? "" : "%"));

        table.setText(rNo.get(), cNo, BeeUtils.join("", Data.getString(view, row,
            COL_TRADE_DOCUMENT_ITEM_VAT), Data.isNull(view, row,
            COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT) ? "" : "%"));
      });
      totalizer.accept(map);
    });
    Global.inputWidget(d.createInvoice(), table, new InputCallback() {
      @Override
      public String getErrorMessage() {
        stocks.clear();
        quantities.forEach((item, pair) ->
            stocks.put(item, Double.max(pair.getA() - pair.getB(), BeeConst.DOUBLE_ZERO)));

        for (Map<IsRow, InputNumber> map : Arrays.asList(items, jobs)) {
          for (Map.Entry<IsRow, InputNumber> entry : map.entrySet()) {
            IsRow row = entry.getKey();
            InputNumber input = entry.getValue();
            Long item = Data.getLong(view, row, COL_ITEM);

            double stock = stocks.getOrDefault(item, Data.isTrue(view, row,
                COL_ITEM_IS_SERVICE) ? Double.MAX_VALUE : BeeConst.DOUBLE_ZERO);
            double qty = BeeUtils.min(row.getPropertyDouble(COL_RESERVE), stock);

            input.setMaxValue(BeeUtils.toString(qty));
            List<String> messages = input.validate(true);

            if (!BeeUtils.isEmpty(messages)) {
              input.setFocus(true);
              return BeeUtils.joinItems(messages);
            }
            stocks.put(item, stock - BeeUtils.unbox(input.getNumber()));
          }
        }
        return InputCallback.super.getErrorMessage();
      }

      @Override
      public void onSuccess() {
        ParameterList args = CarsKeeper.createSvcArgs(SVC_CREATE_INVOICE);
        TradeDocument doc = new TradeDocument(Global
            .getParameterRelation(PRM_SERVICE_TRADE_OPERATION), TradeDocumentPhase.COMPLETED);

        Map<Long, Double> data = new HashMap<>();

        Stream.of(items, jobs).forEach(map -> map.forEach((row, input) -> {
          Double qty = input.getNumber();

          if (BeeUtils.isPositive(qty)) {
            TradeDocumentItem tradeItem = doc.addItem(Data.getLong(view, row, COL_ITEM), qty);

            tradeItem.setPrice(Data.getDouble(view, row, COL_PRICE));

            tradeItem.setDiscount(Data.getDouble(view, row,
                COL_TRADE_DOCUMENT_ITEM_DISCOUNT));
            tradeItem.setDiscountIsPercent(Data.getBoolean(view, row,
                COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT));

            tradeItem.setVat(Data.getDouble(view, row, COL_TRADE_DOCUMENT_ITEM_VAT));
            tradeItem.setVatIsPercent(Data.getBoolean(view, row,
                COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT));

            data.put(row.getId(), qty);
          }
        }));
        args.addDataItem(view, Codec.beeSerialize(data));

        if (!doc.isValid()) {
          getFormView().notifyWarning(d.noData());
          return;
        }
        doc.setDocumentDiscountMode(TradeDiscountMode.FROM_AMOUNT);
        doc.setDate(TimeUtils.nowSeconds());
        doc.setCurrency(getLongValue(COL_CURRENCY));
        doc.setNumber1(getStringValue(COL_ORDER_NO));
        doc.setCustomer(getLongValue(COL_CUSTOMER));
        doc.setManager(getLongValue(COL_EMPLOYEE));
        doc.setWarehouseFrom(getLongValue(COL_WAREHOUSE));
        doc.setDocumentVatMode(EnumUtils.getEnumByIndex(TradeVatMode.class,
            getIntegerValue(COL_TRADE_DOCUMENT_VAT_MODE)));

        args.addDataItem(VAR_DOCUMENT, Codec.beeSerialize(doc));
        createInvoice.running();

        BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            createInvoice.idle();
            response.notify(getFormView());

            if (!response.hasErrors()) {
              getFormView().refresh();
              RowEditor.open(VIEW_TRADE_DOCUMENTS, response.getResponseAsLong(), Opener.NEW_TAB);
            }
          }
        });
      }
    });
  }

  private void selectServicesAndJobs() {
    Queries.getRowSet(TBL_SERVICE_ORDER_ITEMS, null,
        Filter.equals(COL_SERVICE_ORDER, getActiveRowId()), new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet itemsRs) {
            Set<Long> items = itemsRs.getDistinctLongs(itemsRs.getColumnIndex(COL_ITEM));
            Map<Long, Pair<Double, Double>> stocks = new HashMap<>();

            if (items.isEmpty()) {
              renderInvoiceTable(itemsRs, stocks);
            } else {
              DateTime dateTime = getDateTimeValue(COL_DATE);
              Set<Long> rsv = new HashSet<>();
              itemsRs.getRows().stream()
                  .filter(beeRow -> beeRow.isTrue(itemsRs.getColumnIndex(COL_RESERVE)))
                  .forEach(beeRow -> rsv.add(beeRow.getLong(itemsRs.getColumnIndex(COL_ITEM))));

              TradeKeeper.getStock(getLongValue(COL_WAREHOUSE), items, true, stockMultimap -> {
                stockMultimap.asMap().forEach((item, quantities) -> stocks.put(item,
                    Pair.of(quantities.stream().mapToDouble(ItemQuantities::getStock).sum(),
                        quantities.stream().mapToDouble(itemQuantities ->
                            itemQuantities.getReservedMap().entrySet().stream().filter(entry ->
                                !rsv.contains(item) || TimeUtils.isLess(entry.getKey(), dateTime))
                                .mapToDouble(Map.Entry::getValue).sum())
                            .sum())));

                renderInvoiceTable(itemsRs, stocks);
              });
            }
          }
        });
  }

  private void showCarWarning() {
    if (Objects.isNull(carWarning)) {
      return;
    }
    carMessages.clear();
    Long car = getLongValue(COL_CAR);

    if (DataUtils.isId(car)) {
      Queries.getRowSet(TBL_CAR_RECALLS, Arrays.asList(COL_CODE, CarsConstants.COL_DESCRIPTION),
          Filter.and(Filter.equals(COL_VEHICLE, car), Filter.isNull(COL_CHECKED)),
          new Queries.RowSetCallback() {
            @Override
            public void onSuccess(BeeRowSet result) {
              carMessages.clear();
              result.forEach(beeRow -> carMessages.add(BeeUtils.joinWords(
                  beeRow.getString(result.getColumnIndex(COL_CODE)),
                  beeRow.getString(result.getColumnIndex(CarsConstants.COL_DESCRIPTION)))));

              carWarning.setVisible(!carMessages.isEmpty());
            }
          });
    } else {
      carWarning.setVisible(false);
    }
  }

  private void showCustomerWarning() {
    if (Objects.isNull(customerWarning)) {
      return;
    }
    customerMessages.clear();
    Long customer = getLongValue(COL_CUSTOMER);

    if (DataUtils.isId(customer)) {
      Filter filter = Filter.equals(COL_CUSTOMER, customer);

      if (DataUtils.isId(getActiveRowId())) {
        filter = Filter.and(filter, Filter.compareId(Operator.NE, getActiveRowId()));
      }
      Queries.getRowSet(getViewName(), Arrays.asList(COL_ORDER_DATE, COL_ORDER_NO, COL_STAGE_NAME),
          filter, new Queries.RowSetCallback() {
            @Override
            public void onSuccess(BeeRowSet result) {
              customerMessages.clear();
              result.forEach(beeRow -> customerMessages.add(BeeUtils.joinWords(
                  beeRow.getDateTime(result.getColumnIndex(COL_ORDER_DATE)),
                  beeRow.getString(result.getColumnIndex(COL_ORDER_NO)),
                  beeRow.getString(result.getColumnIndex(COL_STAGE_NAME)))));

              customerWarning.setVisible(!customerMessages.isEmpty());
            }
          });
    } else {
      customerWarning.setVisible(false);
    }
  }

  private static void showReservations(String cap, Map<ModuleAndSub, Map<String, Double>> info) {
    Widget widget = TradeUtils.renderReservations(info);

    if (widget == null) {
      Global.showInfo(cap);
    } else {
      Global.showModalWidget(cap, widget);
    }
  }
}
