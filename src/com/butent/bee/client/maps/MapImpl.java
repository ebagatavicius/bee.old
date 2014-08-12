package com.butent.bee.client.maps;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

import com.butent.bee.shared.utils.EnumUtils;

public class MapImpl extends JavaScriptObject {

  public static final MapImpl create(Element element, MapOptions options) {
    return createJso(element, options);
  }

//@formatter:off
  private static native MapImpl createJso(Element element, MapOptions options) /*-{
    return new $wnd.google.maps.Map(element, options);
  }-*/;
//@formatter:on

  protected MapImpl() {
  }

//@formatter:off
  public final native void fitBounds(LatLngBounds bounds) /*-{
    this.fitBounds(bounds);
  }-*/;

  public final native LatLngBounds getBounds() /*-{
    return this.getBounds();
  }-*/;

  public final native LatLng getCenter() /*-{
    return this.getCenter();
  }-*/;

  public final native Element getDiv() /*-{
    return this.getDiv();
  }-*/;

  public final native int getHeading() /*-{
    return this.getHeading() || 0;
  }-*/;
//@formatter:on

  public final MapTypeId getMapTypeId() {
    return EnumUtils.getEnumByName(MapTypeId.class, getMapTypeIdImpl());
  }

//@formatter:off
  public final native int getTilt() /*-{
    return this.getTilt();
  }-*/;

  public final native int getZoom() /*-{
    return this.getZoom();
  }-*/;

  public final native void panBy(int x, int y) /*-{
    this.panBy(x, y);
  }-*/;

  public final native void panTo(LatLng latLng) /*-{
    this.panTo(latLng);
  }-*/;

  public final native void panToBounds(LatLngBounds latLngBounds) /*-{
    this.panToBounds(latLngBounds);
  }-*/;

  public final native void setCenter(LatLng latlng) /*-{
    this.setCenter(latlng);
  }-*/;

  public final native void setHeading(int heading) /*-{
    this.setHeading(heading);
  }-*/;
//@formatter:on

  public final void setMapTypeId(MapTypeId mapTypeId) {
    setMapTypeIdImpl(mapTypeId.name());
  }

//@formatter:off
  public final native void setOnLoad(Runnable callback) /*-{
    var fnc = $entry(function() {
      callback.@java.lang.Runnable::run()();
    });

    $wnd.google.maps.event.addListenerOnce(this, 'idle', fnc);
  }-*/;

  public final native void setOptions(MapOptions options) /*-{
    this.setOptions(options);
  }-*/;

  public final native void setTilt(int tilt) /*-{
    this.setTilt(tilt);
  }-*/;

  public final native void setZoom(int zoom) /*-{
    this.setZoom(zoom);
  }-*/;

  public final native void triggerResize() /*-{
    $wnd.google.maps.event.trigger(this, 'resize');
  }-*/;

  private native String getMapTypeIdImpl() /*-{
    return this.getMapTypeId();
  }-*/;

  private native void setMapTypeIdImpl(String mapTypeId) /*-{
    this.setMapTypeId(mapTypeId);
  }-*/;
//@formatter:on
}
