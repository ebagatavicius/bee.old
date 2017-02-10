package com.butent.bee.client.modules.orders;

import com.google.common.collect.Lists;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.grid.column.CalculatedColumn;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.modules.trade.acts.ItemPricePicker;
import com.butent.bee.client.modules.transport.InvoiceCreator;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.ColumnInfo;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.orders.OrdersConstants.*;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OrderItemsGrid extends AbstractGridInterceptor implements SelectionHandler<BeeRowSet> {

  Long orderForm;
  private OrderItemsPicker picker;
  private Flow invoice = new Flow();

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().clearCommandPanel();
    FaLabel reCalculate = new FaLabel(FontAwesome.CALCULATOR);
    reCalculate.addStyleName(BeeConst.CSS_CLASS_PREFIX + "reCalculate");
    reCalculate.setTitle(Localized.dictionary().taRecalculatePrices());

    reCalculate.addClickHandler(event -> recalculatePrices());

    presenter.getHeader().addCommandItem(reCalculate);
    presenter.getHeader().addCommandItem(invoice);

    super.afterCreatePresenter(presenter);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    IsRow parentRow = ViewHelper.getFormRow(presenter.getMainView());

    int warehouseIdx = Data.getColumnIndex(VIEW_ORDERS, COL_WAREHOUSE);
    int statusIdx = Data.getColumnIndex(VIEW_ORDERS, COL_ORDERS_STATUS);
    String warehouse = parentRow.getString(warehouseIdx);
    int status = parentRow.getInteger(statusIdx);

    if (BeeUtils.isEmpty(warehouse) && status == OrdersStatus.APPROVED.ordinal()) {
      presenter.getGridView().notifySevere(Localized.dictionary()
          .fieldRequired(Localized.dictionary().warehouse()));
    } else {
      ensurePicker().show(parentRow, presenter.getMainView().getElement());
    }

    return false;
  }

  @Override
  public void beforeRender(GridView gridView, RenderingEvent event) {
    if (!gridView.isEnabled()) {
      gridView.asWidget().addStyleName(BeeConst.CSS_CLASS_PREFIX + "ItemPricePicker-disabled");
    } else {
      gridView.asWidget().setStyleName(BeeConst.CSS_CLASS_PREFIX + "ItemPricePicker-disabled",
          false);
    }

    IsRow parentRow = ViewHelper.getFormRow(gridView);
    FormView parentForm = ViewHelper.getForm(gridView);

    if (parentRow == null || parentForm == null) {
      return;
    }

    DataSelector wrhSelector = (DataSelector) parentForm.getWidgetBySource(COL_WAREHOUSE);
    if (wrhSelector != null) {
      wrhSelector.setEnabled(checkIsWarehouseEditable());
    }

    List<ColumnInfo> predefinedColumns = gridView.getGrid().getPredefinedColumns();
    List<Integer> visibleColumns = gridView.getGrid().getVisibleColumns();

    List<Integer> showColumns = new ArrayList<>();
    int status = parentRow.getInteger(Data.getColumnIndex(VIEW_ORDERS, COL_ORDERS_STATUS));
    if (status == OrdersStatus.APPROVED.ordinal()) {
      int qtyPosition = BeeConst.UNDEF;
      int unitPosition = BeeConst.UNDEF;

      for (int i = 0; i < visibleColumns.size(); i++) {
        ColumnInfo columnInfo = predefinedColumns.get(visibleColumns.get(i));

        if (columnInfo.is(COL_TRADE_ITEM_QUANTITY)) {
          qtyPosition = i;
        } else if (columnInfo.is(ALS_UNIT_NAME)) {
          unitPosition = i;
        }
      }

      int resRemIndex = BeeConst.UNDEF;

      for (int i = 0; i < predefinedColumns.size(); i++) {
        ColumnInfo columnInfo = predefinedColumns.get(i);

        if (columnInfo.is(COL_RESERVED_REMAINDER)) {
          resRemIndex = i;
        }
      }

      showColumns.addAll(visibleColumns);

      int pos = (unitPosition == qtyPosition + 1) ? unitPosition : qtyPosition;
      if (!BeeConst.isUndef(resRemIndex) && !showColumns.contains(resRemIndex)) {
        showColumns.add(pos + 2, resRemIndex);
      }
    } else {
      for (int index : visibleColumns) {
        if (!predefinedColumns.get(index).is(COL_RESERVED_REMAINDER)) {
          showColumns.add(index);
        }
      }
    }

    gridView.getGrid().overwriteVisibleColumns(showColumns);
    event.setDataChanged();

    super.beforeRender(gridView, event);
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.inList(columnName, COL_RESERVED_REMAINDER, COL_TRADE_ITEM_QUANTITY,
        COL_TRADE_ITEM_PRICE, "ItemPrices", COL_TRADE_DISCOUNT) && editableColumn != null) {
      editableColumn.addCellValidationHandler(event -> {
        if (event.isCellValidation() && event.isPostValidation()) {
          CellValidation cv = event.getCellValidation();
          IsRow row = cv.getRow();
          double freeRem = BeeUtils.toDouble(row.getProperty(PRP_FREE_REMAINDER));
          double qty =
              BeeUtils.unbox(row.getDouble(Data.getColumnIndex(VIEW_ORDER_ITEMS,
                  COL_TRADE_ITEM_QUANTITY)));
          double newValue = BeeUtils.toDouble(cv.getNewValue());
          double oldValue = BeeUtils.toDouble(cv.getOldValue());

          List<BeeColumn> cols = null;
          List<String> oldValues = null;
          List<String> newValues = null;

          switch (event.getColumnId()) {
            case COL_RESERVED_REMAINDER:

              if (newValue < 0) {
                getGridPresenter().getGridView().notifySevere(
                    Localized.dictionary().minValue() + " 0");
                return false;
              }

              if (freeRem == 0) {
                if (newValue > oldValue) {
                  getGridPresenter().getGridView().notifySevere(
                      Localized.dictionary().ordResNotIncrease());
                  return false;
                }

              } else if (newValue > qty || newValue > freeRem) {
                getGridPresenter().getGridView().notifySevere(
                    Localized.dictionary().ordResQtyIsTooBig());
                return false;
              }

              cols = Lists.newArrayList(cv.getColumn());
              oldValues = Lists.newArrayList(cv.getOldValue());
              newValues = Lists.newArrayList(cv.getNewValue());

              break;

            case COL_TRADE_ITEM_QUANTITY:

              if (!BeeUtils.isPositive(newValue)) {
                getGridPresenter().getGridView().notifySevere(
                    Localized.dictionary().minValue() + " > 0");
                return false;
              }
              IsRow parentRow = ViewHelper.getFormRow(getGridPresenter().getMainView());
              int status = parentRow.getInteger(Data.getColumnIndex(VIEW_ORDERS,
                  COL_ORDERS_STATUS));

              int updIndex = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_RESERVED_REMAINDER);
              double updValue = BeeUtils.unbox(row.getDouble(updIndex));

              BeeColumn updColumn = Data.getColumn(VIEW_ORDER_ITEMS, COL_RESERVED_REMAINDER);

              if (newValue <= (updValue + freeRem)) {
                updValue = newValue;
              } else {
                updValue += freeRem;
              }

              if (OrdersStatus.APPROVED.ordinal() != status) {
                updValue = 0;
              }

              cols = Lists.newArrayList(cv.getColumn(), updColumn);
              oldValues = Lists.newArrayList(cv.getOldValue(),
                  row.getString(updIndex));
              newValues = Lists.newArrayList(cv.getNewValue(),
                  BeeUtils.toString(updValue));

              Long supplier =
                  row.getLong(Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_TRADE_SUPPLIER));
              Integer pckUnits =
                  row.getInteger(Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_ITEM_PACKAGE_UNITS));
              String attribute =
                  row.getString(Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_ITEM_ATTRIBUTE));

              if (BeeUtils.isPositive(supplier) && BeeUtils.isPositive(pckUnits)
                  && BeeUtils.isEmpty(attribute)) {
                getUnpckSuppliers(row.getLong(Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_ITEM)),
                    supplier, newValue, BeeUtils.toString(updValue));
                return false;
              }

              break;

            case COL_TRADE_ITEM_PRICE:
              Double unpack = row.getDouble(Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_UNPACKING));
              if (unpack != null) {
                newValue = BeeUtils.unbox(newValue) + BeeUtils.unbox(unpack)
                    / BeeUtils.unbox(qty);
              }

              cols = Lists.newArrayList(cv.getColumn());
              oldValues = Lists.newArrayList(cv.getOldValue());
              newValues = Lists.newArrayList(BeeUtils.toString(newValue));
              break;

            case COL_TRADE_DISCOUNT:
              double mngDiscount = BeeUtils.unbox(Global.getParameterNumber(PRM_MANAGER_DISCOUNT));

              if (BeeUtils.isPositive(mngDiscount)) {
                double invisibleDiscount = BeeUtils.unbox(row.getDouble(
                    Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_INVISIBLE_DISCOUNT))) + mngDiscount;

                double discount = BeeUtils.unbox(newValue);

                if (discount > invisibleDiscount) {
                  getGridPresenter().getGridView().notifySevere(
                      Localized.dictionary().ordMaxDiscount() + ": " + invisibleDiscount + "%");

                  return false;
                }
              }
              cols = Lists.newArrayList(cv.getColumn());
              oldValues = Lists.newArrayList(cv.getOldValue());
              newValues = Lists.newArrayList(cv.getNewValue());
              break;
          }

          Queries.update(getViewName(), row.getId(), row.getVersion(), cols, oldValues,
              newValues, null, new RowCallback() {
                @Override
                public void onSuccess(BeeRow result) {
                  DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_ORDER_ITEMS);
                }
              });

          return null;
        }
        return true;
      });
    } else if (column instanceof CalculatedColumn) {
      if ("ItemPrices".equals(columnName)) {

        int index = DataUtils.getColumnIndex(COL_TRADE_ITEM_PRICE, dataColumns);
        CellSource cellSource = CellSource.forColumn(dataColumns.get(index), index);

        ItemPricePicker ipp = new ItemPricePicker(cellSource, dataColumns, null);
        ((HasCellRenderer) column).setRenderer(ipp);

      }
    }

    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    if (event.getCharCode() == EditStartEvent.DELETE
        && Objects.equals(event.getColumnId(), COL_TRADE_SUPPLIER)) {

      IsRow row = event.getRowValue();

      if (row == null) {
        return;
      }

      int unpackingIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_UNPACKING);
      int supplierIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_TRADE_SUPPLIER);
      int priceIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_ITEM_PRICE);
      String unpacking = row.getString(unpackingIdx);

      if (!BeeUtils.isEmpty(unpacking)) {
        event.consume();

        List<BeeColumn> cols =
            Data.getColumns(getViewName(), Lists.newArrayList(COL_UNPACKING, COL_TRADE_SUPPLIER,
                COL_ITEM_PRICE));
        List<String> newValues =
            Lists.newArrayList(null, null, BeeUtils.toString(calculatePrice((BeeRow) row, null, row
                .getDouble(unpackingIdx), null)));
        List<String> oldValues =
            Lists.newArrayList(row.getString(unpackingIdx), row.getString(supplierIdx), row
                .getString(priceIdx));

        Queries.update(getViewName(), row.getId(), row.getVersion(), cols, oldValues,
            newValues, null, new RowUpdateCallback(getViewName()));
      }
    }
  }

  @Override
  public void afterCreateEditor(String source, Editor editor, boolean embedded) {
    if (BeeUtils.same(source, COL_TRADE_SUPPLIER) && editor instanceof DataSelector) {
      ((DataSelector) editor).addSelectorHandler(event -> {

        int itemIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_ITEM);

        if (event.isOpened()) {
          Filter filter =
              Filter.in("CompanyID", VIEW_ITEM_SUPPLIERS, COL_TRADE_SUPPLIER, Filter.equals(
                  COL_ITEM, getGridView().getActiveRow().getLong(itemIdx)));
          event.getSelector().setAdditionalFilter(filter);
        }

        if (event.isChanged()) {
          BeeRow supplierRow = event.getRelatedRow();
          if (supplierRow != null) {
            Long supplierId = event.getSelector().getRelatedId();
            Long itemId = getGridView().getActiveRow().getLong(itemIdx);

            if (BeeUtils.isPositive(supplierId) && BeeUtils.isPositive(itemId)) {
              getUnpckSuppliers(itemId, supplierId, null, null);
            }
          }
        }
      });
    }
    super.afterCreateEditor(source, editor, embedded);
  }

  @Override
  public GridInterceptor getInstance() {
    return new OrderItemsGrid();
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {

    Long complInvc = activeRow.getPropertyLong(PRP_COMPLETED_INVOICES);

    if (BeeUtils.isPositive(complInvc)) {
      getGridView().notifySevere(Localized.dictionary().rowIsNotRemovable());
      return DeleteMode.CANCEL;
    } else {
      return DeleteMode.SINGLE;
    }
  }

  @Override
  public void onSelection(SelectionEvent<BeeRowSet> event) {
    addItems(event.getSelectedItem());
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    orderForm = event.getRowId();

    invoice.clear();

    if (DataUtils.isId(orderForm)) {

      int index = Data.getColumnIndex(VIEW_ORDERS, COL_ORDERS_STATUS);
      if (Objects.equals(event.getRow().getInteger(index), OrdersStatus.APPROVED.ordinal())
          || Objects.equals(event.getRow().getInteger(index), OrdersStatus.FINISH.ordinal())) {
        invoice.add(new InvoiceCreator(VIEW_ORDER_SALES, Filter.equals(COL_ORDER, orderForm)));
      }
    }

    super.onParentRow(event);
  }

  private void getUnpckSuppliers(Long itemId, Long supplierId, final Double quantity,
      String resRemainder) {
    Queries.getRowSet(VIEW_ITEM_SUPPLIERS,
        Arrays.asList(COL_ITEM, COL_TRADE_SUPPLIER, COL_DATE_FROM, COL_DATE_TO,
            COL_UNPACKING),
        Filter.and(Filter.equals(COL_ITEM, itemId), Filter.equals(COL_TRADE_SUPPLIER,
            supplierId)), new RowSetCallback() {

          @Override
          public void onSuccess(BeeRowSet result) {
            if (result.getNumberOfRows() > 0) {
              getUnpackingPrice(result, quantity, resRemainder);
            }
          }
        });
  }

  private void getUnpackingPrice(BeeRowSet rowSet, Double qty, String resRemainder) {

    int attributeIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_ITEM_ATTRIBUTE);
    int unpackIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_UNPACKING);
    int pckUnitsIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_ITEM_PACKAGE_UNITS);
    int qtyIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_TRADE_ITEM_QUANTITY);

    boolean doUnpack;
    boolean clearValue = false;
    boolean updateQty = false;
    Double unpackResult = null;
    Double quantity = qty;

    if (BeeConst.isUndef(attributeIdx) || BeeConst.isUndef(pckUnitsIdx)
        || BeeConst.isUndef(qtyIdx)) {
      return;
    }

    IsRow row = getGridView().getActiveRow();
    IsRow parentRow = ViewHelper.getFormRow(getGridPresenter().getMainView());

    if (row == null || parentRow == null) {
      return;
    }

    JustDate orderDate =
        new JustDate(parentRow.getDateTime(Data.getColumnIndex(VIEW_ORDERS,
            ProjectConstants.COL_DATES_START_DATE)));
    String attribute = row.getString(attributeIdx);

    if (BeeUtils.isEmpty(attribute)) {
      Double pckUnits = row.getDouble(pckUnitsIdx);
      if (quantity == null) {
        quantity = row.getDouble(qtyIdx);
      } else {
        updateQty = true;
      }

      if (pckUnits == null || quantity == null) {
        return;
      }

      if (BeeUtils.isMore(pckUnits, quantity) || quantity.intValue() % pckUnits.intValue() != 0) {
        doUnpack = true;
      } else {
        doUnpack = true;
        clearValue = true;
      }

      if (doUnpack) {
        if (!clearValue) {
          for (BeeRow supplierRow : rowSet) {
            Double unpack = supplierRow.getDouble(rowSet.getColumnIndex(COL_UNPACKING));

            if (unpack == null) {
              continue;
            }

            JustDate from = supplierRow.getDate(rowSet.getColumnIndex(COL_DATE_FROM));
            JustDate to = supplierRow.getDate(rowSet.getColumnIndex(COL_DATE_TO));

            if (to == null) {
              to = TimeUtils.nowMillis().getDate();
            }

            if (orderDate.compareTo(to) == -1 || orderDate.compareTo(to) == 0) {
              if (from == null) {
                unpackResult = unpack;
                break;
              } else if (from.compareTo(orderDate) == -1 || from.compareTo(orderDate) == 0) {
                unpackResult = unpack;
              }
            }
          }
        }

        List<String> columns = new ArrayList<>();
        columns.add(COL_UNPACKING);
        columns.add(COL_ITEM_PRICE);
        List<String> values = new ArrayList<>();
        values.add(unpackResult == null ? BeeConst.STRING_EMPTY : unpackResult.toString());

        values.add(BeeUtils.toString(calculatePrice((BeeRow) row, unpackResult, row
            .getDouble(unpackIdx), quantity)));

        if (updateQty) {
          columns.add(COL_TRADE_ITEM_QUANTITY);
          values.add(quantity.toString());
        }
        if (!BeeUtils.isEmpty(resRemainder)) {
          columns.add(COL_RESERVED_REMAINDER);
          values.add(resRemainder);
        }

        Queries.update(VIEW_ORDER_ITEMS, Filter.equals("ID", row.getId()), columns, values,
            new IntCallback() {

              @Override
              public void onSuccess(Integer result) {
                getGridPresenter().handleAction(Action.REFRESH);
              }
            });
      }
    }
  }

  private OrderItemsPicker ensurePicker() {
    if (picker == null) {
      picker = new OrderItemsPicker();
      picker.addSelectionHandler(this);
    }
    return picker;
  }

  private void addItems(final BeeRowSet rowSet) {
    if (!DataUtils.isEmpty(rowSet) && VIEW_ITEMS.equals(rowSet.getViewName())) {
      getGridView().ensureRelId(result -> {
        FormView form = ViewHelper.getForm(getGridView());
        IsRow parentRow = (form == null) ? null : form.getActiveRow();

        if (DataUtils.idEquals(parentRow, result)) {
          addItems(parentRow, form.getDataColumns(), rowSet);
        }
      });
    }
  }

  private void addItems(IsRow parentRow, List<BeeColumn> parentColumns, BeeRowSet items) {

    List<String> colNames =
        Lists.newArrayList(COL_ORDER, COL_TA_ITEM,
            COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE, COL_TRADE_DISCOUNT,
            COL_INVISIBLE_DISCOUNT, COL_RESERVED_REMAINDER, COL_TRADE_VAT, COL_TRADE_VAT_PERC,
            COL_TRADE_SUPPLIER, COL_UNPACKING);

    final BeeRowSet rowSet = new BeeRowSet(getViewName(), Data.getColumns(getViewName(), colNames));

    final int ordIndex = rowSet.getColumnIndex(COL_ORDER);
    final int itemIndex = rowSet.getColumnIndex(COL_TA_ITEM);
    final int qtyIndex = rowSet.getColumnIndex(COL_TRADE_ITEM_QUANTITY);
    final int resRemIndex = rowSet.getColumnIndex(COL_RESERVED_REMAINDER);
    final int priceIndex = rowSet.getColumnIndex(COL_TRADE_ITEM_PRICE);
    final int discountIndex = rowSet.getColumnIndex(COL_TRADE_DISCOUNT);
    final int invisibleDiscountIndex = rowSet.getColumnIndex(COL_INVISIBLE_DISCOUNT);
    final int vatIdx = rowSet.getColumnIndex(COL_TRADE_VAT);
    final int supplierIdx = rowSet.getColumnIndex(COL_TRADE_SUPPLIER);
    final int unpackingIdx = rowSet.getColumnIndex(COL_UNPACKING);
    final int vatPrcIndex = rowSet.getColumnIndex(COL_TRADE_VAT_PERC);
    final int vatPrcItemIdx = items.getColumnIndex(COL_TRADE_VAT_PERC);
    final int vatPrcDefaultIdx = items.getNumberOfColumns() - 5;
    final int vatItemIdx = items.getColumnIndex(COL_TRADE_VAT);
    final int attributeIdx = items.getColumnIndex(COL_ITEM_ATTRIBUTE);
    final int pckgUnitsIdx = items.getColumnIndex(COL_ITEM_PACKAGE_UNITS);

    Map<Long, Double> quantities = new HashMap<>();
    Map<Long, ItemPrice> priceNames = new HashMap<>();

    for (BeeRow item : items) {
      Double qty = BeeUtils.toDouble(item.getProperty(PRP_QUANTITY));
      Double freeRem = BeeUtils.toDouble(item.getProperty(PRP_FREE_REMAINDER));

      if (BeeUtils.isDouble(qty)) {
        final BeeRow row = DataUtils.createEmptyRow(rowSet.getNumberOfColumns());

        row.setValue(ordIndex, parentRow.getId());
        row.setValue(itemIndex, item.getId());

        if (BeeUtils.isDouble(freeRem)) {
          if (qty > freeRem) {
            row.setValue(resRemIndex, freeRem);
          } else {
            row.setValue(resRemIndex, qty);
          }
        }
        row.setValue(qtyIndex, qty);

        quantities.put(item.getId(), qty);

        if (BeeUtils.isPositive(item.getLong(items.getNumberOfColumns() - 8))) {
          row.setValue(supplierIdx, item.getLong(items.getNumberOfColumns() - 8));
          if (BeeUtils.isPositive(item.getDouble(items.getNumberOfColumns() - 7))) {
            if (maybeInsertSupplier(item, attributeIdx, pckgUnitsIdx, qty, item.getDate(items
                .getNumberOfColumns() - 6))) {
              row.setValue(unpackingIdx, item.getDouble(items.getNumberOfColumns() - 7));
            }
          }
        }

        if (BeeUtils.unbox(item.getBoolean(vatItemIdx))) {

          if (BeeUtils.unbox(item.getInteger(vatPrcItemIdx)) > 0) {
            row.setValue(vatIdx, item.getInteger(vatPrcItemIdx));
          } else {
            row.setValue(vatIdx, item.getInteger(vatPrcDefaultIdx));
          }
          row.setValue(vatPrcIndex, true);
        }
        rowSet.addRow(row);
      }
    }

    Long company = DataUtils.getLong(parentColumns, parentRow, COL_COMPANY);

    if (!rowSet.isEmpty() && DataUtils.isId(company)) {
      Map<String, Long> options = new HashMap<>();

      options.put(COL_DISCOUNT_COMPANY, company);

      Long warehouse = DataUtils.getLong(parentColumns, parentRow, COL_WAREHOUSE);
      if (DataUtils.isId(warehouse)) {
        options.put(COL_DISCOUNT_WAREHOUSE, warehouse);
      }

      DateTime startDate = DataUtils.getDateTime(parentColumns, parentRow, "StartDate");
      if (startDate != null) {
        options.put(Service.VAR_TIME, startDate.getTime());
      }

      ClassifierKeeper.getPricesAndDiscounts(options, quantities.keySet(), quantities, priceNames,
          input -> {
            for (BeeRow row : rowSet) {
              Pair<Double, Double> pair = input.get(row.getLong(itemIndex));

              if (pair != null) {
                double price =
                    BeeUtils.unbox(pair.getA())
                        + BeeUtils.unbox(row.getDouble(unpackingIdx))
                        / BeeUtils.unbox(row.getDouble(qtyIndex));
                Double percent = pair.getB();
                if (BeeUtils.isPositive(price)) {
                  row.setValue(priceIndex,
                      Data.round(getViewName(), COL_TRADE_ITEM_PRICE, price));
                }

                if (BeeUtils.isDouble(percent)) {
                  if (BeeUtils.nonZero(percent)) {
                    row.setValue(discountIndex, percent);
                    row.setValue(invisibleDiscountIndex, percent);
                  } else {
                    row.clearCell(discountIndex);
                    row.setValue(invisibleDiscountIndex, 0);
                  }
                }
              }
            }

            Queries.insertRows(rowSet);
          });

    } else if (!rowSet.isEmpty()) {
      Queries.insertRows(rowSet);
    }
  }

  private void recalculatePrices() {

    GridView gridView = getGridView();
    if (gridView == null || gridView.isEmpty()) {
      return;
    }

    Map<Long, Double> quantities = new HashMap<>();
    Map<String, Long> options = new HashMap<>();
    Map<Long, ItemPrice> test = new HashMap<>();
    GridView parentGrid = getGridView();
    FormView parentForm;

    final int qtyIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_TRADE_ITEM_QUANTITY);
    final int itemIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_ITEM);
    final int priceIndex = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_TRADE_ITEM_PRICE);
    final int discountIndex = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_TRADE_DISCOUNT);
    final int unpackingIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_UNPACKING);

    if (parentGrid == null) {
      return;
    }

    parentForm = ViewHelper.getForm(parentGrid.asWidget());

    Long company = parentForm.getLongValue(COL_COMPANY);
    if (DataUtils.isId(company)) {
      options.put(COL_DISCOUNT_COMPANY, parentForm.getLongValue(COL_COMPANY));
    }

    Long warehouse = parentForm.getLongValue(COL_WAREHOUSE);
    if (DataUtils.isId(warehouse)) {
      options.put(COL_DISCOUNT_WAREHOUSE, warehouse);
    }

    DateTime startDate = parentForm.getDateTimeValue("StartDate");
    if (startDate != null) {
      options.put(Service.VAR_TIME, startDate.getTime());
    }

    for (IsRow row : getGridView().getRowData()) {
      quantities.put(row.getLong(itemIdx), row.getDouble(qtyIdx));
    }

    ClassifierKeeper.getPricesAndDiscounts(options, quantities.keySet(), quantities,
        test, input -> {

          for (IsRow row : getGridView().getRowData()) {
            Pair<Double, Double> pair = input.get(row.getLong(itemIdx));

            if (pair != null) {
              List<BeeColumn> cols =
                  Data.getColumns(getViewName(), Lists.newArrayList(COL_TRADE_ITEM_PRICE,
                      COL_TRADE_DISCOUNT, COL_INVISIBLE_DISCOUNT));

              double price =
                  BeeUtils.unbox(pair.getA()) + BeeUtils.unbox(row.getDouble(unpackingIdx))
                      / BeeUtils.unbox(row.getDouble(qtyIdx));
              Double percent = pair.getB();
              Double oldPrice = row.getDouble(priceIndex);
              Double discount = row.getDouble(discountIndex);

              List<String> oldValues =
                  Lists.newArrayList(oldPrice == null ? null : oldPrice.toString(),
                      discount == null ? null : discount.toString(), discount == null ? "0"
                          : discount.toString());

              List<String> newValues =
                  Lists.newArrayList(String.valueOf(price), percent == null ? null : percent
                      .toString(), percent == null ? "0" : percent.toString());

              Queries.update(getViewName(), row.getId(), row.getVersion(), cols, oldValues,
                  newValues, null, new RowUpdateCallback(getViewName()));
            }
          }
        });
  }

  private static double calculatePrice(BeeRow row, Double newUnpacking, Double oldUnpacking,
      Double newQty) {
    double newPrice = BeeConst.DOUBLE_ZERO;

    if (row == null) {
      return newPrice;
    }

    double price =
        BeeUtils.unbox(row.getDouble(Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_ITEM_PRICE)));
    double qty = BeeUtils.unbox(row
        .getDouble(Data.getColumnIndex(VIEW_ORDER_ITEMS, COL_TRADE_ITEM_QUANTITY)));

    if (oldUnpacking != null) {
      if (newUnpacking == null) {
        newPrice = price - BeeUtils.unbox(oldUnpacking) / BeeUtils.unbox(qty);
      } else {
        newPrice = price - BeeUtils.unbox(oldUnpacking) / BeeUtils.unbox(qty)
            + BeeUtils.unbox(newUnpacking) / BeeUtils.unbox(newQty);
      }
    } else {
      if (newUnpacking != null) {
        newPrice = price - BeeUtils.unbox(oldUnpacking) / BeeUtils.unbox(qty)
            + BeeUtils.unbox(newUnpacking) / BeeUtils.unbox(newQty);
      } else {
        newPrice = price;
      }
    }

    return newPrice;
  }

  private boolean checkIsWarehouseEditable() {

    if (!getGridView().isEmpty()) {
      for (IsRow row : getGridView().getRowData()) {
        if (BeeUtils.isPositive(Double.valueOf(row.getProperty(PRP_COMPLETED_INVOICES)))) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean maybeInsertSupplier(BeeRow item, int attributeIdx, int pckgUnitsIdx,
      Double qty, JustDate dateTo) {
    String attribute = item.getString(attributeIdx);
    Double pckgUnits = item.getDouble(pckgUnitsIdx);

    if (BeeUtils.isEmpty(attribute) && BeeUtils.isPositive(pckgUnits) && BeeUtils.isPositive(qty)) {
      if (BeeUtils.isMore(pckgUnits, qty) || qty.intValue() % pckgUnits.intValue() != 0) {
        if (dateTo == null) {
          return true;
        } else {
          IsRow parentRow = ViewHelper.getFormRow(getGridPresenter().getMainView());
          JustDate orderDate =
              new JustDate(parentRow.getDateTime(Data.getColumnIndex(VIEW_ORDERS,
                  ProjectConstants.COL_DATES_START_DATE)));

          if (orderDate.compareTo(dateTo) == -1 || orderDate.compareTo(dateTo) == 0) {
            return true;
          }
        }
      }
    }
    return false;
  }
}