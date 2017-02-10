package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.modules.projects.ProjectsHelper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

class ChildTasksGrid extends TasksGrid {

  private static final List<String> COPY_COLUMNS = Lists.newArrayList(COL_SUMMARY,
    COL_DESCRIPTION, COL_PRIORITY, COL_TASK_TYPE, COL_EXPECTED_DURATION,
    COL_TASK_COMPANY, ClassifierConstants.COL_CONTACT, COL_REMINDER);

  ChildTasksGrid() {
    super(TaskType.RELATED, null);
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    if (copy) {
      return true;
    }

    presenter.getGridView().ensureRelId(relId -> {
      DataInfo childTaskDataInfo = Data.getDataInfo(presenter.getViewName());

      BeeRow childTaskRow = RowFactory.createEmptyRow(childTaskDataInfo, true);
      String relColumn = presenter.getGridView().getRelColumn();

      FormView parentForm = ViewHelper.getForm(presenter.getMainView());
      if (parentForm != null) {
        DataInfo parentFormDataInfo = Data.getDataInfo(parentForm.getViewName());
        IsRow parentFormRow = parentForm.getActiveRow();

        if (parentFormRow != null) {
          RelationUtils.updateRow(childTaskDataInfo, relColumn, childTaskRow,
            parentFormDataInfo, parentFormRow, true);
        }
        if (BeeUtils.same(parentForm.getViewName(), ProjectConstants.VIEW_PROJECTS)
            && parentFormRow != null) {
          Data.setValue(VIEW_TASKS, childTaskRow, COL_TASK_COMPANY, Data.getLong(
            ProjectConstants.VIEW_PROJECTS, parentFormRow, ProjectConstants.COL_COMAPNY));
          RelationUtils.updateRow(childTaskDataInfo, COL_TASK_COMPANY, childTaskRow,
              parentFormDataInfo, parentFormRow, false);
        } else if (BeeUtils.same(parentForm.getViewName(), ProjectConstants.VIEW_PROJECT_STAGES)) {
          IsRow prjRow = ViewHelper.getParentRow(parentForm.asWidget(), ProjectConstants
              .VIEW_PROJECTS);

          if (prjRow != null) {
            RelationUtils.updateRow(childTaskDataInfo, ProjectConstants.COL_PROJECT, childTaskRow,
                Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), prjRow, true);

            Data.setValue(VIEW_TASKS, childTaskRow, COL_TASK_COMPANY, Data.getLong(
              ProjectConstants.VIEW_PROJECTS, prjRow, ProjectConstants.COL_COMAPNY));
            RelationUtils.updateRow(childTaskDataInfo, COL_TASK_COMPANY, childTaskRow,
                Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), prjRow, false);
          }
        }
      }
      RowFactory.createRow(childTaskDataInfo, childTaskRow, Modality.ENABLED, new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          presenter.handleAction(Action.REFRESH);
        }
      });
    });

    return false;
  }

  @Override
  public void beforeRender(final GridView gridView, RenderingEvent event) {

    for (IsRow row : getGridView().getRowData()) {
      if (!BeeUtils.isEmpty(row.getProperty(ProjectConstants.PROP_TEMPLATE))) {
        getGridView().getRowData().remove(row);
      }
    }

    FormView form = ViewHelper.getForm(gridView.asWidget());

    if (form == null) {
      return;
    }

    IsRow formRow = form.getActiveRow();

    if (formRow == null) {
      return;
    }

    setEnabled(form, formRow);

    String prop = formRow.getProperty(ProjectConstants.VIEW_PROJECT_TEMPLATE_TASK_COPY);

    if (BeeUtils.isEmpty(prop)) {
      return;
    }

    BeeRowSet templates = BeeRowSet.restore(prop);
    DataInfo viewTasks = Data.getDataInfo(TaskConstants.VIEW_TASKS);
    DataInfo viewTmpTasks = Data.getDataInfo(ProjectConstants.VIEW_PROJECT_TEMPLATE_TASK_COPY);

    for (IsRow templRow : templates) {

      BeeRow row = RowFactory.createEmptyRow(viewTasks, true);

      for (String col : COPY_COLUMNS) {
        if (!viewTasks.containsColumn(col)) {
          continue;
        }
        row.setValue(viewTasks.getColumnIndex(col),
            templRow.getValue(templates.getColumnIndex(col)));

        if (viewTasks.hasRelation(col)) {
          RelationUtils.updateRow(viewTasks, col, row, viewTmpTasks, templRow, false);
        }
      }
      row.setValue(viewTasks.getColumnIndex(TaskConstants.COL_STATUS),
          TaskConstants.TaskStatus.NOT_VISITED.ordinal());

      row.setProperty(ProjectConstants.PROP_TEMPLATE, BeeUtils.toString(templRow.getId()));

      gridView.getGrid().getRowData().add(0, row);
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new ChildTasksGrid();
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter,
      IsRow activeRow, Collection<RowInfo> selectedRows, DeleteMode defMode) {

    if (!BeeUtils.isEmpty(activeRow.getProperty(ProjectConstants.PROP_TEMPLATE))) {
      return DeleteMode.SINGLE;
    }

    return super.getDeleteMode(presenter, activeRow, selectedRows, defMode);
  }

  @Override
  public DeleteMode beforeDeleteRow(final GridPresenter presenter, IsRow row) {
    if (!BeeUtils.isEmpty(row.getProperty(ProjectConstants.PROP_TEMPLATE))) {

      final Long templateId = BeeUtils.toLong(row.getProperty(ProjectConstants.PROP_TEMPLATE));

      if (!DataUtils.isId(templateId)) {
        return DeleteMode.CANCEL;
      }

      Queries.deleteRow(ProjectConstants.VIEW_PROJECT_TEMPLATE_TASK_COPY, templateId,
          new Queries.IntCallback() {
            @Override
            public void onSuccess(Integer result) {
              DataChangeEvent.fireRefresh(BeeKeeper.getBus(), getViewName());
              presenter.handleAction(Action.REFRESH);
            }
          });
      return DeleteMode.CANCEL;
    }

    return super.beforeDeleteRow(presenter, row);
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    if (!BeeUtils.isEmpty(event.getRowValue().getProperty(ProjectConstants.PROP_TEMPLATE))) {
      event.consume();

      IsRow templRow = event.getRowValue();
      final Long templateId =
          BeeUtils.toLong(templRow.getProperty(ProjectConstants.PROP_TEMPLATE));

      if (!DataUtils.isId(templateId)) {
        return;
      }

      final DataInfo viewTasks = Data.getDataInfo(TaskConstants.VIEW_TASKS);
      final BeeRow row = RowFactory.createEmptyRow(viewTasks, true);

      for (String col : COPY_COLUMNS) {
        if (!viewTasks.containsColumn(col)) {
          continue;
        }
        row.setValue(viewTasks.getColumnIndex(col),
            templRow.getValue(viewTasks.getColumnIndex(col)));

        if (viewTasks.hasRelation(col)) {
          RelationUtils.updateRow(viewTasks, col, row, viewTasks, templRow, false);
        }
      }

      if (getGridView() != null) {

        getGridView().ensureRelId(result -> {
          FormView parentForm = ViewHelper.getForm(getGridView().asWidget());
          if (parentForm != null) {

            if (parentForm.getActiveRow() != null) {

              RelationUtils.updateRow(viewTasks, getGridView().getRelColumn(), row,
                  Data.getDataInfo(parentForm.getViewName()), parentForm.getActiveRow(), true);

              switch (parentForm.getViewName()) {
                case ProjectConstants.VIEW_PROJECT_STAGES:
                  IsRow prjRow = ViewHelper.getParentRow(parentForm.asWidget(), ProjectConstants
                      .VIEW_PROJECTS);

                  if (prjRow != null) {
                    row.setValue(viewTasks.getColumnIndex(ProjectConstants.COL_PROJECT),
                        prjRow.getId());

                    RelationUtils.updateRow(viewTasks, ProjectConstants.COL_PROJECT,
                        row, Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), prjRow, false);

                    if (!DataUtils.isId(row.getLong(viewTasks.getColumnIndex(COL_TASK_COMPANY)))) {

                      row.setValue(viewTasks.getColumnIndex(COL_TASK_COMPANY),
                          Data.getLong(ProjectConstants.VIEW_PROJECTS, prjRow,
                              ClassifierConstants.COL_COMPANY));

                      RelationUtils.updateRow(viewTasks, COL_TASK_COMPANY, row,
                          Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), prjRow, false);
                    }
                  }
                  break;
                case ProjectConstants.VIEW_PROJECTS:
                  if (!DataUtils.isId(row.getLong(viewTasks.getColumnIndex(COL_TASK_COMPANY)))) {
                    row.setValue(viewTasks.getColumnIndex(COL_TASK_COMPANY),
                        parentForm.getLongValue(ClassifierConstants.COL_COMPANY));

                    RelationUtils.updateRow(viewTasks, COL_TASK_COMPANY, row,
                        Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), parentForm.getActiveRow(),
                        false);
                  }
                  break;
                default:
                  break;
              }
            }
          }

          RowFactory.createRow(viewTasks, row, Modality.ENABLED, new RowCallback() {
            @Override
            public void onSuccess(BeeRow createdTask) {
              Queries.deleteRow(ProjectConstants.VIEW_PROJECT_TEMPLATE_TASK_COPY, templateId,
                  new Queries.IntCallback() {
                    @Override
                    public void onSuccess(Integer templateTask) {
                      getGridPresenter().handleAction(Action.REFRESH);
                    }
                  });
            }
          });
        });
      }

      return;
    }

    super.onEditStart(event);
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (!event.hasView(TaskConstants.VIEW_TASKS)
        && !event.hasView(TaskConstants.VIEW_RELATED_TASKS)) {
      return;
    }
    String column = null;
    IsRow row = event.getRow();
    IsRow oldRow = null;

    if (getGridView() != null && getGridView().getGrid() != null) {
      column = getGridView().getRelColumn();
      oldRow = getGridView().getGrid().getRowById(row.getId());
    }

    if (BeeUtils.isEmpty(column)) {
      super.onRowUpdate(event);
      return;
    }

    FormView parentForm = ViewHelper.getForm(getGridView().asWidget());
    if (parentForm == null || DataUtils.isNewRow(parentForm.getActiveRow())) {
      super.onRowUpdate(event);
      return;
    }

    Long parentRowId = parentForm.getActiveRowId();
    Long relId = Data.getLong(event.getViewName(), row, column);
    Long oldId = oldRow != null ? Data.getLong(event.getViewName(), oldRow, column)
        : null;

    boolean isNew = !Objects.equals(relId, oldId) && Objects.equals(relId, parentRowId);
    boolean changed = !Objects.equals(oldId, parentRowId)
        && oldRow != null;

    if ((isNew || changed) && getGridPresenter() != null) {
      getGridPresenter().handleAction(Action.REFRESH);
    }
  }

  private void setEnabled(FormView form, IsRow formRow) {
    if (form == null && formRow == null) {
      return;
    }

    if (!BeeUtils.inListSame(form.getViewName(), ProjectConstants.VIEW_PROJECTS,
        ProjectConstants.VIEW_PROJECT_STAGES)) {
      return;
    }

    if (DataUtils.isNewRow(formRow)) {
      getGridPresenter().getMainView().setEnabled(true);
      return;
    }

    getGridPresenter().getMainView().setEnabled(ProjectsHelper.isProjectOwner(form, formRow)
        || ProjectsHelper.isProjectUser(form, formRow));

  }

}