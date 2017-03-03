package com.butent.bee.client.modules.tasks;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.builder.elements.Span;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.*;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.time.CronExpression;
import com.butent.bee.shared.time.CronExpression.Field;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.ScheduleDateMode;
import com.butent.bee.shared.time.ScheduleDateRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.WorkdayTransition;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

class RecurringTaskHandler extends AbstractFormInterceptor implements CellValidateEvent.Handler {

  private enum Cron {
    DAY_OF_MONTH(Field.DAY_OF_MONTH, COL_RT_DAY_OF_MONTH, 1) {
      @Override
      String getLabel(int value) {
        return BeeUtils.toString(value);
      }
    },
    MONTH(Field.MONTH, COL_RT_MONTH, 1) {
      @Override
      String getLabel(int value) {
        return BeeUtils.proper(Format.renderMonthFullStandalone(value));
      }
    },
    DAY_OF_WEEK(Field.DAY_OF_WEEK, COL_RT_DAY_OF_WEEK, 1) {
      @Override
      String getLabel(int value) {
        return BeeUtils.proper(Format.renderDayOfWeek(value));
      }
    };

    private static Cron getBySource(String source) {
      for (Cron cron : Cron.values()) {
        if (cron.source.equals(source)) {
          return cron;
        }
      }

      return null;
    }

    private final Field field;
    private final String source;
    private final int shift;

    Cron(Field field, String source, int shift) {
      this.field = field;
      this.source = source;
      this.shift = shift;
    }

    abstract String getLabel(int value);
  }

  private static final class DayOfMonth {
    private final int dom;
    private final int dow;

    private final boolean scheduled;

    private final List<String> styleNames = new ArrayList<>();

    private DayOfMonth(JustDate date, boolean scheduled) {
      this.dom = date.getDom();
      this.dow = date.getDow();

      this.scheduled = scheduled;
    }

