package com.butent.bee.client.modules.documents;

import com.google.gwt.core.client.Scheduler;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;

import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

final class DocumentBuilder extends AbstractFormInterceptor {

  private FileCollector collector;
  private UnboundSelector templSelector;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (widget instanceof FileCollector) {
      this.collector = (FileCollector) widget;
      this.collector.bindDnd(getFormView());
    }

    if (widget instanceof UnboundSelector) {
      templSelector = (UnboundSelector) widget;
    }
  }

  @Override
  public void afterInsertRow(final IsRow result, boolean forced) {
    if (collector != null && !collector.isEmpty()) {
      sendFiles(result.getId(), collector.getFiles(), null);
      collector.clear();
    }

    if (result.getString(Data.getColumnIndex(VIEW_DOCUMENTS, COL_DOCUMENT_COMPANY)) != null) {
      DocumentsHandler.insertCompanyInfo(result, null);
    }

    if (templSelector != null) {
      Long selId = BeeUtils.toLong(templSelector.getValue());
      if (DataUtils.isId(selId)) {

        Queries.getRow(VIEW_DOCUMENT_TEMPLATES, selId, new RowCallback() {
          @Override
          public void onSuccess(BeeRow templateRow) {
            DocumentsHandler.insertTemplateContent(result, Data.getString(VIEW_DOCUMENT_TEMPLATES,
                templateRow, COL_DOCUMENT_CONTENT));
          }
        });

      }

    }
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    if (templSelector != null
        && DataUtils.isId(row.getProperty(TaskConstants.PRM_DEFAULT_DBA_TEMPLATE))) {
      templSelector.setValue(BeeUtils.toLong(row.getProperty(
          TaskConstants.PRM_DEFAULT_DBA_TEMPLATE)), true);
    }

    DateTime t1 = row.getDateTime(form.getDataIndex(COL_DOCUMENT_DATE));
    /* resetting document date without current time */
    if (t1 != null && DataUtils.isNewRow(row) && t1.hasTimePart()) {
      t1.clearTimePart();
      row.setValue(form.getDataIndex(COL_DOCUMENT_DATE), t1);
      form.refreshBySource(COL_DOCUMENT_DATE);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new DocumentBuilder();
  }

  private static void sendFiles(Long docId, Collection<FileInfo> files,
      Scheduler.ScheduledCommand onComplete) {

    String viewName = VIEW_DOCUMENT_FILES;
    List<BeeColumn> columns = Data.getColumns(viewName);

    Holder<Integer> latch = Holder.of(files.size());

    for (FileInfo fileInfo : files) {
      FileUtils.uploadFile(fileInfo, result -> {
        BeeRow row = DataUtils.createEmptyRow(columns.size());

        Data.setValue(viewName, row, COL_DOCUMENT, docId);
        Data.setValue(viewName, row, AdministrationConstants.COL_FILE, result.getId());

        Data.setValue(viewName, row, COL_FILE_DATE,
            fileInfo.getFileDate() == null ? new DateTime() : fileInfo.getFileDate());
        Data.setValue(viewName, row, COL_FILE_VERSION, fileInfo.getFileVersion());

        Data.setValue(viewName, row, COL_FILE_CAPTION,
            BeeUtils.notEmpty(fileInfo.getCaption(), fileInfo.getName()));
        Data.setValue(viewName, row, COL_DESCRIPTION, fileInfo.getDescription());

        Queries.insert(viewName, columns, row, new RowCallback() {
          @Override
          public void onSuccess(BeeRow br) {
            latch.set(latch.get() - 1);
            if (!BeeUtils.isPositive(latch.get()) && onComplete != null) {
              onComplete.execute();
            }
          }
        });
      });
    }
  }
}
