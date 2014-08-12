package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.css.CssUnit;

/**
 * Implements a class for panels that can contain multiple child widgets and manages their creation
 * at the specific areas of the layout.
 */

public class Complex extends CustomComplex {

  public Complex() {
    this(Position.ABSOLUTE);
  }

  public Complex(Position position) {
    this(position, Overflow.HIDDEN);
  }

  public Complex(Position position, Overflow overflow) {
    super(Document.get().createDivElement());
    initStyle(position, overflow);
  }

  public void add(Widget child, Edges edges) {
    Assert.notNull(child);
    if (edges != null && !edges.isEmpty()) {
      setChildPosition(child);
      edges.applyPosition(child);
    }
    add(child);
  }

  public void addLeftBottom(Widget child, double left, CssUnit leftCssUnit,
      double bottom, CssUnit bottomCssUnit) {
    Assert.notNull(child);
    setChildPosition(child);
    setChildLeft(child, left, leftCssUnit);
    setChildBottom(child, bottom, bottomCssUnit);
    add(child);
  }

  public void addLeftBottom(Widget child, int left, int bottom) {
    addLeftBottom(child, left, CssUnit.PX, bottom, CssUnit.PX);
  }

  public void addLeftRightTop(Widget child, double left, CssUnit leftCssUnit,
      double right, CssUnit rightCssUnit, double top, CssUnit topCssUnit) {
    Assert.notNull(child);
    setChildPosition(child);
    setChildLeft(child, left, leftCssUnit);
    setChildRight(child, right, rightCssUnit);
    setChildTop(child, top, topCssUnit);
    add(child);
  }

  public void addLeftRightTop(Widget child, int left, int right, int top) {
    addLeftRightTop(child, left, CssUnit.PX, right, CssUnit.PX, top, CssUnit.PX);
  }

  public void addLeftRightTopBottom(Widget child, double left, CssUnit leftCssUnit,
      double right, CssUnit rightCssUnit, double top, CssUnit topCssUnit,
      double bottom, CssUnit bottomCssUnit) {
    Assert.notNull(child);
    setChildPosition(child);
    setChildLeft(child, left, leftCssUnit);
    setChildRight(child, right, rightCssUnit);
    setChildTop(child, top, topCssUnit);
    setChildBottom(child, bottom, bottomCssUnit);
    add(child);
  }

  public void addLeftRightTopBottom(Widget child, int left, int right, int top, int bottom) {
    addLeftRightTopBottom(child, left, CssUnit.PX, right, CssUnit.PX, top, CssUnit.PX,
        bottom, CssUnit.PX);
  }

  public void addLeftRightTopHeight(Widget child, double left, CssUnit leftCssUnit,
      double right, CssUnit rightCssUnit, double top, CssUnit topCssUnit,
      double height, CssUnit heightCssUnit) {
    Assert.notNull(child);
    setChildPosition(child);
    setChildLeft(child, left, leftCssUnit);
    setChildRight(child, right, rightCssUnit);
    setChildTop(child, top, topCssUnit);
    setChildHeight(child, height, heightCssUnit);
    add(child);
  }

  public void addLeftRightTopHeight(Widget child, int left, int right, int top, int height) {
    addLeftRightTopHeight(child, left, CssUnit.PX, right, CssUnit.PX, top, CssUnit.PX,
        height, CssUnit.PX);
  }

  public void addLeftTop(Widget child, double left, CssUnit leftCssUnit,
      double top, CssUnit topCssUnit) {
    Assert.notNull(child);
    setChildPosition(child);
    setChildLeft(child, left, leftCssUnit);
    setChildTop(child, top, topCssUnit);
    add(child);
  }

  public void addLeftTop(Widget child, int left, int top) {
    addLeftTop(child, left, CssUnit.PX, top, CssUnit.PX);
  }

