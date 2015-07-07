package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.composite.FileGroup.Column;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskEvent;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class TaskEditor extends AbstractFormInterceptor {

  private static final String STYLE_EVENT = CRM_STYLE_PREFIX + "taskEvent-";
  private static final String STYLE_EVENT_ROW = STYLE_EVENT + "row";
  private static final String STYLE_EVENT_ROW_NEW = STYLE_EVENT_ROW + "-new";
  private static final String STYLE_EVENT_COL = STYLE_EVENT + "col-";
  private static final String STYLE_EVENT_FILES = STYLE_EVENT + "files";

  private static final String STYLE_DURATION = CRM_STYLE_PREFIX + "taskDuration-";
  private static final String STYLE_DURATION_CELL = "Cell";
  private static final String WIDGET_PROJECT_DATA_SUFFIX = "Data";

  private static final String STYLE_EXTENSION = CRM_STYLE_PREFIX + "taskExtension";
  private static final String NAME_OBSERVERS = "Observers";

  private static final List<String> relations = Lists.newArrayList(PROP_COMPANIES, PROP_PERSONS,
      PROP_DOCUMENTS, PROP_APPOINTMENTS, PROP_DISCUSSIONS, PROP_SERVICE_OBJECTS, PROP_TASKS);

  private static void addDurationCell(HtmlTable display, int row, int col, String value,
      String style) {
    Widget widget = new CustomDiv(STYLE_DURATION + style);
    if (!BeeUtils.isEmpty(value)) {
      widget.getElement().setInnerText(value);
    }

    display.setWidget(row, col, widget, STYLE_DURATION + style + STYLE_DURATION_CELL);
  }

  private static Widget createEventCell(String colName, String value) {
    return createEventCell(colName, value, false);
  }

  private static Widget createEventCell(String colName, String value, boolean serializable) {
    Widget widget = new Flow(STYLE_EVENT + colName);
    if (!BeeUtils.isEmpty(value) && !serializable) {
      widget.getElement().setInnerText(value);
    } else if (!BeeUtils.isEmpty(value) && serializable) {
      Map<String, String> data = Codec.deserializeMap(value);

      if (data == null) {
        return widget;
      }

      if (!data.containsKey(BeeUtils.toString(TaskEvent.CREATE.ordinal()))) {
        return widget;
      }

      List<Long> extTasks =
          DataUtils.parseIdList(data.get(BeeUtils.toString(TaskEvent.CREATE.ordinal())));

      widget.getElement().setInnerHTML(Localized.getConstants().crmTasksDelegatedTasks());

      for (final Long extTaskId : extTasks) {
        InternalLink url = new InternalLink(BeeUtils.toString(extTaskId));
        url.addClickHandler(new ClickHandler() {

          @Override
          public void onClick(ClickEvent arg0) {
            RowEditor.open(VIEW_TASKS, extTaskId, Opener.NEW_TAB);
          }
        });

        ((Flow) widget).add(url);
      }

    }
    return widget;
  }

  private static List<FileInfo> filterEventFiles(List<FileInfo> input, long teId) {
    if (input.isEmpty()) {
      return input;
    }
    List<FileInfo> result = new ArrayList<>();

    for (FileInfo file : input) {
      Long id = file.getRelatedId();
      if (id != null && id == teId) {
        result.add(file);
      }
    }
    return result;
  }

  private static MultiSelector getMultiSelector(FormView form, String source) {
    Widget widget = form.getWidgetBySource(source);
    return (widget instanceof MultiSelector) ? (MultiSelector) widget : null;
  }

  private static BeeRow getResponseRow(String caption, ResponseObject ro, RpcCallback<?> callback) {
    if (!Queries.checkResponse(caption, BeeConst.UNDEF, VIEW_TASKS, ro, BeeRow.class, callback)) {
      return null;
    }

    BeeRow row = BeeRow.restore((String) ro.getResponse());
    if (row == null && callback != null) {
      callback.onFailure(caption, VIEW_TASKS, "cannot restore row");
    }
    return row;
  }

  private static String getTaskUsers(FormView form, IsRow row) {
    return DataUtils.buildIdList(TaskUtils.getTaskUsers(row, form.getDataColumns()));
  }

  private static List<String> getUpdatedRelations(IsRow oldRow, IsRow newRow) {
    List<String> updatedRelations = new ArrayList<>();
    if (oldRow == null || newRow == null) {
      return updatedRelations;
    }

    for (String relation : relations) {
      if (!DataUtils.sameIdSet(oldRow.getProperty(relation), newRow.getProperty(relation))) {
        updatedRelations.add(relation);
      }
    }
    return updatedRelations;
  }

  private static List<String> getUpdateNotes(DataInfo dataInfo, IsRow oldRow, IsRow newRow) {
    List<String> notes = new ArrayList<>();
    if (dataInfo == null || oldRow == null || newRow == null) {
      return notes;
    }

    List<BeeColumn> columns = dataInfo.getColumns();
    for (int i = 0; i < columns.size(); i++) {
      BeeColumn column = columns.get(i);

      String oldValue = oldRow.getString(i);
      String newValue = newRow.getString(i);

      if (!BeeUtils.equalsTrimRight(oldValue, newValue) && column.isEditable()) {
        String label = Localized.getLabel(column);
        String note;

        if (BeeUtils.isEmpty(oldValue)) {
          note = TaskUtils.getInsertNote(label, renderColumn(dataInfo, newRow, column, i));
        } else if (BeeUtils.isEmpty(newValue)) {
          note = TaskUtils.getDeleteNote(label, renderColumn(dataInfo, oldRow, column, i));
        } else {
          note = TaskUtils.getUpdateNote(label, renderColumn(dataInfo, oldRow, column, i),
              renderColumn(dataInfo, newRow, column, i));
        }

        notes.add(note);
      }
    }

    return notes;
  }

  private static boolean hasRelations(IsRow row) {
    if (row == null) {
      return false;
    }

    for (String relation : relations) {
      if (!BeeUtils.isEmpty(row.getProperty(relation))) {
        return true;
      }
    }
    return false;
  }

  private static String renderColumn(DataInfo dataInfo, IsRow row, BeeColumn column, int index) {
    if (COL_TASK_TYPE.equals(column.getId())) {
      int nameIndex = dataInfo.getColumnIndex(ALS_TASK_TYPE_NAME);

      if (!BeeConst.isUndef(nameIndex)) {
        return row.getString(nameIndex);
      }
    }

    return DataUtils.render(dataInfo, row, column, index);
  }

  private static String renderDuration(long millis) {
    return TimeUtils.renderTime(millis, false);
  }

  private static void sendRequest(ParameterList params,
      final RpcCallback<ResponseObject> callback) {

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          if (callback != null) {
            callback.onFailure(response.getErrors());
          }
        } else {
          if (callback != null) {
            callback.onSuccess(response);
          }
        }
      }
    });
  }

  private static boolean setDurationParams(TaskDialog dialog, Map<String, String> ids,
      ParameterList params) {
    String time = dialog.getTime(ids.get(COL_DURATION));

    if (!BeeUtils.isEmpty(time)) {
      Long type = dialog.getSelector(ids.get(COL_DURATION_TYPE)).getRelatedId();
      if (!DataUtils.isId(type)) {
        showError(Localized.getConstants().crmEnterDurationType());
        return false;
      }

      DateTime date = dialog.getDateTime(ids.get(COL_DURATION_DATE));
      if (date == null) {
        showError(Localized.getConstants().crmEnterDueDate());
        return false;
      }

      params.addDataItem(VAR_TASK_DURATION_DATE, date.serialize());
      params.addDataItem(VAR_TASK_DURATION_TIME, time);
      params.addDataItem(VAR_TASK_DURATION_TYPE, type);
    }
    return true;
  }

  private static void showDurations(FormView form, Table<String, String, Long> durations) {
    Widget widget = form.getWidgetByName(VIEW_TASK_DURATIONS);
    if (!(widget instanceof Flow)) {
      return;
    }

    Flow panel = (Flow) widget;
    panel.clear();

    if (durations.isEmpty()) {
      return;
    }

    Set<String> rows = durations.rowKeySet();
    Set<String> columns = durations.columnKeySet();

    HtmlTable display = new HtmlTable();
    display.addStyleName(STYLE_DURATION + "display");

    int r = 0;
    int c = 0;

    addDurationCell(display, r, c++, Localized.getConstants().crmSpentTime(), "caption");
    for (String column : columns) {
      addDurationCell(display, r, c++, column, "colLabel");
    }
    r++;

    long totMillis = 0;
    for (String row : rows) {
      c = 0;
      addDurationCell(display, r, c++, row, "rowLabel");

      long rowMillis = 0;
      for (String column : columns) {
        Long millis = durations.get(row, column);

        if (BeeUtils.isPositive(millis)) {
          addDurationCell(display, r, c, renderDuration(millis), "value");

          rowMillis += millis;
          totMillis += millis;
        }
        c++;
      }

      if (columns.size() > 1) {
        addDurationCell(display, r, c, renderDuration(rowMillis), "rowTotal");
      }
      r++;
    }

    if (rows.size() > 1) {
      c = 1;

      for (String column : columns) {
        Collection<Long> values = durations.column(column).values();

        long colMillis = 0;
        for (Long value : values) {
          colMillis += BeeUtils.unbox(value);
        }
        addDurationCell(display, r, c++, renderDuration(colMillis), "rowTotal");
      }

      if (columns.size() > 1) {
        addDurationCell(display, r, c, renderDuration(totMillis), "colTotal");
      }
    }

    panel.add(display);
  }

  private static void showError(String message) {
    Global.showError(Localized.getConstants().error(), Collections.singletonList(message));
  }

  private void showEvent(Flow panel, final BeeRow row, List<BeeColumn> columns,
      List<FileInfo> files, Table<String, String, Long> durations, boolean renderPhoto,
      Long lastAccess, final IsRow taskRow) {

    Flow container = new Flow();
    container.addStyleName(STYLE_EVENT_ROW);

    if (renderPhoto) {
      Flow colPhoto = new Flow();
      colPhoto.addStyleName(STYLE_EVENT_COL + COL_PHOTO);

      String photo = row.getString(DataUtils.getColumnIndex(COL_PHOTO, columns));
      if (!BeeUtils.isEmpty(photo)) {
        Image image = new Image(PhotoRenderer.getUrl(photo));
        image.addStyleName(STYLE_EVENT + COL_PHOTO);
        colPhoto.add(image);
      }

      container.add(colPhoto);
    }

    int c = 0;
    Flow col0 = new Flow();
    col0.addStyleName(STYLE_EVENT_COL + BeeUtils.toString(c));

    Integer ev = row.getInteger(DataUtils.getColumnIndex(TaskConstants.COL_EVENT, columns));
    TaskEvent event = EnumUtils.getEnumByIndex(TaskEvent.class, ev);
    if (event != null) {
      col0.add(createEventCell(TaskConstants.COL_EVENT, event.getCaption()));
    }

    DateTime publishTime = row.getDateTime(DataUtils.getColumnIndex(COL_PUBLISH_TIME, columns));
    if (publishTime != null) {
      col0.add(createEventCell(COL_PUBLISH_TIME,
          Format.getDefaultDateTimeFormat().format(publishTime)));
    }

    if (lastAccess != null && publishTime != null) {
      if (BeeUtils.unbox(lastAccess) < publishTime.getTime()) {
        container.addStyleName(STYLE_EVENT_ROW_NEW);
      } else {
        container.removeStyleName(STYLE_EVENT_ROW_NEW);
      }
    } else {
      container.removeStyleName(STYLE_EVENT_ROW_NEW);
    }

    String publisher = BeeUtils.joinWords(
        row.getString(DataUtils.getColumnIndex(ALS_PUBLISHER_FIRST_NAME, columns)),
        row.getString(DataUtils.getColumnIndex(ALS_PUBLISHER_LAST_NAME, columns)));
    if (!BeeUtils.isEmpty(publisher)) {
      col0.add(createEventCell(COL_PUBLISHER, publisher));
    }

    container.add(col0);

    c++;
    Flow col1 = new Flow();
    col1.addStyleName(STYLE_EVENT_COL + BeeUtils.toString(c));

    String note = row.getString(DataUtils.getColumnIndex(COL_EVENT_NOTE, columns));
    if (!BeeUtils.isEmpty(note)) {
      col1.add(createEventCell(COL_EVENT_NOTE, note));
    }

    String eventData = row.getString(DataUtils.getColumnIndex(COL_EVENT_DATA, columns));

    if (!BeeUtils.isEmpty(eventData)) {
      col1.add(createEventCell(COL_EVENT_NOTE, eventData, true));
    }

    String comment = row.getString(DataUtils.getColumnIndex(COL_COMMENT, columns));
    if (!BeeUtils.isEmpty(comment)) {
      col1.add(createEventCell(COL_COMMENT, comment));
      int idxOwner = Data.getColumnIndex(VIEW_TASKS, COL_OWNER);

      if (event == TaskEvent.COMMENT && Objects.equals(taskRow.getLong(idxOwner), userId)) {
        FaLabel createTask = new FaLabel(TaskEvent.CREATE.getCommandIcon());
        createTask.setTitle(TaskEvent.CREATE.getCommandLabel());
        createTask.addClickHandler(new ClickHandler() {

          @Override
          public void onClick(ClickEvent arg0) {
            doCreate(row.getId(), taskRow);
          }
        });

        col1.add(createTask);
      }
    }

    container.add(col1);

    String duration = row.getString(DataUtils.getColumnIndex(COL_DURATION, columns));
    if (!BeeUtils.isEmpty(duration)) {
      c++;
      Flow col2 = new Flow();
      col2.addStyleName(STYLE_EVENT_COL + BeeUtils.toString(c));

      col2.add(createEventCell(COL_DURATION, Localized.getConstants().crmSpentTime() + " "
          + duration));

      String durType = row.getString(DataUtils.getColumnIndex(COL_DURATION_TYPE, columns));
      if (!BeeUtils.isEmpty(durType)) {
        col2.add(createEventCell(COL_DURATION_TYPE, durType));
      }

      DateTime durDate = row.getDateTime(DataUtils.getColumnIndex(COL_DURATION_DATE, columns));
      if (durDate != null) {
        col2.add(createEventCell(COL_DURATION_DATE, durDate.toCompactString()));
      }

      container.add(col2);

      Long millis = TimeUtils.parseTime(duration);
      if (BeeUtils.isPositive(millis) && !BeeUtils.isEmpty(publisher)
          && !BeeUtils.isEmpty(durType)) {
        Long value = durations.get(publisher, durType);
        durations.put(publisher, durType, millis + BeeUtils.unbox(value));
      }
    }

    panel.add(container);

    if (!files.isEmpty()) {
      Simple fileContainer = new Simple();
      fileContainer.addStyleName(STYLE_EVENT_FILES);

      FileGroup fileGroup = new FileGroup(Lists.newArrayList(Column.ICON, Column.NAME, Column.SIZE,
          Column.CREATEDOC));
      fileGroup.addFiles(files);

      fileGroup.setDocCreator(new Consumer<FileInfo>() {

        @Override
        public void accept(FileInfo fileInfo) {
          createDocumentFromFile(fileInfo, taskRow);
        }
      });
      fileContainer.setWidget(fileGroup);
      panel.add(fileContainer);
    }
  }

  private void showEventsAndDuration(FormView form, IsRow taskRow, BeeRowSet rowSet,
      List<FileInfo> files, Long lastAccess) {

    Widget widget = form.getWidgetByName(VIEW_TASK_EVENTS);
    if (!(widget instanceof Flow) || DataUtils.isEmpty(rowSet)) {
      return;
    }

    Flow panel = (Flow) widget;
    panel.clear();

    Table<String, String, Long> durations = TreeBasedTable.create();

    boolean hasPhoto = false;
    int photoIndex = rowSet.getColumnIndex(COL_PHOTO);
    if (photoIndex >= 0) {
      for (BeeRow row : rowSet.getRows()) {
        if (!BeeUtils.isEmpty(row.getString(photoIndex))) {
          hasPhoto = true;
          break;
        }
      }
    }

    for (BeeRow row : rowSet.getRows()) {
      showEvent(panel, row, rowSet.getColumns(), filterEventFiles(files, row.getId()), durations,
          hasPhoto, lastAccess, taskRow);
    }

    showExtensions(form, rowSet);
    showDurations(form, durations);

    if (panel.getWidgetCount() > 1 && DomUtils.isVisible(form.getElement())) {
      final Widget last = panel.getWidget(panel.getWidgetCount() - 1);
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          DomUtils.scrollIntoView(last.getElement());
        }
      });
    }
  }

  private static void showExtensions(FormView form, BeeRowSet rowSet) {
    if (DataUtils.isEmpty(rowSet)) {
      return;
    }
    int index = rowSet.getColumnIndex(COL_FINISH_TIME);
    if (BeeConst.isUndef(index)) {
      return;
    }

    Widget widget = form.getWidgetByName("TaskExtensions");
    if (!(widget instanceof HasWidgets)) {
      return;
    }

    HasWidgets panel = (HasWidgets) widget;
    panel.clear();

    List<DateTime> extensions = new ArrayList<>();

    for (BeeRow row : rowSet.getRows()) {
      DateTime dt = row.getDateTime(index);
      if (dt != null) {
        extensions.add(dt);
      }
    }

    if (!extensions.isEmpty()) {
      for (int i = extensions.size() - 1; i >= 0; i--) {
        Label label = new Label(extensions.get(i).toCompactString());
        label.addStyleName(STYLE_EXTENSION);
        panel.add(label);
      }
    }
  }

  private final long userId;
  private MultiSelector observers;
  private List<Long> projectUsers;

  TaskEditor() {
    super();
    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, NAME_OBSERVERS) && widget instanceof MultiSelector) {
      observers = (MultiSelector) widget;
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    if (row == null) {
      return;
    }

    Integer status = row.getInteger(form.getDataIndex(COL_STATUS));

    for (final TaskEvent event : TaskEvent.values()) {
      String label = event.getCommandLabel();
      FontAwesome icon = event.getCommandIcon();

      IdentifiableWidget button = icon != null ? new FaLabel(icon) : new Button(label);

      ((HasClickHandlers) button).addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent e) {
          doEvent(event);
        }
      });

      if (button instanceof FaLabel) {
        ((FaLabel) button).setTitle(label);
      }

      if (!BeeUtils.isEmpty(label) && isEventEnabled(event, status)) {
        header.addCommandItem(button);
      }
    }

    setProjectStagesFilter(form, row);
    setProjectUsersFilter(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TaskEditor();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return row != null && BeeKeeper.getUser().is(row.getLong(getDataIndex(COL_OWNER)));
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    final IsRow oldRow = event.getOldRow();
    IsRow newRow = event.getNewRow();

    if (oldRow == null || newRow == null) {
      return;
    }

    if (event.isEmpty() && TaskUtils.sameObservers(oldRow, newRow)
        && getUpdatedRelations(oldRow, newRow).isEmpty()) {
      return;
    }

    ParameterList params = createParams(TaskEvent.EDIT, null);

    sendRequest(params, new RpcCallback<ResponseObject>() {
      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(TaskEvent.EDIT.getCaption(), result, this);

        if (data != null) {
          RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);

          if (hasRelations(oldRow) || hasRelations(data)) {
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
          }
        }
      }
    });

    event.consume();
    event.getCallback().onCancel();
  }

  @Override
  public boolean onStartEdit(final FormView form, final IsRow row, ScheduledCommand focusCommand) {

    final Long lastAccess = BeeUtils.toLongOrNull(row.getProperty(PROP_LAST_ACCESS));
    Long owner = row.getLong(form.getDataIndex(COL_OWNER));
    Long executor = row.getLong(form.getDataIndex(COL_EXECUTOR));

    TaskStatus oldStatus = EnumUtils.getEnumByIndex(TaskStatus.class,
        row.getInteger(form.getDataIndex(COL_STATUS)));

    DateTime start = row.getDateTime(form.getDataIndex(COL_START_TIME));

    form.setEnabled(Objects.equals(owner, userId));

    TaskStatus newStatus = null;

    if (TaskStatus.NOT_VISITED.equals(oldStatus)) {
      if (Objects.equals(executor, userId)) {
        newStatus = TaskStatus.ACTIVE;
      }
    } else if (TaskStatus.SCHEDULED.equals(oldStatus) && !TaskUtils.isScheduled(start)) {
      newStatus = Objects.equals(executor, userId) ? TaskStatus.ACTIVE : TaskStatus.NOT_VISITED;
    }

    BeeRow visitedRow = DataUtils.cloneRow(row);
    if (newStatus != null) {
      visitedRow.preliminaryUpdate(form.getDataIndex(COL_STATUS),
          BeeUtils.toString(newStatus.ordinal()));
    }

    BeeRowSet rowSet = new BeeRowSet(form.getViewName(), form.getDataColumns());
    rowSet.addRow(visitedRow);

    ParameterList params = TasksKeeper.createTaskRequestParameters(TaskEvent.VISIT);

    if (newStatus == TaskStatus.ACTIVE) {
      params.addQueryItem(VAR_TASK_VISITED, 1);
    }

    params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(rowSet));
    params.addDataItem(VAR_TASK_USERS, getTaskUsers(form, row));

    sendRequest(params, new RpcCallback<ResponseObject>() {
      @Override
      public void onFailure(String... reason) {
        form.updateRow(row, true);
        form.notifySevere(reason);
      }

      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(TaskEvent.VISIT.getCaption(), result, this);
        if (data == null) {
          return;
        }

        RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);
        if (hasRelations(data)) {
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
        }

        Widget fileWidget = form.getWidgetByName(PROP_FILES);
        if (fileWidget instanceof FileGroup) {
          ((FileGroup) fileWidget).clear();
        }

        List<FileInfo> files = getFiles(data);
        if (!files.isEmpty()) {
          if (fileWidget instanceof FileGroup) {
            for (FileInfo file : files) {
              if (file.getRelatedId() == null) {
                ((FileGroup) fileWidget).addFile(file);
              }
            }
            ((FileGroup) fileWidget).setDocCreator(new Consumer<FileInfo>() {

              @Override
              public void accept(FileInfo fileInfo) {
                createDocumentFromFile(fileInfo, row);
              }
            });
          }
        }

        if (!getEvents(data).isEmpty()) {
          showEventsAndDuration(form, data, getEvents(data), files, lastAccess);
        }

        form.updateRow(data, true);
      }
    });
    return false;
  }

  private static void setProjectStagesFilter(FormView form, IsRow row) {
    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);
    /* int idxTaskState = form.getDataIndex(COL_STATUS); */
    int idxProjectStatus = form.getDataIndex(ALS_PROJECT_STATUS);

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

    /*
     * if (BeeConst.isUndef(idxTaskState)) { return; }
     */

    if (BeeConst.isUndef(idxProjectStatus)) {
      return;
    }

    long currentUser = BeeUtils.unbox(BeeKeeper.getUser().getUserId());
    long projectOwner = BeeUtils.unbox(row.getLong(idxProjectOwner));
    long projectId = BeeUtils.unbox(row.getLong(idxProject));
    /* int state = BeeUtils.unbox(row.getInteger(idxTaskState)); */
    int projectStatus = BeeUtils.unbox(row.getInteger(idxProjectStatus));

    if (DataUtils.isId(projectId)) {
      setVisibleProjectData(form, true);
    } else {
      setVisibleProjectData(form, false);
    }

    if (currentUser != projectOwner) {
      return;
    }

    /*
     * if (TaskStatus.SCHEDULED.ordinal() != state) { return; }
     */

    if (ProjectStatus.APPROVED.ordinal() == projectStatus) {
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

    widget = form.getWidgetByName(ProjectConstants.COL_PROJECT + WIDGET_PROJECT_DATA_SUFFIX);

    if (widget != null) {
      widget.setVisible(visible);
    }

    widget = form.getWidgetByName(ProjectConstants.COL_PROJECT_STAGE + WIDGET_PROJECT_DATA_SUFFIX);

    if (widget != null) {
      widget.setVisible(visible);
    }
  }

  public static void createDocumentFromFile(final FileInfo fileInfo, final IsRow row) {

    final DataInfo dataInfo = Data.getDataInfo(DocumentConstants.VIEW_DOCUMENTS);
    final BeeRow docRow = RowFactory.createEmptyRow(dataInfo, true);

    if (docRow != null) {

      int idxCompanyName = Data.getColumnIndex(VIEW_TASKS, ALS_COMPANY_NAME);
      int idxCompany = Data.getColumnIndex(VIEW_TASKS, COL_TASK_COMPANY);

      if (!BeeConst.isUndef(idxCompanyName) && !BeeConst.isUndef(idxCompany)) {
        String companyName = row.getString(idxCompanyName);

        if (!BeeUtils.isEmpty(companyName)) {
          docRow.setValue(dataInfo
              .getColumnIndex(DocumentConstants.ALS_DOCUMENT_COMPANY_NAME),
              companyName);
          docRow.setValue(dataInfo
              .getColumnIndex(DocumentConstants.COL_DOCUMENT_COMPANY), row
              .getLong(idxCompany));
        }

        FileCollector.pushFiles(Lists.newArrayList(fileInfo));

        RowFactory.createRow(dataInfo, docRow, new RowCallback() {

          @Override
          public void onSuccess(final BeeRow br) {
            Filter filter = Filter.equals(COL_TASK, row.getId());

            Queries.getRowSet(VIEW_RELATIONS, null, filter, new Queries.RowSetCallback() {

              @Override
              public void onSuccess(BeeRowSet relRowSet) {
                List<String> valList;
                List<BeeColumn> colList = Data.getColumns(VIEW_RELATIONS);
                int index = Data.getColumnIndex(VIEW_RELATIONS, DocumentConstants.COL_DOCUMENT);

                for (BeeRow beeRow : relRowSet) {
                  valList = beeRow.getValues();

                  for (int i = 0; i < valList.size(); i++) {
                    if (valList.get(i) == String.valueOf(row.getId())) {
                      valList.set(i, null);
                      valList.set(index, String.valueOf(br.getId()));
                    }
                  }
                  Queries.insert(VIEW_RELATIONS, colList, valList);
                }

                Queries.insert(VIEW_RELATIONS, Data.getColumns(VIEW_RELATIONS,
                    Lists.newArrayList(COL_TASK, DocumentConstants.COL_DOCUMENT)),
                    Lists.newArrayList(String.valueOf(row.getId()), String
                        .valueOf(br.getId())));
              }
            });
          }
        });
      }
    }
  }

  private static BeeRowSet getEvents(IsRow row) {
    if (!BeeUtils.isEmpty(row.getProperty(PROP_EVENTS))) {
      return BeeRowSet.restore(row.getProperty(PROP_EVENTS));
    }

    return Data.createRowSet(VIEW_TASK_EVENTS);
  }

  private static List<FileInfo> getFiles(IsRow row) {
    if (BeeUtils.isEmpty(row.getProperty(PROP_FILES))) {
      return Lists.newArrayList();
    }

    return FileInfo.restoreCollection(row.getProperty(PROP_FILES));
  }

  private ParameterList createParams(TaskEvent event, BeeRow newRow, String comment) {
    FormView form = getFormView();
    String viewName = form.getViewName();

    IsRow oldRow = form.getOldRow();

    BeeRowSet updated = DataUtils.getUpdated(viewName, form.getDataColumns(), oldRow, newRow,
        form.getChildrenForUpdate());

    if (!DataUtils.isEmpty(updated)) {
      BeeRow updRow = updated.getRow(0);

      for (int i = 0; i < updated.getNumberOfColumns(); i++) {
        int index = form.getDataIndex(updated.getColumnId(i));

        newRow.setValue(index, oldRow.getString(index));
        newRow.preliminaryUpdate(index, updRow.getString(i));
      }
    }

    BeeRowSet rowSet = new BeeRowSet(viewName, form.getDataColumns());
    rowSet.addRow(newRow);

    ParameterList params = TasksKeeper.createTaskRequestParameters(event);
    params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(rowSet));
    params.addDataItem(VAR_TASK_USERS, getTaskUsers(form, oldRow));

    if (!BeeUtils.isEmpty(comment)) {
      params.addDataItem(VAR_TASK_COMMENT, comment);
    }

    List<String> notes = getUpdateNotes(Data.getDataInfo(viewName), oldRow, newRow);

    if (form.isEnabled()) {
      if (!TaskUtils.sameObservers(oldRow, newRow)) {
        String oldObservers = oldRow.getProperty(PROP_OBSERVERS);
        String newObservers = newRow.getProperty(PROP_OBSERVERS);

        MultiSelector selector = getMultiSelector(form, PROP_OBSERVERS);

        Set<Long> removed = DataUtils.getIdSetDifference(oldObservers, newObservers);
        for (long id : removed) {
          String label = selector.getRowLabel(id);
          if (!BeeUtils.isEmpty(label)) {
            notes.add(TaskUtils.getDeleteNote(Localized.getConstants().crmTaskObservers(), label));
          }
        }

        Set<Long> added = DataUtils.getIdSetDifference(newObservers, oldObservers);
        for (long id : added) {
          String label = selector.getRowLabel(id);
          if (!BeeUtils.isEmpty(label)) {
            notes.add(TaskUtils.getInsertNote(Localized.getConstants().crmTaskObservers(), label));
          }
        }
      }

      List<String> updatedRelations = getUpdatedRelations(oldRow, newRow);
      if (!updatedRelations.isEmpty()) {
        params.addDataItem(VAR_TASK_RELATIONS, NameUtils.join(updatedRelations));

        for (String relation : updatedRelations) {
          String caption = Data.getViewCaption(relation);
          MultiSelector selector = getMultiSelector(form, relation);
          if (selector == null) {
            continue;
          }

          String oldValue = oldRow.getProperty(relation);
          String newValue = newRow.getProperty(relation);

          Set<Long> removed = DataUtils.getIdSetDifference(oldValue, newValue);
          for (long id : removed) {
            String label = selector.getRowLabel(id);
            if (!BeeUtils.isEmpty(label)) {
              notes.add(TaskUtils.getDeleteNote(caption, label));
            }
          }

          Set<Long> added = DataUtils.getIdSetDifference(newValue, oldValue);
          for (long id : added) {
            String label = selector.getRowLabel(id);
            if (!BeeUtils.isEmpty(label)) {
              notes.add(TaskUtils.getInsertNote(caption, label));
            }
          }
        }
      }
    }

    if (!notes.isEmpty()) {
      params.addDataItem(VAR_TASK_NOTES, Codec.beeSerialize(notes));
    }

    return params;
  }

  private void createRelations(final IsRow taskRow, final Long eventId,
      final String taskIds) {
    final Consumer<Boolean> consumer = new Consumer<Boolean>() {
      private int consumeCount = 2;

      @Override
      public void accept(Boolean input) {
        consumeCount--;

        if (consumeCount == 0) {
          Map<String, String> data = Maps.newLinkedHashMap();
          data.put(BeeUtils.toString(TaskEvent.CREATE.ordinal()), taskIds);

          ParameterList params =
              createParams(TaskEvent.EDIT, BeeConst.STRING_EMPTY);

          params.addDataItem(COL_EVENT_DATA, Codec.beeSerialize(data));

          sendRequest(params, TaskEvent.EDIT);
        }
      }
    };

    if (DataUtils.isId(eventId)) {
      Queries.getValue(VIEW_TASK_EVENTS, eventId, COL_EVENT_DATA, new RpcCallback<String>() {

        @Override
        public void onSuccess(String result) {
          Map<String, String> data;

          if (BeeUtils.isEmpty(result)) {
            data = Maps.newLinkedHashMap();
          } else {
            data = Codec.deserializeMap(result);
          }

          if (data.containsKey(BeeUtils.toString(TaskEvent.CREATE.ordinal()))) {
            List<Long> lTaskIds =
                DataUtils.parseIdList(data.get(BeeUtils.toString(TaskEvent.CREATE.ordinal())));
            lTaskIds.addAll(DataUtils.parseIdList(taskIds));

            data.put(BeeUtils.toString(TaskEvent.CREATE.ordinal()), DataUtils
                .buildIdList(lTaskIds));
          } else {
            data.put(BeeUtils.toString(TaskEvent.CREATE.ordinal()), taskIds);
          }

          Queries.update(VIEW_TASK_EVENTS, eventId, COL_EVENT_DATA, Value.getValue(Codec
              .beeSerialize(data)),
              new IntCallback() {

                @Override
                public void onSuccess(Integer updateCount) {
                  consumer.accept(null);
                }
              });

        }
      });
    } else {
      consumer.accept(null);
    }

    List<Long> relIds = DataUtils.parseIdList(taskRow.getProperty(PROP_TASKS));
    relIds.addAll(DataUtils.parseIdList(taskIds));

    Queries.updateChildren(VIEW_TASKS, taskRow.getId(), Lists.newArrayList(RowChildren
        .create(TBL_RELATIONS, COL_TASK, null, COL_TASK, DataUtils.buildIdList(relIds))),
        new RowCallback() {

          @Override
          public void onSuccess(BeeRow result) {
            consumer.accept(null);
          }
        });

  }

  private ParameterList createParams(TaskEvent event, String comment) {
    return createParams(event, getNewRow(), comment);
  }

  private void doApprove() {
    final TaskDialog dialog = new TaskDialog(Localized.getConstants().crmTaskConfirmation());

    final String did = dialog.addDateTime(Localized.getConstants().crmTaskConfirmDate(), true,
        TimeUtils.nowMinutes());
    final String cid = dialog.addComment(false);

    dialog.addAction(Localized.getConstants().crmTaskConfirm(), new ScheduledCommand() {
      @Override
      public void execute() {

        DateTime approved = dialog.getDateTime(did);
        if (approved == null) {
          showError(Localized.getConstants().crmEnterConfirmDate());
          return;
        }

        BeeRow newRow = getNewRow(TaskStatus.APPROVED);
        newRow.setValue(getFormView().getDataIndex(COL_APPROVED), approved);

        ParameterList params = createParams(TaskEvent.APPROVE, newRow, dialog.getComment(cid));

        sendRequest(params, TaskEvent.APPROVE);
        dialog.close();
      }
    });

    dialog.display();
  }

  private void doCancel() {
    final TaskDialog dialog = new TaskDialog(Localized.getConstants().crmTaskCancellation());

    final String cid = dialog.addComment(true);

    dialog.addAction(Localized.getConstants().crmTaskCancel(), new ScheduledCommand() {
      @Override
      public void execute() {

        String comment = dialog.getComment(cid);
        if (BeeUtils.isEmpty(comment)) {
          showError(Localized.getConstants().crmEnterComment());
          return;
        }

        ParameterList params = createParams(TaskEvent.CANCEL, getNewRow(TaskStatus.CANCELED),
            comment);

        sendRequest(params, TaskEvent.CANCEL);
        dialog.close();
      }
    });

    dialog.display();
  }

  private void doComment() {
    final TaskDialog dialog =
        new TaskDialog(Localized.getConstants().crmTaskCommentTimeRegistration());

    final String cid = dialog.addComment(false);
    final String fid = dialog.addFileCollector();

    final Map<String, String> durIds = dialog.addDuration();

    dialog.addAction(Localized.getConstants().actionSave(), new ScheduledCommand() {
      @Override
      public void execute() {

        String comment = dialog.getComment(cid);
        String time = dialog.getTime(durIds.get(COL_DURATION));

        if (BeeUtils.allEmpty(comment, time)) {
          showError(Localized.getConstants().crmEnterCommentOrDuration());
          return;
        }

        ParameterList params = createParams(TaskEvent.COMMENT, comment);

        if (setDurationParams(dialog, durIds, params)) {
          sendRequest(params, TaskEvent.COMMENT, dialog.getFiles(fid));
          dialog.close();
        }
      }
    });

    dialog.display();
  }

  private void doComplete() {
    final TaskDialog dialog = new TaskDialog(Localized.getConstants().crmTaskFinishing());

    final String did = dialog.addDateTime(Localized.getConstants().crmTaskCompleteDate(), true,
        TimeUtils.nowMinutes());

    final String cid = dialog.addComment(false);
    final String fid = dialog.addFileCollector();

    final Map<String, String> durIds = dialog.addDuration();

    dialog.addAction(Localized.getConstants().crmActionFinish(), new ScheduledCommand() {
      @Override
      public void execute() {

        DateTime completed = dialog.getDateTime(did);
        if (completed == null) {
          showError(Localized.getConstants().crmEnterCompleteDate());
          return;
        }

        String comment = dialog.getComment(cid);

        BeeRow newRow = getNewRow(TaskStatus.COMPLETED);
        newRow.setValue(getFormView().getDataIndex(COL_COMPLETED), completed);

        ParameterList params = createParams(TaskEvent.COMPLETE, newRow, comment);

        if (setDurationParams(dialog, durIds, params)) {
          sendRequest(params, TaskEvent.COMPLETE, dialog.getFiles(fid));
          dialog.close();
        }
      }
    });

    dialog.display();
  }

  private void doCreate(final Long eventId, final IsRow taskRow) {
    DataInfo newTaskInfo = Data.getDataInfo(VIEW_TASKS);
    BeeRow newTaskRow = RowFactory.createEmptyRow(newTaskInfo, true);

    int idxSummary = newTaskInfo.getColumnIndex(COL_SUMMARY);
    int idxDescription = newTaskInfo.getColumnIndex(COL_DESCRIPTION);
    int idxPriority = newTaskInfo.getColumnIndex(COL_PRIORITY);
    int idxType = newTaskInfo.getColumnIndex(COL_TASK_TYPE);
    int idxTypeName = newTaskInfo.getColumnIndex(ALS_TASK_TYPE_NAME);
    int idxCompany = newTaskInfo.getColumnIndex(COL_COMPANY);
    int idxCompanyName = newTaskInfo.getColumnIndex(ALS_COMPANY_NAME);
    int idxCompanyType = newTaskInfo.getColumnIndex(ALS_COMPANY_TYPE_NAME);
    int idxContact = newTaskInfo.getColumnIndex(COL_CONTACT);
    int idxContactFirstName = newTaskInfo.getColumnIndex(TaskConstants.ALS_CONTACT_FIRST_NAME);
    int idxContactLastName = newTaskInfo.getColumnIndex(TaskConstants.ALS_CONTACT_LAST_NAME);

    int idxProject = newTaskInfo.getColumnIndex(ProjectConstants.COL_PROJECT);
    int idxProjectStage = newTaskInfo.getColumnIndex(ProjectConstants.COL_PROJECT_STAGE);

    newTaskRow.setValue(idxSummary, Data.clamp(VIEW_TASKS, COL_SUMMARY, BeeUtils.joinWords(taskRow
        .getString(idxSummary), BeeUtils
        .parenthesize(taskRow.getId()))));

    String description = taskRow.getString(idxDescription);

    BeeRowSet events = getEvents(taskRow);

    if (!events.isEmpty() && DataUtils.isId(eventId)) {
      IsRow event = events.getRowById(eventId);

      if (event != null
          && Objects.equals(TaskEvent.COMMENT.ordinal(), event.getInteger(events
              .getColumnIndex(TaskConstants.COL_EVENT)))) {
        description =
            BeeUtils.join(BeeConst.STRING_EOL
                + BeeUtils.replicate(BeeConst.CHAR_MINUS, BeeConst.MAX_SCALE)
                + BeeConst.STRING_EOL, description, BeeUtils
                .joinWords(event.getDateTime(events.getColumnIndex(COL_PUBLISH_TIME)), BeeUtils
                    .nvl(event
                        .getString(events.getColumnIndex(ALS_PUBLISHER_FIRST_NAME)),
                        BeeConst.STRING_EMPTY), BeeUtils.nvl(event
                    .getString(events.getColumnIndex(ALS_PUBLISHER_LAST_NAME)),
                    BeeConst.STRING_EMPTY)
                    + BeeConst.STRING_COLON, event
                    .getString(events
                        .getColumnIndex(COL_COMMENT))));
      }
    } else if (!events.isEmpty()) {
      for (IsRow event : events) {
        if (event != null
            && Objects.equals(TaskEvent.COMMENT.ordinal(), event.getInteger(events
                .getColumnIndex(TaskConstants.COL_EVENT)))) {
          description =
              BeeUtils.join(BeeConst.STRING_EOL
                  + BeeUtils.replicate(BeeConst.CHAR_MINUS, BeeConst.MAX_SCALE)
                  + BeeConst.STRING_EOL, description, BeeUtils
                  .joinWords(event.getDateTime(events.getColumnIndex(COL_PUBLISH_TIME)), BeeUtils
                      .nvl(event
                          .getString(events.getColumnIndex(ALS_PUBLISHER_FIRST_NAME)),
                          BeeConst.STRING_EMPTY), BeeUtils.nvl(event
                      .getString(events.getColumnIndex(ALS_PUBLISHER_LAST_NAME)),
                      BeeConst.STRING_EMPTY)
                      + BeeConst.STRING_COLON, event
                      .getString(events
                          .getColumnIndex(COL_COMMENT))));
        }
      }
    }

    newTaskRow.setValue(idxDescription, Data.clamp(VIEW_TASKS, COL_DESCRIPTION, description));

    newTaskRow.setValue(idxPriority, taskRow
        .getInteger(idxPriority));

    newTaskRow.setValue(idxType, taskRow
        .getLong(idxType));

    newTaskRow.setValue(idxTypeName, taskRow
        .getString(idxTypeName));

    newTaskRow.setValue(idxCompany, taskRow
        .getLong(idxCompany));

    newTaskRow.setValue(idxCompanyName, taskRow
        .getString(idxCompanyName));

    newTaskRow.setValue(idxCompanyType, taskRow
        .getLong(idxCompanyType));

    newTaskRow.setValue(idxContact, taskRow
        .getLong(idxContact));

    newTaskRow.setValue(idxContactFirstName, taskRow
        .getString(idxContactFirstName));

    newTaskRow.setValue(idxContactLastName, taskRow
        .getString(idxContactLastName));

    newTaskRow.setValue(idxProject, taskRow
        .getLong(idxProject));

    newTaskRow.setValue(idxProjectStage, taskRow
        .getLong(idxProjectStage));

    Map<Long, FileInfo> files = Maps.newLinkedHashMap();

    for (FileInfo file : getFiles(taskRow)) {
      files.put(file.getId(), file);
    }

    RowFactory.createRow(newTaskInfo.getNewRowForm(), newTaskInfo.getNewRowCaption(), newTaskInfo,
        newTaskRow, null, new TaskBuilder(files, null, true), new RowCallback() {

          @Override
          public void onSuccess(BeeRow result) {
            createRelations(taskRow, eventId, result.getString(0));
          }
        });
  }

  private void doEvent(TaskEvent event) {
    if (!isEventEnabled(event, getStatus())) {
      showError(Localized.getConstants().actionNotAllowed());
    }

    switch (event) {
      case COMMENT:
        doComment();
        break;

      case FORWARD:
        doForward();
        break;

      case EXTEND:
        doExtend();
        break;

      case SUSPEND:
        doSuspend();
        break;

      case CANCEL:
        doCancel();
        break;

      case COMPLETE:
        doComplete();
        break;

      case APPROVE:
        doApprove();
        break;

      case RENEW:
        doRenew();
        break;

      case CREATE:
        doCreate(null, getActiveRow());
        break;
      case ACTIVATE:
      case VISIT:
      case EDIT:
        Assert.untouchable();
    }
  }

  private void doExtend() {
    final TaskDialog dialog = new TaskDialog(Localized.getConstants().crmTaskTermChange());

    final boolean isScheduled = TaskStatus.SCHEDULED.is(getStatus());

    final String startId = isScheduled
        ? dialog.addDateTime(Localized.getConstants().crmStartDate(), true,
            getDateTime(COL_START_TIME)) : null;
    final String endId = dialog.addDateTime(Localized.getConstants().crmFinishDate(), true, null);

    final String cid = dialog.addComment(false);

    dialog.addAction(Localized.getConstants().crmTaskChangeTerm(), new ScheduledCommand() {
      @Override
      public void execute() {

        DateTime oldStart = getDateTime(COL_START_TIME);
        DateTime oldEnd = getDateTime(COL_FINISH_TIME);

        DateTime newStart = (startId == null) ? oldStart
            : BeeUtils.nvl(dialog.getDateTime(startId), oldStart);
        DateTime newEnd = dialog.getDateTime(endId);

        if (newEnd == null) {
          showError(Localized.getConstants().crmEnterFinishDate());
          return;
        }

        if (Objects.equals(newStart, oldStart) && Objects.equals(newEnd, oldEnd)) {
          showError(Localized.getConstants().crmTermNotChanged());
          return;
        }

        if (TimeUtils.isLeq(newEnd, newStart)) {
          showError(Localized.getConstants().crmFinishDateMustBeGreaterThanStart());
          return;
        }

        DateTime now = TimeUtils.nowMinutes();
        if (TimeUtils.isLess(newEnd, TimeUtils.nowMinutes())) {
          Global.showError("Time travel not supported",
              Collections.singletonList(Localized.getConstants().crmFinishDateMustBeGreaterThan()
                  + " " + now.toCompactString()));
          return;
        }

        BeeRow newRow = getNewRow();
        if (startId != null && newStart != null && !Objects.equals(newStart, oldStart)) {
          newRow.setValue(getFormView().getDataIndex(COL_START_TIME), newStart);
        }
        if (!Objects.equals(newEnd, oldEnd)) {
          newRow.setValue(getFormView().getDataIndex(COL_FINISH_TIME), newEnd);
        }

        ParameterList params = createParams(TaskEvent.EXTEND, newRow, dialog.getComment(cid));
        if (oldEnd != null && !Objects.equals(newEnd, oldEnd)) {
          params.addDataItem(VAR_TASK_FINISH_TIME, oldEnd.getTime());
        }

        sendRequest(params, TaskEvent.EXTEND);
        dialog.close();
      }
    });

    dialog.display(endId);
  }

  private void doForward() {
    final Long oldUser = getExecutor();
    Set<Long> exclusions = new HashSet<>();
    Set<Long> filter = new HashSet<>();
    if (oldUser != null) {
      exclusions.add(oldUser);
    }

    if (!BeeUtils.isEmpty(getProjectUsers())) {
      filter.addAll(getProjectUsers());
    }

    final TaskDialog dialog = new TaskDialog(Localized.getConstants().crmTaskForwarding());

    final String sid =
        dialog.addSelector(Localized.getConstants().crmTaskExecutor(), VIEW_USERS,
            Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME),
            true, exclusions, filter);

    final String cid = dialog.addComment(true);

    dialog.addAction(Localized.getConstants().crmActionForward(), new ScheduledCommand() {
      @Override
      public void execute() {

        DataSelector selector = dialog.getSelector(sid);

        Long newUser = selector.getRelatedId();
        if (newUser == null) {
          showError(Localized.getConstants().crmEnterExecutor());
          return;
        }
        if (Objects.equals(newUser, oldUser)) {
          showError(Localized.getConstants().crmSelectedSameExecutor());
          return;
        }

        String comment = dialog.getComment(cid);
        if (BeeUtils.isEmpty(comment)) {
          showError(Localized.getConstants().crmEnterComment());
          return;
        }

        BeeRow newRow = getNewRow();
        RelationUtils.updateRow(Data.getDataInfo(VIEW_TASKS), COL_EXECUTOR, newRow,
            Data.getDataInfo(VIEW_USERS), selector.getRelatedRow(), true);

        TaskStatus oldStatus = EnumUtils.getEnumByIndex(TaskStatus.class,
            newRow.getInteger(getDataIndex(COL_STATUS)));
        TaskStatus newStatus = null;

        if (oldStatus == TaskStatus.ACTIVE && !Objects.equals(newUser, userId)) {
          newStatus = TaskStatus.NOT_VISITED;
        } else if (oldStatus == TaskStatus.NOT_VISITED && Objects.equals(newUser, userId)) {
          newStatus = TaskStatus.ACTIVE;
        }

        if (newStatus != null) {
          newRow.setValue(getDataIndex(COL_STATUS), newStatus.ordinal());
        }

        ParameterList params = createParams(TaskEvent.FORWARD, newRow, comment);

        sendRequest(params, TaskEvent.FORWARD);
        dialog.close();
      }
    });

    dialog.display();
  }

  private void doRenew() {
    final TaskDialog dialog =
        new TaskDialog(Localized.getConstants().crmTaskReturningForExecution());

    final String cid = dialog.addComment(false);

    dialog.addAction(Localized.getConstants().crmTaskReturnExecution(), new ScheduledCommand() {
      @Override
      public void execute() {

        TaskStatus newStatus = isExecutor() ? TaskStatus.ACTIVE : TaskStatus.NOT_VISITED;

        BeeRow newRow = getNewRow(newStatus);
        newRow.clearCell(getFormView().getDataIndex(COL_COMPLETED));
        newRow.clearCell(getFormView().getDataIndex(COL_APPROVED));

        ParameterList params = createParams(TaskEvent.RENEW, newRow, dialog.getComment(cid));

        sendRequest(params, TaskEvent.RENEW);
        dialog.close();
      }
    });

    dialog.display();
  }

  private void doSuspend() {
    final TaskDialog dialog = new TaskDialog(Localized.getConstants().crmTaskSuspension());

    final String cid = dialog.addComment(true);

    dialog.addAction(Localized.getConstants().crmActionSuspend(), new ScheduledCommand() {
      @Override
      public void execute() {

        String comment = dialog.getComment(cid);
        if (BeeUtils.isEmpty(comment)) {
          showError(Localized.getConstants().crmEnterComment());
          return;
        }

        ParameterList params = createParams(TaskEvent.SUSPEND, getNewRow(TaskStatus.SUSPENDED),
            comment);

        sendRequest(params, TaskEvent.SUSPEND);
        dialog.close();
      }
    });

    dialog.display();
  }

  private DateTime getDateTime(String colName) {
    return getFormView().getActiveRow().getDateTime(getFormView().getDataIndex(colName));
  }

  private Long getExecutor() {
    return getLong(COL_EXECUTOR);
  }

  private Long getLong(String colName) {
    return getFormView().getActiveRow().getLong(getFormView().getDataIndex(colName));
  }

  private BeeRow getNewRow() {
    return DataUtils.cloneRow(getFormView().getActiveRow());
  }

  private BeeRow getNewRow(TaskStatus status) {
    BeeRow row = getNewRow();
    row.setValue(getFormView().getDataIndex(COL_STATUS), status.ordinal());
    return row;
  }

  private Long getOwner() {
    return getLong(COL_OWNER);
  }

  private List<Long> getProjectUsers() {
    return projectUsers;
  }

  private Integer getStatus() {
    return getFormView().getActiveRow().getInteger(getFormView().getDataIndex(COL_STATUS));
  }

  private boolean isEventEnabled(TaskEvent event, Integer status) {
    if (event == null || status == null || getOwner() == null || getExecutor() == null) {
      return false;
    }

    if (isOwner()) {
      if (event == TaskEvent.COMMENT) {
        return true;
      } else if (event == TaskEvent.CREATE) {
        return true;
      } else if (isExecutor() && TaskStatus.in(status, TaskStatus.ACTIVE)) {
        return event == TaskEvent.FORWARD || event == TaskEvent.COMPLETE;
      } else {
        return false;
      }
    }

    switch (event) {
      case COMMENT:
        return true;

      case RENEW:
        return TaskStatus.in(status, TaskStatus.SUSPENDED, TaskStatus.CANCELED,
            TaskStatus.COMPLETED, TaskStatus.APPROVED);

      case FORWARD:
        return TaskStatus.in(status, TaskStatus.NOT_VISITED, TaskStatus.ACTIVE,
            TaskStatus.SCHEDULED);

      case EXTEND:
        return TaskStatus.in(status, TaskStatus.NOT_VISITED, TaskStatus.ACTIVE,
            TaskStatus.SCHEDULED);

      case SUSPEND:
      case COMPLETE:
        return TaskStatus.in(status, TaskStatus.NOT_VISITED, TaskStatus.ACTIVE);

      case CANCEL:
        return TaskStatus.in(status, TaskStatus.NOT_VISITED, TaskStatus.ACTIVE,
            TaskStatus.SUSPENDED, TaskStatus.SCHEDULED);

      case APPROVE:
        return TaskStatus.in(status, TaskStatus.COMPLETED) && !isExecutor();

      case ACTIVATE:
      case CREATE:
      case EDIT:
      case VISIT:
        return false;
    }

    return false;
  }

  private boolean isExecutor() {
    return Objects.equals(userId, getExecutor());
  }

  private boolean isOwner() {
    return Objects.equals(userId, getOwner());
  }

  private void onResponse(BeeRow data) {
    RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);

    FormView form = getFormView();
    Long lastAccess = BeeUtils.toLongOrNull(data.getProperty(PROP_LAST_ACCESS));

    if (hasRelations(form.getOldRow()) || hasRelations(data)) {
      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
    }

    if (!getEvents(data).isEmpty()) {
      showEventsAndDuration(form, form.getActiveRow(), getEvents(data), getFiles(data),
          lastAccess);
    }

    form.updateRow(data, true);
  }

  private void requeryEvents(final long taskId) {
    ParameterList params = TasksKeeper.createArgs(SVC_GET_TASK_DATA);
    params.addDataItem(VAR_TASK_ID, taskId);
    params.addDataItem(VAR_TASK_PROPERTIES, BeeUtils.join(BeeConst.STRING_COMMA,
        PROP_OBSERVERS, PROP_FILES, PROP_EVENTS));
    params.addDataItem(VAR_TASK_RELATIONS, BeeConst.STRING_ASTERISK);

    RpcCallback<ResponseObject> callback = new RpcCallback<ResponseObject>() {
      @Override
      public void onFailure(String... reason) {
        getFormView().notifySevere(reason);
      }

      @Override
      public void onSuccess(ResponseObject result) {
        if (getFormView().getActiveRow().getId() != taskId) {
          return;
        }

        BeeRow data = getResponseRow(SVC_GET_TASK_DATA, result, this);
        if (data != null) {
          onResponse(data);
        }
      }
    };

    sendRequest(params, callback);
  }

  private void sendFiles(final List<FileInfo> files, final long taskId, final long teId) {

    final Holder<Integer> counter = Holder.of(0);

    final List<BeeColumn> columns = Data.getColumns(VIEW_TASK_FILES,
        Lists.newArrayList(COL_TASK, COL_TASK_EVENT, COL_FILE, COL_CAPTION));

    for (final FileInfo fileInfo : files) {
      FileUtils.uploadFile(fileInfo, new Callback<Long>() {
        @Override
        public void onSuccess(Long result) {
          List<String> values = Lists.newArrayList(BeeUtils.toString(taskId),
              BeeUtils.toString(teId), BeeUtils.toString(result), fileInfo.getCaption());

          Queries.insert(VIEW_TASK_FILES, columns, values, null, new RowCallback() {
            @Override
            public void onSuccess(BeeRow row) {
              counter.set(counter.get() + 1);
              if (counter.get() == files.size()) {
                requeryEvents(taskId);
              }
            }
          });
        }
      });
    }
  }

  private void sendRequest(ParameterList params, TaskEvent event) {
    sendRequest(params, event, null);
  }

  private void sendRequest(ParameterList params, final TaskEvent event,
      final List<FileInfo> files) {

    RpcCallback<ResponseObject> callback = new RpcCallback<ResponseObject>() {
      @Override
      public void onFailure(String... reason) {
        getFormView().notifySevere(reason);
      }

      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(event.getCaption(), result, this);
        if (data != null) {
          if (result.hasWarnings()) {
            BeeKeeper.getScreen().notifyWarning(result.getWarnings());
          }

          onResponse(data);

          if (!BeeUtils.isEmpty(files)) {
            Long teId = BeeUtils.toLongOrNull(data.getProperty(PROP_LAST_EVENT_ID));
            if (DataUtils.isId(teId)) {
              sendFiles(files, data.getId(), teId);
            }
          }
        }
      }
    };

    sendRequest(params, callback);
  }

  private void setProjectUsers(List<Long> projectUsers) {
    this.projectUsers = projectUsers;
  }

  private void setProjectUsersFilter(final FormView form, IsRow row) {
    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);

    setProjectUsers(null);

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

    if (observers != null) {
      observers.setEnabled(false);
    }

    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_USERS, Lists
        .newArrayList(COL_USER), Filter.isEqual(
        ProjectConstants.COL_PROJECT, Value.getValue(projectId)), new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        List<Long> userIds = Lists.newArrayList(projectOwner);
        int idxUser = result.getColumnIndex(COL_USER);

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

        if (observers != null) {
          observers.getOracle().setAdditionalFilter(Filter.idIn(userIds), true);
          observers.setEnabled(true);
        }

        setProjectUsers(userIds);
      }
    });

  }
}
