package com.butent.bee.client.maps;

import com.google.gwt.core.client.JavaScriptObject;

public class MarkerOptions extends JavaScriptObject {

  public static final MarkerOptions create(LatLng position, MapImpl map) {
    MarkerOptions options = createJso();

    options.setPosition(position);
    options.setMap(map);

    return options;
  }

  private static MarkerOptions createJso() {
    return JavaScriptObject.createObject().cast();
  }

  protected MarkerOptions() {
  }

//@formatter:off
  public final native boolean getClickable() /*-{
    return this.clickable;
  }-*/;

  public final native boolean getCrossOnDrag() /*-{
    return this.crossOnDrag;
  }-*/;

  public final native String getCursor() /*-{
    return this.cursor;
  }-*/;

  public final native boolean getDraggable() /*-{
    return this.draggable;
  }-*/;

  public final native boolean getFlat() /*-{
    return this.flat;
  }-*/;

  public final native boolean getOptimized() /*-{
    return this.optimized;
  }-*/;

  public final native LatLng getPosition() /*-{
    return this.position;
  }-*/;

  public final native boolean getRaiseOnDrag() /*-{
    return this.raiseOnDrag;
  }-*/;

  public final native String getTitle() /*-{
    return this.title;
  }-*/;

  public final native boolean getVisible() /*-{
    return this.visible;
  }-*/;

  public final native int getZindex() /*-{
    return this.zIndex;
  }-*/;

  public final native void setClickable(boolean clickable) /*-{
    this.clickable = clickable;
  }-*/;

  public final native void setCrossOnDrag(boolean crossOnDrag) /*-{
    this.crossOnDrag = crossOnDrag;
  }-*/;

  public final native void setCursor(String cursor) /*-{
    this.cursor = cursor;
  }-*/;

  public final native void setDraggable(boolean draggable) /*-{
    this.draggable = draggable;
  }-*/;

  public final native void setFlat(boolean flat) /*-{
    this.flat = flat;
  }-*/;

  public final native void setIcon(String icon) /*-{
    this.icon = icon;
  }-*/;

  public final native void setOptimized(boolean optimized) /*-{
    this.optimized = optimized;
  }-*/;

  public final native void setPosition(LatLng position) /*-{
    this.position = position;
  }-*/;

  public final native void setRaiseOnDrag(boolean raiseOnDrag) /*-{
    this.raiseOnDrag = raiseOnDrag;
  }-*/;

  public final native void setShadow(String shadow) /*-{
    this.shadow = shadow;
  }-*/;

  public final native void setTitle(String title) /*-{
    this.title = title;
  }-*/;

  public final native void setVisible(boolean visible) /*-{
    this.visible = visible;
  }-*/;

  public final native void setZindex(int zIndex) /*-{
    this.zIndex = zIndex;
  }-*/;

  private native void setMap(MapImpl map) /*-{
    this.map = map;
  }-*/;
//@formatter:on
}
