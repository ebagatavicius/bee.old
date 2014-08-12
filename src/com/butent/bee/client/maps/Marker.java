package com.butent.bee.client.maps;

import com.google.gwt.core.client.JavaScriptObject;

public class Marker extends JavaScriptObject {

  public static Marker create(MarkerOptions options) {
    return createJso(options);
  }

//@formatter:off
  private static native Marker createJso(MarkerOptions options) /*-{
    return new $wnd.google.maps.Marker(options);
  }-*/;
//@formatter:on

  protected Marker() {
  }

//@formatter:off
  public final native void clear() /*-{
    this.setMap();
  }-*/;

  public final native boolean getClickable() /*-{
    return this.getClickable();
  }-*/;

  public final native String getCursor() /*-{
    return this.getCursor();
  }-*/;

  public final native boolean getDraggable() /*-{
    return this.getDraggable();
  }-*/;

  public final native boolean getFlat() /*-{
    return this.getFlat();
  }-*/;

  public final native LatLng getPosition() /*-{
    return this.getPosition();
  }-*/;

  public final native String getTitle() /*-{
    return this.getTitle();
  }-*/;

  public final native boolean getVisible() /*-{
    return this.getVisible();
  }-*/;

  public final native int getZindex() /*-{
    return this.getZIndex();
  }-*/;

  public final native void setClickable(boolean clickable) /*-{
    this.setClickable(clickable);
  }-*/;

  public final native void setCursor(String cursor) /*-{
    this.setCursor(cursor);
  }-*/;

  public final native void setDraggable(boolean draggable) /*-{
    this.setDraggable(draggable);
  }-*/;

  public final native void setFlat(boolean flat) /*-{
    this.setFlat(flat);
  }-*/;

  public final native void setIcon(String icon) /*-{
    this.setIcon(icon);
  }-*/;

  public final native void setOptions(MarkerOptions options) /*-{
    this.setOptions(options);
  }-*/;

  public final native void setPosition(LatLng latlng) /*-{
    this.setPosition(latlng);
  }-*/;

  public final native void setShadow(String shadow) /*-{
    this.setShadow(shadow);
  }-*/;

  public final native void setTitle(String title) /*-{
    this.setTitle(title);
  }-*/;

  public final native void setVisible(boolean visible) /*-{
    this.setVisible(visible);
  }-*/;

  public final native void setZindex(int number) /*-{
    this.setZIndex(number);
  }-*/;

  private native void setMap(MapImpl map) /*-{
    this.setMap(map);
  }-*/;
//@formatter:on
}
