package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Implements standard label user interface component.
 */

public class Label extends CustomHasHtml {

  public Label() {
    this(false);
  }

  public Label(boolean inline) {
    this(inline ? Document.get().createSpanElement() : Document.get().createDivElement());
  }

  public Label(Element element) {
    super(element);
  }

  public Label(String html) {
    this();
    if (!BeeUtils.isEmpty(html)) {
      setHtml(html);
    }
  }

  public void clear() {
    setHtml(BeeConst.STRING_EMPTY);
  }

  protected String getDefaultStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "Label";
  }

  @Override
  public String getIdPrefix() {
    return "lbl";
  }

  @Override
  protected void init() {
    super.init();
    addStyleName(getDefaultStyleName());
  }
}
