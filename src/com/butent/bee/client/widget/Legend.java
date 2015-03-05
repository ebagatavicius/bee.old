package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;

import com.butent.bee.shared.BeeConst;

public class Legend extends CustomHasHtml {

  public Legend() {
    super(Document.get().createLegendElement());
  }

  public Legend(String text) {
    this();
    setHtml(text);
  }

  @Override
  public String getIdPrefix() {
    return "legend";
  }

  @Override
  protected void init() {
    super.init();
    addStyleName(BeeConst.CSS_CLASS_PREFIX + "Legend");
  }
}
