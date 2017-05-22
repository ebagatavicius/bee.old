package com.butent.bee.client.modules.classifiers;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.output.Report;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasStringValue;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CompanyUsageReport extends ReportInterceptor {

  private static final String NAME_RELATION = "Relation";
  private static final String NAME_OPERATOR = "Operator";
  private static final String NAME_COUNT = "Count";

  private static final String NAME_START_DATE = "StartDate";
  private static final String NAME_END_DATE = "EndDate";

  private static final List<String> RELATIONS = new ArrayList<>();
  private static final List<String> SELECTOR_NAMES = new ArrayList<>();

  private static void initRelations() {
    if (!RELATIONS.isEmpty()) {
      RELATIONS.clear();
    }

    List<String> viewNames = Lists.newArrayList(
        TradeConstants.VIEW_SALES,
        TradeConstants.VIEW_PURCHASES,
        DocumentConstants.VIEW_DOCUMENTS,
        TaskConstants.VIEW_TASKS,
        TaskConstants.VIEW_RECURRING_TASKS,
        CalendarConstants.VIEW_APPOINTMENTS,
        ServiceConstants.VIEW_SERVICE_OBJECTS,
        TransportConstants.VIEW_ORDERS,
        TransportConstants.VIEW_VEHICLES,
        VIEW_COMPANY_CONTACTS,
        VIEW_COMPANY_DEPARTMENTS,
        VIEW_COMPANY_PERSONS,
        VIEW_COMPANY_USERS,
        VIEW_COMPANY_RELATION_TYPE_STORE,
        VIEW_COMPANY_ACTIVITY_STORE);

    for (String viewName : viewNames) {
      if (isDataVisible(viewName)) {
        RELATIONS.add(viewName);
      }
    }
  }

  private static void initSelectorNames() {
    if (!SELECTOR_NAMES.isEmpty()) {
      SELECTOR_NAMES.clear();
    }

    if (isDataVisible(VIEW_COMPANY_TYPES)) {
      SELECTOR_NAMES.add(COL_COMPANY_TYPE);
    }
    if (isDataVisible(VIEW_COMPANY_GROUPS)) {
      SELECTOR_NAMES.add(COL_COMPANY_GROUP);
    }
    if (isDataVisible(VIEW_COMPANY_PRIORITIES)) {
      SELECTOR_NAMES.add(COL_COMPANY_PRIORITY);
    }

    if (isDataVisible(VIEW_COMPANY_RELATION_TYPES)
        && isDataVisible(VIEW_COMPANY_RELATION_TYPE_STORE)) {
      SELECTOR_NAMES.add(COL_RELATION_TYPE);
    }

    if (isDataVisible(VIEW_RELATION_TYPE_STATES)) {
      SELECTOR_NAMES.add(COL_COMPANY_RELATION_TYPE_STATE);
    }
    if (isDataVisible(VIEW_FINANCIAL_STATES)) {
      SELECTOR_NAMES.add(COL_COMPANY_FINANCIAL_STATE);
    }
    if (isDataVisible(VIEW_COMPANY_SIZES)) {
      SELECTOR_NAMES.add(COL_COMPANY_SIZE);
    }
    if (isDataVisible(VIEW_INFORMATION_SOURCES)) {
      SELECTOR_NAMES.add(COL_COMPANY_INFORMATION_SOURCE);
    }

    if (isDataVisible(VIEW_COMPANY_ACTIVITIES) && isDataVisible(VIEW_COMPANY_ACTIVITY_STORE)) {
      SELECTOR_NAMES.add(COL_ACTIVITY);
    }

    if (isDataVisible(VIEW_COUNTRIES)) {
      SELECTOR_NAMES.add(COL_COUNTRY);
    }
    if (isDataVisible(VIEW_CITIES)) {
      SELECTOR_NAMES.add(COL_CITY);
    }
  }

  private static boolean isDataVisible(String viewName) {
    return BeeKeeper.getUser().isDataVisible(viewName);
  }

  public CompanyUsageReport() {
    if (RELATIONS.isEmpty()) {
      initRelations();
    }
    if (SELECTOR_NAMES.isEmpty()) {
      initSelectorNames();
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (NAME_RELATION.equals(name) && widget instanceof ListBox) {
      for (String viewName : RELATIONS) {
        ((ListBox) widget).addItem(Data.getViewCaption(viewName));
      }
    }
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action == Action.PRINT) {
      return true;
    } else {
      return super.beforeAction(action, presenter);
    }
  }

  @Override
  public Set<Action> getEnabledActions(Set<Action> defaultActions) {
    Set<Action> actions = super.getEnabledActions(defaultActions);
    actions.remove(Action.EXPORT);
    return actions;
  }

  @Override
  public FormInterceptor getInstance() {
    return new CompanyUsageReport();
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      Widget widget = form.getWidgetByName(NAME_RELATION);
      Integer index = parameters.getInteger(NAME_RELATION);
      if (widget instanceof ListBox && BeeUtils.isPositive(index)
          && BeeUtils.isIndex(RELATIONS, index - 1)) {
        ((ListBox) widget).setSelectedIndex(index);
      }

      widget = form.getWidgetByName(NAME_OPERATOR);
      String op = parameters.get(NAME_OPERATOR);
      if (widget instanceof ListBox && !BeeUtils.isEmpty(op)) {
        index = ((ListBox) widget).getIndex(op);
        if (!BeeConst.isUndef(index)) {
          ((ListBox) widget).setSelectedIndex(index);
        }
      }

      widget = form.getWidgetByName(NAME_COUNT);
      String cnt = parameters.get(NAME_COUNT);
      if (widget instanceof HasStringValue && BeeUtils.isPositiveInt(cnt)) {
        ((HasStringValue) widget).setValue(cnt);
      }

      widget = form.getWidgetByName(NAME_START_DATE);
      DateTime dateTime = parameters.getDateTime(NAME_START_DATE);
      if (widget instanceof InputDateTime && dateTime != null) {
        ((InputDateTime) widget).setDateTime(dateTime);
      }

      widget = form.getWidgetByName(NAME_END_DATE);
      dateTime = parameters.getDateTime(NAME_END_DATE);
      if (widget instanceof InputDateTime && dateTime != null) {
        ((InputDateTime) widget).setDateTime(dateTime);
      }

      for (String name : SELECTOR_NAMES) {
        widget = form.getWidgetByName(name);
        String idList = parameters.get(name);
        if (widget instanceof MultiSelector && !BeeUtils.isEmpty(idList)) {
          ((MultiSelector) widget).setIds(idList);
        }
      }
    }

    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeEditorValues(NAME_RELATION, NAME_OPERATOR, NAME_COUNT);
    storeDateTimeValues(NAME_START_DATE, NAME_END_DATE);
    storeEditorValues(SELECTOR_NAMES);
  }

  @Override
  protected void clearFilter() {
    clearEditor(NAME_START_DATE);
    clearEditor(NAME_END_DATE);

    for (String name : SELECTOR_NAMES) {
      clearEditor(name);
    }
  }

  @Override
  protected void doReport() {
    List<String> args = new ArrayList<>();
    final List<String> labels = new ArrayList<>();

    String relationIndex = getEditorValue(NAME_RELATION);
    if (BeeUtils.isPositiveInt(relationIndex)) {
      String relation = RELATIONS.get(BeeUtils.toInt(relationIndex) - 1);
      args.add(Service.VAR_VIEW_NAME);
      args.add(relation);

      String operator = getEditorValue(NAME_OPERATOR);
      if (!BeeUtils.isEmpty(operator)) {
        args.add(Service.VAR_OPERATOR);
        args.add(operator);
      }

      String count = getEditorValue(NAME_COUNT);
      if (BeeUtils.isPositiveInt(count)) {
        args.add(Service.VAR_VALUE);
        args.add(count);
      }

      labels.add(BeeUtils.joinWords(Data.getViewCaption(relation), operator, count));

      DateTime start = getDateTime(NAME_START_DATE);
      DateTime end = getDateTime(NAME_END_DATE);

      if (!checkRange(start, end)) {
        return;
      }

      if (start != null) {
        args.add(Service.VAR_FROM);
        args.add(start.serialize());
      }
      if (end != null) {
        args.add(Service.VAR_TO);
        args.add(end.serialize());
      }

      if (start != null || end != null) {
        labels.add(Format.renderPeriod(start, end));
      }
    }

    for (String name : SELECTOR_NAMES) {
      String value = getEditorValue(name);

      if (!BeeUtils.isEmpty(value)) {
        args.add(name);
        args.add(value);

        String label = getSelectorLabel(name);
        if (!BeeUtils.isEmpty(label)) {
          labels.add(label);
        }
      }
    }

    if (args.isEmpty()) {
      getFormView().notifyWarning(Localized.dictionary().specifyCondition());
      return;
    }

    final Filter filter = Filter.custom(FILTER_COMPANY_USAGE, args);

    Queries.getRowCount(VIEW_COMPANIES, filter, new Queries.IntCallback() {
      @Override
      public void onSuccess(Integer result) {
        if (BeeUtils.isPositive(result)) {
          openGrid(filter, labels);
        } else {
          getFormView().notifyWarning(Localized.dictionary().nothingFound());
        }
      }
    });
  }

  @Override
  protected String getBookmarkLabel() {
    List<String> labels = Lists.newArrayList(getReportCaption());

    String relationIndex = getEditorValue(NAME_RELATION);

    if (BeeUtils.isPositiveInt(relationIndex)) {
      String label = Data.getViewCaption(RELATIONS.get(BeeUtils.toInt(relationIndex) - 1));
      labels.add(BeeUtils.joinWords(label, getEditorValue(NAME_OPERATOR),
          getEditorValue(NAME_COUNT)));

      labels.add(Format.renderPeriod(getDateTime(NAME_START_DATE), getDateTime(NAME_END_DATE)));
    }

    List<String> selectorLabels = new ArrayList<>();

    for (String name : SELECTOR_NAMES) {
      String label = getSelectorLabel(name);
      if (!BeeUtils.isEmpty(label)) {
        selectorLabels.add(label);
      }
    }

    if (!selectorLabels.isEmpty()) {
      labels.add(BeeUtils.joinItems(selectorLabels));
    }

    return BeeUtils.joinWords(labels);
  }

  @Override
  protected Report getReport() {
    return Report.COMPANY_USAGE;
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addEditorValues(parameters, NAME_RELATION, NAME_OPERATOR, NAME_COUNT);
    addDateTimeValues(parameters, NAME_START_DATE, NAME_END_DATE);
    addEditorValues(parameters, SELECTOR_NAMES);

    return parameters;
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    DateTime start = parameters.getDateTime(NAME_START_DATE);
    DateTime end = parameters.getDateTime(NAME_END_DATE);

    if (!checkRange(start, end)) {
      return false;
    }

    for (String name : SELECTOR_NAMES) {
      if (parameters.containsKey(name)) {
        return true;
      }
    }

    Integer relationIndex = parameters.getInteger(NAME_RELATION);

    if (BeeUtils.isPositive(relationIndex) && parameters.containsKey(NAME_OPERATOR)) {
      return true;
    } else {
      getFormView().notifyWarning(Localized.dictionary().specifyCondition());
      return false;
    }
  }

  private void openGrid(Filter filter, final List<String> labels) {
    HasIndexedWidgets container = getDataContainer();
    if (!container.isEmpty()) {
      container.clear();
    }

    GridFactory.openGrid("CompanyUsageReport", null, GridOptions.forFilter(filter),
        new PresenterCallback() {
          @Override
          public void onCreate(Presenter presenter) {
            Widget widget = presenter.getMainView().asWidget();
            StyleUtils.occupy(widget);

            getDataContainer().add(widget);

            if (!BeeUtils.isEmpty(labels) && presenter instanceof GridPresenter) {
              ((GridPresenter) presenter).setParentLabels(labels);
            }
          }
        });
  }
}
