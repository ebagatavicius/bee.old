package com.butent.bee.shared.ui;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.menu.MenuConstants;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Relation implements BeeSerializable, HasInfo, HasViewName {

  public enum Caching {
    NONE, QUERY, LOCAL, GLOBAL
  }

  public enum RenderMode {
    SOURCE, TARGET
  }

  private enum Serial {
    ATTRIBUTES, SELECTOR_COLUMNS, ROW_RENDERER_DESCR, ROW_RENDER, ROW_RENDER_TOKENS,
    VIEW_NAME, CHOICE_COLUMNS, SEARCHABLE_COLUMNS
  }

  public static final String TAG_ROW_RENDERER = "rowRenderer";
  public static final String TAG_ROW_RENDER = "rowRender";
  public static final String TAG_ROW_RENDER_TOKEN = "rowRenderToken";

  public static final String TAG_SELECTOR_COLUMN = "selectorColumn";

  public static final String ATTR_CACHING = "caching";
  public static final String ATTR_OPERATOR = "operator";

  public static final String ATTR_CHOICE_COLUMNS = "choiceColumns";
  public static final String ATTR_SEARCHABLE_COLUMNS = "searchableColumns";

  public static final String ATTR_SELECTOR_CLASS = "selectorClass";
  public static final String ATTR_ITEM_TYPE = "itemType";

  public static final String ATTR_MIN_QUERY_LENGTH = "minQueryLength";
  public static final String ATTR_INSTANT = "instant";

  public static final String ATTR_VALUE_SOURCE = "valueSource";
  public static final String ATTR_STRICT = "strict";

  private static final Operator DEFAULT_OPERATOR = Operator.CONTAINS;

  public static Relation create() {
    return new Relation();
  }

  public static Relation create(Map<String, String> attributes,
      List<SelectorColumn> selectorColumns, RendererDescription rowRendererDescription,
      Calculation rowRender, List<RenderableToken> rowRenderTokens) {

    Relation relation = new Relation();
    relation.setAttributes(attributes);

    if (!BeeUtils.isEmpty(selectorColumns)) {
      relation.setSelectorColumns(selectorColumns);
    }

    if (rowRendererDescription != null) {
      relation.setRowRendererDescription(rowRendererDescription);
    }
    if (rowRender != null) {
      relation.setRowRender(rowRender);
    }
    if (!BeeUtils.isEmpty(rowRenderTokens)) {
      relation.setRowRenderTokens(rowRenderTokens);
    }

    return relation;
  }

  public static Relation create(String viewName, List<String> columns) {
    Relation relation = new Relation();

    if (!BeeUtils.isEmpty(viewName)) {
      relation.setViewName(viewName);
    }
    if (!BeeUtils.isEmpty(columns)) {
      relation.getChoiceColumns().addAll(columns);
      relation.getSearchableColumns().addAll(columns);
    }

    return relation;
  }

  public static Relation restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    Relation relation = new Relation();
    relation.deserialize(s);
    return relation;
  }

  private static String deduceViewName(DataInfo dataInfo, List<String> columns) {
    if (!BeeUtils.isEmpty(columns)) {
      for (String colName : columns) {
        String result = deduceViewName(dataInfo, colName);
        if (!BeeUtils.isEmpty(result)) {
          return result;
        }
      }
    }
    return null;
  }

  private static String deduceViewName(DataInfo dataInfo, String colName) {
    return dataInfo.getEditableRelationView(colName);
  }

  private static String deduceViewName(DataInfo.Provider provider, DataInfo targetInfo,
      String target, List<String> renderColumns, List<String> displCols, List<String> searchCols,
      List<String> selectorColumnNames) {

    String result = null;

    if (BeeUtils.isEmpty(target) && BeeUtils.isEmpty(renderColumns) && BeeUtils.isEmpty(displCols)
        && BeeUtils.isEmpty(searchCols) && BeeUtils.isEmpty(selectorColumnNames)) {
      List<String> columnNames = targetInfo.getColumnNames(false);
      for (String colName : columnNames) {
        result = deduceViewName(targetInfo, colName);
        if (!BeeUtils.isEmpty(result)) {
          break;
        }
      }
      return result;
    }

    if (!BeeUtils.isEmpty(target)) {
      result = deduceViewName(targetInfo, target);
      return result;
    }

    result = deduceViewName(targetInfo, renderColumns);
    if (!BeeUtils.isEmpty(result)) {
      return result;
    }

    Set<String> columns = BeeUtils.union(displCols, searchCols, selectorColumnNames);
    if (columns.isEmpty()) {
      return result;
    }

    List<String> tables = targetInfo.getRelatedTables();
    if (tables.isEmpty()) {
      return result;
    }

    for (String table : tables) {
      DataInfo tableInfo = provider.getDataInfo(table, false);
      if (tableInfo != null && tableInfo.containsAllViewColumns(columns)) {
        result = table;
        break;
      }
    }
    return result;
  }

  private static List<String> deriveRenderColumns(DataInfo targetInfo, String original,
      String resolved) {

    if (!BeeUtils.same(original, resolved) && targetInfo.containsColumn(original)) {
      ViewColumn vc = targetInfo.getViewColumn(original);
      if (vc != null && vc.getLevel() > 0) {
        return Lists.newArrayList(original);
      }
    }

    return RelationUtils.getRenderColumns(targetInfo, resolved);
  }

  private static List<String> getDefaultColumnNames(DataInfo dataInfo) {
    List<String> result = new ArrayList<>();

    for (BeeColumn column : dataInfo.getColumns()) {
      if (ValueType.TEXT.equals(column.getType()) && !column.isText()) {
        result.add(column.getId());
      }
    }

    return result.isEmpty() ? DataUtils.getColumnNames(dataInfo.getColumns()) : result;
  }

  private static String resolveTarget(DataInfo dataInfo, String colName) {
    return dataInfo.getEditableRelationSource(colName);
  }

  private static String resolveTarget(DataInfo targetInfo, String target,
      List<String> renderColumns) {

    String result = null;

    if (!BeeUtils.isEmpty(target)) {
      result = resolveTarget(targetInfo, target);
      if (!BeeUtils.isEmpty(result)) {
        return result;
      }
    }

    if (!BeeUtils.isEmpty(renderColumns)) {
      for (String colName : renderColumns) {
        result = resolveTarget(targetInfo, colName);
        if (!BeeUtils.isEmpty(result)) {
          break;
        }
      }
    }
    return result;
  }

  private final Map<String, String> attributes = new HashMap<>();

  private String viewName;

  private Filter filter;
  private String currentUserFilter;
  private Order order;

  private Caching caching;
  private Operator operator;

  private RendererDescription rowRendererDescription;
  private Calculation rowRender;
  private List<RenderableToken> rowRenderTokens;

  private final List<SelectorColumn> selectorColumns = new ArrayList<>();

  private final List<String> choiceColumns = new ArrayList<>();
  private final List<String> searchableColumns = new ArrayList<>();

  private String selectorClass;

  private MenuConstants.ItemType itemType;

  private Integer visibleLines;

  private Integer minQueryLength;
  private Boolean instant;

  private String originalTarget;

  private final List<String> originalRenderColumns = new ArrayList<>();

  private RenderMode renderMode;

  private String targetViewName;

  private String valueSource;
  private Boolean strict;

  private Relation() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (member) {
        case ATTRIBUTES:
          String[] attr = Codec.beeDeserializeCollection(value);
          if (!ArrayUtils.isEmpty(attr)) {
            getAttributes().clear();
            for (int j = 0; j < attr.length; j += 2) {
              getAttributes().put(attr[j], attr[j + 1]);
            }
          }
          break;

        case SELECTOR_COLUMNS:
          String[] cols = Codec.beeDeserializeCollection(value);
          if (!ArrayUtils.isEmpty(cols)) {
            getSelectorColumns().clear();
            for (int j = 0; j < cols.length; j++) {
              BeeUtils.addNotNull(getSelectorColumns(), SelectorColumn.restore(cols[j]));
            }
          }
          break;

        case ROW_RENDERER_DESCR:
          setRowRendererDescription(RendererDescription.restore(value));
          break;

        case ROW_RENDER:
          setRowRender(Calculation.restore(value));
          break;

        case ROW_RENDER_TOKENS:
          setRowRenderTokens(RenderableToken.restoreList(value));
          break;

        case CHOICE_COLUMNS:
          setChoiceColumns(Lists.newArrayList(Codec.beeDeserializeCollection(value)));
          break;

        case SEARCHABLE_COLUMNS:
          setSearchableColumns(Lists.newArrayList(Codec.beeDeserializeCollection(value)));
          break;

        case VIEW_NAME:
          setViewName(value);
          break;
      }
    }
  }

  public void disableEdit() {
    getAttributes().put(UiConstants.ATTR_EDIT_ENABLED, BeeConst.STRING_FALSE);
  }

  public void disableNewRow() {
    getAttributes().put(UiConstants.ATTR_NEW_ROW_ENABLED, BeeConst.STRING_FALSE);
  }

  public void enableEdit() {
    getAttributes().put(UiConstants.ATTR_EDIT_ENABLED, BeeConst.STRING_TRUE);
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public Caching getCaching() {
    return caching;
  }

  public List<String> getChoiceColumns() {
    return choiceColumns;
  }

  public String getCurrentUserFilter() {
    return currentUserFilter;
  }

  public String getEditForm() {
    return getAttribute(UiConstants.ATTR_EDIT_FORM);
  }

  public Integer getEditKey() {
    return BeeUtils.toIntOrNull(getAttribute(UiConstants.ATTR_EDIT_KEY));
  }

  public String getEditSource() {
    return getAttribute(UiConstants.ATTR_EDIT_SOURCE);
  }

  public String getEditViewName() {
    return getAttribute(UiConstants.ATTR_EDIT_VIEW_NAME);
  }

  public Filter getFilter() {
    return filter;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties(getAttributes());
    PropertyUtils.addProperties(info,
        "View Name", getViewName(),
        "Filter", getFilter(),
        "Current User Filter", getCurrentUserFilter(),
        "Order", getOrder(),
        "Caching", getCaching(),
        "Operator", getOperator(),
        "Selector Class", getSelectorClass(),
        "Item Type", getItemType(),
        "Visible Lines", getVisibleLines(),
        "Min Query Length", getMinQueryLength(),
        "Instant", getInstant(),
        "Render Mode", getRenderMode(),
        "Target View Name", getTargetViewName(),
        "Value Source", getValueSource(),
        "Strict", getStrict());

    if (!getChoiceColumns().isEmpty()) {
      PropertyUtils.addProperties(info, "Choice Columns", getChoiceColumns());
    }
    if (!getSearchableColumns().isEmpty()) {
      PropertyUtils.addProperties(info, "Searchable Columns", getSearchableColumns());
    }

    if (!getSelectorColumns().isEmpty()) {
      PropertyUtils.appendWithIndex(info, "Selector Columns", null, getSelectorColumns());
    }

    if (getRowRendererDescription() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Row Renderer",
          getRowRendererDescription().getInfo());
    }
    if (getRowRender() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Row Render", getRowRender().getInfo());
    }
    if (getRowRenderTokens() != null) {
      PropertyUtils.appendWithIndex(info, "Row Render Tokens", "token", getRowRenderTokens());
    }

    return info;
  }

  public Boolean getInstant() {
    return instant;
  }

  public MenuConstants.ItemType getItemType() {
    return itemType;
  }

  public String getLabel() {
    String label = getAttribute(UiConstants.ATTR_LABEL);
    if (BeeUtils.isEmpty(label)) {
      label = getAttribute(UiConstants.ATTR_CAPTION);
    }
    return label;
  }

  public Integer getMinQueryLength() {
    return minQueryLength;
  }

  public String getNewRowCaption() {
    return getAttribute(UiConstants.ATTR_NEW_ROW_CAPTION);
  }

  public String getNewRowColumns() {
    return getAttribute(UiConstants.ATTR_NEW_ROW_COLUMNS);
  }

  public String getNewRowForm() {
    return getAttribute(UiConstants.ATTR_NEW_ROW_FORM);
  }

  public Operator getOperator() {
    return operator;
  }

  public Order getOrder() {
    return order;
  }

  public List<String> getOriginalRenderColumns() {
    return Lists.newArrayList(originalRenderColumns);
  }

  public String getOriginalTarget() {
    return originalTarget;
  }

  public Calculation getRowRender() {
    return rowRender;
  }

  public RendererDescription getRowRendererDescription() {
    return rowRendererDescription;
  }

  public List<RenderableToken> getRowRenderTokens() {
    return rowRenderTokens;
  }

  public List<String> getSearchableColumns() {
    return searchableColumns;
  }

  public String getSelectorClass() {
    return selectorClass;
  }

  public List<SelectorColumn> getSelectorColumns() {
    return selectorColumns;
  }

  public Boolean getStrict() {
    return strict;
  }

  public String getTargetViewName() {
    return targetViewName;
  }

  public String getValueSource() {
    return valueSource;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  public Integer getVisibleLines() {
    return visibleLines;
  }

  public boolean hasRowRenderer() {
    return getRowRendererDescription() != null || getRowRender() != null
        || !BeeUtils.isEmpty(getRowRenderTokens());
  }

  public void initialize(DataInfo.Provider provider, String targetView, Holder<String> target,
      Holder<List<String>> renderColumns, RenderMode mode, Long userId) {

    setTargetViewName(targetView);

    setOriginalTarget(target.get());
    setOriginalRenderColumns(renderColumns.get());

    if (mode != null) {
      setRenderMode(mode);
    }

    String sourceView = getAttribute(UiConstants.ATTR_VIEW_NAME);
    if (!BeeUtils.isEmpty(sourceView)) {
      setViewName(sourceView);
    }

    String cache = getAttribute(ATTR_CACHING);
    if (!BeeUtils.isEmpty(cache)) {
      setCaching(EnumUtils.getEnumByName(Caching.class, cache));
    }
    String op = getAttribute(ATTR_OPERATOR);
    if (!BeeUtils.isEmpty(op)) {
      setOperator(EnumUtils.getEnumByName(Operator.class, op));
    }

    String sc = getAttribute(ATTR_SELECTOR_CLASS);
    if (!BeeUtils.isEmpty(sc)) {
      setSelectorClass(sc);
    }
    String it = getAttribute(ATTR_ITEM_TYPE);
    if (!BeeUtils.isEmpty(it)) {
      setItemType(EnumUtils.getEnumByName(MenuConstants.ItemType.class, it));
    }
    String lines = getAttribute(HasVisibleLines.ATTR_VISIBLE_LINES);
    if (BeeUtils.isPositiveInt(lines)) {
      setVisibleLines(BeeUtils.toInt(lines));
    }

    String minQuery = getAttribute(ATTR_MIN_QUERY_LENGTH);
    if (BeeUtils.isPositiveInt(minQuery)) {
      setMinQueryLength(BeeUtils.toInt(minQuery));
    }
    String instantSearch = getAttribute(ATTR_INSTANT);
    if (instantSearch != null) {
      setInstant(BeeUtils.toBooleanOrNull(instantSearch));
    }

    String valSrc = getAttribute(ATTR_VALUE_SOURCE);
    if (!BeeUtils.isEmpty(valSrc)) {
      setValueSource(valSrc);
    }
    String strictRel = getAttribute(ATTR_STRICT);
    if (strictRel != null) {
      setStrict(BeeUtils.toBooleanOrNull(strictRel));
    }

    String flt = getAttribute(UiConstants.ATTR_FILTER);
    String cuf = getAttribute(UiConstants.ATTR_CURRENT_USER_FILTER);
    String ord = getAttribute(UiConstants.ATTR_ORDER);

    List<String> displCols = NameUtils.toList(getAttribute(ATTR_CHOICE_COLUMNS));
    List<String> searchCols = NameUtils.toList(getAttribute(ATTR_SEARCHABLE_COLUMNS));

    List<String> selectorColumnNames = getSelectorColumnNames();

    DataInfo targetInfo = BeeUtils.isEmpty(targetView)
        ? null : provider.getDataInfo(targetView, true);

    if (BeeUtils.isEmpty(getViewName()) && targetInfo != null) {
      sourceView = deduceViewName(provider, targetInfo, target.get(), renderColumns.get(),
          displCols, searchCols, selectorColumnNames);
      if (!BeeUtils.isEmpty(sourceView)) {
        setViewName(sourceView);
      }
    }

    if (targetInfo != null) {
      String rt;
      if (BeeUtils.anyEmpty(valSrc, target.get())) {
        rt = resolveTarget(targetInfo, target.get(), renderColumns.get());
      } else {
        rt = target.get();
      }

      if (BeeUtils.isEmpty(renderColumns.get()) && renderTarget()) {
        renderColumns.set(deriveRenderColumns(targetInfo, target.get(), rt));
      }
      if (!BeeUtils.isEmpty(rt)) {
        target.set(rt);
      }
    }

    DataInfo sourceInfo = BeeUtils.isEmpty(getViewName())
        ? null : provider.getDataInfo(getViewName(), true);

    if (sourceInfo != null) {
      if (!BeeUtils.isEmpty(flt)) {
        setFilter(sourceInfo.parseFilter(flt, userId));
      }
      if (!BeeUtils.isEmpty(cuf)) {
        setCurrentUserFilter(cuf);
      }
      if (!BeeUtils.isEmpty(ord)) {
        setOrder(sourceInfo.parseOrder(ord));
      }

      if (BeeUtils.isEmpty(renderColumns.get()) && sourceInfo.containsColumn(valSrc)
          && renderSource()) {
        renderColumns.set(Lists.newArrayList(valSrc));
      }
    }

    if (!BeeUtils.isEmpty(displCols)) {
      setChoiceColumns(displCols);
    }

    if (!BeeUtils.isEmpty(searchCols)) {
      if (sourceInfo == null) {
        setSearchableColumns(searchCols);
      } else {
        setSearchableColumns(sourceInfo.parseColumns(searchCols));
      }
    }

    if (getChoiceColumns().isEmpty() && !selectorColumnNames.isEmpty()) {
      if (sourceInfo == null) {
        setChoiceColumns(selectorColumnNames);
      } else {
        setChoiceColumns(sourceInfo.parseColumns(selectorColumnNames));
      }
    }

    if (BeeUtils.isEmpty(renderColumns.get()) && renderSource()) {
      if (!getChoiceColumns().isEmpty()) {
        renderColumns.set(getChoiceColumns());
      } else if (!getSearchableColumns().isEmpty()) {
        renderColumns.set(getSearchableColumns());
      } else if (sourceInfo != null) {
        renderColumns.set(getDefaultColumnNames(sourceInfo));
      }
    }

    if (getChoiceColumns().isEmpty() && getSearchableColumns().isEmpty()) {
      List<String> colNames = new ArrayList<>();

      if (!BeeUtils.isEmpty(valSrc)) {
        colNames.add(valSrc);

      } else if (!BeeUtils.isEmpty(renderColumns.get())) {
        if (sourceInfo != null && targetInfo != null && renderTarget()) {
          int tcLevel = Math.max(targetInfo.getViewColumnLevel(target.get()), 0);

          List<String> fields = new ArrayList<>();
          for (String columnId : renderColumns.get()) {
            ViewColumn vc = targetInfo.getViewColumn(columnId);

            if (vc != null) {
              int index = sourceInfo.getColumnIndexBySource(vc.getTable(), vc.getField(),
                  vc.getLevel() + tcLevel - 1);
              if (!BeeConst.isUndef(index)) {
                fields.add(sourceInfo.getColumnId(index));
              }
            }
          }
          if (!fields.isEmpty()) {
            colNames.addAll(sourceInfo.parseColumns(fields));
          }

        } else if (renderSource()) {
          colNames.addAll(renderColumns.get());
        }
      }

      if (sourceInfo != null && colNames.isEmpty()) {
        colNames.addAll(getDefaultColumnNames(sourceInfo));
      }

      if (!colNames.isEmpty()) {
        setChoiceColumns(colNames);
        setSearchableColumns(colNames);
      }

    } else if (getChoiceColumns().isEmpty()) {
      setChoiceColumns(getSearchableColumns());
    } else if (getSearchableColumns().isEmpty()) {
      setSearchableColumns(getChoiceColumns());
    }
  }

  public boolean isEditEnabled(boolean defEnabled) {
    if (!BeeUtils.isEmpty(getValueSource())) {
      return false;

    } else if (defEnabled) {
      return !BeeConst.isFalse(getAttribute(UiConstants.ATTR_EDIT_ENABLED));

    } else {
      String enabled = getAttribute(UiConstants.ATTR_EDIT_ENABLED);
      return BeeConst.isTrue(enabled)
          || BeeUtils.isEmpty(enabled) && getAttributes().containsKey(UiConstants.ATTR_EDIT_WINDOW);
    }
  }

  public WindowType getEditWindowType() {
    return WindowType.parse(getAttribute(UiConstants.ATTR_EDIT_WINDOW));
  }

  public boolean isNewRowEnabled() {
    return BeeUtils.isEmpty(getValueSource())
        && !BeeConst.isFalse(getAttribute(UiConstants.ATTR_NEW_ROW_ENABLED));
  }

  public Operator nvlOperator() {
    return BeeUtils.nvl(getOperator(), DEFAULT_OPERATOR);
  }

  public boolean renderSource() {
    return RenderMode.SOURCE.equals(getRenderMode());
  }

  public boolean renderTarget() {
    return RenderMode.TARGET.equals(getRenderMode());
  }

  public void replaceTargetColumn(String oldId, String newId) {
    if (!BeeUtils.isEmpty(oldId) && !BeeUtils.isEmpty(newId)
        && !BeeUtils.equalsTrim(oldId, newId)) {

      if (getRowRender() != null) {
        getRowRender().replaceColumn(oldId, newId);
      }
      if (!BeeUtils.isEmpty(getRowRenderTokens())) {
        for (RenderableToken token : getRowRenderTokens()) {
          token.replaceSource(oldId, newId);
        }
      }

      if (BeeUtils.same(getOriginalTarget(), oldId)) {
        setOriginalTarget(newId.trim());
      }
      if (BeeUtils.containsSame(getOriginalRenderColumns(), oldId)) {
        BeeUtils.overwrite(getOriginalRenderColumns(),
            NameUtils.rename(getOriginalRenderColumns(), oldId, newId));
      }
    }
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ATTRIBUTES:
          arr[i++] = getAttributes();
          break;
        case SELECTOR_COLUMNS:
          arr[i++] = getSelectorColumns();
          break;
        case ROW_RENDERER_DESCR:
          arr[i++] = getRowRendererDescription();
          break;
        case ROW_RENDER:
          arr[i++] = getRowRender();
          break;
        case ROW_RENDER_TOKENS:
          arr[i++] = getRowRenderTokens();
          break;
        case CHOICE_COLUMNS:
          arr[i++] = getChoiceColumns();
          break;
        case SEARCHABLE_COLUMNS:
          arr[i++] = getSearchableColumns();
          break;
        case VIEW_NAME:
          arr[i++] = getViewName();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setAttributes(Map<String, String> attributes) {
    getAttributes().clear();
    if (BeeUtils.isEmpty(attributes)) {
      return;
    }
    getAttributes().putAll(attributes);
  }

  public void setCaching(Caching caching) {
    this.caching = caching;
  }

  public void setChoiceColumns(List<String> choiceColumns) {
    getChoiceColumns().clear();
    if (!BeeUtils.isEmpty(choiceColumns)) {
      getChoiceColumns().addAll(choiceColumns);
    }
  }

  public void setCurrentUserFilter(String currentUserFilter) {
    this.currentUserFilter = currentUserFilter;
  }

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  public void setInstant(Boolean instant) {
    this.instant = instant;
  }

  public void setItemType(MenuConstants.ItemType itemType) {
    this.itemType = itemType;
  }

  public void setMinQueryLength(Integer minQueryLength) {
    this.minQueryLength = minQueryLength;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public void setRowRender(Calculation rowRender) {
    this.rowRender = rowRender;
  }

  public void setRowRendererDescription(RendererDescription rowRendererDescription) {
    this.rowRendererDescription = rowRendererDescription;
  }

  public void setRowRenderTokens(List<RenderableToken> rowRenderTokens) {
    this.rowRenderTokens = rowRenderTokens;
  }

  public void setSearchableColumns(List<String> searchableColumns) {
    getSearchableColumns().clear();
    if (!BeeUtils.isEmpty(searchableColumns)) {
      getSearchableColumns().addAll(searchableColumns);
    }
  }

  public void setSelectorClass(String selectorClass) {
    this.selectorClass = selectorClass;
  }

  public void setSelectorColumns(List<SelectorColumn> selectorColumns) {
    getSelectorColumns().clear();
    if (selectorColumns != null) {
      getSelectorColumns().addAll(selectorColumns);
    }
  }

  public void setStrict(Boolean strict) {
    this.strict = strict;
  }

  public void setValueSource(String valueSource) {
    this.valueSource = valueSource;
  }

  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  public void setVisibleLines(Integer visibleLines) {
    this.visibleLines = visibleLines;
  }

  private String getAttribute(String name) {
    return getAttributes().get(name);
  }

  private RenderMode getRenderMode() {
    return renderMode;
  }

  private List<String> getSelectorColumnNames() {
    List<String> result = new ArrayList<>();
    for (SelectorColumn selectorColumn : getSelectorColumns()) {
      BeeUtils.addNotEmpty(result, selectorColumn.getSource());
    }
    return result;
  }

  private void setOriginalRenderColumns(List<String> originalRenderColumns) {
    if (!this.originalRenderColumns.isEmpty()) {
      this.originalRenderColumns.clear();
    }
    if (!BeeUtils.isEmpty(originalRenderColumns)) {
      this.originalRenderColumns.addAll(originalRenderColumns);
    }
  }

  private void setOriginalTarget(String originalTarget) {
    this.originalTarget = originalTarget;
  }

  private void setRenderMode(RenderMode renderMode) {
    this.renderMode = renderMode;
  }

  private void setTargetViewName(String targetViewName) {
    this.targetViewName = targetViewName;
  }
}
