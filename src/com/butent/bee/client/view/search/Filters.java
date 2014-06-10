package com.butent.bee.client.view.search;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.FilterComponent;
import com.butent.bee.shared.data.filter.FilterDescription;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Filters implements HasExtendedInfo {

  private static final class Initial extends CustomDiv {

    private static final String STYLE_CHECKED = "checked";
    private static final String STYLE_UNCHECKED = "unchecked";

    private final long id;
    private boolean value;

    private Initial(String styleName, long id, boolean value) {
      super(styleName);
      this.id = id;
      this.value = value;

      addStyleDependentName(value ? STYLE_CHECKED : STYLE_UNCHECKED);
    }

    private void toggle() {
      removeStyleDependentName(value ? STYLE_CHECKED : STYLE_UNCHECKED);

      value = !value;

      addStyleDependentName(value ? STYLE_CHECKED : STYLE_UNCHECKED);
    }
  }

  private static final class Item implements HasInfo {

    private long id;

    private final FilterDescription filterDescription;

    private final int ordinal;
    private final boolean predefined;

    private Item(long id, FilterDescription filterDescription, int ordinal, boolean predefined) {
      this.id = id;
      this.filterDescription = filterDescription;
      this.ordinal = ordinal;
      this.predefined = predefined;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof Item) ? getId() == ((Item) obj).getId() : false;
    }

    @Override
    public List<Property> getInfo() {
      List<Property> info = PropertyUtils.createProperties("Id", getId(),
          "Ordinal", getOrdinal(), "Predefined", isPredefined());
      
      if (filterDescription != null) {
        info.addAll(filterDescription.getInfo());
      }
      return info;
    }
    
    @Override
    public int hashCode() {
      return Longs.hashCode(getId());
    }

    private boolean containsAnyComponent(Collection<String> names) {
      return filterDescription.containsAnyComponent(names);
    }

    private List<FilterComponent> getComponents() {
      return filterDescription.getComponents();
    }

    private long getId() {
      return id;
    }

    private String getLabel() {
      return filterDescription.getLabel();
    }

    private String getName() {
      return filterDescription.getName();
    }

    private int getOrdinal() {
      return ordinal;
    }

    private boolean isEditable() {
      return filterDescription.isEditable();
    }

    private boolean isInitial() {
      return filterDescription.isInitial();
    }

    private boolean isPredefined() {
      return predefined;
    }

    private boolean isRemovable() {
      return filterDescription.isRemovable();
    }

    private boolean sameComponents(Collection<FilterComponent> otherComponents) {
      return filterDescription.sameComponents(otherComponents);
    }

    private void setId(long id) {
      this.id = id;
    }

    private void setInitial(Boolean initial) {
      filterDescription.setInitial(initial);
    }

    private void setLabel(String label) {
      filterDescription.setLabel(label);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Filters.class);

  private static final String COL_NAME = "Name";
  private static final String COL_LABEL = "Label";
  private static final String COL_INITIAL = "Initial";
  private static final String COL_EDITABLE = "Editable";
  private static final String COL_REMOVABLE = "Removable";
  private static final String COL_PREDEFINED = "Predefined";
  private static final String COL_VALUE = "Value";

  private static final String STYLE_PREFIX = "bee-Filters-";
  private static final String STYLE_WRAPPER = STYLE_PREFIX + "wrapper";

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_ROW = STYLE_PREFIX + "row";
  private static final String STYLE_ROW_INITIAL = STYLE_ROW + "-initial";
  private static final String STYLE_ROW_ACTIVE = STYLE_ROW + "-active";
  private static final String STYLE_ROW_PREDEFINED = STYLE_ROW + "-predefined";

  private static final String STYLE_INITIAL = STYLE_PREFIX + "initial";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_EDIT = STYLE_PREFIX + "edit";
  private static final String STYLE_DELETE = STYLE_PREFIX + "delete";

  private static Item getItem(Collection<Item> items, long id) {
    for (Item item : items) {
      if (item.getId() == id) {
        return item;
      }
    }
    return null;
  }

  private static void insert(String key, FilterDescription filterDescription, int ordinal,
      boolean predefined, RowCallback callback) {

    List<BeeColumn> columns =
        Data.getColumns(VIEW_FILTERS,
            Lists.newArrayList(COL_FILTER_USER, COL_FILTER_KEY,
                COL_NAME, COL_LABEL, COL_VALUE, COL_FILTER_ORDINAL));

    List<String> values = Queries.asList(BeeKeeper.getUser().getUserId(), key,
        filterDescription.getName(), filterDescription.getLabel(),
        filterDescription.serializeComponents(), ordinal);

    if (filterDescription.isInitial()) {
      columns.add(Data.getColumn(VIEW_FILTERS, COL_INITIAL));
      values.add(BooleanValue.pack(filterDescription.isInitial()));
    }

    if (filterDescription.isEditable()) {
      columns.add(Data.getColumn(VIEW_FILTERS, COL_EDITABLE));
      values.add(BooleanValue.pack(filterDescription.isEditable()));
    }
    if (filterDescription.isRemovable()) {
      columns.add(Data.getColumn(VIEW_FILTERS, COL_REMOVABLE));
      values.add(BooleanValue.pack(filterDescription.isRemovable()));
    }

    if (predefined) {
      columns.add(Data.getColumn(VIEW_FILTERS, COL_PREDEFINED));
      values.add(BooleanValue.pack(predefined));
    }

    Queries.insert(VIEW_FILTERS, columns, values, null, callback);
  }

  private static void synchronizeInitialFilters(List<Item> items, Item checkedItem,
      HtmlTable table) {
    Set<String> checkedKeys = Sets.newHashSet();
    for (FilterComponent component : checkedItem.getComponents()) {
      checkedKeys.add(component.getName());
    }

    for (Item item : items) {
      if (item.id != checkedItem.id && item.isInitial() && item.containsAnyComponent(checkedKeys)) {
        item.setInitial(null);
        Queries.update(VIEW_FILTERS, item.id, COL_INITIAL, BooleanValue.NULL);

        for (Widget widget : table) {
          if (widget instanceof Initial && ((Initial) widget).id == item.id) {
            ((Initial) widget).toggle();
            break;
          }
        }
      }
    }
  }

  private final Multimap<String, Item> itemsByKey = ArrayListMultimap.create();
  private int maxLabelLength = BeeConst.UNDEF;

  private int keyColumnIndex = BeeConst.UNDEF;
  private int nameColumnIndex;

  private int labelColumnIndex;
  private int initialColumnIndex;
  private int ordinalColumnIndex;

  private int editableColumnIndex;

  private int removableColumnIndex;

  private int predefinedColumnIndex;

  private int valueColumnIndex;

  public Filters() {
    super();
  }

  public void addCustomFilter(final String key, String label,
      Collection<FilterComponent> components, final Scheduler.ScheduledCommand callback) {

    Assert.notEmpty(key);
    Assert.notEmpty(label);
    Assert.notEmpty(components);

    FilterDescription filterDescription =
        FilterDescription.userDefined(normalizeLabel(label), components);

    int ordinal = -1;
    if (itemsByKey.containsKey(key)) {
      for (Item item : itemsByKey.get(key)) {
        ordinal = Math.max(ordinal, item.getOrdinal());
      }
    }

    insert(key, filterDescription, ordinal + 1, false, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        itemsByKey.put(key, createItem(result));
        if (callback != null) {
          callback.execute();
        }
      }
    });
  }

  public boolean contains(String key, Collection<FilterComponent> components) {
    if (BeeUtils.isEmpty(key) || BeeUtils.isEmpty(components)) {
      return false;
    }

    if (itemsByKey.containsKey(key)) {
      for (Item item : itemsByKey.get(key)) {
        if (item.sameComponents(components)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean containsKey(String key) {
    return itemsByKey.containsKey(key);
  }

  public Widget createWidget(final String key, Collection<FilterComponent> activeComponents,
      final BiConsumer<FilterDescription, Action> callback) {

    Assert.notEmpty(key);
    Assert.notNull(callback);

    final HtmlTable table = new HtmlTable(STYLE_TABLE);

    Simple wrapper = new Simple(table);
    wrapper.addStyleName(STYLE_WRAPPER);

    final List<Item> items = getItems(key);
    if (items.isEmpty()) {
      return wrapper;
    }

    int r = 0;

    for (Item item : items) {
      int c = 0;

      final long id = item.getId();

      final Initial initial = new Initial(STYLE_INITIAL, id, item.isInitial());
      initial.setTitle(Localized.getConstants().initialFilter());

      initial.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          initial.toggle();
          Boolean value = BeeUtils.isTrue(initial.value) ? true : null;

          Item updatedItem = getItem(items, id);
          updatedItem.setInitial(value);

          Queries.update(VIEW_FILTERS, id, COL_INITIAL, BooleanValue.getInstance(value));

          if (BeeUtils.isTrue(value) && items.size() > 1) {
            synchronizeInitialFilters(items, updatedItem, table);
          }
        }
      });

      table.setWidgetAndStyle(r, c, initial, STYLE_INITIAL);
      c++;

      final CustomDiv labelWidget = new CustomDiv();
      labelWidget.setHtml(Localized.maybeTranslate(item.getLabel()));

      labelWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          callback.accept(getItem(items, id).filterDescription, null);
        }
      });

      table.setWidgetAndStyle(r, c, labelWidget, STYLE_LABEL);
      c++;

      if (item.isEditable()) {
        Image edit = new Image(Global.getImages().silverEdit());
        edit.addStyleName(STYLE_EDIT);
        edit.setTitle(Localized.getConstants().actionRenameFilter());

        edit.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            final Item editItem = getItem(items, id);
            final String oldLabel = normalizeLabel(Localized.maybeTranslate(editItem.getLabel()));

            Global.inputString(Localized.getConstants().actionRenameFilter(), null,
                new StringCallback() {
                  @Override
                  public void onSuccess(String newValue) {
                    String newLabel = normalizeLabel(newValue);
                    if (!BeeUtils.isEmpty(newLabel) && !newLabel.equals(oldLabel)) {
                      editItem.setLabel(newLabel);
                      labelWidget.setHtml(newLabel);

                      Queries.update(VIEW_FILTERS, editItem.getId(), COL_LABEL,
                          new TextValue(newLabel));
                      callback.accept(null, Action.EDIT);
                    }
                  }
                }, oldLabel, getMaxLabelLength());
          }
        });

        table.setWidgetAndStyle(r, c, edit, STYLE_EDIT);
      }

      c++;

      if (item.isRemovable()) {
        Image delete = new Image(Global.getImages().silverMinus());
        delete.setTitle(Localized.getConstants().actionRemove());

        delete.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            final Item delItem = getItem(items, id);
            List<String> messages =
                Lists.newArrayList(Localized.getConstants().actionDeleteFilter(), BeeUtils
                    .joinWords(delItem.getLabel(), "?"));

            Global.confirmDelete(Localized.getConstants().filterRemove(), Icon.WARNING, messages,
                new ConfirmationCallback() {
                  @Override
                  public void onConfirm() {
                    Queries.deleteRow(VIEW_FILTERS, delItem.getId());
                    itemsByKey.remove(key, delItem);

                    int index = items.indexOf(delItem);
                    items.remove(index);
                    table.removeRow(index);

                    callback.accept(null, Action.DELETE);
                  }
                });
          }
        });

        table.setWidgetAndStyle(r, c, delete, STYLE_DELETE);
      }

      c++;

      table.getRowFormatter().addStyleName(r, STYLE_ROW);

      if (item.isInitial()) {
        table.getRowFormatter().addStyleName(r, STYLE_ROW_INITIAL);
      }
      if (item.isPredefined()) {
        table.getRowFormatter().addStyleName(r, STYLE_ROW_PREDEFINED);
      }

      if (!BeeUtils.isEmpty(activeComponents) && item.sameComponents(activeComponents)) {
        table.getRowFormatter().addStyleName(r, STYLE_ROW_ACTIVE);
      }

      r++;
    }

    return wrapper;
  }

  public void ensurePredefinedFilters(final String key, List<FilterDescription> filters) {
    if (!BeeUtils.isEmpty(key) && !BeeUtils.isEmpty(filters) && !itemsByKey.containsKey(key)) {
      List<FilterDescription> predefinedFilters = Lists.newArrayList(filters);
      ensureIndexes();

      for (int i = 0; i < predefinedFilters.size(); i++) {
        FilterDescription filterDescription = predefinedFilters.get(i).copy();

        itemsByKey.put(key, new Item(DataUtils.NEW_ROW_ID, filterDescription, i, true));

        insert(key, filterDescription, i, true, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            String name = BeeUtils.trim(result.getString(nameColumnIndex));
            for (Item item : itemsByKey.get(key)) {
              if (item.getName().equals(name)) {
                item.setId(result.getId());
              }
            }
          }
        });
      }
    }
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = Lists.newArrayList();
    info.add(new ExtendedProperty("keys", BeeUtils.bracket(itemsByKey.keySet().size())));
    if (itemsByKey.isEmpty()) {
      return info;
    }
    
    List<String> keys = Lists.newArrayList(itemsByKey.keySet());
    if (keys.size() > 1) {
      Collections.sort(keys);
    }
    
    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);
      String prefix = BeeUtils.joinWords(BeeUtils.progress(i + 1, keys.size()), key);

      Collection<Item> items = itemsByKey.get(key);
      info.add(new ExtendedProperty(prefix, "items", BeeUtils.bracket(items.size())));
      
      int j = 0;
      for (Item item : items) {
        String root = BeeUtils.joinWords(prefix, BeeUtils.progress(++j, items.size()));
        PropertyUtils.appendChildrenToExtended(info, root, item.getInfo());
      }
    }
    
    return info;
  }

  public List<FilterComponent> getInitialValues(String key) {
    List<FilterComponent> initialValues = Lists.newArrayList();

    if (itemsByKey.containsKey(key)) {
      for (Item item : itemsByKey.get(key)) {
        if (item.isInitial()) {
          initialValues.addAll(item.getComponents());
        }
      }
    }

    return initialValues;
  }
  
  public void load(String serialized) {
    Assert.notEmpty(serialized);

    BeeRowSet rowSet = BeeRowSet.restore(serialized);

    keyColumnIndex = rowSet.getColumnIndex(COL_FILTER_KEY);

    nameColumnIndex = rowSet.getColumnIndex(COL_NAME);
    labelColumnIndex = rowSet.getColumnIndex(COL_LABEL);

    initialColumnIndex = rowSet.getColumnIndex(COL_INITIAL);
    ordinalColumnIndex = rowSet.getColumnIndex(COL_FILTER_ORDINAL);

    editableColumnIndex = rowSet.getColumnIndex(COL_EDITABLE);
    removableColumnIndex = rowSet.getColumnIndex(COL_REMOVABLE);
    predefinedColumnIndex = rowSet.getColumnIndex(COL_PREDEFINED);

    valueColumnIndex = rowSet.getColumnIndex(COL_VALUE);

    itemsByKey.clear();

    for (BeeRow row : rowSet.getRows()) {
      String key = BeeUtils.trim(row.getString(keyColumnIndex));
      Item item = createItem(row);

      itemsByKey.put(key, item);
    }

    logger.info("filters", itemsByKey.size());
  }

  private Item createItem(BeeRow row) {
    if (row == null) {
      return null;
    }
    
    ensureIndexes();

    String name = BeeUtils.trim(row.getString(nameColumnIndex));
    String label = BeeUtils.trim(row.getString(labelColumnIndex));

    Boolean initial = row.getBoolean(initialColumnIndex);
    Integer ordinal = row.getInteger(ordinalColumnIndex);

    Boolean editable = row.getBoolean(editableColumnIndex);
    Boolean removable = row.getBoolean(removableColumnIndex);
    boolean predefined = BeeUtils.isTrue(row.getBoolean(predefinedColumnIndex));

    String value = BeeUtils.trim(row.getString(valueColumnIndex));

    return new Item(row.getId(),
        new FilterDescription(name, label, FilterDescription.restoreComponents(value), initial,
            editable, removable),
        BeeUtils.unbox(ordinal), predefined);
  }

  private void ensureIndexes() {
    if (BeeConst.isUndef(keyColumnIndex)) {
      keyColumnIndex = Data.getColumnIndex(VIEW_FILTERS, COL_FILTER_KEY);

      nameColumnIndex = Data.getColumnIndex(VIEW_FILTERS, COL_NAME);
      labelColumnIndex = Data.getColumnIndex(VIEW_FILTERS, COL_LABEL);

      initialColumnIndex = Data.getColumnIndex(VIEW_FILTERS, COL_INITIAL);
      ordinalColumnIndex = Data.getColumnIndex(VIEW_FILTERS, COL_FILTER_ORDINAL);

      editableColumnIndex = Data.getColumnIndex(VIEW_FILTERS, COL_EDITABLE);
      removableColumnIndex = Data.getColumnIndex(VIEW_FILTERS, COL_REMOVABLE);
      predefinedColumnIndex = Data.getColumnIndex(VIEW_FILTERS, COL_PREDEFINED);

      valueColumnIndex = Data.getColumnIndex(VIEW_FILTERS, COL_VALUE);
    }
  }

  private List<Item> getItems(String key) {
    List<Item> result = Lists.newArrayList();

    if (itemsByKey.containsKey(key)) {
      result.addAll(itemsByKey.get(key));
    }

    return result;
  }

  private int getMaxLabelLength() {
    if (maxLabelLength <= 0) {
      maxLabelLength = Data.getColumnPrecision(VIEW_FILTERS, COL_LABEL);
    }
    return maxLabelLength;
  }

  private String normalizeLabel(String label) {
    return BeeUtils.left(BeeUtils.trim(label), getMaxLabelLength());
  }
}
