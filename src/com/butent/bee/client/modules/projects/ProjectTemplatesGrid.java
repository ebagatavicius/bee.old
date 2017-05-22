package com.butent.bee.client.modules.projects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Map;

public class ProjectTemplatesGrid extends AbstractGridInterceptor {

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    HeaderView headerView = presenter.getHeader();

    headerView.clearCommandPanel();

    if (BeeKeeper.getUser().canCreateData(ProjectConstants.VIEW_PROJECTS)
        && !presenter.getGridView().isChild()) {
      FaLabel createProject = new FaLabel(FontAwesome.ROCKET);
      createProject.setTitle(Localized.dictionary().prjCreateFromTasks());
      createProject.addClickHandler(clickEvent -> createProjectFromSelection());

      presenter.getHeader().addCommandItem(createProject);
    }

    super.afterCreatePresenter(presenter);
  }

  @Override
  public GridInterceptor getInstance() {
    return new ProjectTemplatesGrid();
  }

  private void createProjectFromSelection() {
    final GridView gridView = getGridPresenter().getGridView();
    final IsRow selectedRow = gridView.getActiveRow();

    if (selectedRow == null) {
      gridView.notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }
    createProject(Data.getDataInfo(gridView.getViewName()), selectedRow, null);
  }

  private static void copyTasks(final BeeRow prjRow, final IsRow tmlRow,
      final RowCallback callback) {

    final DataInfo stagesView = Data.getDataInfo(ProjectConstants.VIEW_PROJECT_STAGES);
    final DataInfo taskTemplatesView = Data.getDataInfo(TaskConstants.VIEW_TASK_TEMPLATES);

    final List<String> copyCols = Lists.newArrayList(TaskConstants.COL_SUMMARY,
        TaskConstants.COL_DESCRIPTION, TaskConstants.COL_PRIORITY, TaskConstants.COL_TASK_TYPE,
        TaskConstants.COL_EXPECTED_DURATION, ClassifierConstants.COL_COMPANY,
        ClassifierConstants.COL_CONTACT, TaskConstants.COL_REMINDER, ProjectConstants.COL_PROJECT,
        ProjectConstants.COL_PROJECT_STAGE);

    final List<BeeColumn> taskCopyCols =
        Lists.newArrayList(Data.getColumns(ProjectConstants.VIEW_PROJECT_TEMPLATE_TASK_COPY,
            copyCols));

    final BeeRowSet taskCopy = new BeeRowSet(ProjectConstants.VIEW_PROJECT_TEMPLATE_TASK_COPY,
        taskCopyCols);

    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_STAGES, stagesView.getColumnNames(false),
        Filter.equals(ProjectConstants.COL_PROJECT, prjRow.getId()),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(final BeeRowSet stages) {

            Queries.getRowSet(TaskConstants.VIEW_TASK_TEMPLATES,
                taskTemplatesView.getColumnNames(false),
                Filter.equals(ProjectConstants.COL_PROJECT_TEMPLATE, tmlRow.getId()),
                new Queries.RowSetCallback() {
                  @Override
                  public void onSuccess(BeeRowSet taskTemplates) {
                    if (taskTemplates.isEmpty()) {
                      createProjectUsers(prjRow, tmlRow, callback);
                      return;
                    }

                    Map<Long, Long> stageCache = Maps.newConcurrentMap();

                    for (int i = 0; i < stages.getNumberOfRows(); i++) {
                      Long tmlId = stages.getLong(i, stagesView.getColumnIndex(
                          ProjectConstants.COL_STAGE_TEMPLATE));
                      if (DataUtils.isId(tmlId)) {
                        stageCache.put(tmlId, stages.getRow(i).getId());
                      }
                    }

                    for (int i = 0; i < taskTemplates.getNumberOfRows(); i++) {
                      BeeRow row = taskCopy.addEmptyRow();
                      for (String col : copyCols) {
                        switch (col) {
                          case ProjectConstants.COL_PROJECT_STAGE:
                            Long id = taskTemplates.getLong(i, taskTemplatesView.getColumnIndex(
                                ProjectConstants.COL_STAGE_TEMPLATE));

                            if (DataUtils.isId(id)) {
                              row.setValue(taskCopy.getColumnIndex(col),
                                  stageCache.get(id));
                            }
                            break;
                          case ProjectConstants.COL_PROJECT:
                            row.setValue(taskCopy.getColumnIndex(col), prjRow.getId());
                            break;
                          default:
                            row.setValue(taskCopy.getColumnIndex(col),
                                taskTemplates.getString(i, col));
                            break;
                        }
                      }
                    }
                    Queries.insertRows(taskCopy, new RpcCallback<RowInfoList>() {
                      @Override
                      public void onSuccess(RowInfoList result) {
                        createProjectUsers(prjRow, tmlRow, callback);
                      }
                    });
                  }
                });

          }
        });
  }

  public static void createChildData(DataInfo sourceTemplateInfo, DataInfo targetInfo,
                                     IsRow templateRow, IsRow projectRow,
                                     Callback<Integer> afterCreate) {
    final BeeRowSet childRowSet = new BeeRowSet(targetInfo.getViewName(),
      Lists.newArrayList(targetInfo.getColumns()));

    Queries.getRowSet(sourceTemplateInfo.getViewName(), sourceTemplateInfo.getColumnNames(false),
      Filter.equals(ProjectConstants.COL_PROJECT_TEMPLATE, BeeUtils.toString(templateRow.getId())),
      new Queries.RowSetCallback() {

        @Override
        public void onSuccess(BeeRowSet tmlStagesRows) {
          if (tmlStagesRows.isEmpty()) {
            if (afterCreate != null) {
              afterCreate.onSuccess(tmlStagesRows.getNumberOfRows());
            }
            return;
          }

          for (IsRow tmlStageRow : tmlStagesRows) {
            BeeRow row = RowFactory.createEmptyRow(targetInfo, true);
            ProjectsHelper.copyProjectTemplateData(sourceTemplateInfo, tmlStageRow, targetInfo,
              row);

            if (targetInfo.containsColumn(ProjectConstants.COL_STAGE_TEMPLATE)) {
              RelationUtils.updateRow(targetInfo, ProjectConstants.COL_STAGE_TEMPLATE, row,
                sourceTemplateInfo, tmlStageRow, true);
            }

            if (targetInfo.containsColumn(ProjectConstants.COL_PROJECT)) {
              RelationUtils.updateRow(targetInfo, ProjectConstants.COL_PROJECT, row,
                Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), projectRow, true);
            }
            childRowSet.addRow(row);
          }

          for (BeeColumn column : targetInfo.getColumns()) {
            if (!column.isReadOnly() && column.getLevel() == 0) {
              continue;
            }
            childRowSet.removeColumn(childRowSet.getColumnIndex(column.getId()));
          }
          Queries.insertRows(childRowSet, new RpcCallback<RowInfoList>() {
            @Override
            public void onSuccess(RowInfoList result) {
              if (afterCreate != null) {
                afterCreate.onSuccess(result.size());
              }
            }
          });
        }
      });
  }

  public static void createProject(DataInfo templateData, final IsRow templateRow,
      final RowCallback callback) {
    DataInfo prjDataInfo = Data.getDataInfo(ProjectConstants.VIEW_PROJECTS);
    BeeRow prjRow = RowFactory.createEmptyRow(prjDataInfo, true);

    ProjectsHelper.copyProjectTemplateData(templateData, templateRow, prjDataInfo, prjRow);
    RelationUtils.updateRow(prjDataInfo, ProjectConstants.COL_PROJECT_TEMPLATE, prjRow,
      templateData, templateRow, true);
    RowFactory.createRow(ProjectConstants.FORM_NEW_PROJECT_FROM_TEMPLATE,
        prjDataInfo.getNewRowCaption(), prjDataInfo, prjRow, Modality.ENABLED, null, null, null,
        new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            createStages(result, templateRow, callback);
          }
        });
  }

  private static void createProjectContacts(final BeeRow prjRow, IsRow tmlRow,
      final RowCallback callback) {

    DataInfo templateContacts = Data.getDataInfo(ProjectConstants.VIEW_PROJECT_TEMPLATE_CONTACTS);
    DataInfo projectContacts = Data.getDataInfo(ProjectConstants.VIEW_PROJECT_CONTACTS);

    createChildData(templateContacts, projectContacts, tmlRow, prjRow,
      afterCreate -> {
        if (callback == null) {
          openProjectFullForm(prjRow.getId());
        } else {
          callback.onSuccess(prjRow);
        }
      });
  }

  private static void createProjectUsers(final BeeRow prjRow, final IsRow tmlRow,
      final RowCallback callback) {
    DataInfo templateUsers = Data.getDataInfo(ProjectConstants.VIEW_PROJECT_TEMPLATE_USERS);
    DataInfo projectUsers = Data.getDataInfo(ProjectConstants.VIEW_PROJECT_USERS);

    createChildData(templateUsers, projectUsers, tmlRow, prjRow,
      afterCreate -> createProjectContacts(prjRow, tmlRow, callback));
  }

  private static void createStages(final BeeRow prjRow, final IsRow tmlRow,
      final RowCallback callback) {

    final DataInfo tmlStagesInfo = Data.getDataInfo(ProjectConstants.VIEW_PROJECT_TEMPLATE_STAGES);
    final DataInfo stageInfo = Data.getDataInfo(ProjectConstants.VIEW_PROJECT_STAGES);
    createChildData(tmlStagesInfo, stageInfo, tmlRow, prjRow,
      afterCreate -> copyTasks(prjRow, tmlRow, callback));
  }

  private static void openProjectFullForm(long projectId) {
    RowEditor.openForm(ProjectConstants.FORM_PROJECT,
        Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), Filter.compareId(projectId),
        Opener.NEW_TAB);
  }
}
