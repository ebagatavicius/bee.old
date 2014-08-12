package com.butent.bee.client.dom;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

import elemental.client.Browser;
import elemental.html.InputElement;

/**
 * Checks whether a user's browser support certain features like mp3, drag and drop, web sockets,
 * canvas and so on.
 */

public final class Features {

  private static String nsSvg = "http://www.w3.org/2000/svg";

  private static Boolean applicationCache;

  private static Boolean attributeAutofocus;
  private static Boolean attributeList;
  private static Boolean attributeMax;
  private static Boolean attributeMin;
  private static Boolean attributeMultiple;
  private static Boolean attributePattern;
  private static Boolean attributePlaceholder;
  private static Boolean attributeRequired;
  private static Boolean attributeStep;

  private static Boolean audio;
  private static String audioAac;
  private static String audioMp3;
  private static String audioVorbis;
  private static String audioWav;

  private static Boolean autocompleteInput;
  private static Boolean autocompleteTextArea;

  private static Boolean canvas;
  private static Boolean canvasText;

  private static Boolean contentEditable;

  private static Boolean dnd;
  private static Boolean dndEvents;

  private static Boolean elementCommand;
  private static Boolean elementDataList;
  private static Boolean elementDetails;
  private static Boolean elementDevice;
  private static Boolean elementMeter;
  private static Boolean elementOutput;
  private static Boolean elementProgress;
  private static Boolean elementTime;

  private static Boolean fileApi;
  private static Boolean geolocation;
  private static Boolean indexedDB;

  private static Boolean inputColor;
  private static Boolean inputDate;
  private static Boolean inputDatetime;
  private static Boolean inputDatetimeLocal;
  private static Boolean inputEmail;
  private static Boolean inputMonth;
  private static Boolean inputNumber;
  private static Boolean inputRange;
  private static Boolean inputSearch;
  private static Boolean inputTel;
  private static Boolean inputTime;
  private static Boolean inputUrl;
  private static Boolean inputWeek;

  private static Boolean intl;
  private static Boolean localStorage;
  private static Boolean microdata;
  private static Boolean notifications;
  private static Boolean postMessage;
  private static Boolean requestAnimationFrame;
  private static Boolean selectors;
  private static Boolean sendAsFormData;
  private static Boolean serverSentEvents;
  private static Boolean sessionStorage;

  private static Boolean smil;
  private static Boolean svg;
  private static Boolean svgClipPaths;
  private static Boolean svgInline;
  private static Boolean svgInTextHtml;

  private static Boolean undo;

  private static Boolean video;
  private static String videoH264;
  private static String videoTheora;
  private static String videoWebm;
  private static Boolean videoCaptions;
  private static Boolean videoPoster;

  private static Boolean webGl;
  private static Boolean webSockets;
  private static Boolean webWorkers;

  private static Boolean xhrCrossDomain;
  private static Boolean xhrUploadProgress;

  public static String getAudioAac() {
    if (!supportsAudio()) {
      return BeeConst.STRING_EMPTY;
    }
    if (audioAac == null) {
      audioAac = testAudioAac();
    }
    return audioAac;
  }

  public static String getAudioMp3() {
    if (!supportsAudio()) {
      return BeeConst.STRING_EMPTY;
    }
    if (audioMp3 == null) {
      audioMp3 = testAudioMp3();
    }
    return audioMp3;
  }

  public static String getAudioVorbis() {
    if (!supportsAudio()) {
      return BeeConst.STRING_EMPTY;
    }
    if (audioVorbis == null) {
      audioVorbis = testAudioVorbis();
    }
    return audioVorbis;
  }

  public static String getAudioWav() {
    if (!supportsAudio()) {
      return BeeConst.STRING_EMPTY;
    }
    if (audioWav == null) {
      audioWav = testAudioWav();
    }
    return audioWav;
  }

