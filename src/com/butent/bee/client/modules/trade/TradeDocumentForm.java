package com.butent.bee.client.modules.trade;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Storage;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.TabGroup;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.DecimalLabel;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeDocumentSums;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collections;
import java.util.Objects;

public class TradeDocumentForm extends AbstractFormInterceptor {

  private static final BeeLogger logger = LogUtils.getLogger(TradeDocumentForm.class);

  private static final String NAME_SPLIT = "Split";

  private static final String NAME_AMOUNT = "TdAmount";
  private static final String NAME_DISCOUNT = "TdDiscount";
  private static final String NAME_WITHOUT_VAT = "TdWithoutVat";
  private static final String NAME_VAT = "TdVat";
  private static final String NAME_TOTAL = "TdTotal";

  private static final String NAME_PAID = "TdPaid";
  private static final String NAME_DEBT = "TdDebt";

  private static final String NAME_STATUS_UPDATED = "StatusUpdated";

  private static final String NAME_SUM_EXPANDER = "SumExpander";
  private static final String NAME_SOUTH_EXPANDER = "SouthExpander";

  private static final String STYLE_SUM_EXPANDED =
      BeeConst.CSS_CLASS_PREFIX + "trade-document-sum-expanded";

  private static final double DEFAULT_SOUTH_PERCENT = 50d;

  private static String getStorageKey(Direction direction) {
    return Storage.getUserKey(NameUtils.getClassName(TradeDocumentForm.class),
        direction.name().toLowerCase());
  }

  private final TradeDocumentSums tdSums = new TradeDocumentSums();

  private int southExpandedFrom;

