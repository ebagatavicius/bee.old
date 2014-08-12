package com.butent.bee.shared.html.builder;

import com.butent.bee.shared.utils.BeeUtils;

public class Text extends Node {

  private String text;

  public Text(String text) {
    super();
    this.text = text;
  }

  @Override
  public String build(int indentStart, int indentStep) {
    return Node.indent(indentStart, text);
  }

  public String getText() {
    return text;
  }

  public boolean isEmpty() {
    return BeeUtils.isEmpty(getText());
  }

  public void setText(String text) {
    this.text = text;
  }
}
