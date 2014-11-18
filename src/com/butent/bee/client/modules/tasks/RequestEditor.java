package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.UserInfo;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewFactory.SupplierKind;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;
import java.util.Map;

public class RequestEditor extends AbstractFormInterceptor {

  private static final String WIDGET_MANGAER_NAME = "Manager";
  private static final String WIDGET_FILES_NAME = "Files";
  private static final String WIDGET_RESULT_PROPERTIES = "ResultProperties";
  private static final String STYLE_PREFIX = TaskConstants.CRM_STYLE_PREFIX + "request-";

  private static final String STYLE_PROPERTY_CAPTION = STYLE_PREFIX + "prop-caption";
  private static final String STYLE_PROPERTY_DATA = STYLE_PREFIX + "prop-data";

  private final UserInfo currentUser = BeeKeeper.getUser();

  private static class FinishSaveCallback extends RowUpdateCallback {

    private final FormView formView;

    public FinishSaveCallback(FormView formView) {
      super(formView.getViewName());
      this.formView = formView;
    }

    @Override
    public void onSuccess(BeeRow result) {
      super.onSuccess(result);
      formView.updateRow(result, true);
      formView.refresh();
      formView.setEnabled(false);
    }
  }

  FlowPanel resultProperties;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(WIDGET_RESULT_PROPERTIES, name) && widget instanceof FlowPanel) {
      resultProperties = (FlowPanel) widget;
    }
    super.afterCreateWidget(name, widget, callback);
  }


  @Override
  public void afterRefresh(FormView form, IsRow row) {
    boolean finished =
        row.getDateTime(form.getDataIndex(TaskConstants.COL_REQUEST_FINISHED)) != null;

    if (finished) {
      HeaderView header = form.getViewPresenter().getHeader();
      header.clearCommandPanel();
      showResultProperties(form, row);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new RequestEditor();
  }

  @Override
  public boolean onStartEdit(final FormView form, final IsRow row, ScheduledCommand focusCommand) {
    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    boolean finished =
        row.getDateTime(form.getDataIndex(TaskConstants.COL_REQUEST_FINISHED)) != null;

    final Widget fileWidget = form.getWidgetByName(PROP_FILES);

    if (fileWidget instanceof FileGroup) {
      ((FileGroup) fileWidget).clear();

      ParameterList params = TasksKeeper.createArgs(SVC_GET_REQUEST_FILES);
      params.addDataItem(COL_REQUEST, row.getId());

      BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(form);

          if (response.hasErrors()) {
            return;
          }
          List<FileInfo> files = FileInfo.restoreCollection((String) response.getResponse());

          if (!files.isEmpty()) {
            for (FileInfo file : files) {
              ((FileGroup) fileWidget).addFile(file);
            }
          }
        }
      });
    }

    if (!finished) {
      Button btnFinish = new Button(Localized.getConstants().requestFinish());
      btnFinish.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          finishRequest(form, row);
        }
      });

      header.addCommandItem(btnFinish);
    }

    if (currentUser.canCreateData(TaskConstants.VIEW_TASKS) && !finished) {
      Button btnFinishToTask = new Button(Localized.getConstants().requestFinishToTask());
      btnFinishToTask.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          toTaskAndFinish(form, row);
        }
      });

      header.addCommandItem(btnFinishToTask);
    }

    if (finished) {
      form.setEnabled(false);
    }

    showResultProperties(form, row);
    return true;
  }

  private static void finishRequest(final FormView form, final IsRow row) {

    boolean edited = (row != null) && form.isEditing();

    if (!edited) {
      Global.showError(Localized.getConstants().actionCanNotBeExecuted());
      return;
    }

    Global.inputString(Localized.getConstants().requestFinishing(), Localized.getConstants()
        .specifyResult(), new StringCallback(true) {
      @Override
      public void onSuccess(String value) {
        List<BeeColumn> columns = Lists.newArrayList(DataUtils
            .getColumn(TaskConstants.COL_REQUEST_FINISHED, form.getDataColumns()));

        List<String> oldValues = Lists.newArrayList(row
            .getString(form.getDataIndex(TaskConstants.COL_REQUEST_FINISHED)));
        List<String> newValues = Lists.newArrayList(BeeUtils.toString(new DateTime().getTime()));

        columns.add(DataUtils.getColumn(TaskConstants.COL_REQUEST_RESULT, form.getDataColumns()));

        oldValues.add(row.getString(form.getDataIndex(TaskConstants.COL_REQUEST_RESULT)));
        newValues.add(value);

        Queries.update(form.getViewName(), row.getId(), row.getVersion(),
            columns, oldValues, newValues, form.getChildrenForUpdate(),
            new FinishSaveCallback(form));
      }
    }, null, BeeConst.UNDEF, null, 300, CssUnit.PX);
  }

  private static ClickHandler getTaskLinkClickHandler(final Long id) {
    return new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {
        DataInfo dataInfo = Data.getDataInfo(VIEW_TASKS);
        RowEditor.openForm(FORM_TASK, dataInfo, id, Opener.NEW_TAB);
      }
    };
  }

  private static void toTaskAndFinish(final FormView form, final IsRow reqRow) {
    boolean edited = (reqRow != null) && form.isEditing();

    if (!edited) {
      Global.showError(Localized.getConstants().actionCanNotBeExecuted());
      return;
    }

    DataInfo taskDataInfo = Data.getDataInfo(TaskConstants.VIEW_TASKS);
    BeeRow taskRow = RowFactory.createEmptyRow(taskDataInfo, true);

    taskRow.setValue(taskDataInfo.getColumnIndex(ClassifierConstants.COL_COMPANY),
        reqRow.getLong(form.getDataIndex(COL_REQUEST_CUSTOMER)));

    taskRow.setValue(taskDataInfo.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME),
        reqRow.getString(form.getDataIndex(COL_REQUEST_CUSTOMER_NAME)));

    taskRow.setValue(taskDataInfo.getColumnIndex(ClassifierConstants.COL_CONTACT), reqRow
        .getLong(form.getDataIndex(COL_REQUEST_CUSTOMER_PERSON)));

    taskRow.setValue(taskDataInfo.getColumnIndex(TaskConstants.ALS_OWNER_FIRST_NAME),
        reqRow.getString(form.getDataIndex(ClassifierConstants.COL_FIRST_NAME)));

    taskRow.setValue(taskDataInfo.getColumnIndex(TaskConstants.ALS_OWNER_LAST_NAME), reqRow
        .getString(form.getDataIndex(ClassifierConstants.COL_LAST_NAME)));

    taskRow.setValue(taskDataInfo.getColumnIndex(TaskConstants.COL_DESCRIPTION), reqRow
        .getString(form.getDataIndex(COL_REQUEST_CONTENT)));

    DataSelector managerSel = (DataSelector) form.getWidgetByName(WIDGET_MANGAER_NAME);
    Map<Long, FileInfo> files = Maps.newHashMap();
    FileGroup filesList = (FileGroup) form.getWidgetByName(WIDGET_FILES_NAME);

    for (FileInfo f : filesList.getFiles()) {
      files.put(f.getId(), f);
    }

    RowFactory.createRow(taskDataInfo.getNewRowForm(), null, taskDataInfo, taskRow, null,
        new TaskBuilder(files, BeeUtils.toLongOrNull(managerSel.getValue()), true),
        new RowCallback() {

          @Override
          public void onSuccess(BeeRow result) {
            LogUtils.getRootLogger().debug("Created task", result, result == null);

            int idxFinished = form.getDataIndex(TaskConstants.COL_REQUEST_FINISHED);
            int idxProp = form.getDataIndex(TaskConstants.COL_REQUEST_RESULT_PROPERTIES);
            List<BeeColumn> columns = Lists.newArrayList();
            List<String> oldValues = Lists.newArrayList();
            List<String> newValues = Lists.newArrayList();
            Map<String, String> propData = Maps.newHashMap();

            columns.add(DataUtils
                .getColumn(TaskConstants.COL_REQUEST_FINISHED, form.getDataColumns()));

            oldValues.add(reqRow.getString(idxFinished));

            newValues.add(BeeUtils.toString(new DateTime().getTime()));

            if (result != null && BeeUtils.isPositive(result.getNumberOfCells())) {
              columns.add(DataUtils
                  .getColumn(TaskConstants.COL_REQUEST_RESULT_PROPERTIES, form.getDataColumns()));
              oldValues.add(reqRow.getString(idxProp));

              String key = SupplierKind.FORM.getKey(TaskConstants.FORM_TASK);
              String ids = result.getString(0);

              propData.put(key, ids);
              newValues.add(Codec.beeSerialize(propData));
            }

            Queries.update(form.getViewName(), reqRow.getId(), reqRow.getVersion(),
                columns, oldValues, newValues, form.getChildrenForUpdate(),
                new FinishSaveCallback(form));

          }
        });
  }

  private void showResultProperties(final FormView form, final IsRow row) {
    if (resultProperties == null) {
      return;
    }
    int idxProp = form.getDataIndex(TaskConstants.COL_REQUEST_RESULT_PROPERTIES);

    resultProperties.clear();

    if (idxProp < 0) {
      return;
    }

    String data = row.getString(idxProp);

    if (BeeUtils.isEmpty(data)) {
      return;
    }

    Map<String, String> resultData = Codec.deserializeMap(data);

    for (String key : resultData.keySet()) {
      List<Long> rowIds = DataUtils.parseIdList(resultData.get(key));

      if (BeeUtils.isEmpty(rowIds)) {
        continue;
      }

      if (BeeUtils.isSuffix(key, TaskConstants.FORM_TASK)) {
        CustomDiv div = new CustomDiv(STYLE_PROPERTY_CAPTION);
        div.setText(Localized.getMessages().crmCreatedNewTasks(rowIds.size()));
        resultProperties.add(div);
      }

      for (Long id : rowIds) {
        if (!DataUtils.isId(id)) {
          continue;
        }

        InternalLink link = new InternalLink(BeeUtils.toString(id)
            + BeeConst.DEFAULT_ROW_SEPARATOR);
        link.addStyleName(STYLE_PROPERTY_DATA);
        link.addClickHandler(getTaskLinkClickHandler(id));

        resultProperties.add(link);
      }
    }
  }
}
