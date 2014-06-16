package com.butent.bee.client.screen;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.event.logical.BookmarkEvent;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.HandlesDeleteEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Favorites implements HandlesDeleteEvents {

  public enum Group {
    ROW {
      @Override
      String getCaption(String key) {
        return Data.getViewCaption(key);
      }

      @Override
      long getDomainKey(String key) {
        String s = "View_" + key;
        return s.hashCode();
      }

      @Override
      void open(String key, long id, String html) {
        if (!RowActionEvent.fireOpenFavorite(key, id, html)) {
          return;
        }

        DataInfo dataInfo = Data.getDataInfo(key);
        if (dataInfo == null) {
          return;
        }

        String formName = dataInfo.getEditForm();
        if (BeeUtils.isEmpty(formName)) {
          return;
        }

        RowEditor.openRow(formName, dataInfo, id);
      }
    };

    private final Map<String, HtmlTable> displays = new HashMap<>();

    private final Map<String, List<Item>> items = new HashMap<>();

    abstract String getCaption(String key);

    abstract long getDomainKey(String key);

    abstract void open(String key, long id, String html);

    private void add(String key, Item item) {
      if (!BeeUtils.isEmpty(key) && item != null) {
        if (!items.containsKey(key)) {
          items.put(key, Lists.newArrayList(item));
        } else {
          items.get(key).add(item);
        }
      }
    }

    private void clear() {
      displays.clear();
      items.clear();
    }

    private Widget createItemWidget(String key, Item item) {
      return createItemWidget(key, item.getId(), item.getHtml());
    }

    private Widget createItemWidget(final String key, final long id, final String html) {
      InternalLink itemWidget = new InternalLink(html.trim());
      itemWidget.addStyleName(ITEM_STYLE);

      itemWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Group.this.open(key, id, html);
        }
      });

      return itemWidget;
    }

    private Item find(String key, long id) {
      List<Item> values = items.get(key);
      if (values != null) {
        for (Item item : values) {
          if (item.getId() == id) {
            return item;
          }
        }
      }
      return null;
    }

    private HtmlTable getDisplay(String key) {
      return displays.get(key);
    }

    private int getSize() {
      int size = 0;
      for (List<Item> values : items.values()) {
        if (values != null) {
          size += values.size();
        }
      }
      return size;
    }

    private int indexOf(String key, Item item) {
      List<Item> values = items.get(key);
      if (values != null) {
        return values.indexOf(item);
      } else {
        return BeeConst.UNDEF;
      }
    }

    private int maxOrder(String key) {
      int result = 0;
      List<Item> values = items.get(key);
      if (values != null) {
        for (Item item : values) {
          result = Math.max(result, item.getOrder());
        }
      }
      return result;
    }

    private void registerDomainEntry(String key) {
      BeeKeeper.getScreen().addDomainEntry(Domain.FAVORITES, getDisplay(key), getDomainKey(key),
          getCaption(key));
    }

    private boolean remove(String key, Item item) {
      List<Item> values = items.get(key);
      if (values == null) {
        return false;
      } else {
        return values.remove(item);
      }
    }
  }

  private static final class Item {
    private final long id;

    private String html;
    private final int order;

    private Item(long id, String html) {
      this(id, html, 0);
    }

    private Item(long id, String html, int order) {
      this.id = id;

      this.html = html;
      this.order = order;
    }

    private String getHtml() {
      return html;
    }

    private long getId() {
      return id;
    }

    private int getOrder() {
      return order;
    }

    private void setHtml(String html) {
      this.html = html;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Favorites.class);

  private static final String DISPLAY_STYLE = "bee-FavoritesDisplay";
  private static final String ITEM_COLUMN_STYLE = "bee-FavoritesItemColumn";
  private static final String EDIT_COLUMN_STYLE = "bee-FavoritesEditColumn";
  private static final String DELETE_COLUMN_STYLE = "bee-FavoritesDeleteColumn";

  private static final String ITEM_STYLE = "bee-FavoritesItem";
  private static final String EDIT_STYLE = "bee-FavoritesEdit";
  private static final String DELETE_STYLE = "bee-FavoritesDelete";

  private static final int ITEM_COLUMN = 0;
  private static final int EDIT_COLUMN = 1;
  private static final int DELETE_COLUMN = 2;

  private static final String COL_GROUP = "Group";
  private static final String COL_KEY = "Key";
  private static final String COL_ITEM = "Item";
  private static final String COL_ORDER = "Order";
  private static final String COL_HTML = "Html";

  private static final List<BeeColumn> columns = new ArrayList<>();

  private static void addDisplayRow(final HtmlTable display, final Group group, final String key,
      final Item item) {
    int row = display.getRowCount();

    Widget widget = group.createItemWidget(key, item);
    display.setWidget(row, ITEM_COLUMN, widget);

    FaLabel edit = new FaLabel(FontAwesome.EDIT, EDIT_STYLE);
    edit.setTitle(Localized.getConstants().actionRename());

    edit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Global.inputString(Localized.getConstants().actionRename(), null, new StringCallback() {
          @Override
          public void onSuccess(String value) {
            updateItem(group, key, item.getId(), value);
          }
        }, item.getHtml(), BeeConst.UNDEF, display.getEventRowElement(event, false));
      }
    });

    display.setWidget(row, EDIT_COLUMN, edit);

    FaLabel delete = new FaLabel(FontAwesome.TRASH_O, DELETE_STYLE);
    delete.setTitle(Localized.getConstants().actionRemove());

    delete.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Global.confirmRemove(group.getCaption(key), item.getHtml(), new ConfirmationCallback() {
          @Override
          public void onConfirm() {
            removeItem(group, key, item.getId());
          }
        }, display.getEventRowElement(event, false));
      }
    });

    display.setWidget(row, DELETE_COLUMN, delete);
  }
  private static HtmlTable createDisplay() {
    HtmlTable display = new HtmlTable();
    display.addStyleName(DISPLAY_STYLE);

    display.getColumnFormatter().addStyleName(ITEM_COLUMN, ITEM_COLUMN_STYLE);
    display.getColumnFormatter().addStyleName(EDIT_COLUMN, EDIT_COLUMN_STYLE);
    display.getColumnFormatter().addStyleName(DELETE_COLUMN, DELETE_COLUMN_STYLE);

    return display;
  }
  private static boolean removeItem(Group group, String key, long id) {
    Item item = group.find(key, id);
    if (item == null) {
      return false;
    }

    HtmlTable display = group.getDisplay(key);
    display.removeRow(group.indexOf(key, item));

    Filter filter = Filter.and(Filter.isEqual(COL_GROUP, IntegerValue.of(group)),
        Filter.isEqual(COL_KEY, new TextValue(key)), Filter.equals(COL_ITEM, id));

    Queries.delete(VIEW_FAVORITES, filter, null);
    return group.remove(key, item);
  }
  private static boolean updateItem(Group group, String key, long id, String html) {
    Item item = group.find(key, id);
    if (item == null || BeeUtils.equalsTrim(item.getHtml(), html)) {
      return false;
    }
    item.setHtml(html);

    HtmlTable display = group.getDisplay(key);
    display.getWidget(group.indexOf(key, item), ITEM_COLUMN).getElement().setInnerHTML(html.trim());

    CompoundFilter filter = Filter.and();
    filter.add(BeeKeeper.getUser().getFilter(COL_FAVORITE_USER),
        Filter.isEqual(COL_GROUP, IntegerValue.of(group)),
        Filter.isEqual(COL_KEY, new TextValue(key)),
        Filter.equals(COL_ITEM, id));

    Queries.update(VIEW_FAVORITES, filter, COL_HTML, new TextValue(html), null);
    return true;
  }

  private int groupIndex = BeeConst.UNDEF;
  private int keyIndex = BeeConst.UNDEF;
  private int itemIndex = BeeConst.UNDEF;
  private int orderIndex = BeeConst.UNDEF;
  private int htmlIndex = BeeConst.UNDEF;

  public Favorites() {
    super();
  }

  public void addItem(Group group, String key, long id, String html) {
    if (columns.isEmpty()) {
      initViewColumns(Data.getColumns(VIEW_FAVORITES));
    }

    int order = group.maxOrder(key) + 1;
    Item item = new Item(id, html, order);

    group.add(key, item);

    if (!group.displays.containsKey(key)) {
      group.displays.put(key, createDisplay());
      group.registerDomainEntry(key);
    }

    addDisplayRow(group.getDisplay(key), group, key, item);

    Queries.insert(VIEW_FAVORITES,
        DataUtils.getColumns(columns, groupIndex, keyIndex, itemIndex, orderIndex, htmlIndex),
        Lists.newArrayList(BeeUtils.toString(group.ordinal()), key, BeeUtils.toString(id),
            BeeUtils.toString(order), BeeUtils.trim(html)));
  }

  public void bookmark(final String viewName, final IsRow row, List<BeeColumn> sourceColumns,
      List<String> expressions) {
    bookmark(viewName, row, sourceColumns, expressions, BeeConst.STRING_SPACE);
  }

  public void bookmark(final String viewName, final IsRow row, List<BeeColumn> sourceColumns,
      List<String> expressions, String exprSep) {
    if (BeeUtils.isEmpty(viewName) || row == null || BeeUtils.isEmpty(sourceColumns)
        || BeeUtils.isEmpty(expressions)) {
      return;
    }

    final Group group = Group.ROW;

    Item item = group.find(viewName, row.getId());
    if (item != null) {
      Global.showInfo(Lists.newArrayList("Row is already bookmarked as", item.getHtml()));
      return;
    }

    List<String> values = DataUtils.translate(expressions, sourceColumns, row);
    String html = BeeUtils.join(exprSep, values);

    Global.inputString(Localized.getConstants().bookmarkName(), null, new StringCallback() {
      @Override
      public void onSuccess(String value) {
        addItem(group, viewName, row.getId(), value);
        BeeKeeper.getScreen().activateDomainEntry(Domain.FAVORITES, group.getDomainKey(viewName));

        BeeKeeper.getBus().fireEvent(new BookmarkEvent(group, row.getId()));
      }
    }, html);
  }

  public boolean isBookmarked(String viewName, IsRow row) {
    return !BeeUtils.isEmpty(viewName) && DataUtils.hasId(row)
        && Group.ROW.find(viewName, row.getId()) != null;
  }
  
  public void load(String serialized) {
    Assert.notEmpty(serialized);

    BeeRowSet rowSet = BeeRowSet.restore(serialized);
    initViewColumns(rowSet.getColumns());

    loadItems(rowSet);

    int size = 0;
    for (Group group : Group.values()) {
      size += group.getSize();
    }

    logger.info("favorites", size);
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    for (RowInfo rowInfo : event.getRows()) {
      removeItem(Group.ROW, event.getViewName(), rowInfo.getId());
    }
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    removeItem(Group.ROW, event.getViewName(), event.getRowId());
  }

  private Item createItem(BeeRow row) {
    Long itm = row.getLong(itemIndex);
    if (itm == null) {
      return null;
    }

    Integer ord = row.getInteger(orderIndex);
    String html = row.getString(htmlIndex);

    return new Item(itm, html.trim(), BeeUtils.unbox(ord));
  }

  private void initViewColumns(List<BeeColumn> viewColumns) {
    BeeUtils.overwrite(columns, viewColumns);

    groupIndex = DataUtils.getColumnIndex(COL_GROUP, columns);
    keyIndex = DataUtils.getColumnIndex(COL_KEY, columns);
    itemIndex = DataUtils.getColumnIndex(COL_ITEM, columns);
    orderIndex = DataUtils.getColumnIndex(COL_ORDER, columns);
    htmlIndex = DataUtils.getColumnIndex(COL_HTML, columns);
  }

  private void loadItems(BeeRowSet rowSet) {
    if (rowSet.isEmpty()) {
      return;
    }

    for (Group group : Group.values()) {
      group.clear();
    }

    Group lastGroup = null;
    String lastKey = null;

    for (BeeRow row : rowSet.getRows()) {
      Group group = EnumUtils.getEnumByIndex(Group.class, row.getInteger(groupIndex));
      if (group == null) {
        continue;
      }

      String key = row.getString(keyIndex);
      if (BeeUtils.isEmpty(key)) {
        continue;
      }

      Item item = createItem(row);
      if (item == null) {
        continue;
      }

      if (!group.equals(lastGroup) || !key.equals(lastKey)) {
        group.displays.put(key, createDisplay());

        lastGroup = group;
        lastKey = key;
      }

      group.add(key, item);
      addDisplayRow(group.getDisplay(key), group, key, item);
    }

    for (Group group : Group.values()) {
      Set<String> keys = new TreeSet<String>(group.displays.keySet());
      for (String key : keys) {
        group.registerDomainEntry(key);
      }
    }
  }
}
