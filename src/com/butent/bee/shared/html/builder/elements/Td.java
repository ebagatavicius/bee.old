package com.butent.bee.shared.html.builder.elements;

import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.builder.FertileElement;
import com.butent.bee.shared.html.builder.Node;

import java.util.List;

public class Td extends FertileElement {

  public Td() {
    super();
  }

  public Td addClass(String value) {
    super.addClassName(value);
    return this;
  }

  public Td append(List<? extends Node> nodes) {
    super.appendChildren(nodes);
    return this;
  }

  public Td alignRight() {
    return textAlign(TextAlign.RIGHT);
  }

  public Td append(Node... nodes) {
    super.appendChildren(nodes);
    return this;
  }

  public Td backgroundColor(String backgroundColor) {
    setBackgroundColor(backgroundColor);
    return this;
  }

  public Td colSpan(int value) {
    setAttribute(Attributes.COL_SPAN, value);
    return this;
  }

  public Td color(String color) {
    setColor(color);
    return this;
  }

  public Td fontSize(FontSize fontSize) {
    setFontSize(fontSize);
    return this;
  }

  public Td fontWeight(FontWeight fontWeight) {
    setFontWeight(fontWeight);
    return this;
  }

  public Td headers(String value) {
    setAttribute(Attributes.HEADERS, value);
    return this;
  }

  public Td id(String value) {
    setId(value);
    return this;
  }

  public Td insert(int index, Node child) {
    super.insertChild(index, child);
    return this;
  }

  public Td lang(String value) {
    setLang(value);
    return this;
  }

  public Td paddingBottom(int value, CssUnit unit) {
    setPaddingBottom(value, unit);
    return this;
  }

  public Td paddingLeft(int value, CssUnit unit) {
    setPaddingLeft(value, unit);
    return this;
  }

  public Td paddingRight(int value, CssUnit unit) {
    setPaddingRight(value, unit);
    return this;
  }

  public Td paddingTop(int value, CssUnit unit) {
    setPaddingTop(value, unit);
    return this;
  }

  public Td remove(Node child) {
    super.removeChild(child);
    return this;
  }

  public Td rowSpan(int value) {
    setAttribute(Attributes.ROW_SPAN, value);
    return this;
  }

  public Td text(String text) {
    super.appendText(text);
    return this;
  }

  public Td textAlign(TextAlign textAlign) {
    setTextAlign(textAlign);
    return this;
  }

  public Td title(String value) {
    setTitle(value);
    return this;
  }

  public Td verticalAlign(VerticalAlign verticalAlign) {
    setVerticalAlign(verticalAlign);
    return this;
  }

  public Td whiteSpace(WhiteSpace whiteSpace) {
    setWhiteSpace(whiteSpace);
    return this;
  }
}
