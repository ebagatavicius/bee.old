package com.butent.bee.client.cli;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.geolocation.client.Position;
import com.google.gwt.geolocation.client.Position.Coordinates;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.Bee;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.Historian;
import com.butent.bee.client.Settings;
import com.butent.bee.client.animation.RafCallback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcInfo;
import com.butent.bee.client.communication.RpcList;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.composite.FileGroup.Column;
import com.butent.bee.client.composite.RadioGroup;
import com.butent.bee.client.composite.ResourceEditor;
import com.butent.bee.client.composite.SliderBar;
import com.butent.bee.client.composite.Thermometer;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.JsData;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.decorator.TuningFactory;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.MessageBoxes;
import com.butent.bee.client.dialog.NotificationOptions;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dialog.WebNotification;
import com.butent.bee.client.dom.ClientRect;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.dom.Rulers;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.dom.Stacking;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.LocaleUtils;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.client.images.Flags;
import com.butent.bee.client.images.Images;
import com.butent.bee.client.js.Markdown;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.logging.LogFormatter;
import com.butent.bee.client.maps.ApiLoader;
import com.butent.bee.client.maps.LatLng;
import com.butent.bee.client.maps.MapContainer;
import com.butent.bee.client.maps.MapOptions;
import com.butent.bee.client.maps.MapUtils;
import com.butent.bee.client.maps.MapWidget;
import com.butent.bee.client.media.MediaStream;
import com.butent.bee.client.media.MediaStreamConstraints;
import com.butent.bee.client.media.MediaUtils;
import com.butent.bee.client.menu.MenuCommand;
import com.butent.bee.client.modules.administration.AdministrationKeeper;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.tasks.TasksKeeper;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.style.Axis;
import com.butent.bee.client.style.ComputedStyles;
import com.butent.bee.client.style.Font;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Theme;
import com.butent.bee.client.utils.BrowsingContext;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.webrtc.RtcAdapter;
import com.butent.bee.client.webrtc.RtcUtils;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.Audio;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.CustomWidget;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputFile;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Meter;
import com.butent.bee.client.widget.Svg;
import com.butent.bee.client.widget.Video;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Resource;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.communication.TextMessage;
import com.butent.bee.shared.css.CssAngle;
import com.butent.bee.shared.css.CssProperties;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.Display;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.ExtendedPropertiesData;
import com.butent.bee.shared.data.IsTable;
import com.butent.bee.shared.data.PropertiesData;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.StringMatrix;
import com.butent.bee.shared.data.TableColumn;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.html.builder.elements.Input;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;
import com.butent.bee.shared.utils.Wildcards;
import com.butent.bee.shared.websocket.messages.AdminMessage;
import com.butent.bee.shared.websocket.messages.ConfigMessage;
import com.butent.bee.shared.websocket.messages.EchoMessage;
import com.butent.bee.shared.websocket.messages.LogMessage;
import com.butent.bee.shared.websocket.messages.NotificationMessage;
import com.butent.bee.shared.websocket.messages.NotificationMessage.DisplayMode;
import com.butent.bee.shared.websocket.messages.ProgressMessage;
import com.butent.bee.shared.websocket.messages.ShowMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import elemental.js.JsBrowser;
import elemental.js.css.JsCSSRuleList;
import elemental.js.css.JsCSSStyleSheet;
import elemental.js.stylesheets.JsStyleSheetList;

/**
 * Contains the engine for processing client side command line interface commands.
 */

public final class CliWorker {

  private static final BeeLogger logger = LogUtils.getLogger(CliWorker.class);

  private static boolean cornified;

  public static boolean execute(String line, boolean errorPopup) {
    if (BeeUtils.isEmpty(line)) {
      return false;
    }

    String v = line.trim();
    String[] arr = BeeUtils.split(v, BeeConst.CHAR_SPACE);

    String z = arr[0].toLowerCase();
    String args = (arr.length > 1) ? v.substring(z.length()).trim() : BeeConst.STRING_EMPTY;

    if ("?".equals(z)) {
      whereAmI();

    } else if (BeeUtils.isDigit(v.charAt(0)) || v.charAt(0) == '(' || v.charAt(0) == '-'
        || v.startsWith("Math.")) {
      doEval(v);

    } else if (z.startsWith("adm") && !args.isEmpty()) {
      doAdmin(z.substring(3), args, errorPopup);

    } else if ("audio".equals(z)) {
      playAudio(arr);

    } else if (z.startsWith("autoc") && !args.isEmpty()) {
      AutocompleteProvider.switchTo(args);

    } else if ("bee".equals(z)) {
      showPropData(v, Bee.getInfo());

    } else if ("browser".equals(z) || z.startsWith("wind")) {
      showBrowser(arr);

    } else if ("cache".equals(z)) {
      showExtData(v, Global.getCache().getExtendedInfo());

    } else if (z.startsWith("cap")) {
      showCaptions();

    } else if (BeeUtils.inList(z, "center", "east", "north", "south", "screen", "west")) {
      doScreen(arr, errorPopup);

    } else if ("charset".equals(z)) {
      getCharsets();

    } else if (z.startsWith("cho")) {
      showChoice(arr);

    } else if ("class".equals(z)) {
      getClassInfo(args, errorPopup);

    } else if ("clear".equals(z)) {
      clear(args);

    } else if (z.startsWith("collect")) {
      doCollections(arr);

    } else if (z.startsWith("column")) {
      showExtData(v, Data.getColumnMapper().getExtendedInfo());

    } else if (z.startsWith("data")) {
      showDataInfo(args, errorPopup);

    } else if (z.startsWith("chat")) {
      showPropData(v, Global.getChatManager().getInfo());

    } else if (z.startsWith("color")) {
      showColor(arr);

    } else if (z.startsWith("conf")) {
      BeeKeeper.getRpc().invoke("configInfo", ResponseHandler.callback(z));

    } else if (z.startsWith("conn") || "http".equals(z)) {
      BeeKeeper.getRpc().invoke("connectionInfo", ResponseHandler.callback(z));

    } else if ("cornify".equals(z)) {
      cornify(arr);

    } else if ("create".equals(z)) {
      createResource(args, errorPopup);

    } else if (z.startsWith("dbinf")) {
      getDbInfo(args);

    } else if (z.startsWith("dbt")) {
      getTables(v, arr);

    } else if ("debug".equals(z)) {
      setDebug(args);

    } else if (z.startsWith("decor")) {
      if (BeeUtils.isEmpty(args)) {
        showExtData(v, TuningFactory.getExtendedInfo());
      } else {
        TuningFactory.refresh();
      }

    } else if (z.startsWith("df")) {
      showDateFormat(args);

    } else if (z.startsWith("dict") && !args.isEmpty()) {
      doDictionary(args, errorPopup);

    } else if (z.startsWith("dim")) {
      showDimensions(args);

    } else if ("dsn".equals(z)) {
      doDsn();

    } else if (z.startsWith("dt")) {
      showDate(z, args, errorPopup);

    } else if (BeeUtils.inList(z, "dir", "file", "get", "download", "src")) {
      getResource(arr);

    } else if (z.startsWith("el") && !args.isEmpty()) {
      showElement(v, arr, errorPopup);

    } else if ("eval".equals(z) && !args.isEmpty()) {
      doEval(args);

    } else if (z.startsWith("exch")) {
      if (arr.length >= 4) {
        showExchangeRates(arr[1], arr[2], arr[3]);
      } else if (arr.length >= 3) {
        showExchangeRate(arr[1], arr[2]);
      } else if (arr.length >= 2) {
        showCurrentExchangeRate(args);
      } else {
        showListOfCurrencies();
      }

    } else if ("exec".equals(z) && !args.isEmpty()) {
      logger.debug(JsUtils.evalToString(args));

    } else if ("exit".equals(z)) {
      Bee.exit();

    } else if ("explain".equals(z)) {
      if (BeeUtils.isInt(args)) {
        Global.setExplain(BeeUtils.toInt(args));
      }
      logger.debug(z, Global.getExplain());

    } else if (BeeUtils.inList(z, "f", "func")) {
      showFunctions(v, arr, errorPopup);

    } else if (z.startsWith("fa")) {
      showFontAwesome(z.substring(2), args, errorPopup);

    } else if ("files".equals(z) || z.startsWith("repo")) {
      getFiles();

    } else if (z.startsWith("filt")) {
      showExtData("Filters", Global.getFilters().getExtendedInfo());

    } else if (z.startsWith("flag")) {
      showFlags(arr);

    } else if ("font".equals(z) && arr.length == 2) {
      showFont(arr[1], errorPopup);

    } else if ("form".equals(z) && arr.length == 2) {
      FormFactory.openForm(arr[1]);

    } else if (z.startsWith("forminf") || "ff".equals(z)) {
      showPropData(v, FormFactory.getInfo());

    } else if ("fs".equals(z)) {
      getFs();

    } else if ("gen".equals(z)) {
      BeeKeeper.getRpc().sendText(new ParameterList(Service.GENERATE),
          BeeUtils.joinWords(arr[1], arr[2],
              ArrayUtils.getQuietly(arr, 3), ArrayUtils.getQuietly(arr, 4)), null);

    } else if ("geo".equals(z)) {
      showGeo();

    } else if ("grid".equals(z) && arr.length == 2) {
      GridFactory.openGrid(arr[1]);

    } else if (z.startsWith("gridinf")) {
      GridFactory.showGridInfo(args);

    } else if ("gum".equals(z)) {
      showUserMedia(arr, errorPopup);

    } else if ("gwt".equals(z)) {
      showGwt();

    } else if (BeeUtils.inList(z, "h5", "html5") || z.startsWith("feat")) {
      showSupport(args, errorPopup);

    } else if (z.startsWith("hist")) {
      Global.showModalGrid("History", new PropertiesData(Historian.getInstance().getInfo()));

    } else if ("id".equals(z)) {
      showElement(v, arr, errorPopup);

    } else if ("iddqd".equals(z)) {
      respectMyAuthoritah();

    } else if (z.startsWith("image")) {
      showImages(arr);

    } else if ("inject".equals(z) && arr.length == 2) {
      DomUtils.injectExternalScript(arr[1]);

    } else if (z.startsWith("inp") && z.contains("type")) {
      showInputTypes();

    } else if (z.startsWith("inp") && z.contains("box") || "prompt".equals(z)) {
      showInputBox(arr);

    } else if ("jdbc".equals(z)) {
      doJdbc(args, errorPopup);

    } else if (BeeUtils.inList(z, "keys", "pk")) {
      getKeys(arr, errorPopup);

    } else if (z.startsWith("like") && arr.length >= 3) {
      doLike(arr);

    } else if ("loaders".equals(z)) {
      BeeKeeper.getRpc().invoke("loaderInfo", ResponseHandler.callback(z));

    } else if (z.startsWith("loc")) {
      doLocale(arr, args);

    } else if ("log".equals(z)) {
      doLog(arr);

    } else if ("login_bg".equals(z)) {
      setLoginBackground(args);

    } else if ("mail".equals(z)) {
      BeeKeeper.getRpc().sendText(new ParameterList(Service.MAIL), args, null);

    } else if ("map".equals(z) && arr.length >= 3) {
      showMap(arr, errorPopup);

    } else if ("md".equals(z) && !args.isEmpty()) {
      CustomDiv widget = new CustomDiv();
      widget.setHtml(Markdown.toHtml(args));
      Global.showModalWidget(args, widget);

    } else if ("md5".equals(z)) {
      digest(v);

    } else if ("menu".equals(z)) {
      doMenu(args, errorPopup);

    } else if ("meter".equals(z)) {
      showMeter(arr, errorPopup);

    } else if ("money".equals(z)) {
      showExtData(z, Money.getExtendedInfo());

    } else if ("nf".equals(z) && arr.length >= 3) {
      logger.info(NumberFormat.getFormat(arr[1]).format(BeeUtils.toDouble(arr[2])));

    } else if ("notify".equals(z) && arr.length >= 2) {
      showNotes(args);

    } else if (z.startsWith("omni")) {
      logger.debug(z, ColumnDescription.toggleOmniView());

    } else if (BeeUtils.inList(z, "p", "prop")) {
      showProperties(v, arr, errorPopup);

    } else if (z.startsWith("ping")) {
      BeeKeeper.getRpc().makeGetRequest(Service.DB_PING, ResponseHandler.callback(z));

    } else if (z.startsWith("prev")) {
      Global.showModalGrid("Previewers", new PropertiesData(Previewer.getInstance().getInfo()));

    } else if (z.startsWith("print")) {
      print(args, errorPopup);

    } else if ("progress".equals(z)) {
      showProgress(arr, errorPopup);

    } else if ("rates".equals(z)) {
      showRates(args, errorPopup);

    } else if ("rebuild".equals(z)) {
      rebuildSomething(args);

    } else if (z.startsWith("rect") && arr.length >= 2) {
      showRectangle(arr[1], errorPopup);

    } else if (z.startsWith("rot") || "scale".equals(z) || "skew".equals(z) || "tt".equals(z)) {
      animate(arr);

    } else if ("rpc".equals(z)) {
      showRpc(args);

    } else if ("rtc".equals(z)) {
      doRtc(args, errorPopup);

    } else if ("rts".equals(z)) {
      scheduleTasks(arr, errorPopup);

    } else if ("run".equals(z)) {
      BeeKeeper.getRpc().sendText(new ParameterList(Service.RUN), args, null);

    } else if ("script".equals(z)) {
      if (args.isEmpty() || "info".equals(args)) {
        BeeKeeper.getRpc().invoke("scriptEngineInfo", ResponseHandler.callback(v));
      } else {
        BeeKeeper.getRpc().invoke("execScript", args, ResponseHandler.callback(args));
      }

    } else if (z.startsWith("selector") && arr.length >= 2) {
      querySelector(z, args, errorPopup);

    } else if (z.startsWith("serv") || z.startsWith("sys")) {
      BeeKeeper.getRpc().invoke("systemInfo", ResponseHandler.callback(z));

    } else if ("settings".equals(z)) {
      showPropData(v, Settings.getInfo());

    } else if (z.startsWith("sheets")) {
      Global.showTable(v, new PropertiesData(Global.getStyleSheets()));

    } else if ("size".equals(z) && arr.length >= 2) {
      showSize(arr);

    } else if ("sleep".equals(z) && arr.length >= 2) {
      sleep(arr);

    } else if ("slider".equals(z)) {
      showSlider(arr);

    } else if ("sql".equals(z)) {
      doSql(args);

    } else if ("stacking".equals(z) || z.startsWith("zind") || z.startsWith("z-ind")) {
      showPropData(v, Stacking.getInfo());

    } else if (z.startsWith("stor")) {
      storage(arr, errorPopup);

    } else if ("style".equals(z)) {
      style(v, arr, errorPopup);

    } else if (z.startsWith("suppl")) {
      showViewSuppliers();

    } else if ("svg".equals(z)) {
      showSvg(arr);

    } else if (z.startsWith("tabl") || z.startsWith("tbl")) {
      showTableInfo(v, args);

    } else if (z.startsWith("theme")) {
      JSONObject theme = Theme.getValues();
      inform(z, (theme == null) ? BeeConst.NULL : theme.toString());

    } else if (z.startsWith("tile")) {
      doTiles(args);

    } else if ("uc".equals(z) || "unicode".startsWith(z)) {
      unicode(arr);

    } else if (z.startsWith("unit")) {
      showUnits(arr);

    } else if (z.startsWith("upl")) {
      upload();

    } else if (z.startsWith("user")) {
      showPropData(v, BeeKeeper.getUser().getInfo());

    } else if ("video".equals(z)) {
      playVideo(args);

    } else if (z.startsWith("view")) {
      showViewInfo(v, args);

    } else if ("vm".equals(z)) {
      BeeKeeper.getRpc().invoke("vmInfo", ResponseHandler.callback(z));

    } else if ("widget".equals(z) && arr.length >= 2) {
      showWidgetInfo(arr, errorPopup);

    } else if ("wn".equals(z) || z.startsWith("webnot")) {
      showWebNote(arr);

    } else if ("ws".equals(z) || z.startsWith("websock")) {
      doWebSocket(args, arr);

    } else {
      showError(errorPopup, "command not recognized", v);
      return false;
    }
    return true;
  }

