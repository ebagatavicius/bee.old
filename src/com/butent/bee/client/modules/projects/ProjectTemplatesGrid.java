package com.butent.bee.client.modules.projects;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
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
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class ProjectTemplatesGrid extends AbstractGridInterceptor {

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    HeaderView headerView = presenter.getHeader();

    headerView.clearCommandPanel();

    if (BeeKeeper.getUser().canCreateData(ProjectConstants.VIEW_PROJECTS)
        && !presenter.getGridView().isChild()) {
      FaLabel createProject = new FaLabel(FontAwesome.ROCKET);
      createProject.setTitle(Localized.getConstants().prjCreateFromTasks());
      createProject.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent arg0) {
          createProjectFromSelecrion();
        }
      });

      presenter.getHeader().addCommandItem(createProject);
    }

    super.afterCreatePresenter(presenter);
  }

  @Override
  public GridInterceptor getInstance() {
    return new ProjectTemplatesGrid();
  }

  private void createProjectFromSelecrion() {
    final GridView gridView = getGridPresenter().getGridView();

    final IsRow selectedRow = gridView.getActiveRow();

    if (selectedRow == null) {
      gridView.notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }

    createProject(Data.getDataInfo(gridView.getViewName()), selectedRow, null);
  }

  public static void createProject(DataInfo teplateData, final IsRow templateRow,
      final RowCallback callback) {
    int idxTMLName = teplateData.getColumnIndex(ProjectConstants.COL_PROJECT_NAME);
    int idxTMLPriority = teplateData.getColumnIndex(ProjectConstants.COL_PROJECT_PRIORITY);
    int idxTMLType = teplateData.getColumnIndex(ProjectConstants.COL_PROJECT_TYPE);
    int idxTMLCompany = teplateData.getColumnIndex(ClassifierConstants.COL_COMPANY);
    int idxTMLCompanyName = teplateData.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);
    int idxTMLCompanyTypeName = teplateData.getColumnIndex(ProjectConstants.ALS_COMPANY_TYPE_NAME);
    int idxTMLCategory = teplateData.getColumnIndex(ProjectConstants.COL_PROJECT_CATEGORY);
    int idxTMLCategoryName = teplateData.getColumnIndex(ProjectConstants.ALS_CATEGORY_NAME);
    int idxTMLExpectedDuration = teplateData.getColumnIndex(ProjectConstants.COL_EXPECTED_DURATION);
    int idxTMLTimeUnit = teplateData.getColumnIndex(ProjectConstants.COL_PROJECT_TIME_UNIT);
    int idxTMLPrice = teplateData.getColumnIndex(ProjectConstants.COL_PROJECT_PRICE);
    int idxTMLContractPrice = teplateData.getColumnIndex(ProjectConstants.COL_CONTRACT_PRICE);
    int idxTMLCurrency = teplateData.getColumnIndex(ProjectConstants.COL_PROJECT_CURENCY);
    int idxTMLDescription = teplateData.getColumnIndex(ProjectConstants.COL_DESCRIPTION);
    int idxTMLDefaultStage =
        teplateData.getColumnIndex(ProjectConstants.COL_DEFAULT_PROJECT_TEMPLATE_STAGE);

    DataInfo prjDataInfo = Data.getDataInfo(ProjectConstants.VIEW_PROJECTS);
    BeeRow prjRow = RowFactory.createEmptyRow(prjDataInfo, true);

    int idxPrjName = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_NAME);
    int idxPrjPriority = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_PRIORITY);
    int idxPrjType = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_TYPE);
    int idxPrjCompany = prjDataInfo.getColumnIndex(ClassifierConstants.COL_COMPANY);
    int idxPrjCompanyName = prjDataInfo.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);
    int idxPrjCompanyTypeName = prjDataInfo.getColumnIndex(ProjectConstants.ALS_COMPANY_TYPE_NAME);
    int idxPrjCategory = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_CATEGORY);
    int idxPrjCategoryName = prjDataInfo.getColumnIndex(ProjectConstants.ALS_CATEGORY_NAME);
    int idxPrjExpectedDuration = prjDataInfo.getColumnIndex(ProjectConstants.COL_EXPECTED_DURATION);
    int idxPrjTimeUnit = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_TIME_UNIT);
    int idxPrjPrice = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_PRICE);
    int idxPrjContractPrice = prjDataInfo.getColumnIndex(ProjectConstants.COL_CONTRACT_PRICE);
    int idxPrjCurrency = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_CURENCY);
    int idxPrjDescription = prjDataInfo.getColumnIndex(ProjectConstants.COL_DESCRIPTION);
    int idxPrjTemplate = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_TEMPLATE);

    prjRow.setValue(idxPrjTemplate, templateRow.getId());
    prjRow.setValue(idxPrjName, templateRow.getValue(idxTMLName));
    prjRow.setValue(idxPrjPriority, templateRow.getValue(idxTMLPriority));
    prjRow.setValue(idxPrjType, templateRow.getValue(idxTMLType));
    prjRow.setValue(idxPrjCompany, templateRow.getValue(idxTMLCompany));
    prjRow.setValue(idxPrjCompanyName, templateRow.getValue(idxTMLCompanyName));
    prjRow.setValue(idxPrjCompanyTypeName, templateRow.getValue(idxTMLCompanyTypeName));
    prjRow.setValue(idxPrjCategory, templateRow.getValue(idxTMLCategory));
    prjRow.setValue(idxPrjCategoryName, templateRow.getValue(idxTMLCategoryName));

    prjRow.setValue(idxPrjExpectedDuration, templateRow.getValue(idxTMLExpectedDuration));

    prjRow.setValue(idxPrjTimeUnit, templateRow.getValue(idxTMLTimeUnit));
    prjRow.setValue(idxPrjPrice, templateRow.getValue(idxTMLPrice));
    prjRow.setValue(idxPrjContractPrice, templateRow.getValue(idxTMLContractPrice));
    prjRow.setValue(idxPrjCurrency, templateRow.getValue(idxTMLCurrency));
    prjRow.setValue(idxPrjDescription, templateRow.getValue(idxTMLDescription));
    prjRow.setProperty(ProjectConstants.COL_DEFAULT_PROJECT_TEMPLATE_STAGE,
        templateRow.getString(idxTMLDefaultStage));

    RowFactory.createRow(ProjectConstants.FORM_NEW_PROJECT_FROM_TEMPLATE,
        prjDataInfo.getNewRowCaption(), prjDataInfo, prjRow, null, null,
        new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            createInitialStage(result, templateRow, callback);
          }
        });
  }

  private static void createProjectContacts(final BeeRow prjRow, IsRow tmlRow,
      final RowCallback callback) {

    final List<BeeColumn> personCols =
        Lists.newArrayList(Data.getColumns(ProjectConstants.VIEW_PROJECT_CONTACTS,
            Lists.newArrayList(ProjectConstants.COL_PROJECT,
                ClassifierConstants.COL_COMPANY_PERSON)));

    final BeeRowSet persons = new BeeRowSet(ProjectConstants.VIEW_PROJECT_CONTACTS, personCols);

    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_TEMPLATE_CONTACTS, Lists.newArrayList(
        ClassifierConstants.COL_COMPANY_PERSON),
        Filter.equals(ProjectConstants.COL_PROJECT_TEMPLATE, BeeUtils.toString(tmlRow.getId())),
        new Queries.RowSetCallback() {

          @Override
          public void onSuccess(BeeRowSet tmlPersons) {
            if (tmlPersons.isEmpty()) {
              if (callback == null) {
                openProjectFullForm(prjRow.getId());
              } else {
                callback.onSuccess(prjRow);
              }
              return;
            }

            for (int i = 0; i < tmlPersons.getNumberOfRows(); i++) {
              BeeRow row = persons.addEmptyRow();
              row.setValue(persons.getColumnIndex(ProjectConstants.COL_PROJECT), prjRow.getId());
              row.setValue(persons.getColumnIndex(ClassifierConstants.COL_COMPANY_PERSON),
                  tmlPersons.getLong(i, ClassifierConstants.COL_COMPANY_PERSON));
            }

            Queries.insertRows(persons, new RpcCallback<RowInfoList>() {
              @Override
              public void onSuccess(RowInfoList result) {
                if (callback == null) {
                  openProjectFullForm(prjRow.getId());
                } else {
                  callback.onSuccess(prjRow);
                }
              }
            });
          }
        });
  }

  private static void createProjectUsers(final BeeRow prjRow, final IsRow tmlRow,
      final RowCallback callback) {

    final List<BeeColumn> usersCols =
        Lists.newArrayList(Data.getColumns(ProjectConstants.VIEW_PROJECT_USERS,
            Lists.newArrayList(ProjectConstants.COL_PROJECT,
                AdministrationConstants.COL_USER, ProjectConstants.COL_NOTES,
                ProjectConstants.COL_RATE, ProjectConstants.COL_PROJECT_CURENCY)));

    final BeeRowSet users = new BeeRowSet(ProjectConstants.VIEW_PROJECT_USERS, usersCols);

    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_TEMPLATE_USERS, Lists.newArrayList(
        AdministrationConstants.COL_USER, ProjectConstants.COL_NOTES,
        ProjectConstants.COL_RATE, ProjectConstants.COL_PROJECT_CURENCY),
        Filter.equals(ProjectConstants.COL_PROJECT_TEMPLATE, BeeUtils.toString(tmlRow.getId())),
        new Queries.RowSetCallback() {

          @Override
          public void onSuccess(BeeRowSet tmlUsers) {
            if (tmlUsers.isEmpty()) {
              createProjectContacts(prjRow, tmlRow, callback);
              return;
            }

            for (int i = 0; i < tmlUsers.getNumberOfRows(); i++) {
              BeeRow row = users.addEmptyRow();
              row.setValue(users.getColumnIndex(ProjectConstants.COL_PROJECT), prjRow.getId());
              row.setValue(users.getColumnIndex(AdministrationConstants.COL_USER),
                  tmlUsers.getLong(i, AdministrationConstants.COL_USER));
              row.setValue(users.getColumnIndex(ProjectConstants.COL_NOTES),
                  tmlUsers.getLong(i, ProjectConstants.COL_NOTES));
              row.setValue(users.getColumnIndex(ProjectConstants.COL_RATE),
                  tmlUsers.getLong(i, ProjectConstants.COL_RATE));
              row.setValue(users.getColumnIndex(ProjectConstants.COL_PROJECT_CURENCY),
                  tmlUsers.getLong(i, ProjectConstants.COL_PROJECT_CURENCY));
            }

            Queries.insertRows(users, new RpcCallback<RowInfoList>() {
              @Override
              public void onSuccess(RowInfoList result) {
                createProjectContacts(prjRow, tmlRow, callback);
                return;
              }
            });
          }
        });
  }

  private static void createInitialStage(final BeeRow prjRow, final IsRow tmlRow,
      final RowCallback callback) {

    final List<String> copyCols = Lists.newArrayList(ProjectConstants.COL_PROJECT,
        ProjectConstants.COL_STAGE_NAME, ProjectConstants.COL_EXPECTED_DURATION,
        ProjectConstants.COL_EXPENSES, ProjectConstants.COL_PROJECT_CURENCY,
        ProjectConstants.COL_STAGE_TEMPLATE);

    final List<BeeColumn> stageCols =
        Lists.newArrayList(Data.getColumns(ProjectConstants.VIEW_PROJECT_STAGES, copyCols
            ));

    final BeeRowSet stages = new BeeRowSet(ProjectConstants.VIEW_PROJECT_STAGES, stageCols);

    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_TEMPLATE_STAGES, Lists.newArrayList(
        ProjectConstants.COL_STAGE_NAME, ProjectConstants.COL_EXPECTED_DURATION,
        ProjectConstants.COL_EXPENSES, ProjectConstants.COL_PROJECT_CURENCY),
        Filter.equals(ProjectConstants.COL_PROJECT_TEMPLATE, BeeUtils.toString(tmlRow.getId())),
        new Queries.RowSetCallback() {

          @Override
          public void onSuccess(BeeRowSet stageTml) {
            if (stageTml.isEmpty()) {
              createProjectUsers(prjRow, tmlRow, callback);
              return;
            }

            for (int i = 0; i < stageTml.getNumberOfRows(); i++) {
              BeeRow row = stages.addEmptyRow();
              for (String col : copyCols) {
                switch (col) {
                  case ProjectConstants.COL_STAGE_TEMPLATE:
                    row.setValue(stages.getColumnIndex(col), stageTml.getRow(i).getId());
                    break;
                  case ProjectConstants.COL_PROJECT:
                    row.setValue(stages.getColumnIndex(col), prjRow.getId());
                    break;
                  default:
                    row.setValue(stages.getColumnIndex(col), stageTml.getString(i, col));
                    break;
                }
              }
            }

            Queries.insertRows(stages, new RpcCallback<RowInfoList>() {
              @Override
              public void onSuccess(RowInfoList result) {
                createProjectUsers(prjRow, tmlRow, callback);
              }
            });

          }
        }
        );
  }

  private static void openProjectFullForm(long projectId) {
    RowEditor.openForm(ProjectConstants.FORM_PROJECT,
        Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), projectId, Opener.NEW_TAB);
  }

}