package com.butent.bee.client.modules.trade.acts;

import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TradeActItemImporter {

  static final class ImportEntry {
    private final String article;
    private final String name;

    private final Double quantity;

    private BeeRow item;

    private ImportEntry(String article, String name, Double quantity) {
      this.article = article;
      this.name = name;
      this.quantity = quantity;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(TradeActItemImporter.class);

  private static final String STYLE_PREFIX = TradeActKeeper.STYLE_PREFIX + "import-items-";

  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_HAS_CANDIDATES = STYLE_PREFIX + "has-candidates";
  private static final String STYLE_NO_CANDIDATES = STYLE_PREFIX + "no-candidates";

  private static final String STYLE_SAVE = STYLE_PREFIX + "save";
  private static final String STYLE_CLOSE = STYLE_PREFIX + "close";

  private static final String STYLE_WRAPPER = STYLE_PREFIX + "wrapper";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";

  private static final String STYLE_HEADER_ROW = STYLE_PREFIX + "header";
  private static final String STYLE_ITEM_ROW = STYLE_PREFIX + "item";
  private static final String STYLE_SELECTED_ROW = STYLE_PREFIX + "selected";
  private static final String STYLE_DUPLICATE_ROW = STYLE_PREFIX + "duplicate";
  private static final String STYLE_NO_STOCK_ROW = STYLE_PREFIX + "no-stock";

  private static final String STYLE_TOGGLE_PREFIX = STYLE_PREFIX + "toggle-";
  private static final String STYLE_TOGGLE_WIDGET = STYLE_PREFIX + "toggle";

  private static final String STYLE_ARTICLE_PREFIX = STYLE_PREFIX + "article-";
  private static final String STYLE_NAME_PREFIX = STYLE_PREFIX + "name-";
  private static final String STYLE_QTY_PREFIX = STYLE_PREFIX + "qty-";

  private static final String STYLE_STOCK_PREFIX = STYLE_PREFIX + "stock-";
  private static final String STYLE_FROM_PREFIX = STYLE_PREFIX + "from-";

  private static final String STYLE_ID_PREFIX = STYLE_PREFIX + "id-";
  private static final String STYLE_ITEM_NAME_PREFIX = STYLE_PREFIX + "item-name-";

  private static final String STYLE_HEADER_CELL_SUFFIX = "label";
  private static final String STYLE_CELL_SUFFIX = "cell";

  static List<ImportEntry> parse(List<String> lines, String pattern) {
    List<ImportEntry> result = new ArrayList<>();

    RegExp rx;
    try {
      rx = RegExp.compile(pattern);
    } catch (RuntimeException ex) {
      logger.severe(pattern, ex);
      BeeKeeper.getScreen().notifySevere("cannot compile pattern", pattern);

      rx = null;
    }

    if (!BeeUtils.isEmpty(lines) && rx != null) {
      for (String line : lines) {
        MatchResult matchResult = rx.exec(line);

        if (matchResult != null && matchResult.getGroupCount() == 4) {
          String article = BeeUtils.trim(matchResult.getGroup(1));
          String name = BeeUtils.trim(matchResult.getGroup(2));
          Double qty = BeeUtils.toDoubleOrNull(matchResult.getGroup(3));

          if (!BeeUtils.isEmpty(article) && BeeUtils.isPositive(qty)) {
            result.add(new ImportEntry(article, name, qty));
          } else {
            logger.warning("cannot parse", line);
            logger.warning("article", article, "name", name, "qty", matchResult.getGroup(3));
          }

        } else {
          logger.warning(line, "does not match");
        }
      }
    }

    logger.debug(pattern);
    logger.debug("matched", result.size(), "of", lines.size());

    return result;
  }

  static void queryItems(final String caption, final List<ImportEntry> input, TradeActKind kind,
      final Long warehouseFrom, final Collection<Long> existingItems,
      final Consumer<BeeRowSet> consumer) {

    ParameterList params = TradeActKeeper.createArgs(SVC_GET_ITEMS_FOR_SELECTION);
    if (kind != null) {
      params.addDataItem(COL_TA_KIND, kind.ordinal());
    }

    Set<String> articles = new HashSet<>();
    for (ImportEntry entry : input) {
      articles.add(entry.article);
    }

    Filter filter = Filter.anyString(COL_ITEM_ARTICLE, articles);
    params.addDataItem(Service.VAR_VIEW_WHERE, filter.serialize());

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRowSet.class)) {
          BeeRowSet items = BeeRowSet.restore(response.getResponseAsString());
          int index = items.getColumnIndex(COL_ITEM_ARTICLE);

          List<ImportEntry> output = new ArrayList<>();
          for (ImportEntry ie : input) {
            ImportEntry entry = new ImportEntry(ie.article, ie.name, ie.quantity);
            entry.item = items.findRow(index, entry.article);

            output.add(entry);
          }

          Map<Long, String> warehouses =
              TradeActKeeper.getWarehouses(TradeActKeeper.extractWarehouses(items));
          show(caption, output, warehouses, warehouseFrom, existingItems, consumer);

        } else {
          Map<Long, String> whs = Collections.emptyMap();
          show(caption, input, whs, warehouseFrom, existingItems, consumer);
        }
      }
    });
  }

  static void show(String caption, final List<ImportEntry> entries,
      Map<Long, String> warehouses, Long warehouseFrom, Collection<Long> existingItems,
      final Consumer<BeeRowSet> consumer) {

    List<Long> warehouseIds = new ArrayList<>();
    if (!BeeUtils.isEmpty(warehouses)) {
      warehouseIds.addAll(warehouses.keySet());
    }

    final HtmlTable table = new HtmlTable(STYLE_TABLE);

    String pfx;
    boolean duplicate;

    int r = 0;
    int c = 1;

    table.setText(r, c++, Localized.getConstants().article(),
        STYLE_ARTICLE_PREFIX + STYLE_HEADER_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().name(),
        STYLE_NAME_PREFIX + STYLE_HEADER_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().quantity(),
        STYLE_QTY_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    for (Long w : warehouseIds) {
      pfx = w.equals(warehouseFrom) ? STYLE_FROM_PREFIX : STYLE_STOCK_PREFIX;
      table.setText(r, c++, warehouses.get(w), pfx + STYLE_HEADER_CELL_SUFFIX);
    }

    table.setText(r, c++, Localized.getLabel(Data.getColumn(VIEW_ITEMS, COL_ITEM_NAME)),
        STYLE_ID_PREFIX + STYLE_HEADER_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().captionId(),
        STYLE_ID_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    table.getRowFormatter().addStyleName(r, STYLE_HEADER_ROW);

    int nameIndex = Data.getColumnIndex(VIEW_ITEMS, COL_ITEM_NAME);
    int qtyScale = Data.getColumnScale(VIEW_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY);

    Set<Long> itemIds = new HashSet<>();
    if (!BeeUtils.isEmpty(existingItems)) {
      itemIds.addAll(existingItems);
    }

    boolean hasCandidates = false;

    r++;
    for (ImportEntry entry : entries) {
      c = 0;

      duplicate = entry.item != null && itemIds.contains(entry.item.getId());

      if (entry.item != null && !duplicate) {
        Toggle toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
            STYLE_TOGGLE_WIDGET, true);

        toggle.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (event.getSource() instanceof Toggle) {
              onToggle((Toggle) event.getSource());
            }
          }
        });

        table.setWidget(r, c, toggle, STYLE_TOGGLE_PREFIX + STYLE_CELL_SUFFIX);

        itemIds.add(entry.item.getId());
        hasCandidates = true;
      }
      c++;

      table.setText(r, c++, entry.article, STYLE_ARTICLE_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, entry.name, STYLE_NAME_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, BeeUtils.toString(entry.quantity, qtyScale),
          STYLE_QTY_PREFIX + STYLE_CELL_SUFFIX);

      if (entry.item != null) {
        Double qtyFrom = null;

        if (!warehouseIds.isEmpty() && !BeeUtils.isEmpty(entry.item.getProperties())) {
          for (Map.Entry<String, String> property : entry.item.getProperties().entrySet()) {
            if (BeeUtils.isPrefix(property.getKey(), PRP_WAREHOUSE_PREFIX)) {
              Long w = BeeUtils.toLongOrNull(BeeUtils.removePrefix(property.getKey(),
                  PRP_WAREHOUSE_PREFIX));

              if (DataUtils.isId(w) && warehouseIds.contains(w)) {
                if (w.equals(warehouseFrom)) {
                  pfx = STYLE_FROM_PREFIX;
                  qtyFrom = BeeUtils.toDoubleOrNull(property.getValue());
                } else {
                  pfx = STYLE_STOCK_PREFIX;
                }

                table.setText(r, c + warehouseIds.indexOf(w), property.getValue(),
                    pfx + STYLE_CELL_SUFFIX);
              }
            }
          }
        }
        c += warehouseIds.size();

        table.setText(r, c++, entry.item.getString(nameIndex),
            STYLE_ITEM_NAME_PREFIX + STYLE_CELL_SUFFIX);
        table.setText(r, c++, BeeUtils.toString(entry.item.getId()),
            STYLE_ID_PREFIX + STYLE_CELL_SUFFIX);

        if (!duplicate && DataUtils.isId(warehouseFrom)
            && (!BeeUtils.isPositive(qtyFrom) || BeeUtils.isMore(entry.quantity, qtyFrom))) {
          table.getRowFormatter().addStyleName(r, STYLE_NO_STOCK_ROW);
        }
      }

      table.getRowFormatter().addStyleName(r, STYLE_ITEM_ROW);

      if (duplicate) {
        table.getRowFormatter().addStyleName(r, STYLE_DUPLICATE_ROW);
        table.getRow(r).setTitle(Localized.getMessages().valueExists(entry.article));

      } else if (entry.item != null) {
        table.getRowFormatter().addStyleName(r, STYLE_SELECTED_ROW);
      }

      r++;
    }

    final DialogBox dialog = DialogBox.withoutCloseBox(caption, STYLE_DIALOG);
    dialog.addStyleName(hasCandidates ? STYLE_HAS_CANDIDATES : STYLE_NO_CANDIDATES);

    if (hasCandidates) {
      Toggle toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
          STYLE_TOGGLE_WIDGET, true);

      toggle.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (event.getSource() instanceof Toggle) {
            boolean checked = ((Toggle) event.getSource()).isChecked();

            Collection<Toggle> toggles = UiHelper.getChildren(table, Toggle.class);
            for (Toggle t : toggles) {
              if (t.isChecked() != checked) {
                t.setChecked(checked);
                onToggle(t);
              }
            }
          }
        }
      });

      table.setWidget(0, 0, toggle, STYLE_TOGGLE_PREFIX + STYLE_HEADER_CELL_SUFFIX);

      FaLabel save = new FaLabel(FontAwesome.SAVE);
      save.addStyleName(STYLE_SAVE);

      save.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          dialog.close();

          BeeRowSet selection = new BeeRowSet(VIEW_ITEMS, Data.getColumns(VIEW_ITEMS));
          for (int i = 0; i < entries.size(); i++) {
            if (table.getRow(i + 1).hasClassName(STYLE_SELECTED_ROW)) {
              ImportEntry entry = entries.get(i);

              if (entry.item != null) {
                BeeRow row = DataUtils.cloneRow(entry.item);
                row.setProperty(PRP_QUANTITY, BeeUtils.toString(entry.quantity));

                selection.addRow(row);
              }
            }
          }

          if (!selection.isEmpty()) {
            consumer.accept(selection);
          }
        }
      });

      dialog.addAction(Action.SAVE, save);
    }

    FaLabel close = new FaLabel(FontAwesome.CLOSE);
    close.addStyleName(STYLE_CLOSE);

    close.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        dialog.close();
      }
    });

    dialog.addAction(Action.CLOSE, close);

    Simple wrapper = new Simple(table);
    wrapper.addStyleName(STYLE_WRAPPER);

    dialog.setWidget(wrapper);
    dialog.center();
  }

  private static void onToggle(Toggle toggle) {
    TableRowElement rowElement = DomUtils.getParentRow(toggle.getElement(), false);

    if (rowElement != null) {
      if (toggle.isChecked()) {
        rowElement.addClassName(STYLE_SELECTED_ROW);
      } else {
        rowElement.removeClassName(STYLE_SELECTED_ROW);
      }
    }
  }

  private TradeActItemImporter() {
  }
}
