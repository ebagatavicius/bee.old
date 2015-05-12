package com.butent.bee.shared.ui;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasBounds;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.data.value.HasValueType;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains column properties and methods for managing them.
 */

public class ColumnDescription implements BeeSerializable, HasInfo, HasOptions, HasValueType,
    HasBounds {

  public enum ColType {
    DATA("DataColumn", false),
    RELATED("RelColumn", false),
    AUTO("AutoColumn", false),
    CALCULATED("CalcColumn", true),
    ID("IdColumn", true),
    VERSION("VerColumn", true),
    SELECTION("SelectionColumn", true),
    ACTION("ActionColumn", true),
    PROPERTY("PropColumn", true),
    RIGHTS(null, true);

    public static ColType getColType(String tagName) {
      if (!BeeUtils.isEmpty(tagName)) {
        for (ColType type : ColType.values()) {
          if (BeeUtils.same(type.getTagName(), tagName)) {
            return type;
          }
        }
      }
      return null;
    }

    private final String tagName;
    private final boolean readOnly;

    private ColType(String tagName, boolean readOnly) {
      this.tagName = tagName;
      this.readOnly = readOnly;
    }

    public String getTagName() {
      return tagName;
    }

    public boolean isReadOnly() {
      return readOnly;
    }
  }

  /**
   * Contains a list of serializable members of column object.
   */

  private enum Serial {
    COL_TYPE, ID, CAPTION, LABEL, READ_ONLY, WIDTH, SOURCE, PROPERTY, RELATION,
    MIN_WIDTH, MAX_WIDTH, SORTABLE, VISIBLE, FORMAT, HOR_ALIGN, VERT_ALIGN, WHITE_SPACE,
    VALIDATION, EDITABLE, CARRY_CALC, CARRY_ON, EDITOR, MIN_VALUE, MAX_VALUE, REQUIRED, ENUM_KEY,
    RENDERER_DESCR, RENDER, RENDER_TOKENS, VALUE_TYPE, PRECISION, SCALE, RENDER_COLUMNS,
    SEARCH_BY, FILTER_SUPPLIER, FILTER_OPTIONS, SORT_BY,
    HEADER_STYLE, BODY_STYLE, FOOTER_STYLE, DYN_STYLES, CELL_TYPE, CELL_RESIZABLE, UPDATE_MODE,
    AUTO_FIT, FLEXIBILITY, OPTIONS, ELEMENT_TYPE, FOOTER_DESCRIPTION, DYNAMIC,
    EXPORTABLE, EXPORT_WIDTH_FACTOR, EDIT_IN_PLACE, DRAGGABLE
  }

  public static final String TBL_COLUMN_SETTINGS = "GridColumnSettings";
  public static final String VIEW_COLUMN_SETTINGS = "GridColumnSettings";
  public static final String COL_GRID_SETTING = "GridSetting";

  private static boolean omniView;

  public static ColumnDescription restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    ColumnDescription column = new ColumnDescription();
    column.deserialize(s);
    return column;
  }

  public static boolean toggleOmniView() {
    omniView = !omniView;
    return omniView;
  }

  private ColType colType;

  private String id;
  private String caption;
  private String label;
  private Boolean readOnly;
  private Integer width;

  private String source;
  private String property;
  private Relation relation;

  private Integer minWidth;
  private Integer maxWidth;
  private String autoFit;
  private Flexibility flexibility;

  private Boolean sortable;

  private Boolean visible;

  private String format;
  private String horAlign;
  private String vertAlign;
  private String whiteSpace;

  private Calculation validation;
  private Calculation editable;

  private Calculation carryCalc;
  private Boolean carryOn;

  private RendererDescription rendererDescription;
  private Calculation render;
  private List<RenderableToken> renderTokens;

  private EditorDescription editor;

  private String enumKey;

  private String minValue;
  private String maxValue;

  private Boolean required;

  private ValueType valueType;
  private Integer precision;
  private Integer scale;

  private List<String> renderColumns;

  private String sortBy;
  private String searchBy;

  private FilterSupplierType filterSupplierType;
  private String filterOptions;

  private StyleDeclaration headerStyle;
  private StyleDeclaration bodyStyle;
  private StyleDeclaration footerStyle;

  private Collection<ConditionalStyleDeclaration> dynStyles;

  private CellType cellType;
  private Boolean cellResizable;

  private RefreshType updateMode;
  private String elementType;

  private FooterDescription footerDescription;

  private String options;

  private Boolean dynamic;

  private Boolean exportable;
  private Double exportWidthFactor;

  private Boolean editInPlace;
  private Boolean draggable;

  private boolean relationInitialized;

  public ColumnDescription(ColType colType, String id) {
    Assert.notNull(colType);
    Assert.notEmpty(id);

    this.colType = colType;
    this.id = id;
  }

  private ColumnDescription() {
  }

  public ColumnDescription copy() {
    return restore(serialize());
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
        case COL_TYPE:
          setColType(ColType.getColType(value));
          break;
        case ID:
          setId(value);
          break;
        case CAPTION:
          setCaption(value);
          break;
        case LABEL:
          setLabel(value);
          break;
        case READ_ONLY:
          setReadOnly(BeeUtils.toBooleanOrNull(value));
          break;
        case WIDTH:
          setWidth(BeeUtils.toIntOrNull(value));
          break;
        case SOURCE:
          setSource(value);
          break;
        case PROPERTY:
          setProperty(value);
          break;
        case RELATION:
          setRelation(Relation.restore(value));
          break;
        case VALUE_TYPE:
          setValueType(ValueType.getByTypeCode(value));
          break;
        case PRECISION:
          setPrecision(BeeUtils.toIntOrNull(value));
          break;
        case SCALE:
          setScale(BeeUtils.toIntOrNull(value));
          break;
        case SEARCH_BY:
          setSearchBy(value);
          break;
        case FILTER_SUPPLIER:
          setFilterSupplierType(FilterSupplierType.getByTypeCode(value));
          break;
        case FILTER_OPTIONS:
          setFilterOptions(value);
          break;
        case SORT_BY:
          setSortBy(value);
          break;
        case CARRY_CALC:
          setCarryCalc(Calculation.restore(value));
          break;
        case CARRY_ON:
          setCarryOn(BeeUtils.toBooleanOrNull(value));
          break;
        case EDITABLE:
          setEditable(Calculation.restore(value));
          break;
        case EDITOR:
          setEditor(EditorDescription.restore(value));
          break;
        case FORMAT:
          setFormat(value);
          break;
        case HOR_ALIGN:
          setHorAlign(value);
          break;
        case VERT_ALIGN:
          setVertAlign(value);
          break;
        case WHITE_SPACE:
          setWhiteSpace(value);
          break;
        case MAX_VALUE:
          setMaxValue(value);
          break;
        case MAX_WIDTH:
          setMaxWidth(BeeUtils.toIntOrNull(value));
          break;
        case MIN_VALUE:
          setMinValue(value);
          break;
        case REQUIRED:
          setRequired(BeeUtils.toBooleanOrNull(value));
          break;
        case MIN_WIDTH:
          setMinWidth(BeeUtils.toIntOrNull(value));
          break;
        case SORTABLE:
          setSortable(BeeUtils.toBooleanOrNull(value));
          break;
        case VALIDATION:
          setValidation(Calculation.restore(value));
          break;
        case VISIBLE:
          setVisible(BeeUtils.toBooleanOrNull(value));
          break;
        case BODY_STYLE:
          setBodyStyle(StyleDeclaration.restore(value));
          break;
        case DYN_STYLES:
          String[] scs = Codec.beeDeserializeCollection(value);
          if (ArrayUtils.isEmpty(scs)) {
            setDynStyles(null);
          } else {
            List<ConditionalStyleDeclaration> lst = new ArrayList<>();
            for (String z : scs) {
              lst.add(ConditionalStyleDeclaration.restore(z));
            }
            setDynStyles(lst);
          }
          break;
        case FOOTER_STYLE:
          setFooterStyle(StyleDeclaration.restore(value));
          break;
        case HEADER_STYLE:
          setHeaderStyle(StyleDeclaration.restore(value));
          break;
        case CELL_TYPE:
          setCellType(CellType.getByCode(value));
          break;
        case CELL_RESIZABLE:
          setCellResizable(BeeUtils.toBooleanOrNull(value));
          break;
        case UPDATE_MODE:
          setUpdateMode(RefreshType.getByCode(value));
          break;
        case RENDERER_DESCR:
          setRendererDescription(RendererDescription.restore(value));
          break;
        case RENDER:
          setRender(Calculation.restore(value));
          break;
        case RENDER_TOKENS:
          setRenderTokens(RenderableToken.restoreList(value));
          break;
        case ENUM_KEY:
          setEnumKey(value);
          break;
        case AUTO_FIT:
          setAutoFit(value);
          break;
        case FLEXIBILITY:
          setFlexibility(Flexibility.restore(value));
          break;
        case OPTIONS:
          setOptions(value);
          break;
        case ELEMENT_TYPE:
          setElementType(value);
          break;
        case RENDER_COLUMNS:
          String[] cols = Codec.beeDeserializeCollection(value);
          if (ArrayUtils.isEmpty(cols)) {
            setRenderColumns(null);
          } else {
            setRenderColumns(Lists.newArrayList(cols));
          }
          break;
        case FOOTER_DESCRIPTION:
          setFooterDescription(FooterDescription.restore(value));
          break;
        case DYNAMIC:
          setDynamic(BeeUtils.toBooleanOrNull(value));
          break;
        case EXPORTABLE:
          setExportable(BeeUtils.toBooleanOrNull(value));
          break;
        case EXPORT_WIDTH_FACTOR:
          setExportWidthFactor(BeeUtils.toDoubleOrNull(value));
          break;
        case EDIT_IN_PLACE:
          setEditInPlace(BeeUtils.toBooleanOrNull(value));
          break;
        case DRAGGABLE:
          setDraggable(BeeUtils.toBooleanOrNull(value));
          break;
      }
    }
  }

  public String getAutoFit() {
    return autoFit;
  }

  public StyleDeclaration getBodyStyle() {
    return bodyStyle;
  }

  public String getCaption() {
    return caption;
  }

  public Calculation getCarryCalc() {
    return carryCalc;
  }

  public Boolean getCarryOn() {
    return carryOn;
  }

  public Boolean getCellResizable() {
    return cellResizable;
  }

  public CellType getCellType() {
    return cellType;
  }

  public ColType getColType() {
    return colType;
  }

  public Boolean getDraggable() {
    return draggable;
  }

  public Boolean getDynamic() {
    return dynamic;
  }

  public Collection<ConditionalStyleDeclaration> getDynStyles() {
    return dynStyles;
  }

  public Calculation getEditable() {
    return editable;
  }

  public Boolean getEditInPlace() {
    return editInPlace;
  }

  public EditorDescription getEditor() {
    return editor;
  }

  public String getElementType() {
    return elementType;
  }

  public String getEnumKey() {
    return enumKey;
  }

  public Boolean getExportable() {
    return exportable;
  }

  public Double getExportWidthFactor() {
    return exportWidthFactor;
  }

  public String getFilterOptions() {
    return filterOptions;
  }

  public FilterSupplierType getFilterSupplierType() {
    return filterSupplierType;
  }

  public Flexibility getFlexibility() {
    return flexibility;
  }

  public FooterDescription getFooterDescription() {
    return footerDescription;
  }

  public StyleDeclaration getFooterStyle() {
    return footerStyle;
  }

  public String getFormat() {
    return format;
  }

  public StyleDeclaration getHeaderStyle() {
    return headerStyle;
  }

  public String getHorAlign() {
    return horAlign;
  }

  public String getId() {
    return id;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties(
        "Col Type", getColType(),
        "Id", getId(),
        "Caption", getCaption(),
        "Label", getLabel(),
        "Read Only", getReadOnly(),
        "Width", getWidth(),
        "Source", getSource(),
        "Property", getProperty(),
        "Min Width", getMinWidth(),
        "Max Width", getMaxWidth(),
        "Auto Fit", getAutoFit(),
        "Sortable", getSortable(),
        "Visible", getVisible(),
        "Format", getFormat(),
        "Horizontal Alignment", getHorAlign(),
        "Vertical Alignment", getVertAlign(),
        "White Space", getWhiteSpace(),
        "Min Value", getMinValue(),
        "Max Value", getMaxValue(),
        "Required", getRequired(),
        "Value Type", getValueType(),
        "Precision", getPrecision(),
        "Scale", getScale(),
        "Render Columns", getRenderColumns(),
        "Search By", getSearchBy(),
        "Filter Supplier", getFilterSupplierType(),
        "Filter Options", getFilterOptions(),
        "Sort By", getSortBy(),
        "Cell Type", getCellType(),
        "Cell Resizable", getCellResizable(),
        "Update Mode", getUpdateMode(),
        "Enum Key", getEnumKey(),
        "Element Type", getElementType(),
        "Options", getOptions(),
        "Dynamic", getDynamic(),
        "Exportable", getExportable(),
        "Export Width Factor", getExportWidthFactor(),
        "Carry On", getCarryOn(),
        "Edit In Place", getEditInPlace(),
        "Draggable", getDraggable());

    if (getFlexibility() != null) {
      info.addAll(getFlexibility().getInfo());
    }

    if (getRelation() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Relation", getRelation().getInfo());
      PropertyUtils.addProperty(info, "Relation Initialized", isRelationInitialized());
    }

    if (getValidation() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Validation", getValidation().getInfo());
    }
    if (getEditable() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Editable", getEditable().getInfo());
    }
    if (getEditor() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Editor", getEditor().getInfo());
    }
    if (getCarryCalc() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Carry Calc", getCarryCalc().getInfo());
    }

    if (getRendererDescription() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Renderer",
          getRendererDescription().getInfo());
    }
    if (getRender() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Render", getRender().getInfo());
    }
    if (getRenderTokens() != null) {
      PropertyUtils.appendWithIndex(info, "Render Tokens", "token", getRenderTokens());
    }

    if (getHeaderStyle() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Header Style", getHeaderStyle().getInfo());
    }
    if (getBodyStyle() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Body Style", getBodyStyle().getInfo());
    }
    if (getFooterStyle() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Footer Style", getFooterStyle().getInfo());
    }

    if (getDynStyles() != null && !getDynStyles().isEmpty()) {
      int cnt = getDynStyles().size();
      info.add(new Property("Dyn Styles", BeeUtils.bracket(cnt)));

      int i = 0;
      for (ConditionalStyleDeclaration conditionalStyle : getDynStyles()) {
        i++;
        if (conditionalStyle != null) {
          PropertyUtils.appendChildrenToProperties(info, "Dyn Style " + BeeUtils.progress(i, cnt),
              conditionalStyle.getInfo());
        }
      }
    }

    if (getFooterDescription() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Footer", getFooterDescription().getInfo());
    }

    PropertyUtils.addWhenEmpty(info, getClass());
    return info;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public String getMaxValue() {
    return maxValue;
  }

  public Integer getMaxWidth() {
    return maxWidth;
  }

  @Override
  public String getMinValue() {
    return minValue;
  }

  public Integer getMinWidth() {
    return minWidth;
  }

  @Override
  public String getOptions() {
    return options;
  }

  public Integer getPrecision() {
    return precision;
  }

  public String getProperty() {
    return property;
  }

  public Boolean getReadOnly() {
    return readOnly;
  }

  public Relation getRelation() {
    return relation;
  }

  public Calculation getRender() {
    return render;
  }

  public List<String> getRenderColumns() {
    return renderColumns;
  }

  public RendererDescription getRendererDescription() {
    return rendererDescription;
  }

  public List<RenderableToken> getRenderTokens() {
    return renderTokens;
  }

  public Boolean getRequired() {
    return required;
  }

  public Integer getScale() {
    return scale;
  }

  public String getSearchBy() {
    return searchBy;
  }

  public Boolean getSortable() {
    return sortable;
  }

  public String getSortBy() {
    return sortBy;
  }

  public String getSource() {
    return source;
  }

  public RefreshType getUpdateMode() {
    return updateMode;
  }

  public Calculation getValidation() {
    return validation;
  }

  @Override
  public ValueType getValueType() {
    return valueType;
  }

  public String getVertAlign() {
    return vertAlign;
  }

  public Boolean getVisible() {
    return omniView ? null : visible;
  }

  public String getWhiteSpace() {
    return whiteSpace;
  }

  public Integer getWidth() {
    return width;
  }

  public boolean is(String columnId) {
    return BeeUtils.same(getId(), columnId);
  }

  public boolean isRelationInitialized() {
    return relationInitialized;
  }

  public void replaceSource(String oldId, String newId) {
    if (!BeeUtils.isEmpty(oldId) && !BeeUtils.isEmpty(newId)
        && !BeeUtils.equalsTrim(oldId, newId)) {

      if (BeeUtils.same(getSource(), oldId)) {
        setSource(newId.trim());
      }
      if (BeeUtils.same(getProperty(), oldId)) {
        setProperty(newId.trim());
      }

      if (getRelation() != null) {
        getRelation().replaceTargeColumn(oldId, newId);
      }

      if (getValidation() != null) {
        getValidation().replaceColumn(oldId, newId);
      }
      if (getEditable() != null) {
        getEditable().replaceColumn(oldId, newId);
      }
      if (getCarryCalc() != null) {
        getCarryCalc().replaceColumn(oldId, newId);
      }

      if (getRender() != null) {
        getRender().replaceColumn(oldId, newId);
      }
      if (!BeeUtils.isEmpty(getRenderTokens())) {
        for (RenderableToken token : getRenderTokens()) {
          token.replaceSource(oldId, newId);
        }
      }

      if (BeeUtils.containsSame(getRenderColumns(), oldId)) {
        setRenderColumns(NameUtils.rename(getRenderColumns(), oldId, newId));
      }

      if (BeeUtils.containsSame(getSortBy(), oldId)) {
        setSortBy(NameUtils.rename(getSortBy(), oldId, newId));
      }
      if (BeeUtils.containsSame(getSearchBy(), oldId)) {
        setSortBy(NameUtils.rename(getSearchBy(), oldId, newId));
      }

      if (!BeeUtils.isEmpty(getDynStyles())) {
        for (ConditionalStyleDeclaration declaration : getDynStyles()) {
          declaration.replaceColumn(oldId, newId);
        }
      }

      if (getFooterDescription() != null) {
        getFooterDescription().replaceColumn(oldId, newId);
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
        case COL_TYPE:
          arr[i++] = (getColType() == null) ? null : getColType().getTagName();
          break;
        case ID:
          arr[i++] = getId();
          break;
        case CAPTION:
          arr[i++] = getCaption();
          break;
        case LABEL:
          arr[i++] = getLabel();
          break;
        case READ_ONLY:
          arr[i++] = getReadOnly();
          break;
        case WIDTH:
          arr[i++] = getWidth();
          break;
        case SOURCE:
          arr[i++] = getSource();
          break;
        case PROPERTY:
          arr[i++] = getProperty();
          break;
        case RELATION:
          arr[i++] = getRelation();
          break;
        case VALUE_TYPE:
          arr[i++] = (getValueType() == null) ? null : getValueType().getTypeCode();
          break;
        case PRECISION:
          arr[i++] = getPrecision();
          break;
        case SCALE:
          arr[i++] = getScale();
          break;
        case SEARCH_BY:
          arr[i++] = getSearchBy();
          break;
        case FILTER_SUPPLIER:
          arr[i++] =
              (getFilterSupplierType() == null) ? null : getFilterSupplierType().getTypeCode();
          break;
        case FILTER_OPTIONS:
          arr[i++] = getFilterOptions();
          break;
        case SORT_BY:
          arr[i++] = getSortBy();
          break;
        case CARRY_CALC:
          arr[i++] = getCarryCalc();
          break;
        case CARRY_ON:
          arr[i++] = getCarryOn();
          break;
        case EDITABLE:
          arr[i++] = getEditable();
          break;
        case EDITOR:
          arr[i++] = getEditor();
          break;
        case FORMAT:
          arr[i++] = getFormat();
          break;
        case HOR_ALIGN:
          arr[i++] = getHorAlign();
          break;
        case VERT_ALIGN:
          arr[i++] = getVertAlign();
          break;
        case WHITE_SPACE:
          arr[i++] = getWhiteSpace();
          break;
        case MAX_VALUE:
          arr[i++] = getMaxValue();
          break;
        case MAX_WIDTH:
          arr[i++] = getMaxWidth();
          break;
        case MIN_VALUE:
          arr[i++] = getMinValue();
          break;
        case REQUIRED:
          arr[i++] = getRequired();
          break;
        case MIN_WIDTH:
          arr[i++] = getMinWidth();
          break;
        case SORTABLE:
          arr[i++] = getSortable();
          break;
        case VALIDATION:
          arr[i++] = getValidation();
          break;
        case VISIBLE:
          arr[i++] = getVisible();
          break;
        case BODY_STYLE:
          arr[i++] = getBodyStyle();
          break;
        case DYN_STYLES:
          arr[i++] = getDynStyles();
          break;
        case FOOTER_STYLE:
          arr[i++] = getFooterStyle();
          break;
        case HEADER_STYLE:
          arr[i++] = getHeaderStyle();
          break;
        case CELL_TYPE:
          arr[i++] = (getCellType() == null) ? null : getCellType().getCode();
          break;
        case CELL_RESIZABLE:
          arr[i++] = getCellResizable();
          break;
        case UPDATE_MODE:
          arr[i++] = (getUpdateMode() == null) ? null : getUpdateMode().getCode();
          break;
        case RENDERER_DESCR:
          arr[i++] = getRendererDescription();
          break;
        case RENDER:
          arr[i++] = getRender();
          break;
        case RENDER_TOKENS:
          arr[i++] = getRenderTokens();
          break;
        case ENUM_KEY:
          arr[i++] = getEnumKey();
          break;
        case AUTO_FIT:
          arr[i++] = getAutoFit();
          break;
        case FLEXIBILITY:
          arr[i++] = getFlexibility();
          break;
        case OPTIONS:
          arr[i++] = getOptions();
          break;
        case ELEMENT_TYPE:
          arr[i++] = getElementType();
          break;
        case RENDER_COLUMNS:
          arr[i++] = getRenderColumns();
          break;
        case FOOTER_DESCRIPTION:
          arr[i++] = getFooterDescription();
          break;
        case DYNAMIC:
          arr[i++] = getDynamic();
          break;
        case EXPORTABLE:
          arr[i++] = getExportable();
          break;
        case EXPORT_WIDTH_FACTOR:
          arr[i++] = getExportWidthFactor();
          break;
        case EDIT_IN_PLACE:
          arr[i++] = getEditInPlace();
          break;
        case DRAGGABLE:
          arr[i++] = getDraggable();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setAutoFit(String autoFit) {
    this.autoFit = autoFit;
  }

  public void setBodyStyle(StyleDeclaration bodyStyle) {
    this.bodyStyle = bodyStyle;
  }

  public void setCaption(String caption) {
    this.caption = caption;
  }

  public void setCarryCalc(Calculation carryCalc) {
    this.carryCalc = carryCalc;
  }

  public void setCarryOn(Boolean carryOn) {
    this.carryOn = carryOn;
  }

  public void setCellResizable(Boolean cellResizable) {
    this.cellResizable = cellResizable;
  }

  public void setCellType(CellType cellType) {
    this.cellType = cellType;
  }

  public void setDraggable(Boolean draggable) {
    this.draggable = draggable;
  }

  public void setDynamic(Boolean dynamic) {
    this.dynamic = dynamic;
  }

  public void setDynStyles(Collection<ConditionalStyleDeclaration> dynStyles) {
    this.dynStyles = dynStyles;
  }

  public void setEditable(Calculation editable) {
    this.editable = editable;
  }

  public void setEditInPlace(Boolean editInPlace) {
    this.editInPlace = editInPlace;
  }

  public void setEditor(EditorDescription editor) {
    this.editor = editor;
  }

  public void setElementType(String elementType) {
    this.elementType = elementType;
  }

  public void setEnumKey(String enumKey) {
    this.enumKey = enumKey;
  }

  public void setExportable(Boolean exportable) {
    this.exportable = exportable;
  }

  public void setExportWidthFactor(Double exportWidthFactor) {
    this.exportWidthFactor = exportWidthFactor;
  }

  public void setFilterOptions(String filterOptions) {
    this.filterOptions = filterOptions;
  }

  public void setFilterSupplierType(FilterSupplierType filterSupplierType) {
    this.filterSupplierType = filterSupplierType;
  }

  public void setFlexibility(Flexibility flexibility) {
    this.flexibility = flexibility;
  }

  public void setFooterDescription(FooterDescription footerDescription) {
    this.footerDescription = footerDescription;
  }

  public void setFooterStyle(StyleDeclaration footerStyle) {
    this.footerStyle = footerStyle;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void setHeaderStyle(StyleDeclaration headerStyle) {
    this.headerStyle = headerStyle;
  }

  public void setHorAlign(String horAlign) {
    this.horAlign = horAlign;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public void setMaxValue(String maxValue) {
    this.maxValue = maxValue;
  }

  public void setMaxWidth(Integer maxWidth) {
    this.maxWidth = maxWidth;
  }

  @Override
  public void setMinValue(String minValue) {
    this.minValue = minValue;
  }

  public void setMinWidth(Integer minWidth) {
    this.minWidth = minWidth;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setPrecision(Integer precision) {
    this.precision = precision;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public void setReadOnly(Boolean readOnly) {
    this.readOnly = readOnly;
  }

  public void setRelation(Relation relation) {
    this.relation = relation;
  }

  public void setRelationInitialized(boolean relationInitialized) {
    this.relationInitialized = relationInitialized;
  }

  public void setRender(Calculation render) {
    this.render = render;
  }

  public void setRenderColumns(List<String> renderColumns) {
    this.renderColumns = renderColumns;
  }

  public void setRendererDescription(RendererDescription rendererDescription) {
    this.rendererDescription = rendererDescription;
  }

  public void setRenderTokens(List<RenderableToken> renderTokens) {
    this.renderTokens = renderTokens;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public void setScale(Integer scale) {
    this.scale = scale;
  }

  public void setSearchBy(String searchBy) {
    this.searchBy = searchBy;
  }

  public void setSortable(Boolean sortable) {
    this.sortable = sortable;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setUpdateMode(RefreshType updateMode) {
    this.updateMode = updateMode;
  }

  public void setValidation(Calculation validation) {
    this.validation = validation;
  }

  public void setValueType(ValueType valueType) {
    this.valueType = valueType;
  }

  public void setVertAlign(String vertAlign) {
    this.vertAlign = vertAlign;
  }

  public void setVisible(Boolean visible) {
    this.visible = visible;
  }

  public void setWhiteSpace(String whiteSpace) {
    this.whiteSpace = whiteSpace;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  private void setColType(ColType colType) {
    this.colType = colType;
  }
}