    private void addStyleName(String styleName) {
      styleNames.add(styleName);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(RecurringTaskHandler.class);

  private static final String STYLE_PREFIX = CRM_STYLE_PREFIX + "rt-";

  private static final String STYLE_SCHEDULE_PANEL = STYLE_PREFIX + "schedule-panel";
  private static final String STYLE_CALENDAR_PANEL = STYLE_PREFIX + "calendar-panel";
  private static final String STYLE_MONTH_PANEL = STYLE_PREFIX + "month-panel";
  private static final String STYLE_MONTH_LABEL = STYLE_PREFIX + "month-label";
  private static final String STYLE_MONTH_TABLE = STYLE_PREFIX + "month-table";
  private static final String STYLE_WEEKDAY_CELL = STYLE_PREFIX + "weekday-cell";

  private static final String STYLE_DAY_CELL = STYLE_PREFIX + "day-cell";
  private static final String STYLE_DAY_HAS_TASKS = STYLE_PREFIX + "day-has-tasks";
  private static final String STYLE_DAY_SCHEDULED = STYLE_PREFIX + "day-scheduled";
  private static final String STYLE_INFERTILE = STYLE_PREFIX + "infertile";

  private static final String STYLE_HAS_TASKS = STYLE_PREFIX + "has-tasks";
  private static final String STYLE_SCHEDULED = STYLE_PREFIX + "scheduled";

  private static final String STYLE_TODAY = STYLE_PREFIX + "today";
  private static final String STYLE_HOLIDAY = STYLE_PREFIX + "holiday";
  private static final String STYLE_WEEKEND = STYLE_PREFIX + "weekend";

  private static final String STYLE_INFO_PANEL = STYLE_PREFIX + "info-panel";
  private static final String STYLE_INFO_LABEL = STYLE_PREFIX + "info-label";

  private static final String STYLE_VALUE_TOGGLE = STYLE_PREFIX + "value-toggle";

  private static final String STYLE_CRON_EXAMPLES = STYLE_PREFIX + "cron-examples";

  private static final String STYLE_FAILURE = STYLE_PREFIX + "failure";

  private static final String STYLE_OFFSPRING_PREFIX = STYLE_PREFIX + "offspring-";
  private static final String STYLE_OFFSPRING_DIALOG = STYLE_OFFSPRING_PREFIX + "dialog";
  private static final String STYLE_OFFSPRING_PANEL = STYLE_OFFSPRING_PREFIX + "panel";
  private static final String STYLE_OFFSPRING_TABLE = STYLE_OFFSPRING_PREFIX + "table";

  private static final String STYLE_OFFSPRING_ACTUAL = STYLE_OFFSPRING_PREFIX + "actual";
  private static final String STYLE_OFFSPRING_POTENTIAL = STYLE_OFFSPRING_PREFIX + "potential";

  private static final String STYLE_OFFSPRING_FIRST_NAME = STYLE_OFFSPRING_PREFIX + "firstName";
  private static final String STYLE_OFFSPRING_LAST_NAME = STYLE_OFFSPRING_PREFIX + "lastName";
  private static final String STYLE_OFFSPRING_STATUS = STYLE_OFFSPRING_PREFIX + "status";

  private static final String STYLE_OFFSPRING_OPEN = STYLE_OFFSPRING_PREFIX + "open";
  private static final String STYLE_OFFSPRING_CREATE = STYLE_OFFSPRING_PREFIX + "create";
  private static final String STYLE_OFFSPRING_DELETE = STYLE_OFFSPRING_PREFIX + "delete";

  private static final int COL_OFFSPRING_FIRST_NAME = 0;
  private static final int COL_OFFSPRING_LAST_NAME = 1;
  private static final int COL_OFFSPRING_STATUS = 2;
  private static final int COL_OFFSPRING_OPEN = 3;
  private static final int COL_OFFSPRING_CREATE = 4;
  private static final int COL_OFFSPRING_DELETE = 4;

  private static void initDayOfMonthHelp(HasClickHandlers widget, final Element target) {
    widget.addClickHandler(event -> showExamples(target, CronExpression.getDayOfMonthExamples()));
  }

  private static void initDayOfWeekHelp(HasClickHandlers widget, final Element target) {
    widget.addClickHandler(event -> showExamples(target, CronExpression.getDayOfWeekExamples()));
  }

  private static void initMonthHelp(HasClickHandlers widget, final Element target) {
    widget.addClickHandler(event -> showExamples(target, CronExpression.getMonthExamples()));
  }

  private static void setProjectScheduleTimeLimit(FormView form, IsRow row) {
    Widget wScheduleStart = form.getWidgetBySource(COL_RT_SCHEDULE_FROM);
    Widget wScheduleUntil = form.getWidgetBySource(COL_RT_SCHEDULE_UNTIL);
    Widget wScheduleUntilLabel = form.getWidgetByName(COL_RT_SCHEDULE_UNTIL);

    HasDateValue projectStart =
        row.getDate(form.getDataIndex(ProjectConstants.ALS_PROJECT_START_DATE));
    HasDateValue projectEnd = row.getDate(form.getDataIndex(ProjectConstants.ALS_PROJECT_END_DATE));

    if (wScheduleStart instanceof InputDate && projectStart != null) {
      InputDate start = (InputDate) wScheduleStart;
      start.setMinDate(projectStart);
      start.setMaxDate(projectEnd);
    }

    if (wScheduleUntil instanceof InputDate && projectEnd != null) {
      InputDate end = (InputDate) wScheduleUntil;
      end.setMinDate(projectStart);
      end.setMaxDate(projectEnd);
      end.setNullable(false);
    }

    if (wScheduleUntilLabel != null && projectEnd != null) {
      wScheduleUntilLabel.setStyleName(StyleUtils.NAME_REQUIRED);
    }

  }

  private static void showExamples(Element target, List<Property> examples) {
    HtmlTable table = new HtmlTable(STYLE_CRON_EXAMPLES);

    int row = 0;
    for (Property property : examples) {
      table.setText(row, 0, property.getName());
      table.setText(row, 1, property.getValue());

      row++;
    }

    Global.showModalWidget(null, table, target);
  }

  private final Multimap<Integer, BeeRow> offspring = ArrayListMultimap.create();

  private BeeRowSet executors;
  private final Map<Integer, String> dayElementIds = new HashMap<>();

  private final EnumMap<Cron, Flow> toggleContainers = new EnumMap<>(Cron.class);
  private final EnumMap<Cron, HasHtml> errorLabels = new EnumMap<>(Cron.class);

  RecurringTaskHandler() {
  }

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (BeeUtils.inList(editableWidget.getColumnId(),
        COL_RT_DAY_OF_MONTH, COL_RT_MONTH, COL_RT_DAY_OF_WEEK)) {
      editableWidget.addCellValidationHandler(this);
    }

    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (!BeeUtils.isEmpty(name)) {
      switch (name) {
        case "DomValues":
          if (widget instanceof Flow) {
            initValueContainer(Cron.DAY_OF_MONTH, (Flow) widget);
          }
          break;
        case "MonthValues":
          if (widget instanceof Flow) {
            initValueContainer(Cron.MONTH, (Flow) widget);
          }
          break;
        case "DowValues":
          if (widget instanceof Flow) {
            initValueContainer(Cron.DAY_OF_WEEK, (Flow) widget);
          }
          break;

        case "DomHelp":
          if (widget instanceof HasClickHandlers) {
            initDayOfMonthHelp((HasClickHandlers) widget, widget.getElement());
          }
          break;
        case "MonthHelp":
          if (widget instanceof HasClickHandlers) {
            initMonthHelp((HasClickHandlers) widget, widget.getElement());
          }
          break;
        case "DowHelp":
          if (widget instanceof HasClickHandlers) {
            initDayOfWeekHelp((HasClickHandlers) widget, widget.getElement());
          }
          break;

        case "DomError":
          if (widget instanceof HasHtml) {
            errorLabels.put(Cron.DAY_OF_MONTH, (HasHtml) widget);
          }
          break;
        case "MonthError":
          if (widget instanceof HasHtml) {
            errorLabels.put(Cron.MONTH, (HasHtml) widget);
          }
          break;
        case "DowError":
          if (widget instanceof HasHtml) {
            errorLabels.put(Cron.DAY_OF_WEEK, (HasHtml) widget);
          }
          break;
      }
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    HeaderView header = getHeaderView();

    if (header != null && !header.hasCommands()) {
      FaLabel schedule = new FaLabel(FontAwesome.CALENDAR);
      schedule.addStyleName(STYLE_PREFIX + "actionSchedule");
      schedule.setTitle(Localized.dictionary().crmRTActionSchedule());

      schedule.addClickHandler(event -> {
        IsRow activeRow = getActiveRow();
        if (!validateBounds(activeRow)) {
          return;
        }

        clearOffspring();
        setExecutors(null);

        if (DataUtils.hasId(activeRow)) {
          int updateSize;

          if (getFormView().isRowEnabled(activeRow)) {
            updateSize = Queries.update(getViewName(), getFormView().getDataColumns(),
                getFormView().getOldRow(), activeRow, getFormView().getChildrenForUpdate(),
                new RowCallback() {
                  @Override
                  public void onSuccess(BeeRow result) {
                    RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), result);
                    showSchedule(result.getId());
                  }
                });

          } else {
            updateSize = 0;
          }

          if (updateSize == 0) {
            showSchedule(activeRow.getId());
          }

        } else {
          List<ScheduleDateRange> scheduleDateRanges = new ArrayList<>();
          timeCube(scheduleDateRanges, false);
        }
      });

      header.addCommandItem(schedule);
    }

