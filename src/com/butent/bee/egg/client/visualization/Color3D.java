package com.butent.bee.egg.client.visualization;

import com.google.gwt.core.client.JavaScriptObject;

import com.butent.bee.egg.client.ajaxloader.Properties;

public class Color3D extends Properties {
  public static Color3D create() {
    return JavaScriptObject.createObject().<Color3D> cast();
  }

  public static Color3D create(String color, String shadeColor) {
    return create().setFaceColor(color).setShadeColor(shadeColor);
  }

  protected Color3D() {
  }

  public final Color3D setFaceColor(String color) {
    set("color", color);
    return this;
  }

  public final Color3D setShadeColor(String shadeColor) {
    set("darker", shadeColor);
    return this;
  }
}
