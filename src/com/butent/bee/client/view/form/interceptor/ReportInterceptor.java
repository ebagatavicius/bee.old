package com.butent.bee.client.view.form.interceptor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.ModalGrid;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.TextBox;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.ui.HasStringValue;
import com.butent.bee.shared.ui.UserInterface.Component;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ReportInterceptor extends AbstractFormInterceptor implements Printable {

  protected static final String PERCENT_PATTERN = "0.0";
  protected static final String QUANTITY_PATTERN = "#,###";
  protected static final String AMOUNT_PATTERN = "#,##0.00";

  protected static final String NAME_DATA_CONTAINER = "DataContainer";

  private static BeeLogger logger = LogUtils.getLogger(ReportInterceptor.class);

  private static final NumberFormat percentFormat = Format.getNumberFormat(PERCENT_PATTERN);
  private static final NumberFormat quantityFormat = Format.getNumberFormat(QUANTITY_PATTERN);
  private static final NumberFormat amountFormat = Format.getNumberFormat(AMOUNT_PATTERN);

  private static final String STORAGE_KEY_SEPARATOR = "-";

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "report-";

  private static final String STYLE_COMMAND_GENERATE = STYLE_PREFIX + "generate";
  private static final String STYLE_COMMAND_CLEAR_FILTER = STYLE_PREFIX + "clear-filter";

  protected static void drillDown(String gridName, String caption, Filter filter) {
    GridOptions gridOptions = GridOptions.forCaptionAndFilter(caption, filter);
    PresenterCallback presenterCallback = ModalGrid.opener(80, CssUnit.PCT, 60, CssUnit.PCT);

    GridFactory.openGrid(gridName, null, gridOptions, presenterCallback);
  }

  protected static Double percent(int x, int y) {
    if (x > 0 && y > 0) {
      return x * 100d / y;
    } else {
      return null;
    }
  }

  protected static String renderAmount(Double x) {
    if (BeeUtils.nonZero(x)) {
      return amountFormat.format(x);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  protected static String renderPercent(Double p) {
    if (BeeUtils.nonZero(p)) {
      return percentFormat.format(p);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  protected static String renderPercent(int x, int y) {
    if (x > 0 && y > 0) {
      return percentFormat.format(x * 100d / y);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  protected static String renderQuantity(int x) {
    if (x > 0) {
      return quantityFormat.format(x);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  private static void widgetIsNot(String name, Class<?> clazz) {
    logger.severe(name, "is not", clazz.getSimpleName());
  }

  private static void widgetNotFound(String name) {
    logger.severe("widget not found", name);
  }

  private ReportParameters initialParameters;

  protected ReportInterceptor() {
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    switch (action) {
      case REFRESH:
        doReport();
        return false;

      case REMOVE_FILTER:
        clearFilter();
        return false;

      case PRINT:
        if (hasReport()) {
          Printer.print(this);
        }
        return false;

      case BOOKMARK:
        bookmark();
        return false;

      case EXPORT:
        export();
        return false;

      default:
        return super.beforeAction(action, presenter);
    }
  }

  @Override
  public Set<Action> getEnabledActions(Set<Action> defaultActions) {
    EnumSet<Action> actions = EnumSet.of(Action.REFRESH, Action.EXPORT, Action.PRINT);

    if (BeeKeeper.getScreen().getUserInterface().hasComponent(Component.REPORTS)) {
      actions.add(Action.BOOKMARK);
    }

    return actions;
  }

  @Override
  public Element getPrintElement() {
    if (hasReport()) {
      return getDataContainer().getWidget(0).getElement();
    } else {
      return null;
    }
  }

  @Override
  public String getSupplierKey() {
    return getReport().getSupplierKey();
  }

  @Override
  public void onLoad(FormView form) {
    if (getInitialParameters() != null) {
      doReport();
    }

    HeaderView header = form.getViewPresenter().getHeader();

    if (header != null && !header.hasCommands()) {
      Button generate = new Button(Localized.dictionary().generateReport());
      generate.addStyleName(STYLE_COMMAND_GENERATE);

      generate.addClickHandler(event -> doReport());
      header.addCommandItem(generate);

      if (!header.hasAction(Action.REMOVE_FILTER)) {
        Button clearFilter = new Button(Localized.dictionary().clearFilter());
        clearFilter.addStyleName(STYLE_COMMAND_CLEAR_FILTER);

        clearFilter.addClickHandler(event -> clearFilter());
        header.addCommandItem(clearFilter);
      }
    }
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    return true;
  }

  public void setInitialParameters(ReportParameters initialParameters) {
    this.initialParameters = initialParameters;
  }

  protected void addBooleanValues(ReportParameters parameters, String... names) {
    for (String name : names) {
      parameters.add(name, getBoolean(name));
    }
  }

  protected void addDateTimeValues(ReportParameters parameters, String... names) {
    for (String name : names) {
      parameters.add(name, getDateTime(name));
    }
  }

  protected void addEditorValues(ReportParameters parameters, List<String> names) {
    for (String name : names) {
      parameters.add(name, getEditorValue(name));
    }
  }

  protected void addEditorValues(ReportParameters parameters, String... names) {
    for (String name : names) {
      parameters.add(name, getEditorValue(name));
    }
  }

  protected void addGroupByIndex(ReportParameters parameters, List<String> names) {
    for (String name : names) {
      Integer index = getSelectedIndex(name);
      if (BeeUtils.isPositive(index)) {
        parameters.add(name, index);
      }
    }
  }

  protected void addGroupByValue(ReportParameters parameters, List<String> names) {
    for (String name : names) {
      Integer index = getSelectedIndex(name);
      if (BeeUtils.isPositive(index)) {
        parameters.add(name, getSelectedItemValue(name));
      }
    }
  }

  protected void addSelectedIndex(ReportParameters parameters, String name, int minValue) {
    Integer index = getSelectedIndex(name);
    if (index != null && index >= minValue) {
      parameters.add(name, index);
    }
  }

  protected void addSelectedValue(ReportParameters parameters, String name, int minValue) {
    Integer index = getSelectedIndex(name);
    if (index != null && index >= minValue) {
      parameters.add(name, getSelectedItemValue(name));
    }
  }

  protected boolean checkRange(DateTime start, DateTime end) {
    if (start != null && end != null && TimeUtils.isMeq(start, end)) {
      getFormView().notifyWarning(Localized.dictionary().invalidRange(),
          Format.renderPeriod(start, end));
      return false;
    } else {
      return true;
    }
  }

  protected boolean checkFilter(String viewName, String input) {
    if (BeeUtils.isEmpty(input)) {
      return true;

    } else if (Data.getDataInfo(viewName).parseFilter(input,
        BeeKeeper.getUser().getUserId()) == null) {

      getFormView().notifyWarning(Localized.dictionary().invalidFilter(), input);
      return false;

    } else {
      return true;
    }
  }

  protected boolean checkRequired(String label, DateTime date) {
    if (date == null) {
      getFormView().notifyWarning(label, Localized.dictionary().valueRequired());
      return false;
    } else {
      return true;
    }
  }

  protected void clearEditor(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof Editor) {
      ((Editor) widget).clearValue();
      SummaryChangeEvent.maybeFire((Editor) widget);
    } else {
      widgetNotFound(name);
    }
  }

  protected void clearEditors(String... names) {
    Arrays.stream(names).forEach(this::clearEditor);
  }

  protected void clearEditors(Collection<String> names) {
    names.forEach(this::clearEditor);
  }

  protected abstract void clearFilter();

  protected abstract void doReport();

  protected void export() {
    logger.warning("export not implemented");
  }

  protected abstract String getBookmarkLabel();

  protected boolean getBoolean(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof HasCheckedness) {
      return ((HasCheckedness) widget).isChecked();
    } else {
      widgetNotFound(name);
      return false;
    }
  }

  protected HasIndexedWidgets getDataContainer() {
    Widget widget = getFormView().getWidgetByName(NAME_DATA_CONTAINER);

    if (widget instanceof HasIndexedWidgets) {
      return (HasIndexedWidgets) widget;
    } else {
      widgetNotFound(NAME_DATA_CONTAINER);
      return null;
    }
  }

  protected DateTime getDateTime(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof InputDateTime) {
      return ((InputDateTime) widget).getDateTime();
    } else {
      widgetNotFound(name);
      return null;
    }
  }

  protected String getEditorValue(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof HasStringValue) {
      return ((HasStringValue) widget).getValue();
    } else {
      widgetNotFound(name);
      return null;
    }
  }

  protected List<String> getGroupBy(List<String> names, List<String> values) {
    List<String> groupBy = StringList.uniqueCaseInsensitive();

    for (String name : names) {
      Integer index = getSelectedIndex(name);

      if (BeeUtils.isPositive(index)) {
        groupBy.add(BeeUtils.getQuietly(values, index - 1));
      }
    }

    return groupBy;
  }

  protected String getGroupByLabel(String name) {
    if (BeeUtils.isPositive(getSelectedIndex(name))) {
      return getEditorValue(name);
    } else {
      return null;
    }
  }

  protected abstract Report getReport();

  protected String getReportCaption() {
    return getFormView().getCaption();
  }

  protected abstract ReportParameters getReportParameters();

  protected Integer getSelectedIndex(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof ListBox) {
      return ((ListBox) widget).getSelectedIndex();
    } else {
      widgetIsNot(name, ListBox.class);
      return null;
    }
  }

  protected String getSelectedItemText(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof ListBox) {
      int index = ((ListBox) widget).getSelectedIndex();
      return (index >= 0) ? ((ListBox) widget).getItemText(index) : null;
    } else {
      widgetIsNot(name, ListBox.class);
      return null;
    }
  }

  protected String getSelectedItemValue(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof ListBox) {
      int index = ((ListBox) widget).getSelectedIndex();
      return (index >= 0) ? ((ListBox) widget).getValue(index) : null;
    } else {
      widgetIsNot(name, ListBox.class);
      return null;
    }
  }

  protected String getSelectorLabel(String name) {
    Widget widget = getFormView().getWidgetByName(name);

    if (widget instanceof MultiSelector) {
      MultiSelector selector = (MultiSelector) widget;
      List<Long> ids = DataUtils.parseIdList(selector.getValue());

      if (ids.isEmpty()) {
        return null;
      } else {
        List<String> labels = new ArrayList<>();
        for (Long id : ids) {
          labels.add(selector.getRowLabel(id));
        }

        return BeeUtils.joinItems(labels);
      }

    } else if (widget instanceof UnboundSelector) {
      return ((UnboundSelector) widget).getRenderedValue();

    } else {
      widgetNotFound(name);
      return null;
    }
  }

  protected static void loadBoolean(ReportParameters parameters, String name, FormView form) {
    Boolean value = parameters.getBoolean(name);
    if (BeeUtils.isTrue(value)) {
      Widget widget = form.getWidgetByName(name);

      if (widget instanceof HasCheckedness) {
        ((HasCheckedness) widget).setChecked(value);
        SummaryChangeEvent.maybeSummarize(widget);
      } else {
        widgetIsNot(name, HasCheckedness.class);
      }
    }
  }

  protected static void loadDateTime(ReportParameters parameters, String name, FormView form) {
    DateTime dateTime = parameters.getDateTime(name);
    if (dateTime != null) {
      Widget widget = form.getWidgetByName(name);

      if (widget instanceof InputDateTime) {
        ((InputDateTime) widget).setDateTime(dateTime);
        SummaryChangeEvent.maybeSummarize(widget);
      } else {
        widgetIsNot(name, InputDateTime.class);
      }
    }
  }

  protected static void loadEditor(ReportParameters parameters, String name, FormView form) {
    String value = parameters.get(name);
    if (!BeeUtils.isEmpty(value)) {
      Widget widget = form.getWidgetByName(name);

      if (widget instanceof HasStringValue) {
        ((HasStringValue) widget).setValue(value);
        SummaryChangeEvent.maybeSummarize(widget);
      } else {
        widgetIsNot(name, HasStringValue.class);
      }
    }
  }

  protected static void loadListByIndex(ReportParameters parameters, String name, FormView form) {
    Integer index = parameters.getInteger(name);
    if (index != null) {
      Widget widget = form.getWidgetByName(name);

      if (widget instanceof ListBox) {
        ((ListBox) widget).setSelectedIndex(index);
        SummaryChangeEvent.maybeSummarize(widget);
      } else {
        widgetIsNot(name, ListBox.class);
      }
    }
  }

  protected static void loadListByValue(ReportParameters parameters, String name, FormView form) {
    String value = parameters.getText(name);
    if (!BeeUtils.isEmpty(value)) {
      Widget widget = form.getWidgetByName(name);

      if (widget instanceof ListBox) {
        ((ListBox) widget).setValue(value);
        SummaryChangeEvent.maybeSummarize(widget);
      } else {
        widgetIsNot(name, ListBox.class);
      }
    }
  }

  protected static void loadGroupByIndex(ReportParameters parameters, Collection<String> names,
      FormView form) {

    for (String name : names) {
      loadListByIndex(parameters, name, form);
    }
  }

  protected static void loadGroupByValue(ReportParameters parameters, Collection<String> names,
      FormView form) {

    for (String name : names) {
      loadListByValue(parameters, name, form);
    }
  }

  protected static void loadId(ReportParameters parameters, String name, FormView form) {
    Long id = parameters.getLong(name);
    if (DataUtils.isId(id)) {
      Widget widget = form.getWidgetByName(name);

      if (widget instanceof UnboundSelector) {
        ((UnboundSelector) widget).setValue(id, false);
        SummaryChangeEvent.maybeSummarize(widget);
      } else {
        widgetIsNot(name, UnboundSelector.class);
      }
    }
  }

  protected static void loadIds(ReportParameters parameters, String name, FormView form) {
    String ids = parameters.get(name);
    if (!BeeUtils.isEmpty(ids)) {
      Widget widget = form.getWidgetByName(name);

      if (widget instanceof MultiSelector) {
        ((MultiSelector) widget).setIds(ids);
      } else {
        widgetIsNot(name, MultiSelector.class);
      }
    }
  }

  protected static void loadMulti(ReportParameters parameters, Collection<String> names,
      FormView form) {

    for (String name : names) {
      loadIds(parameters, name, form);
    }
  }

  protected static void loadText(ReportParameters parameters, String name, FormView form) {
    String text = parameters.getText(name);
    if (!BeeUtils.isEmpty(text)) {
      Widget widget = form.getWidgetByName(name);

      if (widget instanceof TextBox) {
        ((TextBox) widget).setText(text);
        SummaryChangeEvent.maybeSummarize(widget);
      } else {
        widgetIsNot(name, TextBox.class);
      }
    }
  }

  protected ReportParameters readParameters() {
    if (getInitialParameters() != null) {
      return getInitialParameters();
    }

    Long user = BeeKeeper.getUser().getUserId();
    if (!DataUtils.isId(user)) {
      return null;
    }

    String prefix = getReport().getReportName() + STORAGE_KEY_SEPARATOR
        + BeeUtils.toString(user) + STORAGE_KEY_SEPARATOR;

    Map<String, String> map = BeeKeeper.getStorage().getSubMap(prefix);
    if (BeeUtils.isEmpty(map)) {
      return null;
    } else {
      return new ReportParameters(map);
    }
  }

  protected void storeBooleanValues(String... names) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      for (String name : names) {
        BeeKeeper.getStorage().set(storageKey(user, name), getBoolean(name));
      }
    }
  }

  protected void storeDateTimeValues(String... names) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      for (String name : names) {
        BeeKeeper.getStorage().set(storageKey(user, name), getDateTime(name));
      }
    }
  }

  protected void storeEditorValues(List<String> names) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      for (String name : names) {
        BeeKeeper.getStorage().set(storageKey(user, name), getEditorValue(name));
      }
    }
  }

  protected void storeEditorValues(String... names) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      for (String name : names) {
        BeeKeeper.getStorage().set(storageKey(user, name), getEditorValue(name));
      }
    }
  }

  protected void storeGroupByIndex(List<String> names) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      for (String name : names) {
        Integer index = getSelectedIndex(name);
        if (!BeeUtils.isPositive(index)) {
          index = null;
        }

        BeeKeeper.getStorage().set(storageKey(user, name), index);
      }
    }
  }

  protected void storeGroupByValue(List<String> names) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      for (String name : names) {
        Integer index = getSelectedIndex(name);
        String value = BeeUtils.isPositive(index) ? getSelectedItemValue(name) : null;

        BeeKeeper.getStorage().set(storageKey(user, name), value);
      }
    }
  }

  protected void storeSelectedIndex(String name, int minValue) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      Integer index = getSelectedIndex(name);
      if (index != null && index < minValue) {
        index = null;
      }

      BeeKeeper.getStorage().set(storageKey(user, name), index);
    }
  }

  protected void storeSelectedValue(String name, int minValue) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      Integer index = getSelectedIndex(name);
      String value = (index != null && index >= minValue) ? getSelectedItemValue(name) : null;

      BeeKeeper.getStorage().set(storageKey(user, name), value);
    }
  }

  protected void storeValue(String name, String value) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      BeeKeeper.getStorage().set(storageKey(user, name), value);
    }
  }

  protected void storeValue(String name, Integer value) {
    Long user = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(user)) {
      BeeKeeper.getStorage().set(storageKey(user, name), value);
    }
  }

  protected abstract boolean validateParameters(ReportParameters parameters);

  private void bookmark() {
    ReportParameters parameters = getReportParameters();

    if (parameters != null && validateParameters(parameters)) {
      String caption = BeeUtils.notEmpty(getBookmarkLabel(), getReportCaption());
      Global.getReportSettings().bookmark(getReport(), caption, parameters);
    }
  }

  private ReportParameters getInitialParameters() {
    return initialParameters;
  }

  private boolean hasReport() {
    HasIndexedWidgets container = getDataContainer();
    return container != null && !container.isEmpty();
  }

  private String storageKey(long user, String name) {
    return BeeUtils.join(STORAGE_KEY_SEPARATOR, getReport().getReportName(), user, name);
  }
}
