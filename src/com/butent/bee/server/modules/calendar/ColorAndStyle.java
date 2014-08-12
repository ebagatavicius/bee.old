package com.butent.bee.server.modules.calendar;

import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.BeeUtils;

class ColorAndStyle {

  static ColorAndStyle maybeCreate(String background, String foreground, Long style) {
    if (!BeeUtils.isEmpty(background) || !BeeUtils.isEmpty(foreground) || DataUtils.isId(style)) {
      return new ColorAndStyle(background, foreground, style);
    } else {
      return null;
    }
  }

  private String background;
  private String foreground;

  private Long style;

  ColorAndStyle(String background, String foreground, Long style) {
    this.background = background;
    this.foreground = foreground;
    this.style = style;
  }

  ColorAndStyle copy() {
    return new ColorAndStyle(getBackground(), getForeground(), getStyle());
  }

  String getBackground() {
    return background;
  }

  String getForeground() {
    return foreground;
  }

  Long getStyle() {
    return style;
  }

  void merge(ColorAndStyle other, boolean overwrite) {
    if (other == null) {
      return;
    }

    if (!BeeUtils.isEmpty(other.getBackground())
        && (overwrite || BeeUtils.isEmpty(getBackground()))) {
      setBackground(other.getBackground());
    }

    if (!BeeUtils.isEmpty(other.getForeground())
        && (overwrite || BeeUtils.isEmpty(getForeground()))) {
      setForeground(other.getForeground());
    }

    if (DataUtils.isId(other.getStyle()) && (overwrite || !DataUtils.isId(getStyle()))) {
      setStyle(other.getStyle());
    }
  }

  void setBackground(String background) {
    this.background = background;
  }

  void setForeground(String foreground) {
    this.foreground = foreground;
  }

  void setStyle(Long style) {
    this.style = style;
  }
}
