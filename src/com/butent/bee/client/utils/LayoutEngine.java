package com.butent.bee.client.utils;

import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.time.JustDate;

import elemental.client.Browser.Info;
import elemental.js.JsBrowser;

public enum LayoutEngine {
  WEBKIT("webkit", "webkit", new JustDate(2014, 6, 6)),
  GECKO("gecko", null, null),
  TRIDENT("trident", "trident", new JustDate(2014, 5, 23));

  public static LayoutEngine detect() {
    String userAgent = JsBrowser.getWindow().getNavigator().getUserAgent().toLowerCase();
    if (userAgent.contains(TRIDENT.substring)) {
      return TRIDENT;
    }

    Info info = JsBrowser.getInfo();

    if (info != null) {
      if (info.isWebKit()) {
        return WEBKIT;
      } else if (info.isGecko()) {
        return GECKO;
      }
    }

    if (BrowsingContext.isChrome()) {
      return WEBKIT;
    }

    for (LayoutEngine engine : LayoutEngine.values()) {
      if (userAgent.contains(engine.substring)) {
        return engine;
      }
    }
    return null;
  }

  private final String substring;

  private final String styleSheetName;
  private final JustDate styleSheetVersion;

  private LayoutEngine(String substring, String styleSheetName, JustDate styleSheetVersion) {
    this.substring = substring;
    this.styleSheetName = styleSheetName;
    this.styleSheetVersion = styleSheetVersion;
  }

  public String getStyleSheet() {
    if (hasStyleSheet()) {
      return Paths.getStyleSheetUrl(styleSheetName, styleSheetVersion);
    } else {
      return null;
    }
  }

  public boolean hasStyleSheet() {
    return styleSheetName != null;
  }
}