  public static List<Property> getInfo() {
    List<Property> lst = PropertyUtils.createProperties(
        "Application Cache", supportsApplicationCache(),

        "Attribute Autofocus", supportsAttributeAutofocus(),
        "Attribute List", supportsAttributeList(),
        "Attribute Max", supportsAttributeMax(),
        "Attribute Min", supportsAttributeMin(),
        "Attribute Multiple", supportsAttributeMultiple(),
        "Attribute Pattern", supportsAttributePattern(),
        "Attribute Placeholder", supportsAttributePlaceholder(),
        "Attribute Required", supportsAttributeRequired(),
        "Attribute Step", supportsAttributeStep(),

        "Audio", supportsAudio(),
        "Audio Aac", getAudioAac(),
        "Audio Mp3", getAudioMp3(),
        "Audio Vorbis", getAudioVorbis(),
        "Audio Wav", getAudioWav(),

        "Autocomplete Input", supportsAutocompleteInput(),
        "Autocomplete Text Area", supportsAutocompleteTextArea(),

        "Canvas", supportsCanvas(),
        "Canvas Text", supportsCanvasText(),

        "Content Editable", supportsContentEditable(),

        "Dnd", supportsDnd(),
        "Dnd Events", supportsDndEvents(),

        "Element Command", supportsElementCommand(),
        "Element DataList", supportsElementDataList(),
        "Element Details", supportsElementDetails(),
        "Element Device", supportsElementDevice(),
        "Element Meter", supportsElementMeter(),
        "Element Output", supportsElementOutput(),
        "Element Progress", supportsElementProgress(),
        "Element Time", supportsElementTime(),

        "File Api", supportsFileApi(),
        "Geolocation", supportsGeolocation(),
        "Indexed DB", supportsIndexedDB(),

        "Input Color", supportsInputColor(),
        "Input Date", supportsInputDate(),
        "Input Datetime", supportsInputDatetime(),
        "Input Datetime Local", supportsInputDatetimeLocal(),
        "Input Email", supportsInputEmail(),
        "Input Month", supportsInputMonth(),
        "Input Number", supportsInputNumber(),
        "Input Range", supportsInputRange(),
        "Input Search", supportsInputSearch(),
        "Input Tel", supportsInputTel(),
        "Input Time", supportsInputTime(),
        "Input Url", supportsInputUrl(),
        "Input Week", supportsInputWeek(),

        "Intl", supportsIntl(),
        "Local Storage", supportsLocalStorage(),
        "Microdata", supportsMicrodata(),
        "Notifications", supportsNotifications(),
        "Post Message", supportsPostMessage(),
        "Request Animation Frame", supportsRequestAnimationFrame(),
        "Send As Form Data", supportsSendAsFormData(),
        "Selectors", supportsSelectors(),
        "Server Sent Events", supportsServerSentEvents(),
        "Session Storage", supportsSessionStorage(),

        "Smil", supportsSmil(),
        "Svg", supportsSvg(),
        "Svg Clip Paths", supportsSvgClipPaths(),
        "Svg Inline", supportsSvgInline(),
        "Svg In Text Html", supportsSvgInTextHtml(),

        "Undo", supportsUndo(),

        "Video", supportsVideo(),
        "Video H264", getVideoH264(),
        "Video Theora", getVideoTheora(),
        "Video Webm", getVideoWebm(),
        "Video Captions", supportsVideoCaptions(),
        "Video Poster", supportsVideoPoster(),

        "Web Gl", supportsWebGl(),
        "Web Sockets", supportsWebSockets(),
        "Web Workers", supportsWebWorkers(),

        "Xhr Cross Domain", supportsXhrCrossDomain(),
        "Xhr Upload Progress", supportsXhrUploadProgress());

    return lst;
  }

  public static String getVideoH264() {
    if (!supportsVideo()) {
      return BeeConst.STRING_EMPTY;
    }
    if (videoH264 == null) {
      videoH264 = testVideoH264();
    }
    return videoH264;
  }

  public static String getVideoTheora() {
    if (!supportsVideo()) {
      return BeeConst.STRING_EMPTY;
    }
    if (videoTheora == null) {
      videoTheora = testVideoTheora();
    }
    return videoTheora;
  }