  private static void animate(String[] arr) {

    final class Raf extends RafCallback {
      private final String function;
      private Axis axis;

      private final EnumMap<Axis, Double> from = new EnumMap<>(Axis.class);
      private final EnumMap<Axis, Double> to = new EnumMap<>(Axis.class);

      private Style style;

      private Raf(String trf, double duration) {
        super(duration);

        double lower;
        double upper;

        if (trf.startsWith("r")) {
          function = StyleUtils.TRANSFORM_ROTATE;
          lower = 0.0;
          upper = 360.0;

        } else if (trf.startsWith("sk")) {
          function = StyleUtils.TRANSFORM_SKEW;
          axis = Axis.X;
          lower = 0.0;
          upper = 180.0;

        } else if (trf.startsWith("t")) {
          function = StyleUtils.TRANSFORM_TRANSLATE;
          lower = 0.0;
          upper = 50.0;

        } else {
          function = StyleUtils.TRANSFORM_SCALE;
          lower = 1.0;
          upper = 0.1;
        }

        for (Axis a : Axis.values()) {
          from.put(a, lower);
          to.put(a, upper);
        }
      }

      @Override
      protected void onComplete() {
        StyleUtils.clearTransform(style);
      }

      @Override
      protected boolean run(double elapsed) {
        if (axis == null) {
          if (isRotate()) {
            StyleUtils.setTransformRotate(style, getInt(elapsed, Axis.X), CssAngle.DEG);
          } else if (isScale()) {
            StyleUtils.setTransformScale(style, getDouble(elapsed, Axis.X),
                getDouble(elapsed, Axis.Y));
          } else if (isSkew()) {
            StyleUtils.setTransformSkew(style, Axis.X, getInt(elapsed, Axis.X), CssAngle.DEG);
          } else if (isTranslate()) {
            StyleUtils.setTransformTranslate(style, getDouble(elapsed, Axis.X), CssUnit.PX,
                getDouble(elapsed, Axis.Y), CssUnit.PX);
          }

        } else if (isRotate()) {
          StyleUtils.setTransformRotate(style, axis, getInt(elapsed, axis), CssAngle.DEG);
        } else if (isScale()) {
          StyleUtils.setTransformScale(style, axis, getDouble(elapsed, axis));
        } else if (isSkew()) {
          StyleUtils.setTransformSkew(style, axis, getInt(elapsed, axis), CssAngle.DEG);
        } else if (isTranslate()) {
          StyleUtils.setTransformTranslate(style, axis, getDouble(elapsed, axis), CssUnit.PX);
        }

        return true;
      }

      private double getDouble(double elapsed, Axis a) {
        if (elapsed < getDuration() / 2) {
          return BeeUtils.rescale(elapsed, 0, getDuration() / 2, from.get(a), to.get(a));
        } else {
          return BeeUtils
              .rescale(elapsed, getDuration() / 2, getDuration(), to.get(a), from.get(a));
        }
      }

      private int getInt(double elapsed, Axis a) {
        return BeeUtils.round(getDouble(elapsed, a));
      }

      private boolean isRotate() {
        return StyleUtils.TRANSFORM_ROTATE.equals(function);
      }

      private boolean isScale() {
        return StyleUtils.TRANSFORM_SCALE.equals(function);
      }

      private boolean isSkew() {
        return StyleUtils.TRANSFORM_SKEW.equals(function);
      }

      private boolean isTranslate() {
        return StyleUtils.TRANSFORM_TRANSLATE.equals(function);
      }
    }

    Raf raf = new Raf(arr[0], 5000);

    for (int i = 1; i < ArrayUtils.length(arr); i++) {
      char prop = arr[i].toLowerCase().charAt(0);
      String value = (arr[i].length() > 1) ? arr[i].substring(1) : BeeConst.STRING_EMPTY;

      value = BeeUtils.removePrefix(value, '=');
      value = BeeUtils.removePrefix(value, ':');

      switch (prop) {
        case '#':
          if (!value.isEmpty()) {
            Element elem = Document.get().getElementById(value);
            if (elem == null) {
              logger.warning("element id:", value, "not found");
            } else {
              raf.style = elem.getStyle();
              logger.debug("id", value, elem.getTagName(), DomUtils.getClassName(elem));
            }
          }
          break;

        case 'd':
          if (BeeUtils.isPositiveDouble(value)) {
            raf.setDuration(BeeUtils.toDouble(value));
            logger.debug("duration", raf.getDuration());
          }
          break;

        case 'l':
          if (BeeUtils.isDouble(value)) {
            double lower = BeeUtils.toDouble(value);
            for (Axis a : Axis.values()) {
              raf.from.put(a, lower);
            }
            logger.debug("from", lower);
          }
          break;

        case 'u':
          if (BeeUtils.isDouble(value)) {
            double upper = BeeUtils.toDouble(value);
            for (Axis a : Axis.values()) {
              raf.to.put(a, upper);
            }
            logger.debug("to", upper);
          }
          break;

        case 'x':
          if (value.isEmpty() || raf.isRotate() || raf.isSkew()) {
            raf.axis = Axis.X;
            logger.debug("axis x");
          }
          if (BeeUtils.isDouble(value)) {
            raf.to.put(Axis.X, BeeUtils.toDouble(value));
            logger.debug("to x", raf.to.get(Axis.X));
          }
          break;

        case 'y':
          if (value.isEmpty() || raf.isRotate() || raf.isSkew()) {
            raf.axis = Axis.Y;
            logger.debug("axis y");
          }
          if (BeeUtils.isDouble(value)) {
            raf.to.put(Axis.Y, BeeUtils.toDouble(value));
            logger.debug("to y", raf.to.get(Axis.Y));
          }
          break;

        case 'z':
          raf.axis = Axis.Z;
          logger.debug("axis z");
          if (BeeUtils.isDouble(value)) {
            raf.to.put(Axis.Z, BeeUtils.toDouble(value));
            logger.debug("to z", raf.to.get(Axis.Z));
          }
          break;
      }
    }

    if (raf.style == null) {
      IdentifiableWidget widget = BeeKeeper.getScreen().getActiveWidget();
      if (widget == null) {
        widget = BeeKeeper.getScreen().getScreenPanel();
      }
      raf.style = widget.asWidget().getElement().getStyle();
    }

    raf.start();
  }

  private static void clear(String args) {
    if (BeeUtils.isEmpty(args) || BeeUtils.startsSame(args, "log")) {
      ClientLogManager.clearPanel();

    } else if (BeeUtils.startsSame(args, "grid")) {
      GridFactory.clearDescriptionCache();
      debugWithSeparator("grid cache cleared");

    } else if (BeeUtils.startsSame(args, "form")) {
      FormFactory.clearDescriptionCache();
      debugWithSeparator("form cache cleared");

    } else if (BeeUtils.startsSame(args, "cache")) {
      Global.getCache().clear();
      debugWithSeparator("cache cleared");

    } else if (BeeUtils.startsSame(args, "rpc")) {
      BeeKeeper.getRpc().getRpcList().clear();
      debugWithSeparator("rpc list cleared");

    } else if (BeeUtils.startsSame(args, "hist")) {
      Historian.getInstance().clear();
      debugWithSeparator("history cleared");

    } else if (BeeUtils.startsSame(args, "widget")) {
      ViewFactory.clear();
      debugWithSeparator("widget factory cleared");

    } else if (BeeUtils.startsSame(args, "export")) {
      Exporter.clearServerCache(null);

    } else if (BeeUtils.startsSame(args, "size")) {
      Data.clearApproximateSizes();
      debugWithSeparator("approximate sizes cleared");
    }
  }

  private static void cornify(String[] arr) {
    if (!cornified) {
      DomUtils.injectExternalScript("http://www.cornify.com/js/cornify.js");
      cornified = true;
    }

    final int cnt = BeeUtils.clamp(BeeUtils.toInt(ArrayUtils.getQuietly(arr, 1)), 1, 50);

    int delay = BeeUtils.toInt(ArrayUtils.getQuietly(arr, 2));
    if (delay <= 0) {
      delay = 2000;
    }

    Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
      private int counter;

      @Override
      public boolean execute() {
        cornifyAdd();
        return ++counter < cnt;
      }
    }, delay);
  }

  //@formatter:off
  private static native void cornifyAdd() /*-{
    try {
      $wnd.cornify_add();
    } catch (err) {
    }
  }-*/;
