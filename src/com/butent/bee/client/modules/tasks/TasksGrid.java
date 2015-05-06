package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.images.star.Stars;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.client.view.grid.GridSettings;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class TasksGrid extends AbstractGridInterceptor {

  private static final String NAME_MODE = "Mode";
  private static final String NAME_SLACK = "Slack";
  private static final String NAME_STAR = "Star";

  private static final int DEFAULT_STAR_COUNT = 3;

  static BiConsumer<GridOptions, PresenterCallback> getFeedFilterHandler(Feed feed) {
    final TaskType type = TaskType.getByFeed(feed);
    Assert.notNull(type);

    BiConsumer<GridOptions, PresenterCallback> consumer =
        new BiConsumer<GridFactory.GridOptions, PresenterCallback>() {
          @Override
          public void accept(GridOptions gridOptions, PresenterCallback callback) {
            String cap = BeeUtils.notEmpty(gridOptions.getCaption(), type.getCaption());
            GridFactory.openGrid(GRID_TASKS, new TasksGrid(type, cap), gridOptions, callback);
          }
        };

    return consumer;
  }

  private final TaskType type;
  private final String caption;

  private final Long userId;

  protected TasksGrid(TaskType type, String caption) {
    this.type = type;
    this.caption = caption;

    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public boolean afterCreateColumn(String columnId, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.same(columnId, NAME_MODE) && column instanceof HasCellRenderer) {
      ((HasCellRenderer) column).setRenderer(new ModeRenderer());

    } else if (BeeUtils.same(columnId, NAME_SLACK) && column instanceof HasCellRenderer) {
      ((HasCellRenderer) column).setRenderer(new SlackRenderer(dataColumns));

    } else if (BeeUtils.inListSame(columnId, COL_FINISH_TIME, COL_EXECUTOR)) {
      editableColumn.addCellValidationHandler(ValidationHelper.DO_NOT_VALIDATE);
    }

    return true;
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().clearCommandPanel();

    if (type.equals(TaskType.ALL) || type.equals(TaskType.DELEGATED)) {
      FaLabel confirmTask = new FaLabel(FontAwesome.CHECK_SQUARE_O);
      confirmTask.setTitle(Localized.getConstants().crmTaskConfirm());
      confirmTask.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent arg0) {
          confirmTasksClick();
        }
      });

      presenter.getHeader().addCommandItem(confirmTask);
    }

    if (type.equals(TaskType.DELEGATED)
        && BeeKeeper.getUser().canCreateData(ProjectConstants.VIEW_PROJECTS)) {
      FaLabel createProject = new FaLabel(FontAwesome.ROCKET);
      createProject.setTitle(Localized.getConstants().prjCreateFromTasks());
      createProject.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent arg0) {
          createProjectClick();
        }
      });

      presenter.getHeader().addCommandItem(createProject);
    }
  }

  @Override
  public boolean beforeAction(Action action, final GridPresenter presenter) {
    if (action == Action.COPY) {
      if (presenter.getMainView().isEnabled() && presenter.getActiveRow() != null) {
        String title = presenter.getActiveRow().getString(getDataIndex(COL_SUMMARY));
        List<String> msg = Lists.newArrayList(Localized.getConstants().crmTaskCopyQuestion());

        List<String> options = Lists.newArrayList(Localized.getConstants().crmNewTask(),
            Localized.getConstants().crmNewRecurringTask(), Localized.getConstants().cancel());
        int defValue = options.size() - 1;

        Global.messageBox(title, Icon.QUESTION, msg, options, defValue, new ChoiceCallback() {
          @Override
          public void onSuccess(final int value) {
            if (value < 0 || value > 1 || presenter.getActiveRow() == null) {
              return;
            }

            ParameterList params = TasksKeeper.createArgs(SVC_GET_TASK_DATA);
            params.addQueryItem(VAR_TASK_ID, getTaskId(presenter.getActiveRow()));
            params.addQueryItem(VAR_TASK_PROPERTIES, PROP_OBSERVERS);
            params.addQueryItem(VAR_TASK_RELATIONS, 1);

            BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                if (Queries.checkRowResponse(SVC_GET_TASK_DATA, VIEW_TASKS, response)) {
                  BeeRow original = BeeRow.restore(response.getResponseAsString());

                  switch (value) {
                    case 0:
                      copyTask(original);
                      break;
                    case 1:
                      copyAsRecurringTask(original);
                      break;
                  }
                }
              }
            });
          }
        });
      }

      return false;

    } else {
      return super.beforeAction(action, presenter);
    }
  }

  @Override
  public ColumnDescription beforeCreateColumn(GridView gridView,
      ColumnDescription columnDescription) {

    if (type == TaskType.ASSIGNED && columnDescription.is(COL_EXECUTOR)
        || type == TaskType.DELEGATED && columnDescription.is(COL_OWNER)) {

      if (columnDescription.getVisible() == null
          && !GridSettings.hasVisibleColumns(gridView.getGridKey())) {
        ColumnDescription copy = columnDescription.copy();
        copy.setVisible(false);

        return copy;
      }
    }

    return columnDescription;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {
    Provider provider = presenter.getDataProvider();
    Long owner = activeRow.getLong(provider.getColumnIndex(COL_OWNER));

    if (Objects.equals(owner, userId)) {
      return GridInterceptor.DeleteMode.SINGLE;
    } else {
      presenter.getGridView().notifyWarning(
          BeeUtils.joinWords(Localized.getConstants().crmTask(), getTaskId(activeRow)),
          Localized.getConstants().crmTaskDeleteCanManager());
      return GridInterceptor.DeleteMode.CANCEL;
    }
  }

  @Override
  public List<String> getDeleteRowMessage(IsRow row) {
    String m1 = BeeUtils.joinWords(Localized.getConstants().crmTask(), getTaskId(row));
    String m2 = Localized.getConstants().crmTaskDeleteQuestion();

    return Lists.newArrayList(m1, m2);
  }

  @Override
  public AbstractFilterSupplier getFilterSupplier(String columnName,
      ColumnDescription columnDescription) {
    if (BeeUtils.same(columnName, NAME_SLACK)) {
      return new SlackFilterSupplier(columnDescription.getFilterOptions());

    } else if (BeeUtils.same(columnName, NAME_STAR)) {
      return new StarFilterSupplier(columnDescription.getFilterOptions());

    } else if (BeeUtils.same(columnName, NAME_MODE)) {
      return new ModeFilterSupplier(columnDescription.getFilterOptions());

    } else {
      return super.getFilterSupplier(columnName, columnDescription);
    }
  }

  @Override
  public ColumnHeader getHeader(String columnName, String headerCaption) {
    if (PROP_STAR.equals(columnName)) {
      return new ColumnHeader(columnName, Stars.getDefaultHeader(), BeeConst.STRING_ASTERISK);
    } else {
      return super.getHeader(columnName, headerCaption);
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new TasksGrid(type, caption);
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    gridDescription.setFilter(type.getFilter(new LongValue(userId)));
    return true;
  }

  @Override
  public void onEditStart(final EditStartEvent event) {
    maybeEditStar(event);
  }

  protected void afterCopyAsRecurringTask() {
  }

  protected void afterCopyTask() {
  }

  protected Long getTaskId(IsRow row) {
    return (row == null) ? null : row.getId();
  }

  protected boolean maybeEditStar(final EditStartEvent event) {
    if (event != null && PROP_STAR.equals(event.getColumnId())
        && event.getRowValue() != null && event.getRowValue().getProperty(PROP_USER) != null) {

      final CellSource source = CellSource.forProperty(PROP_STAR, ValueType.INTEGER);
      EditorAssistant.editStarCell(DEFAULT_STAR_COUNT, event, source, new Consumer<Integer>() {
        @Override
        public void accept(Integer parameter) {
          updateStar(event, source, parameter);
        }
      });

      return true;
    } else {
      return false;
    }
  }

  private void confirmTasksClick() {
    final GridView gridView = getGridPresenter().getGridView();
    CompoundFilter filter = CompoundFilter.or();

    for (RowInfo row : gridView.getSelectedRows(SelectedRows.ALL)) {
      filter.add(Filter.compareId(row.getId()));
    }

    if (filter.isEmpty()) {
      IsRow selectedRow = gridView.getActiveRow();
      if (selectedRow == null) {
        gridView.notifyWarning(Localized.getConstants().selectAtLeastOneRow());
        return;
      } else {
        confirmTask(gridView, selectedRow);
      }

    } else {
      confirmTasks(gridView, filter);
    }
  }

  private void createProjectClick() {
    final GridView gridView = getGridPresenter().getGridView();
    int idxTaskProject = gridView.getDataIndex(ProjectConstants.COL_PROJECT);
    int idxTaskCompany = gridView.getDataIndex(ClassifierConstants.COL_COMPANY);
    int idxTaskCompanyName = gridView.getDataIndex(ClassifierConstants.ALS_COMPANY_NAME);
    int idxTaskCompanyTypeName = gridView.getDataIndex(ClassifierConstants.ALS_COMPANY_TYPE_NAME);
    int idxTaskContact = gridView.getDataIndex(ClassifierConstants.COL_CONTACT);
    int idxTaskContactPerson = gridView.getDataIndex(ClassifierConstants.ALS_CONTACT_PERSON);
    int idxTaskContactFirstName = gridView.getDataIndex(ClassifierConstants.ALS_CONTACT_FIRST_NAME);
    int idxTaskContactLastName = gridView.getDataIndex(ClassifierConstants.ALS_CONTACT_LAST_NAME);
    int idxTaskContactCompanyName =
        gridView.getDataIndex(ClassifierConstants.ALS_CONTACT_COMPANY_NAME);

    int idxTaskOwner = gridView.getDataIndex(COL_OWNER);
    int idxTaskOwnerFirstName = gridView.getDataIndex(ALS_OWNER_FIRST_NAME);
    int idxTaskOwnerLastName = gridView.getDataIndex(ALS_OWNER_LAST_NAME);
    int idxTaskDescrition = gridView.getDataIndex(COL_DESCRIPTION);

    final IsRow selectedRow = gridView.getActiveRow();

    if (selectedRow == null) {
      gridView.notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }

    if (!BeeUtils.isEmpty(selectedRow.getString(idxTaskProject))) {
      gridView.notifyWarning(Localized.getMessages().taskAssignedToProject(selectedRow.getId(),
          selectedRow.getLong(idxTaskProject)));
      return;
    }

    DataInfo prjDataInfo = Data.getDataInfo(ProjectConstants.VIEW_PROJECTS);
    int idxPrjCompany = prjDataInfo.getColumnIndex(ClassifierConstants.COL_COMPANY);
    int idxPrjCompanyName = prjDataInfo.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);
    int idxPrjCompanyTypeName =
        prjDataInfo.getColumnIndex(ClassifierConstants.ALS_COMPANY_TYPE_NAME);
    int idxPrjContact = prjDataInfo.getColumnIndex(ClassifierConstants.COL_CONTACT);
    int idxPrjContactPerson = prjDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_PERSON);
    int idxPrjContactFirstName =
        prjDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_FIRST_NAME);
    int idxPrjContactLastName =
        prjDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_LAST_NAME);
    int idxPrjContactCompanyName =
        prjDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_COMPANY_NAME);

    int idxPrjOwner = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_OWNER);
    int idxPrjOwnerFirstName = prjDataInfo.getColumnIndex(ProjectConstants.ALS_OWNER_FIRST_NAME);
    int idxPrjOwnerLastName = prjDataInfo.getColumnIndex(ProjectConstants.ALS_OWNER_LAST_NAME);
    int idxPrjDescrition = prjDataInfo.getColumnIndex(ProjectConstants.COL_DESCRIPTION);
    int idxPrjStartDate = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_START_DATE);

    BeeRow prjRow = RowFactory.createEmptyRow(prjDataInfo, true);
    prjRow.setValue(idxPrjCompany, selectedRow.getValue(idxTaskCompany));
    prjRow.setValue(idxPrjCompanyName, selectedRow.getValue(idxTaskCompanyName));
    prjRow.setValue(idxPrjCompanyTypeName, selectedRow.getValue(idxTaskCompanyTypeName));
    prjRow.setValue(idxPrjContact, selectedRow.getValue(idxTaskContact));
    prjRow.setValue(idxPrjContactPerson, selectedRow.getValue(idxTaskContactPerson));
    prjRow.setValue(idxPrjContactFirstName, selectedRow.getValue(idxTaskContactFirstName));
    prjRow.setValue(idxPrjContactLastName, selectedRow.getValue(idxTaskContactLastName));
    prjRow.setValue(idxPrjContactCompanyName, selectedRow.getValue(idxTaskContactCompanyName));
    prjRow.setValue(idxPrjOwner, selectedRow.getValue(idxTaskOwner));
    prjRow.setValue(idxPrjOwnerFirstName, selectedRow.getValue(idxTaskOwnerFirstName));
    prjRow.setValue(idxPrjOwnerLastName, selectedRow.getValue(idxTaskOwnerLastName));
    prjRow.setValue(idxPrjDescrition, selectedRow.getValue(idxTaskDescrition));
    prjRow.setValue(idxPrjStartDate, new JustDate());

    RowFactory.createRow(prjDataInfo, prjRow, new RowCallback() {

      @Override
      public void onSuccess(final BeeRow projectRow) {
        final List<Long> observers = Lists.newArrayList();
        // Temporary disabled create project users
        // DataUtils.parseIdList(selectedRow.getProperty(TaskConstants.PROP_OBSERVERS));

        if (!BeeUtils.isEmpty(observers)) {
          addProjectUsers(observers, projectRow);
        }

        Queries.update(VIEW_TASKS, selectedRow.getId(), ProjectConstants.COL_PROJECT, Value
            .getValue(projectRow.getId()), new IntCallback() {

          @Override
          public void onSuccess(Integer result) {
            gridView.notifyInfo(Localized.getMessages().newProjectCreated(projectRow.getId()));
          }
        });
      }
    });
  }

  private void confirmTask(final GridView gridView, final IsRow row) {
    Assert.notNull(row);

    final List<BeeRow> taskRow = Lists.newArrayList(DataUtils.cloneRow(row));
    final DataInfo info = Data.getDataInfo(gridView.getViewName());
    final ResponseObject messages = ResponseObject.emptyResponse();
    long user = BeeUtils.unbox(userId);

    if (!TaskUtils.canConfirmTasks(info, taskRow, user, messages)) {
      if (messages.hasWarnings()) {
        gridView.notifyWarning(messages.getWarnings());
      }
      return;
    }

    final TaskDialog dialog = new TaskDialog(Localized.getConstants().crmTaskConfirmation());

    final String did =
        dialog.addDateTime(Localized.getConstants().crmTaskConfirmDate(), true, TimeUtils
            .nowMinutes());
    final String cid = dialog.addComment(false);

    dialog.addAction(Localized.getConstants().crmTaskConfirm(), new ScheduledCommand() {
      @Override
      public void execute() {
        DateTime approved = dialog.getDateTime(did);
        if (approved == null) {
          gridView.notifySevere(Localized.getConstants().crmEnterConfirmDate());
          return;
        }

        List<String> notes = new ArrayList<>();

        BeeRow newRow = DataUtils.cloneRow(row);
        newRow.setValue(info.getColumnIndex(COL_STATUS), TaskStatus.APPROVED.ordinal());
        newRow.setValue(info.getColumnIndex(COL_APPROVED), approved);

        notes.add(TaskUtils.getUpdateNote(Localized.getLabel(info.getColumn(COL_STATUS)),
            DataUtils.render(info, row, info.getColumn(COL_STATUS),
                info.getColumnIndex(COL_STATUS)),
            DataUtils.render(info, newRow, info.getColumn(COL_STATUS),
                info.getColumnIndex(COL_STATUS))));

        notes.add(TaskUtils.getInsertNote(Localized.getLabel(info.getColumn(COL_APPROVED)),
            DataUtils.render(info, newRow, info.getColumn(COL_APPROVED),
                info.getColumnIndex(COL_APPROVED))));

        ParameterList params = TasksKeeper.createArgs(SVC_CONFIRM_TASKS);
        params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(Lists.newArrayList(row.getId())));
        params.addDataItem(VAR_TASK_APPROVED_TIME, approved.serialize());

        String comment = dialog.getComment(cid);
        if (!BeeUtils.isEmpty(comment)) {
          params.addDataItem(VAR_TASK_COMMENT, comment);
        }

        if (!notes.isEmpty()) {
          params.addDataItem(VAR_TASK_NOTES, Codec.beeSerialize(notes));
        }

        sendRequest(params, gridView);
        dialog.close();
      }
    });

    dialog.display();
  }

  private void confirmTasks(final GridView gridView, final CompoundFilter filter) {
    Queries.getRowSet(gridView.getViewName(), null, filter, new RowSetCallback() {
      @Override
      public void onSuccess(final BeeRowSet result) {
        final DataInfo info = Data.getDataInfo(gridView.getViewName());
        final ResponseObject messages = ResponseObject.emptyResponse();
        long user = BeeUtils.unbox(userId);

        if (!TaskUtils.canConfirmTasks(info, result.getRows(), user, messages)) {
          if (messages.hasWarnings()) {
            gridView.notifyWarning(messages.getWarnings());
          }
          return;
        }

        Global.confirm(Localized.getConstants().crmTasksConfirmQuestion(),
            new ConfirmationCallback() {
              @Override
              public void onConfirm() {
                DateTime approved = new DateTime();

                ParameterList params = TasksKeeper.createArgs(SVC_CONFIRM_TASKS);
                params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(result.getRowIds()));
                params.addDataItem(VAR_TASK_APPROVED_TIME, approved.serialize());

                sendRequest(params, gridView);
              }
            });
      }
    });
  }

  private void copyAsRecurringTask(BeeRow oldRow) {
    DataInfo sourceInfo = Data.getDataInfo(VIEW_TASKS);
    if (sourceInfo == null) {
      return;
    }
    DataInfo targetInfo = Data.getDataInfo(VIEW_RECURRING_TASKS);
    if (targetInfo == null) {
      return;
    }

    BeeRow newRow = RowFactory.createEmptyRow(targetInfo, true);

    Set<String> colNames = Sets.newHashSet(COL_PRIORITY, COL_SUMMARY, COL_DESCRIPTION);
    for (String colName : colNames) {
      String value = oldRow.getString(sourceInfo.getColumnIndex(colName));
      if (!BeeUtils.isEmpty(value)) {
        newRow.setValue(targetInfo.getColumnIndex(colName), value);
      }
    }

    DateTime startTime = oldRow.getDateTime(sourceInfo.getColumnIndex(COL_START_TIME));
    DateTime finishTime = oldRow.getDateTime(sourceInfo.getColumnIndex(COL_FINISH_TIME));

    if (startTime != null) {
      int minutes = TimeUtils.minutesSinceDayStarted(startTime);
      if (minutes > 0) {
        newRow.setValue(targetInfo.getColumnIndex(COL_RT_START_AT),
            TimeUtils.renderMinutes(minutes, true));
      }
    }

    String duration = oldRow.getString(sourceInfo.getColumnIndex(COL_EXPECTED_DURATION));
    if (!BeeUtils.isEmpty(duration)) {
      newRow.clearCell(targetInfo.getColumnIndex(COL_RT_DURATION_DAYS));
      newRow.setValue(targetInfo.getColumnIndex(COL_RT_DURATION_TIME), duration);

    } else if (startTime != null && finishTime != null) {
      long durationMillis = finishTime.getTime() - startTime.getTime();
      long days = durationMillis / TimeUtils.MILLIS_PER_DAY;
      long time = durationMillis % TimeUtils.MILLIS_PER_DAY;

      if (durationMillis > 0 && (days > 0 || time > TimeUtils.MILLIS_PER_MINUTE)) {
        if (days > 0) {
          newRow.setValue(targetInfo.getColumnIndex(COL_RT_DURATION_DAYS), days);
        } else {
          newRow.clearCell(targetInfo.getColumnIndex(COL_RT_DURATION_DAYS));
        }

        if (time > TimeUtils.MILLIS_PER_MINUTE) {
          newRow.setValue(targetInfo.getColumnIndex(COL_RT_DURATION_TIME),
              TimeUtils.renderMinutes(BeeUtils.toInt(time / TimeUtils.MILLIS_PER_MINUTE), true));
        }
      }
    }

    colNames = Sets.newHashSet(ClassifierConstants.COL_COMPANY, ClassifierConstants.COL_CONTACT,
        COL_REMINDER, COL_TASK_TYPE);
    for (String colName : colNames) {
      RelationUtils.copyWithDescendants(sourceInfo, colName, oldRow, targetInfo, colName, newRow);
    }

    Long owner = BeeKeeper.getUser().getUserId();
    Long executor = oldRow.getLong(sourceInfo.getColumnIndex(COL_EXECUTOR));

    if (executor != null) {
      newRow.setProperty(PROP_EXECUTORS, executor.toString());
    }

    String observers = oldRow.getProperty(PROP_OBSERVERS);
    if (!BeeUtils.isEmpty(observers)) {
      List<Long> users = DataUtils.parseIdList(observers);

      if (users.contains(owner)) {
        users.remove(owner);
      }
      if (users.contains(executor)) {
        users.remove(executor);
      }

      if (!users.isEmpty()) {
        newRow.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(users));
      }
    }

    for (String propName : TaskUtils.getRelationPropertyNames()) {
      String propValue = oldRow.getProperty(propName);
      if (!BeeUtils.isEmpty(propValue)) {
        newRow.setProperty(propName, propValue);
      }
    }

    RowFactory.createRow(targetInfo, newRow, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        afterCopyAsRecurringTask();
      }
    });
  }

  private void copyTask(BeeRow oldRow) {
    DataInfo dataInfo = Data.getDataInfo(VIEW_TASKS);
    if (dataInfo == null) {
      return;
    }

    BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);

    Set<String> colNames = Sets.newHashSet(COL_PRIORITY, COL_SUMMARY, COL_DESCRIPTION,
        COL_EXPECTED_DURATION);
    for (String colName : colNames) {
      int index = dataInfo.getColumnIndex(colName);
      String value = oldRow.getString(index);

      if (!BeeUtils.isEmpty(value)) {
        newRow.setValue(index, value);
      }
    }

    colNames = Sets.newHashSet(ClassifierConstants.COL_COMPANY, ClassifierConstants.COL_CONTACT,
        COL_REMINDER, COL_TASK_TYPE);
    for (String colName : colNames) {
      RelationUtils.copyWithDescendants(dataInfo, colName, oldRow, dataInfo, colName, newRow);
    }

    Long owner = BeeKeeper.getUser().getUserId();
    Long executor = oldRow.getLong(dataInfo.getColumnIndex(COL_EXECUTOR));

    if (executor != null) {
      newRow.setProperty(PROP_EXECUTORS, executor.toString());
    }

    String observers = oldRow.getProperty(PROP_OBSERVERS);
    if (!BeeUtils.isEmpty(observers)) {
      List<Long> users = DataUtils.parseIdList(observers);

      if (users.contains(owner)) {
        users.remove(owner);
      }
      if (users.contains(executor)) {
        users.remove(executor);
      }

      if (!users.isEmpty()) {
        newRow.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(users));
      }
    }

    for (String propName : TaskUtils.getRelationPropertyNames()) {
      String propValue = oldRow.getProperty(propName);
      if (!BeeUtils.isEmpty(propValue)) {
        newRow.setProperty(propName, propValue);
      }
    }

    RowFactory.createRow(dataInfo, newRow, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        afterCopyTask();
      }
    });
  }

  private void sendRequest(final ParameterList params, final GridView gridView) {
    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasMessages()) {
          gridView.notifySevere(BeeUtils.joinItems(response.getMessages()));
          return;
        }

        gridView.notifyInfo(Localized.getConstants().crmTaskStatusApproved());
        getGridPresenter().refresh(true);
      }
    });
  }

  private void updateStar(final EditStartEvent event, final CellSource source,
      final Integer value) {

    final Long taskId = getTaskId(event.getRowValue());
    if (!DataUtils.isId(taskId)) {
      return;
    }

    Filter filter = Filter.and(Filter.equals(COL_TASK, taskId),
        Filter.equals(AdministrationConstants.COL_USER, userId));

    Queries.update(VIEW_TASK_USERS, filter, COL_STAR, new IntegerValue(value),
        new Queries.IntCallback() {
          @Override
          public void onSuccess(Integer result) {
            CellUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), event.getRowValue().getId(),
                event.getRowValue().getVersion(), source,
                (value == null) ? null : BeeUtils.toString(value));
          }
        });
  }

  private static void addProjectUsers(final List<Long> userIds, final BeeRow projectRow) {
    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_USERS, Lists
        .newArrayList(AdministrationConstants.COL_USER),
        Filter.equals(ProjectConstants.COL_PROJECT, projectRow.getId()), new RowSetCallback() {

          @Override
          public void onSuccess(BeeRowSet projectUsers) {
            for (IsRow row : projectUsers) {
              userIds.remove(row.getLong(projectUsers
                  .getColumnIndex(AdministrationConstants.COL_USER)));
            }

            for (Long user : userIds) {
              Queries.insert(ProjectConstants.VIEW_PROJECT_USERS, Data.getColumns(
                  ProjectConstants.VIEW_PROJECT_USERS, Lists.newArrayList(
                      ProjectConstants.COL_PROJECT, AdministrationConstants.COL_USER)),
                  Lists.newArrayList(BeeUtils.toString(projectRow.getId()), BeeUtils
                      .toString(user)));
            }
          }
        });
  }
}
