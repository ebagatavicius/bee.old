package com.butent.bee.client.modules.trade.acts;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.modules.trade.acts.TradeActItemImporter.ImportEntry;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import elemental.html.File;

public class TradeActItemsGrid extends AbstractGridInterceptor implements
    SelectionHandler<BeeRowSet> {

  private static final String STYLE_COMMAND_IMPORT = TradeActKeeper.STYLE_PREFIX
      + "command-import-items";

  private TradeActItemPicker picker;
  private FileCollector collector;

  TradeActItemsGrid() {
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    Button command = new Button(Localized.getConstants().actionImport());
    command.addStyleName(STYLE_COMMAND_IMPORT);

    command.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        ensureCollector().clickInput();
      }
    });

    presenter.getHeader().addCommandItem(command);

    super.afterCreatePresenter(presenter);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    IsRow parentRow = ViewHelper.getFormRow(presenter.getMainView());

    if (parentRow != null) {
      final TradeActKind kind = TradeActKeeper.getKind(VIEW_TRADE_ACTS, parentRow);
      Long parent = Data.getLong(VIEW_TRADE_ACTS, parentRow, COL_TA_PARENT);

      if (kind == TradeActKind.RETURN && DataUtils.isId(parent)) {
        ParameterList params = TradeActKeeper.createArgs(SVC_GET_ITEMS_FOR_RETURN);
        params.addQueryItem(COL_TRADE_ACT, parent);

        BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (response.hasResponse(BeeRowSet.class)) {
              BeeRowSet parentItems = BeeRowSet.restore(response.getResponseAsString());

              String pa = parentItems.getTableProperty(PRP_PARENT_ACT);
              final BeeRow parentAct = BeeUtils.isEmpty(pa) ? null : BeeRow.restore(pa);

              TradeActItemReturn.show(kind.getCaption(), parentAct, parentItems,
                  new Consumer<BeeRowSet>() {
                    @Override
                    public void accept(BeeRowSet actItems) {
                      addActItems(parentAct, actItems);
                    }
                  });

            } else {
              getGridView().notifyWarning(Localized.getConstants().noData());
            }
          }
        });

      } else {
        ensurePicker().show(parentRow, presenter.getMainView().getElement());
      }
    }

    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeActItemsGrid();
  }

  @Override
  public void onSelection(SelectionEvent<BeeRowSet> event) {
    addItems(event.getSelectedItem());
  }

  private void addActItems(final BeeRow act, final BeeRowSet actItems) {
    if (!DataUtils.isEmpty(actItems) && getViewName().equals(actItems.getViewName())) {
      getGridView().ensureRelId(new IdCallback() {
        @Override
        public void onSuccess(Long result) {
          IsRow parentRow = ViewHelper.getFormRow(getGridView());

          if (DataUtils.idEquals(parentRow, result)) {
            Long sourceCurrency =
                (act == null) ? null : Data.getLong(VIEW_TRADE_ACTS, act, COL_TA_CURRENCY);

            DateTime date = Data.getDateTime(VIEW_TRADE_ACTS, parentRow, COL_TA_DATE);
            Long targetCurrency = Data.getLong(VIEW_TRADE_ACTS, parentRow, COL_TA_CURRENCY);

            addActItems(result, date, sourceCurrency, targetCurrency, actItems);
          }
        }
      });
    }
  }

  private void addActItems(long targetId, DateTime date, Long sourceCurrency,
      Long targetCurrency, BeeRowSet sourceItems) {

    List<BeeColumn> columns = new ArrayList<>();
    Map<Integer, Integer> indexes = new HashMap<>();

    for (int i = 0; i < sourceItems.getNumberOfColumns(); i++) {
      BeeColumn column = sourceItems.getColumn(i);

      if (COL_TRADE_ACT.equals(column.getId())) {
        columns.add(column);

      } else if (column.isEditable()) {
        indexes.put(i, columns.size());
        columns.add(column);
      }
    }

    BeeRowSet rowSet = new BeeRowSet(getViewName(), columns);

    int actIndex = rowSet.getColumnIndex(COL_TRADE_ACT);
    int priceIndex = rowSet.getColumnIndex(COL_TRADE_ITEM_PRICE);

    for (BeeRow sourceItem : sourceItems) {
      BeeRow row = DataUtils.createEmptyRow(columns.size());
      row.setValue(actIndex, targetId);

      for (Map.Entry<Integer, Integer> indexEntry : indexes.entrySet()) {
        if (!sourceItem.isNull(indexEntry.getKey())) {
          row.setValue(indexEntry.getValue(), sourceItem.getString(indexEntry.getKey()));
        }
      }

      Double price = row.getDouble(priceIndex);

      if (BeeUtils.nonZero(price) && DataUtils.isId(sourceCurrency)
          && DataUtils.isId(targetCurrency) && !sourceCurrency.equals(targetCurrency)) {
        row.setValue(priceIndex, Money.exchange(sourceCurrency, targetCurrency, price, date));
      }

      rowSet.addRow(row);
    }

    Queries.insertRows(rowSet);
  }

  private void addItems(final BeeRowSet rowSet) {
    if (!DataUtils.isEmpty(rowSet) && VIEW_ITEMS.equals(rowSet.getViewName())) {
      getGridView().ensureRelId(new IdCallback() {
        @Override
        public void onSuccess(Long result) {
          IsRow parentRow = ViewHelper.getFormRow(getGridView());

          if (DataUtils.idEquals(parentRow, result)) {
            ItemPrice itemPrice = TradeActKeeper.getItemPrice(VIEW_TRADE_ACTS, parentRow);

            DateTime date = Data.getDateTime(VIEW_TRADE_ACTS, parentRow, COL_TA_DATE);
            Long currency = Data.getLong(VIEW_TRADE_ACTS, parentRow, COL_TA_CURRENCY);

            addItems(parentRow, date, currency, itemPrice, getDefaultDiscount(), rowSet);
          }
        }
      });
    }
  }

  private void addItems(IsRow parentRow, DateTime date, Long currency, ItemPrice itemPrice,
      Double discount, BeeRowSet items) {

    List<String> colNames = Lists.newArrayList(COL_TRADE_ACT, COL_TA_ITEM,
        COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE, COL_TRADE_DISCOUNT);
    BeeRowSet rowSet = new BeeRowSet(getViewName(), Data.getColumns(getViewName(), colNames));

    int actIndex = rowSet.getColumnIndex(COL_TRADE_ACT);
    int itemIndex = rowSet.getColumnIndex(COL_TA_ITEM);
    int qtyIndex = rowSet.getColumnIndex(COL_TRADE_ITEM_QUANTITY);
    int priceIndex = rowSet.getColumnIndex(COL_TRADE_ITEM_PRICE);
    int discountIndex = rowSet.getColumnIndex(COL_TRADE_DISCOUNT);

    for (BeeRow item : items) {
      Double qty = BeeUtils.toDoubleOrNull(item.getProperty(PRP_QUANTITY));

      if (BeeUtils.isDouble(qty)) {
        BeeRow row = DataUtils.createEmptyRow(rowSet.getNumberOfColumns());

        row.setValue(actIndex, parentRow.getId());
        row.setValue(itemIndex, item.getId());

        row.setValue(qtyIndex, qty);

        if (itemPrice != null) {
          Double price = item.getDouble(items.getColumnIndex(itemPrice.getPriceColumn()));

          if (BeeUtils.isDouble(price)) {
            if (DataUtils.isId(currency)) {
              Long ic = item.getLong(items.getColumnIndex(itemPrice.getCurrencyColumn()));
              if (DataUtils.isId(ic) && !currency.equals(ic)) {
                price = Money.exchange(ic, currency, price, date);
              }
            }

            row.setValue(priceIndex, Data.round(getViewName(), COL_TRADE_ITEM_PRICE, price));
          }
        }

        if (BeeUtils.nonZero(discount)) {
          row.setValue(discountIndex, discount);
        }

        rowSet.addRow(row);
      }
    }

    if (!rowSet.isEmpty()) {
      Queries.insertRows(rowSet);
    }
  }

  private IsRow getParentRow() {
    return ViewHelper.getFormRow(getGridView());
  }

  private FileCollector ensureCollector() {
    if (collector == null) {
      collector = FileCollector.headless(new Consumer<Collection<? extends FileInfo>>() {
        @Override
        public void accept(Collection<? extends FileInfo> input) {
          List<FileInfo> fileInfos = FileUtils.validateFileSize(input, 100_000L, getGridView());

          if (!BeeUtils.isEmpty(fileInfos)) {
            List<String> fileNames = new ArrayList<>();
            for (FileInfo fileInfo : fileInfos) {
              fileNames.add(fileInfo.getName());
            }

            final String importCaption = BeeUtils.joinItems(fileNames);

            readImport(fileInfos, new Consumer<List<String>>() {
              @Override
              public void accept(final List<String> lines) {
                if (lines.isEmpty()) {
                  getGridView().notifyWarning(importCaption, Localized.getConstants().noData());

                } else {
                  Global.getParameter(PRM_IMPORT_TA_ITEM_RX, new Consumer<String>() {
                    @Override
                    public void accept(String pattern) {
                      List<ImportEntry> importEntries = TradeActItemImporter.parse(lines,
                          BeeUtils.notEmpty(pattern, RX_IMPORT_ACT_ITEM));

                      if (importEntries.isEmpty()) {
                        getGridView().notifyWarning(importCaption,
                            Localized.getConstants().nothingFound());

                      } else {
                        IsRow parentRow = getParentRow();
                        TradeActKind kind = TradeActKeeper.getKind(VIEW_TRADE_ACTS, parentRow);
                        Long wFrom = TradeActKeeper.getWarehouseFrom(VIEW_TRADE_ACTS, parentRow);

                        Set<Long> itemIds = new HashSet<>();

                        if (!getGridView().isEmpty()) {
                          int itemIndex = getDataIndex(COL_TA_ITEM);
                          for (IsRow row : getGridView().getRowData()) {
                            itemIds.add(row.getLong(itemIndex));
                          }
                        }

                        TradeActItemImporter.queryItems(importCaption, importEntries,
                            kind, wFrom, itemIds, new Consumer<BeeRowSet>() {
                              @Override
                              public void accept(BeeRowSet items) {
                                addItems(items);
                              }
                            });
                      }
                    }
                  });
                }
              }
            });
          }
        }
      });

      collector.setAccept(CommUtils.MEDIA_TYPE_TEXT_PLAIN);
      getGridView().add(collector);
    }
    return collector;
  }

  private void readImport(Collection<? extends FileInfo> input,
      final Consumer<List<String>> consumer) {

    final List<File> files = new ArrayList<>();

    for (FileInfo fileInfo : input) {
      File file = (fileInfo instanceof NewFileInfo) ? ((NewFileInfo) fileInfo).getFile() : null;
      if (file != null) {
        files.add(file);
      }
    }

    if (files.isEmpty()) {
      getGridView().notifyWarning(Localized.getConstants().noData());

    } else {
      final List<String> lines = new ArrayList<>();

      final Latch latch = new Latch(files.size());
      for (File file : files) {
        FileUtils.readLines(file, new Consumer<List<String>>() {
          @Override
          public void accept(List<String> content) {
            if (!content.isEmpty()) {
              lines.addAll(content);
            }

            latch.decrement();
            if (latch.isOpen()) {
              consumer.accept(lines);
            }
          }
        });
      }
    }
  }

  private TradeActItemPicker ensurePicker() {
    if (picker == null) {
      picker = new TradeActItemPicker();
      picker.addSelectionHandler(this);
    }

    return picker;
  }

  private Double getDefaultDiscount() {
    IsRow row = getGridView().getActiveRow();
    if (row == null) {
      row = BeeUtils.getLast(getGridView().getRowData());
    }

    return (row == null) ? null : row.getDouble(getDataIndex(COL_TRADE_DISCOUNT));
  }
}