  public static String getVideoWebm() {
    if (!supportsVideo()) {
      return BeeConst.STRING_EMPTY;
    }
    if (videoWebm == null) {
      videoWebm = testVideoWebm();
    }
    return videoWebm;
  }

  public static boolean isEventSupported(String tagName, String eventName) {
    Assert.notEmpty(tagName);
    Assert.notEmpty(eventName);

    Element element = Document.get().createElement(tagName);
    if (element == null) {
      return false;
    }
    String event = eventName.startsWith("on") ? eventName : "on" + eventName;

    boolean ok = JsUtils.isIn(event, element);

    if (!ok) {
      element.setAttribute(eventName, BeeConst.STRING_EMPTY);
      ok = JsUtils.isFunction(element, event);

      JsUtils.clearProperty(element, event);
      element.removeAttribute(eventName);
    }

    element = null;
    return ok;
  }

  public static boolean supportsApplicationCache() {
    if (applicationCache == null) {
      applicationCache = testApplicationCache();
    }
    return applicationCache;
  }

  public static boolean supportsAttributeAutofocus() {
    if (attributeAutofocus == null) {
      attributeAutofocus = testAttributeAutofocus();
    }
    return attributeAutofocus;
  }

  public static boolean supportsAttributeList() {
    if (attributeList == null) {
      attributeList = testAttributeList();
    }
    return attributeList;
  }

  public static boolean supportsAttributeMax() {
    if (attributeMax == null) {
      attributeMax = testAttributeMax();
    }
    return attributeMax;
  }

  public static boolean supportsAttributeMin() {
    if (attributeMin == null) {
      attributeMin = testAttributeMin();
    }
    return attributeMin;
  }

  public static boolean supportsAttributeMultiple() {
    if (attributeMultiple == null) {
      attributeMultiple = testAttributeMultiple();
    }
    return attributeMultiple;
  }

  public static boolean supportsAttributePattern() {
    if (attributePattern == null) {
      attributePattern = testAttributePattern();
    }
    return attributePattern;
  }

  public static boolean supportsAttributePlaceholder() {
    if (attributePlaceholder == null) {
      attributePlaceholder = testAttributePlaceholder();
    }
    return attributePlaceholder;
  }

  public static boolean supportsAttributeRequired() {
    if (attributeRequired == null) {
      attributeRequired = testAttributeRequired();
    }
    return attributeRequired;
  }

  public static boolean supportsAttributeStep() {
    if (attributeStep == null) {
      attributeStep = testAttributeStep();
    }
    return attributeStep;
  }

  public static boolean supportsAudio() {
    if (audio == null) {
      audio = testAudio();
    }
    return audio;
  }

  public static boolean supportsAutocompleteInput() {
    if (autocompleteInput == null) {
      autocompleteInput = testAutocompleteInput();
    }
    return autocompleteInput;
  }

  public static boolean supportsAutocompleteTextArea() {
    if (autocompleteTextArea == null) {
      autocompleteTextArea = testAutocompleteTextArea();
    }
    return autocompleteTextArea;
  }

  public static boolean supportsCanvas() {
    if (canvas == null) {
      canvas = testCanvas();
    }
    return canvas;
  }

  public static boolean supportsCanvasText() {
    if (canvasText == null) {
      canvasText = testCanvasText();
    }
    return canvasText;
  }

  public static boolean supportsContentEditable() {
    if (contentEditable == null) {
      contentEditable = testContentEditable();
    }
    return contentEditable;
  }

  public static boolean supportsDnd() {
    if (dnd == null) {
      dnd = testDnd();
    }
    return dnd;
  }

  public static boolean supportsDndEvents() {
    if (dndEvents == null) {
      dndEvents = testDndEvents();
    }
    return dndEvents;
  }

  public static boolean supportsElementCommand() {
    if (elementCommand == null) {
      elementCommand = testElementCommand();
    }
    return elementCommand;
  }

  public static boolean supportsElementDataList() {
    if (elementDataList == null) {
      elementDataList = testElementDataList();
    }
    return elementDataList;
  }

