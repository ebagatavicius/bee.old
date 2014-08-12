package com.butent.bee.client.maps;

import com.google.gwt.core.client.JavaScriptObject;

import com.butent.bee.shared.utils.EnumUtils;

public class MapOptions extends JavaScriptObject {

  public static final int DEFAULT_ZOOM = 10;

  public static final MapOptions create(LatLng center) {
    return create(center, DEFAULT_ZOOM);
  }

  public static final MapOptions create(LatLng center, int zoom) {
    MapOptions options = createJso();

    options.setCenter(center);
    options.setZoom(zoom);

    return options;
  }

  private static MapOptions createJso() {
    return JavaScriptObject.createObject().cast();
  }

  protected MapOptions() {
  }

//@formatter:off
  public final native String getBackgroundColor() /*-{
    return this.backgroundColor;
  }-*/;

  public final native LatLng getCenter() /*-{
    return this.center;
  }-*/;

  public final native boolean getDisableDefaultUi() /*-{
    return this.disableDefaultUI;
  }-*/;

  public final native boolean getDisableDoubleClickZoom() /*-{
    return this.disableDoubleClickZoom;
  }-*/;

  public final native boolean getDraggable() /*-{
    return this.draggable;
  }-*/;

  public final native String getDraggableCursor() /*-{
    return this.draggableCursor;
  }-*/;

  public final native String getDraggingCursor() /*-{
    return this.draggingCursor;
  }-*/;

  public final native int getHeading() /*-{
    return this.heading;
  }-*/;

  public final native boolean getKeyboardShortcuts() /*-{
    return this.keyboardShortcuts;
  }-*/;

  public final native boolean getMapMaker() /*-{
    return this.mapMaker;
  }-*/;

  public final native boolean getMapTypeControl() /*-{
    return this.mapTypeControl;
  }-*/;
//@formatter:on

  public final MapTypeId getMapTypeId() {
    String type = getMapTypeIdJs();
    return EnumUtils.getEnumByName(MapTypeId.class, type);
  }

//@formatter:off
  public final native int getMaxZoom() /*-{
    return this.maxZoom;
  }-*/;

  public final native int getMinZoom() /*-{
    return this.minZoom;
  }-*/;

  public final native boolean getNoClear() /*-{
    return this.noClear;
  }-*/;

  public final native boolean getOverviewMapControl() /*-{
    return this.overviewMapControl;
  }-*/;

  public final native boolean getPanControl() /*-{
    return this.panControl;
  }-*/;

  public final native boolean getRotateControl() /*-{
    return this.rotateControl;
  }-*/;

  public final native boolean getScaleControl() /*-{
    return this.scaleControl;
  }-*/;

  public final native boolean getScrollWheel() /*-{
    return this.scrollwheel;
  }-*/;

  public final native boolean getStreetViewControl() /*-{
    return this.streetViewControl;
  }-*/;

  public final native int getTilt() /*-{
    return this.tilt;
  }-*/;

  public final native int getZoom() /*-{
    return this.zoom;
  }-*/;

  public final native boolean getZoomControl() /*-{
    return this.zoomControl;
  }-*/;

  public final native void setBackgroundColor(String backgroundColor) /*-{
    this.backgroundColor = backgroundColor;
  }-*/;

  public final native void setCenter(LatLng center) /*-{
    this.center = center;
  }-*/;

  public final native void setDisableDefaultUi(boolean disableDefaultUI) /*-{
    this.disableDefaultUI = disableDefaultUI;
  }-*/;

  public final native void setDisableDoubleClickZoom(boolean disableDoubleClickZoom) /*-{
    this.disableDoubleClickZoom = disableDoubleClickZoom;
  }-*/;

  public final native void setDraggable(boolean draggable) /*-{
    this.draggable = draggable;
  }-*/;

  public final native void setDraggableCursor(String draggableCursor) /*-{
    this.draggableCursor = draggableCursor;
  }-*/;

  public final native void setDraggingCursor(String draggingCursor) /*-{
    this.draggingCursor = draggingCursor;
  }-*/;

  public final native void setHeading(int heading) /*-{
    this.heading = heading;
  }-*/;

  public final native void setKeyboardShortcuts(boolean keyboardShortcuts) /*-{
    this.keyboardShortcuts = keyboardShortcuts;
  }-*/;

  public final native void setMapMaker(boolean mapMaker) /*-{
    this.mapMaker = mapMaker;
  }-*/;

  public final native void setMapTypeControl(boolean mapTypeControl) /*-{
    this.mapTypeControl = mapTypeControl;
  }-*/;
//@formatter:on

  public final void setMapTypeId(MapTypeId mapTypeId) {
    setMapTypeIdJs(mapTypeId.name());
  }

//@formatter:off
  public final native void setMaxZoom(int maxZoom) /*-{
    this.maxZoom = maxZoom;
  }-*/;

  public final native void setMinZoom(int minZoom) /*-{
    this.minZoom = minZoom;
  }-*/;

  public final native void setNoClear(boolean noClear) /*-{
    this.noClear = noClear;
  }-*/;

  public final native void setOverviewMapControl(boolean overviewMapControl) /*-{
    this.overviewMapControl = overviewMapControl;
  }-*/;

  public final native void setPanControl(boolean panControl) /*-{
    this.panControl = panControl;
  }-*/;

  public final native void setRotateControl(boolean rotateControl) /*-{
    this.rotateControl = rotateControl;
  }-*/;

  public final native void setScaleControl(boolean scaleControl) /*-{
    this.scaleControl = scaleControl;
  }-*/;

  public final native void setScrollWheel(boolean scrollWheel) /*-{
    this.scrollwheel = scrollWheel;
  }-*/;

  public final native void setStreetViewControl(boolean streetViewControl) /*-{
    this.streetViewControl = streetViewControl;
  }-*/;

  public final native void setTilt(int tilt) /*-{
    this.tilt = tilt;
  }-*/;

  public final native void setZoom(int zoom) /*-{
    this.zoom = zoom;
  }-*/;

  public final native void setZoomControl(boolean zoomControl) /*-{
    this.zoomControl = zoomControl;
  }-*/;

  private native String getMapTypeIdJs() /*-{
    return this.mapTypeId;
  }-*/;

  private native void setMapTypeIdJs(String type) /*-{
    this.mapTypeId = $wnd.google.maps.MapTypeId[type];
  }-*/;
//@formatter:on
}
