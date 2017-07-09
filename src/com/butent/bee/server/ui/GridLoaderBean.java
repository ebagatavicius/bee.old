package com.butent.bee.server.ui;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasBounds;
import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.data.CustomProperties;
import com.butent.bee.shared.data.ProviderType;
import com.butent.bee.shared.data.filter.FilterComponent;
import com.butent.bee.shared.data.filter.FilterDescription;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.CellType;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.ui.ColumnRelation;
import com.butent.bee.shared.ui.ConditionalStyleDeclaration;
import com.butent.bee.shared.ui.EditorDescription;
import com.butent.bee.shared.ui.EditorType;
import com.butent.bee.shared.ui.FilterSupplierType;
import com.butent.bee.shared.ui.Flexibility;
import com.butent.bee.shared.ui.FooterDescription;
import com.butent.bee.shared.ui.GridComponentDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.RefreshType;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.RenderableToken;
import com.butent.bee.shared.ui.RendererDescription;
import com.butent.bee.shared.ui.RendererType;
import com.butent.bee.shared.ui.SelectorColumn;
import com.butent.bee.shared.ui.StyleDeclaration;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.ui.WindowType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Reads grids structure from xml files.
 */

@Stateless
public class GridLoaderBean {

  private static BeeLogger logger = LogUtils.getLogger(GridLoaderBean.class);

  private static final String TAG_GRID = "Grid";
  private static final String TAG_COLUMNS = "columns";

  private static final String TAG_CSS = "css";

  private static final String TAG_HEADER = "header";
  private static final String TAG_BODY = "body";
  private static final String TAG_FOOTER = "footer";

  private static final String TAG_ROW_STYLE = "rowStyle";
  private static final String TAG_ROW_MESSAGE = "rowMessage";
  private static final String TAG_ROW_EDITABLE = "rowEditable";
  private static final String TAG_ROW_VALIDATION = "rowValidation";
  private static final String TAG_EDIT_MESSAGE = "editMessage";

  private static final String TAG_STYLE = "style";
  private static final String TAG_HEADER_STYLE = "headerStyle";
  private static final String TAG_BODY_STYLE = "bodyStyle";
  private static final String TAG_FOOTER_STYLE = "footerStyle";

  private static final String TAG_VALIDATION = "validation";
  private static final String TAG_EDITABLE = "editable";
  private static final String TAG_CARRY = "carry";

  private static final String TAG_EDITOR = "editor";

  private static final Set<String> WIDGET_TAGS = Sets.newHashSet("north", "south", "west", "east");

  private static final String ATTR_PARENT = "parent";

  private static final String ATTR_MIN_COLUMN_WIDTH = "minColumnWidth";
  private static final String ATTR_MAX_COLUMN_WIDTH = "maxColumnWidth";

  private static final String ATTR_HEADER_MODE = "headerMode";
  private static final String ATTR_FOOTER_MODE = "footerMode";

  private static final String ATTR_CACHE_DATA = "cacheData";

  private static final String ATTR_DATA_PROVIDER = "dataProvider";
  private static final String ATTR_INITIAL_ROW_SET_SIZE = "initialRowSetSize";

  private static final String ATTR_ENABLED_ACTIONS = "enabledActions";
  private static final String ATTR_DISABLED_ACTIONS = "disabledActions";

  private static final String ATTR_NEW_ROW_DEFAULTS = "newRowDefaults";
  private static final String ATTR_NEW_ROW_WINDOW = "newRowWindow";

  private static final String ATTR_EDIT_MODE = "editMode";
  private static final String ATTR_EDIT_SAVE = "editSave";
  private static final String ATTR_EDIT_SHOW_ID = "editShowId";

  private static final String ATTR_EDIT_IN_PLACE = "editInPlace";
  private static final String ATTR_INSTANT_KARMA = "instantKarma";

  private static final String ATTR_WIDTH = "width";
  private static final String ATTR_MIN_WIDTH = "minWidth";
  private static final String ATTR_MAX_WIDTH = "maxWidth";
  private static final String ATTR_AUTO_FIT = "autoFit";
  private static final String ATTR_AUTO_FLEX = "autoFlex";

  private static final String ATTR_SORTABLE = "sortable";

  private static final String ATTR_REQUIRED = "required";

  private static final String ATTR_TYPE = "type";
  private static final String ATTR_PRECISION = "precision";

  private static final String ATTR_SORT_BY = "sortBy";

  private static final String ATTR_SEARCH_BY = "searchBy";
  private static final String ATTR_FILTER_SUPPLIER = "filterSupplier";
  private static final String ATTR_FILTER_OPTIONS = "filterOptions";

  private static final String ATTR_CELL_TYPE = "cellType";
  private static final String ATTR_CELL_RESIZABLE = "cellResizable";

  private static final String ATTR_ELEMENT = "element";

  private static final String ATTR_ID = "id";

  private static final String ATTR_FAVORITE = "favorite";
  private static final String ATTR_ENABLE_COPY = "enableCopy";

  private static final String ATTR_MIN_NUMBER_OF_ROWS = "minNumberOfRows";
  private static final String ATTR_MAX_NUMBER_OF_ROWS = "maxNumberOfRows";

  private static final String ATTR_RENDER_MODE = "renderMode";