//@formatter:on

  private static void createResource(String args, boolean errorPopup) {
    if (BeeUtils.isEmpty(args)) {
      showError(errorPopup, "path not specified");

    } else {
      Resource resource = new Resource(args, BeeConst.STRING_EMPTY);
      ResourceEditor resourceEditor = new ResourceEditor(resource);

      BeeKeeper.getScreen().show(resourceEditor);
    }
  }

  private static void debugWithSeparator(String message) {
    logger.debug(message);
    logger.addSeparator();
  }

  private static void digest(String v) {
    String src = null;
    int p = v.indexOf(BeeConst.CHAR_SPACE);

    if (p > 0) {
      String z = v.substring(p + 1).trim();

      if (BeeUtils.isDigit(z)) {
        int x = BeeUtils.toInt(z);
        if (BeeUtils.betweenInclusive(x, 1, BeeUtils.exp10(6))) {
          src = BeeUtils.randomString(x, x, BeeConst.CHAR_SPACE, '\u0800');
        } else {
          src = z;
        }
      } else if (z.length() > 0) {
        src = z;
      }
    }

    if (src == null) {
      src = BeeUtils.randomString(10, 20, BeeConst.CHAR_SPACE, '\u0400');
    }

    if (src.length() > 100) {
      logger.info("Source length", src.length());
    } else {
      logger.info(Codec.escapeUnicode(src));
    }
    logger.info(BeeConst.CLIENT, Codec.md5(src));

    BeeKeeper.getRpc().makePostRequest(Service.GET_DIGEST, ContentType.HTML, src);
  }

  private static void doAdmin(String note, String args, boolean errorPopup) {
    String cmnd;

    boolean all;
    Set<Long> userIds = new HashSet<>();

    if (args.startsWith(BeeConst.STRING_ASTERISK)) {
      all = true;
      cmnd = BeeUtils.removePrefix(args.substring(1).trim(), BeeConst.STRING_ASTERISK);

    } else if (args.startsWith(BeeConst.STRING_LEFT_PARENTHESIS)
        || args.startsWith(BeeConst.STRING_LEFT_BRACKET)) {

      int endIndex = args.indexOf(args.startsWith(BeeConst.STRING_LEFT_PARENTHESIS)
          ? BeeConst.STRING_RIGHT_PARENTHESIS : BeeConst.STRING_RIGHT_BRACKET);
      if (endIndex <= 0) {
        showError(errorPopup, args, "user list not closed");
        return;
      }

      String userList = args.substring(1, endIndex).trim();
      if (BeeUtils.isEmpty(userList) || BeeUtils.containsOnly(userList, BeeConst.CHAR_ASTERISK)) {
        all = true;
      } else {
        all = false;

        List<String> userNames = NameUtils.toList(userList);
        for (String userName : userNames) {
          Long userId = Global.getUsers().parseUserName(userName, true);
          if (userId == null) {
            showError(errorPopup, "user not found", userName);
            return;
          }

          userIds.add(userId);
        }
      }

      cmnd = args.substring(endIndex + 1);

    } else {
      all = true;
      cmnd = args;
    }

    if (BeeUtils.isEmpty(cmnd)) {
      showError(errorPopup, "cannot parse", args);
      return;
    }

    Set<String> sessions = all ? Global.getUsers().getAllSessions()
        : Global.getUsers().getSessions(userIds);
    if (BeeUtils.isEmpty(sessions)) {
      showError(errorPopup, "no open sessions found");
      return;
    }

    DisplayMode displayMode = null;
    for (DisplayMode dm : DisplayMode.values()) {
      if (BeeUtils.containsSame(note, BeeUtils.left(dm.name(), 3))) {
        displayMode = dm;
        break;
      }
    }

    if (displayMode == null) {
      for (String session : sessions) {
        Endpoint.send(AdminMessage.command(Endpoint.getSessionId(), session, cmnd));
      }

    } else {
      Icon icon = null;
      for (Icon ic : Icon.values()) {
        if (BeeUtils.containsSame(note, BeeUtils.left(ic.name(), 3))) {
          icon = ic;
          break;
        }
      }

      for (String session : sessions) {
        NotificationMessage notificationMessage = NotificationMessage.create(displayMode,
            Endpoint.getSessionId(), session,
            new TextMessage(BeeKeeper.getUser().getUserId(), cmnd));
        if (icon != null) {
          notificationMessage.setIcon(icon.name());
        }

        Endpoint.send(notificationMessage);
      }
    }
  }

  private static void doCollections(String[] arr) {
    int size = 2000;
    int query = 20;

    for (int i = 1; i < arr.length; i++) {
      if (BeeUtils.isPositiveInt(arr[i])) {
        if (i == 1) {
          size = BeeUtils.toInt(arr[i]);
        } else {
          query = BeeUtils.toInt(arr[i]);
        }
      }
    }

    List<Integer> lia = new ArrayList<>();
    List<Long> lla = new ArrayList<>();
    List<Double> lda = new ArrayList<>();
    List<String> lsa = new ArrayList<>();

    List<Integer> lil = new LinkedList<>();
    List<Long> lll = new LinkedList<>();
    List<Double> ldl = new LinkedList<>();
    List<String> lsl = new LinkedList<>();

    Map<Integer, String> mih = new HashMap<>();
    Map<Long, String> mlh = new HashMap<>();
    Map<Double, String> mdh = new HashMap<>();
    Map<String, String> msh = new HashMap<>();

    Map<Integer, String> mil = new LinkedHashMap<>();
    Map<Long, String> mll = new LinkedHashMap<>();
    Map<Double, String> mdl = new LinkedHashMap<>();
    Map<String, String> msl = new LinkedHashMap<>();

    for (int i = 0; i < size; i++) {
      String s = Integer.toString(i);

      lia.add(i);
      lla.add((long) i);
      lda.add((double) i);
      lsa.add(s);

      lil.add(i);
      lll.add((long) i);
      ldl.add((double) i);
      lsl.add(s);

      mih.put(i, s);
      mlh.put((long) i, s);
      mdh.put((double) i, s);
      msh.put(s, s);

      mil.put(i, s);
      mll.put((long) i, s);
      mdl.put((double) i, s);
      msl.put(s, s);
    }

    logger.debug(size, query);

    double x = 0;
    long start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += lia.indexOf((int) (Math.random() * size));
    }
    logger.debug("lia", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += lla.indexOf((long) (Math.random() * size));
    }
    logger.debug("lla", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += lda.indexOf(Math.floor(Math.random() * size));
    }
    logger.debug("lda", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += lsa.indexOf(Integer.toString((int) (Math.random() * size)));
    }
    logger.debug("lsa", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += lil.indexOf((int) (Math.random() * size));
    }
    logger.debug("lil", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += lll.indexOf((long) (Math.random() * size));
    }
    logger.debug("lll", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += ldl.indexOf(Math.floor(Math.random() * size));
    }
    logger.debug("ldl", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += lsl.indexOf(Integer.toString((int) (Math.random() * size)));
    }
    logger.debug("lsl", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += (mih.get((int) (Math.random() * size)) == null) ? 0 : 1;
    }
    logger.debug("mih", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += (mlh.get((long) (Math.random() * size)) == null) ? 0 : 1;
    }
    logger.debug("mlh", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += (mdh.get(Math.floor(Math.random() * size)) == null) ? 0 : 1;
    }
    logger.debug("mdh", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += (msh.get(Integer.toString((int) (Math.random() * size))) == null) ? 0 : 1;
    }
    logger.debug("msh", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += (mil.get((int) (Math.random() * size)) == null) ? 0 : 1;
    }
    logger.debug("mil", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += (mll.get((long) (Math.random() * size)) == null) ? 0 : 1;
    }
    logger.debug("mll", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += (mdl.get(Math.floor(Math.random() * size)) == null) ? 0 : 1;
    }
    logger.debug("mdl", System.currentTimeMillis() - start, x);

    x = 0;
    start = System.currentTimeMillis();
    for (int i = 0; i < query; i++) {
      x += (msl.get(Integer.toString((int) (Math.random() * size))) == null) ? 0 : 1;
    }
    logger.debug("msl", System.currentTimeMillis() - start, x);
    logger.addSeparator();
  }

  private static void doDictionary(String args, boolean errorPopup) {
    final String service;

    if (BeeUtils.startsSame(args, "get", "load")) {
      service = SVC_GET_DICTIONARY;

    } else if (BeeUtils.startsSame(args, "d2p", "b2p")) {
      service = SVC_DICTIONARY_DATABASE_TO_PROPERTIES;

    } else {
      showError(errorPopup, "dictionary service not recognized:", args);
      return;
    }

    BeeKeeper.getRpc().makeRequest(AdministrationKeeper.createArgs(service),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (!response.hasErrors()) {
              if (SVC_GET_DICTIONARY.equals(service)) {
                Localized.setGlossary(Codec.deserializeHashMap(response.getResponseAsString()));
                logger.debug(service, Localized.getGlossary().size());

              } else {
                logger.debug(service, response.getResponse());
              }
            }
          }
        });
  }

  private static void doDsn() {
    BeeKeeper.getRpc().makeGetRequest(Service.GET_DSNS, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        final List<String> dsns = new ArrayList<>();
        String current = null;

        if (response.hasResponse()) {
          char defChar = '*';
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (arr != null) {
            for (String s : arr) {
              if (BeeUtils.isPrefixOrSuffix(s, defChar)) {
                current = BeeUtils.removePrefixAndSuffix(s, defChar);
                dsns.add(current);
              } else {
                dsns.add(s);
              }
            }
          }
        }
        if (dsns.isEmpty()) {
          Global.showError(Collections.singletonList("No DSN's available"));

        } else if (dsns.size() == 1) {
          inform("Only one DSN is available:", dsns.get(0));

        } else {
          Horizontal panel = new Horizontal();
          panel.add(new Label("Available DSN's"));

          final String currentDsn = current;
          final RadioGroup rg = new RadioGroup(Orientation.VERTICAL, dsns.indexOf(currentDsn),
              dsns);
          panel.add(rg);

          Global.inputWidget("Choose DSN", panel, () -> {
            String dsn = dsns.get(rg.getSelectedIndex());

            if (!BeeUtils.same(dsn, currentDsn)) {
              ParameterList params = BeeKeeper.getRpc().createParameters(Service.SWITCH_DSN);
              params.addQueryItem(Service.VAR_DSN, dsn);

              BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject rsp) {
                  logger.info("DSN switched to:", rsp.getResponseAsString());
                }
              });
            }
          });
        }
      }
    });
  }

  private static void doEval(String args) {
    String result;
    if (BeeUtils.isDigit(args)) {
      result = new DateTime(BeeUtils.toLong(args)).toString();
    } else {
      result = JsUtils.evalToString(args);
    }

    inform(args, result);
  }

  private static void doJdbc(String args, boolean errorPopup) {
    if (BeeUtils.isEmpty(args)) {
      showError(errorPopup, "Query not specified");
      return;
    }

    Splitter splitter = Splitter.on(BeeConst.CHAR_SEMICOLON).omitEmptyStrings().trimResults();
    List<String> input = splitter.splitToList(args);

    if (BeeUtils.isEmpty(input)) {
      showError(errorPopup, "Query not specified");
      return;
    }

    Map<String, String> params = new HashMap<>();
    params.put(Service.VAR_JDBC_QUERY, input.get(0));

    if (input.size() > 1) {
      Set<String> keys = Sets.newHashSet(
          Service.VAR_CONNECTION_AUTO_COMMIT,
          Service.VAR_CONNECTION_HOLDABILITY,
          Service.VAR_CONNECTION_READ_ONLY,
          Service.VAR_CONNECTION_TRANSACTION_ISOLATION,
          Service.VAR_STATEMENT_CURSOR_NAME,
          Service.VAR_STATEMENT_ESCAPE_PROCESSING,
          Service.VAR_STATEMENT_FETCH_DIRECTION,
          Service.VAR_STATEMENT_FETCH_SIZE,
          Service.VAR_STATEMENT_MAX_FIELD_SIZE,
          Service.VAR_STATEMENT_MAX_ROWS,
          Service.VAR_STATEMENT_POOLABLE,
          Service.VAR_STATEMENT_QUERY_TIMEOUT,
          Service.VAR_STATEMENT_RS_TYPE,
          Service.VAR_STATEMENT_RS_CONCURRENCY,
          Service.VAR_STATEMENT_RS_HOLDABILITY,
          Service.VAR_RESULT_SET_FETCH_DIRECTION,
          Service.VAR_RESULT_SET_FETCH_SIZE,
          Service.VAR_JDBC_RETURN);

      for (int i = 1; i < input.size(); i++) {
        String s = input.get(i);

        char sep = (s.indexOf(BeeConst.CHAR_EQ) > 0) ? BeeConst.CHAR_EQ : BeeConst.CHAR_SPACE;
        String name = BeeUtils.getPrefix(s, sep);
        String value = BeeUtils.getSuffix(s, sep);

        if (!BeeUtils.isEmpty(name) && !BeeUtils.isEmpty(value)) {
          for (String key : keys) {
            if (BeeUtils.isSuffix(key, name)) {
              params.put(key, value);
              break;
            }
          }
        }
      }
    }

    BeeKeeper.getRpc().makePostRequest(Service.DB_JDBC,
        XmlUtils.createString(Service.VAR_DATA, params), ResponseHandler.callback(args));
  }

  private static void doLike(String[] arr) {
    int len = ArrayUtils.length(arr);
    if (len < 3) {
      Global.sayHuh(ArrayUtils.join(BeeConst.STRING_SPACE, arr));
      return;
    }

    String mode = ArrayUtils.getQuietly(arr, 0);
    String input = ArrayUtils.join(BeeConst.STRING_SPACE, arr, 1, len - 1);
    String expr = ArrayUtils.getQuietly(arr, len - 1);

    boolean sens = mode.indexOf('+') > 0;
    boolean insens = mode.indexOf('-') > 0;
    String defCase = null;
    boolean match;

    if (BeeUtils.containsSame(mode, "s")) {
      if (sens || insens) {
        match = Wildcards.isSqlLike(input, expr, sens);
      } else {
        defCase = BeeUtils.joinWords("sql", BooleanValue.pack(Wildcards.isSqlCaseSensitive()));
        match = Wildcards.isSqlLike(input, expr);
      }
    } else if (BeeUtils.containsSame(mode, "f")) {
      if (sens || insens) {
        match = Wildcards.isFsLike(input, expr, sens);
      } else {
        defCase = BeeUtils.joinWords("fs", BooleanValue.pack(Wildcards.isFsCaseSensitive()));
        match = Wildcards.isFsLike(input, expr);
      }
    } else {
      if (sens || insens) {
        match = Wildcards.isLike(input, expr, sens);
      } else {
        defCase = BeeUtils.joinWords("def",
            BooleanValue.pack(Wildcards.isDefaultCaseSensitive()));
        match = Wildcards.isLike(input, expr);
      }
    }

    inform(mode, NameUtils.addName("input", input), NameUtils.addName("pattern", expr),
        NameUtils.addName("case", sens ? "sensitive" : (insens ? "insensitive" : defCase)),
        NameUtils.addName("match", BeeUtils.toString(match)));
  }

  private static void doLocale(String[] arr, String args) {
    if (BeeUtils.contains(arr[0], 's')) {
      BeeKeeper.getRpc().invoke("localeInfo", args,
          ResponseHandler.callback(ArrayUtils.joinWords(arr)));

    } else if (BeeUtils.isEmpty(args)) {
      showExtData("Locale info", LocaleUtils.getInfo());

    } else {
      List<Property> info = new ArrayList<>();
      for (int i = 1; i < arr.length; i++) {
        String value = Localized.translate(arr[i]);
        PropertyUtils.addProperty(info, arr[i], BeeUtils.notEmpty(value, BeeConst.NULL));
      }
      showPropData("Locale info", info);
    }
  }

  private static void doLog(String[] arr) {
    if (ArrayUtils.length(arr) > 1) {
      String z = arr[1];

      if (BeeUtils.inList(z, BeeConst.STRING_ZERO, BeeConst.STRING_MINUS)) {
        ClientLogManager.setPanelVisible(false);

      } else if (z.equals(BeeConst.STRING_PLUS)) {
        ClientLogManager.setPanelVisible(true);

      } else if (BeeUtils.isDigit(z)) {
        ClientLogManager.setPanelSize(BeeUtils.toInt(z));

      } else if (BeeUtils.startsSame(z, "clear")) {
        ClientLogManager.clearPanel();

      } else if (BeeUtils.startsSame(z, "level")) {
        ClientLogManager.setPanelVisible(true);

        LogLevel level = (arr.length > 2) ? LogLevel.parse(arr[2]) : null;
        if (level == null) {
          for (LogLevel lvl : LogLevel.values()) {
            logger.log(lvl, lvl.name().toLowerCase());
          }
          logger.addSeparator();

          inform(z, (logger.getLevel() == null) ? BeeConst.NULL : logger.getLevel().name());

        } else {
          logger.setLevel(level);
          logger.log(level, "level", level);
          logger.log(level, LogFormatter.LOG_SEPARATOR_TAG);
        }

      } else {
        ClientLogManager.setPanelVisible(true);
        logger.info((Object[]) arr);
        logger.addSeparator();
      }

    } else {
      ClientLogManager.setPanelVisible(!ClientLogManager.isPanelVisible());
    }
  }

  private static void doMenu(String args, boolean errorPopup) {
    if (BeeUtils.isEmpty(args)) {
      BeeKeeper.getMenu().showMenuInfo();

    } else if (BeeUtils.same(args, "load")) {
      BeeKeeper.getMenu().loadMenu();

    } else if (BeeUtils.same(args, "refresh")) {
      BeeKeeper.getMenu().refresh();

    } else if (BeeUtils.isDigit(args)) {
      BeeKeeper.getScreen().closeAll();

      final List<MenuCommand> commands = new ArrayList<>();

      List<MenuCommand> menuCommands = BeeKeeper.getMenu().getCommands();
      for (MenuCommand command : menuCommands) {
        switch (command.getService()) {
          case ASSESSMENTS_GRID:
          case DISCUSS_LIST:
          case DRIVER_TIME_BOARD:
          case EDIT_EC_CONTACTS:
          case EDIT_TERMS_OF_DELIVERY:
          case ENSURE_CATEGORIES_AND_OPEN_GRID:
          case FORM:
          case FREIGHT_EXCHANGE:
          case GRID:
          case ITEMS:
          case OPEN_MAIL:
          case PARAMETERS:
          case REPORT:
          case SERVICE_CALENDAR:
          case SERVICE_MAINTENANCE_LIST:
          case SHIPPING_SCHEDULE:
          case TASK_LIST:
          case TASK_REPORTS:
          case TRAILER_TIME_BOARD:
          case TRUCK_TIME_BOARD:
          case TRADE_ACT_LIST:
          case TRADE_DOCUMENTS:
            commands.add(command);
            break;

          default:
            logger.debug("skip", command.getService(), command.getParameters());
            break;
        }
      }

      final Holder<Integer> position = Holder.of(0);
      final Holder<String> progId = Holder.of(null);

      InlineLabel close = new InlineLabel(String.valueOf(BeeConst.CHAR_TIMES));
      close.addClickHandler(event -> {
        BeeKeeper.getScreen().removeProgress(progId.get());
        progId.set(null);
      });

      Thermometer thermometer = new Thermometer(args, (double) commands.size(), close);
      progId.set(BeeKeeper.getScreen().addProgress(thermometer));

      final Timer timer = new Timer() {
        @Override
        public void run() {
          int index = position.get();

          if (index < commands.size()
              && BeeKeeper.getScreen().updateProgress(progId.get(), null, index + 0.5)) {

            position.set(index + 1);

            MenuCommand command = commands.get(index);
            logger.debug(index, command.getService(), command.getParameters());

            command.execute();

          } else {
            cancel();
            if (progId.isNotNull()) {
              BeeKeeper.getScreen().removeProgress(progId.get());
            }
          }
        }
      };

      int period = Math.max(BeeUtils.toInt(args), 1);
      timer.scheduleRepeating(period);

    } else {
      showError(errorPopup, args);
    }
  }

  private static void doRtc(String args, boolean errorPopup) {
    if (BeeUtils.isEmpty(args)) {
      showPropData("rtc adapter", RtcAdapter.getInfo());
      return;
    }

    if (!Features.supportsWebRtc()) {
      showError(errorPopup, "rtc not supported");
      return;
    }

    switch (args) {
      case "b":
        BeeKeeper.getScreen().show(RtcUtils.createBasicDemo());
        return;

      case "t":
        BeeKeeper.getScreen().show(RtcUtils.createTextDemo());
        return;
    }

    Long userId = Global.getUsers().parseUserName(args, true);
    if (userId == null) {
      showError(errorPopup, "user not found", args);
      return;
    } else if (BeeKeeper.getUser().is(userId)) {
      showError(errorPopup, "me myself & I");
      return;
    }

    Set<String> sessions = Global.getUsers().getSessions(Collections.singleton(userId));
    if (BeeUtils.isEmpty(sessions)) {
      showError(errorPopup, args, "no open sessions found");
      return;
    }

    String session = BeeUtils.peek(sessions);
    RtcUtils.call(session);
  }

  private static void doScreen(String[] arr, boolean errorPopup) {
    Split screen = BeeKeeper.getScreen().getScreenPanel();
    Assert.notNull(screen);

    String p1 = ArrayUtils.getQuietly(arr, 0);
    String p2 = ArrayUtils.getQuietly(arr, 1);

    if (BeeUtils.same(p1, "screen")) {
      showExtData(p1, screen.getExtendedInfo());
      return;
    }

    Direction dir = EnumUtils.getEnumByName(Direction.class, p1);
    if (dir == null) {
      Global.sayHuh(p1, p2);
      return;
    }

    if (BeeUtils.isEmpty(p2)) {
      showExtData(p1, screen.getDirectionInfo(dir));
      return;
    }

    if (!BeeUtils.isInt(p2)) {
      showError(errorPopup, p1, p2, "size not an int");
      return;
    }

    screen.setDirectionSize(dir, BeeUtils.toInt(p2), true);
  }

  private static void doSql(final String sql) {
    BeeKeeper.getRpc().sendText(new ParameterList(Service.DO_SQL), sql,
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            Assert.notNull(response);

            if (response.hasResponse(BeeRowSet.class)) {
              BeeRowSet rs = BeeRowSet.restore((String) response.getResponse());

              if (rs.isEmpty()) {
                logger.debug("sql: RowSet is empty");
              } else {
                Global.showTable(BeeUtils.clip(sql, 100), rs);
              }
            }
          }
        });
  }

  private static void doTiles(String args) {
    if (BeeUtils.isInt(args)) {
      int cnt = BeeUtils.toInt(args);

      if (cnt > 0) {
        for (int i = 0; i < cnt; i++) {
          BeeKeeper.getScreen().getWorkspace().randomSplit();
        }
      } else {
        boolean debug = Global.isDebug();
        for (int i = 0; i > cnt; i--) {
          boolean ok = BeeKeeper.getScreen().getWorkspace().randomClose(debug);
          if (!ok) {
            break;
          }
        }
      }

    } else {
      List<ExtendedProperty> info = BeeKeeper.getScreen().getExtendedInfo();

      ExtendedPropertiesData data = new ExtendedPropertiesData(info, false);
      Global.showModalGrid("Screen", data);
    }
  }

  private static void doWebSocket(String args, String[] arr) {
    if (BeeUtils.isEmpty(args) || BeeUtils.same(args, "info")) {
      showTable("WebSocket", new PropertiesData(Endpoint.getInfo()));

    } else if (BeeUtils.same(args, "open")) {
      if (Endpoint.isOpen()) {
        inform("endpoint already open");
      } else {
        logger.debug("opening endpoint");
        Endpoint.open(BeeKeeper.getUser().getUserId(),
            input -> logger.debug("endpoint open", input));
      }

    } else if (BeeUtils.same(args, "close")) {
      if (Endpoint.isOpen()) {
        logger.debug("closing endpoint");
        Endpoint.close();
      } else {
        inform("endpoint not open");
      }

    } else if (!Endpoint.isOpen()) {
      inform("endpoint not open");

    } else if (BeeUtils.same(args, "session")) {
      Endpoint.send(ShowMessage.showSessionInfo());
    } else if (BeeUtils.same(args, "users")) {
      Endpoint.send(ShowMessage.showOpenSessions());
    } else if (BeeUtils.same(args, "server")) {
      Endpoint.send(ShowMessage.showEndpoint());

    } else if (BeeUtils.inListSame(args, "async", "basic")) {
      Endpoint.send(ConfigMessage.switchRemoteEndpointType(args));

    } else {
      LogLevel level = LogLevel.parse(arr[1]);

      if (level != null) {
        if (arr.length > 2) {
          Endpoint.send(LogMessage.log(level, ArrayUtils.join(BeeConst.STRING_SPACE, arr, 2)));
        } else {
          Endpoint.send(LogMessage.level(level));
        }

      } else {
        Endpoint.send(new EchoMessage(args));
      }
    }
  }

  private static void getCharsets() {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.GET_RESOURCE);
    params.addPositionalHeader("cs");

    BeeKeeper.getRpc().makeGetRequest(params, ResponseHandler.callback("cs"));
  }

  private static void getClassInfo(String args, boolean errorPopup) {
    if (BeeUtils.isEmpty(args)) {
      showError(errorPopup, "Class name not specified");

    } else if (args.length() < 2) {
      showError(errorPopup, "Class name", args, "too short");

    } else {
      BeeKeeper.getRpc().sendText(new ParameterList(Service.GET_CLASS_INFO), args,
          ResponseHandler.callback(args));
    }
  }

  private static void getDbInfo(String args) {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.DB_INFO);
    if (!BeeUtils.isEmpty(args)) {
      params.addPositionalHeader(args.trim());
    }
    BeeKeeper.getRpc().makeRequest(params, ResponseHandler.callback(Service.DB_INFO));
  }

  private static void getFiles() {
    BeeKeeper.getRpc().makeGetRequest(Service.GET_FILES, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse()) {
          FileGroup fileGroup = new FileGroup(Lists.newArrayList(Column.ICON, Column.NAME,
              Column.SIZE, Column.TYPE, Column.DATE));
          fileGroup.render((String) response.getResponse());

          if (fileGroup.isEmpty()) {
            inform("files not available");
            return;
          }

          long totSize = 0;
          for (FileInfo sf : fileGroup.getFiles()) {
            totSize += sf.getSize();
          }

          fileGroup.setCaption("Files: " + fileGroup.getFiles().size() + " size: " + totSize);
          BeeKeeper.getScreen().show(fileGroup);
        }
      }
    });
  }

  private static void getFs() {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.GET_RESOURCE);
    params.addPositionalHeader("fs");

    BeeKeeper.getRpc().makeGetRequest(params, ResponseHandler.callback("fs"));
  }

  private static void getKeys(String[] arr, boolean errorPopup) {
    int parCnt = ArrayUtils.length(arr) - 1;
    if (parCnt <= 0) {
      showError(errorPopup, "getKeys", "table not specified");
      return;
    }

    ParameterList params = BeeKeeper.getRpc().createParameters(
        BeeUtils.same(arr[0], "pk") ? Service.DB_PRIMARY : Service.DB_KEYS);
    for (int i = 0; i < parCnt; i++) {
      params.addPositionalHeader(arr[i + 1]);
    }

    BeeKeeper.getRpc().makeGetRequest(params, ResponseHandler.callback(params.getService()));
  }

  private static void getResource(final String[] arr) {
    if (ArrayUtils.length(arr) < 2) {
      Global.sayHuh(ArrayUtils.join(BeeConst.STRING_SPACE, arr));
      return;
    }
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.GET_RESOURCE);
    for (String v : arr) {
      params.addPositionalData(v);
    }
    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(Resource.class)) {
          Resource resource = Resource.restore(response.getResponseAsString());
          ResourceEditor resourceEditor = new ResourceEditor(resource);

          BeeKeeper.getScreen().show(resourceEditor);

        } else {
          ResponseHandler.dispatch(ArrayUtils.joinWords(arr), response);
        }
      }
    });
  }

  private static void getSimpleRowSet(final String caption, ParameterList params) {
    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          Global.showError(caption, Arrays.asList(response.getErrors()));
        } else if (response.hasResponse(SimpleRowSet.class)) {
          showSimpleRowSet(caption, SimpleRowSet.restore(response.getResponseAsString()));
        } else if (response.hasWarnings()) {
          Global.showError(caption, Arrays.asList(response.getWarnings()));
        } else {
          Global.showError(caption, Arrays.asList("response type", response.getType()));
        }
      }
    });
  }

  private static void getTables(String input, String[] arr) {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.DB_TABLES);

    int len = ArrayUtils.length(arr);
    for (int i = 1; i < len; i++) {
      String s = arr[i];

      if (s.length() > 2 && s.charAt(1) == BeeConst.CHAR_EQ) {
        String value = s.substring(2);

        switch (s.toLowerCase().charAt(0)) {
          case 'c':
            params.addQueryItem(Service.VAR_CATALOG, value);
            break;
          case 's':
            params.addQueryItem(Service.VAR_SCHEMA, value);
            break;
          case 'n':
            params.addQueryItem(Service.VAR_TABLE, value);
            break;
          case 't':
            params.addQueryItem(Service.VAR_TYPE, value);
            break;
        }

      } else if (s.equals(BeeConst.STRING_MINUS) || s.equals(BeeConst.STRING_QUESTION)) {
        params.addQueryItem(Service.VAR_CHECK, 1);

      } else {
        params.addQueryItem(Service.VAR_TABLE, s);
      }
    }

    BeeKeeper.getRpc().makeRequest(params, ResponseHandler.callback(input));
  }

  private static void inform(String... messages) {
    List<String> lst = Arrays.asList(messages);
    Global.showInfo(lst);
  }

  private static void playAudio(String[] arr) {
    String src = ArrayUtils.getQuietly(arr, 1);
    if (BeeUtils.isEmpty(src)) {
      logger.warning("source not specified");
      return;
    }

    boolean autoplay = false;
    boolean controls = true;
    boolean load = false;
    boolean loop = false;
    boolean play = false;
    boolean muted = false;
    Double volume = null;

    int len = ArrayUtils.length(arr);
    for (int i = 2; i < len; i++) {
      String s = BeeUtils.toLowerCase(arr[i]);

      boolean on = !BeeUtils.isPrefixOrSuffix(s, BeeConst.CHAR_MINUS);
      if (!on) {
        s = BeeUtils.removePrefixAndSuffix(s, BeeConst.CHAR_MINUS);
      }

      switch (s) {
        case "a":
          autoplay = on;
          logger.debug("autoplay", on);
          break;

        case "c":
          controls = on;
          logger.debug("controls", on);
          break;

        case "l":
          load = on;
          logger.debug("load", on);
          break;

        case "m":
          muted = on;
          logger.debug("muted", on);
          break;

        case "o":
          loop = on;
          logger.debug("loop", on);
          break;

        case "p":
          play = on;
          logger.debug("play", on);
          break;

        default:
          if (BeeUtils.isDouble(s)) {
            volume = BeeUtils.toDouble(s);
            logger.debug("volume", volume);
          }
      }
    }

    Audio widget = new Audio();

    widget.getAudioElement().setAutoplay(autoplay);
    widget.getAudioElement().setControls(controls);
    widget.getAudioElement().setLoop(loop);
    widget.getAudioElement().setMuted(muted);

    if (volume != null) {
      widget.setVolume(volume);
    }

    widget.getAudioElement().setSrc(src);

    widget.addDomHandler(event -> BeeKeeper.getScreen().notifyWarning(src,
        EventUtils.transformMediaError(widget.getError())), ErrorEvent.getType());

    if (load) {
      widget.addAttachHandler(event -> widget.load());
    }
    if (play) {
      Timer timer = new Timer() {
        @Override
        public void run() {
          widget.play();
        }
      };
      timer.schedule(1_000);
    }

    BeeKeeper.getScreen().show(widget);
  }

  private static void playVideo(String args) {
    final String src = BeeUtils.notEmpty(args,
        "http://people.opera.com/shwetankd/webm/sunflower.webm");

    final Video widget = new Video();
    widget.getVideoElement().setSrc(src);
    widget.getVideoElement().setControls(true);

    widget.addDomHandler(event -> BeeKeeper.getScreen().notifyWarning(src,
        EventUtils.transformMediaError(widget.getError())), ErrorEvent.getType());

    BeeKeeper.getScreen().show(widget);
  }

  private static void print(String args, boolean errorPopup) {
    Widget widget = null;
    Element element = null;

    if (BeeUtils.same(args, "log")) {
      IdentifiableWidget lp = ClientLogManager.getLogPanel();
      if (lp == null) {
        showError(errorPopup, "log widget not available");
        return;
      } else {
        widget = lp.asWidget();
      }

    } else if (!BeeUtils.isEmpty(args)) {
      element = DomUtils.getElementQuietly(args);
      if (element == null) {
        showError(errorPopup, args, "element not found");
        return;
      }

    } else {
      IdentifiableWidget iw = BeeKeeper.getScreen().getActiveWidget();
      if (iw == null) {
        showError(errorPopup, "active widget not available");
        return;
      } else {
        widget = iw.asWidget();
      }
    }

    if (widget != null) {
      if (widget instanceof Printable) {
        Printer.print((Printable) widget);

      } else if (widget instanceof IdentifiableWidget) {
        final Element root = widget.getElement();
        final String id = root.getId();

        Printer.print(new Printable() {
          @Override
          public String getCaption() {
            return id;
          }

          @Override
          public Element getPrintElement() {
            return root;
          }

          @Override
          public boolean onPrint(Element source, Element target) {
            if (id.equals(source.getId())) {
              int width = Math.max(source.getScrollWidth(), DomUtils.getClientWidth() / 2);
              if (width > target.getClientWidth()) {
                StyleUtils.setWidth(target, width);
              }

              int height = source.getScrollHeight();
              if (height > target.getClientHeight()) {
                StyleUtils.setHeight(target, height);
              }
            }
            return true;
          }
        });

      } else {
        Printer.print(widget.getElement(), null);
      }

    } else if (element != null) {
      Printer.print(element, null);
    }
  }

  private static void querySelector(String command, String selectors, boolean errorPopup) {
    int p = command.indexOf('#');
    Element root = null;
    if (p > 0 && p < command.length() - 1) {
      String id = command.substring(p + 1);
      root = Document.get().getElementById(id);
      if (root == null) {
        showError(errorPopup, command, id, "element id not found");
        return;
      }
    }
    boolean all = command.indexOf('=') < 0;

    Element element = null;
    NodeList<Element> nodes = null;

    if (root == null) {
      if (all) {
        nodes = Selectors.getNodes(selectors);
      } else {
        element = Selectors.getElement(selectors);
      }
    } else {
      if (all) {
        nodes = Selectors.getNodes(root, selectors);
      } else {
        element = Selectors.getElement(root, selectors);
      }
    }

    if (element != null) {
      inform(DomUtils.transformElement(element));
      return;
    }

    int cnt = (nodes == null) ? 0 : nodes.getLength();
    if (cnt <= 0) {
      showError(errorPopup, command, selectors, "no elements found");
      return;
    }

    List<Property> info = PropertyUtils.createProperties(command, selectors, "Count", cnt);
    for (int i = 0; i < cnt; i++) {
      info.add(new Property(BeeUtils.progress(i + 1, cnt),
          DomUtils.transformElement(nodes.getItem(i))));
    }

    showTable("Selectors", new PropertiesData(info));
  }

  private static void rebuildSomething(final String args) {
    if (BeeUtils.same(args, "check")) {
      Endpoint.initProgress("rebuild", progress -> {
        ParameterList params = new ParameterList(Service.REBUILD);

        if (!BeeUtils.isEmpty(progress)) {
          params.addQueryItem(Service.VAR_PROGRESS, progress);
        }
        BeeKeeper.getRpc().sendText(params, args, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            Assert.notNull(response);

            if (progress != null) {
              Endpoint.removeProgress(progress);
              Endpoint.send(ProgressMessage.close(progress));
            }
            if (response.hasResponse()) {
              showPropData(BeeUtils.joinWords("Rebuild", args),
                  PropertyUtils.restoreProperties((String) response.getResponse()));
            }
          }
        });
      });

    } else {
      BeeKeeper.getRpc().sendText(new ParameterList(Service.REBUILD), args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          Assert.notNull(response);

          if (response.hasResponse()) {
            showPropData(BeeUtils.joinWords("Rebuild", args),
                PropertyUtils.restoreProperties((String) response.getResponse()));
          }
        }
      });
    }
  }

  private static void respectMyAuthoritah() {
    BeeKeeper.getRpc().makeGetRequest(Service.RESPECT_MY_AUTHORITAH, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(Boolean.class)) {
          boolean authoritah = BeeUtils.toBoolean(response.getResponseAsString());
          UserData userData = BeeKeeper.getUser().getUserData();

          if (userData != null && userData.hasAuthoritah() != authoritah) {
            BeeKeeper.getUser().getUserData().respectMyAuthoritah();
            BeeKeeper.onRightsChange();

            logger.debug(authoritah ? "respect my authoritah" : "screw you guys i'm going home");
          }
        }
      }
    });
  }

  private static void scheduleTasks(String[] arr, boolean errorPopup) {
    JustDate from = (arr.length > 1)
        ? TimeUtils.parseDate(arr[1], Format.getDefaultDateOrdering()) : TimeUtils.today();
    JustDate until = (arr.length > 2)
        ? TimeUtils.parseDate(arr[2], Format.getDefaultDateOrdering()) : null;

    if (from == null || until != null && TimeUtils.isLess(until, from)) {
      showError(errorPopup, arr);
    } else {
      DateRange range = (until == null) ? DateRange.day(from) : DateRange.closed(from, until);
      logger.debug("schedule tasks", range);

      TasksKeeper.scheduleTasks(range);
    }
  }

  private static void setDebug(String args) {
    if (BeeUtils.same(args, "ec")) {
      logger.debug(args, "debug", EcKeeper.toggleDebug());
    } else {
      if (BeeUtils.isEmpty(args)) {
        Global.setDebug(true);
      } else if (!"?".equals(args)) {
        Global.setDebug(BeeConst.isTrue(args));
      }
      logger.debug("debug", Global.isDebug());
    }
  }

  private static void setLoginBackground(String args) {
    final String key = "login_bg";

    if (BeeUtils.isEmpty(args)) {
      final Popup popup = new Popup(OutsideClick.CLOSE);

      final InputFile widget = new InputFile();
      widget.setAccept(Keywords.ACCEPT_IMAGE);

      widget.addChangeHandler(event -> {
        popup.close();

        List<NewFileInfo> fileInfos = FileUtils.getNewFileInfos(widget.getFiles());
        List<NewFileInfo> files = Images.sanitizeInput(fileInfos, BeeKeeper.getScreen());

        if (!BeeUtils.isEmpty(files)) {
          FileUtils.readAsDataURL(files.get(0).getNewFile(),
              e -> BeeKeeper.getStorage().set(key, e));
        }
      });

      popup.setWidget(widget);
      popup.center();

    } else if (BeeConst.STRING_MINUS.equals(args)) {
      BeeKeeper.getStorage().remove(key);
      logger.debug(key, "removed");

    } else {
      BeeKeeper.getStorage().set(key, args);
      logger.debug(key, BeeConst.STRING_EQ, args);
    }
  }

  private static void showBrowser(String[] arr) {
    boolean bro = false;
    boolean wnd = false;
    boolean loc = false;
    boolean nav = false;
    boolean scr = false;
    boolean typ = false;
    boolean prf = false;
    boolean doc = false;

    for (int i = 1; i < ArrayUtils.length(arr); i++) {
      switch (arr[i].toLowerCase().charAt(0)) {
        case 'b':
          bro = true;
          break;
        case 'w':
          wnd = true;
          break;
        case 'l':
          loc = true;
          break;
        case 'n':
          nav = true;
          break;
        case 's':
          scr = true;
          break;
        case 't':
          typ = true;
          break;
        case 'p':
          prf = true;
          break;
        case 'd':
          doc = true;
          break;
      }
    }

    if (!bro && !wnd && !loc && !nav && !scr && !typ && !prf && !doc) {
      bro = true;
      wnd = true;
      loc = true;
      nav = true;
      scr = true;
      typ = true;
      prf = true;
      doc = true;
    }

    List<ExtendedProperty> info = new ArrayList<>();

    if (bro) {
      PropertyUtils.appendChildrenToExtended(info, "Browser", BrowsingContext.getBrowserInfo());
    }
    if (wnd) {
      PropertyUtils.appendChildrenToExtended(info, "Window", BrowsingContext.getWindowInfo());
    }
    if (loc) {
      PropertyUtils.appendChildrenToExtended(info, "Location", BrowsingContext.getLocationInfo());
    }
    if (nav) {
      PropertyUtils.appendChildrenToExtended(info, "Navigator", BrowsingContext.getNavigatorInfo());
    }
    if (scr) {
      PropertyUtils.appendChildrenToExtended(info, "Screen", BrowsingContext.getScreenInfo());
    }
    if (typ) {
      PropertyUtils.appendChildrenToExtended(info, "Mime Types",
          BrowsingContext.getSupportedMimeTypes());
    }
    if (prf) {
      PropertyUtils.appendChildrenToExtended(info, "Performance",
          BrowsingContext.getPerformanceInfo());
    }
    if (doc) {
      PropertyUtils.appendChildrenToExtended(info, "Document", BrowsingContext.getDocumentInfo());
    }

    showExtData(ArrayUtils.joinWords(arr), info);
  }

  private static void showCaptions() {
    Set<String> keys = EnumUtils.getRegisteredKeys();
    if (BeeUtils.isEmpty(keys)) {
      logger.debug("no captions registered");
      return;
    }

    List<Property> props = PropertyUtils.createProperties("Caption Keys",
        BeeUtils.bracket(keys.size()));

    for (String key : keys) {
      for (String caption : EnumUtils.getCaptions(key)) {
        props.add(new Property(key, caption));
      }
    }

    showPropData("Captions", props);
  }

  private static void showChoice(String[] arr) {
    String caption = null;
    String prompt = null;
    List<String> options = new ArrayList<>();
    int defaultValue = BeeConst.UNDEF;
    int timeout = BeeConst.UNDEF;
    String cancelHtml = null;

    final Holder<String> widgetName = new Holder<>(null);
    final Holder<String> widgetStyle = Holder.of("background-color:green");

    String v;

    for (int i = 1; i < ArrayUtils.length(arr); i++) {
      if (arr[i].length() < 3 || arr[i].charAt(1) != '=') {
        options.add(arr[i]);
        continue;
      }

      char k = arr[i].toLowerCase().charAt(0);
      v = arr[i].substring(2).trim();
      if (BeeUtils.isEmpty(v)) {
        continue;
      }

      switch (k) {
        case 'c':
          caption = v;
          break;
        case 'p':
          prompt = v;
          break;
        case 'd':
          defaultValue = BeeUtils.toInt(v);
          break;
        case 't':
          timeout = BeeUtils.toInt(v);
          break;
        case 'n':
          cancelHtml = v;
          break;
        case 'i':
          widgetName.set(v);
          break;
        case 's':
          widgetStyle.set(v);
          break;
        default:
          options.add(arr[i]);
      }
    }

    if (options.isEmpty()) {
      for (int i = 0; i < BeeUtils.randomInt(1, 10); i++) {
        options.add(BeeUtils.randomString(3, 20, ' ', 'z'));
      }
    }

    MessageBoxes.choice(caption, prompt, options, new ChoiceCallback() {
      @Override
      public void onCancel() {
        logger.info("cancel");
      }

      @Override
      public void onSuccess(int value) {
        logger.info("success", value);
      }

      @Override
      public void onTimeout() {
        logger.info("timeout");
      }

    }, defaultValue, timeout, cancelHtml, (widget, name) -> {
      if (BeeUtils.containsSame(name, widgetName.get())) {
        StyleUtils.updateAppearance(widget, null, widgetStyle.get());
        logger.info(name, StyleUtils.getCssText(widget));
      }
      return widget;
    });
  }

  private static void showColor(String[] arr) {
    Map<String, String> output = new HashMap<>();

    if (arr.length > 1) {
      for (int i = 1; i < arr.length; i++) {
        String normalized = Color.normalize(arr[i]);

        if (BeeUtils.isEmpty(normalized)) {
          boolean found = false;

          for (Map.Entry<String, String> entry : Color.getNames().entrySet()) {
            if (Wildcards.isLike(entry.getKey(), arr[i])
                || Wildcards.isLike(entry.getValue(), arr[i])) {
              output.put(entry.getKey(), entry.getValue());
              found = true;
            }
          }

          if (!found) {
            logger.warning("color not recognized", arr[i]);
          }

        } else {
          output.put(arr[i], normalized);
        }
      }

    } else {
      output.putAll(Color.getNames());
    }

    if (output.isEmpty()) {
      return;
    }

    List<String> keys = new ArrayList<>(output.keySet());
    Collections.sort(keys);

    HtmlTable table = new HtmlTable();

    for (int row = 0; row < keys.size(); row++) {
      String key = keys.get(row);
      String value = output.get(key);

      int col = 0;
      table.getCellFormatter().setHorizontalAlignment(row, col, TextAlign.RIGHT);
      table.setHtml(row, col, BeeUtils.toString(row + 1));
      col++;

      table.setHtml(row, col, key);
      col++;

      CustomDiv keySwatch = new CustomDiv();
      StyleUtils.setSize(keySwatch, 60, 20);
      StyleUtils.setBackgroundColor(keySwatch, key);
      table.setWidget(row, col, keySwatch);
      col++;

      CustomDiv valueSwatch = new CustomDiv();
      StyleUtils.setSize(valueSwatch, 60, 20);
      StyleUtils.setBackgroundColor(valueSwatch, value);
      table.setWidget(row, col, valueSwatch);
      col++;

      table.setHtml(row, col, value);
    }

    BeeKeeper.getScreen().show(table);
  }

  private static void addRateTypeAndCurrency(ParameterList params, String args) {
    String type = null;
    String currency = null;

    for (String arg : NameUtils.toList(args)) {
      if (type == null && BeeUtils.inListSame(arg, "lt", "eu")) {
        type = arg.toUpperCase();
      } else {
        currency = arg.toUpperCase();
      }
    }

    if (!BeeUtils.isEmpty(type)) {
      params.addQueryItem(Service.VAR_TYPE, type);
    }
    if (!BeeUtils.isEmpty(currency)) {
      params.addQueryItem(COL_CURRENCY_NAME, currency);
    }
  }

  private static void showCurrentExchangeRate(String args) {
    ParameterList params = AdministrationKeeper.createArgs(SVC_GET_CURRENT_EXCHANGE_RATE);
    addRateTypeAndCurrency(params, args);

    getSimpleRowSet(BeeUtils.joinWords("Current rate", args), params);
  }

  private static void showDataInfo(String viewName, boolean errorPopup) {
    if (BeeUtils.inListSame(viewName, "load", "refresh", "+", "x")) {
      Data.getDataInfoProvider().load();

    } else if (BeeUtils.inListSame(viewName, "size", "s", "c", "r", "rc")) {
      Map<String, Integer> sizes = new TreeMap<>(Data.getApproximateSizes());

      if (sizes.isEmpty()) {
        logger.debug("data sizes are empty");
      } else {
        showPropData("approximate sizes", PropertyUtils.createProperties(sizes));
      }

    } else {
      if (!BeeUtils.isEmpty(viewName)) {
        DataInfo dataInfo = Data.getDataInfo(viewName, false);
        if (dataInfo != null) {
          showExtData(viewName, dataInfo.getExtendedInfo());
          return;
        }
      }

      List<DataInfo> list = new ArrayList<>();
      if (BeeUtils.isEmpty(viewName)) {
        list.addAll(Data.getDataInfoProvider().getViews());
      } else {
        for (DataInfo dataInfo : Data.getDataInfoProvider().getViews()) {
          if (BeeUtils.containsSame(dataInfo.getViewName(), viewName)
              || BeeUtils.containsSame(dataInfo.getTableName(), viewName)) {
            list.add(dataInfo);
          }
        }
      }

      if (list.isEmpty()) {
        showError(errorPopup, viewName, "no data infos available");
        return;
      }

      Collections.sort(list);
      String[][] data = new String[list.size()][6];

      for (int i = 0; i < list.size(); i++) {
        DataInfo di = list.get(i);

        data[i][0] = di.getViewName();
        data[i][1] = di.getTableName();
        data[i][2] = di.getIdColumn();
        data[i][3] = di.getVersionColumn();

        data[i][4] = BeeUtils.toString(di.getColumnCount());
        data[i][5] = BeeUtils.toString(di.getViewColumns().size());
      }
      showMatrix("Data info", data, "view", "table", "id", "version", "cc", "vc");
    }
  }

  private static void showDate(String cmnd, String args, boolean errorPopup) {
    DateTimeFormat dtf = null;
    String inp = null;

    char sep = ';';
    if (BeeUtils.contains(args, sep)) {
      dtf = Format.parseDateTimeFormat(BeeUtils.getPrefix(args, sep));
      inp = BeeUtils.getSuffix(args, sep);
    } else if (BeeUtils.contains(cmnd, 'f') && !BeeUtils.isEmpty(args)) {
      dtf = Format.getPredefinedFormat(PredefinedFormat.DATE_TIME_SHORT);
      inp = args;
    }

    DateTime t = null;
    JustDate d = null;

    if (dtf != null) {
      try {
        if (BeeUtils.contains(cmnd, 's')) {
          t = dtf.parseStrict(inp);
        } else {
          t = dtf.parse(inp);
        }
      } catch (IllegalArgumentException ex) {
        showError(errorPopup, args, dtf.getPattern(), inp, ex.toString());
      }

    } else if (!BeeUtils.isEmpty(args)) {
      t = TimeUtils.parseDateTime(args, Format.getDefaultDateOrdering());
      if (t == null) {
        logger.severe("cannot parse", args);
      } else {
        d = TimeUtils.parseDate(args, Format.getDefaultDateOrdering());
      }

    } else {
      t = new DateTime();
      d = new JustDate();
    }

    if (t == null) {
      return;
    }
    if (d == null) {
      d = new JustDate(t);
    }

    List<Property> lst = new ArrayList<>();
    if (dtf != null) {
      lst.add(new Property("Format", dtf.getPattern()));
    }
    if (!BeeUtils.isEmpty(inp)) {
      lst.add(new Property("Input", inp));
    }

    PropertyUtils.addProperties(lst,
        "Day", d.getDays(),
        "Year", d.getYear(),
        "Month", d.getMonth(),
        "Dom", d.getDom(),
        "Dow", d.getDow(),
        "Doy", d.getDoy(),
        "String", d.toString(),
        "DateTime", TimeUtils.toDateTime(d).toString(),
        "Java Date", TimeUtils.toJava(d).toString(),
        "Time", t.getTime(),
        "Year", t.getYear(),
        "Month", t.getMonth(),
        "Dom", t.getDom(),
        "Dow", t.getDow(),
        "Doy", t.getDoy(),
        "Hour", t.getHour(),
        "Minute", t.getMinute(),
        "Second", t.getSecond(),
        "Millis", t.getMillis(),
        "Time String", t.toTimeString(),
        "String", t.toString(),
        "Timezone Offset", t.getTimezoneOffset(),
        "Utc Year", t.getUtcYear(),
        "Utc Month", t.getUtcMonth(),
        "Utc Dom", t.getUtcDom(),
        "Utc Dow", t.getUtcDow(),
        "Utc Doy", t.getUtcDoy(),
        "Utc Hour", t.getUtcHour(),
        "Utc Minute", t.getUtcMinute(),
        "Utc Second", t.getUtcSecond(),
        "Utc Millis", t.getUtcMillis(),
        "Utc Date String", t.toUtcDateString(),
        "Utc Time String", t.toUtcTimeString(),
        "Utc String", t.toUtcString(),
        "JustDate", Format.renderDate(TimeUtils.toDate(t)),
        "Java Date", TimeUtils.toJava(t).toString());

    showPropData(BeeUtils.joinWords(cmnd, args), lst);
  }

  private static void showDateFormat(String args) {
    if (BeeUtils.isEmpty(args)) {
      int r = PredefinedFormat.values().length;
      int c = SupportedLocale.values().length * 2 + 1;

      String[][] data = new String[r][c];

      DateTime d = new DateTime();
      int i = 0;
      int j;

      for (PredefinedFormat pf : PredefinedFormat.values()) {
        j = 0;
        data[i][j++] = pf.name();

        for (SupportedLocale locale : SupportedLocale.values()) {
          DateTimeFormat dtf = DateTimeFormat.of(pf, locale.getDateTimeFormatInfo());
          data[i][j++] = dtf.getPattern();
          data[i][j++] = dtf.format(d);
        }

        i++;
      }

      String[] labels = new String[c];
      j = 0;
      labels[j++] = "Format";

      for (SupportedLocale locale : SupportedLocale.values()) {
        labels[j++] = "Pattern " + locale.getLanguage();
        labels[j++] = "Value " + locale.getLanguage();
      }

      showMatrix("DateTimeFormats", data, labels);

    } else {
      DateTimeFormat dtf;
      DateTime t;

      char sep = ';';
      if (BeeUtils.contains(args, sep)) {
        dtf = Format.parseDateTimeFormat(BeeUtils.getPrefix(args, sep));
        t = TimeUtils.parseDateTime(BeeUtils.getSuffix(args, sep), Format.getDefaultDateOrdering());
      } else {
        dtf = Format.parseDateTimeFormat(args);
        t = new DateTime();
      }

      if (dtf == null || t == null) {
        logger.severe("cannot parse", args);
      } else {
        String result = dtf.format(t);
        logger.debug("format", dtf.getPattern());
        logger.debug("input", t);
        logger.debug("result", result);
        logger.addSeparator();
      }
    }
  }

  private static void showDimensions(String id) {
    final String caption;
    final List<Property> info;

    if (BeeUtils.isEmpty(id)) {
      caption = "Dimensions";
      info = PropertyUtils.createProperties(
          "Viewport width", DomUtils.getClientWidth(),
          "Viewport height", DomUtils.getClientHeight(),
          "TextBox client width", DomUtils.getTextBoxClientWidth(),
          "TextBox client height", DomUtils.getTextBoxClientHeight(),
          "TextBox offset width", DomUtils.getTextBoxOffsetWidth(),
          "TextBox offset height", DomUtils.getTextBoxOffsetHeight(),
          "CheckBox client width", DomUtils.getCheckBoxClientWidth(),
          "CheckBox client height", DomUtils.getCheckBoxClientHeight(),
          "CheckBox offset width", DomUtils.getCheckBoxOffsetWidth(),
          "CheckBox offset height", DomUtils.getCheckBoxOffsetHeight(),
          "Scrollbar width", DomUtils.getScrollBarWidth(),
          "Scrollbar height", DomUtils.getScrollBarHeight());

    } else {
      Element elem = Document.get().getElementById(id);
      if (elem == null) {
        inform("element id", id, "not found");
        return;
      }

      caption = id;
      info = PropertyUtils.createProperties(
          "Client width", elem.getClientWidth(),
          "Client height", elem.getClientHeight(),
          "Offset width", elem.getOffsetWidth(),
          "Offset height", elem.getOffsetHeight(),
          "Scroll width", elem.getScrollWidth(),
          "Scroll height", elem.getScrollHeight(),
          "Outer width", DomUtils.getOuterWidth(elem),
          "Outer height", DomUtils.getOuterHeight(elem));
    }

    showTable(caption, new PropertiesData(info));
  }

  private static void showElement(String v, String[] arr, boolean errorPopup) {
    if (ArrayUtils.length(arr) < 2) {
      Global.sayHuh(v);
      return;
    }

    Element el = Document.get().getElementById(arr[1]);
    if (el == null) {
      showError(errorPopup, arr[1], "element id not found");
      return;
    }

    if (v.startsWith("id")) {
      String patt = ArrayUtils.getQuietly(arr, 2);
      JsArrayString prp = JsUtils.getProperties(el, patt);

      if (JsUtils.isEmpty(prp)) {
        showError(errorPopup, v, "properties not found");
        return;
      }

      JsData<?> table = new JsData<TableColumn>(prp, "property", "type", "value");

      sortTable(table, 0);
      showTable(v, table);

    } else {
      showPropData(v, DomUtils.getElementInfo(el));
    }
  }

  private static void showError(boolean popup, String... messages) {
    if (popup) {
      Global.showError(null, Arrays.asList(messages), null, "kthxbai");
    } else {
      logger.severe(ArrayUtils.joinWords(messages));
    }
  }

  private static void showExchangeRate(String currency, String date) {
    ParameterList params = AdministrationKeeper.createArgs(SVC_GET_EXCHANGE_RATE);

    addRateTypeAndCurrency(params, currency);
    if (!BeeUtils.isEmpty(date)) {
      params.addQueryItem(COL_CURRENCY_RATE_DATE, date);
    }

    getSimpleRowSet(BeeUtils.joinWords("Exchange rate", currency, date), params);
  }

  private static void showExchangeRates(String currency, String dateLow, String dateHigh) {
    ParameterList params = AdministrationKeeper.createArgs(SVC_GET_EXCHANGE_RATES_FOR_CURRENCY);

    addRateTypeAndCurrency(params, currency);

    if (!BeeUtils.isEmpty(dateLow)) {
      params.addQueryItem(VAR_DATE_LOW, dateLow);
    }
    if (!BeeUtils.isEmpty(dateHigh)) {
      params.addQueryItem(VAR_DATE_HIGH, dateHigh);
    }

    getSimpleRowSet(BeeUtils.joinWords("Rates for currency", currency, dateLow, dateHigh), params);
  }

  private static void showExtData(String caption, List<ExtendedProperty> data) {
    Global.showTable(caption, new ExtendedPropertiesData(data, true));
  }

  private static void showFlags(String[] arr) {
    final HtmlTable table = new HtmlTable();
    table.setBorderSpacing(10);

    if (ArrayUtils.length(arr) > 1) {
      final int cnt = arr.length - 1;
      final Holder<Integer> counter = Holder.of(0);

      for (int i = 1; i <= cnt; i++) {
        Flags.get(arr[i], new Callback<String>() {
          @Override
          public void onFailure(String... reason) {
            count();
          }

          @Override
          public void onSuccess(String uri) {
            table.setWidget(0, counter.get(), new Image(uri));
            count();
          }

          private void count() {
            counter.set(counter.get() + 1);
            if (counter.get() == cnt && !table.isEmpty()) {
              Global.showModalWidget(table);
            }
          }
        });
      }

    } else {
      Callback<Integer> callback = result -> {
        int row = 0;
        int col = 0;

        Map<String, String> flags = new TreeMap<>(Flags.getFlags());

        for (Map.Entry<String, String> entry : flags.entrySet()) {
          table.setHtml(row, col, entry.getKey());
          table.getCellFormatter().setHorizontalAlignment(row, col, TextAlign.RIGHT);
          col++;

          ImageElement imageElement = Document.get().createImageElement();
          imageElement.setSrc(entry.getValue());
          CustomWidget widget = new CustomWidget(imageElement);

          table.setWidget(row, col, widget);
          col++;
          if (col > 20) {
            row++;
            col = 0;
          }
        }

        if (!table.isEmpty()) {
          BeeKeeper.getScreen().show(table);
        }
      };

      if (Flags.isEmpty()) {
        Flags.load(callback);
      } else {
        callback.onSuccess(null);
      }
    }
  }

  private static void showFont(String id, boolean errorPopup) {
    Element el = Document.get().getElementById(id);
    if (el == null) {
      showError(errorPopup, id, "element not found");
      return;
    }

    showTable(id, new PropertiesData(Font.getComputed(el).getInfo()));
  }

  private static void showFontAwesome(String names, String args, boolean errorPopup) {
    Flow panel = new Flow();
    StyleUtils.setFontFamily(panel, FontAwesome.FAMILY);
    StyleUtils.setFontSize(panel, FontSize.MEDIUM);

    Set<String> context = new HashSet<>();
    if (!BeeUtils.isEmpty(names)) {
      for (String s : NameUtils.NAME_SPLITTER.split(names)) {
        context.add(s);
      }
    }

    Range<Character> range = null;

    List<Property> styles = PropertyUtils.createProperties(
        CssProperties.DISPLAY, Display.INLINE_BLOCK.getCssName(),
        CssProperties.PADDING, CssUnit.format(5, CssUnit.PX));

    if (!BeeUtils.isEmpty(args)) {
      String st;

      if (args.contains("4.1")) {
        range = Range.closed(FontAwesome.SPACE_SHUTTLE.getCode(), FontAwesome.BOMB.getCode());
        st = args.replace("4.1", "");

      } else if (args.contains("4.2")) {
        range = Range.closed(FontAwesome.SOCCER_BALL_O.getCode(), FontAwesome.MEANPATH.getCode());
        st = args.replace("4.2", "");

      } else if (args.contains("4.3")) {
        range = Range.closed(FontAwesome.BUYSELLADS.getCode(), FontAwesome.MEDIUM.getCode());
        st = args.replace("4.3", "");

      } else if (args.contains("4.4")) {
        range = Range.closed(FontAwesome.YC.getCode(), FontAwesome.FONTICONS.getCode());
        st = args.replace("4.4", "");

      } else if (args.contains("4.5")) {
        range = Range.closed(FontAwesome.REDDIT_ALIEN.getCode(), FontAwesome.PERCENT.getCode());
        st = args.replace("4.5", "");

      } else if (args.contains("4.7")) {
        range = Range.closed(FontAwesome.GITLAB.getCode(), FontAwesome.MEETUP.getCode());
        st = args.replace("4.7", "");

      } else {
        st = args;
      }

      if (!BeeUtils.isEmpty(st)) {
        styles.addAll(StyleUtils.parseStyles(st.trim()));
      }
    }

    int count = 0;
    for (FontAwesome fa : FontAwesome.values()) {
      if (range != null && !range.contains(fa.getCode())) {
        continue;
      }

      if (!context.isEmpty()) {
        boolean ok = false;
        for (String s : context) {
          if (BeeUtils.containsSame(fa.name(), s)) {
            ok = true;
            break;
          }
        }

        if (!ok) {
          continue;
        }
      }

      FaLabel label = new FaLabel(fa, true);
      label.setTitle(BeeUtils.joinWords(fa.name().toLowerCase(),
          Integer.toHexString(fa.getCode())));

      StyleUtils.updateStyle(label, styles);
      panel.add(label);

      count++;
    }

    if (count > 0) {
      logger.debug(FontAwesome.FAMILY, names, count);
      BeeKeeper.getScreen().show(panel);
    } else {
      showError(errorPopup, FontAwesome.FAMILY, names, "not found");
    }
  }

  private static void showFunctions(String v, String[] arr, boolean errorPopup) {
    if (ArrayUtils.length(arr) < 2) {
      Global.sayHuh(v);
      return;
    }

    JavaScriptObject obj;
    if (arr[1].startsWith("#")) {
      obj = Document.get().getElementById(arr[1].substring(1));
    } else {
      obj = JsUtils.eval(arr[1]);
    }
    if (obj == null) {
      showError(errorPopup, arr[1], "not a js object");
      return;
    }

    String patt = ArrayUtils.getQuietly(arr, 2);
    JsArrayString fnc = JsUtils.getFunctions(obj, patt);

    if (JsUtils.isEmpty(fnc)) {
      showError(errorPopup, v, "functions not found");
      return;
    }
    if (fnc.length() <= 5) {
      inform(v, fnc.join());
      return;
    }

    JsData<?> table = new JsData<TableColumn>(fnc, "function");

    sortTable(table, 0);
    showTable(v, table);
  }

  private static void showGeo() {
    Consumer<Position> onPosition = input -> {
      Coordinates coords = input.getCoordinates();

      List<Property> info = PropertyUtils.createProperties(
          "Latitude", coords.getLatitude(),
          "Longitude", coords.getLongitude(),
          "Accuracy", coords.getAccuracy(),
          "Altitude", coords.getAltitude(),
          "Accuracy", coords.getAltitudeAccuracy(),
          "Speed", coords.getSpeed(),
          "Heading", coords.getHeading(),
          "Timestamp", input.getTimestamp());

      showPropData("Position", info);
    };

    MapUtils.getCurrentPosition(onPosition);
  }

  private static void showGwt() {
    List<Property> info = PropertyUtils.createProperties(
        "Host Page Base URL", GWT.getHostPageBaseURL(),
        "Module Base URL", GWT.getModuleBaseURL(),
        "Module Base For Static Files", GWT.getModuleBaseForStaticFiles(),
        "Module Name", GWT.getModuleName(),
        "Permutation Strong Name", GWT.getPermutationStrongName(),
        "Uncaught Exception Handler", GWT.getUncaughtExceptionHandler(),
        "Unique Thread Id", GWT.getUniqueThreadId(),
        "Version", GWT.getVersion(),
        "Is Client", GWT.isClient(),
        "Is Prod Mode", GWT.isProdMode(),
        "Is Script", GWT.isScript());

    showTable("GWT", new PropertiesData(info));
  }

  private static void showImages(String[] arr) {
    final HtmlTable table = new HtmlTable();
    table.setBorderSpacing(10);

    if (ArrayUtils.length(arr) > 1) {
      for (int i = 1; i < arr.length; i++) {
        ImageResource resource = Images.getMap().get(arr[i].toLowerCase());
        if (resource == null) {
          logger.warning("image", arr[i], "not found");
        } else {
          table.setWidget(0, i - 1, new Image(resource));
        }
      }

      if (!table.isEmpty()) {
        Global.showModalWidget(table);
      }

    } else {
      int row = 0;
      int col = 0;

      Map<String, ImageResource> map = new TreeMap<>(Images.getMap());

      for (Map.Entry<String, ImageResource> entry : map.entrySet()) {
        table.setHtml(row, col, entry.getKey());
        table.getCellFormatter().setHorizontalAlignment(row, col, TextAlign.RIGHT);
        col++;

        table.setWidget(row, col, new Image(entry.getValue()));
        col++;
        if (col > 10) {
          row++;
          col = 0;
        }
      }

      if (!table.isEmpty()) {
        BeeKeeper.getScreen().show(table);
      }
    }
  }

  private static void showInputBox(String[] arr) {
    String caption = null;
    String prompt = null;
    String defaultValue = null;
    int maxLength = BeeConst.UNDEF;
    double width = BeeConst.DOUBLE_UNDEF;
    CssUnit widthUnit = null;
    int timeout = BeeConst.UNDEF;
    String confirmHtml = Localized.dictionary().ok();
    String cancelHtml = Localized.dictionary().cancel();

    boolean required = true;

    final Holder<String> widgetName = new Holder<>(null);
    final Holder<String> widgetStyle = Holder.of("background-color:green");

    String v;

    for (int i = 1; i < ArrayUtils.length(arr); i++) {
      if (arr[i].length() < 2) {
        continue;
      }
      char k = arr[i].toLowerCase().charAt(0);
      if (arr[i].charAt(1) == '=') {
        v = arr[i].substring(2).trim();
      } else {
        v = arr[i].substring(1).trim();
      }
      if (BeeUtils.isEmpty(v)) {
        continue;
      }

      switch (k) {
        case 'c':
          caption = v;
          break;
        case 'p':
          prompt = v;
          break;
        case 'd':
          defaultValue = v;
          break;
        case 'm':
          maxLength = BeeUtils.toInt(v);
          break;
        case 'w':
          width = BeeUtils.toDouble(v);
          break;
        case 'u':
          widthUnit = CssUnit.parse(v);
          break;
        case 't':
          timeout = BeeUtils.toInt(v);
          break;
        case 'y':
          confirmHtml = BeeConst.isFalse(v) ? null : v;
          break;
        case 'n':
          cancelHtml = BeeConst.isFalse(v) ? null : v;
          break;
        case 'r':
          required = BeeUtils.toBoolean(v);
          break;
        case 'i':
          widgetName.set(v);
          break;
        case 's':
          widgetStyle.set(v);
          break;
        default:
          logger.info("option not recognized", i, arr[i], k, v);
      }
    }

    Global.inputString(caption, prompt,
        new StringCallback(required) {
          @Override
          public void onCancel() {
            logger.info("cancel");
          }

          @Override
          public void onSuccess(String value) {
            logger.info("success", value);
          }

          @Override
          public void onTimeout(String value) {
            logger.info("timeout", value);
          }

          @Override
          public boolean validate(String value) {
            if (BeeUtils.isEmpty(value)) {
              return super.validate(value);
            } else {
              return value.indexOf('x') < 0;
            }
          }
        }, null, defaultValue, maxLength, null, width, widthUnit, timeout, confirmHtml, cancelHtml,
        (widget, name) -> {
          if (BeeUtils.containsSame(name, widgetName.get())) {
            StyleUtils.updateAppearance(widget, null, widgetStyle.get());
            logger.info(name, StyleUtils.getCssText(widget));
          }
          return widget;
        });
  }

  private static void showInputTypes() {
    HtmlTable table = new HtmlTable();
    table.setBorderSpacing(3);

    CustomWidget widget;

    int row = 0;
    for (Input.Type type : Input.Type.values()) {
      table.setWidget(row, 0, new Label(type.getKeyword()));

      if (Features.supportsInputType(type.getKeyword())) {
        widget = new CustomWidget(DomUtils.createElement(Tags.INPUT));
        DomUtils.setInputType(widget, type);

        if (Input.Type.SEARCH.equals(type)) {
          if (Features.supportsAttributePlaceholder()) {
            widget.getElement().setAttribute(Attributes.PLACEHOLDER, "Search...");
          }

        } else if (Input.Type.NUMBER.equals(type)) {
          widget.getElement().setAttribute(Attributes.MIN, "0");
          widget.getElement().setAttribute(Attributes.MAX, "20");
          widget.getElement().setAttribute(Attributes.STEP, "2");
          widget.getElement().setAttribute(Attributes.VALUE, "4");

        } else if (Input.Type.RANGE.equals(type)) {
          widget.getElement().setAttribute(Attributes.MIN, "0");
          widget.getElement().setAttribute(Attributes.MAX, "50");
          widget.getElement().setAttribute(Attributes.STEP, "5");
          widget.getElement().setAttribute(Attributes.VALUE, "30");
        }

        table.setWidget(row, 1, widget);
      } else {
        table.setWidget(row, 1, new Label("not supported"));
      }
      row++;
    }

    BeeKeeper.getScreen().show(table);
  }

  private static void showListOfCurrencies() {
    getSimpleRowSet(SVC_GET_LIST_OF_CURRENCIES,
        AdministrationKeeper.createArgs(SVC_GET_LIST_OF_CURRENCIES));
  }

  private static void showMap(final String[] arr, boolean errorPopup) {
    final Double latitude = BeeUtils.toDoubleOrNull(arr[1]);
    final Double longitude = BeeUtils.toDoubleOrNull(arr[2]);

    if (latitude == null || longitude == null) {
      showError(errorPopup, "invalid lat lng", ArrayUtils.joinWords(arr));
      return;
    }

    final int zoom;
    final int pos;

    if (arr.length > 3 && BeeUtils.isInt(arr[3])) {
      zoom = BeeUtils.toInt(arr[3]);
      pos = 4;
    } else {
      zoom = BeeConst.UNDEF;
      pos = 3;
    }

    final String caption = ArrayUtils.joinWords(arr);

    ApiLoader.ensureApi(() -> {
      LatLng latLng = LatLng.create(latitude, longitude);
      MapOptions mapOptions = (zoom >= 0) ? MapOptions.create(latLng, zoom)
          : MapOptions.create(latLng);

      final MapWidget widget = MapWidget.create(mapOptions);

      if (widget != null) {
        if (pos < arr.length - 2) {
          widget.addAttachHandler(event -> {
            if (event.isAttached() && widget.getMapImpl() != null) {
              for (int i = pos; i < arr.length - 2; i += 3) {
                Double lat = BeeUtils.toDoubleOrNull(arr[i + 1]);
                Double lng = BeeUtils.toDoubleOrNull(arr[i + 2]);

                if (lat != null && lng != null) {
                  widget.addMarker(lat, lng, BeeUtils.joinWords(lat, lng, arr[i]));
                }
              }
            }
          });
        }

        MapContainer container = new MapContainer(caption, widget);
        BeeKeeper.getScreen().show(container);
      }
    });
  }

  @SuppressWarnings("rawtypes")
  private static void showMatrix(String caption, String[][] data, String... columnLabels) {
    Global.showTable(caption, new StringMatrix(data, columnLabels));
  }

  private static void showMeter(String[] arr, boolean errorPopup) {
    if (!Features.supportsElementMeter()) {
      showError(errorPopup, "meter element not supported");
      return;
    }

    double min = 0;
    double max = 100;
    double value = 30;
    double low = 20;
    double high = 80;
    double optimum = 50;

    String pars = "<>vlho";
    int p = -1;
    int j;
    String s;
    String z;
    char c;

    for (int i = 1; i < arr.length; i++) {
      s = arr[i].toLowerCase();
      c = s.charAt(0);
      j = pars.indexOf(c);

      if (j >= 0) {
        z = s.substring(1);
        p = j;
      } else {
        z = s;
        p++;
      }

      switch (p) {
        case 0:
          min = BeeUtils.toDouble(z);
          break;
        case 1:
          max = BeeUtils.toDouble(z);
          break;
        case 2:
          value = BeeUtils.toDouble(z);
          break;
        case 3:
          low = BeeUtils.toDouble(z);
          break;
        case 4:
          high = BeeUtils.toDouble(z);
          break;
        case 5:
          optimum = BeeUtils.toDouble(z);
          break;
      }
    }

    if (max <= min) {
      max = min + 100;
    }
    value = BeeUtils.clamp(value, min, max);
    low = BeeUtils.clamp(low, min, max);
    high = BeeUtils.clamp(high, low, max);
    optimum = BeeUtils.clamp(optimum, min, max);

    HtmlTable table = new HtmlTable();
    table.setBorderSpacing(3);
    table.setDefaultCellStyles("padding: 3px; border: 1px solid black;");

    int r = 0;
    table.setHtml(r, 0, "min");
    table.setHtml(r, 1, BeeUtils.toString(min));
    r++;
    table.setHtml(r, 0, "max");
    table.setHtml(r, 1, BeeUtils.toString(max));
    r++;
    table.setHtml(r, 0, "low");
    table.setHtml(r, 1, BeeUtils.toString(low));
    r++;
    table.setHtml(r, 0, "high");
    table.setHtml(r, 1, BeeUtils.toString(high));
    r++;
    table.setHtml(r, 0, "optimum");
    table.setHtml(r, 1, BeeUtils.toString(optimum));

    r++;
    table.setHtml(r, 0, BeeUtils.toString(value));
    table.setWidget(r, 1, new Meter(min, max, value, low, high, optimum));

    for (double i = min; i <= max; i += (max - min) / 10) {
      r++;
      table.setHtml(r, 0, BeeUtils.toString(i));
      table.setWidget(r, 1, new Meter(min, max, i, low, high, optimum));
    }
    BeeKeeper.getScreen().show(table);
  }

  private static boolean showModal(int rowCount) {
    if (rowCount <= 1) {
      return true;
    }
    if (rowCount > 50) {
      return false;
    }

    int h = rowCount * 20 + 70;
    int vph = DomUtils.getClientHeight();
    int aph = BeeKeeper.getScreen().getActivePanelHeight();

    if (vph > 0 && h > vph / 2) {
      return false;
    }
    if (aph > 0 && aph < 80) {
      return true;
    }
    return rowCount <= 12;
  }

  private static void showNotes(String args) {
    if (BeeUtils.isEmpty(args)) {
      Global.sayHuh(args);
      return;
    }

    for (String msg : Splitter.on(';').omitEmptyStrings().trimResults().split(args)) {
      List<String> lst = new ArrayList<>();
      for (String line : Splitter.on(',').trimResults().split(msg)) {
        lst.add(line);
      }
      int c = lst.size();
      if (c <= 0) {
        continue;
      }

      String lvl = lst.get(0);
      if (c > 1 && BeeUtils.inListSame(lvl, "w", "e", "s")) {
        String[] arr = ArrayUtils.toArray(lst.subList(1, c));
        if (BeeUtils.same(lvl, "w")) {
          BeeKeeper.getScreen().notifyWarning(arr);
        } else {
          BeeKeeper.getScreen().notifySevere(arr);
        }

      } else {
        BeeKeeper.getScreen().notifyInfo(ArrayUtils.toArray(lst));
      }
    }
  }

  private static void showProgress(String[] arr, boolean errorPopup) {
    if (!Features.supportsElementProgress()) {
      showError(errorPopup, "progress element not supported");
      return;
    }

    final int count;
    int maxDuration;

    String s = ArrayUtils.getQuietly(arr, 1);
    if (BeeUtils.isPositiveInt(s)) {
      count = BeeUtils.toInt(s);
    } else {
      count = 20;
    }

    s = ArrayUtils.getQuietly(arr, 2);
    if (BeeUtils.isPositiveInt(s)) {
      maxDuration = BeeUtils.toInt(s);
      if (maxDuration < 100) {
        maxDuration *= TimeUtils.MILLIS_PER_SECOND;
      }
    } else {
      maxDuration = TimeUtils.MILLIS_PER_MINUTE;
    }

    final class Prog {
      final double max;
      final long start;
      final long finish;

      String id;

      private Prog(double max, long start, long finish) {
        super();
        this.max = max;
        this.start = start;
        this.finish = finish;
      }
    }

    final long now = System.currentTimeMillis();

    final List<Prog> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      double max = BeeUtils.randomDouble(100, 10000);
      long start = now + BeeUtils.randomInt(0, maxDuration / 2);
      long finish = start + BeeUtils.randomInt(maxDuration / 5, maxDuration);

      list.add(new Prog(max, start, finish));
    }

    Timer timer = new Timer() {
      private int closed;

      @Override
      public void run() {
        long time = System.currentTimeMillis();

        for (final Prog prog : list) {
          if (prog.id == null) {
            if (prog.start < time && prog.finish > time) {
              String caption =
                  BeeUtils.toString(prog.start - now) + "-" + BeeUtils.toString(prog.finish - now);

              InlineLabel close = new InlineLabel(String.valueOf(BeeConst.CHAR_TIMES));
              close.addClickHandler(event -> BeeKeeper.getScreen().removeProgress(prog.id));

              Thermometer thermometer = new Thermometer(caption, prog.max, close);
              prog.id = BeeKeeper.getScreen().addProgress(thermometer);
            }

          } else if (prog.finish > time) {
            double value = prog.max * (time - prog.start) / (prog.finish - prog.start);
            BeeKeeper.getScreen().updateProgress(prog.id, null, value);

          } else {
            BeeKeeper.getScreen().removeProgress(prog.id);
            prog.id = null;

            closed++;
            if (closed >= count) {
              cancel();
            }
          }
        }
      }
    };
    timer.scheduleRepeating(100);
  }

  private static void showPropData(String caption, List<Property> data, String... columnLabels) {
    Global.showTable(caption, new PropertiesData(data, columnLabels));
  }

  private static void showProperties(String v, String[] arr, boolean errorPopup) {
    if (ArrayUtils.length(arr) < 2) {
      Global.sayHuh(v);
      return;
    }

    JavaScriptObject obj;
    if (arr[1].startsWith("#")) {
      obj = Document.get().getElementById(arr[1].substring(1));
    } else {
      obj = JsUtils.eval(arr[1]);
    }
    if (obj == null) {
      showError(errorPopup, arr[1], "not a js object");
      return;
    }

    String patt = ArrayUtils.getQuietly(arr, 2);
    JsArrayString prp = JsUtils.getProperties(obj, patt);

    if (JsUtils.isEmpty(prp)) {
      showError(errorPopup, v, "properties not found");
      return;
    }

    JsData<?> table = new JsData<TableColumn>(prp, "property", "type", "value");

    sortTable(table, 0);
    showTable(v, table);
  }

  private static void showRates(String args, final boolean errorPopup) {
    final DateTime dt = TimeUtils.parseDateTime(args, Format.getDefaultDateOrdering());
    final Table<Long, Long, Double> rates = Money.getRates(dt);

    if (rates.isEmpty()) {
      showError(errorPopup, "rates not available");
      return;
    }

    Queries.getRowSet(VIEW_CURRENCIES, Collections.singletonList(COL_CURRENCY_NAME),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            if (DataUtils.isEmpty(result)) {
              showError(errorPopup, VIEW_CURRENCIES, "not available");
              return;
            }

            List<Pair<Long, String>> currencies = new ArrayList<>();
            int index = result.getColumnIndex(COL_CURRENCY_NAME);

            for (BeeRow row : result) {
              currencies.add(Pair.of(row.getId(), row.getString(index)));
            }

            HtmlTable table = new HtmlTable(StyleUtils.NAME_INFO_TABLE);

            String center = StyleUtils.className(TextAlign.CENTER);
            String right = StyleUtils.className(TextAlign.RIGHT);
            String bold = StyleUtils.className(FontWeight.BOLD);

            table.setText(0, 0, Localized.dictionary().currency());

            for (int i = 0; i < currencies.size(); i++) {
              String name = currencies.get(i).getB();

              table.setText(i + 1, 0, name, bold);
              table.setText(0, i + 1, name, bold, center);
            }

            for (int r = 0; r < currencies.size(); r++) {
              long from = currencies.get(r).getA();

              for (int c = 0; c < currencies.size(); c++) {
                Double rate = rates.get(from, currencies.get(c).getA());
                if (rate != null) {
                  table.setText(r + 1, c + 1, BeeUtils.toString(rate, 10), right);
                }
              }
            }

            table.setCaption(BeeUtils.joinWords(Localized.dictionary().currencyRates(), dt));
            BeeKeeper.getScreen().show(table);
          }
        });
  }

  private static void showRectangle(String id, boolean errorPopup) {
    Element el = Document.get().getElementById(id);
    if (el == null) {
      showError(errorPopup, id, "element not found");
      return;
    }

    ClientRect startRect = ClientRect.createBounding(el);
    if (startRect == null) {
      showError(errorPopup, id, "rectangle not available");
    }

    String center = StyleUtils.className(TextAlign.CENTER);
    String right = StyleUtils.className(TextAlign.RIGHT);

    HtmlTable table = new HtmlTable();

    int row = 0;
    int col = 0;

    table.setHtml(row, col++, "tag");
    table.setHtml(row, col++, "id");
    table.setHtml(row, col++, "left");
    table.setHtml(row, col++, "right");
    table.setHtml(row, col++, "top");
    table.setHtml(row, col++, "bottom");
    table.setHtml(row, col++, "width");
    table.setHtml(row, col++, "height");
    table.setHtml(row, col++, "contains");
    table.setHtml(row, col++, "intersects");

    for (Element p = el; p != null; p = p.getParentElement()) {
      ClientRect rect = id.equals(p.getId()) ? startRect : ClientRect.createBounding(p);
      if (rect == null) {
        break;
      }

      row++;
      col = 0;

      table.setHtml(row, col++, p.getTagName());
      table.setHtml(row, col++, p.getId());
      table.setHtml(row, col++, BeeUtils.toString(rect.getLeft()), right);
      table.setHtml(row, col++, BeeUtils.toString(rect.getRight()), right);
      table.setHtml(row, col++, BeeUtils.toString(rect.getTop()), right);
      table.setHtml(row, col++, BeeUtils.toString(rect.getBottom()), right);
      table.setHtml(row, col++, BeeUtils.toString(rect.getWidth()), right);
      table.setHtml(row, col++, BeeUtils.toString(rect.getHeight()), right);
      table.setHtml(row, col++, rect.contains(startRect) ? "x" : "", center);
      table.setHtml(row, col++, rect.intersects(startRect) ? "x" : "", center);
    }

    Global.showModalWidget(table);
  }

  private static void showRpc(String args) {
    if (BeeKeeper.getRpc().getRpcList().isEmpty()) {
      inform("RpcList empty");

    } else if (BeeUtils.startsWith(args, 'c')) {
      BeeKeeper.getRpc().getRpcList().tryCompress();

    } else if (BeeUtils.startsWith(args, 'p')) {
      List<RpcInfo> requests = BeeKeeper.getRpc().getPendingRequests();

      if (requests.isEmpty()) {
        inform("no pending requests found");
      } else {
        String[][] data = new String[requests.size()][6];

        for (int i = 0; i < requests.size(); i++) {
          RpcInfo info = requests.get(i);

          int j = 0;
          data[i][j++] = BeeUtils.toString(info.getId());
          data[i][j++] = info.getStartTime();
          data[i][j++] = info.getService();
          data[i][j++] = info.getStateString();
          data[i][j++] = (info.getRequest() == null)
              ? BeeConst.NULL : BeeUtils.toString(info.getRequest().isPending());
          data[i][j++] = TimeUtils.elapsedSeconds(info.getStartMillis());
        }

        showMatrix("Pending requests", data,
            "id", "start", "service", "state", "pending", "elapsed");
      }

    } else {
      Global.showTable("rpc", new StringMatrix<TableColumn>(
          BeeKeeper.getRpc().getRpcList().getDefaultInfo(), RpcList.DEFAULT_INFO_COLUMNS));
    }
  }

  private static void showSimpleRowSet(String caption, SimpleRowSet rs) {
    if (DataUtils.isEmpty(rs)) {
      Global.showInfo("Simple rowset is empty");
      return;
    }

    String[][] matrix = new String[rs.getNumberOfRows()][rs.getNumberOfColumns()];

    for (int i = 0; i < rs.getNumberOfRows(); i++) {
      for (int j = 0; j < rs.getNumberOfColumns(); j++) {
        matrix[i][j] = rs.getValue(i, j);
      }
    }

    showMatrix(caption, matrix, rs.getColumnNames());
  }

  private static void showSize(String[] arr) {
    int len = ArrayUtils.length(arr);
    String html = ArrayUtils.getQuietly(arr, 1);

    if (BeeUtils.isEmpty(html)) {
      Global.sayHuh();
      return;
    }

    int pos = 2;

    if ("[".equals(html) && len > 3) {
      pos = len;
      StringBuilder sb = new StringBuilder(arr[2]);
      for (int i = 3; i < len; i++) {
        if ("]".equals(arr[i])) {
          pos = i + 1;
          break;
        }
        sb.append(BeeConst.CHAR_SPACE).append(arr[i]);
      }
      html = sb.toString();
    } else {
      Element elem = Document.get().getElementById(html);
      if (elem != null) {
        html = elem.getInnerHTML();
      }
    }

    Font font = (len > pos) ? Font.parse(ArrayUtils.slice(arr, pos)) : null;
    Size lineSize = Rulers.getLineSize(font, html, false);
    Size areaSize = Rulers.getAreaSize(font, html, true);

    int lineW = -1;
    int lineH = -1;
    int areaW = -1;
    int areaH = -1;

    if (lineSize != null) {
      lineW = lineSize.getWidth();
      lineH = lineSize.getHeight();
    }
    if (areaSize != null) {
      areaW = areaSize.getWidth();
      areaH = areaSize.getHeight();
    }

    List<Property> info = PropertyUtils.createProperties("Text Width", lineW, "Text Height", lineH,
        "Html Width", areaW, "Html Height", areaH);
    if (font != null) {
      info.addAll(font.getInfo());
    }

    InlineLabel span = new InlineLabel(html);
    if (font != null) {
      font.applyTo(span);
    }
    StyleUtils.setWhiteSpace(span, WhiteSpace.PRE);

    Label div = new Label(html);
    if (font != null) {
      font.applyTo(div);
    }

    HtmlTable table = new HtmlTable();
    table.setDefaultCellStyles("padding: 3px; border: 1px solid black;");
    StyleUtils.collapseBorders(table);

    for (int i = 0; i < info.size(); i++) {
      table.setHtml(i, 0, info.get(i).getName());
      table.setHtml(i, 1, info.get(i).getValue());
    }

    Flow panel = new Flow();

    panel.add(span);
    panel.add(div);
    panel.add(table);

    Global.showModalWidget(panel);
  }

  private static void showSlider(String[] arr) {
    double value = 0;
    double min = 0;
    double max = 100;
    double step = 1;
    int labels = 5;
    int ticks = 10;

    String pars = "v<>slt";
    int p = -1;
    int j;
    String s;
    String z;
    char c;

    for (int i = 1; i < arr.length; i++) {
      s = arr[i].toLowerCase();
      c = s.charAt(0);
      j = pars.indexOf(c);

      if (j >= 0) {
        z = s.substring(1);
        p = j;
      } else {
        z = s;
        p++;
      }

      switch (p) {
        case 0:
          value = BeeUtils.toDouble(z);
          break;
        case 1:
          min = BeeUtils.toDouble(z);
          break;
        case 2:
          max = BeeUtils.toDouble(z);
          break;
        case 3:
          step = BeeUtils.toDouble(z);
          break;
        case 4:
          labels = BeeUtils.toInt(z);
          break;
        case 5:
          ticks = BeeUtils.toInt(z);
          break;
      }
    }

    if (max <= min) {
      max = min + 100;
    }
    if (step <= 0 || step > (max - min) / 2) {
      step = (max - min) / 10;
    }
    if (value < min) {
      value = min;
    }
    if (value > max) {
      value = max;
    }

    BeeKeeper.getScreen().show(new SliderBar(value, min, max, step, labels, ticks));
  }

  private static void showSupport(String args, boolean errorPopup) {
    List<Property> data = Features.getInfo();

    if (BeeUtils.isEmpty(args)) {
      showPropData("Support", data);

    } else {
      List<Property> filtered = new ArrayList<>();
      for (Property p : data) {
        if (BeeUtils.containsSame(p.getName(), args) || p.getValue().equalsIgnoreCase(args)) {
          filtered.add(p);
        }
      }

      if (filtered.isEmpty()) {
        showError(errorPopup, "feature not tested:", args);
      } else if (filtered.size() == 1) {
        Property p = filtered.get(0);
        inform(p.getName(), p.getValue());
      } else {
        showPropData("Features " + args, filtered);
      }
    }
  }

  private static void showSvg(String[] arr) {
    if (!Features.supportsSvg()) {
      BeeKeeper.getScreen().notifySevere("svg not supported");
      return;
    }

    int width = BeeKeeper.getScreen().getActivePanelWidth();
    int height = BeeKeeper.getScreen().getActivePanelHeight();

    int type = BeeUtils.randomInt(0, 3);

    int rMin = Math.min(width, height) / 50;
    int rMax = rMin * 10;

    int cntMin = 10;
    int cntMax = 100;

    int colorStep = 16;

    double minOpacity = 0.5;
    double maxOpacity = 1;

    int len = ArrayUtils.length(arr);
    if (len > 1) {
      for (int i = 1; i < len; i++) {
        String s = BeeUtils.trim(arr[i]);
        if (BeeUtils.isEmpty(s) || s.length() < 2) {
          continue;
        }

        char pName = s.charAt(0);
        String pMin = BeeUtils.getPrefix(s.substring(1), BeeConst.CHAR_MINUS);
        String pMax = BeeUtils.getSuffix(s.substring(1), BeeConst.CHAR_MINUS);
        if (BeeUtils.isEmpty(pMin)) {
          pMin = s.substring(1);
        }

        if (BeeUtils.isDouble(pMin) || BeeUtils.isDouble(pMax)) {
          switch (pName) {
            case 't':
              if (BeeUtils.isDigit(pMin)) {
                type = BeeUtils.toInt(pMin);
              } else if (BeeUtils.isDigit(pMax)) {
                type = BeeUtils.toInt(pMax);
              }
              break;

            case 'r':
              if (BeeUtils.isDigit(pMin)) {
                rMin = BeeUtils.toInt(pMin);
              }
              if (BeeUtils.isDigit(pMax)) {
                rMax = BeeUtils.toInt(pMax);
              }
              if (rMax == rMin) {
                rMax = rMin + 1;
              } else if (rMax < rMin) {
                int z = rMax;
                rMax = rMin;
                rMin = z;
              }
              break;

            case 'c':
              if (BeeUtils.isDigit(pMin)) {
                cntMin = BeeUtils.toInt(pMin);
              }
              if (BeeUtils.isDigit(pMax)) {
                cntMax = BeeUtils.toInt(pMax);
              }
              if (cntMax == cntMin) {
                cntMax = cntMin + 1;
              } else if (cntMax < cntMin) {
                int z = cntMax;
                cntMax = cntMin;
                cntMin = z;
              }
              break;

            case 's':
              if (BeeUtils.isDigit(pMin)) {
                colorStep = BeeUtils.toInt(pMin);
              } else if (BeeUtils.isDigit(pMax)) {
                colorStep = BeeUtils.toInt(pMax);
              }
              break;

            case 'o':
              if (BeeUtils.isDouble(pMin)) {
                minOpacity = BeeUtils.toDouble(pMin);
              }
              if (BeeUtils.isDouble(pMax)) {
                maxOpacity = BeeUtils.toDouble(pMax);
              }
              if (maxOpacity < minOpacity) {
                double z = maxOpacity;
                maxOpacity = minOpacity;
                minOpacity = z;
              }
              break;
          }
        }
      }
    }

    Svg widget = new Svg();

    Element parent = widget.getElement();
    StyleUtils.fullWidth(parent);
    StyleUtils.fullHeight(parent);

    Element child;

    for (int i = 0; i < BeeUtils.randomInt(cntMin, cntMax); i++) {
      String x = BeeUtils.toString(BeeUtils.randomInt(0, width));
      String y = BeeUtils.toString(BeeUtils.randomInt(0, height));
      String rx = BeeUtils.toString(BeeUtils.randomInt(rMin, rMax));
      String ry = BeeUtils.toString(BeeUtils.randomInt(rMin, rMax));

      switch (type) {
        case 0:
          child = DomUtils.createSvg("rect");
          child.setAttribute("x", x);
          child.setAttribute("y", y);
          child.setAttribute("width", rx);
          child.setAttribute("height", ry);
          break;
        case 1:
          child = DomUtils.createSvg("circle");
          child.setAttribute("cx", x);
          child.setAttribute("cy", y);
          child.setAttribute("r", rx);
          break;
        default:
          child = DomUtils.createSvg("ellipse");
          child.setAttribute("cx", x);
          child.setAttribute("cy", y);
          child.setAttribute("rx", rx);
          child.setAttribute("ry", ry);
      }

      int r = Math.min(BeeUtils.randomInt(0, colorStep + 1) * 256 / colorStep, 255);
      int g = Math.min(BeeUtils.randomInt(0, colorStep + 1) * 256 / colorStep, 255);
      int b = Math.min(BeeUtils.randomInt(0, colorStep + 1) * 256 / colorStep, 255);

      child.setAttribute("fill", "rgb(" + r + "," + g + "," + b + ")");
      child.setAttribute("opacity", BeeUtils.toString((minOpacity == maxOpacity)
          ? minOpacity : BeeUtils.randomDouble(minOpacity, maxOpacity)));

      DomUtils.createId(child, child.getTagName());

      parent.appendChild(child);
    }

    BeeKeeper.getScreen().show(new Simple(widget, Overflow.HIDDEN));
  }

  private static void showTable(String caption, IsTable<?, ?> table) {
    if (showModal(table.getNumberOfRows())) {
      Global.showModalGrid(caption, table);
    } else {
      Global.showTable(caption, table);
    }
  }

  private static void showTableInfo(String input, String args) {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.GET_TABLE_INFO);
    if (!BeeUtils.isEmpty(args)) {
      params.addPositionalHeader(args.trim());
    }

    BeeKeeper.getRpc().makeGetRequest(params, ResponseHandler.callback(input));
  }

  private static void showUnits(String[] arr) {
    int len = ArrayUtils.length(arr);

    Double value = null;
    CssUnit unit = null;
    Font font = null;
    int containerSize = BeeKeeper.getScreen().getActivePanelWidth();

    int i = 1;
    while (i < len) {
      if ("f".equals(arr[i]) && i < len - 1) {
        font = Font.parse(ArrayUtils.slice(arr, i + 1));
        break;
      }
      if ("c".equals(arr[i]) && i < len - 1) {
        containerSize = BeeUtils.toInt(arr[i + 1]);
        i += 2;
        continue;
      }

      if (BeeUtils.isDouble(arr[i])) {
        value = BeeUtils.toDouble(arr[i]);
      } else {
        unit = CssUnit.parse(arr[i]);
      }
      i++;
    }

    List<CssUnit> units = new ArrayList<>();
    if (value == null) {
      value = 1.0;
    }

    if (unit != null) {
      units.add(unit);
    } else {
      Collections.addAll(units, CssUnit.values());
    }

    List<Property> info = PropertyUtils.createProperties("Value", value);
    if (font != null) {
      info.addAll(font.getInfo());
    }

    info.add(new Property("Container Size", containerSize));

    for (CssUnit u : units) {
      double px = Rulers.getPixels(value, u, font, BeeUtils.unbox(containerSize));
      info.add(new Property(u.getCaption(), BeeUtils.toString(px, 5)));
    }

    showTable("Pixels", new PropertiesData(info));
  }

  private static void showUserMedia(String[] arr, boolean errorPopup) {
    if (!Features.supportsGetUserMedia()) {
      showError(errorPopup, "gum not supported");
      return;
    }

    int len = ArrayUtils.length(arr);

    MediaStreamConstraints constraints = new MediaStreamConstraints();
    if (len > 1) {
      for (int i = 1; i < len; i++) {
        if (BeeUtils.startsWith(arr[i], 'a')) {
          constraints.setAudio(true);
        } else if (BeeUtils.startsWith(arr[i], 'v')) {
          constraints.setVideo(true);
        }
      }
    }

    if (!constraints.isAudio() && !constraints.isVideo()) {
      constraints.setAudio(true);
      constraints.setVideo(true);
    }

    RtcAdapter.getUserMedia(constraints, stream -> renderUserMedia(stream, constraints),
        error -> showError(errorPopup, "gum error", MediaUtils.format(error)));
  }

  private static void renderUserMedia(MediaStream stream, MediaStreamConstraints constraints) {
    Flow container = new Flow();

    if (constraints.isVideo()) {
      Video video = new Video();
      video.setControls(true);
      video.setAutoplay(true);

      RtcAdapter.attachMediaStream(video.getMediaElement(), stream);

      container.add(video);

    } else {
      Audio audio = new Audio();
      audio.setControls(true);
      audio.setAutoplay(true);

      RtcAdapter.attachMediaStream(audio.getMediaElement(), stream);

      container.add(audio);
    }

    BeeKeeper.getScreen().show(container);
  }

  private static void showViewInfo(String input, String args) {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.GET_VIEW_INFO);
    if (!BeeUtils.isEmpty(args)) {
      params.addPositionalHeader(args.trim());
    }

    BeeKeeper.getRpc().makeGetRequest(params, ResponseHandler.callback(input));
  }

  private static void showViewSuppliers() {
    Collection<String> keys = ViewFactory.getKeys();

    if (keys.isEmpty()) {
      logger.info("view suppliers not registered");

    } else {
      HtmlTable table = new HtmlTable(BeeConst.CSS_CLASS_PREFIX + "info-table");
      table.setCaption("Suppliers");

      int row = 0;
      for (String key : keys) {
        table.setText(row++, 0, key);
      }

      BeeKeeper.getScreen().show(table);
    }
  }

  private static void showWebNote(String[] arr) {
    String title = null;

    String dir = null;
    String lang = null;
    String body = null;
    String tag = null;
    String icon = null;

    for (int i = 1; i < arr.length; i++) {
      String s = arr[i];

      if (s.length() > 2 && s.charAt(1) == BeeConst.CHAR_EQ) {
        String v = s.substring(2).trim();

        switch (s.charAt(0)) {
          case 'd':
            dir = v;
            break;
          case 'l':
            lang = v;
            break;
          case 'b':
            body = v;
            break;
          case 't':
            tag = v;
            break;
          case 'i':
            icon = v;
            break;

          default:
            title = BeeUtils.joinWords(title, s);
        }

      } else {
        title = BeeUtils.joinWords(title, s);
      }
    }

    NotificationOptions options = null;
    if (!BeeUtils.allEmpty(dir, lang, body, tag, icon)) {
      options = new NotificationOptions();

      if (dir != null) {
        options.setDir(EnumUtils.getEnumByName(NotificationOptions.NotificationDirection.class,
            dir));
      }
      if (lang != null) {
        options.setLang(lang);
      }
      if (body != null) {
        options.setBody(body);
      }
      if (tag != null) {
        options.setTag(tag);
      }
      if (icon != null) {
        options.setIcon(icon);
      }
    }

    logger.debug(title, options);

    WebNotification.create(title, options, new Callback<WebNotification>() {
      @Override
      public void onFailure(String... reason) {
        logger.warning(ArrayUtils.joinWords(reason));
      }

      @Override
      public void onSuccess(WebNotification result) {
        logger.debug("success");
        logger.addSeparator();
      }
    });
  }

  private static void showWidgetInfo(String[] arr, boolean errorPopup) {
    String id = ArrayUtils.getQuietly(arr, 1);
    if (BeeUtils.isEmpty(id)) {
      showError(errorPopup, "widget id not specified");
      return;
    }

    Widget widget = DomUtils.getWidget(id);
    if (widget == null) {
      showError(errorPopup, id, "widget not found");
      return;
    }

    String z = ArrayUtils.getQuietly(arr, 2);
    int depth = BeeUtils.isDigit(z) ? BeeUtils.toInt(z) : 0;

    List<ExtendedProperty> info = DomUtils.getInfo(widget, id, depth);
    showExtData(BeeUtils.joinWords("Widget", id, z), info);
  }

  private static void sleep(String[] arr) {
    for (int i = 1; i < arr.length; i++) {
      String millis = arr[i];

      if (BeeUtils.isPositiveInt(millis)) {
        BeeKeeper.getRpc().invoke("sleep", millis, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (response.hasResponse()) {
              logger.debug(response.getResponseAsString());
            }
          }
        });
      }
    }
  }

  private static void sortTable(IsTable<?, ?> table, int col) {
    if (table.getNumberOfRows() > 1) {
      List<Pair<Integer, Boolean>> sortInfo = new ArrayList<>();
      sortInfo.add(Pair.of(col, true));

      table.sort(sortInfo, Collator.DEFAULT);
    }
  }

  private static void storage(String[] arr, boolean errorPopup) {
    int parCnt = ArrayUtils.length(arr) - 1;
    int len = BeeKeeper.getStorage().length();

    if (parCnt <= 1 && len <= 0) {
      inform("Storage empty");
      return;
    }

    if (parCnt <= 0) {
      showPropData("Storage", BeeKeeper.getStorage().getAll(200));
      return;
    }

    String key = arr[1];

    if (parCnt == 1) {
      if (key.equals(BeeConst.STRING_MINUS)) {
        BeeKeeper.getStorage().clear();
        inform(BeeUtils.joinWords(len, "items cleared"));
      } else {
        String z = BeeKeeper.getStorage().get(key);
        if (z == null) {
          showError(errorPopup, Localized.dictionary().keyNotFound(key));
        } else {
          inform(key, z);
        }
      }
      return;
    }

    String value = ArrayUtils.join(BeeConst.STRING_SPACE, arr, 2);

    if (key.equals(BeeConst.STRING_MINUS)) {
      if (BeeKeeper.getStorage().hasItem(value)) {
        BeeKeeper.getStorage().remove(value);
        inform(value, "removed");

      } else {
        int count = 0;
        List<String> keys = BeeKeeper.getStorage().keys();

        for (String k : keys) {
          if (BeeUtils.isPrefix(k, value)) {
            BeeKeeper.getStorage().remove(k);
            count++;
          }
        }
        inform(value, "removed", BeeUtils.toString(count), "entries");
      }

    } else {
      BeeKeeper.getStorage().set(key, value);
      inform("Storage", NameUtils.addName(key, value));
    }
  }

  private static void style(String v, String[] arr, boolean errorPopup) {
    if (ArrayUtils.length(arr) < 2) {
      JsStyleSheetList sheets = JsBrowser.getDocument().getStyleSheets();
      int sheetCnt = (sheets == null) ? 0 : sheets.getLength();

      List<ExtendedProperty> lst = new ArrayList<>();
      PropertyUtils.addExtended(lst, "sheets", "count", sheetCnt);

      for (int i = 0; i < sheetCnt; i++) {
        JsCSSStyleSheet sheet = (JsCSSStyleSheet) sheets.item(i);
        if (sheet == null) {
          PropertyUtils.addExtended(lst, "sheet", i, "null");
          continue;
        }

        JsCSSRuleList rules = sheet.getCssRules();
        if (rules == null) {
          rules = sheet.getRules();
        }

        int len = (rules == null) ? 0 : rules.length();
        PropertyUtils.addExtended(lst, "sheet " + BeeUtils.progress(i + 1, sheetCnt), "rules", len);

        for (int j = 0; j < len; j++) {
          PropertyUtils.addExtended(lst, "rule", BeeUtils.progress(j + 1, len),
              rules.item(j).getCssText());
        }
      }

      showExtData("Sheets", lst);
      return;
    }

    if (!v.contains("{")) {
      Element elem = Document.get().getElementById(arr[1]);
      if (elem == null) {
        inform("element id", arr[1], "not found");
        return;
      }

      List<Property> info = new ArrayList<>();

      if (arr.length > 2) {
        for (int i = 2; i < arr.length; i++) {
          String key = arr[i];

          String value = elem.getStyle().getProperty(key);
          if (value != null) {
            info.add(new Property(key, value));
          }

          value = ComputedStyles.get(elem, key);
          if (!BeeUtils.isEmpty(value)) {
            info.add(new Property("computed " + key, value));
          }
        }

      } else {
        List<Property> lst;

        if (elem.getStyle() != null) {
          lst = JsUtils.getInfo(elem.getStyle());
          if (!BeeUtils.isEmpty(lst)) {
            info.add(new Property("element style", BeeUtils.bracket(lst.size())));
            info.addAll(lst);
          }
        }

        lst = new ComputedStyles(elem).getInfo();
        if (!BeeUtils.isEmpty(lst)) {
          info.add(new Property("computed style", BeeUtils.bracket(lst.size())));
          info.addAll(lst);
        }
      }

      if (BeeUtils.isEmpty(info)) {
        inform("id", arr[1], "has no style", ArrayUtils.join(BeeConst.STRING_SPACE, arr, 2));
      } else {
        showPropData(v, info);
      }
      return;
    }

    boolean start = false;
    boolean end = false;
    boolean immediate = false;

    StringBuilder sb = new StringBuilder();
    boolean tg = false;

    for (int i = 1; i < arr.length; i++) {
      if (!tg) {
        if (arr[i].contains("{")) {
          tg = true;
        } else {
          if (BeeUtils.same(arr[i], "start")) {
            start = true;
            continue;
          }
          if (BeeUtils.same(arr[i], "end")) {
            end = true;
            continue;
          }
          if ("+".equals(arr[i])) {
            immediate = true;
            continue;
          }
        }
      }
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(arr[i]);
    }

    String st = sb.toString();
    if (!st.contains("{") || !st.contains("}")) {
      showError(errorPopup, "Nah pop no style, a strictly roots", v, st);
      return;
    }

    logger.info(st);
    logger.addSeparator();

    if (start) {
      StyleInjector.injectAtStart(st, immediate);
    } else if (end) {
      StyleInjector.injectAtEnd(st, immediate);
    } else {
      StyleInjector.inject(st, immediate);
    }
  }

  private static void unicode(String[] arr) {
    StringBuilder sb = new StringBuilder();
    int len = ArrayUtils.length(arr);

    if (len < 2 || len == 2 && BeeUtils.isDigit(ArrayUtils.getQuietly(arr, 1))) {
      int n = (len < 2) ? 10 : BeeUtils.toInt(arr[1]);
      for (int i = 0; i < n; i++) {
        sb.append((char) BeeUtils.randomInt(Character.MIN_VALUE, Character.MIN_SURROGATE));
      }

    } else {
      for (int i = 1; i < len; i++) {
        String s = arr[i];

        if (s.length() > 1 && BeeUtils.inListSame(s.substring(0, 1), "u", "x")
            && BeeUtils.isHexString(s.substring(1))) {
          sb.append(BeeUtils.fromHex(s.substring(1)));
        } else if (s.length() > 2 && BeeUtils.startsSame(s, "0x")
            && BeeUtils.isHexString(s.substring(2))) {
          sb.append(BeeUtils.fromHex(s.substring(2)));

        } else if (BeeUtils.isDigit(s)) {
          int n = BeeUtils.toInt(s);
          if (n > 0 && n < Character.MAX_VALUE && sb.length() > 0) {
            for (int j = 0; j < n; j++) {
              sb.append((char) (sb.charAt(sb.length() - 1) + 1));
            }
          } else {
            sb.append((char) n);
          }

        } else {
          sb.append(s);
        }
      }
    }

    final String input = sb.toString();

    BeeKeeper.getRpc().invoke("stringInfo", input, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        ResponseHandler.unicodeTest(input, response);
      }
    });
  }

  private static void upload() {
    final Popup popup = new Popup(OutsideClick.CLOSE);

    final InputFile widget = new InputFile(true);
    widget.addChangeHandler(event -> {
      popup.close();
      List<NewFileInfo> files = FileUtils.getNewFileInfos(widget.getFiles());

      for (final NewFileInfo fi : files) {
        logger.debug("uploading", fi.getName(), fi.getType(), fi.getSize());
        FileUtils.uploadFile(fi,
            result -> logger.debug("uploaded", fi.getName(), ", result:", result));
      }
    });

    popup.setWidget(widget);
    popup.center();
  }

  private static void whereAmI() {
    logger.info(BeeConst.whereAmI());
    BeeKeeper.getRpc().makeGetRequest(Service.WHERE_AM_I);
  }

  private CliWorker() {
  }
}
