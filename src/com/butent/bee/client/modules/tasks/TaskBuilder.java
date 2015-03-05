package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputTime;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskEvent;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TaskBuilder extends AbstractFormInterceptor {

  private static final String NAME_START_DATE = "Start_Date";
  private static final String NAME_START_TIME = "Start_Time";
  private static final String NAME_END_DATE = "End_Date";
  private static final String NAME_END_TIME = "End_Time";
  private static final String NAME_EXPECTED_DURATION = "ExpectedDuration";
  private static final String NAME_USER_GROUP_SETTINGS = "UserGroupSettings";
  private static final String NAME_EXECUTORS_LABEL = "ExecutorsLabel";
  private static final String NAME_END_DATE_LABEL = "EndDateLabel";
  private static final String NAME_EXECUTORS = "Executors";
  private static final String NAME_OBSERVERS = "Observers";
  private static final String NAME_OBSERVER_GROUPS = "ObserverGroups";

  private static final String NAME_REMINDER_DATE = "Reminder_Date";
  private static final String NAME_REMINDER_TIME = "Reminder_Time";
  private static final String NAME_LABEL_SUFFIX = "Label";

  private static final String NAME_FILES = "Files";

  private HasCheckedness mailToggle;
  private InputTime expectedDurationInput;
  private Label endDateInputLabel;
  private Label executorsLabel;

  private MultiSelector executors;
  private MultiSelector executorGroups;
  private MultiSelector observers;
  private MultiSelector observerGroups;

  private final Map<Long, FileInfo> filesToUpload = new HashMap<>();
  private Long executor;
  private boolean taskIdsCallback;

  TaskBuilder() {
    super();
  }

  public TaskBuilder(Map<Long, FileInfo> files, Long executor, boolean taskIdsCallback) {
    this();

    if (files != null) {
      filesToUpload.putAll(files);
    }

    this.executor = executor;
    this.taskIdsCallback = taskIdsCallback;
  }

  @Override
  public void afterCreateEditableWidget(final EditableWidget editableWidget,
      IdentifiableWidget widget) {

    if (BeeUtils.inList(editableWidget.getWidgetName(), NAME_EXPECTED_DURATION,
        NAME_USER_GROUP_SETTINGS)) {

      editableWidget.addCellValidationHandler(new CellValidateEvent.Handler() {
        @Override
        public Boolean validateCell(CellValidateEvent event) {
          if (event.isCellValidation() && event.isPreValidation()
              && BeeUtils.isEmpty(event.getNewValue())) {

            if (BeeUtils.same(editableWidget.getWidgetName(), NAME_EXPECTED_DURATION)) {
              endDateInputLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
            } else if (BeeUtils.same(editableWidget.getWidgetName(), NAME_USER_GROUP_SETTINGS)) {
              executorsLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
            }

          } else if (event.isCellValidation() && event.isPreValidation()
              && !BeeUtils.isEmpty(event.getNewValue())) {
            if (BeeUtils.same(editableWidget.getWidgetName(), NAME_EXPECTED_DURATION)) {
              endDateInputLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
            } else if (BeeUtils.same(editableWidget.getWidgetName(), NAME_USER_GROUP_SETTINGS)) {
              executorsLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
            }
          }
          return true;
        }
      });
    }

    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof FileCollector) {
      ((FileCollector) widget).bindDnd(getFormView());
      if (!filesToUpload.isEmpty()) {
        ((FileCollector) widget).addFiles(filesToUpload.values());
      }

    } else if (BeeUtils.same(name, PROP_MAIL) && (widget instanceof HasCheckedness)) {
      mailToggle = (HasCheckedness) widget;

    } else if (BeeUtils.same(name, NAME_END_DATE_LABEL) && (widget instanceof Label)) {
      endDateInputLabel = (Label) widget;

    } else if (BeeUtils.same(name, NAME_EXPECTED_DURATION) && (widget instanceof InputTime)) {
      expectedDurationInput = (InputTime) widget;

      expectedDurationInput.addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          if (!BeeUtils.isEmpty(expectedDurationInput.getText()) && endDateInputLabel != null) {
            endDateInputLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.isEmpty(event.getValue()) && endDateInputLabel != null) {
            endDateInputLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }
        }
      });

    } else if (BeeUtils.same(NAME_EXECUTORS_LABEL, name) && (widget instanceof Label)) {
      executorsLabel = (Label) widget;

    } else if (BeeUtils.same(NAME_USER_GROUP_SETTINGS, name) && (widget instanceof MultiSelector)) {
      final MultiSelector userGroups = (MultiSelector) widget;
      executorGroups = userGroups;

      userGroups.addEditChangeHandler(new EditChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          if (!BeeUtils.isEmpty(event.getValue()) && executorsLabel != null) {
            executorsLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.isEmpty(event.getValue()) && endDateInputLabel != null) {
            executorsLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }
        }

        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (!BeeUtils.isEmpty(userGroups.getDisplayValue()) && executorsLabel != null) {
            executorsLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.isEmpty(userGroups.getDisplayValue()) && endDateInputLabel != null) {
            executorsLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }
        }
      });

      userGroups.addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (!BeeUtils.isEmpty(userGroups.getDisplayValue()) && executorsLabel != null) {
            executorsLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.isEmpty(userGroups.getDisplayValue()) && endDateInputLabel != null) {
            executorsLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }
        }
      });
    } else if (BeeUtils.same(NAME_EXECUTORS, name) && (widget instanceof MultiSelector)) {
      executors = (MultiSelector) widget;
    } else if (BeeUtils.same(NAME_OBSERVERS, name) && (widget instanceof MultiSelector)) {
      observers = (MultiSelector) widget;
    } else if (BeeUtils.same(NAME_OBSERVER_GROUPS, name) && (widget instanceof MultiSelector)) {
      observerGroups = (MultiSelector) widget;
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    setProjectStagesFilter(form, row);
    setProjectUsersFilter(form, row);
  }

  @Override
  public void beforeStateChange(State state, boolean modal) {
    if (state == State.OPEN && mailToggle != null && mailToggle.isChecked()) {
      mailToggle.setChecked(false);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new TaskBuilder();
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    if (executor != null) {
      newRow.setProperty(PROP_EXECUTORS, DataUtils.buildIdList(executor));
    }

    DateTime start = newRow.getDateTime(getDataIndex(COL_START_TIME));
    if (start == null) {
      start = TimeUtils.nowHours(1);
    }

    Widget widget = getFormView().getWidgetByName(NAME_START_DATE);
    if (widget instanceof InputDate) {
      ((InputDate) widget).setDate(start);
    }
    widget = getFormView().getWidgetByName(NAME_START_TIME);
    if (widget instanceof InputTime) {
      ((InputTime) widget).setTime(start);
    }

    DateTime end = newRow.getDateTime(getDataIndex(COL_FINISH_TIME));
    if (end != null) {
      widget = getFormView().getWidgetByName(NAME_END_DATE);
      if (widget instanceof InputDate) {
        ((InputDate) widget).setDate(end);
      }
      widget = getFormView().getWidgetByName(NAME_END_TIME);
      if (widget instanceof InputTime) {
        ((InputTime) widget).setTime(end);
      }
    }
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, final ReadyForInsertEvent event) {
    event.consume();

    IsRow activeRow = getFormView().getActiveRow();

    DateTime start = getStart();
    if (start == null) {
      event.getCallback().onFailure(Localized.getConstants().crmEnterStartDate());
      return;
    }

    DateTime end = getEnd(start, Data.getString(VIEW_TASKS, activeRow, COL_EXPECTED_DURATION));
    if (end == null) {
      event.getCallback().onFailure(Localized.getConstants().crmEnterFinishDateOrEstimatedTime());
      return;
    }

    if (TimeUtils.isLeq(end, start)) {
      event.getCallback().onFailure(Localized.getConstants().crmFinishTimeMustBeGreaterThanStart());
      return;
    }

    DateTime reminderTime = getReminderTime(end);
    if (reminderTime != null) {
      DateTime now = TimeUtils.nowMinutes();
      if (TimeUtils.isLess(reminderTime, now)) {
        event.getCallback().onFailure(BeeUtils.joinWords(
            Localized.getConstants().crmReminderTimeMustBeGreaterThan(), now));
        return;
      }

      if (TimeUtils.isMeq(reminderTime, end)) {
        event.getCallback().onFailure(BeeUtils.joinWords(
            Localized.getConstants().crmReminderTimeMustBeLessThan(), end));
        return;
      }
    }

    if (Data.isNull(VIEW_TASKS, activeRow, COL_SUMMARY)) {
      event.getCallback().onFailure(Localized.getConstants().crmEnterSubject());
      return;
    }

    if (BeeUtils.allEmpty(activeRow.getProperty(PROP_EXECUTORS),
        activeRow.getProperty(PROP_EXECUTOR_GROUPS))) {
      event.getCallback().onFailure(Localized.getConstants().crmSelectExecutor());
      return;
    }

    BeeRow newRow = DataUtils.cloneRow(activeRow);

    Data.setValue(VIEW_TASKS, newRow, COL_START_TIME, start);
    Data.setValue(VIEW_TASKS, newRow, COL_FINISH_TIME, end);

    if (reminderTime != null) {
      Data.setValue(VIEW_TASKS, newRow, COL_REMINDER_TIME, reminderTime);
    }

    if (mailToggle != null && mailToggle.isChecked()) {
      newRow.setProperty(PROP_MAIL, BooleanValue.S_TRUE);
    }

    BeeRowSet rowSet = DataUtils.createRowSetForInsert(VIEW_TASKS, getFormView().getDataColumns(),
        newRow, Sets.newHashSet(COL_EXECUTOR, COL_STATUS), true);

    ParameterList args = TasksKeeper.createTaskRequestParameters(TaskEvent.CREATE);
    args.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(rowSet));

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        Assert.notNull(response);

        if (response.hasWarnings()) {
          BeeKeeper.getScreen().notifyWarning(response.getWarnings());
        }

        if (response.hasErrors()) {
          event.getCallback().onFailure(response.getErrors());

        } else if (!response.hasResponse()) {
          event.getCallback().onFailure("No tasks created");

        } else if (response.hasResponse(BeeRowSet.class)) {
          BeeRowSet tasks = BeeRowSet.restore(response.getResponseAsString());
          if (tasks.isEmpty()) {
            event.getCallback().onFailure("No tasks created");
            return;
          }

          clearValue(NAME_START_DATE);
          clearValue(NAME_START_TIME);
          clearValue(NAME_END_DATE);
          clearValue(NAME_END_TIME);
          clearValue(NAME_REMINDER_DATE);
          clearValue(NAME_REMINDER_TIME);

          createFiles(tasks.getRowIds());

          if (!taskIdsCallback) {
            event.getCallback().onSuccess(null);
          } else {
            BeeRow row = new BeeRow(0, new String[] {DataUtils.buildIdList(tasks)});
            event.getCallback().onSuccess(row);
          }

          String message = Localized.getMessages().crmCreatedNewTasks(tasks.getNumberOfRows());
          BeeKeeper.getScreen().notifyInfo(message);

          if (!taskIdsCallback) {
            event.getCallback().onSuccess(tasks.getRow(0));
          }

          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TASKS);

        } else {
          event.getCallback().onFailure("Unknown response");
        }
      }
    });
  }

  private static void setProjectStagesFilter(FormView form, IsRow row) {
    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);

    if (BeeConst.isUndef(idxProjectOwner)) {
      return;
    }

    if (BeeConst.isUndef(idxProject)) {
      return;
    }

    Widget wProjectStage = form.getWidgetBySource(ProjectConstants.COL_PROJECT_STAGE);

    if (wProjectStage instanceof DataSelector) {
      ((DataSelector) wProjectStage).setEnabled(false);
    } else {
      return;
    }

    long currentUser = BeeUtils.unbox(BeeKeeper.getUser().getUserId());
    long projectOwner = BeeUtils.unbox(row.getLong(idxProjectOwner));
    long projectId = BeeUtils.unbox(row.getLong(idxProject));

    if (DataUtils.isId(projectId)) {
      setVisibleProjectData(form, true);
    } else {
      setVisibleProjectData(form, false);
    }

    if (currentUser != projectOwner) {
      return;
    }

    ((DataSelector) wProjectStage).getOracle().setAdditionalFilter(
        Filter.equals(ProjectConstants.COL_PROJECT, projectId), true);
    ((DataSelector) wProjectStage).setEnabled(true);

  }

  private static void setVisibleProjectData(FormView form, boolean visible) {
    Widget widget = form.getWidgetBySource(ProjectConstants.COL_PROJECT);
    if (widget != null) {
      widget.setVisible(visible);
    }

    widget = form.getWidgetBySource(ProjectConstants.COL_PROJECT_STAGE);

    if (widget != null) {
      widget.setVisible(visible);
    }

    widget = form.getWidgetByName(ProjectConstants.COL_PROJECT + NAME_LABEL_SUFFIX);

    if (widget != null) {
      widget.setVisible(visible);
    }

    widget = form.getWidgetByName(ProjectConstants.COL_PROJECT_STAGE + NAME_LABEL_SUFFIX);

    if (widget != null) {
      widget.setVisible(visible);
    }
  }

  private void clearValue(String widgetName) {
    Widget widget = getFormView().getWidgetByName(widgetName);
    if (widget instanceof Editor) {
      ((Editor) widget).clearValue();
    }
  }

  private void createFiles(final List<Long> tasks) {
    Widget widget = getFormView().getWidgetByName(NAME_FILES);

    if (widget instanceof FileCollector && !((FileCollector) widget).isEmpty()) {
      List<FileInfo> files = Lists.newArrayList(((FileCollector) widget).getFiles());

      final List<BeeColumn> columns = Data.getColumns(VIEW_TASK_FILES,
          Lists.newArrayList(COL_TASK, AdministrationConstants.COL_FILE, COL_CAPTION));

      for (final FileInfo fileInfo : files) {
        FileUtils.uploadFile(fileInfo, new Callback<Long>() {
          @Override
          public void onSuccess(Long result) {
            for (long taskId : tasks) {
              List<String> values = Lists.newArrayList(BeeUtils.toString(taskId),
                  BeeUtils.toString(result), fileInfo.getCaption());
              Queries.insert(VIEW_TASK_FILES, columns, values);
            }
          }
        });
      }

      ((FileCollector) widget).clear();
    }
  }

  private HasDateValue getDate(String widgetName) {
    Widget widget = getFormView().getWidgetByName(widgetName);
    if (widget instanceof InputDate) {
      return ((InputDate) widget).getDate();
    } else {
      return null;
    }
  }

  private DateTime getEnd(DateTime start, String duration) {
    HasDateValue datePart = getDate(NAME_END_DATE);
    if (datePart != null) {
      return TimeUtils.combine(datePart, getMillis(NAME_END_TIME));
    }

    if (start != null && !BeeUtils.isEmpty(duration)) {
      Long millis = TimeUtils.parseTime(duration);
      if (BeeUtils.isPositive(millis)) {
        return new DateTime(start.getTime() + millis);
      }
    }
    return null;
  }

  private Long getMillis(String widgetName) {
    Widget widget = getFormView().getWidgetByName(widgetName);
    if (widget instanceof InputTime) {
      return ((InputTime) widget).getMillis();
    } else {
      return null;
    }
  }

  private DateTime getReminderTime(DateTime end) {
    HasDateValue datePart = getDate(NAME_REMINDER_DATE);
    Long timePart = getMillis(NAME_REMINDER_TIME);

    if (datePart == null && timePart == null) {
      return null;
    } else if (datePart == null) {
      return TimeUtils.combine(end, timePart);
    } else {
      return TimeUtils.combine(datePart, timePart);
    }
  }

  private DateTime getStart() {
    HasDateValue datePart = getDate(NAME_START_DATE);
    if (datePart == null) {
      return null;
    } else {
      return TimeUtils.combine(datePart, getMillis(NAME_START_TIME));
    }
  }

  private void setProjectUsersFilter(final FormView form, IsRow row) {
    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);

    if (BeeConst.isUndef(idxProjectOwner)) {
      return;
    }

    if (BeeConst.isUndef(idxProject)) {
      return;
    }

    final long projectOwner = BeeUtils.unbox(row.getLong(idxProjectOwner));
    long projectId = BeeUtils.unbox(row.getLong(idxProject));

    if (!DataUtils.isId(projectId)) {
      return;
    }

    if (executors != null) {
      executors.setEnabled(false);
    }

    if (executorGroups != null) {
      executorGroups.setEnabled(false);
      executorGroups.getElement().addClassName(StyleUtils.NAME_DISABLED);
    }

    if (observers != null) {
      observers.setEnabled(false);
    }

    if (observerGroups != null) {
      observerGroups.setEnabled(false);
      observerGroups.getElement().addClassName(StyleUtils.NAME_DISABLED);
    }

    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_USERS, Lists
        .newArrayList(AdministrationConstants.COL_USER), Filter.isEqual(
        ProjectConstants.COL_PROJECT, Value.getValue(projectId)), new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        if (DataUtils.isEmpty(result)) {
          if (executors != null) {
            executors.getOracle().setAdditionalFilter(Filter.compareId(projectOwner), true);
            executors.setEnabled(true);
            return;
          }
        }

        List<Long> userIds = Lists.newArrayList(projectOwner);
        int idxUser = result.getColumnIndex(AdministrationConstants.COL_USER);

        if (BeeConst.isUndef(idxUser)) {
          Assert.untouchable();
          return;
        }

        for (IsRow userRow : result) {
          long projectUser = BeeUtils.unbox(userRow.getLong(idxUser));

          if (DataUtils.isId(projectUser)) {
            userIds.add(projectUser);
          }
        }

        if (executors != null) {
          executors.getOracle().setAdditionalFilter(Filter.idIn(userIds), true);
          executors.setEnabled(true);
        }

        if (observers != null) {
          observers.getOracle().setAdditionalFilter(Filter.idIn(userIds), true);
          observers.setEnabled(true);
        }
      }
    });
  }
}
