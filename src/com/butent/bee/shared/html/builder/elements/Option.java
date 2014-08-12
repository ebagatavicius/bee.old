package com.butent.bee.shared.html.builder.elements;

import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.builder.FertileElement;

public class Option extends FertileElement {

  public Option() {
    super();
  }

  public Option addClass(String value) {
    super.addClassName(value);
    return this;
  }

  public Option disabled() {
    setAttribute(Attributes.DISABLED, true);
    return this;
  }

  public Option enabled() {
    setAttribute(Attributes.DISABLED, false);
    return this;
  }

  public Option id(String value) {
    setId(value);
    return this;
  }

  public Option label(String value) {
    setAttribute(Attributes.LABEL, value);
    return this;
  }

  public Option lang(String value) {
    setLang(value);
    return this;
  }

  public Option selected() {
    setAttribute(Attributes.SELECTED, true);
    return this;
  }

  public Option text(String text) {
    super.appendText(text);
    return this;
  }

  public Option title(String value) {
    setTitle(value);
    return this;
  }

  public Option value(int value) {
    setAttribute(Attributes.VALUE, value);
    return this;
  }

  public Option value(long value) {
    setAttribute(Attributes.VALUE, value);
    return this;
  }

  public Option value(String value) {
    setAttribute(Attributes.VALUE, value);
    return this;
  }
}