  public static boolean supportsElementDetails() {
    if (elementDetails == null) {
      elementDetails = testElementDetails();
    }
    return elementDetails;
  }

  public static boolean supportsElementDevice() {
    if (elementDevice == null) {
      elementDevice = testElementDevice();
    }
    return elementDevice;
  }

  public static boolean supportsElementMeter() {
    if (elementMeter == null) {
      elementMeter = testElementMeter();
    }
    return elementMeter;
  }

  public static boolean supportsElementOutput() {
    if (elementOutput == null) {
      elementOutput = testElementOutput();
    }
    return elementOutput;
  }

  public static boolean supportsElementProgress() {
    if (elementProgress == null) {
      elementProgress = testElementProgress();
    }
    return elementProgress;
  }

  public static boolean supportsElementTime() {
    if (elementTime == null) {
      elementTime = testElementTime();
    }
    return elementTime;
  }

  public static boolean supportsFileApi() {
    if (fileApi == null) {
      fileApi = testFileApi();
    }
    return fileApi;
  }

  public static boolean supportsGeolocation() {
    if (geolocation == null) {
      geolocation = testGeolocation();
    }
    return geolocation;
  }

  public static boolean supportsIndexedDB() {
    if (indexedDB == null) {
      indexedDB = testIndexedDB();
    }
    return indexedDB;
  }

  public static boolean supportsInputColor() {
    if (inputColor == null) {
      inputColor = testInputColor();
    }
    return inputColor;
  }

  public static boolean supportsInputDate() {
    if (inputDate == null) {
      inputDate = testInputDate();
    }
    return inputDate;
  }

  public static boolean supportsInputDatetime() {
    if (inputDatetime == null) {
      inputDatetime = testInputDatetime();
    }
    return inputDatetime;
  }

  public static boolean supportsInputDatetimeLocal() {
    if (inputDatetimeLocal == null) {
      inputDatetimeLocal = testInputDatetimeLocal();
    }
    return inputDatetimeLocal;
  }

  public static boolean supportsInputEmail() {
    if (inputEmail == null) {
      inputEmail = testInputEmail();
    }
    return inputEmail;
  }

  public static boolean supportsInputMonth() {
    if (inputMonth == null) {
      inputMonth = testInputMonth();
    }
    return inputMonth;
  }

  public static boolean supportsInputNumber() {
    if (inputNumber == null) {
      inputNumber = testInputNumber();
    }
    return inputNumber;
  }

  public static boolean supportsInputRange() {
    if (inputRange == null) {
      inputRange = testInputRange();
    }
    return inputRange;
  }

  public static boolean supportsInputSearch() {
    if (inputSearch == null) {
      inputSearch = testInputSearch();
    }
    return inputSearch;
  }

  public static boolean supportsInputTel() {
    if (inputTel == null) {
      inputTel = testInputTel();
    }
    return inputTel;
  }

  public static boolean supportsInputTime() {
    if (inputTime == null) {
      inputTime = testInputTime();
    }
    return inputTime;
  }

  public static boolean supportsInputType(String type) {
    Assert.notEmpty(type);
    return testInputType(type);
  }

  public static boolean supportsInputUrl() {
    if (inputUrl == null) {
      inputUrl = testInputUrl();
    }
    return inputUrl;
  }

  public static boolean supportsInputWeek() {
    if (inputWeek == null) {
      inputWeek = testInputWeek();
    }
    return inputWeek;
  }

  public static boolean supportsIntl() {
    if (intl == null) {
      intl = testIntl();
    }
    return intl;
  }

  public static boolean supportsLocalStorage() {
    if (localStorage == null) {
      localStorage = testLocalStorage();
    }
    return localStorage;
  }

  public static boolean supportsMicrodata() {
    if (microdata == null) {
      microdata = testMicrodata();
    }
    return microdata;
  }

  public static boolean supportsNotifications() {
    if (notifications == null) {
      notifications = testNotifications();
    }
    return notifications;
  }

  public static boolean supportsPostMessage() {
    if (postMessage == null) {
      postMessage = testPostMessage();
    }
    return postMessage;
  }

