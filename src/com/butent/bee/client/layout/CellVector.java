package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.style.HasTextAlign;
import com.butent.bee.client.style.HasVerticalAlign;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;

public abstract class CellVector extends ComplexPanel implements IdentifiableWidget,
    HasIndexedWidgets, HasTextAlign, HasVerticalAlign, IsHtmlTable {

  private final Element table;
  private final Element body;

  private TextAlign horAlign;
  private VerticalAlign vertAlign;

  private String defaultCellClasses;
  private String defaultCellStyles;

  public CellVector() {
    super();

    table = DOM.createTable();
    body = DOM.createTBody();
    DOM.appendChild(table, body);

    setElement(table);

    DomUtils.createId(table, getIdPrefix());

    table.setClassName(BeeConst.CSS_CLASS_PREFIX + "CellVector");
  }

  @Override
  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(handler, ClickEvent.getType());
  }

  public void addStyleToCell(Widget w, String styleName) {
    Element td = getWidgetTd(w);
    if (td != null) {
      td.addClassName(styleName);
    }
  }

  public Element getCell(int index) {
    return getWidgetTd(getWidget(index));
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public TextAlign getTextAlign() {
    return horAlign;
  }

  @Override
  public VerticalAlign getVerticalAlign() {
    return vertAlign;
  }

  @Override
  public boolean isEmpty() {
    return getWidgetCount() <= 0;
  }

  @Override
  public void setBorderSpacing(int spacing) {
    StyleUtils.setBorderSpacing(table, spacing);
  }

  public void setCellHeight(Widget w, double height, CssUnit unit) {
    Element td = getWidgetTd(w);
    if (td != null) {
      StyleUtils.setHeight(td, height, unit);
    }
  }

  public void setCellHeight(Widget w, int height) {
    Element td = getWidgetTd(w);
    if (td != null) {
      StyleUtils.setHeight(td, height);
    }
  }

  public void setCellHorizontalAlignment(Widget w, TextAlign align) {
    Element td = getWidgetTd(w);
    if (td != null) {
      setCellHorizontalAlignment(td, align);
    }
  }

  public void setCellVerticalAlignment(Widget w, VerticalAlign align) {
    Element td = getWidgetTd(w);
    if (td != null) {
      setCellVerticalAlignment(td, align);
    }
  }

  public void setCellWidth(Widget w, double width, CssUnit unit) {
    Element td = getWidgetTd(w);
    if (td != null) {
      StyleUtils.setWidth(td, width, unit);
    }
  }

  public void setCellWidth(Widget w, int width) {
    Element td = getWidgetTd(w);
    if (td != null) {
      StyleUtils.setWidth(td, width);
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

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  public void setTextAlign(TextAlign align) {
    this.horAlign = align;
  }

  @Override
  public void setVerticalAlign(VerticalAlign align) {
    this.vertAlign = align;
  }

  protected Element createDefaultCell() {
    Element td = DOM.createTD();

    StyleUtils.updateAppearance(td, getDefaultCellClasses(), getDefaultCellStyles());

    if (getTextAlign() != null) {
      setCellHorizontalAlignment(td, getTextAlign());
    }
    if (getVerticalAlign() != null) {
      setCellVerticalAlignment(td, getVerticalAlign());
    }

    return td;
  }

  protected Element getBody() {
    return body;
  }

  protected Element getTable() {
    return table;
  }

  protected void setCellHorizontalAlignment(Element td, TextAlign align) {
    if (align != null) {
      StyleUtils.setTextAlign(td, align);
    }
  }

  protected void setCellVerticalAlignment(Element td, VerticalAlign align) {
    if (align != null) {
      StyleUtils.setVerticalAlign(td, align);
    }
  }

  private String getDefaultCellClasses() {
    return defaultCellClasses;
  }

  private String getDefaultCellStyles() {
    return defaultCellStyles;
  }

  private Element getWidgetTd(Widget w) {
    if (w.getParent() == this) {
      return w.getElement().getParentElement();
    } else {
      return null;
    }
  }
}
