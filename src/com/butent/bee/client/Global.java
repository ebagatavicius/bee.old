package com.butent.bee.client;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.ClientDefaults;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.InputBoxes;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.MessageBoxes;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.images.Images;
import com.butent.bee.client.modules.administration.AdministrationKeeper;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.output.ReportSettings;
import com.butent.bee.client.screen.Favorites;
import com.butent.bee.client.screen.Spaces;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.WidgetInitializer;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.search.Filters;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssProperties;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.Defaults;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.IsTable;
import com.butent.bee.shared.data.cache.CacheManager;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * initializes and contains system parameters, which are used globally in the whole system.
 */

public final class Global {

  private static final BeeLogger logger = LogUtils.getLogger(Global.class);

  private static final MessageBoxes msgBoxen = new MessageBoxes();
  private static final InputBoxes inpBoxen = new InputBoxes();

  private static final CacheManager cache = new CacheManager();

  private static final Images.Resources images = Images.createResources();

  private static final Map<String, String> styleSheets = Maps.newHashMap();

  private static final Favorites favorites = new Favorites();
  private static final Spaces spaces = new Spaces();

  private static final Defaults defaults = new ClientDefaults();

  private static final Search search = new Search();

  private static final Filters filters = new Filters();

  private static final Users users = new Users();
  private static final Rooms rooms = new Rooms();

  private static final NewsAggregator newsAggregator = new NewsAggregator();

  private static final ReportSettings reportSettings = new ReportSettings();

  private static boolean debug;

  public static void addStyleSheet(String name, String text) {
    if (BeeUtils.isEmpty(name)) {
      logger.warning("style sheet name not specified");
      return;
    }
    if (BeeUtils.isEmpty(text)) {
      logger.warning("style sheet text not specified");
      return;
    }

    String key = name.trim().toLowerCase();
    String value = text.trim();

    if (!value.equals(styleSheets.get(key))) {
      styleSheets.put(key, value);

      String css = CharMatcher.BREAKING_WHITESPACE.collapseFrom(value, BeeConst.CHAR_SPACE);
      StyleInjector.inject(css);
      Printer.onInjectStyleSheet(css);
    }
  }

  public static void addStyleSheets(Map<String, String> sheets) {
    if (sheets == null) {
      return;
    }
    for (Map.Entry<String, String> entry : sheets.entrySet()) {
      addStyleSheet(entry.getKey(), entry.getValue());
    }
  }

  public static void choice(String caption, String prompt, List<String> options,
      ChoiceCallback callback) {
    msgBoxen.choice(caption, prompt, options, callback, BeeConst.UNDEF, BeeConst.UNDEF, null, null);
  }

  public static void confirm(String message, ConfirmationCallback callback) {
    confirm(null, null, Lists.newArrayList(message), callback);
  }

  public static void confirm(String caption, Icon icon, List<String> messages,
      ConfirmationCallback callback) {
    confirm(caption, icon, messages, Localized.getConstants().yes(), Localized.getConstants().no(),
        callback);
  }

  public static void confirm(String caption, Icon icon, List<String> messages,
      String optionYes, String optionNo, ConfirmationCallback callback) {
    confirm(caption, icon, messages, optionYes, optionNo, callback, null);
  }

  public static void confirm(String caption, Icon icon, List<String> messages,
      String optionYes, String optionNo, ConfirmationCallback callback, Element target) {
    msgBoxen.confirm(caption, icon, messages, optionYes, optionNo, callback, null, null, null,
        target);
  }

  public static void confirmDelete(String caption, Icon icon, List<String> messages,
      ConfirmationCallback callback) {
    confirmDelete(caption, icon, messages, callback, null);
  }

  public static void confirmDelete(String caption, Icon icon, List<String> messages,
      ConfirmationCallback callback, Element target) {
    msgBoxen.confirm(caption, icon, messages, Localized.getConstants().delete(),
        Localized.getConstants().cancel(), callback, null,
        StyleUtils.className(FontSize.LARGE), StyleUtils.className(FontSize.MEDIUM), target);
  }

  public static void confirmRemove(String caption, String item, ConfirmationCallback callback) {
    confirmRemove(caption, item, callback, null);
  }