  public static boolean supportsRequestAnimationFrame() {
    if (requestAnimationFrame == null) {
      requestAnimationFrame = testRequestAnimationFrame();
    }
    return requestAnimationFrame;
  }

  public static boolean supportsSelectors() {
    if (selectors == null) {
      selectors = testSelectors();
    }
    return selectors;
  }

  public static boolean supportsSendAsFormData() {
    if (sendAsFormData == null) {
      sendAsFormData = testSendAsFormData();
    }
    return sendAsFormData;
  }

  public static boolean supportsServerSentEvents() {
    if (serverSentEvents == null) {
      serverSentEvents = testServerSentEvents();
    }
    return serverSentEvents;
  }

  public static boolean supportsSessionStorage() {
    if (sessionStorage == null) {
      sessionStorage = testSessionStorage();
    }
    return sessionStorage;
  }

  public static boolean supportsSmil() {
    if (smil == null) {
      smil = testSmil();
    }
    return smil;
  }

  public static boolean supportsSvg() {
    if (svg == null) {
      svg = testSvg();
    }
    return svg;
  }

  public static boolean supportsSvgClipPaths() {
    if (svgClipPaths == null) {
      svgClipPaths = testSvgClipPaths();
    }
    return svgClipPaths;
  }

  public static boolean supportsSvgInline() {
    if (svgInline == null) {
      svgInline = testSvgInline();
    }
    return svgInline;
  }

  public static boolean supportsSvgInTextHtml() {
    if (svgInTextHtml == null) {
      svgInTextHtml = testSvgInTextHtml();
    }
    return svgInTextHtml;
  }

  public static boolean supportsUndo() {
    if (undo == null) {
      undo = testUndo();
    }
    return undo;
  }

  public static boolean supportsVideo() {
    if (video == null) {
      video = testVideo();
    }
    return video;
  }

  public static boolean supportsVideoCaptions() {
    if (!supportsVideo()) {
      return false;
    }
    if (videoCaptions == null) {
      videoCaptions = testVideoCaptions();
    }
    return videoCaptions;
  }

  public static boolean supportsVideoPoster() {
    if (!supportsVideo()) {
      return false;
    }
    if (videoPoster == null) {
      videoPoster = testVideoPoster();
    }
    return videoPoster;
  }

  public static boolean supportsWebGl() {
    if (webGl == null) {
      webGl = testWebGl();
    }
    return webGl;
  }

  public static boolean supportsWebSockets() {
    if (webSockets == null) {
      webSockets = testWebSockets();
    }
    return webSockets;
  }

  public static boolean supportsWebWorkers() {
    if (webWorkers == null) {
      webWorkers = testWebWorkers();
    }
    return webWorkers;
  }

  public static boolean supportsXhrCrossDomain() {
    if (xhrCrossDomain == null) {
      xhrCrossDomain = testXhrCrossDomain();
    }
    return xhrCrossDomain;
  }

  public static boolean supportsXhrUploadProgress() {
    if (xhrUploadProgress == null) {
      xhrUploadProgress = testXhrUploadProgress();
    }
    return xhrUploadProgress;
  }

//@formatter:off
  private static native JavaScriptObject getWindowProperty(String p) /*-{
    var obj;

    try {
      obj = $wnd[p];
    } catch (err) {
      obj = null;
    }

    return obj;
  }-*/;

  private static native boolean isDocumentFunction(String fnc) /*-{
    var ok;

    try {
      ok = (typeof ($doc[fnc]) == "function");
    } catch (err) {
      ok = false;
    }

    return ok;
  }-*/;

  private static native boolean isDocumentProperty(String p) /*-{
    var ok;

    try {
      ok = !!$doc[p];
    } catch (err) {
      ok = false;
    }

    return ok;
  }-*/;

  private static native boolean isInWindow(String p) /*-{
    return p in $wnd;
  }-*/;

  private static native boolean isNavigatorProperty(String p) /*-{
    var ok;

    try {
      ok = !!$wnd.navigator[p];
    } catch (err) {
      ok = false;
    }

    return ok;
  }-*/;