  public void addLeftWidthTopBottom(Widget child, double left, CssUnit leftCssUnit,
      double width, CssUnit widthCssUnit, double top, CssUnit topCssUnit,
      double bottom, CssUnit bottomCssUnit) {
    Assert.notNull(child);
    setChildPosition(child);
    setChildLeft(child, left, leftCssUnit);
    setChildWidth(child, width, widthCssUnit);
    setChildTop(child, top, topCssUnit);
    setChildBottom(child, bottom, bottomCssUnit);
    add(child);
  }

  public void addLeftWidthTopBottom(Widget child, int left, int width, int top, int bottom) {
    addLeftWidthTopBottom(child, left, CssUnit.PX, width, CssUnit.PX, top, CssUnit.PX,
        bottom, CssUnit.PX);
  }

  public void addRightBottom(Widget child, double right, CssUnit rightCssUnit,
      double bottom, CssUnit bottomCssUnit) {
    Assert.notNull(child);
    setChildPosition(child);
    setChildRight(child, right, rightCssUnit);
    setChildBottom(child, bottom, bottomCssUnit);
    add(child);
  }

  public void addRightBottom(Widget child, int right, int bottom) {
    addRightBottom(child, right, CssUnit.PX, bottom, CssUnit.PX);
  }

  public void addRightTop(Widget child, double right, CssUnit rightCssUnit,
      double top, CssUnit topCssUnit) {
    Assert.notNull(child);
    setChildPosition(child);
    setChildRight(child, right, rightCssUnit);
    setChildTop(child, top, topCssUnit);
    add(child);
  }

  public void addRightTop(Widget child, int right, int top) {
    addRightTop(child, right, CssUnit.PX, top, CssUnit.PX);
  }

  public void addTopBottomFillHorizontal(Widget child, double top, CssUnit topCssUnit,
      double bottom, CssUnit bottomCssUnit) {
    addLeftRightTopBottom(child, 0, CssUnit.PX, 0, CssUnit.PX, top, topCssUnit,
        bottom, bottomCssUnit);
  }

  public void addTopBottomFillHorizontal(Widget child, int top, int bottom) {
    addTopBottomFillHorizontal(child, top, CssUnit.PX, bottom, CssUnit.PX);
  }

  public void addTopHeightFillHorizontal(Widget child, double top, CssUnit topCssUnit,
      double height, CssUnit heightCssUnit) {
    addLeftRightTopHeight(child, 0, CssUnit.PX, 0, CssUnit.PX, top, topCssUnit,
        height, heightCssUnit);
  }

  public void addTopHeightFillHorizontal(Widget child, int top, int height) {
    addTopHeightFillHorizontal(child, top, CssUnit.PX, height, CssUnit.PX);
  }

  @Override
  public String getIdPrefix() {
    return "complex";
  }

  private void initStyle(Position position, Overflow overflow) {
    if (position != null) {
      getElement().getStyle().setPosition(position);
    }
    if (overflow != null) {
      getElement().getStyle().setOverflow(overflow);
    }
  }

  private static void setChildBottom(Widget child, double value, CssUnit unit) {
    StyleUtils.setBottom(child, value, unit);
  }

  private static void setChildHeight(Widget child, double value, CssUnit unit) {
    StyleUtils.setHeight(child, value, unit);
  }

  private static void setChildLeft(Widget child, double value, CssUnit unit) {
    StyleUtils.setLeft(child, value, unit);
  }

  private static void setChildPosition(Widget child) {
    child.getElement().getStyle().setPosition(Position.ABSOLUTE);
  }

  private static void setChildRight(Widget child, double value, CssUnit unit) {
    StyleUtils.setRight(child, value, unit);
  }

  private static void setChildTop(Widget child, double value, CssUnit unit) {
    StyleUtils.setTop(child, value, unit);
  }

  private static void setChildWidth(Widget child, double value, CssUnit unit) {
    StyleUtils.setWidth(child, value, unit);
  }
}
