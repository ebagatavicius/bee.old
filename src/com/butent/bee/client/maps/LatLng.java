package com.butent.bee.client.maps;

import com.google.gwt.core.client.JavaScriptObject;

public class LatLng extends JavaScriptObject {

  public static LatLng create(double lat, double lng) {
    return create(lat, lng, false);
  }

  public static LatLng create(double lat, double lng, boolean noWrap) {
    return createJso(lat, lng, noWrap);
  }

//@formatter:off
  private static native LatLng createJso(double lat, double lng, boolean noWrap) /*-{
    return new $wnd.google.maps.LatLng(lat, lng, noWrap);
  }-*/;
//@formatter:on

  protected LatLng() {
  }

  public final double getLatitude() {
    return getLat();
  }

  public final double getLongitude() {
    return getLng();
  }

//@formatter:off
  public final native String getString() /*-{
    return this.toString();
  }-*/;

  public final native String toUrlValue(int precision) /*-{
    return this.toUrlValue(precision);
  }-*/;

  private native double getLat() /*-{
    return this.lat();
  }-*/;

  private native double getLng() /*-{
    return this.lng();
  }-*/;
//@formatter:on
}
