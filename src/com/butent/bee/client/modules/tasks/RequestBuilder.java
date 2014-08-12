package com.butent.bee.client.modules.tasks;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.modules.administration.AdministrationConstants;

public class RequestBuilder extends AbstractFormInterceptor {

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof FileCollector) {
      ((FileCollector) widget).bindDnd(getFormView());
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new RequestBuilder();
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, final ReadyForInsertEvent event) {
    event.consume();
    String viewName = getFormView().getViewName();

    Queries.insert(viewName, event.getColumns(), event.getValues(), event.getChildren(),
        new RowInsertCallback(viewName, event.getSourceId()) {
          @Override
          public void onSuccess(BeeRow result) {
            super.onSuccess(result);
            event.getCallback().onSuccess(result);
            createFiles(result.getId());
          }
        });
  }

  private void createFiles(Long requestId) {
    Widget widget = getFormView().getWidgetByName("Files");

    if (widget instanceof FileCollector && !((FileCollector) widget).isEmpty()) {
      FileUtils.commitFiles(((FileCollector) widget).getFiles(), VIEW_REQUEST_FILES, COL_REQUEST,
          requestId, AdministrationConstants.COL_FILE, COL_CAPTION);

      ((FileCollector) widget).clear();
    }
  }
}
