package com.butent.bee.client.animation;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.shared.BeeConst;

public interface HasHoverAnimation {

  String NAME_ANIMATE_HOVER = BeeConst.CSS_CLASS_PREFIX + "animate-hover";

  static void init(Widget widget) {
    if (widget instanceof HasHoverAnimation) {
      widget.addStyleName(NAME_ANIMATE_HOVER);
    }
  }
}
