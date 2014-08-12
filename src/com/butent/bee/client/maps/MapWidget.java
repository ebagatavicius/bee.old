package com.butent.bee.client.maps;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.RequiresResize;

import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.ReadyEvent.Handler;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.CustomWidget;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class MapWidget extends CustomWidget implements RequiresResize,
    ReadyEvent.HasReadyHandlers {

  public static MapWidget create(MapOptions options) {
    if (options == null) {
      return null;
    } else {
      return new MapWidget(options);
    }
  }

  private MapOptions mapOptions;
  private MapImpl impl;

  private final Collection<Marker> markers = new ArrayList<>();

  private MapWidget(MapOptions options) {
    super(Document.get().createDivElement(), StyleUtils.CLASS_NAME_PREFIX + "Map");
    this.mapOptions = options;
  }

  public boolean addMarker(double lat, double lng, String title) {
    if (impl != null) {
      LatLng position = LatLng.create(lat, lng);
      MarkerOptions markerOptions = MarkerOptions.create(position, getMapImpl());

      if (!BeeUtils.isEmpty(title)) {
        markerOptions.setTitle(title);
      }

      Marker marker = Marker.create(markerOptions);
      markers.add(marker);
      return true;

    } else {
      return false;
    }
  }

  @Override
  public HandlerRegistration addReadyHandler(Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  public void fitBounds(LatLngBounds bounds) {
    if (impl != null) {
      impl.fitBounds(bounds);
    }
  }

  public LatLngBounds getBounds() {
    return (impl == null) ? null : impl.getBounds();
  }

  public LatLng getCenter() {
    return (impl == null) ? null : impl.getCenter();
  }

  public Element getDiv() {
    return (impl == null) ? null : impl.getDiv();
  }

  public int getHeading() {
    return (impl == null) ? BeeConst.UNDEF : impl.getHeading();
  }

  @Override
  public String getIdPrefix() {
    return "map";
  }

  public MapImpl getMapImpl() {
    return impl;
  }

  public MapTypeId getMapTypeId() {
    return (impl == null) ? null : impl.getMapTypeId();
  }

  public int getTilt() {
    return (impl == null) ? BeeConst.UNDEF : impl.getTilt();
  }

  public List<String> getValues() {
    List<String> values = new ArrayList<>();
    LatLng center = getCenter();

    if (center != null) {
      values.add(MapUtils.toString(center.getLatitude()));
      values.add(MapUtils.toString(center.getLongitude()));

      values.add(BeeUtils.toString(getZoom()));

      for (Marker marker : markers) {
        LatLng position = marker.getPosition();

        if (position != null) {
          values.add(MapUtils.toString(position.getLatitude()));
          values.add(MapUtils.toString(position.getLongitude()));

          values.add(marker.getTitle());
        }
      }
    }
    return values;
  }

  public int getZoom() {
    return (impl == null) ? BeeConst.UNDEF : impl.getZoom();
  }

  @Override
  public void onResize() {
    triggerResize();
  }

  public void panBy(int x, int y) {
    if (impl != null) {
      impl.panBy(x, y);
    }
  }

  public void panTo(LatLng latLng) {
    if (impl != null) {
      impl.panTo(latLng);
    }
  }

  public void panToBounds(LatLngBounds latLngBounds) {
    if (impl != null) {
      impl.panToBounds(latLngBounds);
    }
  }

  public void setCenter(LatLng latlng) {
    if (impl != null) {
      impl.setCenter(latlng);
    }
  }

  public void setHeading(int heading) {
    if (impl != null) {
      impl.setHeading(heading);
    }
  }

  public void setMapTypeId(MapTypeId mapTypeId) {
    if (impl != null) {
      impl.setMapTypeId(mapTypeId);
    }
  }

  public void setOptions(MapOptions options) {
    this.mapOptions = options;
    if (impl != null) {
      impl.setOptions(options);
    }
  }

  public void setTilt(int tilt) {
    if (impl != null) {
      impl.setTilt(tilt);
    }
  }

  public void setZoom(int zoom) {
    if (impl != null) {
      impl.setZoom(zoom);
    }
  }

  public void triggerResize() {
    if (impl != null) {
      LatLng center = getCenter();
      impl.triggerResize();
      setCenter(center);
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    this.impl = MapImpl.create(getElement(), mapOptions);

    impl.setOnLoad(new Runnable() {
      @Override
      public void run() {
        ReadyEvent.fire(MapWidget.this);
      }
    });
  }
}
