package com.butent.bee.client.view.form.interceptor;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.dialog.ModalGrid;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.output.ReportParameters;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasStringValue;
import com.butent.bee.shared.ui.UserInterface.Component;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Map;

public abstract class ReportInterceptor extends AbstractFormInterceptor implements Printable {

  protected static final String PERCENT_PATTERN = "0.0";
  protected static final String QUANTITY_PATTERN = "#,###";
  protected static final String AMOUNT_PATTERN = "#,##0.00";

  private static BeeLogger logger = LogUtils.getLogger(ReportInterceptor.class);

  private static final String NAME_DATA_CONTAINER = "DataContainer";

  private static final NumberFormat percentFormat = Format.getNumberFormat(PERCENT_PATTERN);
  private static final NumberFormat quantityFormat = Format.getNumberFormat(QUANTITY_PATTERN);
  private static final NumberFormat amountFormat = Format.getNumberFormat(AMOUNT_PATTERN);

  private static final String STORAGE_KEY_SEPARATOR = "-";

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
      case FILTER:
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
  public String getCaption() {
    return getFormView().getCaption();
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

    if (!BeeKeeper.getScreen().getUserInterface().hasComponent(Component.REPORTS)) {
      HeaderView header = form.getViewPresenter().getHeader();

      if (header != null && header.hasAction(Action.BOOKMARK)) {
        header.showAction(Action.BOOKMARK, false);
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

  protected ReportParameters addDateTimeValues(ReportParameters parameters, String... names) {
    for (String name : names) {
      parameters.add(name, getDateTime(name));
    }
    return parameters;
  }

  protected ReportParameters addEditorValues(ReportParameters parameters, List<String> names) {
    for (String name : names) {
      parameters.add(name, getEditorValue(name));
    }
    return parameters;
  }

  protected ReportParameters addEditorValues(ReportParameters parameters, String... names) {
    for (String name : names) {
      parameters.add(name, getEditorValue(name));
    }
    return parameters;
  }

  protected boolean checkRange(DateTime start, DateTime end) {
    if (start != null && end != null && TimeUtils.isMore(start, end)) {
      getFormView().notifyWarning(Localized.getConstants().invalidRange(),
          TimeUtils.renderPeriod(start, end));
      return false;
    } else {
      return true;
    }
  }

  protected void clearEditor(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof Editor) {
      ((Editor) widget).clearValue();
    } else {
      widgetNotFound(name);
    }
  }

  protected abstract void clearFilter();

  protected abstract void doReport();

  protected void export() {
    logger.warning("export not implemented");
  }

  protected abstract String getBookmarkLabel();

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

  protected String getFilterLabel(String name) {
    Widget widget = getFormView().getWidgetByName(name);

    if (widget instanceof MultiSelector) {
      MultiSelector selector = (MultiSelector) widget;
      List<Long> ids = DataUtils.parseIdList(selector.getValue());

      if (ids.isEmpty()) {
        return null;
      } else {
        List<String> labels = Lists.newArrayList();
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

  protected abstract Report getReport();

  protected abstract ReportParameters getReportParameters();

  protected Integer getSelectedIndex(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof ListBox) {
      return ((ListBox) widget).getSelectedIndex();
    } else {
      widgetNotFound(name);
      return null;
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
      String caption = BeeUtils.notEmpty(getBookmarkLabel(), getCaption());
      Global.getReportSettings().bookmark(getReport(), caption, getReportParameters());
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
