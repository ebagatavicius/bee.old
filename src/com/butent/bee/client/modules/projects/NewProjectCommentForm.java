package com.butent.bee.client.modules.projects;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.projects.ProjectConstants.ProjectEvent;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class NewProjectCommentForm extends AbstractFormInterceptor {
  private static final String WIDGET_FILES = "Files";

  private Long projectId;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof FileCollector) {
      ((FileCollector) widget).bindDnd(getFormView());
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new NewProjectCommentForm(projectId);
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    event.consume();
    final FormView form = getFormView();

    if (form == null) {
      return;
    }

    IsRow row = form.getActiveRow();

    if (row == null) {
      return;
    }

    String viewName = form.getViewName();

    String comment = null;

    if (!BeeUtils.isNegative(form.getDataIndex(COL_COMMENT))) {
      comment = row.getString(form.getDataIndex(COL_COMMENT));
    }

    List<FileInfo> files = null;
    final Widget widget = getFormView().getWidgetByName(WIDGET_FILES);

    if (widget instanceof FileCollector) {
      files = ((FileCollector) widget).getFiles();
    }

    ProjectsHelper.registerProjectEvent(viewName, VIEW_PROJECT_FILES, ProjectEvent.COMMENT,
        getProjectId(), comment, files, getEventsIdCallBack(viewName, event.getSourceId()),
        getFilesUploadedCallback((FileCollector) widget));
  }

  public NewProjectCommentForm(Long projectId) {
    this();
    this.projectId = projectId;
  }

  private static Callback<BeeRow> getEventsIdCallBack(final String viewName,
      final String sourceId) {
    return new Callback<BeeRow>() {

      @Override
      public void onSuccess(BeeRow result) {
        RowInsertEvent.fire(BeeKeeper.getBus(), viewName, result, sourceId);
        Popup.getActivePopup().close();
      }
    };
  }

  private static Callback<Boolean> getFilesUploadedCallback(final FileCollector collector) {
    return new Callback<Boolean>() {

      @Override
      public void onSuccess(Boolean result) {
        collector.clear();
      }
    };
  }

  private NewProjectCommentForm() {
    this.projectId = null;
  }

  private Long getProjectId() {
    return projectId;
  }

}
