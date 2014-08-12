package com.butent.bee.client.maps;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.geolocation.client.Geolocation.PositionOptions;
import com.google.gwt.geolocation.client.Geolocation;
import com.google.gwt.geolocation.client.Position;
import com.google.gwt.geolocation.client.PositionError;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public final class MapUtils {

  private static final class MarkerData {
    private final double lat;
    private final double lng;

    private final String title;

    private MarkerData(double lat, double lng, String title) {
      this.lat = lat;
      this.lng = lng;
      this.title = title;
    }

    private double getLat() {
      return lat;
    }

    private double getLng() {
      return lng;
    }

    private String getTitle() {
      return title;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(MapUtils.class);

  private static final boolean DEFAULT_POSITION_HIGH_ACCURACY = true;
  private static final int DEFAULT_POSITION_MAXIMUM_AGE = 0;
  private static final int DEFAULT_POSITION_TIMEOUT = 10000;

  private static final int DEFAULT_POSITION_ZOOM = 14;

  public static PositionOptions defaultPositionOptions() {
    return new PositionOptions()
        .setHighAccuracyEnabled(DEFAULT_POSITION_HIGH_ACCURACY)
        .setMaximumAge(DEFAULT_POSITION_MAXIMUM_AGE)
        .setTimeout(DEFAULT_POSITION_TIMEOUT);
  }

  public static void getCurrentPosition(Consumer<Position> onPosition) {
    getCurrentPosition(onPosition, null, defaultPositionOptions());
  }

  public static void getCurrentPosition(Consumer<Position> onPosition, Consumer<String> onError) {
    getCurrentPosition(onPosition, onError, defaultPositionOptions());
  }

  public static void getCurrentPosition(Consumer<Position> onPosition, Consumer<String> onError,
      PositionOptions options) {
    Assert.notNull(onPosition);

    Geolocation geolocation = Geolocation.getIfSupported();
    if (geolocation == null) {
      String message = "geolocation not supported";
      logger.warning(message);

      if (onError != null) {
        onError.accept(message);
      }
      return;
    }

    geolocation.getCurrentPosition(locationCallback(onPosition, onError), options);
  }

  public static Callback<Position, PositionError> locationCallback(
      final Consumer<Position> onPosition, final Consumer<String> onError) {

    return new Callback<Position, PositionError>() {
      @Override
      public void onSuccess(Position result) {
        if (onPosition != null && result != null && result.getCoordinates() != null) {
          onPosition.accept(result);
        }
      }

      @Override
      public void onFailure(PositionError reason) {
        String message = (reason == null) ? "geolocation failure"
            : BeeUtils.joinOptions("code", BeeUtils.toString(reason.getCode()),
                "message", reason.getMessage());
        logger.warning(message);

        if (onError != null) {
          onError.accept(message);
        }
      }
    };
  }

  public static void open(String key, final ViewCallback callback) {
    Assert.notEmpty(key);
    Assert.notNull(callback);

    String[] arr = Codec.beeDeserializeCollection(key);
    int length = ArrayUtils.length(arr);
    Assert.minLength(length, 4);

    int i = 0;

    final String caption = arr[i++];

    final double latitude = BeeUtils.toDouble(arr[i++]);
    final double longitude = BeeUtils.toDouble(arr[i++]);

    final int zoom = BeeUtils.toInt(arr[i++]);

    final List<MarkerData> markerData = new ArrayList<>();

    while (i + 2 < length) {
      markerData.add(new MarkerData(BeeUtils.toDouble(arr[i]), BeeUtils.toDouble(arr[i + 1]),
          arr[i + 2]));
      i += 3;
    }

    ApiLoader.ensureApi(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        LatLng latLng = LatLng.create(latitude, longitude);
        MapOptions mapOptions = (zoom >= 0) ? MapOptions.create(latLng, zoom)
            : MapOptions.create(latLng);

        final MapWidget widget = MapWidget.create(mapOptions);

        if (widget != null) {
          if (!markerData.isEmpty()) {
            widget.addAttachHandler(new AttachEvent.Handler() {
              @Override
              public void onAttachOrDetach(AttachEvent event) {
                if (event.isAttached() && widget.getMapImpl() != null) {
                  for (MarkerData md : markerData) {
                    widget.addMarker(md.getLat(), md.getLng(), md.getTitle());
                  }
                }
              }
            });
          }

          MapContainer container = new MapContainer(caption, widget);
          callback.onSuccess(container);

        } else {
          callback.onFailure("cannot create map");
        }
      }
    });
  }

  public static void showPosition(String caption, double latitude, double longitude, String title) {
    showPosition(caption, latitude, longitude, DEFAULT_POSITION_ZOOM, title);
  }

  public static void showPosition(final String caption, final double latitude,
      final double longitude, final int zoom, final String title) {

    ApiLoader.ensureApi(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        LatLng latLng = LatLng.create(latitude, longitude);
        MapOptions mapOptions = (zoom >= 0) ? MapOptions.create(latLng, zoom)
            : MapOptions.create(latLng);

        final MapWidget widget = MapWidget.create(mapOptions);

        if (widget != null) {
          widget.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
              if (event.isAttached() && widget.getMapImpl() != null) {
                widget.addMarker(latitude, longitude, title);
              }
            }
          });

          MapContainer container = new MapContainer(caption, widget);
          BeeKeeper.getScreen().show(container);
        }
      }
    });
  }

  public static String toString(double d) {
    return BeeUtils.toString(d, 7);
  }

  private MapUtils() {
  }
}