  private static native boolean isWindowFunction(String fnc) /*-{
    var ok;

    try {
      ok = (typeof ($wnd[fnc]) == "function");
    } catch (err) {
      ok = false;
    }

    return ok;
  }-*/;

  private static native boolean isWindowProperty(String p) /*-{
    var ok;

    try {
      ok = !!$wnd[p];
    } catch (err) {
      ok = false;
    }

    return ok;
  }-*/;
//@formatter:on

  private static boolean testApplicationCache() {
    return isWindowProperty("applicationCache");
  }

  private static boolean testAttributeAutofocus() {
    return testInputAttribute(Attributes.AUTOFOCUS);
  }

  private static boolean testAttributeList() {
    return testInputAttribute(Attributes.LIST);
  }

  private static boolean testAttributeMax() {
    return testInputAttribute(Attributes.MAX);
  }

  private static boolean testAttributeMin() {
    return testInputAttribute(Attributes.MIN);
  }

  private static boolean testAttributeMultiple() {
    return testInputAttribute(Attributes.MULTIPLE);
  }

  private static boolean testAttributePattern() {
    return testInputAttribute(Attributes.PATTERN);
  }

  private static boolean testAttributePlaceholder() {
    return testInputAttribute(Attributes.PLACEHOLDER);
  }

  private static boolean testAttributeRequired() {
    return testInputAttribute(Attributes.REQUIRED);
  }

  private static boolean testAttributeStep() {
    return testInputAttribute(Attributes.STEP);
  }

  private static boolean testAudio() {
    Element element = Document.get().createElement(Tags.AUDIO);
    boolean ok;

    if (element == null) {
      ok = false;
    } else {
      ok = JsUtils.isFunction(element, "canPlayType");
      element = null;
    }

    return ok;
  }

  private static String testAudioAac() {
    String z = testAudioType("audio/x-m4a;");
    if (BeeUtils.isEmpty(z) || BeeUtils.same(z, BeeConst.NO)) {
      z = testAudioType("audio/aac;");
    }
    if (BeeUtils.isEmpty(z) || BeeUtils.same(z, BeeConst.NO)) {
      z = testAudioType("audio/mp4; codecs=\"mp4a.40.2\"");
    }

    return z;
  }

  private static String testAudioMp3() {
    return testAudioType("audio/mpeg;");
  }

//@formatter:off
  private static native String testAudioType(String type) /*-{
    var el = $doc.createElement('audio');
    var v = "";

    try {
      if (typeof el["canPlayType"] == "function") {
        v = el.canPlayType(type);
      }
    } catch (err) {
      v = null;
    }

    el = null;
    return v;
  }-*/;
//@formatter:on

  private static String testAudioVorbis() {
    return testAudioType("audio/ogg; codecs=\"vorbis\"");
  }

  private static String testAudioWav() {
    return testAudioType("audio/wav; codecs=\"1\"");
  }

  private static boolean testAutocompleteInput() {
    return testInputAttribute(Attributes.AUTOCOMPLETE);
  }

  private static boolean testAutocompleteTextArea() {
    return JsUtils.isIn(Attributes.AUTOCOMPLETE, DomUtils.createElement(Tags.TEXT_AREA));
  }

//@formatter:off
  private static native boolean testCanvas() /*-{
    var elem = $doc.createElement('canvas');
    var ok = !!(elem.getContext && elem.getContext('2d'));
    elem = null;
    return ok;
  }-*/;

  private static native boolean testCanvasText() /*-{
    var elem = $doc.createElement('canvas');
    var ok = false;

    try {
      ok = !!(elem.getContext && typeof elem.getContext('2d').fillText == "function");
    } catch (err) {
      ok = false;
    }

    elem = null;
    return ok;
  }-*/;
//@formatter:on

  private static boolean testContentEditable() {
    return JsUtils.isIn("isContentEditable", DomUtils.createElement(Tags.SPAN));
  }

  private static boolean testDnd() {
    return JsUtils.isIn("draggable", DomUtils.createElement(Tags.SPAN));
  }

