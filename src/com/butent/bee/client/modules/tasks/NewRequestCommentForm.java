package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskEvent;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class NewRequestCommentForm extends AbstractFormInterceptor {
  private static final String WIDGET_FILES = "Files";

  private Long requestId;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof FileCollector) {
      ((FileCollector) widget).bindDnd(getFormView());
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new NewRequestCommentForm(requestId);
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

    Long currentUserId = BeeKeeper.getUser().getUserId();
    DateTime time = new DateTime();

    List<BeeColumn> columns =
        Data.getColumns(viewName,
            Lists.newArrayList(COL_REQUEST, COL_PUBLISHER, COL_PUBLISH_TIME, COL_COMMENT,
                COL_EVENT, COL_EVENT_PROPERTIES));

    List<String> values =
        Lists.newArrayList(BeeUtils.toString(getRequestId()),
            BeeUtils.toString(currentUserId), BeeUtils.toString(time.getTime()), comment, BeeUtils
                .toString(TaskEvent.COMMENT.ordinal()), null);

    Queries.insert(viewName, columns, values, null, getEventRowCallback(VIEW_REQUEST_FILES,
        getRequestId(), files, getEventsIdCallBack(viewName, event),
        getFilesUploadedCallback((FileCollector) widget)));
  }

  NewRequestCommentForm(Long requestId) {
    this();
    this.requestId = requestId;
  }

  public static void createFiles(final String eventFilesViewName, final long projectId,
      final Long eventId, final List<FileInfo> files, final Callback<Boolean> allUploadCallback) {
    if (files == null) {
      return;
    }

    if (files.isEmpty()) {
      return;
    }

    Assert.notEmpty(eventFilesViewName);
    Assert.isPositive(projectId);

    final Holder<Integer> counter = Holder.of(0);
    final List<BeeColumn> columns =
        Data.getColumns(eventFilesViewName,
            Lists.newArrayList(COL_REQUEST, COL_REQUEST_EVENT, AdministrationConstants.COL_FILE,
                COL_CAPTION));

    for (final FileInfo fileInfo : files) {
      FileUtils.uploadFile(fileInfo, result -> {
        List<String> values = Lists.newArrayList(BeeUtils.toString(projectId),
            BeeUtils.toString(eventId), BeeUtils.toString(result.getId()), fileInfo.getCaption());

        Queries.insert(eventFilesViewName, columns, values, null, new RowCallback() {
          @Override
          public void onSuccess(BeeRow row) {
            counter.set(counter.get() + 1);

            if (counter.get() == files.size()) {
              if (allUploadCallback != null) {
                allUploadCallback.onSuccess(Boolean.TRUE);
                RowInsertEvent.fire(BeeKeeper.getBus(), eventFilesViewName, row, null);
              }
            }
          }
        });
      });
    }
  }

  public static RowCallback getEventRowCallback(final String eventFilesViewName,
      final long requestId, final List<FileInfo> files, final Callback<BeeRow> idCallback,
      final Callback<Boolean> uploaded) {
    return new RowCallback() {

      @Override
      public void onSuccess(BeeRow result) {
        if (idCallback != null) {
          idCallback.onSuccess(result);
        }
        createFiles(eventFilesViewName, requestId, result.getId(), files, uploaded);
      }
    };
  }

  private static Callback<BeeRow> getEventsIdCallBack(final String viewName,
      ReadyForInsertEvent event) {
    return new Callback<BeeRow>() {

      @Override
      public void onSuccess(BeeRow result) {
        RowInsertEvent.fire(BeeKeeper.getBus(), viewName, result, event.getSourceId());
        event.getCallback().onSuccess(result);
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

  private NewRequestCommentForm() {
    this.requestId = null;
  }

  private Long getRequestId() {
    return requestId;
  }
}