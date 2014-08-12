package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class RequestEditor extends AbstractFormInterceptor {

  private static class SaveCallback extends RowUpdateCallback {

    private final FormView formView;

    public SaveCallback(FormView formView) {
      super(formView.getViewName());
      this.formView = formView;
    }

    @Override
    public void onSuccess(BeeRow result) {
      super.onSuccess(result);
      formView.updateRow(result, false);
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    IsRow activeRow = form.getActiveRow();

    boolean edited = (activeRow != null) && form.isEditing();

    if (edited) {
      boolean finished =
          activeRow.getDateTime(form.getDataIndex(TaskConstants.COL_REQUEST_FINISHED)) != null;
      boolean registred = (!finished)
          && (activeRow.getDateTime(form.getDataIndex(TaskConstants.COL_REQUEST_DATE)) != null);

      if (registred) {
        Button btnFinish = new Button(Localized.getConstants().requestFinish());
        btnFinish.addClickHandler(new ClickHandler() {

          @Override
          public void onClick(ClickEvent event) {
            finishRequest();
          }
        });

        header.addCommandItem(btnFinish);

        Button btnFinishToTask = new Button(Localized.getConstants().requestFinishToTask());
        btnFinishToTask.addClickHandler(new ClickHandler() {

          @Override
          public void onClick(ClickEvent event) {
            toTaskAndFinish();
          }

        });
        // TODO: header.addCommandItem(btnFinishToTask);
      }
      form.setEnabled(!finished);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new RequestEditor();
  }

  @Override
  public boolean onStartEdit(final FormView form, IsRow row, ScheduledCommand focusCommand) {
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
    return true;
  }

  private void finishRequest() {
    final FormView form = getFormView();
    final IsRow activeRow = form.getActiveRow();

    boolean edited = (activeRow != null) && form.isEditing();

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

        List<String> oldValues = Lists.newArrayList(activeRow
            .getString(form.getDataIndex(TaskConstants.COL_REQUEST_FINISHED)));

        List<String> newValues = Lists.newArrayList(BeeUtils.toString(new DateTime().getTime()));

        columns.add(DataUtils.getColumn(TaskConstants.COL_REQUEST_RESULT, form.getDataColumns()));
        oldValues.add(activeRow.getString(form.getDataIndex(TaskConstants.COL_REQUEST_RESULT)));
        newValues.add(value);

        Queries.update(form.getViewName(), activeRow.getId(), activeRow.getId(),
            columns, oldValues, newValues, form.getChildrenForUpdate(), new SaveCallback(form));
      }
    }, null, BeeConst.UNDEF, null, 300, CssUnit.PX);
  }

  private void toTaskAndFinish() {
    // TODO:
  }
}