  private static boolean testDndEvents() {
    String tg = Tags.DIV;
    return isEventSupported(tg, "drag") && isEventSupported(tg, "dragstart")
        && isEventSupported(tg, "dragenter")
        && isEventSupported(tg, "dragover")
        && isEventSupported(tg, "dragleave") && isEventSupported(tg, "dragend")
        && isEventSupported(tg, "drop");
  }

  private static boolean testElementCommand() {
    return JsUtils.isIn("type", DomUtils.createElement("command"));
  }

  private static boolean testElementDataList() {
    return JsUtils.isIn("options", DomUtils.createElement("datalist"));
  }

  private static boolean testElementDetails() {
    return JsUtils.isIn("open", DomUtils.createElement("details"));
  }

  private static boolean testElementDevice() {
    return JsUtils.isIn("type", DomUtils.createElement("device"));
  }

  private static boolean testElementMeter() {
    return JsUtils.isIn("value", DomUtils.createElement(Tags.METER));
  }

  private static boolean testElementOutput() {
    return JsUtils.isIn("value", DomUtils.createElement("output"));
  }

  private static boolean testElementProgress() {
    return JsUtils.isIn("value", DomUtils.createElement(Tags.PROGRESS));
  }

  private static boolean testElementTime() {
    return JsUtils.isIn("valueAsDate", DomUtils.createElement("time"));
  }

//@formatter:off
  private static native boolean testFileApi() /*-{
    return typeof FileReader != 'undefined';
  }-*/;
//@formatter:on

  private static boolean testGeolocation() {
    return isNavigatorProperty("geolocation");
  }

  private static boolean testIndexedDB() {
    return isWindowProperty("indexedDB");
  }

  private static boolean testInputAttribute(String attr) {
    Element element = DomUtils.createElement(Tags.INPUT);
    return JsUtils.isIn(attr, element);
  }

  private static boolean testInputColor() {
    return testInputType("color");
  }

  private static boolean testInputDate() {
    return testInputType("date");
  }

  private static boolean testInputDatetime() {
    return testInputType("datetime");
  }

  private static boolean testInputDatetimeLocal() {
    return testInputType("datetime-local");
  }

  private static boolean testInputEmail() {
    return testInputType("email");
  }

  private static boolean testInputMonth() {
    return testInputType("month");
  }

  private static boolean testInputNumber() {
    return testInputType("number");
  }

  private static boolean testInputRange() {
    return testInputType("range");
  }

  private static boolean testInputSearch() {
    return testInputType("search");
  }

  private static boolean testInputTel() {
    return testInputType("tel");
  }

  private static boolean testInputTime() {
    return testInputType("time");
  }

  private static boolean testInputType(String type) {
    if (BeeUtils.isEmpty(type)) {
      return false;
    }

    InputElement inputElement = Browser.getDocument().createInputElement();

    boolean ok;
    try {
      inputElement.setType(type);
      ok = true;
    } catch (JavaScriptException ex) {
      ok = false;
    }

    return ok && BeeUtils.same(inputElement.getType(), type);
  }

  private static boolean testInputUrl() {
    return testInputType("url");
  }

  private static boolean testInputWeek() {
    return testInputType("week");
  }

//@formatter:off
  private static native boolean testIntl() /*-{
    return typeof Intl != 'undefined';
  }-*/;
//@formatter:on

  private static boolean testLocalStorage() {
    return getWindowProperty("localStorage") != null;
  }

  private static boolean testMicrodata() {
    return isDocumentProperty("getItems");
  }

  private static boolean testNotifications() {
    return isInWindow("Notification");
  }

  private static boolean testPostMessage() {
    return isWindowProperty("postMessage");
  }

  private static boolean testRequestAnimationFrame() {
    return isWindowFunction("requestAnimationFrame");
  }

  private static boolean testSelectors() {
    return isDocumentFunction("querySelector") && isDocumentFunction("querySelectorAll");
  }