    setProjectScheduleTimeLimit(form, row);

    for (Cron cron : Cron.values()) {
      refreshValues(row.getId(), cron, row.getString(form.getDataIndex(cron.source)));
    }

    enableToggles(form.isRowEnabled(row));
  }

  @Override
  public FormInterceptor getInstance() {
    return new RecurringTaskHandler();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return row != null && BeeKeeper.getUser().is(row.getLong(getDataIndex(COL_OWNER)));
  }

  @Override
  public Boolean validateCell(CellValidateEvent event) {
    if (event.isCellValidation() && event.isPreValidation()) {
      String source = event.getColumnId();
      if (BeeUtils.isEmpty(source)) {
        return true;
      }

      Cron cron = Cron.getBySource(source);
      if (cron == null) {
        return true;
      }

      if (BeeUtils.isEmpty(event.getNewValue())) {
        clearValues(cron);
        clearErrors(cron);

      } else {
        refreshValues(event.getRowId(), cron, event.getNewValue());
      }
    }

    return true;
  }

  private void addOffspring(BeeRowSet rowSet) {
    int dateIndex = rowSet.getColumnIndex(COL_START_TIME);

    for (BeeRow row : rowSet.getRows()) {
      JustDate start = JustDate.get(row.getDateTime(dateIndex));
      if (start != null) {
        offspring.put(start.getDays(), row);
      }
    }
  }

  private void clearErrors(Cron cron) {
    HasHtml errorLabel = errorLabels.get(cron);
    if (errorLabel != null) {
      errorLabel.setText(BeeConst.STRING_EMPTY);
    }
  }

  private void clearOffspring() {
    if (!offspring.isEmpty()) {
      offspring.clear();
    }
  }

  private void clearValues(Cron cron) {
    Flow toggleContainer = toggleContainers.get(cron);
    if (toggleContainer != null) {
      for (int i = 0; i < toggleContainer.getWidgetCount(); i++) {
        setValue(cron, i, false);
      }
    }
  }

