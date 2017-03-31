package com.butent.bee.client.modules.cars;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.COL_OBJECT;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.COL_EMPLOYEE;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.ModalGrid;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.grid.CellKind;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.modules.administration.Stage;
import com.butent.bee.client.modules.administration.StageUtils;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.modules.classifiers.VehiclesGrid;
import com.butent.bee.client.modules.mail.NewMailMessage;
import com.butent.bee.client.modules.trade.TradeDocumentsGrid;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.output.ReportUtils;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HasStages;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.CustomAction;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.cars.CarsConstants;
import com.butent.bee.shared.modules.cars.Option;
import com.butent.bee.shared.modules.cars.Specification;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocument;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CarOrderForm extends SpecificationForm implements HasStages {

  private HasWidgets stageContainer;
  private List<Stage> orderStages;

  private CustomAction createInvoice = new CustomAction(FontAwesome.FILE_TEXT_O,
      clickEvent -> createCarInvoice());

  @Override
  public void afterCreatePresenter(Presenter presenter) {
    HeaderView hdr = presenter.getHeader();

    if (Data.isViewEditable(TBL_TRADE_DOCUMENTS)) {
      createInvoice.setTitle(Localized.dictionary().createInvoice());
      hdr.addCommandItem(createInvoice);
    }
    super.afterCreatePresenter(presenter);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (Objects.equals(name, COL_CAR) && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(clickEvent -> buildCar());
    }
    if (Objects.equals(name, TBL_STAGES) && widget instanceof HasWidgets) {
      stageContainer = (HasWidgets) widget;
    }
    if (Objects.equals(name, VIEW_TRADE_DOCUMENTS) && widget instanceof GridPanel) {
      ((GridPanel) widget).setGridInterceptor(new TradeDocumentsGrid().setFilterSupplier(() ->
          Filter.custom(FILTER_CAR_DOCUMENTS, getLongValue(COL_CAR))));
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    refreshStages();

    createInvoice.setVisible(!DataUtils.isNewRow(row));

    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new CarOrderForm();
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
  public void refreshStages() {
    HasStages.super.refreshStages();
    Stage stage = StageUtils.findStage(getStages(), getLongValue(COL_STAGE));
    getFormView().setEnabled(Objects.isNull(stage) || !stage.hasAction(STAGE_ACTION_READONLY));

    if (DataUtils.isNewRow(getActiveRow()) && Objects.isNull(stage)) {
      triggerStage(STAGE_TRIGGER_NEW);
    }
  }

  @Override
  public void setStages(List<Stage> stages) {
    orderStages = stages;
  }

  @Override
  public void updateStage(Stage stage) {
    Runnable doDefault = () -> HasStages.super.updateStage(stage);

    if (stage.hasAction(STAGE_ACTION_LOST)) {
      InputArea comment = new InputArea();
      comment.setWidth("100%");
      comment.setVisibleLines(4);

      Relation relation = Relation.create(TBL_LOSS_REASONS,
          Collections.singletonList(COL_LOSS_REASON_NAME));

      relation.disableNewRow();
      relation.disableEdit();
      UnboundSelector reason = UnboundSelector.create(relation);

      reason.addSelectorHandler(event -> {
        if (event.isChanged()) {
          comment.setValue(event.getRelatedRow() != null
              ? Data.getString(event.getRelatedViewName(), event.getRelatedRow(),
              COL_LOSS_REASON_TEMPLATE) : null);
        }
      });
      HtmlTable layout = new HtmlTable();
      layout.setText(0, 0, Localized.dictionary().reason());
      layout.setWidget(0, 1, reason);
      layout.getCellFormatter().setColSpan(1, 0, 2);
      layout.setText(1, 0, Localized.dictionary().comment());
      layout.getCellFormatter().setColSpan(2, 0, 2);
      layout.setWidget(2, 0, comment);

      Global.inputWidget(stage.getName(), layout, new InputCallback() {
        @Override
        public String getErrorMessage() {
          if (!DataUtils.isId(reason.getRelatedId())) {
            reason.setFocus(true);
            return Localized.dictionary().valueRequired();
          }
          return null;
        }

        @Override
        public void onSuccess() {
          IsRow row = getActiveRow();
          row.setValue(getFormView().getDataIndex(COL_LOSS_REASON), reason.getRelatedId());
          row.setValue(getFormView().getDataIndex(COL_LOSS_REASON_NAME), reason.getDisplayValue());
          row.setValue(getFormView().getDataIndex(COL_LOSS_NOTES), comment.getValue());
          doDefault.run();
        }
      });
    } else {
      doDefault.run();
    }
  }

  @Override
  protected void buildSpecification() {
    Global.choice(null, null, Arrays.asList(Localized.dictionary().specification(),
        Localized.dictionary().template(), Localized.dictionary().car()), value -> {
      switch (value) {
        case 0:
          new SpecificationBuilder(hasCar(getActiveRow()) ? null : getSpecification(), this);
          break;

        case 1:
          GridFactory.openGrid(TBL_CONF_TEMPLATES, new AbstractGridInterceptor() {
            @Override
            public GridInterceptor getInstance() {
              return null;
            }

            @Override
            public boolean initDescription(GridDescription gridDescription) {
              gridDescription.setReadOnly(true);
              return super.initDescription(gridDescription);
            }

            @Override
            public void onEditStart(EditStartEvent event) {
              event.consume();
              getGridPresenter().handleAction(Action.CLOSE);

              Long obj = event.getRowValue().getLong(getGridView().getDataIndex(COL_OBJECT));

              if (DataUtils.isId(obj)) {
                ParameterList args = CarsKeeper.createSvcArgs(SVC_GET_OBJECT);
                args.addDataItem(COL_OBJECT, obj);

                BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                  @Override
                  public void onResponse(ResponseObject response) {
                    response.notify(getFormView());

                    if (!response.hasErrors()) {
                      new SpecificationBuilder(Specification
                          .restore(response.getResponseAsString()), CarOrderForm.this);
                    }
                  }
                });
              } else {
                new SpecificationBuilder(null, CarOrderForm.this);
              }
            }
          }, null, ModalGrid.opener(80, CssUnit.PCT, 70, CssUnit.PCT));
          break;

        case 2:
          FormView form = getFormView();
          Long oldObject = form.getLongValue(COL_OBJECT);

          GridFactory.openGrid(VIEW_CARS, new VehiclesGrid() {
            @Override
            public boolean initDescription(GridDescription gridDescription) {
              gridDescription.setReadOnly(true);
              return super.initDescription(gridDescription);
            }

            @Override
            public void onEditStart(EditStartEvent event) {
              event.consume();
              getGridPresenter().handleAction(Action.CLOSE);

              Map<String, String> updates = new HashMap<>();
              updates.put(COL_CAR, BeeUtils.toString(event.getRowValue().getId()));

              if (DataUtils.isId(oldObject)) {
                updates.put(COL_OBJECT, null);
              }
              commit(form, updates, updatedRow -> {
                if (DataUtils.isId(oldObject)) {
                  Queries.deleteRow(TBL_CONF_OBJECTS, oldObject, new Queries.IntCallback() {
                    @Override
                    public void onSuccess(Integer cnt) {
                      if (BeeUtils.isPositive(cnt)) {
                        Data.onViewChange(TBL_CAR_ORDER_ITEMS, DataChangeEvent.RESET_REFRESH);
                      }
                    }
                  });
                }
              });
            }
          }, null, ModalGrid.opener(80, CssUnit.PCT, 70, CssUnit.PCT));
          break;
      }
    });
  }

  @Override
  protected void commit(Specification specification) {
    Map<Long, Integer> opts = new HashMap<>();
    Holder<Integer> price = Holder.of(0);

    if (Objects.nonNull(specification)) {
      for (Option option : specification.getOptions()) {
        opts.put(option.getId(), specification.getOptionPrice(option));
      }
      price.set(specification.getPrice());
    }
    Runnable runnable = () -> {
      Map<String, String> updates = new HashMap<>();
      updates.put(COL_OBJECT,
          Objects.nonNull(specification) ? BeeUtils.toString(specification.getId()) : null);
      updates.put(COL_TRADE_AMOUNT, BeeUtils.toString(price.get()));
      updates.put(COL_CAR, null);

      Long oldObject = getLongValue(COL_OBJECT);

      commit(getFormView(), updates, updatedRow -> {
        if (DataUtils.isId(oldObject)) {
          Queries.deleteRow(TBL_CONF_OBJECTS, oldObject, new Queries.IntCallback() {
            @Override
            public void onSuccess(Integer cnt) {
              if (BeeUtils.isPositive(cnt)) {
                Data.onViewChange(TBL_CAR_ORDER_ITEMS, DataChangeEvent.RESET_REFRESH);
              }
            }
          });
        }
      });
    };
    if (opts.isEmpty()) {
      runnable.run();
    } else {
      Queries.getRowSet(TBL_CONF_OPTIONS, Collections.singletonList(COL_ITEM),
          Filter.and(Filter.idIn(opts.keySet()), Filter.notNull(COL_ITEM)),
          new Queries.RowSetCallback() {
            @Override
            public void onSuccess(BeeRowSet res) {
              BeeRowSet items = new BeeRowSet(TBL_CAR_ORDER_ITEMS,
                  Data.getColumns(TBL_CAR_ORDER_ITEMS, Arrays.asList(COL_ORDER, COL_ITEM,
                      COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE, COL_OBJECT)));

              for (BeeRow item : res) {
                Integer prc = opts.get(item.getId());
                price.set(price.get() - BeeUtils.unbox(prc));

                BeeRow row = items.addEmptyRow();
                row.setValue(items.getColumnIndex(COL_ORDER), getActiveRowId());
                row.setValue(items.getColumnIndex(COL_ITEM), item.getLong(0));
                row.setValue(items.getColumnIndex(COL_TRADE_ITEM_QUANTITY), 1);
                row.setValue(items.getColumnIndex(COL_TRADE_ITEM_PRICE), prc);
                row.setValue(items.getColumnIndex(COL_OBJECT), specification.getId());
              }
              if (!DataUtils.isEmpty(items)) {
                Queries.insertRows(items, new RpcCallback<RowInfoList>() {
                  @Override
                  public void onSuccess(RowInfoList result) {
                    runnable.run();
                  }
                });
              } else {
                runnable.run();
              }
            }
          });
    }
  }

  @Override
  protected Long getObjectId(IsRow row) {
    if (Objects.nonNull(row) && hasCar(row)) {
      return row.getLong(getDataIndex(COL_CAR + COL_OBJECT));
    }
    return super.getObjectId(row);
  }

  @Override
  protected ReportUtils.ReportCallback getReportCallback() {
    return new ReportUtils.ReportCallback() {
      @Override
      public void accept(FileInfo fileInfo) {
        NewMailMessage.create(BeeUtils.notEmpty(getStringValue("ContactEmail"),
            getStringValue("CustomerEmail")), BeeUtils.joinWords(Localized.dictionary().offer(),
            getStringValue("OrderNo")), null, Collections.singleton(fileInfo),
            (messageId, isSaved) -> triggerStage(STAGE_TRIGGER_SENT));
      }

      @Override
      public Widget getActionWidget() {
        FaLabel action = new FaLabel(FontAwesome.ENVELOPE_O);
        action.setTitle(Localized.dictionary().trWriteEmail());
        return action;
      }
    };
  }

  @Override
  protected void getReportData(Consumer<BeeRowSet[]> dataConsumer) {
    Queries.getRowSet(TBL_CAR_ORDER_ITEMS, null, Filter.equals(COL_ORDER, getActiveRowId()),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            dataConsumer.accept(new BeeRowSet[] {result});
          }
        });
  }

  @Override
  protected void getReportParameters(Consumer<Map<String, String>> parametersConsumer) {
    Map<String, Long> companies = new HashMap<>();
    companies.put(COL_TRADE_CUSTOMER, getLongValue(COL_TRADE_CUSTOMER));
    companies.put(COL_TRADE_SUPPLIER, BeeKeeper.getUser().getCompany());

    super.getReportParameters(defaultParameters ->
        ClassifierUtils.getCompaniesInfo(companies, companiesInfo -> {
          defaultParameters.putAll(companiesInfo);
          Specification specification = getSpecification();

          if (specification != null) {
            BeeRowSet critRs = new BeeRowSet(COL_CRITERIA,
                Arrays.asList(new BeeColumn(DocumentConstants.COL_CRITERION_NAME),
                    new BeeColumn(DocumentConstants.COL_CRITERION_VALUE)));

            specification.getBundle().getOptions().forEach(opt ->
                critRs.addRow(0, 0, Arrays.asList(opt.getDimension().getName(), opt.getName())));

            specification.getOptions().stream().filter(opt -> opt.getDimension().isRequired())
                .forEach(opt -> critRs.addRow(0, 0,
                    Arrays.asList(opt.getDimension().getName(), opt.getName())));

            specification.getCriteria().forEach((key, val) ->
                critRs.addRow(0, 0, Arrays.asList(key, val)));

            specification.getPhotos()
                .forEach((key, val) -> defaultParameters.put(COL_PHOTO + key, val));

            defaultParameters.put(CarsConstants.COL_BRANCH_NAME, specification.getBranchName());
            defaultParameters.put(COL_CRITERIA, critRs.serialize());
            defaultParameters.put(COL_DESCRIPTION, specification.getDescription());
          }
          parametersConsumer.accept(defaultParameters);
        }));
  }

  private void buildCar() {
    if (hasCar(getActiveRow())) {
      RowEditor.open(VIEW_CARS, getLongValue(COL_CAR), Opener.NEW_TAB);
      return;
    }
    if (!getFormView().isEnabled()) {
      return;
    }
    Long obj = getLongValue(COL_OBJECT);

    DataInfo info = Data.getDataInfo(VIEW_CARS);
    BeeRow car = RowFactory.createEmptyRow(info, true);
    car.setValue(info.getColumnIndex(COL_OBJECT), obj);

    RowFactory.createRow(info, car, Modality.ENABLED, new RowCallback() {
      @Override
      public void onSuccess(BeeRow newCar) {
        Map<String, String> updates = new HashMap<>();
        updates.put(COL_OBJECT, null);
        updates.put(COL_CAR, BeeUtils.toString(newCar.getId()));

        commit(getFormView(), updates, updatedRow -> {
          if (DataUtils.isId(obj)) {
            Queries.update(TBL_CAR_ORDER_ITEMS, Filter.equals(COL_OBJECT, obj), COL_OBJECT,
                (String) null, null);
          }
        });
      }
    });
  }

  private void createCarInvoice() {
    Dictionary d = Localized.dictionary();

    if (!hasCar(getActiveRow())) {
      getFormView().notifySevere(d.valueRequired() + ":", d.car());
      return;
    }
    HtmlTable table = new HtmlTable();
    table.setColumnCellClasses(0, StyleUtils.NAME_REQUIRED);
    table.setColumnCellKind(0, CellKind.LABEL);

    Relation op = Relation.create(TBL_TRADE_OPERATIONS,
        Collections.singletonList(COL_OPERATION_NAME));
    op.disableNewRow();
    op.disableEdit();
    op.setFilter(Filter.equals(COL_OPERATION_TYPE, OperationType.PURCHASE));

    UnboundSelector operation = UnboundSelector.create(op);
    Holder<IsRow> opHolder = Holder.absent();

    Relation wh = Relation.create(TBL_WAREHOUSES,
        Arrays.asList(COL_WAREHOUSE_CODE, COL_WAREHOUSE_NAME));
    wh.disableNewRow();
    wh.disableEdit();

    UnboundSelector warehouse = UnboundSelector.create(wh);

    Relation itm = Relation.create(TBL_ITEMS, Arrays.asList(COL_ITEM_NAME, COL_ITEM_ARTICLE));
    itm.disableNewRow();
    itm.disableEdit();

    UnboundSelector item = UnboundSelector.create(itm);

    table.setText(0, 0, d.trdOperation());
    table.setWidget(0, 1, operation);

    table.setText(1, 0, d.trdWarehouseTo());
    table.setWidget(1, 1, warehouse);

    table.setText(2, 0, d.trAccountingItem());
    table.setWidget(2, 1, item);

    Queries.getValue(VIEW_CARS, getLongValue(COL_CAR), COL_ITEM, new RpcCallback<String>() {
      @Override
      public void onSuccess(String itemId) {
        if (DataUtils.isId(itemId)) {
          item.setValue(BeeUtils.toLongOrNull(itemId), false);
        }
      }
    });
    operation.addSelectorHandler(event -> {
      if (event.isChanged()) {
        opHolder.set(event.getRelatedRow());

        if (!DataUtils.isId(warehouse.getValue())) {
          warehouse.setValue(Data.getLong(event.getRelatedViewName(), opHolder.get(),
              COL_OPERATION_WAREHOUSE_TO), false);
        }
      }
    });
    Global.inputWidget(d.createInvoice(), table, new InputCallback() {
      @Override
      public String getErrorMessage() {
        Optional<UnboundSelector> empty = Stream.of(operation, warehouse, item)
            .filter(w -> BeeUtils.isEmpty(w.getValue()))
            .findFirst();

        if (empty.isPresent()) {
          empty.get().setFocus(true);
          return d.valueRequired();
        }
        return null;
      }

      @Override
      public void onSuccess() {
        TradeDocument doc = new TradeDocument(opHolder.get().getId(), TradeDocumentPhase.PENDING);
        doc.addItem(BeeUtils.toLongOrNull(item.getValue()), 1.0)
            .setItemVehicle(getLongValue(COL_CAR));

        doc.setDocumentDiscountMode(Data.getEnum(op.getViewName(), opHolder.get(),
            COL_OPERATION_DISCOUNT_MODE, TradeDiscountMode.class));
        doc.setDocumentVatMode(Data.getEnum(op.getViewName(), opHolder.get(),
            COL_OPERATION_VAT_MODE, TradeVatMode.class));

        doc.setDate(TimeUtils.nowSeconds());
        doc.setCurrency(Global.getParameterRelation(PRM_CURRENCY));
        doc.setManager(getLongValue(COL_EMPLOYEE));
        doc.setVehicle(getLongValue(COL_CAR));
        doc.setWarehouseTo(BeeUtils.toLongOrNull(warehouse.getValue()));

        TradeKeeper.createDocument(doc, tradeId -> {
          DataChangeEvent.fireLocalRefresh(BeeKeeper.getBus(), VIEW_TRADE_DOCUMENTS);
          RowEditor.open(VIEW_TRADE_DOCUMENTS, tradeId, Opener.NEW_TAB);
        });
      }
    });
  }

  private boolean hasCar(IsRow row) {
    return DataUtils.isId(row.getLong(getDataIndex(COL_CAR)));
  }
}