  TradeDocumentForm() {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, COL_TRADE_DOCUMENT_PHASE) && widget instanceof TabGroup) {
      ((TabGroup) widget).addBeforeSelectionHandler(this::onPhaseTransition);

    } else if (BeeUtils.same(name, COL_TRADE_OPERATION) && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(event -> {
        if (event.isOpened()) {
          event.getSelector().setAdditionalFilter(getOperationFilter());
        } else if (event.isChanged()) {
          onOperationChange(event.getRelatedRow());
        }
      });

    } else if (BeeUtils.same(name, COL_TRADE_DOCUMENT_STATUS) && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(event -> {
        if (event.isOpened()) {
          event.getSelector().setAdditionalFilter(getStatusFilter());
        }
      });

    } else if (BeeUtils.same(name, GRID_TRADE_DOCUMENT_ITEMS) && widget instanceof ChildGrid) {
      TradeDocumentItemsGrid tdiGrid = new TradeDocumentItemsGrid();

      tdiGrid.setTdsSupplier(() -> tdSums);
      tdiGrid.setTdsListener(this::refreshSums);

      ((ChildGrid) widget).setGridInterceptor(tdiGrid);

    } else if (BeeUtils.same(name, GRID_TRADE_PAYMENTS) && widget instanceof ChildGrid) {
      TradePaymentsGrid tpGrid = new TradePaymentsGrid();

      tpGrid.setTdsSupplier(() -> tdSums);
      tpGrid.setTdsListener(() -> {
        double paid = tdSums.getPaid();

        refreshSum(NAME_PAID, paid);
        refreshSum(NAME_DEBT, tdSums.getTotal() - paid);
      });

      ((ChildGrid) widget).setGridInterceptor(tpGrid);

    } else if (BeeUtils.same(name, NAME_SPLIT) && widget instanceof Split) {
      ((Split) widget).addMutationHandler(event -> {
        if (event.getSource() instanceof Split) {
          Direction direction = Direction.parse(event.getOptions());

          if (direction != null) {
            Split split = (Split) event.getSource();
            switch (direction) {
              case EAST:
                saveEastSize(split);
                break;

              case SOUTH:
                if (southExpandedFrom <= 0) {
                  saveSouthSize(split);
                }
                break;

              default:
                saveSplitLayout(split);
            }
          }
        }
      });

    } else if (BeeUtils.same(name, NAME_SUM_EXPANDER) && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(event -> {
        if (event.getSource() instanceof HasCheckedness && getFormView() != null) {
          if (((HasCheckedness) event.getSource()).isChecked()) {
            getFormView().addStyleName(STYLE_SUM_EXPANDED);
          } else {
            getFormView().removeStyleName(STYLE_SUM_EXPANDED);
          }
        }
      });

    } else if (BeeUtils.same(name, NAME_SOUTH_EXPANDER) && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(event -> {
        if (event.getSource() instanceof HasCheckedness) {
          boolean expand = ((HasCheckedness) event.getSource()).isChecked();

          int top = BeeConst.UNDEF;
          if (event.getSource() instanceof HasOptions) {
            top = BeeUtils.toInt(((HasOptions) event.getSource()).getOptions());
          }

          expandSouth(expand, top);
        }
      });

    } else if (COL_TRADE_DATE.equals(name) || COL_TRADE_DOCUMENT_RECEIVED_DATE.equals(name)) {
      JustDate minDate = Global.getParameterDate(PRM_PROTECT_TRADE_DOCUMENTS_BEFORE);

      if (minDate != null && widget instanceof InputDate) {
        ((InputDate) widget).setMinDate(minDate);
      }
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    Split split = getSplit(form);

    if (split != null) {
      Integer eastSize = BeeKeeper.getStorage().getInteger(getStorageKey(Direction.EAST));

      Double southPercent = BeeKeeper.getStorage().getDouble(getStorageKey(Direction.SOUTH));
      if (!BeeUtils.isPositive(southPercent)) {
        southPercent = DEFAULT_SOUTH_PERCENT;
      }

      boolean doLayout = false;

      if (southExpandedFrom <= 0) {
        int height = split.getOffsetHeight();
        int size = BeeUtils.round(height * southPercent / BeeConst.DOUBLE_ONE_HUNDRED);

        if (size > 0 && size < height
            && !Objects.equals(split.getDirectionSize(Direction.SOUTH), size)) {

          split.setDirectionSize(Direction.SOUTH, size, false);
          doLayout = true;

          logger.debug(getClass().getSimpleName(), State.LOADED, Direction.SOUTH,
              southPercent, height, size);
        }
      }

      if (BeeUtils.isPositive(eastSize) && BeeUtils.isLess(eastSize, split.getOffsetWidth())
          && !Objects.equals(split.getDirectionSize(Direction.EAST), eastSize)) {

        split.setDirectionSize(Direction.EAST, eastSize, false);
        doLayout = true;

        logger.debug(getClass().getSimpleName(), State.LOADED, Direction.EAST,
            split.getOffsetWidth(), eastSize);
      }

      if (doLayout) {
        split.doLayout();
      }
    }

    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeDocumentForm();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return super.isRowEditable(row) && TradeUtils.isDocumentEditable(row);
  }

  @Override
  public void onSetActiveRow(IsRow row) {
    if (row == null) {
      tdSums.clear();

    } else {
      if (tdSums.updateDocumentId(row.getId())) {
        tdSums.clearItems();
      }

      tdSums.updateDocumentDiscount(getDocumentDiscount(row));

      tdSums.updateDiscountMode(getDiscountMode(row));
      tdSums.updateVatMode(getVatMode(row));
    }

    refreshSums();
    refreshStatusLastUpdated(row);

    super.onSetActiveRow(row);
  }

  @Override
  public void onSourceChange(IsRow row, String source, String value) {
    if (row != null && !BeeUtils.isEmpty(source)) {
      switch (source) {
        case COL_TRADE_DOCUMENT_DISCOUNT:
          if (tdSums.updateDocumentDiscount(BeeUtils.toDoubleOrNull(value))) {
            refreshSums();
          }
          break;

        case COL_TRADE_DOCUMENT_DISCOUNT_MODE:
          if (tdSums.updateDiscountMode(EnumUtils.getEnumByIndex(TradeDiscountMode.class, value))) {
            refreshSums();
            refreshItems();
          }
          break;

        case COL_TRADE_DOCUMENT_VAT_MODE:
          if (tdSums.updateVatMode(EnumUtils.getEnumByIndex(TradeVatMode.class, value))) {
            refreshSums();
            refreshItems();
          }
          break;
      }
    }

    super.onSourceChange(row, source, value);
  }

  private Double getDocumentDiscount(IsRow row) {
    return DataUtils.getDoubleQuietly(row, getDataIndex(COL_TRADE_DOCUMENT_DISCOUNT));
  }

  private TradeDiscountMode getDiscountMode(IsRow row) {
    return EnumUtils.getEnumByIndex(TradeDiscountMode.class,
        DataUtils.getIntegerQuietly(row, getDataIndex(COL_TRADE_DOCUMENT_DISCOUNT_MODE)));
  }

  private static Split getSplit(FormView form) {
    Widget widget = (form == null) ? null : form.getWidgetByName(NAME_SPLIT);
    return (widget instanceof Split) ? (Split) widget : null;
  }

  private TradeVatMode getVatMode(IsRow row) {
    return EnumUtils.getEnumByIndex(TradeVatMode.class,
        DataUtils.getIntegerQuietly(row, getDataIndex(COL_TRADE_DOCUMENT_VAT_MODE)));
  }

  private Filter getOperationFilter() {
    if (DataUtils.isId(getActiveRowId())) {
      OperationType operationType = getOperationType();
      TradeDocumentPhase phase = getPhase();

      if (operationType != null && phase != null && phase.modifyStock()) {
        return Filter.equals(COL_OPERATION_TYPE, operationType);
      }
    }
    return null;
  }

  private OperationType getOperationType() {
    return EnumUtils.getEnumByIndex(OperationType.class, getIntegerValue(COL_OPERATION_TYPE));
  }

  private TradeDocumentPhase getPhase() {
    return EnumUtils.getEnumByIndex(TradeDocumentPhase.class,
        getIntegerValue(COL_TRADE_DOCUMENT_PHASE));
  }

  private String getShortCaption() {
    String number = getStringValue(COL_TRADE_NUMBER);

    String s1;
    if (BeeUtils.isEmpty(number)) {
      s1 = BeeUtils.joinItems(getStringValue(COL_TRADE_DOCUMENT_NUMBER_1),
          getStringValue(COL_TRADE_DOCUMENT_NUMBER_2));
    } else {
      s1 = BeeUtils.joinWords(getStringValue(COL_SERIES), number);
    }

    return BeeUtils.joinItems(s1, getStringValue(COL_OPERATION_NAME));
  }

  private Filter getStatusFilter() {
    TradeDocumentPhase phase = getPhase();
    return (phase == null) ? null : Filter.notNull(phase.getStatusColumnName());
  }

  private boolean isOwner(IsRow row) {
    Long owner = row.getLong(getDataIndex(COL_TRADE_DOCUMENT_OWNER));
    return owner == null || BeeKeeper.getUser().is(owner);
  }

  private void maybeClearStatus(final IsRow row) {
    TradeDocumentPhase phase = TradeUtils.getDocumentPhase(row);

    final int statusIndex = getDataIndex(COL_TRADE_DOCUMENT_STATUS);
    Long status = row.getLong(statusIndex);

    if (DataUtils.isId(status) && phase != null) {
      Queries.getValue(VIEW_TRADE_STATUSES, status, phase.getStatusColumnName(),
          new RpcCallback<String>() {
            @Override
            public void onSuccess(String result) {
              if (!BeeConst.isTrue(result) && DataUtils.sameId(row, getActiveRow())) {
                getActiveRow().clearCell(statusIndex);
                RelationUtils.clearRelatedValues(Data.getDataInfo(getViewName()),
                    COL_TRADE_DOCUMENT_STATUS, getActiveRow());

                getFormView().refreshBySource(COL_TRADE_DOCUMENT_STATUS);
              }
            }
          });
    }
  }

  private void onOperationChange(IsRow operationRow) {
    if (operationRow != null) {
      getFormView().updateCell(COL_TRADE_DOCUMENT_PRICE_NAME,
          Data.getString(VIEW_TRADE_OPERATIONS, operationRow, COL_OPERATION_PRICE));

      getFormView().updateCell(COL_TRADE_DOCUMENT_VAT_MODE,
          Data.getString(VIEW_TRADE_OPERATIONS, operationRow, COL_OPERATION_VAT_MODE));

      getFormView().updateCell(COL_TRADE_DOCUMENT_DISCOUNT_MODE,
          Data.getString(VIEW_TRADE_OPERATIONS, operationRow, COL_OPERATION_DISCOUNT_MODE));

      OperationType operationType = Data.getEnum(VIEW_TRADE_OPERATIONS, operationRow,
          COL_OPERATION_TYPE, OperationType.class);
      TradeDocumentPhase phase = getPhase();

      Long warehouseFrom = Data.getLong(VIEW_TRADE_OPERATIONS, operationRow,
          COL_OPERATION_WAREHOUSE_FROM);
      Long warehouseTo = Data.getLong(VIEW_TRADE_OPERATIONS, operationRow,
          COL_OPERATION_WAREHOUSE_TO);

      if (operationType != null && phase != null && !phase.modifyStock()
          && operationType.consumesStock() == DataUtils.isId(warehouseFrom)
          && operationType.producesStock() == DataUtils.isId(warehouseTo)) {

        DataInfo targetInfo = Data.getDataInfo(getViewName());
        DataInfo sourceInfo = Data.getDataInfo(VIEW_TRADE_OPERATIONS);

        if (!Objects.equals(warehouseFrom, getLongValue(COL_TRADE_WAREHOUSE_FROM))) {
          RelationUtils.maybeUpdateColumn(targetInfo, COL_TRADE_WAREHOUSE_FROM, getActiveRow(),
              sourceInfo, COL_OPERATION_WAREHOUSE_FROM, operationRow);

          RelationUtils.maybeUpdateColumn(targetInfo, ALS_WAREHOUSE_FROM_CODE, getActiveRow(),
              sourceInfo, ALS_WAREHOUSE_FROM_CODE, operationRow);
          RelationUtils.maybeUpdateColumn(targetInfo, ALS_WAREHOUSE_FROM_NAME, getActiveRow(),
              sourceInfo, ALS_WAREHOUSE_FROM_NAME, operationRow);

          getFormView().refreshBySource(COL_TRADE_WAREHOUSE_FROM);
        }

        if (!Objects.equals(warehouseTo, getLongValue(COL_TRADE_WAREHOUSE_TO))) {
          RelationUtils.maybeUpdateColumn(targetInfo, COL_TRADE_WAREHOUSE_TO, getActiveRow(),
              sourceInfo, COL_OPERATION_WAREHOUSE_TO, operationRow);

          RelationUtils.maybeUpdateColumn(targetInfo, ALS_WAREHOUSE_TO_CODE, getActiveRow(),
              sourceInfo, ALS_WAREHOUSE_TO_CODE, operationRow);
          RelationUtils.maybeUpdateColumn(targetInfo, ALS_WAREHOUSE_TO_NAME, getActiveRow(),
              sourceInfo, ALS_WAREHOUSE_TO_NAME, operationRow);

          getFormView().refreshBySource(COL_TRADE_WAREHOUSE_TO);
        }
      }
    }
  }

  private void onPhaseTransition(BeforeSelectionEvent<Integer> event) {
    final IsRow row = getActiveRow();

    final TradeDocumentPhase from = getPhase();
    final TradeDocumentPhase to = EnumUtils.getEnumByIndex(TradeDocumentPhase.class,
        event.getItem());

    boolean fromStock = from != null && from.modifyStock();
    boolean toStock = to != null && to.modifyStock();

    if (row == null || to == null || from == to) {
      event.cancel();
      return;
    }

    if (TradeUtils.isDocumentProtected(row) || !super.isRowEditable(row)) {
      event.cancel();
      return;
    }

    if (from != null && !from.isEditable(BeeKeeper.getUser().isAdministrator()) && !isOwner(row)) {
      event.cancel();
      return;
    }

    if (DataUtils.isNewRow(row)) {
      setPhase(row, to);
      if (setOwner(row)) {
        getFormView().refreshBySource(COL_TRADE_DOCUMENT_OWNER);
      }

      maybeClearStatus(row);

    } else if (fromStock == toStock) {
      setPhase(row, to);
      if (setOwner(row)) {
        getFormView().refreshBySource(COL_TRADE_DOCUMENT_OWNER);
      }

      getFormView().saveChanges(new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          getFormView().setEnabled(isRowEditable(result));
          maybeClearStatus(result);
        }
      });

    } else {
      event.cancel();

      String frLabel = (from == null) ? BeeConst.NULL : from.getCaption();
      String toLabel = to.getCaption();
      String message = Localized.dictionary().trdDocumentPhaseTransitionQuestion(frLabel, toLabel);

      Global.confirm(getShortCaption(), Icon.QUESTION, Collections.singletonList(message),
          Localized.dictionary().actionChange(), Localized.dictionary().actionCancel(), () -> {
            if (DataUtils.sameId(row, getActiveRow())) {
              BeeRow newRow = DataUtils.cloneRow(getActiveRow());
              setPhase(newRow, to);
              setOwner(newRow);

              BeeRowSet rowSet = new BeeRowSet(getViewName(), getFormView().getDataColumns());
              rowSet.addRow(newRow);

              ParameterList params = TradeKeeper.createArgs(SVC_DOCUMENT_PHASE_TRANSITION);
              params.setSummary(getViewName(), newRow.getId());

              BeeKeeper.getRpc().sendText(params, rowSet.serialize(), new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  if (Queries.checkRowResponse(SVC_DOCUMENT_PHASE_TRANSITION, getViewName(),
                      response)) {

                    BeeRow r = BeeRow.restore(response.getResponseAsString());

                    RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), r, true);
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_STOCK);

                    getFormView().setEnabled(isRowEditable(getActiveRow()));
                  }
                }
              });
            }
          });
    }
  }

  private boolean setOwner(IsRow row) {
    int index = getDataIndex(COL_TRADE_DOCUMENT_OWNER);

    if (row == null || BeeKeeper.getUser().is(row.getLong(index))) {
      return false;

    } else {
      row.setValue(index, BeeKeeper.getUser().getUserId());
      RelationUtils.setUserFields(Data.getDataInfo(getViewName()), row, COL_TRADE_DOCUMENT_OWNER,
          BeeKeeper.getUser().getUserData());

      return true;
    }
  }

  private void setPhase(IsRow row, TradeDocumentPhase phase) {
    row.setValue(getDataIndex(COL_TRADE_DOCUMENT_PHASE), phase.ordinal());
  }

  private void refreshItems() {
    GridView gridView = ViewHelper.getChildGrid(getFormView(), GRID_TRADE_DOCUMENT_ITEMS);

    if (gridView != null) {
      gridView.refresh(false, false);
    }
  }

  private void refreshStatusLastUpdated(IsRow row) {
    final Widget widget = getWidgetByName(NAME_STATUS_UPDATED);

    if (widget instanceof HasHtml) {
      ((HasHtml) widget).setText(BeeConst.STRING_EMPTY);

      if (DataUtils.hasId(row)) {
        final long id = row.getId();

        Queries.getLastUpdated(TBL_TRADE_DOCUMENTS, id, COL_TRADE_DOCUMENT_STATUS,
            new RpcCallback<DateTime>() {
              @Override
              public void onSuccess(DateTime result) {
                if (result != null && Objects.equals(getActiveRowId(), id)) {
                  ((HasHtml) widget).setText(BeeUtils.joinWords(
                      Localized.dictionary().statusUpdated(),
                      Format.render(PredefinedFormat.DATE_SHORT_TIME_MEDIUM, result)));
                }
              }
            });
      }
    }
  }

  private void refreshSum(String name, double value) {
    Widget widget = getFormView().getWidgetByName(name);

    if (widget instanceof DecimalLabel) {
      ((DecimalLabel) widget).setValue(BeeUtils.toDecimalOrNull(value));
    }
  }

  private void refreshSums() {
    double amount = tdSums.getAmount();
    double discount = tdSums.getDiscount();
    double vat = tdSums.getVat();
    double total = tdSums.getTotal();

    double paid = tdSums.getPaid();

    refreshSum(NAME_AMOUNT, amount);
    refreshSum(NAME_DISCOUNT, discount);
    refreshSum(NAME_WITHOUT_VAT, total - vat);
    refreshSum(NAME_VAT, vat);
    refreshSum(NAME_TOTAL, total);

    refreshSum(NAME_PAID, paid);
    refreshSum(NAME_DEBT, total - paid);
  }

  private static void saveSplitLayout(Split split) {
    saveSouthSize(split);
    saveEastSize(split);
  }

  private static void saveSouthSize(Split split) {
    int southSize = split.getDirectionSize(Direction.SOUTH);
    int height = split.getOffsetHeight();

    if (height > 2) {
      southSize = BeeUtils.clamp(southSize, 1, height - 1);
      double southPercent = southSize * BeeConst.DOUBLE_ONE_HUNDRED / height;
      BeeKeeper.getStorage().set(getStorageKey(Direction.SOUTH), southPercent);
    }
  }

  private static void saveEastSize(Split split) {
    int eastSize = split.getDirectionSize(Direction.EAST);
    int width = split.getOffsetWidth();

    if (width > 2) {
      eastSize = BeeUtils.clamp(eastSize, 1, width - 1);
      BeeKeeper.getStorage().set(getStorageKey(Direction.EAST), eastSize);
    }
  }

  private void expandSouth(boolean expand, int top) {
    Split split = getSplit(getFormView());

    if (split != null) {
      int splitHeight = split.getOffsetHeight();

      int oldSize = split.getDirectionSize(Direction.SOUTH);
      int newSize;

      if (expand) {
        newSize = splitHeight - top;
        setSouthExpandedFrom(oldSize);

      } else {
        newSize = southExpandedFrom;
        setSouthExpandedFrom(BeeConst.UNDEF);
      }

      if (splitHeight <= 2) {
        newSize = oldSize;

      } else if (newSize <= 0 || newSize >= splitHeight) {
        if (expand) {
          newSize = BeeUtils.percent(splitHeight, 90d);
        } else {
          newSize = BeeUtils.percent(splitHeight, DEFAULT_SOUTH_PERCENT);
        }

        newSize = BeeUtils.clamp(newSize, 1, splitHeight - 1);
      }

      if (oldSize != newSize) {
        split.setDirectionSize(Direction.SOUTH, newSize, true);
      }
    }
  }

  private void setSouthExpandedFrom(int southExpandedFrom) {
    this.southExpandedFrom = southExpandedFrom;
  }
}
