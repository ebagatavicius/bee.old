package com.butent.bee.client.widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.html.Tags;

public class Summary extends CustomHasHtml {

  public Summary() {
    super(DomUtils.createElement(Tags.SUMMARY));
  }

  public Summary(String text) {
    this();
    setHtml(text);
  }

  @Override
  public String getIdPrefix() {
    return "summary";
  }

  @Override
  protected void init() {
    super.init();
    addStyleName(BeeConst.CSS_CLASS_PREFIX + "Summary");
  }
}
