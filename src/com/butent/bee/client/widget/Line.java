package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;

import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssAngle;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.utils.BeeUtils;

public class Line extends CustomWidget {

  private static final String DEFAULT_STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "Line";

  private static final int DECIMALS = 6;

  private static double round(double value) {
    return BeeUtils.round(value, DECIMALS);
  }

  public Line(double x1, double y1, double x2, double y2) {
    this(x1, y1, x2, y2, DEFAULT_STYLE_NAME);
  }

  public Line(double x1, double y1, double x2, double y2, String styleName) {
    super(Document.get().createDivElement(), styleName);
    draw(x1, y1, x2, y2);
  }

  public void draw(double startX, double startY, double endX, double endY) {
    double x1;
    double y1;
    double x2;
    double y2;

    if (startY < endY) {
      x1 = endX;
      y1 = endY;
      x2 = startX;
      y2 = startY;
    } else {
      x1 = startX;
      y1 = startY;
      x2 = endX;
      y2 = endY;
    }

    double a = Math.abs(x1 - x2);
    double b = Math.abs(y1 - y2);

    double sx = (x1 + x2) / 2;
    double sy = (y1 + y2) / 2;

    double width = Math.sqrt(a * a + b * b);

    double x = sx - width / 2;
    double y = sy;

    a = width / 2;
    double c = Math.abs(sx - x);
    b = Math.sqrt((x1 - x) * (x1 - x) + (y1 - y) * (y1 - y));

    double cosb = (b * b - a * a - c * c) / (2 * a * c);
    double rad = Math.acos(cosb);
    double deg = rad * 180 / Math.PI;

    Style style = getElement().getStyle();

    StyleUtils.makeAbsolute(style);
    StyleUtils.setLeft(style, round(x), CssUnit.PX);
    StyleUtils.setTop(style, round(y), CssUnit.PX);

    StyleUtils.setWidth(style, round(width), CssUnit.PX);
    StyleUtils.setTransformRotate(style, round(deg), CssAngle.DEG);
  }

  @Override
  public String getIdPrefix() {
    return "line";
  }
}
