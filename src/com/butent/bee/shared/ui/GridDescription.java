package com.butent.bee.shared.ui;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.ProviderType;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterDescription;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GridDescription implements BeeSerializable, HasExtendedInfo, HasViewName {

  private enum Serial {
    NAME, PARENT, CAPTION, VIEW, ID_NAME, VERSION_NAME, FILTER, CURRENT_USER_FILTER, ORDER,
    HEADER_MODE, FOOTER_MODE, DATA_PROVIDER, INITIAL_ROW_SET_SIZE, PAGING, READONLY,
    NEW_ROW_FORM, NEW_ROW_COLUMNS, NEW_ROW_DEFAULTS, NEW_ROW_CAPTION, NEW_ROW_WINDOW,
    EDIT_FORM, EDIT_MODE, EDIT_SAVE, EDIT_MESSAGE, EDIT_SHOW_ID, EDIT_WINDOW, EDIT_IN_PLACE,
    ENABLED_ACTIONS, DISABLED_ACTIONS, STYLE_SHEETS, HEADER, BODY, FOOTER,
    ROW_STYLES, ROW_MESSAGE, ROW_EDITABLE, ROW_VALIDATION, MIN_COLUMN_WIDTH, MAX_COLUMN_WIDTH,
    COLUMNS, WIDGETS, AUTO_FIT, AUTO_FLEX, FLEXIBILITY,
    FAVORITE, ENABLE_COPY, CACHE_DATA, CACHE_DESCRIPTION,
    MIN_NUMBER_OF_ROWS, MAX_NUMBER_OF_ROWS, RENDER_MODE, ROW_CHANGE_SENSITIVITY_MILLIS,
    PREDEFINED_FILTERS, OPTIONS, PROPERTIES
  }

  public static final String TBL_GRID_SETTINGS = "GridSettings";
  public static final String VIEW_GRID_SETTINGS = "GridSettings";

  public static final String COL_GRID_SETTING_KEY = "Key";
  public static final String COL_GRID_SETTING_USER = "User";

  public static final String HEADER_MODE_ALL = "all";
  public static final String HEADER_MODE_COLUMN = "column";
  public static final String HEADER_MODE_GRID = "grid";
  public static final String HEADER_MODE_NONE = "none";

  public static final String FOOTER_MODE_NONE = "none";

  public static final char FORM_ITEM_SEPARATOR = BeeConst.CHAR_COMMA;
  public static final char FORM_LABEL_SEPARATOR = BeeConst.CHAR_COLON;

  public static GridDescription restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    GridDescription grid = new GridDescription();
    grid.deserialize(s);
    return grid;
  }

  private String name;
  private String parent;
  private String caption;

  private String viewName;
  private String idName;
  private String versionName;

  private Filter filter;
  private String currentUserFilter;
  private Order order;

  private String headerMode;
  private String footerMode;

  private Boolean cacheData;
  private Boolean cacheDescription;

  private ProviderType dataProvider;
  private Integer initialRowSetSize;
  private Boolean paging;

  private Boolean readOnly;

  private String newRowForm;
  private String newRowColumns;
  private String newRowDefaults;
  private String newRowCaption;
  private WindowType newRowWindow;

  private String editForm;
  private Boolean editMode;
  private Boolean editSave;
  private Calculation editMessage;
  private Boolean editShowId;
  private WindowType editWindow;
  private Boolean editInPlace;

  private Map<String, String> styleSheets;

  private GridComponentDescription header;
  private GridComponentDescription body;
  private GridComponentDescription footer;

  private Collection<ConditionalStyleDeclaration> rowStyles;

  private Calculation rowMessage;
  private Calculation rowEditable;
  private Calculation rowValidation;

  private Integer minColumnWidth;
  private Integer maxColumnWidth;
  private String autoFit;
  private Boolean autoFlex;
  private Flexibility flexibility;

  private final List<ColumnDescription> columns = new ArrayList<>();

  private final Set<Action> enabledActions = new HashSet<>();
  private final Set<Action> disabledActions = new HashSet<>();

  private String favorite;
  private String enableCopy;

  private Integer minNumberOfRows;
  private Integer maxNumberOfRows;

  private String renderMode;

  private Integer rowChangeSensitivityMillis;

  private final List<String> widgets = new ArrayList<>();

  private final List<FilterDescription> predefinedFilters = new ArrayList<>();

  private String options;
  private final Map<String, String> properties = new HashMap<>();

  public GridDescription(String name) {
    this(name, null, null, null);
  }

  public GridDescription(String name, String viewName, String idName, String versionName) {
    super();

    this.name = name;
    this.viewName = viewName;
    this.idName = idName;
    this.versionName = versionName;
  }

  private GridDescription() {
  }

  public void addColumn(ColumnDescription column) {
    Assert.notNull(column);
    Assert.state(!hasColumn(column.getId()),
        BeeUtils.joinWords("Duplicate column id:", getName(), column.getId()));

    getColumns().add(column);
  }

  public GridDescription copy() {
    return restore(serialize());
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    String[] items;

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case NAME:
          setName(value);
          break;
        case VIEW:
          setViewName(value);
          break;
        case CAPTION:
          setCaption(value);
          break;
        case READONLY:
          setReadOnly(BeeUtils.toBooleanOrNull(value));
          break;

        case COLUMNS:
          getColumns().clear();
          items = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(items)) {
            for (String z : items) {
              addColumn(ColumnDescription.restore(z));
            }
          }
          break;

        case DATA_PROVIDER:
          setDataProvider(Codec.unpack(ProviderType.class, value));
          break;
        case BODY:
          setBody(GridComponentDescription.restore(value));
          break;
        case FOOTER:
          setFooter(GridComponentDescription.restore(value));
          break;
        case HEADER_MODE:
          setHeaderMode(value);
          break;
        case FOOTER_MODE:
          setFooterMode(value);
          break;
        case HEADER:
          setHeader(GridComponentDescription.restore(value));
          break;
        case MAX_COLUMN_WIDTH:
          setMaxColumnWidth(BeeUtils.toIntOrNull(value));
          break;
        case MIN_COLUMN_WIDTH:
          setMinColumnWidth(BeeUtils.toIntOrNull(value));
          break;
        case NEW_ROW_COLUMNS:
          setNewRowColumns(value);
          break;
        case NEW_ROW_DEFAULTS:
          setNewRowDefaults(value);
          break;
        case NEW_ROW_CAPTION:
          setNewRowCaption(value);
          break;
        case ROW_EDITABLE:
          setRowEditable(Calculation.restore(value));
          break;
        case ROW_MESSAGE:
          setRowMessage(Calculation.restore(value));
          break;
        case ROW_VALIDATION:
          setRowValidation(Calculation.restore(value));
          break;

        case ROW_STYLES:
          String[] styles = Codec.beeDeserializeCollection(value);

          if (ArrayUtils.isEmpty(styles)) {
            setRowStyles(null);
          } else {
            List<ConditionalStyleDeclaration> lst = new ArrayList<>();
            for (String cs : styles) {
              lst.add(ConditionalStyleDeclaration.restore(cs));
            }
            setRowStyles(lst);
          }
          break;

        case NEW_ROW_FORM:
          setNewRowForm(value);
          break;
        case EDIT_FORM:
          setEditForm(value);
          break;
        case EDIT_MODE:
          setEditMode(BeeUtils.toBooleanOrNull(value));
          break;
        case EDIT_SAVE:
          setEditSave(BeeUtils.toBooleanOrNull(value));
          break;
        case EDIT_MESSAGE:
          setEditMessage(Calculation.restore(value));
          break;
        case EDIT_SHOW_ID:
          setEditShowId(BeeUtils.toBooleanOrNull(value));
          break;
        case INITIAL_ROW_SET_SIZE:
          setInitialRowSetSize(BeeUtils.toIntOrNull(value));
          break;
        case PAGING:
          setPaging(BeeUtils.toBooleanOrNull(value));
          break;
        case FILTER:
          setFilter(Filter.restore(value));
          break;
        case CURRENT_USER_FILTER:
          setCurrentUserFilter(value);
          break;
        case ORDER:
          setOrder(Order.restore(value));
          break;
        case ID_NAME:
          setIdName(value);
          break;
        case VERSION_NAME:
          setVersionName(value);
          break;
        case CACHE_DATA:
          setCacheData(BeeUtils.toBooleanOrNull(value));
          break;
        case CACHE_DESCRIPTION:
          setCacheDescription(BeeUtils.toBooleanOrNull(value));
          break;

        case ENABLED_ACTIONS:
          getEnabledActions().clear();
          items = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(items)) {
            for (String z : items) {
              getEnabledActions().add(Action.restore(z));
            }
          }
          break;
        case DISABLED_ACTIONS:
          getDisabledActions().clear();
          items = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(items)) {
            for (String z : items) {
              getDisabledActions().add(Action.restore(z));
            }
          }
          break;

        case STYLE_SHEETS:
          String[] css = Codec.beeDeserializeCollection(value);

          if (ArrayUtils.isEmpty(css)) {
            setStyleSheets(null);
          } else {
            Map<String, String> map = new HashMap<>();
            for (int j = 0; j < css.length - 1; j += 2) {
              map.put(css[j], css[j + 1]);
            }
            setStyleSheets(map);
          }
          break;

        case WIDGETS:
          getWidgets().clear();
          items = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(items)) {
            for (String z : items) {
              getWidgets().add(z);
            }
          }
          break;

        case NEW_ROW_WINDOW:
          setNewRowWindow(Codec.unpack(WindowType.class, value));
          break;
        case EDIT_WINDOW:
          setEditWindow(Codec.unpack(WindowType.class, value));
          break;
        case PARENT:
          setParent(value);
          break;
        case AUTO_FIT:
          setAutoFit(value);
          break;
        case AUTO_FLEX:
          setAutoFlex(BeeUtils.toBooleanOrNull(value));
          break;
        case FLEXIBILITY:
          setFlexibility(Flexibility.restore(value));
          break;
        case FAVORITE:
          setFavorite(value);
          break;
        case ENABLE_COPY:
          setEnableCopy(value);
          break;
        case MIN_NUMBER_OF_ROWS:
          setMinNumberOfRows(BeeUtils.toIntOrNull(value));
          break;
        case MAX_NUMBER_OF_ROWS:
          setMaxNumberOfRows(BeeUtils.toIntOrNull(value));
          break;
        case RENDER_MODE:
          setRenderMode(value);
          break;
        case ROW_CHANGE_SENSITIVITY_MILLIS:
          setRowChangeSensitivityMillis(BeeUtils.toIntOrNull(value));
          break;

        case EDIT_IN_PLACE:
          setEditInPlace(BeeUtils.toBooleanOrNull(value));
          break;

        case PREDEFINED_FILTERS:
          setPredefinedFilters(FilterDescription.restoreList(value));
          break;

        case OPTIONS:
          setOptions(value);
          break;
        case PROPERTIES:
          setProperties(Codec.deserializeLinkedHashMap(value));
          break;
      }
    }
  }

  public String getAutoFit() {
    return autoFit;
  }

  public Boolean getAutoFlex() {
    return autoFlex;
  }

  public GridComponentDescription getBody() {
    return body;
  }

  public Boolean getCacheData() {
    return cacheData;
  }

  public Boolean getCacheDescription() {
    return cacheDescription;
  }

  public String getCaption() {
    return caption;
  }

  public ColumnDescription getColumn(String id) {
    for (ColumnDescription column : getColumns()) {
      if (column.is(id)) {
        return column;
      }
    }
    return null;
  }

  public int getColumnCount() {
    return getColumns().size();
  }

  public int getColumnIndex(String id) {
    for (int i = 0; i < columns.size(); i++) {
      if (columns.get(i).is(id)) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  public List<ColumnDescription> getColumns() {
    return columns;
  }

  public String getCurrentUserFilter() {
    return currentUserFilter;
  }

  public ProviderType getDataProvider() {
    return dataProvider;
  }

  public Set<Action> getDisabledActions() {
    return disabledActions;
  }

  public String getEditForm() {
    return editForm;
  }

  public Boolean getEditInPlace() {
    return editInPlace;
  }

  public Calculation getEditMessage() {
    return editMessage;
  }

  public Boolean getEditMode() {
    return editMode;
  }

  public WindowType getEditWindow() {
    return editWindow;
  }

  public Boolean getEditSave() {
    return editSave;
  }

  public Boolean getEditShowId() {
    return editShowId;
  }

  public String getEnableCopy() {
    return enableCopy;
  }

  public Set<Action> getEnabledActions() {
    return enabledActions;
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = new ArrayList<>();

    PropertyUtils.addProperties(info, false,
        "Name", getName(),
        "Parent", getParent(),
        "Caption", getCaption(),
        "View Name", getViewName(),
        "Id Name", getIdName(),
        "Version Name", getVersionName(),
        "Filter", getFilter(),
        "Current User Filter", getCurrentUserFilter(),
        "Order", getOrder(),
        "Header Mode", getHeaderMode(),
        "Footer Mode", getFooterMode(),
        "Cache Data", getCacheData(),
        "Cache Description", getCacheDescription(),
        "Data Provider", getDataProvider(),
        "Initial Row Set Size", getInitialRowSetSize(),
        "Paging", getPaging(),
        "Read Only", isReadOnly(),
        "New Row Form", getNewRowForm(),
        "New Row Columns", getNewRowColumns(),
        "New Row Defaults", getNewRowDefaults(),
        "New Row Caption", getNewRowCaption(),
        "New Row Window", getNewRowWindow(),
        "Edit Form", getEditForm(),
        "Edit Mode", getEditMode(),
        "Edit Save", getEditSave(),
        "Edit Show Id", getEditShowId(),
        "Edit Window", getEditWindow(),
        "Edit In Place", getEditInPlace(),
        "Enabled Actions", getEnabledActions(),
        "Disabled Actions", getDisabledActions(),
        "Min Column Width", getMinColumnWidth(),
        "Max Column Width", getMaxColumnWidth(),
        "Auto Fit", getAutoFit(),
        "Auto Flex", getAutoFlex(),
        "Favorite", getFavorite(),
        "Enable Copy", getEnableCopy(),
        "Min Number Of Rows", getMinNumberOfRows(),
        "Max Number Of Rows", getMaxNumberOfRows(),
        "Render Mode", getRenderMode(),
        "Row Change Sensitivity Millis", getRowChangeSensitivityMillis(),
        "Options", getOptions());

    int cnt;
    int i;

    if (getFlexibility() != null) {
      PropertyUtils.appendChildrenToExtended(info, "Flexibility", getFlexibility().getInfo());
    }

    if (!getProperties().isEmpty()) {
      cnt = getProperties().size();
      PropertyUtils.addExtended(info, "Properties", BeeUtils.bracket(cnt));
      i = 0;
      for (Map.Entry<String, String> entry : getProperties().entrySet()) {
        i++;
        PropertyUtils.addExtended(info, "Property " + BeeUtils.progress(i, cnt),
            entry.getKey(), entry.getValue());
      }
    }

    if (getStyleSheets() != null && !getStyleSheets().isEmpty()) {
      cnt = getStyleSheets().size();
      PropertyUtils.addExtended(info, "Style Sheets", BeeUtils.bracket(cnt));
      i = 0;
      for (Map.Entry<String, String> entry : getStyleSheets().entrySet()) {
        i++;
        PropertyUtils.addExtended(info, "Style Sheet " + BeeUtils.progress(i, cnt),
            entry.getKey(), entry.getValue());
      }
    }

    if (getWidgets() != null && !getWidgets().isEmpty()) {
      cnt = getWidgets().size();
      PropertyUtils.addExtended(info, "Widgets", BeeUtils.bracket(cnt));
      i = 0;
      for (String w : getWidgets()) {
        PropertyUtils.addExtended(info, "Widget", BeeUtils.progress(++i, cnt), w);
      }
    }

    if (getHeader() != null) {
      PropertyUtils.appendChildrenToExtended(info, "Header", getHeader().getInfo());
    }
    if (getBody() != null) {
      PropertyUtils.appendChildrenToExtended(info, "Body", getBody().getInfo());
    }
    if (getFooter() != null) {
      PropertyUtils.appendChildrenToExtended(info, "Footer", getFooter().getInfo());
    }

    if (getRowStyles() != null && !getRowStyles().isEmpty()) {
      cnt = getRowStyles().size();
      PropertyUtils.addExtended(info, "Row Styles", BeeUtils.bracket(cnt));
      i = 0;
      for (ConditionalStyleDeclaration cs : getRowStyles()) {
        i++;
        if (cs != null) {
          PropertyUtils.appendChildrenToExtended(info, "Row Style " + BeeUtils.progress(i, cnt),
              cs.getInfo());
        }
      }
    }

    if (getRowMessage() != null) {
      PropertyUtils.appendChildrenToExtended(info, "Row Message", getRowMessage().getInfo());
    }
    if (getRowEditable() != null) {
      PropertyUtils.appendChildrenToExtended(info, "Row Editable", getRowEditable().getInfo());
    }
    if (getRowValidation() != null) {
      PropertyUtils.appendChildrenToExtended(info, "Row Validation", getRowValidation().getInfo());
    }

    if (getEditMessage() != null) {
      PropertyUtils.appendChildrenToExtended(info, "Edit Message", getEditMessage().getInfo());
    }

    int cc = getColumnCount();
    PropertyUtils.addExtended(info, "Column Count", BeeUtils.bracket(cc));

    i = 0;
    for (ColumnDescription column : getColumns()) {
      PropertyUtils.appendChildrenToExtended(info,
          BeeUtils.joinWords("Column", BeeUtils.progress(++i, cc), column.getId()),
          column.getInfo());
    }

    if (!getPredefinedFilters().isEmpty()) {
      cnt = getPredefinedFilters().size();
      PropertyUtils.addExtended(info, "Predefined Filters", BeeUtils.bracket(cnt));

      i = 0;
      for (FilterDescription filterDescription : getPredefinedFilters()) {
        PropertyUtils.appendChildrenToExtended(info, "Filter " + BeeUtils.progress(++i, cnt),
            filterDescription.getInfo());
      }
    }

    return info;
  }

  public String getFavorite() {
    return favorite;
  }

  public Filter getFilter() {
    return filter;
  }

  public Flexibility getFlexibility() {
    return flexibility;
  }

  public GridComponentDescription getFooter() {
    return footer;
  }

  public GridComponentDescription getHeader() {
    return header;
  }

  public String getIdName() {
    return idName;
  }

  public Integer getInitialRowSetSize() {
    return initialRowSetSize;
  }

  public Integer getMaxColumnWidth() {
    return maxColumnWidth;
  }

  public Integer getMaxNumberOfRows() {
    return maxNumberOfRows;
  }

  public Integer getMinColumnWidth() {
    return minColumnWidth;
  }

  public Integer getMinNumberOfRows() {
    return minNumberOfRows;
  }

  public String getName() {
    return name;
  }

  public String getNewRowCaption() {
    return newRowCaption;
  }

  public String getNewRowColumns() {
    return newRowColumns;
  }

  public String getNewRowDefaults() {
    return newRowDefaults;
  }

  public String getNewRowForm() {
    return newRowForm;
  }

  public WindowType getNewRowWindow() {
    return newRowWindow;
  }

  public String getOptions() {
    return options;
  }

  public Order getOrder() {
    return order;
  }

  public Boolean getPaging() {
    return paging;
  }

  public String getParent() {
    return parent;
  }

  public List<FilterDescription> getPredefinedFilters() {
    return predefinedFilters;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public String getRenderMode() {
    return renderMode;
  }

  public Integer getRowChangeSensitivityMillis() {
    return rowChangeSensitivityMillis;
  }

  public Calculation getRowEditable() {
    return rowEditable;
  }

  public Calculation getRowMessage() {
    return rowMessage;
  }

  public Collection<ConditionalStyleDeclaration> getRowStyles() {
    return rowStyles;
  }

  public Calculation getRowValidation() {
    return rowValidation;
  }

  public Map<String, String> getStyleSheets() {
    return styleSheets;
  }

  public String getVersionName() {
    return versionName;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  public List<String> getWidgets() {
    return widgets;
  }

  public boolean hasColumn(String id) {
    for (ColumnDescription column : getColumns()) {
      if (column.is(id)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasColumnHeaders() {
    return BeeUtils.isEmpty(getHeaderMode())
        || BeeUtils.inListSame(getHeaderMode(), HEADER_MODE_ALL, HEADER_MODE_COLUMN);
  }

  public boolean hasFooters() {
    return BeeUtils.isEmpty(getFooterMode()) || !BeeUtils.same(getFooterMode(), FOOTER_MODE_NONE);
  }

  public boolean hasGridHeader() {
    return BeeUtils.isEmpty(getHeaderMode())
        || BeeUtils.inListSame(getHeaderMode(), HEADER_MODE_ALL, HEADER_MODE_GRID);
  }

  public boolean hasWidgets() {
    return getWidgets() != null && !getWidgets().isEmpty();
  }

  public boolean isEmpty() {
    return getColumnCount() <= 0;
  }

  public Boolean isReadOnly() {
    return readOnly;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case NAME:
          arr[i++] = getName();
          break;
        case VIEW:
          arr[i++] = getViewName();
          break;
        case CAPTION:
          arr[i++] = getCaption();
          break;
        case READONLY:
          arr[i++] = isReadOnly();
          break;
        case COLUMNS:
          arr[i++] = getColumns();
          break;
        case DATA_PROVIDER:
          arr[i++] = Codec.pack(getDataProvider());
          break;
        case BODY:
          arr[i++] = getBody();
          break;
        case FOOTER:
          arr[i++] = getFooter();
          break;
        case HEADER_MODE:
          arr[i++] = getHeaderMode();
          break;
        case FOOTER_MODE:
          arr[i++] = getFooterMode();
          break;
        case HEADER:
          arr[i++] = getHeader();
          break;
        case MAX_COLUMN_WIDTH:
          arr[i++] = getMaxColumnWidth();
          break;
        case MIN_COLUMN_WIDTH:
          arr[i++] = getMinColumnWidth();
          break;
        case NEW_ROW_COLUMNS:
          arr[i++] = getNewRowColumns();
          break;
        case NEW_ROW_DEFAULTS:
          arr[i++] = getNewRowDefaults();
          break;
        case NEW_ROW_CAPTION:
          arr[i++] = getNewRowCaption();
          break;
        case ROW_EDITABLE:
          arr[i++] = getRowEditable();
          break;
        case ROW_MESSAGE:
          arr[i++] = getRowMessage();
          break;
        case ROW_VALIDATION:
          arr[i++] = getRowValidation();
          break;
        case ROW_STYLES:
          arr[i++] = getRowStyles();
          break;
        case NEW_ROW_FORM:
          arr[i++] = getNewRowForm();
          break;
        case EDIT_FORM:
          arr[i++] = getEditForm();
          break;
        case EDIT_MODE:
          arr[i++] = getEditMode();
          break;
        case EDIT_SAVE:
          arr[i++] = getEditSave();
          break;
        case EDIT_MESSAGE:
          arr[i++] = getEditMessage();
          break;
        case EDIT_SHOW_ID:
          arr[i++] = getEditShowId();
          break;
        case INITIAL_ROW_SET_SIZE:
          arr[i++] = getInitialRowSetSize();
          break;
        case PAGING:
          arr[i++] = getPaging();
          break;
        case FILTER:
          arr[i++] = getFilter();
          break;
        case CURRENT_USER_FILTER:
          arr[i++] = getCurrentUserFilter();
          break;
        case ORDER:
          arr[i++] = getOrder();
          break;
        case ID_NAME:
          arr[i++] = getIdName();
          break;
        case VERSION_NAME:
          arr[i++] = getVersionName();
          break;
        case CACHE_DATA:
          arr[i++] = getCacheData();
          break;
        case CACHE_DESCRIPTION:
          arr[i++] = getCacheDescription();
          break;
        case ENABLED_ACTIONS:
          arr[i++] = getEnabledActions();
          break;
        case DISABLED_ACTIONS:
          arr[i++] = getDisabledActions();
          break;
        case STYLE_SHEETS:
          arr[i++] = getStyleSheets();
          break;
        case WIDGETS:
          arr[i++] = getWidgets();
          break;
        case NEW_ROW_WINDOW:
          arr[i++] = Codec.pack(getNewRowWindow());
          break;
        case EDIT_WINDOW:
          arr[i++] = Codec.pack(getEditWindow());
          break;
        case PARENT:
          arr[i++] = getParent();
          break;
        case AUTO_FIT:
          arr[i++] = getAutoFit();
          break;
        case AUTO_FLEX:
          arr[i++] = getAutoFlex();
          break;
        case FLEXIBILITY:
          arr[i++] = getFlexibility();
          break;
        case FAVORITE:
          arr[i++] = getFavorite();
          break;
        case ENABLE_COPY:
          arr[i++] = getEnableCopy();
          break;
        case MIN_NUMBER_OF_ROWS:
          arr[i++] = getMinNumberOfRows();
          break;
        case MAX_NUMBER_OF_ROWS:
          arr[i++] = getMaxNumberOfRows();
          break;
        case RENDER_MODE:
          arr[i++] = getRenderMode();
          break;
        case ROW_CHANGE_SENSITIVITY_MILLIS:
          arr[i++] = getRowChangeSensitivityMillis();
          break;
        case EDIT_IN_PLACE:
          arr[i++] = getEditInPlace();
          break;
        case PREDEFINED_FILTERS:
          arr[i++] = getPredefinedFilters();
          break;
        case OPTIONS:
          arr[i++] = getOptions();
          break;
        case PROPERTIES:
          arr[i++] = getProperties();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setAutoFit(String autoFit) {
    this.autoFit = autoFit;
  }

  public void setAutoFlex(Boolean autoFlex) {
    this.autoFlex = autoFlex;
  }

  public void setBody(GridComponentDescription body) {
    this.body = body;
  }

  public void setCacheData(Boolean cacheData) {
    this.cacheData = cacheData;
  }

  public void setCacheDescription(Boolean cacheDescription) {
    this.cacheDescription = cacheDescription;
  }

  public void setCaption(String caption) {
    this.caption = caption;
  }

  public void setCurrentUserFilter(String currentUserFilter) {
    this.currentUserFilter = currentUserFilter;
  }

  public void setDataProvider(ProviderType dataProvider) {
    this.dataProvider = dataProvider;
  }

  public void setDefaults() {
    setCacheData(true);
    setCacheDescription(true);
  }

  public void setDisabledActions(Set<Action> disabledActions) {
    BeeUtils.overwrite(this.disabledActions, disabledActions);
  }

  public void setEditForm(String editForm) {
    this.editForm = editForm;
  }

  public void setEditInPlace(Boolean editInPlace) {
    this.editInPlace = editInPlace;
  }

  public void setEditMessage(Calculation editMessage) {
    this.editMessage = editMessage;
  }

  public void setEditMode(Boolean editMode) {
    this.editMode = editMode;
  }

  public void setEditSave(Boolean editSave) {
    this.editSave = editSave;
  }

  public void setEditShowId(Boolean editShowId) {
    this.editShowId = editShowId;
  }

  public void setEditWindow(WindowType editWindow) {
    this.editWindow = editWindow;
  }

  public void setEnableCopy(String enableCopy) {
    this.enableCopy = enableCopy;
  }

  public void setEnabledActions(Set<Action> enabledActions) {
    BeeUtils.overwrite(this.enabledActions, enabledActions);
  }

  public void setFavorite(String favorite) {
    this.favorite = favorite;
  }

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  public void setFlexibility(Flexibility flexibility) {
    this.flexibility = flexibility;
  }

  public void setFooter(GridComponentDescription footer) {
    this.footer = footer;
  }

  public void setFooterMode(String footerMode) {
    this.footerMode = footerMode;
  }

  public void setHeader(GridComponentDescription header) {
    this.header = header;
  }

  public void setHeaderMode(String headerMode) {
    this.headerMode = headerMode;
  }

  public void setInitialRowSetSize(Integer initialRowSetSize) {
    this.initialRowSetSize = initialRowSetSize;
  }

  public void setMaxColumnWidth(Integer maxColumnWidth) {
    this.maxColumnWidth = maxColumnWidth;
  }

  public void setMaxNumberOfRows(Integer maxNumberOfRows) {
    this.maxNumberOfRows = maxNumberOfRows;
  }

  public void setMinColumnWidth(Integer minColumnWidth) {
    this.minColumnWidth = minColumnWidth;
  }

  public void setMinNumberOfRows(Integer minNumberOfRows) {
    this.minNumberOfRows = minNumberOfRows;
  }

  public void setNewRowCaption(String newRowCaption) {
    this.newRowCaption = newRowCaption;
  }

  public void setNewRowColumns(String newRowColumns) {
    this.newRowColumns = newRowColumns;
  }

  public void setNewRowDefaults(String newRowDefaults) {
    this.newRowDefaults = newRowDefaults;
  }

  public void setNewRowForm(String newRowForm) {
    this.newRowForm = newRowForm;
  }

  public void setNewRowWindow(WindowType newRowWindow) {
    this.newRowWindow = newRowWindow;
  }

  public void setOptions(String options) {
    this.options = options;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public void setPaging(Boolean paging) {
    this.paging = paging;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public void setPredefinedFilters(List<FilterDescription> predefinedFilters) {
    BeeUtils.overwrite(this.predefinedFilters, predefinedFilters);
  }

  public void setProperties(Map<String, String> properties) {
    BeeUtils.overwrite(this.properties, properties);
  }

  public void setReadOnly(Boolean readOnly) {
    this.readOnly = readOnly;
  }

  public void setRenderMode(String renderMode) {
    this.renderMode = renderMode;
  }

  public void setRowChangeSensitivityMillis(Integer rowChangeSensitivityMillis) {
    this.rowChangeSensitivityMillis = rowChangeSensitivityMillis;
  }

  public void setRowEditable(Calculation rowEditable) {
    this.rowEditable = rowEditable;
  }

  public void setRowMessage(Calculation rowMessage) {
    this.rowMessage = rowMessage;
  }

  public void setRowStyles(Collection<ConditionalStyleDeclaration> rowStyles) {
    this.rowStyles = rowStyles;
  }

  public void setRowValidation(Calculation rowValidation) {
    this.rowValidation = rowValidation;
  }

  public void setStyleSheets(Map<String, String> styleSheets) {
    this.styleSheets = styleSheets;
  }

  public void setWidgets(List<String> widgets) {
    BeeUtils.overwrite(this.widgets, widgets);
  }

  private String getFooterMode() {
    return footerMode;
  }

  private String getHeaderMode() {
    return headerMode;
  }

  private void setIdName(String idName) {
    this.idName = idName;
  }

  private void setName(String name) {
    this.name = name;
  }

  private void setVersionName(String versionName) {
    this.versionName = versionName;
  }

  private void setViewName(String viewName) {
    this.viewName = viewName;
  }
}
