package com.butent.bee.client.communication;

import com.google.gwt.core.client.JavaScriptException;

import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;

public final class RtcAdapter {

  private static final BeeLogger logger = LogUtils.getLogger(RtcAdapter.class);

  public static List<Property> getInfo() {
    List<Property> info = new ArrayList<>();

    try {
      PropertyUtils.addProperties(info,
          "Detected Browser", getWebrtcDetectedBrowser(),
          "Detected Version", getWebrtcDetectedVersion(),
          "Minimum Version", getWebrtcMinimumVersion());

    } catch (JavaScriptException ex) {
      logger.error(ex);
    }

    return info;
  }

  @JsProperty(namespace = JsPackage.GLOBAL)
  public static native Object getWebrtcDetectedBrowser();

  @JsProperty(namespace = JsPackage.GLOBAL)
  public static native Object getWebrtcDetectedVersion();

  @JsProperty(namespace = JsPackage.GLOBAL)
  public static native Object getWebrtcMinimumVersion();

  private RtcAdapter() {
  }
}