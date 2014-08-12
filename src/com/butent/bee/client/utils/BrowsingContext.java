package com.butent.bee.client.utils;

import com.google.common.collect.Lists;

import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

import elemental.js.html.JsPerformance;
import elemental.dom.Element;
import elemental.dom.Document;
import elemental.client.Browser.Info;
import elemental.html.PerformanceTiming;
import elemental.html.PerformanceNavigation;
import elemental.html.Window;
import elemental.html.DOMMimeType;
import elemental.html.DOMMimeTypeArray;
import elemental.html.Screen;
import elemental.html.MemoryInfo;
import elemental.html.Navigator;
import elemental.html.Location;
import elemental.js.JsBrowser;

/**
 * Enables system to get information about user's browser.
 */

public final class BrowsingContext {

  public static List<Property> getBrowserInfo() {
    Info info = JsBrowser.getInfo();

    return PropertyUtils.createProperties(
        "Layout Engine", LayoutEngine.detect(),
        "Gecko", info.isGecko(),
        "Linux", info.isLinux(),
        "Mac", info.isMac(),
        "Supported", info.isSupported(),
        "WebKit", info.isWebKit(),
        "Windows", info.isWindows());
  }

  public static List<Property> getDocumentInfo() {
    Document document = JsBrowser.getDocument();

    List<Property> properties = PropertyUtils.createProperties(
        "Active Element", toString(document.getActiveElement()),
        "Alink Color", document.getAlinkColor(),
        "Bg Color", document.getBgColor(),
        "Character Set", document.getCharacterSet(),
        "Charset", document.getCharset(),
        "Compat Mode", document.getCompatMode(),
        "Cookie", document.getCookie(),
        "Default Charset", document.getDefaultCharset(),
        "Design Mode", document.getDesignMode(),
        "Dir", document.getDir(),
        "Document URI", document.getDocumentURI(),
        "Domain", document.getDomain(),
        "Fg Color", document.getFgColor(),
        "Height", document.getHeight(),
        "Input Encoding", document.getInputEncoding(),
        "Last Modified", document.getLastModified(),
        "Link Color", document.getLinkColor(),
        "Preferred Stylesheet Set", document.getPreferredStylesheetSet(),
        "Ready State", document.getReadyState(),
        "Referrer", document.getReferrer(),
        "Selected Stylesheet Set", document.getSelectedStylesheetSet(),
        "Title", document.getTitle(),
        "URL", document.getURL(),
        "Vlink Color", document.getVlinkColor(),
        "Width", document.getWidth(),
        "Xml Encoding", document.getXmlEncoding(),
        "Xml Version", document.getXmlVersion(),
        "Has Focus", document.hasFocus(),
        "Xml Standalone", document.isXmlStandalone());

    if (JsBrowser.getInfo().isWebKit()) {
      PropertyUtils.addProperties(properties,
          "Webkit Visibility State", document.getWebkitVisibilityState(),
          "Webkit Fullscreen Enabled", document.isWebkitFullscreenEnabled(),
          "Webkit Full Screen Keyboard Input Allowed",
          document.isWebkitFullScreenKeyboardInputAllowed(),
          "Webkit Hidden", document.isWebkitHidden(),
          "Webkit Is Full Screen", document.isWebkitIsFullScreen());
    }

    return properties;
  }

  public static List<Property> getLocationInfo() {
    Location location = JsBrowser.getWindow().getLocation();

    return PropertyUtils.createProperties(
        "Hash", location.getHash(),
        "Host", location.getHost(),
        "Host Name", location.getHostname(),
        "Href", location.getHref(),
        "Origin", location.getOrigin(),
        "Path Name", location.getPathname(),
        "Port", location.getPort(),
        "Protocol", location.getProtocol(),
        "Search", location.getSearch());
  }

  public static List<Property> getNavigatorInfo() {
    Navigator navigator = JsBrowser.getWindow().getNavigator();

    return PropertyUtils.createProperties(
        "App Code Name", navigator.getAppCodeName(),
        "App Name", navigator.getAppName(),
        "App Version", navigator.getAppVersion(),
        "Language", navigator.getLanguage(),
        "Platform", navigator.getPlatform(),
        "Product", navigator.getProduct(),
        "Product Sub", navigator.getProductSub(),
        "User Agent", navigator.getUserAgent(),
        "Vendor", navigator.getVendor(),
        "Vendor Sub", navigator.getVendorSub(),
        "Cookie Enabled", navigator.isCookieEnabled(),
        "On Line", navigator.isOnLine(),
        "Java Enabled", navigator.javaEnabled());
  }

