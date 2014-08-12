package com.butent.bee.client.maps;

import com.google.gwt.core.client.JavaScriptObject;

public class LatLngBounds extends JavaScriptObject {

  public static final LatLngBounds create(LatLng sw, LatLng ne) {
    return createJso(sw, ne).cast();
  }

//@formatter:off
  private static native JavaScriptObject createJso(LatLng sw, LatLng ne) /*-{
    return new $wnd.google.maps.LatLngBounds(sw, ne);
  }-*/;
//@formatter:on

  protected LatLngBounds() {
  }

//@formatter:off
  public final native boolean contains(LatLng latlng) /*-{
    return this.contains(latlng);
  }-*/;

  public final native LatLngBounds extend(LatLng point) /*-{
    return this.extend(point);
  }-*/;

  public final native LatLng getCenter() /*-{
    return this.getCenter();
  }-*/;

  public final native LatLng getNorthEast() /*-{
    return this.getNorthEast();
  }-*/;

  public final native LatLng getSouthWest() /*-{
    return this.getSouthWest();
  }-*/;

  public final native String getString() /*-{
    return this.toString();
  }-*/;

  public final native boolean intersects(LatLngBounds other) /*-{
    return this.intersects(other);
  }-*/;

  public final native boolean isEmpty() /*-{
    return this.isEmpty();
  }-*/;

  public final native LatLng toSpan() /*-{
    return this.toSpan();
  }-*/;

  public final native String toUrlValue(int precision) /*-{
    return this.toUrlValue(precision);
  }-*/;

  public final native LatLngBounds union(LatLngBounds other) /*-{
    return this.union(other);
  }-*/;
//@formatter:on
}
