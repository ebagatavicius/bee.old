package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

public class InternalLink extends CustomHasHtml {

  public InternalLink(String html) {
    super(Document.get().createDivElement());
    if (!BeeUtils.isEmpty(html)) {
      setHtml(html);
    }
  }

  @Override
  public String getIdPrefix() {
    return "internal-link";
  }

  @Override
  protected void init() {
    super.init();
    addStyleName(BeeConst.CSS_CLASS_PREFIX + "InternalLink");
  }
}
