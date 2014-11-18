package com.butent.bee.shared.html.builder.elements;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.builder.FertileElement;
import com.butent.bee.shared.html.builder.Node;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Iframe extends FertileElement {

  public enum Sandbox {
    ALLOW_FORMS("allow-forms"),
    ALLOW_POINTER_LOCK("allow-pointer-lock"),
    ALLOW_POPUPS("allow-popups"),
    ALLOW_SAME_ORIGIN("allow-same-origin"),
    ALLOW_SCRIPTS("allow-scripts"),
    ALLOW_TOP_NAVIGATION("allow-top-navigation");

    private final String keyword;

    private Sandbox(String keyword) {
      this.keyword = keyword;
    }

    public String getKeyword() {
      return keyword;
    }
  }

  public Iframe() {
    super();
  }

  public Iframe addClass(String value) {
    super.addClassName(value);
    return this;
  }

  public Iframe allowFullScreen() {
    setAttribute(Attributes.ALLOW_FULL_SCREEN, true);
    return this;
  }

  public Iframe append(List<? extends Node> nodes) {
    super.appendChildren(nodes);
    return this;
  }

  public Iframe append(Node... nodes) {
    super.appendChildren(nodes);
    return this;
  }

  public Iframe height(int value) {
    setAttribute(Attributes.HEIGHT, value);
    return this;
  }

  public Iframe id(String value) {
    setId(value);
    return this;
  }

  public Iframe insert(int index, Node child) {
    super.insertChild(index, child);
    return this;
  }

  public Iframe lang(String value) {
    setLang(value);
    return this;
  }

  public Iframe name(String value) {
    setAttribute(Attributes.NAME, value);
    return this;
  }

  public Iframe remove(Node child) {
    super.removeChild(child);
    return this;
  }

  public Iframe sandbox(Collection<Sandbox> values) {
    Set<String> kwds = new HashSet<>();

    if (!BeeUtils.isEmpty(values)) {
      for (Sandbox sandbox : values) {
        if (sandbox != null) {
          kwds.add(sandbox.getKeyword());
        }
      }
    }

    if (kwds.isEmpty()) {
      removeAttribute(Attributes.SANDBOX);
    } else {
      setAttribute(Attributes.SANDBOX, BeeUtils.join(BeeConst.STRING_SPACE, kwds));
    }

    return this;
  }

  public Iframe seamless() {
    setAttribute(Attributes.SEAMLESS, true);
    return this;
  }

  public Iframe src(String value) {
    setAttribute(Attributes.SRC, value);
    return this;
  }

  public Iframe srcDoc(String value) {
    setAttribute(Attributes.SRC_DOC, value);
    return this;
  }

  public Iframe text(String text) {
    super.appendText(text);
    return this;
  }

  public Iframe title(String value) {
    setTitle(value);
    return this;
  }

  public Iframe width(int value) {
    setAttribute(Attributes.WIDTH, value);
    return this;
  }
}
