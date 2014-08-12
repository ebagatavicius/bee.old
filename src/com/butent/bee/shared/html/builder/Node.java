package com.butent.bee.shared.html.builder;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

public abstract class Node {

  protected static String indent(int indent, String string) {
    if (string == null) {
      return BeeConst.STRING_EMPTY;
    } else if (indent < 0) {
      return string;
    } else if (indent == 0) {
      return BeeConst.STRING_EOL + string;
    } else {
      return BeeConst.STRING_EOL + BeeUtils.space(indent) + string;
    }
  }

  private Node parent;

  protected Node() {
  }

  public String build() {
    return build(BeeConst.UNDEF, BeeConst.UNDEF);
  }

  public abstract String build(int indentStart, int indentStep);

  public int index() {
    if (getParent() instanceof FertileElement) {
      return ((FertileElement) getParent()).indexOf(this);
    } else {
      return BeeConst.UNDEF;
    }
  }

  public int getLevel() {
    return (getParent() == null) ? 0 : getParent().getLevel() + 1;
  }

  public Node getParent() {
    return parent;
  }

  public void setParent(Node parent) {
    this.parent = parent;
  }

  @Override
  public String toString() {
    return build();
  }
}