  public static List<Property> getPerformanceInfo() {
    List<Property> result = Lists.newArrayList();

    JsPerformance performance = JsBrowser.getWindow().getPerformance();
    if (performance == null) {
      return null;
    }

    MemoryInfo memory = performance.getMemory();
    if (memory != null) {
      PropertyUtils.addProperties(result,
          "Js Heap Size Limit", memory.getJsHeapSizeLimit(),
          "Total JS Heap Size", memory.getTotalJSHeapSize(),
          "Used JS Heap Size", memory.getUsedJSHeapSize());
    }

    PerformanceNavigation navigation = performance.getNavigation();
    if (navigation != null) {
      PropertyUtils.addProperties(result,
          "Redirect Count", navigation.getRedirectCount(),
          "Navigation Type", navigation.getType());
    }

    PerformanceTiming timing = performance.getTiming();
    if (timing != null) {
      PropertyUtils.addProperties(result,
          "Connect Start", timing.getConnectStart(),
          "Connect End", timing.getConnectEnd(),
          "Domain Lookup Start", timing.getDomainLookupStart(),
          "Domain Lookup End", timing.getDomainLookupEnd(),
          "Dom Complete", timing.getDomComplete(),
          "Dom Content Loaded Event Start", timing.getDomContentLoadedEventStart(),
          "Dom Content Loaded Event End", timing.getDomContentLoadedEventEnd(),
          "Dom Interactive", timing.getDomInteractive(),
          "Dom Loading", timing.getDomLoading(),
          "Fetch Start", timing.getFetchStart(),
          "Load Event Start", timing.getLoadEventStart(),
          "Load Event End", timing.getLoadEventEnd(),
          "Navigation Start", timing.getNavigationStart(),
          "Redirect Start", timing.getRedirectStart(),
          "Redirect End", timing.getRedirectEnd(),
          "Request Start", timing.getRequestStart(),
          "Response Start", timing.getResponseStart(),
          "Response End", timing.getResponseEnd(),
          "Secure Connection Start", timing.getSecureConnectionStart(),
          "Unload Event Start", timing.getUnloadEventStart(),
          "Unload Event End", timing.getUnloadEventEnd());
    }

    if (JsUtils.isFunction(performance, "now")) {
      result.add(new Property("Now", JsUtils.doMethod(performance, "now")));
    }

    return result;
  }

  public static List<Property> getScreenInfo() {
    Screen screen = JsBrowser.getWindow().getScreen();

    return PropertyUtils.createProperties(
        "Avail Height", screen.getAvailHeight(),
        "Avail Left", screen.getAvailLeft(),
        "Avail Top", screen.getAvailTop(),
        "Avail Width", screen.getAvailWidth(),
        "Color Depth", screen.getColorDepth(),
        "Height", screen.getHeight(),
        "Pixel Depth", screen.getPixelDepth(),
        "Width", screen.getWidth());
  }

  public static List<Property> getSupportedMimeTypes() {
    List<Property> result = Lists.newArrayList();

    DOMMimeTypeArray mimeTypes = JsBrowser.getWindow().getNavigator().getMimeTypes();
    if (mimeTypes == null) {
      return null;
    }

    for (int i = 0; i < mimeTypes.getLength(); i++) {
      DOMMimeType item = mimeTypes.item(i);
      result.add(new Property(item.getType(),
          BeeUtils.joinItems(item.getSuffixes(), item.getDescription())));
    }
    return result;
  }

  public static List<Property> getWindowInfo() {
    Window window = JsBrowser.getWindow();

    return PropertyUtils.createProperties(
        "Default Status", window.getDefaultStatus(),
        "Device Pixel Ratio", window.getDevicePixelRatio(),
        "History Length", window.getHistory().getLength(),
        "History State", window.getHistory().getState(),
        "Inner Height", window.getInnerHeight(),
        "Inner Width", window.getInnerWidth(),
        "Length", window.getLength(),
        "Name", window.getName(),
        "Outer Height", window.getOuterHeight(),
        "Outer Width", window.getOuterWidth(),
        "Page X Offset", window.getPageXOffset(),
        "Page Y Offset", window.getPageYOffset(),
        "Screen Left", window.getScreenLeft(),
        "Screen Top", window.getScreenTop(),
        "Screen X", window.getScreenX(),
        "Screen Y", window.getScreenY(),
        "Scroll X", window.getScrollX(),
        "Scroll Y", window.getScrollY(),
        "Status", window.getStatus(),
        "Offscreen Buffering", window.isOffscreenBuffering());
  }

  public static boolean isChrome() {
    return BeeUtils.containsSame(JsBrowser.getWindow().getNavigator().getUserAgent(), "chrome");
  }

  public static Window open(String url) {
    return JsBrowser.getWindow().open(url, Keywords.BROWSING_CONTEXT_BLANK);
  }

  private static String toString(Element element) {
    if (element == null) {
      return null;
    } else {
      return BeeUtils.joinWords(element.getTagName(), element.getId(), element.getClassName());
    }
  }

  private BrowsingContext() {
  }
}
