package com.butent.bee.client.grid;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.impl.ElementMapperImpl;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.IsHtmlTable;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HtmlTable extends Panel implements IdentifiableWidget, IsHtmlTable,
    HasMouseMoveHandlers, HasMouseOutHandlers, HasMouseOverHandlers, HasCaption {

  public class CellFormatter {

    protected CellFormatter() {
      super();
    }

    public void addStyleName(int row, int column, String styleName) {
      if (!BeeUtils.isEmpty(styleName)) {
        ensureElement(row, column).addClassName(styleName);
      }
    }

    public Element ensureElement(int row, int column) {
      prepareCell(row, column);
      return getElement(row, column);
    }

    public int getColSpan(int row, int column) {
      return DomUtils.getColSpan(getElement(row, column));
    }

    public TableCellElement getElement(int row, int column) {
      checkCellBounds(row, column);
      return getCell(bodyElem, row, column);
    }

    public int getRowSpan(int row, int column) {
      return DomUtils.getRowSpan(getElement(row, column));
    }

    public String getStyleName(int row, int column) {
      return getElement(row, column).getClassName();
    }

    public boolean isVisible(int row, int column) {
      return DomUtils.isVisible(getElement(row, column));
    }

    public void removeStyleName(int row, int column, String styleName) {
      if (!BeeUtils.isEmpty(styleName)) {
        getElement(row, column).removeClassName(styleName);
      }
    }

    public void setAlignment(int row, int column, TextAlign hAlign, VerticalAlign vAlign) {
      setHorizontalAlignment(row, column, hAlign);
      setVerticalAlignment(row, column, vAlign);
    }

    public void setColSpan(int row, int column, int colSpan) {
      DomUtils.setColSpan(TableCellElement.as(ensureElement(row, column)), colSpan);
    }

    public void setHeight(int row, int column, double height, CssUnit unit) {
      StyleUtils.setHeight(ensureElement(row, column), height, unit);
    }

    public void setHeight(int row, int column, int height) {
      StyleUtils.setHeight(ensureElement(row, column), height);
    }

    public void setHorizontalAlignment(int row, int column, TextAlign align) {
      StyleUtils.setTextAlign(ensureElement(row, column), align);
    }

    public void setRowSpan(int row, int column, int rowSpan) {
      DomUtils.setRowSpan(ensureElement(row, column), rowSpan);
    }

    public void setStyleName(int row, int column, String styleName) {
      ensureElement(row, column).setClassName(styleName);
    }

    public void setStyleName(int row, int column, String styleName, boolean add) {
      if (add) {
        addStyleName(row, column, styleName);
      } else {
        removeStyleName(row, column, styleName);
      }
    }

    public void setVerticalAlignment(int row, int column, VerticalAlign align) {
      StyleUtils.setVerticalAlign(ensureElement(row, column), align);
    }

    public void setVisible(int row, int column, boolean visible) {
      UIObject.setVisible(getElement(row, column), visible);
    }

    public void setWidth(int row, int column, double width, CssUnit unit) {
      StyleUtils.setWidth(ensureElement(row, column), width, unit);
    }

    public void setWidth(int row, int column, int width) {
      StyleUtils.setWidth(ensureElement(row, column), width);
    }

    public void setWordWrap(int row, int column, boolean wrap) {
      StyleUtils.setWordWrap(ensureElement(row, column), wrap);
    }

    //@formatter:off
    private native TableCellElement getCell(Element table, int row, int column) /*-{
      return table.rows[row].cells[column];
    }-*/;
    //@formatter:on
  }

  public class ColumnFormatter {

    private Element columnGroup;

    protected ColumnFormatter() {
      super();
    }

    public void addStyleName(int column, String styleName) {
      if (!BeeUtils.isEmpty(styleName)) {
        ensureColumn(column).addClassName(styleName);
      }
    }

    public Element getElement(int column) {
      return ensureColumn(column);
    }

    public String getStyleName(int column) {
      return ensureColumn(column).getClassName();
    }

    public void removeStyleName(int column, String styleName) {
      if (!BeeUtils.isEmpty(styleName)) {
        ensureColumn(column).removeClassName(styleName);
      }
    }

    public void setStyleName(int column, String styleName) {
      ensureColumn(column).setClassName(styleName);
    }

    public void setWidth(int column, double width, CssUnit unit) {
      StyleUtils.setWidth(ensureColumn(column), width, unit);
    }

    public void setWidth(int column, int width) {
      StyleUtils.setWidth(ensureColumn(column), width);
    }

    protected Element ensureColumn(int column) {
      Assert.nonNegative(column, "Column " + column + " must be non-negative");
      ensureColumnGroup();

      int count = columnGroup.getChildCount();
      for (int i = count; i <= column; i++) {
        columnGroup.appendChild(Document.get().createColElement());
      }

      return columnGroup.getChild(column).cast();
    }

    protected void ensureColumnGroup() {
      if (columnGroup == null) {
        columnGroup = Document.get().createColGroupElement();
        tableElem.insertFirst(columnGroup);
        columnGroup.appendChild(Document.get().createColElement());
      }
    }
  }

  public class RowFormatter {

    protected RowFormatter() {
      super();
    }

    public void addStyleName(int row, String styleName) {
      if (!BeeUtils.isEmpty(styleName)) {
        ensureElement(row).addClassName(styleName);
      }
    }

    public Element ensureElement(int row) {
      prepareRow(row);
      return getElement(row);
    }

    public Element getElement(int row) {
      checkRowBounds(row);
      return getTr(bodyElem, row);
    }

    public String getStyleName(int row) {
      return getElement(row).getClassName();
    }

    public boolean isVisible(int row) {
      return DomUtils.isVisible(getElement(row));
    }

    public void removeStyleName(int row, String styleName) {
      if (!BeeUtils.isEmpty(styleName)) {
        getElement(row).removeClassName(styleName);
      }
    }

    public void setStyleName(int row, String styleName) {
      ensureElement(row).setClassName(styleName);
    }

    public void setStyleName(int row, String styleName, boolean add) {
      if (add) {
        addStyleName(row, styleName);
      } else {
        removeStyleName(row, styleName);
      }
    }

    public void setVerticalAlign(int row, VerticalAlign align) {
      StyleUtils.setVerticalAlign(ensureElement(row), align);
    }

    public void setVisible(int row, boolean visible) {
      UIObject.setVisible(getElement(row), visible);
    }

    //@formatter:off
    private native Element getTr(Element elem, int row) /*-{
      return elem.rows[row];
    }-*/;
    //@formatter:on
  }

  public static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "HtmlTable";

  private static final String STYLE_SUFFIX_COL = "-col";
  private static final String STYLE_SUFFIX_CELL = "-cell";

  private final TableElement tableElem;
  private TableSectionElement bodyElem;

  private final ElementMapperImpl<Widget> widgetMap;

  private final CellFormatter cellFormatter;
  private final ColumnFormatter columnFormatter;

  private final RowFormatter rowFormatter;

  private String defaultCellClasses;
  private String defaultCellStyles;

  private final Map<Integer, String> columnCellClases = new HashMap<>();
  private final Map<Integer, String> columnCellStyles = new HashMap<>();

  private String caption;

  public HtmlTable() {
    this.tableElem = Document.get().createTableElement();
    this.bodyElem = Document.get().createTBodyElement();
    tableElem.appendChild(bodyElem);

    setElement(tableElem);

    this.widgetMap = new ElementMapperImpl<>();

    this.cellFormatter = new CellFormatter();
    this.rowFormatter = new RowFormatter();
    this.columnFormatter = new ColumnFormatter();

    setStyleName(STYLE_NAME);

    init();
  }

  public HtmlTable(String styleName) {
    this();
    if (!BeeUtils.isEmpty(styleName)) {
      addStyleName(styleName);
    }
  }

  @Override
  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(handler, ClickEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
    return addDomHandler(handler, MouseMoveEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(handler, MouseOutEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
  }

  public void alignCenter(int row, int column) {
    getCellFormatter().setHorizontalAlignment(row, column, TextAlign.CENTER);
  }

  public void alignLeft(int row, int column) {
    getCellFormatter().setHorizontalAlignment(row, column, TextAlign.LEFT);
  }

  public void alignRight(int row, int column) {
    getCellFormatter().setHorizontalAlignment(row, column, TextAlign.RIGHT);
  }

  @Override
  public void clear() {
    int numRows = getRowCount();
    for (int i = 0; i < numRows; i++) {
      removeRow(0);
    }
  }

  @Override
  public String getCaption() {
    return caption;
  }

  public int getCellCount(int row) {
    checkRowBounds(row);
    return getDOMCellCount(bodyElem, row);
  }

  public CellFormatter getCellFormatter() {
    return cellFormatter;
  }

  public List<TableCellElement> getColumnCells(int column) {
    Assert.nonNegative(column);
    List<TableCellElement> cells = new ArrayList<>();

    for (int row = 0; row < getRowCount(); row++) {
      if (getCellCount(row) > column) {
        cells.add(getCellFormatter().getCell(tableElem, row, column));
      }
    }
    return cells;
  }

  public ColumnFormatter getColumnFormatter() {
    return columnFormatter;
  }

  public Integer getEventRow(GwtEvent<?> event, boolean incl) {
    Integer index = null;

    if (event != null && event.getSource() instanceof Widget) {
      TableRowElement rowElement =
          DomUtils.getParentRow(((Widget) event.getSource()).getElement(), incl);

      while (rowElement != null) {
        TableElement tableElement = DomUtils.getParentTable(rowElement, false);

        if (tableElement == null) {
          break;
        } else if (getId().equals(tableElement.getId())) {
          index = rowElement.getRowIndex();
          break;
        } else {
          rowElement = DomUtils.getParentRow(tableElement, false);
        }
      }
    }

    return index;
  }

  public Element getEventRowElement(GwtEvent<?> event, boolean incl) {
    Integer row = getEventRow(event, incl);
    return (row == null) ? null : getRow(row);
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "table";
  }

  public Element getRow(int row) {
    return rowFormatter.getTr(bodyElem, row);
  }

  public List<TableCellElement> getRowCells(int row) {
    Assert.nonNegative(row);
    List<TableCellElement> cells = new ArrayList<>();

    if (row < getRowCount()) {
      int cc = getCellCount(row);
      for (int column = 0; column < cc; column++) {
        cells.add(getCellFormatter().getCell(tableElem, row, column));
      }
    }
    return cells;
  }

  public int getRowCount() {
    return getDOMRowCount(bodyElem);
  }

  public RowFormatter getRowFormatter() {
    return rowFormatter;
  }

  public Widget getWidget(int row, int column) {
    checkCellBounds(row, column);
    return getWidgetImpl(row, column);
  }

  public Widget getWidgetByElement(Element elem) {
    return (elem == null) ? null : widgetMap.get(elem);
  }

  public int insertRow(int beforeRow) {
    if (beforeRow != getRowCount()) {
      checkRowBounds(beforeRow);
    }
    Element tr = Document.get().createTRElement();
    DOM.insertChild(bodyElem, tr, beforeRow);
    return beforeRow;
  }

  public boolean isEmpty() {
    return getRowCount() <= 0;
  }

  @Override
  public Iterator<Widget> iterator() {
    return new Iterator<Widget>() {
      final ArrayList<Widget> widgetList = widgetMap.getObjectList();
      int lastIndex = -1;
      int nextIndex = -1;
      {
        findNext();
      }

      @Override
      public boolean hasNext() {
        return nextIndex < widgetList.size();
      }

      @Override
      public Widget next() {
        if (!hasNext()) {
          Assert.untouchable("no such element");
        }
        Widget result = widgetList.get(nextIndex);
        lastIndex = nextIndex;
        findNext();
        return result;
      }

      @Override
      public void remove() {
        Assert.state(lastIndex >= 0);
        Widget w = widgetList.get(lastIndex);
        w.removeFromParent();
        lastIndex = -1;
      }

      private void findNext() {
        while (++nextIndex < widgetList.size()) {
          if (widgetList.get(nextIndex) != null) {
            return;
          }
        }
      }
    };
  }

  @Override
  public boolean remove(Widget widget) {
    if (widget.getParent() != this) {
      return false;
    }

    try {
      orphan(widget);
    } finally {
      Element elem = widget.getElement();
      elem.removeFromParent();

      widgetMap.removeByElement(elem);
    }
    return true;
  }

  public void removeRow(int row) {
    int columnCount = getCellCount(row);
    for (int column = 0; column < columnCount; ++column) {
      cleanCell(row, column, false);
    }
    bodyElem.removeChild(rowFormatter.getElement(row));
  }

  @Override
  public void setBorderSpacing(int spacing) {
    StyleUtils.setBorderSpacing(tableElem, spacing);
  }

  public void setCaption(String caption) {
    this.caption = caption;
  }

  public void setColumnCellClasses(int column, String classes) {
    Assert.nonNegative(column);
    if (BeeUtils.isEmpty(classes)) {
      columnCellClases.remove(column);
    } else {
      columnCellClases.put(column, classes);
    }
  }

  public void setColumnCellKind(int column, CellKind cellKind) {
    Assert.nonNegative(column);
    Assert.notNull(cellKind);

    String classes = columnCellClases.get(column);
    if (BeeUtils.isEmpty(classes)) {
      columnCellClases.put(column, cellKind.getStyleName());
    } else {
      columnCellClases.put(column, StyleUtils.buildClasses(classes, cellKind.getStyleName()));
    }
  }

  public void setColumnCellStyles(int column, String styles) {
    Assert.nonNegative(column);
    if (BeeUtils.isEmpty(styles)) {
      columnCellStyles.remove(column);
    } else {
      columnCellStyles.put(column, styles);
    }
  }

  @Override
  public void setDefaultCellClasses(String classes) {
    this.defaultCellClasses = classes;
  }

  @Override
  public void setDefaultCellStyles(String styles) {
    this.defaultCellStyles = styles;
  }

  public void setHtml(int row, int column, String html) {
    prepareCell(row, column);
    Element td = cleanCell(row, column, html == null);
    if (html != null) {
      td.setInnerHTML(html);
    }
  }

  public void setHtml(int row, int column, String html, String... cellStyles) {
    setHtml(row, column, html);
    if (cellStyles != null) {
      for (String cellStyle : cellStyles) {
        getCellFormatter().addStyleName(row, column, cellStyle);
      }
    }
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setKind(TableKind kind) {
    if (kind != null && !BeeUtils.isEmpty(kind.getStyleSuffix())) {
      addStyleName(StyleUtils.joinName(STYLE_NAME, kind.getStyleSuffix()));
    }
  }

  public void setText(int row, int column, String text) {
    prepareCell(row, column);
    Element td = cleanCell(row, column, text == null);
    if (text != null) {
      td.setInnerText(text);
    }
  }

  public void setText(int row, int column, String text, String... cellStyles) {
    setText(row, column, text);
    if (cellStyles != null) {
      for (String cellStyle : cellStyles) {
        getCellFormatter().addStyleName(row, column, cellStyle);
      }
    }
  }

  public void setText(int row, int column, String text, Collection<String> cellStyles) {
    setText(row, column, text);
    if (cellStyles != null) {
      for (String cellStyle : cellStyles) {
        getCellFormatter().addStyleName(row, column, cellStyle);
      }
    }
  }

  public void setValue(int row, int column, int value, String... cellStyles) {
    setText(row, column, Integer.toString(value), cellStyles);
  }

  public void setValue(int row, int column, long value, String... cellStyles) {
    setText(row, column, Long.toString(value), cellStyles);
  }

  public void setWidget(int row, int column, Widget widget) {
    prepareCell(row, column);
    if (widget != null) {
      widget.removeFromParent();

      Element td = cleanCell(row, column, true);

      widgetMap.put(widget);

      td.appendChild(widget.getElement());

      adopt(widget);
    }
  }

  public void setWidget(int row, int column, Widget widget, String cellStyleName) {
    setWidget(row, column, widget);
    getCellFormatter().addStyleName(row, column, cellStyleName);
  }

  public void setWidgetAndStyle(int row, int column, Widget widget, String styleName) {
    widget.addStyleName(styleName);
    setWidget(row, column, widget, styleName + STYLE_SUFFIX_CELL);

    if (row == 0) {
      getColumnFormatter().addStyleName(column, styleName + STYLE_SUFFIX_COL);
    }
  }

  public void transpose() {
    int rc = getRowCount();
    if (rc <= 0 || rc == 1 && getCellCount(0) <= 1) {
      return;
    }

    TableSectionElement newBody = Document.get().createTBodyElement();

    for (int i = 0; i < rc; i++) {
      for (int j = 0; j < getCellCount(i); j++) {
        Element oldTd = getCellFormatter().getElement(i, j);

        Element child = oldTd.getFirstChildElement();
        Widget widget = (child == null) ? null : widgetMap.get(child);

        Element newTd = oldTd.cloneNode(widget == null).cast();
        if (widget != null) {
          newTd.appendChild(child);
        }

        NodeList<TableRowElement> rows = newBody.getRows();
        TableRowElement tr;
        if (rows == null || rows.getLength() <= j) {
          tr = Document.get().createTRElement();
          newBody.appendChild(tr);
        } else {
          tr = rows.getItem(j);
        }

        tr.appendChild(newTd);
      }
    }

    tableElem.removeAllChildren();
    tableElem.appendChild(newBody);

    this.bodyElem = newBody;
  }

  private void checkCellBounds(int row, int column) {
    checkRowBounds(row);
    Assert.nonNegative(column, "Column " + column + " must be non-negative");
    int cellSize = getCellCount(row);
    Assert.isTrue(cellSize > column, "Column index: " + column + ", Column size: " + cellSize);
  }

  private void checkRowBounds(int row) {
    int rowSize = getRowCount();
    Assert.isTrue(row < rowSize && row >= 0, "Row index: " + row + ", Row size: " + rowSize);
  }

  private Element cleanCell(int row, int column, boolean clearInnerHtml) {
    Element td = getCellFormatter().getElement(row, column);
    internalClearCell(td, clearInnerHtml);
    return td;
  }

  private TableCellElement createCell(int column) {
    TableCellElement td = Document.get().createTDElement();

    StyleUtils.updateAppearance(td, getDefaultCellClasses(), getDefaultCellStyles());
    StyleUtils.updateAppearance(td, getColumnCellClasses(column), getColumnCellStyles(column));
    return td;
  }

  private String getColumnCellClasses(int column) {
    return columnCellClases.get(column);
  }

  private String getColumnCellStyles(int column) {
    return columnCellStyles.get(column);
  }

  private String getDefaultCellClasses() {
    return defaultCellClasses;
  }

  private String getDefaultCellStyles() {
    return defaultCellStyles;
  }

//@formatter:off
  private native int getDOMCellCount(Element tableBody, int row) /*-{
    return tableBody.rows[row].cells.length;
  }-*/;

  private native int getDOMRowCount(Element elem) /*-{
    return elem.rows.length;
  }-*/;
//@formatter:on

  private Widget getWidgetImpl(int row, int column) {
    Element td = cellFormatter.getElement(row, column);
    Element child = td.getFirstChildElement();

    if (child == null) {
      return null;
    } else {
      return widgetMap.get(child);
    }
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
  }

  private boolean internalClearCell(Element td, boolean clearInnerHtml) {
    if (td == null) {
      return false;
    }

    Element maybeChild = td.getFirstChildElement();
    Widget widget = null;
    if (maybeChild != null) {
      widget = widgetMap.get(maybeChild);
    }

    if (widget != null) {
      remove(widget);
      return true;
    } else {
      if (clearInnerHtml) {
        td.setInnerHTML(BeeConst.STRING_EMPTY);
      }
      return false;
    }
  }

  private void prepareCell(int row, int column) {
    prepareRow(row);
    Assert.nonNegative(column, "Cannot create a column with a negative index: " + column);

    int cellCount = getCellCount(row);
    int required = column + 1 - cellCount;
    if (required > 0) {
      Element tr = getRow(row);
      for (int i = 0; i < required; i++) {
        tr.appendChild(createCell(column));
      }
    }
  }

  private void prepareRow(int row) {
    Assert.nonNegative(row, "Cannot create a row with a negative index: " + row);

    int rowCount = getRowCount();
    for (int i = rowCount; i <= row; i++) {
      insertRow(i);
    }
  }
}
