package com.butent.bee.shared.html.builder;

import com.butent.bee.shared.BeeConst;

public class Comment extends Node {

  private final String text;

  public Comment(String text) {
    super();
    this.text = text;
  }

  @Override
  public String build(int indentStart, int indentStep) {
    if (text == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return Node.indent(indentStart, "<!-- " + text + " -->");
    }
  }

  public String getText() {
    return text;
  }
}
