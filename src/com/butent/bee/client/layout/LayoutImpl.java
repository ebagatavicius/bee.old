package com.butent.bee.client.layout;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;

import com.butent.bee.client.layout.Layout.Layer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.Assert;

class LayoutImpl {

  LayoutImpl() {
    super();
  }

  Element attachChild(Element parent, Element child, Element before) {
    DivElement container = Document.get().createDivElement();
    container.appendChild(child);

    container.getStyle().setPosition(Position.ABSOLUTE);
    container.getStyle().setOverflow(Overflow.HIDDEN);

    fillParent(child);

    Element beforeContainer = null;
    if (before != null) {
      beforeContainer = before.getParentElement();
      Assert.isTrue(beforeContainer.getParentElement() == parent,
          "Element to insert before must be a sibling");
    }
    parent.insertBefore(container, beforeContainer);
    return container;
  }

  void fillParent(Element elem) {
    Style style = elem.getStyle();
    style.setPosition(Position.ABSOLUTE);

    StyleUtils.setLeft(style, 0);
    StyleUtils.setTop(style, 0);
    StyleUtils.setRight(style, 0);
    StyleUtils.setBottom(style, 0);
  }

  void initParent(Element parent) {
    if (!StyleUtils.isPositioned(parent.getStyle())) {
      parent.getStyle().setPosition(Position.RELATIVE);
    }
  }

  void layout(Layer layer) {
    Style style = layer.container.getStyle();

    if (layer.visible) {
      style.clearDisplay();
    } else {
      style.setDisplay(Display.NONE);
    }

    if (layer.setLeft) {
      StyleUtils.setLeft(style, layer.left, layer.leftUnit);
    } else {
      style.clearLeft();
    }

    if (layer.setTop) {
      StyleUtils.setTop(style, layer.top, layer.topUnit);
    } else {
      style.clearTop();
    }

    if (layer.setRight) {
      StyleUtils.setRight(style, layer.right, layer.rightUnit);
    } else {
      style.clearRight();
    }

    if (layer.setBottom) {
      StyleUtils.setBottom(style, layer.bottom, layer.bottomUnit);
    } else {
      style.clearBottom();
    }

    if (layer.setWidth) {
      StyleUtils.setWidth(style, layer.width, layer.widthUnit);
    } else {
      style.clearWidth();
    }

    if (layer.setHeight) {
      StyleUtils.setHeight(style, layer.height, layer.heightUnit);
    } else {
      style.clearHeight();
    }

    style = layer.child.getStyle();
    switch (layer.hPos) {
      case BEGIN:
        StyleUtils.setLeft(style, 0);
        style.clearRight();
        break;
      case END:
        style.clearLeft();
        StyleUtils.setRight(style, 0);
        break;
      case STRETCH:
        StyleUtils.setLeft(style, 0);
        StyleUtils.setRight(style, 0);
        break;
    }

    switch (layer.vPos) {
      case BEGIN:
        StyleUtils.setTop(style, 0);
        style.clearBottom();
        break;
      case END:
        style.clearTop();
        StyleUtils.setBottom(style, 0);
        break;
      case STRETCH:
        StyleUtils.setTop(style, 0);
        StyleUtils.setBottom(style, 0);
        break;
    }
  }

  void removeChild(Element container, Element child) {
    container.removeFromParent();

    if (child.getParentElement() == container) {
      child.removeFromParent();
    }

    Style style = child.getStyle();
    style.clearPosition();
    style.clearLeft();
    style.clearTop();
    style.clearWidth();
    style.clearHeight();
  }
}