  private static boolean testSendAsFormData() {
    return isWindowProperty("FormData");
  }

//@formatter:off
  private static native boolean testServerSentEvents() /*-{
    return typeof EventSource !== 'undefined';
  }-*/;
//@formatter:on

  private static boolean testSessionStorage() {
    return getWindowProperty("sessionStorage") != null;
  }

  private static boolean testSmil() {
    Element element = DomUtils.createElementNs(nsSvg, "animate");
    if (element == null) {
      return false;
    }
    return BeeUtils.containsSame(element.getString(), "svg");
  }

  private static boolean testSvg() {
    Element element = DomUtils.createElementNs(nsSvg, "svg");
    return JsUtils.isFunction(element, "createSVGRect");
  }

  private static boolean testSvgClipPaths() {
    Element element = DomUtils.createElementNs(nsSvg, "clipPath");
    if (element == null) {
      return false;
    }
    return BeeUtils.containsSame(element.getString(), "svg");
  }

  private static boolean testSvgInline() {
    Element element = Document.get().createElement(Tags.DIV);
    element.setInnerHTML("<svg/>");
    Node node = element.getFirstChild();

    return BeeUtils.same(DomUtils.getNamespaceUri(node), nsSvg);
  }

//@formatter:off
  private static native boolean testSvgInTextHtml() /*-{
    var el = $doc.createElement('div');
    el.innerHTML = '<svg></svg>';
    return !!($wnd.SVGSVGElement && el.firstChild instanceof $wnd.SVGSVGElement);
  }-*/;

  private static native boolean testUndo() /*-{
    return typeof UndoManager !== 'undefined';
  }-*/;
//@formatter:on

  private static boolean testVideo() {
    Element element = DomUtils.createElement(Tags.VIDEO);
    return JsUtils.isFunction(element, "canPlayType");
  }

  private static boolean testVideoCaptions() {
    return JsUtils.isIn("src", DomUtils.createElement("track"));
  }

  private static String testVideoH264() {
    String h264 = "video/mp4; codecs=\"avc1.42E01E'";

    String z = testVideoType(h264 + "\"");
    if (BeeUtils.isEmpty(z) || BeeUtils.same(z, BeeConst.NO)) {
      z = testVideoType(h264 + ", mp4a.40.2\"");
    }

    return z;
  }

  private static boolean testVideoPoster() {
    return JsUtils.isIn("poster", DomUtils.createElement(Tags.VIDEO));
  }

  private static String testVideoTheora() {
    return testVideoType("video/ogg; codecs=\"theora\"");
  }

//@formatter:off
  private static native String testVideoType(String type) /*-{
    var elem = $doc.createElement('video');
    if (elem == null || elem == undefined) {
      return "";
    }

    if (!!elem.canPlayType) {
      return elem.canPlayType(type);
    } else {
      return "";
    }
  }-*/;
//@formatter:on

  private static String testVideoWebm() {
    return testVideoType("video/webm; codecs=\"vp8, vorbis\"");
  }

//@formatter:off
  private static native boolean testWebGl() /*-{
    var elem = $doc.createElement('canvas');
    if (elem == null || elem == undefined || !elem.getContext) {
      return false;
    }

    var ok = false;
    try {
      if (elem.getContext('webgl')) {
        ok = true;
      } else if (elem.getContext('experimental-webgl')) {
        ok = true;
      }
    } catch (err) {
      ok = false;
    }

    return ok;
  }-*/;
//@formatter:on

  private static boolean testWebSockets() {
    return isInWindow("WebSocket");
  }

  private static boolean testWebWorkers() {
    return isWindowProperty("Worker");
  }

//@formatter:off
  private static native boolean testXhrCrossDomain() /*-{
    if ($wnd.XMLHttpRequest) {
      return "withCredentials" in new $wnd.XMLHttpRequest;
    } else {
      return false;
    }
  }-*/;

  private static native boolean testXhrUploadProgress() /*-{
    if ($wnd.XMLHttpRequest) {
      return "upload" in new $wnd.XMLHttpRequest;
    } else {
      return false;
    }
  }-*/;
//@formatter:on

  private Features() {
  }
}