  private static final String ATTR_ROW_CHANGE_SENSITIVITY_MILLIS = "rowChangeSensitivityMillis";

  private static final String ATTR_DYNAMIC = "dynamic";

  private static final String ATTR_EXPORTABLE = "exportable";
  private static final String ATTR_EXPORT_WIDTH_FACTOR = "exportWidthFactor";

  private static final String ATTR_CARRY = "carry";

  private static GridComponentDescription getComponent(Element parent, String tagName) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);

    Element element = XmlUtils.getFirstChildElement(parent, tagName);
    if (element == null) {
      return null;
    }

    StyleDeclaration style = XmlUtils.getStyle(element, GridComponentDescription.TAG_STYLE);
    Map<String, String> attributes = XmlUtils.getAttributes(element);

    if (style == null && (attributes == null || attributes.isEmpty())) {
      return null;
    }
    return new GridComponentDescription(style, attributes);
  }

  private static EditorDescription getEditor(Element parent) {
    Assert.notNull(parent);

    Element element = XmlUtils.getFirstChildElement(parent, TAG_EDITOR);
    if (element == null) {
      return null;
    }
    String typeCode = element.getAttribute(ATTR_TYPE);
    if (BeeUtils.isEmpty(typeCode)) {
      return null;
    }
    EditorType editorType = EditorType.getByTypeCode(typeCode);
    if (editorType == null) {
      return null;
    }

    EditorDescription editor = new EditorDescription(editorType);
    editor.setAttributes(XmlUtils.getAttributes(element));

    List<Element> itemNodes = XmlUtils.getElementsByLocalName(element, HasItems.TAG_ITEM);
    if (!itemNodes.isEmpty()) {
      List<String> items = new ArrayList<>();
      for (Element itemNode : itemNodes) {
        String item = itemNode.getTextContent();
        if (!BeeUtils.isEmpty(item)) {
          items.add(item);
        }
      }
      if (!items.isEmpty()) {
        editor.setItems(items);
      }
    }
    return editor;
  }

  private static Relation getRelation(Element element, Map<String, String> attributes) {
    Assert.notNull(element);

    RendererDescription rowRenderer = null;
    Calculation rowRender = null;
    List<RenderableToken> rowRenderTokens = null;

    List<SelectorColumn> selectorColumns = new ArrayList<>();

    for (Element child : XmlUtils.getChildrenElements(element)) {
      String tagName = XmlUtils.getLocalName(child);

      if (BeeUtils.same(tagName, Relation.TAG_ROW_RENDERER)) {
        rowRenderer = getRenderer(child, null);

      } else if (BeeUtils.same(tagName, Relation.TAG_ROW_RENDER)) {
        rowRender = XmlUtils.getCalculation(child);

      } else if (BeeUtils.same(tagName, Relation.TAG_ROW_RENDER_TOKEN)) {
        RenderableToken token = RenderableToken.create(XmlUtils.getAttributes(child));
        if (token != null) {
          if (rowRenderTokens == null) {
            rowRenderTokens = Lists.newArrayList(token);
          } else {
            rowRenderTokens.add(token);
          }
        }

      } else if (BeeUtils.same(tagName, Relation.TAG_SELECTOR_COLUMN)) {
        RendererDescription renderer = getRenderer(child, RendererDescription.TAG_RENDERER, null);
        Calculation render = XmlUtils.getCalculation(child, RendererDescription.TAG_RENDER);
        List<RenderableToken> tokens = getRenderTokens(child, RenderableToken.TAG_RENDER_TOKEN);

        selectorColumns.add(SelectorColumn.create(XmlUtils.getAttributes(child),
            renderer, render, tokens));
      }
    }

    return Relation.create(attributes, selectorColumns, rowRenderer, rowRender, rowRenderTokens);
  }

  private static RendererDescription getRenderer(Element element, EditorDescription editor) {
    if (element == null) {
      return null;
    }
    String typeCode = element.getAttribute(RendererDescription.ATTR_TYPE);
    if (BeeUtils.isEmpty(typeCode)) {
      return null;
    }
    RendererType type = RendererType.getByTypeCode(typeCode);
    if (type == null) {
      return null;
    }

    RendererDescription renderer = new RendererDescription(type);
    renderer.setAttributes(XmlUtils.getAttributes(element));

    List<String> items = new ArrayList<>();
    List<Element> itemNodes = XmlUtils.getElementsByLocalName(element, HasItems.TAG_ITEM);
    if (!itemNodes.isEmpty()) {
      for (Element itemNode : itemNodes) {
        String item = itemNode.getTextContent();
        if (!BeeUtils.isEmpty(item)) {
          items.add(item);
        }
      }
    }

    if (items.isEmpty() && editor != null && editor.getItems() != null) {
      items.addAll(editor.getItems());
      if (renderer.getValueStartIndex() == null && editor.getValueStartIndex() != null) {
        renderer.setValueStartIndex(editor.getValueStartIndex());
      }
    }
    if (!items.isEmpty()) {
      renderer.setItems(items);
    }

    return renderer;
  }

  private static RendererDescription getRenderer(Element parent, String tagName,
      EditorDescription editor) {
    Assert.notNull(parent);

    Element element = XmlUtils.getFirstChildElement(parent, tagName);
    if (element == null) {
      return null;
    } else {
      return getRenderer(element, editor);
    }
  }

  private static List<RenderableToken> getRenderTokens(Element parent, String tagName) {
    if (parent == null) {
      return null;
    }
    List<Element> tokens = XmlUtils.getElementsByLocalName(parent, tagName);
    if (tokens.isEmpty()) {
      return null;
    }

    List<RenderableToken> result = new ArrayList<>();
    for (Element token : tokens) {
      RenderableToken renderableToken = RenderableToken.create(XmlUtils.getAttributes(token));
      if (renderableToken != null) {
        result.add(renderableToken);
      }
    }
    return result;
  }

  private static void xmlToColumn(Element src, Map<String, String> attributes,
      ColumnDescription dst) {

    if (!attributes.isEmpty()) {
      boolean hasFlexibility = false;

      for (Map.Entry<String, String> attribute : attributes.entrySet()) {
        String key = attribute.getKey();
        String value = attribute.getValue();
        if (BeeUtils.isEmpty(value)) {
          continue;
        }

        if (BeeUtils.same(key, UiConstants.ATTR_CAPTION)) {
          dst.setCaption(value.trim());
        } else if (BeeUtils.same(key, UiConstants.ATTR_LABEL)) {
          dst.setLabel(value.trim());

        } else if (BeeUtils.same(key, UiConstants.ATTR_READ_ONLY)) {
          dst.setReadOnly(BeeUtils.toBooleanOrNull(value));

        } else if (BeeUtils.same(key, ATTR_WIDTH)) {
          dst.setWidth(BeeUtils.toIntOrNull(value));
        } else if (BeeUtils.same(key, ATTR_MIN_WIDTH)) {
          dst.setMinWidth(BeeUtils.toIntOrNull(value));
        } else if (BeeUtils.same(key, ATTR_MAX_WIDTH)) {
          dst.setMaxWidth(BeeUtils.toIntOrNull(value));
        } else if (BeeUtils.same(key, ATTR_AUTO_FIT)) {
          dst.setAutoFit(value.trim());

        } else if (BeeUtils.same(key, ATTR_SORTABLE)) {
          dst.setSortable(BeeUtils.toBooleanOrNull(value));

        } else if (BeeUtils.same(key, UiConstants.ATTR_VISIBLE)) {
          dst.setVisible(BeeUtils.toBooleanOrNull(value));

        } else if (BeeUtils.same(key, UiConstants.ATTR_FORMAT)) {
          dst.setFormat(value.trim());
        } else if (BeeUtils.same(key, UiConstants.ATTR_HORIZONTAL_ALIGNMENT)) {
          dst.setHorAlign(value.trim());
        } else if (BeeUtils.same(key, UiConstants.ATTR_VERTICAL_ALIGNMENT)) {
          dst.setVertAlign(value.trim());
        } else if (BeeUtils.same(key, UiConstants.ATTR_WHITE_SPACE)) {
          dst.setWhiteSpace(value.trim());

        } else if (BeeUtils.same(key, UiConstants.ATTR_SOURCE)) {
          dst.setSource(value.trim());
        } else if (BeeUtils.same(key, UiConstants.ATTR_PROPERTY)) {
          dst.setProperty(value.trim());
        } else if (BeeUtils.same(key, UiConstants.ATTR_USER_MODE)) {
          dst.setUserMode(BeeUtils.toBooleanOrNull(value));

        } else if (BeeUtils.same(key, ATTR_REQUIRED)) {
          dst.setRequired(BeeUtils.toBooleanOrNull(value));

        } else if (BeeUtils.same(key, HasBounds.ATTR_MIN_VALUE)) {
          dst.setMinValue(value.trim());
        } else if (BeeUtils.same(key, HasBounds.ATTR_MAX_VALUE)) {
          dst.setMaxValue(value.trim());

        } else if (BeeUtils.same(key, ATTR_TYPE)) {
          dst.setValueType(ValueType.getByTypeCode(value));
        } else if (BeeUtils.same(key, ATTR_PRECISION)) {
          dst.setPrecision(BeeUtils.toIntOrNull(value));
        } else if (BeeUtils.same(key, UiConstants.ATTR_SCALE)) {
          dst.setScale(BeeUtils.toIntOrNull(value));

        } else if (BeeUtils.same(key, RendererDescription.ATTR_RENDER_COLUMNS)) {
          dst.setRenderColumns(NameUtils.toList(value.trim()));
        } else if (BeeUtils.same(key, ATTR_SORT_BY)) {
          dst.setSortBy(value.trim());

        } else if (BeeUtils.same(key, ATTR_SEARCH_BY)) {
          dst.setSearchBy(value.trim());
        } else if (BeeUtils.same(key, ATTR_FILTER_SUPPLIER)) {
          dst.setFilterSupplierType(FilterSupplierType.getByTypeCode(value));
        } else if (BeeUtils.same(key, ATTR_FILTER_OPTIONS)) {
          dst.setFilterOptions(value.trim());

        } else if (BeeUtils.same(key, ATTR_CELL_TYPE)) {
          dst.setCellType(CellType.getByCode(value));
        } else if (BeeUtils.same(key, ATTR_CELL_RESIZABLE)) {
          dst.setCellResizable(BeeUtils.toBooleanOrNull(value));

        } else if (BeeUtils.same(key, ATTR_ELEMENT)) {
          dst.setElementType(value.trim());

        } else if (BeeUtils.same(key, RefreshType.ATTR_UPDATE_MODE)) {
          dst.setUpdateMode(RefreshType.getByCode(value));

        } else if (BeeUtils.same(key, EnumUtils.ATTR_ENUM_KEY)) {
          dst.setEnumKey(value.trim());

        } else if (BeeUtils.same(key, HasOptions.ATTR_OPTIONS)) {
          dst.setOptions(value.trim());

        } else if (BeeUtils.same(key, ATTR_DYNAMIC)) {
          dst.setDynamic(BeeUtils.toBooleanOrNull(value));

        } else if (BeeUtils.same(key, ATTR_EXPORTABLE)) {
          dst.setExportable(BeeUtils.toBooleanOrNull(value));
        } else if (BeeUtils.same(key, ATTR_EXPORT_WIDTH_FACTOR)) {
          dst.setExportWidthFactor(BeeUtils.toDoubleOrNull(value));

        } else if (BeeUtils.same(key, ATTR_CARRY)) {
          dst.setCarryOn(BeeUtils.toBooleanOrNull(value));

        } else if (BeeUtils.same(key, ATTR_EDIT_IN_PLACE)) {
          dst.setEditInPlace(BeeUtils.toBooleanOrNull(value));
        } else if (BeeUtils.same(key, Attributes.DRAGGABLE)) {
          dst.setDraggable(BeeUtils.toBooleanOrNull(value));

        } else if (BeeUtils.same(key, UiConstants.ATTR_BACKGROUND_SOURCE)) {
          dst.setBackgroundSource(value.trim());
        } else if (BeeUtils.same(key, UiConstants.ATTR_FOREGROUND_SOURCE)) {
          dst.setForegroundSource(value.trim());

        } else if (BeeUtils.same(key, ATTR_INSTANT_KARMA)) {
          dst.setInstantKarma(BeeUtils.toBooleanOrNull(value));

        } else if (Flexibility.isAttributeRelevant(key)) {
          hasFlexibility = true;
        }
      }

      if (hasFlexibility) {
        dst.setFlexibility(Flexibility.createIfDefined(attributes));
      }
    }

    Element styleElement = XmlUtils.getFirstChildElement(src, TAG_STYLE);
    if (styleElement != null) {
      StyleDeclaration headerStyle = XmlUtils.getStyle(styleElement, TAG_HEADER_STYLE);
      if (headerStyle != null) {
        dst.setHeaderStyle(headerStyle);
      }
      StyleDeclaration bodyStyle = XmlUtils.getStyle(styleElement, TAG_BODY_STYLE);
      if (bodyStyle != null) {
        dst.setBodyStyle(bodyStyle);
      }
      StyleDeclaration footerStyle = XmlUtils.getStyle(styleElement, TAG_FOOTER_STYLE);
      if (footerStyle != null) {
        dst.setFooterStyle(footerStyle);
      }
    }

    List<Element> dynStyleNodes = XmlUtils.getElementsByLocalName(src,
        ConditionalStyleDeclaration.TAG_DYN_STYLE);
    if (!dynStyleNodes.isEmpty()) {
      List<ConditionalStyleDeclaration> dynStyles = new ArrayList<>();
      for (Element dynStyleNode : dynStyleNodes) {
        ConditionalStyleDeclaration cs = XmlUtils.getConditionalStyle(dynStyleNode);
        if (cs != null) {
          dynStyles.add(cs);
        }
      }
      if (!dynStyles.isEmpty()) {
        dst.setDynStyles(dynStyles);
      }
    }

    Calculation validation = XmlUtils.getCalculation(src, TAG_VALIDATION);
    if (validation != null) {
      dst.setValidation(validation);
    }
    Calculation editable = XmlUtils.getCalculation(src, TAG_EDITABLE);
    if (editable != null) {
      dst.setEditable(editable);
    }
    Calculation carryCalc = XmlUtils.getCalculation(src, TAG_CARRY);
    if (carryCalc != null) {
      dst.setCarryCalc(carryCalc);
    }

    EditorDescription editor = getEditor(src);
    if (editor != null) {
      dst.setEditor(editor);
    }

    RendererDescription renderer = getRenderer(src, RendererDescription.TAG_RENDERER, editor);
    if (renderer != null) {
      dst.setRendererDescription(renderer);
    }
    Calculation render = XmlUtils.getCalculation(src, RendererDescription.TAG_RENDER);
    if (render != null) {
      dst.setRender(render);
    }
    List<RenderableToken> renderTokens = getRenderTokens(src, RenderableToken.TAG_RENDER_TOKEN);
    if (!BeeUtils.isEmpty(renderTokens)) {
      dst.setRenderTokens(renderTokens);
    }

    Element footerElement = XmlUtils.getFirstChildElement(src, TAG_FOOTER);
    if (footerElement != null) {
      dst.setFooterDescription(new FooterDescription(XmlUtils.getAttributes(footerElement)));
    }
  }

  @EJB
  SystemBean sys;
  @EJB
  UiHolderBean ui;
  @EJB
  UserServiceBean usr;

  public GridDescription getGridDescription(Element gridElement, Set<String> hiddenColumns) {
    if (gridElement == null) {
      logger.severe("grid element is null");
      return null;
    }
    if (!BeeUtils.same(XmlUtils.getLocalName(gridElement), TAG_GRID)) {
      logger.warning("unrecognized grid element tag name", gridElement.getTagName());
      return null;
    }

    String gridName = gridElement.getAttribute(UiConstants.ATTR_NAME);
    String viewName = gridElement.getAttribute(UiConstants.ATTR_VIEW_NAME);

    if (BeeUtils.isEmpty(gridName)) {
      logger.severe("grid attribute", UiConstants.ATTR_NAME, "not found");
      return null;
    }

    BeeView view;
    GridDescription grid;

    if (BeeUtils.isEmpty(viewName)) {
      view = null;
      grid = new GridDescription(gridName);
    } else {
      if (!sys.isView(viewName)) {
        logger.warning("grid", gridName, "unrecognized view name:", viewName);
        return null;
      }

      view = sys.getView(viewName);
      grid = new GridDescription(gridName, viewName, view.getSourceIdName(),
          view.getSourceVersionName());
    }

    xmlToGrid(gridElement, grid, view, hiddenColumns);

    List<Element> columnGroups = XmlUtils.getElementsByLocalName(gridElement, TAG_COLUMNS);
    if (columnGroups.isEmpty()) {
      logger.warning("grid", gridName, "tag", TAG_COLUMNS, "not found");
      return null;
    }

    List<Element> columns = new ArrayList<>();
    for (Element columnGroup : columnGroups) {
      columns.addAll(XmlUtils.getChildrenElements(columnGroup));
    }
    if (columns.isEmpty()) {
      logger.warning("grid", gridName, "has no columns");
      return null;
    }

    for (int i = 0; i < columns.size(); i++) {
      Element columnElement = columns.get(i);

      String colTag = XmlUtils.getLocalName(columnElement);
      ColType colType = ColType.getColType(colTag);
      String colName = columnElement.getAttribute(UiConstants.ATTR_NAME);

      if (colType == null) {
        logger.warning("grid", gridName, "column", i, colName,
            "type", colTag, "not recognized");

      } else if (BeeUtils.isEmpty(colName)) {
        logger.warning("grid", gridName, "column", i, colTag, "name not specified");

      } else if (grid.hasColumn(colName)) {
        logger.warning("grid", gridName, "column", i, colTag, "duplicate column name:", colName);

      } else if (isColumnVisible(view, colType, colName, columnElement, hiddenColumns)) {
        ColumnDescription column = new ColumnDescription(colType, colName);

        Map<String, String> attributes = XmlUtils.getAttributes(columnElement);
        xmlToColumn(columnElement, attributes, column);

        if (ColType.RELATED.equals(colType)) {
          column.setRelation(getRelation(columnElement, attributes));

        } else if (ColType.AUTO.equals(colType)) {
          Relation relation =
              Relation.create(columnElement.getAttribute(UiConstants.ATTR_VIEW_NAME),
                  Lists.newArrayList(columnElement.getAttribute("viewColumn")));

          relation.setAttributes(attributes);
          column.setRelation(relation);

        } else {
          column.setColumnRelation(ColumnRelation.maybeCreate(attributes));
        }

        if (initColumn(view, column)) {
          grid.addColumn(column);
        }
      }
    }

    if (grid.isEmpty()) {
      logger.warning("grid", gridName, "has no columns");
      return null;
    }

    if (view != null) {
      ListMultimap<String, String> translationColumns = view.getTranslationColumns();

      for (String original : translationColumns.keySet()) {
        int index = grid.getColumnIndex(original);

        if (!BeeConst.isUndef(index)) {
          for (String translation : translationColumns.get(original)) {

            if (!grid.hasColumn(translation) && usr.isColumnVisible(view, translation)
                && !BeeUtils.contains(hiddenColumns, translation)) {

              ColumnDescription column = grid.getColumn(original).copy();

              column.setId(translation);
              column.replaceSource(original, translation);

              String label = view.getColumnLabel(translation);
              if (!BeeUtils.isEmpty(label) && !BeeUtils.equalsTrim(label, column.getLabel())) {
                column.setLabel(label);
              } else {
                column.setLabel(null);
              }
              column.setCaption(null);

              column.setVisible(false);
              column.setEditInPlace(true);

              index++;
              grid.getColumns().add(index, column);
            }
          }
        }
      }
    }

    return grid;
  }

  private boolean initColumn(BeeView view, ColumnDescription columnDescription) {
    Assert.notNull(columnDescription);

    ColType colType = columnDescription.getColType();
    String source = columnDescription.getSource();

    if (!colType.isReadOnly() && BeeUtils.isEmpty(source)) {
      source = columnDescription.getId();
      columnDescription.setSource(source);
    }

    if (view == null) {
      return true;
    }

    boolean ok = false;
    String viewName = view.getName();

    switch (colType) {
      case ID:
        columnDescription.setSource(view.getSourceIdName());
        ok = true;
        break;

      case VERSION:
        columnDescription.setSource(view.getSourceVersionName());
        ok = true;
        break;

      case DATA:
      case RELATED:
      case AUTO:
        if (view.hasColumn(source)) {
          if (view.isColReadOnly(source)
              || colType.equals(ColType.DATA) && !view.isColEditable(source)
              || !usr.canEditColumn(viewName, source)) {
            columnDescription.setReadOnly(true);
          }
          ok = true;
        } else {
          logger.warning(viewName, "unrecognized view column:", source);
        }
        break;

      case CALCULATED:
      case SELECTION:
      case PROPERTY:
        ok = true;
        break;

      case ACTION:
        if (BeeUtils.isEmpty(source) && view.hasColumn(columnDescription.getId())) {
          columnDescription.setSource(columnDescription.getId());
        }
        ok = true;
        break;

      case RIGHTS:
        break;
    }

    return ok;
  }

  private boolean isColumnVisible(BeeView view, ColType colType, String colName, Element element,
      Set<String> hiddenColumns) {

    if (element.hasAttribute(UiConstants.ATTR_VISIBLE)
        && BeeConst.isTrue(element.getAttribute(UiConstants.ATTR_VISIBLE))) {
      return true;
    }

    if (element.hasAttribute(UiConstants.ATTR_MODULE)
        && !usr.isAnyModuleVisible(element.getAttribute(UiConstants.ATTR_MODULE))) {
      return false;
    }

    if (view != null) {
      String source = element.getAttribute(UiConstants.ATTR_SOURCE);

      if (BeeUtils.isEmpty(source)) {
        switch (colType) {
          case ACTION:
          case AUTO:
          case CALCULATED:
          case DATA:
          case RELATED:
            source = colName;
            break;
          default:
        }
      }

      if (!BeeUtils.isEmpty(source)
          && (!usr.isColumnVisible(view, source) || BeeUtils.contains(hiddenColumns, source))) {
        return false;
      }
    }

    return true;
  }

  private void xmlToGrid(Element src, GridDescription dst, BeeView view,
      Set<String> hiddenColumns) {

    Assert.notNull(src);
    Assert.notNull(dst);

    String parent = src.getAttribute(ATTR_PARENT);
    if (!BeeUtils.isEmpty(parent)) {
      dst.setParent(parent.trim());
    }

    String caption = src.getAttribute(UiConstants.ATTR_CAPTION);
    if (!BeeUtils.isEmpty(caption)) {
      dst.setCaption(caption.trim());
    }

    if (view != null) {
      String filter = src.getAttribute(UiConstants.ATTR_FILTER);
      if (!BeeUtils.isEmpty(filter)) {
        dst.setFilter(view.parseFilter(filter.trim(), usr.getCurrentUserId()));
      }

      String currentUserFilter = src.getAttribute(UiConstants.ATTR_CURRENT_USER_FILTER);
      if (!BeeUtils.isEmpty(currentUserFilter)) {
        dst.setCurrentUserFilter(currentUserFilter.trim());
      }

      String order = src.getAttribute(UiConstants.ATTR_ORDER);
      if (!BeeUtils.isEmpty(order)) {
        dst.setOrder(view.parseOrder(order.trim()));
      }
    }

    Integer minColumnWidth = XmlUtils.getAttributeInteger(src, ATTR_MIN_COLUMN_WIDTH);
    if (minColumnWidth != null) {
      dst.setMinColumnWidth(minColumnWidth);
    }
    Integer maxColumnWidth = XmlUtils.getAttributeInteger(src, ATTR_MAX_COLUMN_WIDTH);
    if (maxColumnWidth != null) {
      dst.setMaxColumnWidth(maxColumnWidth);
    }

    String autoFit = src.getAttribute(ATTR_AUTO_FIT);
    if (!BeeUtils.isEmpty(autoFit)) {
      dst.setAutoFit(autoFit);
    }
    Boolean autoFlex = XmlUtils.getAttributeBoolean(src, ATTR_AUTO_FLEX);
    if (autoFlex != null) {
      dst.setAutoFlex(autoFlex);
    }

    String flexGrow = src.getAttribute(Flexibility.ATTR_GROW);
    String flexShrink = src.getAttribute(Flexibility.ATTR_SHRINK);
    String flexBasis = src.getAttribute(Flexibility.ATTR_BASIS);

    if (!BeeUtils.allEmpty(flexGrow, flexShrink, flexBasis)) {
      Map<String, String> flexAttributes = new HashMap<>();
      flexAttributes.put(Flexibility.ATTR_GROW, flexGrow);
      flexAttributes.put(Flexibility.ATTR_SHRINK, flexShrink);
      flexAttributes.put(Flexibility.ATTR_BASIS, flexBasis);

      String flexBasisUnit = src.getAttribute(Flexibility.ATTR_BASIS_UNIT);
      if (!BeeUtils.isEmpty(flexBasisUnit)) {
        flexAttributes.put(Flexibility.ATTR_BASIS_UNIT, flexBasisUnit);
      }

      dst.setFlexibility(Flexibility.createIfDefined(flexAttributes));
    }

    String headerMode = src.getAttribute(ATTR_HEADER_MODE);
    if (!BeeUtils.isEmpty(headerMode)) {
      dst.setHeaderMode(headerMode);
    }
    String footerMode = src.getAttribute(ATTR_FOOTER_MODE);
    if (!BeeUtils.isEmpty(footerMode)) {
      dst.setFooterMode(footerMode);
    }

    Boolean cacheData = XmlUtils.getAttributeBoolean(src, ATTR_CACHE_DATA);
    if (cacheData != null) {
      dst.setCacheData(cacheData);
    }
    Boolean cacheDescription = XmlUtils.getAttributeBoolean(src,
        UiConstants.ATTR_CACHE_DESCRIPTION);
    if (cacheDescription != null) {
      dst.setCacheDescription(cacheDescription);
    }

    String dataProvider = src.getAttribute(ATTR_DATA_PROVIDER);
    if (!BeeUtils.isEmpty(dataProvider)) {
      dst.setDataProvider(EnumUtils.getEnumByName(ProviderType.class, dataProvider));
    }
    Integer initialRowSetSize = XmlUtils.getAttributeInteger(src, ATTR_INITIAL_ROW_SET_SIZE);
    if (initialRowSetSize != null) {
      dst.setInitialRowSetSize(initialRowSetSize);
    }
    Boolean paging = XmlUtils.getAttributeBoolean(src, UiConstants.ATTR_PAGING);
    if (paging != null) {
      dst.setPaging(paging);
    }

    Boolean readOnly = XmlUtils.getAttributeBoolean(src, UiConstants.ATTR_READ_ONLY);
    if (readOnly != null) {
      dst.setReadOnly(readOnly);
    }
    String enabledActions = src.getAttribute(ATTR_ENABLED_ACTIONS);
    if (!BeeUtils.isEmpty(enabledActions)) {
      dst.setEnabledActions(Action.parse(enabledActions));
    }
    String disabledActions = src.getAttribute(ATTR_DISABLED_ACTIONS);
    if (!BeeUtils.isEmpty(disabledActions)) {
      dst.setDisabledActions(Action.parse(disabledActions));
    }

    String favorite = src.getAttribute(ATTR_FAVORITE);
    if (!BeeUtils.isEmpty(favorite)) {
      dst.setFavorite(favorite.trim());
    }
    String enableCopy = src.getAttribute(ATTR_ENABLE_COPY);
    if (!BeeUtils.isEmpty(enableCopy)) {
      dst.setEnableCopy(enableCopy.trim());
    }

    Integer minNumberOfRows = XmlUtils.getAttributeInteger(src, ATTR_MIN_NUMBER_OF_ROWS);
    if (minNumberOfRows != null) {
      dst.setMinNumberOfRows(minNumberOfRows);
    }
    Integer maxNumberOfRows = XmlUtils.getAttributeInteger(src, ATTR_MAX_NUMBER_OF_ROWS);
    if (maxNumberOfRows != null) {
      dst.setMaxNumberOfRows(maxNumberOfRows);
    }

    String renderMode = src.getAttribute(ATTR_RENDER_MODE);
    if (!BeeUtils.isEmpty(renderMode)) {
      dst.setRenderMode(renderMode.trim());
    }

    Integer rowChangeSensitivityMillis =
        XmlUtils.getAttributeInteger(src, ATTR_ROW_CHANGE_SENSITIVITY_MILLIS);
    if (rowChangeSensitivityMillis != null) {
      dst.setRowChangeSensitivityMillis(rowChangeSensitivityMillis);
    }

    String newRowForm = src.getAttribute(UiConstants.ATTR_NEW_ROW_FORM);
    if (!BeeUtils.isEmpty(newRowForm)) {
      dst.setNewRowForm(newRowForm);
    }

    String newRowColumns = src.getAttribute(UiConstants.ATTR_NEW_ROW_COLUMNS);
    if (!BeeUtils.isEmpty(newRowColumns)) {
      dst.setNewRowColumns(newRowColumns.trim());
    }
    String newRowDefaults = src.getAttribute(ATTR_NEW_ROW_DEFAULTS);
    if (!BeeUtils.isEmpty(newRowDefaults)) {
      dst.setNewRowDefaults(newRowDefaults.trim());
    }

    String newRowCaption = src.getAttribute(UiConstants.ATTR_NEW_ROW_CAPTION);
    if (!BeeUtils.isEmpty(newRowCaption)) {
      dst.setNewRowCaption(newRowCaption.trim());
    }
    String newRowWindow = src.getAttribute(ATTR_NEW_ROW_WINDOW);
    if (!BeeUtils.isEmpty(newRowWindow)) {
      dst.setNewRowWindow(WindowType.parse(newRowWindow));
    }

    String editForm = src.getAttribute(UiConstants.ATTR_EDIT_FORM);
    if (!BeeUtils.isEmpty(editForm)) {
      dst.setEditForm(editForm);
    }
    Boolean editMode = XmlUtils.getAttributeBoolean(src, ATTR_EDIT_MODE);
    if (editMode != null) {
      dst.setEditMode(editMode);
    }
    Boolean editSave = XmlUtils.getAttributeBoolean(src, ATTR_EDIT_SAVE);
    if (editSave != null) {
      dst.setEditSave(editSave);
    }
    Calculation editMessage = XmlUtils.getCalculation(src, TAG_EDIT_MESSAGE);
    if (editMessage != null) {
      dst.setEditMessage(editMessage);
    }
    Boolean editShowId = XmlUtils.getAttributeBoolean(src, ATTR_EDIT_SHOW_ID);
    if (editShowId != null) {
      dst.setEditShowId(editShowId);
    }
    String editWindow = src.getAttribute(UiConstants.ATTR_EDIT_WINDOW);
    if (!BeeUtils.isEmpty(editWindow)) {
      dst.setEditWindow(WindowType.parse(editWindow));
    }

    Boolean editInPlace = XmlUtils.getAttributeBoolean(src, ATTR_EDIT_IN_PLACE);
    if (editInPlace != null) {
      dst.setEditInPlace(editInPlace);
    }

    List<Element> cssNodes = XmlUtils.getElementsByLocalName(src, TAG_CSS);
    if (!cssNodes.isEmpty()) {
      Map<String, String> styleSheets = new HashMap<>();
      for (Element cssNode : cssNodes) {
        String name = cssNode.getAttribute(ATTR_ID);
        String text = cssNode.getTextContent();
        if (!BeeUtils.isEmpty(name) && !BeeUtils.isEmpty(text)) {
          styleSheets.put(name.trim(), text.trim());
        }
      }
      if (!styleSheets.isEmpty()) {
        dst.setStyleSheets(styleSheets);
      }
    }

    List<Element> widgetElements = XmlUtils.getChildrenElements(src, WIDGET_TAGS);
    if (!widgetElements.isEmpty()) {
      List<String> widgets = new ArrayList<>();

      for (Element widgetElement : widgetElements) {
        ui.checkWidgetChildrenVisibility(widgetElement, hiddenColumns);
        if (XmlUtils.hasChildElements(widgetElement)) {
          widgets.add(XmlUtils.toString(widgetElement, false));
        }
      }

      if (!widgets.isEmpty()) {
        dst.setWidgets(widgets);
      }
    }

    GridComponentDescription header = getComponent(src, TAG_HEADER);
    if (header != null) {
      dst.setHeader(header);
    }
    GridComponentDescription body = getComponent(src, TAG_BODY);
    if (body != null) {
      dst.setBody(body);
    }
    GridComponentDescription footer = getComponent(src, TAG_FOOTER);
    if (footer != null) {
      dst.setFooter(footer);
    }

    List<Element> rowStyleNodes = XmlUtils.getElementsByLocalName(src, TAG_ROW_STYLE);
    if (!rowStyleNodes.isEmpty()) {
      List<ConditionalStyleDeclaration> rowStyles = new ArrayList<>();
      for (Element rowStyleNode : rowStyleNodes) {
        ConditionalStyleDeclaration cs = XmlUtils.getConditionalStyle(rowStyleNode);
        if (cs != null) {
          rowStyles.add(cs);
        }
      }
      if (!rowStyles.isEmpty()) {
        dst.setRowStyles(rowStyles);
      }
    }

    Calculation rowMessage = XmlUtils.getCalculation(src, TAG_ROW_MESSAGE);
    if (rowMessage != null) {
      dst.setRowMessage(rowMessage);
    }
    Calculation rowEditable = XmlUtils.getCalculation(src, TAG_ROW_EDITABLE);
    if (rowEditable != null) {
      dst.setRowEditable(rowEditable);
    }
    Calculation rowValidation = XmlUtils.getCalculation(src, TAG_ROW_VALIDATION);
    if (rowValidation != null) {
      dst.setRowValidation(rowValidation);
    }

    List<Element> filterDescriptionElements = XmlUtils.getElementsByLocalName(src,
        FilterDescription.TAG_PREDEFINED_FILTER);

    if (!filterDescriptionElements.isEmpty()) {
      List<FilterDescription> predefinedFilters = new ArrayList<>();

      for (Element fdElement : filterDescriptionElements) {
        List<Element> filterComponentElements = XmlUtils.getElementsByLocalName(fdElement,
            FilterDescription.TAG_COLUMN);

        if (!filterComponentElements.isEmpty()) {
          List<FilterComponent> filterComponents = new ArrayList<>();

          for (Element componentElement : filterComponentElements) {
            FilterComponent component =
                FilterComponent.create(XmlUtils.getAttributes(componentElement));
            if (component != null) {
              filterComponents.add(component);
            }
          }

          FilterDescription fd = FilterDescription.create(dst.getName(),
              XmlUtils.getAttributes(fdElement), filterComponents);
          if (fd != null) {
            predefinedFilters.add(fd);
          }
        }
      }

      if (!predefinedFilters.isEmpty()) {
        dst.setPredefinedFilters(predefinedFilters);
      }
    }

    String options = src.getAttribute(HasOptions.ATTR_OPTIONS);
    if (!BeeUtils.isEmpty(options)) {
      dst.setOptions(options);
    }

    List<Element> propElements = XmlUtils.getElementsByLocalName(src,
        CustomProperties.TAG_PROPERTIES);
    if (!propElements.isEmpty()) {
      Map<String, String> properties = new HashMap<>();

      for (Element propElement : propElements) {
        properties.putAll(XmlUtils.getAttributes(propElement));
      }

      if (!properties.isEmpty()) {
        dst.setProperties(properties);
      }
    }
  }
}
