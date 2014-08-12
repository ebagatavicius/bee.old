package com.butent.bee.client.widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.utils.BeeUtils;

public class FaLabel extends Label {

  public FaLabel(FontAwesome fa) {
    super();
    setChar(fa);
  }

  public FaLabel(FontAwesome fa, boolean inline) {
    super(inline);
    setChar(fa);
  }

  public FaLabel(FontAwesome fa, String styleName) {
    this(fa);
    if (!BeeUtils.isEmpty(styleName)) {
      addStyleName(styleName);
    }
  }

  @Override
  public String getIdPrefix() {
    return "fa";
  }

  public void setChar(FontAwesome fa) {
    if (fa == null) {
      clear();
    } else {
      setText(String.valueOf(fa.getCode()));
    }
  }

  @Override
  protected String getDefaultStyleName() {
    return "bee-fa-label";
  }

  @Override
  protected void init() {
    super.init();

    StyleUtils.setFontFamily(this, FontAwesome.FAMILY);
    DomUtils.preventSelection(this);
  }
}
