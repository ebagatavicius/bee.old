package com.butent.bee.client.widget;

import com.butent.bee.client.animation.HasHoverAnimation;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.utils.BeeUtils;

public class FaLabel extends AnimatableLabel implements HasHoverAnimation {

  public static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "fa-label";
  private static final String STYLE_DISABLED = STYLE_NAME + "-" + StyleUtils.SUFFIX_DISABLED;

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
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    setStyleName(STYLE_DISABLED, !enabled);
  }

  @Override
  protected String getDefaultStyleName() {
    return STYLE_NAME;
  }

  @Override
  protected void init() {
    super.init();

    StyleUtils.setFontFamily(this, FontAwesome.FAMILY);
    DomUtils.preventSelection(this);
  }
}
