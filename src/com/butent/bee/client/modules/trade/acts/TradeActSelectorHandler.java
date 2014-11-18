package com.butent.bee.client.modules.trade.acts;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class TradeActSelectorHandler implements SelectorEvent.Handler {

  private static final BeeLogger logger = LogUtils.getLogger(TradeActSelectorHandler.class);

  private static void addNotes(String notes, FormView form) {
    IsRow row = form.getActiveRow();
    int index = form.getDataIndex(COL_TA_NOTES);

    if (row != null && !BeeConst.isUndef(index)) {
      String oldValue = row.getString(index);

      if (!BeeUtils.containsSame(oldValue, notes)) {
        row.setValue(index, BeeUtils.buildLines(oldValue, notes));
        form.refreshBySource(COL_TA_NOTES);
      }
    }
  }

  private static void addTemplateChildren(String viewName, IsRow templRow, IsRow actRow,
      BeeRowSet templChildren, BeeRowSet items) {

    ItemPrice itemPrice = TradeActKeeper.getItemPrice(VIEW_TRADE_ACTS, actRow);

    DateTime date = Data.getDateTime(VIEW_TRADE_ACTS, actRow, COL_TA_DATE);

    Long templCurrency = Data.getLong(VIEW_TRADE_ACT_TEMPLATES, templRow, COL_TA_CURRENCY);
    Long actCurrency = Data.getLong(VIEW_TRADE_ACTS, actRow, COL_TA_CURRENCY);

    List<BeeColumn> columns = new ArrayList<>();
    Map<Integer, Integer> indexes = new HashMap<>();

    for (BeeColumn column : Data.getColumns(viewName)) {
      if (COL_TRADE_ACT.equals(column.getId())) {
        columns.add(column);

      } else if (column.isEditable()) {
        int templIndex = templChildren.getColumnIndex(column.getId());

        if (!BeeConst.isUndef(templIndex)) {
          indexes.put(templIndex, columns.size());
          columns.add(column);
        }
      }
    }

    BeeRowSet actItems = new BeeRowSet(viewName, columns);

    int actIndex = actItems.getColumnIndex(COL_TRADE_ACT);
    int itemIndex = actItems.getColumnIndex(COL_TA_ITEM);

    int qtyIndex = actItems.getColumnIndex(COL_TRADE_ITEM_QUANTITY);
    boolean qtyNullable = BeeConst.isUndef(qtyIndex)
        ? true : actItems.getColumn(qtyIndex).isNullable();

    int priceIndex = actItems.getColumnIndex(COL_TRADE_ITEM_PRICE);

    for (BeeRow templItem : templChildren) {
      BeeRow actItem = DataUtils.createEmptyRow(actItems.getNumberOfColumns());
      actItem.setValue(actIndex, actRow.getId());

      for (Map.Entry<Integer, Integer> indexEntry : indexes.entrySet()) {
        if (!templItem.isNull(indexEntry.getKey())) {
          actItem.setValue(indexEntry.getValue(), templItem.getString(indexEntry.getKey()));
        }
      }

      if (!qtyNullable && actItem.isNull(qtyIndex)) {
        actItem.setValue(qtyIndex, 0);
      }

      if (!BeeConst.isUndef(priceIndex)) {
        Double price = actItem.getDouble(priceIndex);

        if (BeeUtils.nonZero(price)) {
          if (DataUtils.isId(templCurrency) && DataUtils.isId(actCurrency)
              && !templCurrency.equals(actCurrency)) {
            actItem.setValue(priceIndex, Money.exchange(templCurrency, actCurrency, price, date));
          }

        } else if (!BeeUtils.isDouble(price) && itemPrice != null && items != null) {
          BeeRow item = items.getRowById(actItem.getLong(itemIndex));

          if (item != null) {
            price = item.getDouble(items.getColumnIndex(itemPrice.getPriceColumn()));

            if (BeeUtils.isDouble(price)) {
              if (DataUtils.isId(actCurrency)) {
                Long ic = item.getLong(items.getColumnIndex(itemPrice.getCurrencyColumn()));
                if (DataUtils.isId(ic) && !actCurrency.equals(ic)) {
                  price = Money.exchange(ic, actCurrency, price, date);
                }
              }

              actItem.setValue(priceIndex, Data.round(viewName, COL_TRADE_ITEM_PRICE, price));
            }
          }
        }
      }

      actItems.addRow(actItem);
    }

    if (!actItems.isEmpty()) {
      Queries.insertRows(actItems);
    }
  }

  private static void applyActTemplate(final IsRow templRow, final FormView form) {
    IsRow targetRow = form.getActiveRow();
    List<BeeColumn> templColumns = Data.getColumns(VIEW_TRADE_ACT_TEMPLATES);

    if (targetRow != null && !BeeUtils.isEmpty(templColumns)) {
      Set<String> updatedColumns = new HashSet<>();

      BeeRow actRow = DataUtils.cloneRow(targetRow);
      TradeActKind actKind = TradeActKeeper.getKind(VIEW_TRADE_ACTS, actRow);

      for (int i = 0; i < templColumns.size(); i++) {
        String colName = templColumns.get(i).getId();
        String newValue = templRow.getString(i);

        int targetIndex = form.getDataIndex(colName);
        boolean upd;

        if (BeeConst.isUndef(targetIndex)) {
          upd = false;

        } else if (COL_TA_TEMPLATE_NAME.equals(colName)) {
          upd = false;

        } else if (COL_TA_NAME.equals(colName) || COL_TRADE_ACT_TEMPLATE.equals(colName)) {
          upd = isTemplatable(actRow, templRow, COL_TA_NAME);

        } else if (COL_TA_UNTIL.equals(colName)) {
          upd = actKind != null && actKind.enableServices()
              && !templRow.isNull(i) && targetRow.isNull(targetIndex)
              && TimeUtils.monthDiff(TimeUtils.today(), templRow.getDateTime(i)) > 0;

        } else if (colName.contains(COL_TA_SERIES)) {
          upd = isTemplatable(actRow, templRow, COL_TA_SERIES)
              && TradeActKeeper.isUserSeries(getTemplateLong(templRow, COL_TA_SERIES));

        } else if (colName.contains(COL_TA_OPERATION)) {
          upd = isTemplatable(actRow, templRow, COL_TA_OPERATION);
          if (upd) {
            TradeActKind templKind =
                TradeActKeeper.getOperationKind(getTemplateLong(templRow, COL_TA_OPERATION));
            upd = actKind != null && actKind == templKind;
          }

        } else if (colName.contains(COL_TA_STATUS)) {
          upd = isTemplatable(actRow, templRow, COL_TA_STATUS);

        } else if (colName.contains(COL_TA_COMPANY)) {
          upd = isTemplatable(actRow, templRow, COL_TA_COMPANY);

        } else if (colName.contains(COL_TA_CONTACT)) {
          upd = isTemplatable(actRow, templRow, COL_TA_CONTACT);

        } else if (colName.contains(COL_TA_OBJECT)) {
          upd = isTemplatable(actRow, templRow, COL_TA_OBJECT);

        } else if (colName.contains(COL_TA_MANAGER)) {
          upd = isTemplatable(actRow, templRow, COL_TA_MANAGER);

        } else if (colName.contains(COL_TA_CURRENCY)) {
          upd = isTemplatable(actRow, templRow, COL_TA_CURRENCY);

        } else if (colName.contains(COL_TA_VEHICLE)) {
          upd = isTemplatable(actRow, templRow, COL_TA_VEHICLE);

        } else if (colName.contains(COL_TA_DRIVER)) {
          upd = isTemplatable(actRow, templRow, COL_TA_DRIVER);

        } else {
          upd = !BeeUtils.isEmpty(newValue) && targetRow.isNull(targetIndex);
        }

        if (upd) {
          targetRow.setValue(targetIndex, newValue);
          if (templColumns.get(i).isEditable()) {
            updatedColumns.add(colName);
          }
        }
      }

      if (!updatedColumns.isEmpty()) {
        for (String colName : updatedColumns) {
          form.refreshBySource(colName);
        }
        logger.debug(updatedColumns);
      }

      ParameterList params = TradeActKeeper.createArgs(SVC_GET_TEMPLATE_ITEMS_AND_SERVICES);

      params.addQueryItem(COL_TRADE_ACT_TEMPLATE, templRow.getId());
      if (DataUtils.hasId(targetRow)) {
        params.addQueryItem(COL_TRADE_ACT, actRow.getId());
      }
      if (actKind != null) {
        params.addQueryItem(COL_TA_KIND, actKind.ordinal());
      }

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          final Map<String, BeeRowSet> data = new HashMap<>();

          if (response.getSize() > 0) {
            String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

            if (arr != null) {
              for (String s : arr) {
                BeeRowSet rowSet = BeeRowSet.restore(s);

                if (!DataUtils.isEmpty(rowSet)) {
                  data.put(rowSet.getViewName(), rowSet);
                  logger.debug(rowSet.getViewName(), rowSet.getNumberOfRows());
                }
              }
            }
          }

          if (!data.isEmpty()) {
            GridView gridView = ViewHelper.getChildGrid(form, GRID_TRADE_ACT_ITEMS);

            if (gridView != null) {
              gridView.ensureRelId(new IdCallback() {
                @Override
                public void onSuccess(Long result) {
                  if (DataUtils.isId(result) && DataUtils.idEquals(form.getActiveRow(), result)) {
                    if (data.containsKey(VIEW_TRADE_ACT_TMPL_ITEMS)) {
                      addTemplateChildren(VIEW_TRADE_ACT_ITEMS, templRow, form.getActiveRow(),
                          data.get(VIEW_TRADE_ACT_TMPL_ITEMS), data.get(VIEW_ITEMS));
                    }

                    if (data.containsKey(VIEW_TRADE_ACT_TMPL_SERVICES)) {
                      addTemplateChildren(VIEW_TRADE_ACT_SERVICES, templRow, form.getActiveRow(),
                          data.get(VIEW_TRADE_ACT_TMPL_SERVICES), data.get(VIEW_ITEMS));
                    }
                  }
                }
              });
            }
          }
        }
      });
    }
  }

  private static TradeActKind getKind(SelectorEvent event) {
    DataView dataView = ViewHelper.getDataView(event.getSelector());

    if (dataView != null && VIEW_TRADE_ACTS.equals(dataView.getViewName())
        && dataView.getActiveRow() != null) {

      return EnumUtils.getEnumByIndex(TradeActKind.class,
          dataView.getActiveRow().getInteger(dataView.getDataIndex(COL_TA_KIND)));
    } else {
      return null;
    }
  }

  private static Long getSeries(SelectorEvent event) {
    DataView dataView = ViewHelper.getDataView(event.getSelector());

    if (dataView != null && VIEW_TRADE_ACTS.equals(dataView.getViewName())
        && dataView.getActiveRow() != null) {

      return dataView.getActiveRow().getLong(dataView.getDataIndex(COL_TA_SERIES));
    } else {
      return null;
    }
  }

  private static Long getTemplateLong(IsRow row, String colName) {
    int index = Data.getColumnIndex(VIEW_TRADE_ACT_TEMPLATES, colName);
    return BeeConst.isUndef(index) ? null : row.getLong(index);
  }

  private static boolean isActOrTemplate(FormView form) {
    return form != null
        && BeeUtils.inListSame(form.getViewName(), VIEW_TRADE_ACTS, VIEW_TRADE_ACT_TEMPLATES);
  }

  private static boolean isTemplatable(IsRow actRow, IsRow templRow, String colName) {
    int actIndex = Data.getColumnIndex(VIEW_TRADE_ACTS, colName);
    int templIndex = Data.getColumnIndex(VIEW_TRADE_ACT_TEMPLATES, colName);

    return !BeeConst.isUndef(actIndex) && !BeeConst.isUndef(templIndex)
        && actRow.isNull(actIndex) && !templRow.isNull(templIndex);
  }

  private static void maybeExchange(final FormView actForm, final long fromId, String fromName,
      final long toId, String toName) {

    final List<IsRow> actItems = new ArrayList<>();
    final List<IsRow> actServices = new ArrayList<>();

    final GridView itemGrid = ViewHelper.getChildGrid(actForm, GRID_TRADE_ACT_ITEMS);

    if (itemGrid != null && !itemGrid.isEmpty()) {
      int index = itemGrid.getDataIndex(COL_TRADE_ITEM_PRICE);

      for (IsRow row : itemGrid.getRowData()) {
        if (BeeUtils.isPositive(row.getDouble(index))) {
          actItems.add(row);
        }
      }
    }

    TradeActKind kind = TradeActKeeper.getKind(actForm.getViewName(), actForm.getActiveRow());
    if (kind != null && kind.enableServices()) {
      GridView serviceGrid = ViewHelper.getChildGrid(actForm, GRID_TRADE_ACT_SERVICES);

      if (serviceGrid != null && !serviceGrid.isEmpty()) {
        int index = serviceGrid.getDataIndex(COL_TRADE_ITEM_PRICE);

        for (IsRow row : serviceGrid.getRowData()) {
          if (BeeUtils.isPositive(row.getDouble(index))) {
            actServices.add(row);
          }
        }
      }
    }

    if (!actItems.isEmpty() || !actServices.isEmpty()) {
      Global.confirm(null, Icon.QUESTION,
          Collections.singletonList(Localized.getMessages().exchangeFromTo(fromName, toName)),
          Localized.getConstants().actionExchange(), Localized.getConstants().actionCancel(),
          new ConfirmationCallback() {
            @Override
            public void onCancel() {
              if (!actItems.isEmpty()) {
                itemGrid.refresh(false, false);
              }
            }

            @Override
            public void onConfirm() {
              DateTime date = actForm.getDateTimeValue(COL_TA_DATE);

              int updated = 0;

              if (!actItems.isEmpty()) {
                updated += Money.exchange(fromId, toId, date,
                    VIEW_TRADE_ACT_ITEMS, actItems, COL_TRADE_ITEM_PRICE);
              }
              if (!actServices.isEmpty()) {
                updated += Money.exchange(fromId, toId, date,
                    VIEW_TRADE_ACT_SERVICES, actServices, COL_TRADE_ITEM_PRICE);
              }

              if (updated > 0) {
                actForm.flush();
              }
            }
          });
    }
  }

  private static void update(DataView dataView, IsRow row, String colName,
      Integer oldValue, Integer newValue) {

    if (dataView.isFlushable() || !DataUtils.hasId(row)) {
      Data.setValue(dataView.getViewName(), row, colName, newValue);
      dataView.refreshBySource(colName);

    } else {
      Queries.updateCellAndFire(dataView.getViewName(), row.getId(), row.getVersion(), colName,
          (oldValue == null) ? null : oldValue.toString(),
          (newValue == null) ? null : newValue.toString());
    }
  }

  TradeActSelectorHandler() {
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    String relatedViewName = event.getRelatedViewName();
    if (BeeUtils.isEmpty(relatedViewName)) {
      return;
    }

    switch (relatedViewName) {
      case VIEW_TRADE_OPERATIONS:
        if (event.isOpened()) {
          TradeActKind kind = getKind(event);

          if (kind != null) {
            Filter filter;
            if (kind == TradeActKind.SUPPLEMENT) {
              filter = Filter.or(Filter.equals(COL_TA_KIND, TradeActKind.SALE),
                  Filter.equals(COL_TA_KIND, kind));
            } else {
              filter = Filter.equals(COL_TA_KIND, kind);
            }

            event.getSelector().setAdditionalFilter(filter);
          }
        }
        break;

      case VIEW_TRADE_SERIES:
        if (event.isOpened()) {
          DataView dataView = ViewHelper.getDataView(event.getSelector());

          if (dataView != null && BeeUtils.inList(dataView.getViewName(),
              VIEW_TRADE_ACTS, VIEW_TRADE_ACT_TEMPLATES)) {

            BeeRowSet series = TradeActKeeper.getUserSeries(false);
            Filter filter;

            if (DataUtils.isEmpty(series)) {
              filter = null;
            } else {
              filter = Filter.idIn(series.getRowIds());
            }

            event.getSelector().setAdditionalFilter(filter);
          }
        }
        break;

      case VIEW_TRADE_NOTES:
        if (event.isOpened()) {
          Long series = getSeries(event);

          Filter filter;
          if (DataUtils.isId(series)) {
            filter = Filter.or(Filter.equals(COL_SERIES, series), Filter.isNull(COL_SERIES));
          } else {
            filter = null;
          }

          event.getSelector().setAdditionalFilter(filter);

        } else if (event.isChanged()) {
          IsRow relatedRow = event.getRelatedRow();
          String notes = (relatedRow == null)
              ? null : Data.getString(relatedViewName, relatedRow, COL_TRADE_NOTES);

          FormView form = ViewHelper.getForm(event.getSelector());

          if (!BeeUtils.isEmpty(notes) && form != null
              && VIEW_TRADE_ACTS.equals(form.getViewName())) {
            addNotes(notes, form);
          }
        }
        break;

      case VIEW_TRADE_ACT_TEMPLATES:
        if (event.isOpened()) {
          CompoundFilter filter = Filter.and();

          TradeActKind kind = getKind(event);

          Collection<Long> operations = TradeActKeeper.filterOperations(kind);
          if (operations.isEmpty() && kind == TradeActKind.SUPPLEMENT) {
            operations.addAll(TradeActKeeper.filterOperations(TradeActKind.SALE));
          }

          if (operations.isEmpty()) {
            filter.add(Filter.isNull(COL_TA_OPERATION));
          } else {
            filter.add(Filter.or(Filter.any(COL_TA_OPERATION, operations),
                Filter.isNull(COL_TA_OPERATION)));
          }

          BeeRowSet series = TradeActKeeper.getUserSeries(false);
          if (!DataUtils.isEmpty(series)) {
            filter.add(Filter.or(Filter.any(COL_TA_SERIES, series.getRowIds()),
                Filter.isNull(COL_TA_SERIES)));
          }

          if (filter.isEmpty()) {
            event.getSelector().setAdditionalFilter(null);
          } else {
            event.getSelector().setAdditionalFilter(filter);
          }

        } else if (event.isChanged()) {
          IsRow relatedRow = event.getRelatedRow();
          FormView form = ViewHelper.getForm(event.getSelector());

          if (relatedRow != null && form != null) {
            applyActTemplate(relatedRow, form);
          }
        }
        break;

      case AdministrationConstants.VIEW_CURRENCIES:
        if (event.isChanged() && event.getRelatedRow() != null) {
          FormView form = ViewHelper.getForm(event.getSelector());

          if (form != null && VIEW_TRADE_ACTS.equals(form.getViewName())
              && DataUtils.hasId(form.getActiveRow())) {

            int index = form.getDataIndex(COL_TA_CURRENCY);
            Long oldValue = (form.getOldRow() == null) ? null : form.getOldRow().getLong(index);

            if (DataUtils.isId(oldValue) && !DataUtils.idEquals(event.getRelatedRow(), oldValue)) {
              int nameIndex =
                  Data.getColumnIndex(relatedViewName, AdministrationConstants.COL_CURRENCY_NAME);

              String oldName =
                  DataUtils.getStringQuietly(
                      event.getSelector().getOracle().getCachedRow(oldValue), nameIndex);
              String newName = event.getRelatedRow().getString(nameIndex);

              maybeExchange(form, oldValue, oldName, event.getRelatedRow().getId(), newName);
            }
          }
        }
        break;

      case VIEW_COMPANY_OBJECTS:
        if (event.isOpened()) {
          FormView form = ViewHelper.getForm(event.getSelector());

          if (isActOrTemplate(form)) {
            Long company = form.getLongValue(COL_TA_COMPANY);
            Filter filter = DataUtils.isId(company) ? Filter.equals(COL_COMPANY, company) : null;

            event.getSelector().setAdditionalFilter(filter);
          }

        } else if (event.isChanged() && event.getRelatedRow() != null) {
          FormView form = ViewHelper.getForm(event.getSelector());

          if (isActOrTemplate(form) && form.getActiveRow() != null
              && !DataUtils.isId(form.getLongValue(COL_TA_COMPANY))) {

            Long company = Data.getLong(relatedViewName, event.getRelatedRow(), COL_COMPANY);
            form.getActiveRow().setValue(form.getDataIndex(COL_TA_COMPANY), company);

            String name = Data.getString(relatedViewName, event.getRelatedRow(), ALS_COMPANY_NAME);
            form.getActiveRow().setValue(form.getDataIndex(ALS_COMPANY_NAME), name);

            form.refreshBySource(COL_TA_COMPANY);
          }
        }
        break;

      case VIEW_ITEMS:
        if (event.isChanged() && event.getRelatedRow() != null) {
          DataView dataView = ViewHelper.getDataView(event.getSelector());

          String viewName = (dataView == null) ? null : dataView.getViewName();
          IsRow dst = (dataView == null) ? null : dataView.getActiveRow();

          if (dataView.getActiveRow() != null
              && (VIEW_TRADE_ACT_SERVICES.equals(viewName)
              || VIEW_TRADE_ACT_TMPL_SERVICES.equals(viewName))) {

            Integer oldDpw = Data.getInteger(viewName, dst, COL_TA_SERVICE_DAYS);
            Integer newDpw = Data.getInteger(relatedViewName, event.getRelatedRow(), COL_ITEM_DPW);

            if (!Objects.equals(oldDpw, newDpw)) {
              update(dataView, dst, COL_TA_SERVICE_DAYS, oldDpw, newDpw);
            }

            Integer oldMin = Data.getInteger(viewName, dst, COL_TA_SERVICE_MIN);
            Integer newMin = Data.getInteger(relatedViewName, event.getRelatedRow(),
                COL_ITEM_MIN_TERM);

            if (!Objects.equals(oldMin, newMin)) {
              update(dataView, dst, COL_TA_SERVICE_MIN, oldMin, newMin);
            }
          }
        }
        break;
    }
  }
}
