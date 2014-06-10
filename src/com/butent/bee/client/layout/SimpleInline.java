package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Enables to manage a inline panel which contains only one child component.
 */

public class SimpleInline extends Panel implements HasOneWidget, IdentifiableWidget {

  private Widget widget;

  public SimpleInline() {
    this(DOM.createSpan());
  }

  public SimpleInline(Element elem) {
    setElement(elem);
    init();
  }

  public SimpleInline(Widget child) {
    this();
    setWidget(child);
  }

  @Override
  public void add(Widget w) {
    Assert.isNull(getWidget(), "Simple Inline Panel can only contain one child widget");
    setWidget(w);
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "simple-inline";
  }

  @Override
  public Widget getWidget() {
    return widget;
  }

  @Override
  public Iterator<Widget> iterator() {
    return new Iterator<Widget>() {
      boolean hasElement = widget != null;
      Widget returned;

      @Override
      public boolean hasNext() {
        return hasElement;
      }

      @Override
      public Widget next() {
        if (!hasElement || (widget == null)) {
          throw new NoSuchElementException();
        }
        hasElement = false;
        returned = widget;
        return returned;
      }

      @Override
      public void remove() {
        if (returned != null) {
          SimpleInline.this.remove(returned);
        }
      }
    };
  }

  @Override
  public boolean remove(Widget w) {
    if (widget != w) {
      return false;
    }

    try {
      orphan(w);
    } finally {
      getElement().removeChild(w.getElement());
      widget = null;
    }
    return true;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  public void setWidget(IsWidget w) {
    setWidget(asWidgetOrNull(w));
  }

  @Override
  public void setWidget(Widget w) {
    if (w == widget) {
      return;
    }

    if (w != null) {
      w.removeFromParent();
    }
    if (widget != null) {
      remove(widget);
    }

    widget = w;

    if (w != null) {
      getElement().appendChild(widget.getElement());
      adopt(w);
    }
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
  }
}
