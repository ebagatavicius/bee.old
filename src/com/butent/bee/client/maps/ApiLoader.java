package com.butent.bee.client.maps;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Window;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.State;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;

import java.util.List;

public final class ApiLoader {

  private static final String SRC = getProtocol()
      + "//maps.googleapis.com/maps/api/js?v=3.exp&sensor=false&callback=beemapsapiloaded";

  private static BeeLogger logger = LogUtils.getLogger(ApiLoader.class);

  private static List<ScheduledCommand> callbacks = Lists.newArrayList();

  private static State state;

  public static void ensureApi(ScheduledCommand callback) {
    Assert.notNull(callback);

    if (isLoaded()) {
      callback.execute();

    } else {
      callbacks.add(callback);

      if (state == null) {
        state = State.LOADING;

        createCallbackFunction();
        DomUtils.injectExternalScript(SRC);
      }
    }
  }

  public static boolean isLoaded() {
    return state == State.LOADED;
  }

//@formatter:off
  private static native void createCallbackFunction() /*-{
    $wnd.beemapsapiloaded = function() {
      @com.butent.bee.client.maps.ApiLoader::onLoad()();
    }
  }-*/;
//@formatter:on

  private static String getProtocol() {
    return Window.Location.getProtocol();
  }

  private static void onLoad() {
    logger.debug("maps api loaded");
    state = State.LOADED;

    for (ScheduledCommand callback : callbacks) {
      callback.execute();
    }
    callbacks.clear();
  }

  private ApiLoader() {
  }
}