  public static void confirmRemove(String caption, String item, ConfirmationCallback callback,
      Element target) {
    List<String> messages = Lists.newArrayList(Localized.getMessages().removeQuestion(item));
    msgBoxen.confirm(caption, Icon.WARNING, messages, Localized.getConstants().actionRemove(),
        Localized.getConstants().cancel(), callback, null,
        StyleUtils.className(FontSize.MEDIUM), StyleUtils.className(FontSize.MEDIUM), target);
  }

  public static void debug(String s) {
    logger.debug(s);
  }

  public static void decide(String caption, List<String> messages, DecisionCallback callback,
      int defaultValue) {
    msgBoxen.decide(caption, messages, callback, defaultValue, null, null, null, null);
  }

  public static CacheManager getCache() {
    return cache;
  }

  public static Defaults getDefaults() {
    return defaults;
  }

  public static Favorites getFavorites() {
    return favorites;
  }

  public static Filters getFilters() {
    return filters;
  }

  public static Images.Resources getImages() {
    return images;
  }

  public static InputBoxes getInpBoxen() {
    return inpBoxen;
  }

  public static MessageBoxes getMsgBoxen() {
    return msgBoxen;
  }

  public static NewsAggregator getNewsAggregator() {
    return newsAggregator;
  }

  public static void getParameter(String prm, final Consumer<String> prmConsumer) {
    Assert.notEmpty(prm);
    Assert.notNull(prmConsumer);

    ParameterList args = AdministrationKeeper.createArgs(SVC_GET_PARAMETER);
    args.addDataItem(VAR_PARAMETER, prm);

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (!response.hasErrors()) {
          prmConsumer.accept(response.getResponseAsString());
        }
      }
    });
  }

  public static ReportSettings getReportSettings() {
    return reportSettings;
  }

  public static Rooms getRooms() {
    return rooms;
  }

  public static Search getSearch() {
    return search;
  }

  public static Widget getSearchWidget() {
    return search.ensureSearchWidget();
  }

  public static Spaces getSpaces() {
    return spaces;
  }

  public static Map<String, String> getStyleSheets() {
    return styleSheets;
  }

  public static Users getUsers() {
    return users;
  }

  public static void inputCollection(String caption, String valueCaption, boolean unique,
      Collection<String> defaultCollection, Consumer<Collection<String>> consumer,
      Function<String, Editor> editorSupplier) {
    inpBoxen.inputCollection(caption, valueCaption, unique, defaultCollection, consumer,
        editorSupplier);
  }

  public static void inputMap(String caption, String keyCaption, String valueCaption,
      Map<String, String> map, Consumer<Map<String, String>> consumer) {
    inpBoxen.inputMap(caption, keyCaption, valueCaption, map, consumer);
  }

  public static void inputString(String caption, String prompt, StringCallback callback) {
    inputString(caption, prompt, callback, null);
  }

  public static void inputString(String caption, String prompt, StringCallback callback,
      String defaultValue) {
    inputString(caption, prompt, callback, defaultValue, BeeConst.UNDEF);
  }

  public static void inputString(String caption, String prompt, StringCallback callback,
      String defaultValue, int maxLength) {
    inputString(caption, prompt, callback, defaultValue, maxLength, null);
  }

  public static void inputString(String caption, String prompt, StringCallback callback,
      String defaultValue, int maxLength, Element target) {
    inputString(caption, prompt, callback, defaultValue, maxLength, target,
        BeeConst.DOUBLE_UNDEF, null);
  }

  public static void inputString(String caption, String prompt, StringCallback callback,
      String defaultValue, int maxLength, Element target, double width, CssUnit widthUnit) {
    inputString(caption, prompt, callback, defaultValue, maxLength, target, width, widthUnit,
        BeeConst.UNDEF, Localized.getConstants().ok(), Localized.getConstants().cancel(), null);
  }

  public static void inputString(String caption, String prompt, StringCallback callback,
      String defaultValue, int maxLength, Element target, double width, CssUnit widthUnit,
      int timeout, String confirmHtml, String cancelHtml, WidgetInitializer initializer) {
    inpBoxen.inputString(caption, prompt, callback, defaultValue, maxLength, target,
        width, widthUnit, timeout, confirmHtml, cancelHtml, initializer);
  }

  public static void inputString(String caption, StringCallback callback) {
    inputString(caption, null, callback);
  }

  public static DialogBox inputWidget(String caption, IsWidget input, InputCallback callback) {
    return inputWidget(caption, input, callback, null, null, Action.NO_ACTIONS);
  }

  public static DialogBox inputWidget(String caption, IsWidget input, InputCallback callback,
      String dialogStyle) {
    return inputWidget(caption, input, callback, dialogStyle, null, Action.NO_ACTIONS);
  }

  public static DialogBox inputWidget(String caption, IsWidget input, InputCallback callback,
      String dialogStyle, Element target) {
    return inputWidget(caption, input, callback, dialogStyle, target, Action.NO_ACTIONS);
  }

  public static DialogBox inputWidget(String caption, IsWidget input, InputCallback callback,
      String dialogStyle, Element target, Set<Action> enabledActions) {
    return inpBoxen.inputWidget(caption, input, callback, dialogStyle, target, enabledActions,
        null);
  }

  public static boolean isDebug() {
    return debug;
  }

  public static void messageBox(String caption, Icon icon, List<String> messages,
      List<String> options, int defaultValue, ChoiceCallback callback) {
    msgBoxen.display(caption, icon, messages, options, defaultValue, callback, BeeConst.UNDEF,
        null, null, null, null, null);
  }

  public static void messageBox(String caption, Icon icon, String message) {
    messageBox(caption, icon, Lists.newArrayList(message),
        Lists.newArrayList(Localized.getConstants().ok()), 0, null);
  }

  public static boolean nativeConfirm(String... lines) {
    return msgBoxen.nativeConfirm(lines);
  }

  public static HtmlTable renderTable(String caption, IsTable<?, ?> data) {
    int c = data.getNumberOfColumns();
    Assert.isPositive(c);

    HtmlTable table = new HtmlTable();
    table.setCaption(caption);

    int r = 0;
    for (int i = 0; i < c; i++) {
      String label = BeeUtils.notEmpty(data.getColumnLabel(i), data.getColumnId(i));
      table.setHtml(r, i, label);

      TableCellElement cell = table.getCellFormatter().getElement(r, i);
      StyleUtils.setTextAlign(cell, TextAlign.CENTER);
      StyleUtils.setProperty(cell, CssProperties.FONT_WEIGHT, FontWeight.BOLD);
    }

    Range<Long> maybeTime = Range.closed(
        TimeUtils.startOfYear(TimeUtils.today(), -10).getTime(),
        TimeUtils.startOfYear(TimeUtils.today(), 100).getTime());

    for (IsRow row : data) {
      r++;

      for (int i = 0; i < c; i++) {
        if (!row.isNull(i)) {
          ValueType type = data.getColumnType(i);
          String value = DataUtils.render(data.getColumn(i), row, i);

          if (type == ValueType.LONG) {
            Long x = row.getLong(i);
            if (x != null && maybeTime.contains(x)) {
              type = ValueType.DATE_TIME;
              value = new DateTime(x).toCompactString();
            }
          }

          table.setHtml(r, i, value);

          if (ValueType.isNumeric(type)) {
            table.getCellFormatter().setHorizontalAlignment(r, i, TextAlign.RIGHT);
          }
        }
      }
    }

    return table;
  }

  public static void sayHuh(String... huhs) {
    String caption;
    List<String> messages;

    if (huhs == null) {
      caption = null;
      messages = Lists.newArrayList("Huh");
    } else {
      caption = "Huh";
      messages = Lists.newArrayList(huhs);
    }

    messageBox(caption, Icon.QUESTION, messages, Lists.newArrayList("kthxbai"), 0, null);
  }

  public static void setDebug(boolean debug) {
    Global.debug = debug;
  }

  public static void setParameter(String prm, String value) {
    Assert.notEmpty(prm);

    ParameterList args = AdministrationKeeper.createArgs(SVC_SET_PARAMETER);
    args.addDataItem(VAR_PARAMETER, prm);

    if (!BeeUtils.isEmpty(value)) {
      args.addDataItem(VAR_PARAMETER_VALUE, value);
    }
    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());
      }
    });
  }

  public static void showError(List<String> messages) {
    showError(Localized.getConstants().error(), messages, null, null);
  }

  public static void showError(String message) {
    List<String> messages = Lists.newArrayList();
    if (!BeeUtils.isEmpty(message)) {
      messages.add(message);
    }

    showError(Localized.getConstants().error(), messages);
  }

  public static void showError(String caption, List<String> messages) {
    showError(caption, messages, null, null);
  }

  public static void showError(String caption, List<String> messages, String dialogStyle) {
    showError(caption, messages, dialogStyle, null);
  }

  public static void showError(String caption, List<String> messages, String dialogStyle,
      String closeHtml) {
    msgBoxen.showError(caption, messages, dialogStyle, closeHtml);
  }

  public static void showInfo(List<String> messages) {
    showInfo(null, messages, null, null);
  }

  public static void showInfo(String message) {
    List<String> messages = Lists.newArrayList();
    if (!BeeUtils.isEmpty(message)) {
      messages.add(message);
    }

    showInfo(messages);
  }

  public static void showInfo(String caption, List<String> messages) {
    showInfo(caption, messages, null, null);
  }

  public static void showInfo(String caption, List<String> messages, String dialogStyle) {
    showInfo(caption, messages, dialogStyle, null);
  }

  public static void showInfo(String caption, List<String> messages, String dialogStyle,
      String closeHtml) {
    msgBoxen.showInfo(caption, messages, dialogStyle, closeHtml);
  }

  public static void showModalGrid(String caption, IsTable<?, ?> table) {
    msgBoxen.showTable(caption, table);
  }

  public static void showModalWidget(String caption, Widget widget) {
    showModalWidget(caption, widget, null);
  }

  public static void showModalWidget(String caption, Widget widget, Element target) {
    msgBoxen.showWidget(caption, widget, target);
  }

  public static void showModalWidget(Widget widget) {
    showModalWidget(null, widget, null);
  }

  public static void showModalWidget(Widget widget, Element target) {
    showModalWidget(null, widget, target);
  }

  public static void showTable(String caption, IsTable<?, ?> table) {
    if (table == null || table.getNumberOfColumns() <= 0 || table.getNumberOfRows() <= 0) {
      logger.warning(caption, "table is empty");
      return;
    }

    IdentifiableWidget widget;

    if (BeeKeeper.getStorage().hasItem("info-grid")) {
      widget = GridFactory.simpleGrid(caption, table, BeeKeeper.getScreen().getActivePanelWidth());
    } else {
      widget = renderTable(caption, table);
      widget.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "info-table");
    }

    if (widget != null) {
      BeeKeeper.getScreen().showWidget(widget);
    }
  }

  static void init() {
    initCache();
    initImages();
    initFavorites();
    initNewsAggregator();

    exportMethods();
  }

  // CHECKSTYLE:OFF
  private static native void exportMethods() /*-{
    $wnd.Bee_updateForm = $entry(@com.butent.bee.client.ui.UiHelper::updateForm(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
    $wnd.Bee_debug = $entry(@com.butent.bee.client.Global::debug(Ljava/lang/String;));
    $wnd.Bee_updateActor = $entry(@com.butent.bee.client.decorator.TuningHelper::updateActor(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
    $wnd.Bee_maybeTranslate = $entry(@com.butent.bee.shared.i18n.Localized::maybeTranslate(Ljava/lang/String;));
  }-*/;

  // CHECKSTYLE:ON

  private static void initCache() {
    BeeKeeper.getBus().registerDataHandler(getCache(), true);
  }

  private static void initFavorites() {
    BeeKeeper.getBus().registerRowDeleteHandler(getFavorites(), false);
    BeeKeeper.getBus().registerMultiDeleteHandler(getFavorites(), false);
  }

  private static void initImages() {
    Images.init(getImages());
  }

  private static void initNewsAggregator() {
    BeeKeeper.getBus().registerDataHandler(getNewsAggregator(), true);
  }

  private Global() {
  }
}