  private void editOffspring(Element target, final int dayNumber) {
    Flow panel = new Flow(STYLE_OFFSPRING_PANEL);

    final HtmlTable table = new HtmlTable(STYLE_OFFSPRING_TABLE);
    renderOffspring(table, dayNumber);

    panel.add(table);

    String caption = BeeUtils.joinWords(Localized.dictionary().crmTasks(),
        Format.renderDate(new JustDate(dayNumber)));

    DialogBox dialog = DialogBox.create(caption, STYLE_OFFSPRING_DIALOG);
    dialog.setWidget(panel);

    dialog.setAnimationEnabled(true);
    dialog.setHideOnEscape(true);

    RowUpdateEvent.Handler updateHandler = event -> {
      if (event.hasView(VIEW_TASKS) && table.isAttached() && !offspring.isEmpty()) {
        long id = event.getRowId();
        int oldKey = BeeConst.UNDEF;

        for (Map.Entry<Integer, BeeRow> entry : offspring.entries()) {
          if (DataUtils.idEquals(entry.getValue(), id)) {
            oldKey = entry.getKey();
            break;
          }
        }

        if (!BeeConst.isUndef(oldKey)) {
          offspring.remove(oldKey, getOffspring(id));

          BeeRow row = event.getRow();
          JustDate start = JustDate.get(Data.getDateTime(VIEW_TASKS, row, COL_START_TIME));

          int newKey = (start == null) ? BeeConst.UNDEF : start.getDays();
          if (start != null) {
            offspring.put(newKey, row);
          }

          if (oldKey != newKey) {
            if (!offspring.containsKey(oldKey)) {
              toggleDayStyle(oldKey, false);
            }
            if (!BeeConst.isUndef(newKey) && offspring.get(newKey).size() == 1) {
              toggleDayStyle(newKey, true);
            }
          }

          if (oldKey == dayNumber || newKey == dayNumber) {
            if (offspring.containsKey(dayNumber)) {
              renderOffspring(table, dayNumber);
            } else {
              UiHelper.closeDialog(table);
            }
          }
        }
      }
    };

    final HandlerRegistration handlerRegistration =
        BeeKeeper.getBus().registerRowUpdateHandler(updateHandler, false);

    dialog.addCloseHandler(event -> handlerRegistration.removeHandler());

    dialog.showRelativeTo(target);
  }

  private void enableToggles(boolean enabled) {
    for (Flow container : toggleContainers.values()) {
      for (Widget widget : container) {
        if (widget instanceof HasEnabled) {
          ((HasEnabled) widget).setEnabled(enabled);
        }
      }
    }
  }

  private BeeRowSet getExecutors() {
    return executors;
  }

  private Consumer<Map<Integer, String>> getFailureHandler(final Cron cron, final String input) {
    if (!errorLabels.containsKey(cron)) {
      return null;
    }
    clearErrors(cron);

    return failures -> {
      StringBuilder sb = new StringBuilder();

      int index = 0;
      while (index < input.length()) {
        if (failures.containsKey(index)) {
          String failure = failures.get(index);
          Span span = new Span().addClass(STYLE_FAILURE).text(failure);

          sb.append(span.build());
          index += failure.length();

        } else {
          sb.append(input.charAt(index));
          index++;
        }
      }

      errorLabels.get(cron).setHtml(sb.toString());
    };
  }

  private BeeRow getOffspring(Long id) {
    if (DataUtils.isId(id)) {
      for (BeeRow row : offspring.values()) {
        if (DataUtils.idEquals(row, id)) {
          return row;
        }
      }
    }
    return null;
  }

  private String getOffspringLabel(Long taskId) {
    BeeRow row = getOffspring(taskId);

    if (row == null) {
      return null;
    } else {
      return DataUtils.join(Data.getDataInfo(VIEW_TASKS), row,
          Lists.newArrayList(ALS_EXECUTOR_FIRST_NAME, ALS_EXECUTOR_LAST_NAME, COL_STATUS),
          BeeConst.STRING_SPACE, Format.getDateRenderer(), Format.getDateTimeRenderer());
    }
  }

  private void handleOffspring(Action action, final int dayNumber, final Widget source) {
    TableRowElement rowElement = DomUtils.getParentRow(source.getElement(), false);
    if (rowElement == null) {
      return;
    }

    final long dataId = DomUtils.getDataIndexLong(rowElement);
    if (!DataUtils.isId(dataId)) {
      return;
    }

    BeeRow dataRow;

    switch (action) {
      case EDIT:
        dataRow = getOffspring(dataId);
        if (dataRow != null) {
          RowEditor.open(VIEW_TASKS, dataRow, Opener.MODAL);
        }
        break;

      case ADD:
        maybeSpawn(rowElement, dayNumber, dataId, () -> {
          if (source.isAttached()) {
            refreshOffspring(source, dayNumber);
          }
        });
        break;

      case DELETE:
        Global.confirmDelete(getOffspringLabel(dataId), Icon.WARNING,
            Collections.singletonList(Localized.dictionary().crmTaskDeleteQuestion()),
            () -> Queries.delete(VIEW_TASKS, Filter.compareId(dataId), new Queries.IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                if (BeeUtils.isPositive(result)) {
                  RowDeleteEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, dataId);
                  onOffspringDelete(dataId, dayNumber, source);
                }
              }
            }), rowElement);
        break;

      default:
        Assert.untouchable();
    }
  }

  private void initToggle(final Cron cron, final Toggle toggle, final int value) {
    toggle.addClickHandler(event -> {
      String updated = updateCronValue(cron.source, value, toggle.isChecked());
      refreshErrors(getActiveRowId(), cron, updated);
    });
  }

  private void initValueContainer(Cron cron, Flow widget) {
    toggleContainers.put(cron, widget);

    for (int i = cron.field.getMin(); i <= cron.field.getMax(); i++) {
      String text = cron.getLabel(i);
      Toggle toggle = new Toggle(text, text, STYLE_VALUE_TOGGLE, false);

      initToggle(cron, toggle, i);
      widget.add(toggle);
    }
  }

  private void maybeSpawn(Element target, final int dayNumber, final Long executor,
      final Runnable callback) {
    String caption = Format.renderDateFull(new JustDate(dayNumber));

    List<String> messages = new ArrayList<>();

    List<Integer> indexes = Lists.newArrayList(
        getExecutors().getColumnIndex(ClassifierConstants.COL_FIRST_NAME),
        getExecutors().getColumnIndex(ClassifierConstants.COL_LAST_NAME));

    int size = DataUtils.isId(executor) ? 1 : getExecutors().getNumberOfRows();
    int maxCount = (size > 15) ? 10 : (size + 1);

    int count = 0;
    for (BeeRow row : getExecutors().getRows()) {
      if (DataUtils.isId(executor) && !DataUtils.idEquals(row, executor)) {
        continue;
      }

      String userName = DataUtils.join(getExecutors().getColumns(), row, indexes,
          BeeConst.STRING_SPACE, Format.getDateRenderer(), Format.getDateTimeRenderer());
      messages.add(userName);

      count++;
      if (count >= maxCount) {
        messages.add(BeeConst.ELLIPSIS + BeeUtils.bracket(size));
        break;
      }
    }

    if (count == 0) {
      logger.warning("executor not available", executor);
      return;
    }

    if (size > 1) {
      messages.add(Localized.dictionary().crmRTSpawnTasksQuestion());
    } else {
      messages.add(Localized.dictionary().crmRTSpawnTaskQuestion());
    }

    Global.confirm(caption, Icon.QUESTION, messages,
        Localized.dictionary().actionCreate(), Localized.dictionary().actionCancel(),
        () -> {
          long rtId = getActiveRowId();

          if (DataUtils.isId(rtId)) {
            ParameterList params = TasksKeeper.createArgs(SVC_RT_SPAWN);
            params.addQueryItem(VAR_RT_ID, rtId);
            params.addQueryItem(VAR_RT_DAY, dayNumber);

            if (DataUtils.isId(executor)) {
              params.addQueryItem(COL_EXECUTOR, executor);
            }

            BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                if (response.hasResponse(BeeRowSet.class)) {
                  BeeRowSet taskData = BeeRowSet.restore(response.getResponseAsString());

                  if (!DataUtils.isEmpty(taskData)) {
                    for (BeeRow row : taskData.getRows()) {
                      offspring.put(dayNumber, row);
                    }

                    String message =
                        Localized.dictionary().crmCreatedNewTasks(taskData.getNumberOfRows());
                    BeeKeeper.getScreen().notifyInfo(message);

                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TASKS);

                    if (callback != null) {
                      callback.run();
                    }
                  }
                }
              }
            });
          }
        }, target);
  }

  private void onOffspringDelete(long taskId, int dayNumber, Widget source) {
    BeeRow row = getOffspring(taskId);
    if (row != null) {
      offspring.remove(dayNumber, row);
    }

    if (offspring.containsKey(dayNumber)) {
      if (source.isAttached()) {
        refreshOffspring(source, dayNumber);
      }

    } else {
      UiHelper.closeDialog(source);
      toggleDayStyle(dayNumber, false);
    }
  }

  private void refreshErrors(long id, Cron cron, String input) {
    if (BeeUtils.isEmpty(input)) {
      clearErrors(cron);
    } else {
      CronExpression.parseSimpleValues(BeeUtils.toString(id), cron.field, input,
          getFailureHandler(cron, input));
    }
  }

  private void refreshOffspring(Widget source, int dayNumber) {
    HtmlTable table = UiHelper.getParentTable(source);
    if (table != null) {
      renderOffspring(table, dayNumber);
    }
  }

  private void refreshValues(long id, Cron cron, String input) {
    Set<Integer> values = CronExpression.parseSimpleValues(BeeUtils.toString(id),
        cron.field, input, getFailureHandler(cron, input));
    setValues(cron, values);
  }

  private Widget renderMonth(YearMonth ym, List<DayOfMonth> days, final boolean fertile) {
    Flow panel = new Flow(STYLE_MONTH_PANEL);

    Label monthLabel = new Label(Format.renderYearMonth(ym));
    monthLabel.addStyleName(STYLE_MONTH_LABEL);

    panel.add(monthLabel);

    HtmlTable table = new HtmlTable(STYLE_MONTH_TABLE);

    List<String> wn = Format.getWeekdaysNarrowStandalone();
    for (int i = 0; i < wn.size(); i++) {
      table.setText(0, i, wn.get(i), STYLE_WEEKDAY_CELL);
    }

    JustDate startOfMonth = ym.getDate();
    int shift = startOfMonth.getDow() - 1;

    for (DayOfMonth day : days) {
      int row = (day.dom + shift - 1) / TimeUtils.DAYS_PER_WEEK + 1;
      int col = day.dow - 1;
      String text = BeeUtils.toString(day.dom);

      int dayNumber = startOfMonth.getDays() + day.dom - 1;
      boolean hasOffspring = offspring.containsKey(dayNumber);

      if (day.scheduled || hasOffspring) {
        CustomDiv widget = new CustomDiv(hasOffspring ? STYLE_HAS_TASKS : STYLE_SCHEDULED);
        widget.setText(text);
        DomUtils.setDataIndex(widget.getElement(), dayNumber);

        if (fertile || hasOffspring) {
          dayElementIds.put(dayNumber, widget.getId());

          widget.addClickHandler(event -> {
            Element target = EventUtils.getEventTargetElement(event);
            final int number = DomUtils.getDataIndexInt(target);

            if (number > 0) {
              if (offspring.containsKey(number)) {
                editOffspring(target, number);

              } else if (fertile && !DataUtils.isEmpty(getExecutors())) {
                maybeSpawn(target, number, null, () -> toggleDayStyle(number, true));
              }
            }
          });
        }

        String cellStyleName = hasOffspring ? STYLE_DAY_HAS_TASKS
            : (fertile ? STYLE_DAY_SCHEDULED : STYLE_INFERTILE);
        table.setWidget(row, col, widget, cellStyleName);

      } else {
        table.setText(row, col, text, STYLE_DAY_CELL);
      }

      if (!day.styleNames.isEmpty()) {
        for (String styleName : day.styleNames) {
          table.getCellFormatter().addStyleName(row, col, styleName);
        }
      }
    }

    panel.add(table);
    return panel;
  }

  private void renderOffspring(HtmlTable table, final int dayNumber) {
    if (!table.isEmpty()) {
      table.clear();
    }

    Set<Long> taskExecutors = new HashSet<>();
    int r = 0;

    Collection<BeeRow> taskData = offspring.get(dayNumber);
    if (BeeUtils.isEmpty(taskData) && DataUtils.isEmpty(getExecutors())) {
      return;
    }

    List<BeeColumn> taskColumns = Data.getColumns(VIEW_TASKS);

    if (!BeeUtils.isEmpty(taskData) && !BeeUtils.isEmpty(taskColumns)) {
      int ownerIndex = DataUtils.getColumnIndex(COL_OWNER, taskColumns);
      int executorIndex = DataUtils.getColumnIndex(COL_EXECUTOR, taskColumns);
      int firstNameIndex = DataUtils.getColumnIndex(ALS_EXECUTOR_FIRST_NAME, taskColumns);
      int lastNameIndex = DataUtils.getColumnIndex(ALS_EXECUTOR_LAST_NAME, taskColumns);
      int statusIndex = DataUtils.getColumnIndex(COL_STATUS, taskColumns);

      for (BeeRow taskRow : taskData) {
        Long executor = taskRow.getLong(executorIndex);
        if (!DataUtils.isId(executor)) {
          continue;
        }

        taskExecutors.add(executor);

        table.setText(r, COL_OFFSPRING_FIRST_NAME, taskRow.getString(firstNameIndex),
            STYLE_OFFSPRING_FIRST_NAME);
        table.setText(r, COL_OFFSPRING_LAST_NAME, taskRow.getString(lastNameIndex),
            STYLE_OFFSPRING_LAST_NAME);

        table.setText(r, COL_OFFSPRING_STATUS,
            EnumUtils.getCaption(TaskStatus.class, taskRow.getInteger(statusIndex)),
            STYLE_OFFSPRING_STATUS);

        FaLabel open = new FaLabel(FontAwesome.EDIT);
        open.setTitle(Localized.dictionary().actionEdit());

        open.addClickHandler(event -> {
          if (event.getSource() instanceof Widget) {
            handleOffspring(Action.EDIT, dayNumber, (Widget) event.getSource());
          }
        });

        table.setWidgetAndStyle(r, COL_OFFSPRING_OPEN, open, STYLE_OFFSPRING_OPEN);

        if (BeeKeeper.getUser().is(taskRow.getLong(ownerIndex))) {
          FaLabel delete = new FaLabel(FontAwesome.TRASH_O);
          delete.setTitle(Localized.dictionary().actionDelete());

          delete.addClickHandler(event -> {
            if (event.getSource() instanceof FaLabel) {
              handleOffspring(Action.DELETE, dayNumber, (Widget) event.getSource());
            }
          });

          table.setWidgetAndStyle(r, COL_OFFSPRING_DELETE, delete, STYLE_OFFSPRING_DELETE);
        }

        DomUtils.setDataIndex(table.getRow(r), taskRow.getId());
        table.getRowFormatter().addStyleName(r, STYLE_OFFSPRING_ACTUAL);

        r++;
      }
    }

    if (!DataUtils.isEmpty(getExecutors()) && getFormView().isRowEnabled(getActiveRow())) {
      for (BeeRow userRow : getExecutors().getRows()) {
        if (taskExecutors.contains(userRow.getId())) {
          continue;
        }

        table.setText(r, COL_OFFSPRING_FIRST_NAME,
            DataUtils.getString(getExecutors(), userRow, ClassifierConstants.COL_FIRST_NAME),
            STYLE_OFFSPRING_FIRST_NAME);
        table.setText(r, COL_OFFSPRING_LAST_NAME,
            DataUtils.getString(getExecutors(), userRow, ClassifierConstants.COL_LAST_NAME),
            STYLE_OFFSPRING_LAST_NAME);

        FaLabel create = new FaLabel(FontAwesome.PLUS_SQUARE_O);
        create.setTitle(Localized.dictionary().actionCreate());

        create.addClickHandler(event -> {
          if (event.getSource() instanceof FaLabel) {
            handleOffspring(Action.ADD, dayNumber, (Widget) event.getSource());
          }
        });

        table.setWidgetAndStyle(r, COL_OFFSPRING_CREATE, create, STYLE_OFFSPRING_CREATE);

        DomUtils.setDataIndex(table.getRow(r), userRow.getId());
        table.getRowFormatter().addStyleName(r, STYLE_OFFSPRING_POTENTIAL);

        r++;
      }
    }
  }

  private void setExecutors(BeeRowSet executors) {
    this.executors = executors;
  }

  private void setValue(Cron cron, int index, boolean selected) {
    Widget widget = toggleContainers.get(cron).getWidget(index);
    if (widget instanceof Toggle) {
      ((Toggle) widget).setChecked(selected);
    }
  }

  private void setValues(Cron cron, Set<Integer> values) {
    if (toggleContainers.containsKey(cron)) {
      for (int i = 0; i < toggleContainers.get(cron).getWidgetCount(); i++) {
        setValue(cron, i, values.contains(i + cron.shift));
      }
    }
  }

  private void showSchedule(final long rtId) {
    ParameterList params = TasksKeeper.createArgs(SVC_RT_GET_SCHEDULING_DATA);
    params.addQueryItem(VAR_RT_ID, rtId);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (getFormView() == null || !getFormView().asWidget().isAttached()
            || !DomUtils.isVisible(getFormView().getElement())) {
          return;
        }
        if (!DataUtils.idEquals(getActiveRow(), rtId)) {
          return;
        }

        if (response.hasErrors()) {
          response.notify(getFormView());
          return;
        }

        clearOffspring();
        setExecutors(null);

        List<ScheduleDateRange> scheduleDateRanges = new ArrayList<>();

        if (response.hasResponse()) {
          Map<String, String> data = Codec.deserializeLinkedHashMap(response.getResponseAsString());

          if (data.containsKey(AdministrationConstants.VIEW_USERS)) {
            setExecutors(BeeRowSet.restore(data.get(AdministrationConstants.VIEW_USERS)));
          }

          if (data.containsKey(VIEW_TASKS)) {
            BeeRowSet tasks = BeeRowSet.restore(data.get(VIEW_TASKS));
            if (!DataUtils.isEmpty(tasks)) {
              addOffspring(tasks);
            }
          }

          if (data.containsKey(VIEW_RT_DATES)) {
            BeeRowSet rtDates = BeeRowSet.restore(data.get(VIEW_RT_DATES));
            if (!DataUtils.isEmpty(rtDates)) {
              scheduleDateRanges.addAll(TaskUtils.getScheduleDateRanges(rtDates));
            }
          }
        }

        boolean fertile = getExecutors() != null && getFormView().isRowEnabled(getActiveRow());
        timeCube(scheduleDateRanges, fertile);
      }
    });
  }

  private void timeCube(List<ScheduleDateRange> scheduleDateRanges, boolean fertile) {
    JustDate from = getDateValue(COL_RT_SCHEDULE_FROM);
    JustDate until = getDateValue(COL_RT_SCHEDULE_UNTIL);

    CronExpression.Builder builder = new CronExpression.Builder(from, until)
        .id(BeeUtils.toString(getActiveRowId()))
        .dayOfMonth(getStringValue(COL_RT_DAY_OF_MONTH))
        .month(getStringValue(COL_RT_MONTH))
        .dayOfWeek(getStringValue(COL_RT_DAY_OF_WEEK))
        .year(getStringValue(COL_RT_YEAR))
        .workdayTransition(EnumUtils.getEnumByIndex(WorkdayTransition.class,
            getIntegerValue(COL_RT_WORKDAY_TRANSITION)));

    if (!BeeUtils.isEmpty(scheduleDateRanges)) {
      for (ScheduleDateRange sdr : scheduleDateRanges) {
        builder.rangeMode(sdr.getRange(), sdr.getMode());
      }
    }

    CronExpression cron = builder.build();

    JustDate min;
    JustDate max;

    if (from == null && until == null) {
      min = TimeUtils.startOfMonth();
      max = TimeUtils.endOfMonth(min, 12);
    } else if (from == null) {
      min = TimeUtils.startOfMonth(until);
      max = until;
    } else if (until == null) {
      min = TimeUtils.max(from, TimeUtils.startOfPreviousMonth(TimeUtils.today()));
      max = TimeUtils.endOfMonth(min, 12);
    } else {
      min = from;
      max = TimeUtils.max(from, until);
    }

    List<JustDate> cronDates = cron.getDates(min, max);

    Flow container = new Flow(STYLE_SCHEDULE_PANEL);

    Flow calendarPanel = new Flow(STYLE_CALENDAR_PANEL);

    dayElementIds.clear();

    JustDate today = TimeUtils.today();
    int monthCount = TimeUtils.monthDiff(min, max);

    for (int i = 0; i <= monthCount; i++) {
      YearMonth ym = new YearMonth(min).shiftMonth(i);

      int domMin = (i == 0) ? min.getDom() : 1;
      int domMax = (i == monthCount) ? max.getDom() : TimeUtils.monthLength(ym);

      List<DayOfMonth> days = new ArrayList<>();

      for (int dom = domMin; dom <= domMax; dom++) {
        JustDate date = new JustDate(ym.getYear(), ym.getMonth(), dom);
        ScheduleDateMode mode = cron.getDateMode(date);
        boolean scheduled = cronDates.contains(date);

        DayOfMonth dayOfMonth = new DayOfMonth(date, scheduled);

        if (today.equals(date)) {
          dayOfMonth.addStyleName(STYLE_TODAY);
        }
        if (mode == ScheduleDateMode.NON_WORK) {
          dayOfMonth.addStyleName((dayOfMonth.dow <= 5) ? STYLE_HOLIDAY : STYLE_WEEKEND);
        } else if (mode != null) {
          dayOfMonth.addStyleName(STYLE_PREFIX + mode.name().toLowerCase());
        }

        days.add(dayOfMonth);
      }

      Widget monthWidget = renderMonth(ym, days, fertile);
      calendarPanel.add(monthWidget);
    }

    container.add(calendarPanel);

    if (Global.isDebug()) {
      Flow infoPanel = new Flow(STYLE_INFO_PANEL);

      List<Property> info = cron.getInfo();
      for (Property p : info) {
        Label infoLabel = new Label(p.getValue());
        infoLabel.addStyleName(STYLE_INFO_LABEL);
        infoLabel.setTitle(p.getName());

        infoPanel.add(infoLabel);
      }

      container.add(infoPanel);
    }

    StyleUtils.setMaxWidth(container, BeeKeeper.getScreen().getWidth() * 8 / 10);
    StyleUtils.setMaxHeight(container, BeeKeeper.getScreen().getHeight() * 8 / 10);

    String caption = BeeUtils.joinWords(getStringValue(COL_SUMMARY),
        DateRange.closed(min, max));
    Global.showModalWidget(caption, container);
  }

  private void toggleDayStyle(int dayNumber, boolean hasOffspring) {
    Element element = DomUtils.getElementQuietly(dayElementIds.get(dayNumber));

    if (element != null) {
      element.removeClassName(hasOffspring ? STYLE_SCHEDULED : STYLE_HAS_TASKS);
      element.addClassName(hasOffspring ? STYLE_HAS_TASKS : STYLE_SCHEDULED);

      Element cell = element.getParentElement();
      if (DomUtils.isTableCellElement(cell)) {
        cell.removeClassName(hasOffspring ? STYLE_DAY_SCHEDULED : STYLE_DAY_HAS_TASKS);
        cell.addClassName(hasOffspring ? STYLE_DAY_HAS_TASKS : STYLE_DAY_SCHEDULED);
      }
    }
  }

  private String updateCronValue(String source, int value, boolean selected) {
    String oldValue = getStringValue(source);
    String newValue;

    String s = BeeUtils.toString(value);

    if (selected) {
      newValue = CronExpression.appendItem(oldValue, s);
    } else {
      newValue = CronExpression.removeItem(oldValue, s);
    }

    if (!BeeUtils.equalsTrimRight(oldValue, newValue)) {
      getActiveRow().setValue(getDataIndex(source), newValue);
      getFormView().refreshBySource(source);
    }

    return newValue;
  }

  private boolean validateBounds(IsRow row) {
    if (row == null) {
      return false;
    }

    JustDate from = getDateValue(COL_RT_SCHEDULE_FROM);
    if (from == null) {
      getFormView().notifyWarning(Localized.dictionary().valueRequired());
      focusSource(COL_RT_SCHEDULE_FROM);
      return false;
    }

    JustDate until = getDateValue(COL_RT_SCHEDULE_UNTIL);
    if (until != null && TimeUtils.isMore(from, until)) {
      getFormView().notifyWarning(Localized.dictionary().invalidRange(),
          BeeUtils.joinWords(from, until));
      focusSource(COL_RT_SCHEDULE_UNTIL);
      return false;
    }

    return true;
  }
}
