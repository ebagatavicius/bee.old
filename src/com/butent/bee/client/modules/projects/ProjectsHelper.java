package com.butent.bee.client.modules.projects;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants.ProjectEvent;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;
import java.util.Map;

/**
 * Auxiliary functions for project client-side operations.
 */
public final class ProjectsHelper {

  /**
   * Checks current online user of system is owner related row of Projects table. Returns true if
   * {@code BeeKeeper.getUser().getUserId} are equals with Projects.Owner field relation.
   *
   * @param form View of Form ({@code FormView}) has indexes (
   *        {@code form.getDataIndex(String source)}}) of data source related with Projects.Owner
   *        field. Name of data source index can be {@code ProjectConstants.COL_PROJECT_OWNER} if
   *        FormView data source linked to Projects table or
   *        {@code ProjectConstants.ALS_PROJECT_OWNER} if FormView data source linked to related
   *        tables where has relations to Projects table.
   * @param row Set of data where related with Project.Owner field.
   * @return true if current system user equals with Project.Owner field.
   */
  public static boolean isProjectOwner(FormView form, IsRow row) {
    int idxOwner;

    Assert.notNull(form, "FormView containing project data must be not null");
    Assert.notNull(row, "IsRow containing project data must be not null");

    if (BeeUtils.same(form.getViewName(), VIEW_PROJECTS)) {
      idxOwner = form.getDataIndex(COL_PROJECT_OWNER);
    } else {
      idxOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    }

    Assert.nonNegative(idxOwner);

    long currentUser = BeeUtils.unbox(BeeKeeper.getUser().getUserId());
    long projectUser = BeeUtils.unbox(row.getLong(idxOwner));

    return currentUser == projectUser;
  }

  /**
   * Checks current online user of system is one of rows contains with ProjectUsers.User field
   * filtered by ProjectUser.Project field. Returns true if {@code BeeKeeper.getUser().getUserId()}
   * are match with one Projects.Owner field.
   *
   * @param form View of Form ({@code FormView}) has indexes (
   *        {@code form.getDataIndex(String source)}}) of data source related with filtered
   *        ProjectUsers.User field by ProjectUsers.Project filed. Name of data source index can be
   *        {@code ProjectConstants.ALS_FILTERED_PROJECT_USER}
   * @param row Set of data where related with filtered ProjectUsers.User by ProjectUsers.Project
   *        field.
   * @return true if current system user match Project.Owner field.
   */
  public static boolean isProjectUser(FormView form, IsRow row) {
    Assert.notNull(form, "FormView containing project data must be not null");
    Assert.notNull(row, "IsRow containing project data must be not null");

    int idxProjectUser = form.getDataIndex(ALS_FILTERED_PROJECT_USER);

    Assert.nonNegative(idxProjectUser);

    long currentUser = BeeUtils.unbox(BeeKeeper.getUser().getUserId());
    long projectUser = BeeUtils.unbox(row.getLong(idxProjectUser));

    return currentUser == projectUser;
  }

  public static void registerProjectEvent(String eventsViewName,
      ProjectEvent eventType, long projectId, String comment,
      Map<String, Map<String, String>> newData,
      Map<String, Map<String, String>> oldData) {
    registerProjectEvent(eventsViewName, null, eventType, projectId, comment, null,
        newData, oldData, null, null);
  }

  public static void registerProjectEvent(String eventsViewName,
      ProjectEvent eventType, long projectId, String comment,
      Map<String, Map<String, String>> newData,
      Map<String, Map<String, String>> oldData, Callback<BeeRow> eventIdCallback) {
    registerProjectEvent(eventsViewName, null, eventType, projectId, comment, null,
        newData, oldData, eventIdCallback, null);
  }

  public static void registerProjectEvent(String eventsViewName, String eventFilesViewName,
      ProjectEvent eventType,
      long projectId, String comment, List<FileInfo> files, Callback<BeeRow> eventIdCallback,
      Callback<Boolean> filesUploadedCallBack) {
    registerProjectEvent(eventsViewName, eventFilesViewName, eventType, projectId, comment, files,
        null, null, eventIdCallback, filesUploadedCallBack);
  }

  public static void registerProjectEvent(String eventsViewName,
      String eventFilesViewName, ProjectEvent eventType,
      long projectId, String comment, List<FileInfo> files,
      Map<String, Map<String, String>> newData,
      Map<String, Map<String, String>> oldData, Callback<BeeRow> eventIdCallback,
      Callback<Boolean> filesUploadedCallBack) {

    Assert.notEmpty(eventsViewName);
    Assert.notNull(eventType);
    Assert.isPositive(projectId);

    Long currentUserId = BeeKeeper.getUser().getUserId();
    DateTime time = new DateTime();

    String newDataProp = Codec.beeSerialize(newData);
    String oldDataProp = Codec.beeSerialize(oldData);

    String prop = Codec.beeSerialize(Lists.newArrayList(oldDataProp, newDataProp));

    List<BeeColumn> columns = Data.getColumns(eventsViewName,
        Lists.newArrayList(COL_PROJECT, COL_PUBLISHER, COL_PUBLISH_TIME, COL_COMMENT,
            COL_EVENT, COL_EVENT_PROPERTIES));

    List<String> values = Lists.newArrayList(BeeUtils.toString(projectId),
        BeeUtils.toString(currentUserId), BeeUtils.toString(time.getTime()), comment,
        BeeUtils.toString(eventType.ordinal()), prop);

    Queries.insert(eventsViewName, columns, values, null, getEventRowCallback(eventFilesViewName,
        projectId, files, eventIdCallback, filesUploadedCallBack));

    Filter flt = Filter.and(Filter.equals(COL_PROJECT, projectId),
        Filter.equals(AdministrationConstants.COL_USER, currentUserId));

    Queries.update(TBL_PROJECT_USAGE, flt, COL_ACCESS,
        BeeUtils.toString(System.currentTimeMillis()), new Queries.IntCallback() {
          @Override
          public void onSuccess(Integer result) {
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), ProjectConstants.VIEW_PROJECTS);
          }
        });

  }

  public static void registerReason(FormView form, IsRow row, CellValidateEvent event,
      final Callback<Boolean> success) {
    DataInfo data = Data.getDataInfo(VIEW_PROJECT_EVENTS);
    BeeRow emptyRow = RowFactory.createEmptyRow(data, false);
    String caption =
        BeeUtils.joinWords(form.getCaption(), BeeUtils.bracket(row.getString(form
            .getDataIndex(COL_PROJECT_NAME))));

    RowFactory.createRow(FORM_NEW_PROJECT_REASON_COMMENT, caption, data, emptyRow,
        Modality.ENABLED, null,
        new NewReasonCommentForm(form, row, event), null, new RowCallback() {

          @Override
          public void onSuccess(BeeRow result) {
            if (success != null) {
              success.onSuccess(true);
            }
          }

          @Override
          public void onCancel() {
            if (success != null) {
              success.onSuccess(false);
            }
          }

          @Override
          public void onFailure(String... reason) {
            if (success != null) {
              success.onFailure(reason);
              success.onSuccess(false);
            }
          }

        });

  }

  public static String getDisplayValue(String viewName, String column, String value, IsRow row) {
    return getDisplayValue(viewName, column, value, row, null);
  }

  public static String getDisplayValue(String viewName, String column, String value, IsRow row,
      final Callback<String> relatedValue) {
    String result = value;

    if (BeeUtils.isEmpty(result)) {
      return result;
    }
    if (BeeUtils.isEmpty(viewName)) {
      return result;
    }

    DataInfo info = Data.getDataInfo(viewName);

    if (info == null) {
      return result;
    }

    if (info.hasRelation(column) && row == null && !DataUtils.isId(value)) {
      return result;
    } else if (info.hasRelation(column) && row != null) {
      result = BeeConst.STRING_EMPTY;
      for (ViewColumn vCol : info.getDescendants(column, false)) {
        if (BeeUtils.same(ALS_OWNER_PERSON, vCol.getName())) {
          continue;
        }
        result =
            BeeUtils.joinWords(result, getDisplayValue(viewName, vCol.getName(), row.getString(info
                .getColumnIndex(vCol.getName())), null));
      }
      return result;
    } else if (info.hasRelation(column) && row == null && DataUtils.isId(value)) {
      String relView = info.getRelation(column);
      List<String> cols = Lists.newArrayList();

      for (ViewColumn vCol : info.getDescendants(column, false)) {
        cols.add(vCol.getField());
      }

      if (cols.isEmpty()) {
        return result;
      }

      Queries.getRow(relView, Filter.compareId(BeeUtils.toLong(value)), cols, new RowCallback() {

        @Override
        public void onSuccess(BeeRow wResult) {
          if (relatedValue == null) {
            return;
          }

          relatedValue.onSuccess(BeeUtils.join(BeeConst.STRING_SPACE, wResult.getValues()));
        }
      });

      return result;
    }

    ValueType type = info.getColumnType(column);
    IsColumn col = info.getColumn(column);

    if (row != null) {
      result = DataUtils.render(col, row, info.getColumnIndex(column),
          Format.getDateRenderer(), Format.getDateTimeRenderer());
    } else if (ValueType.DATE_TIME.equals(type)) {
      DateTime time = TimeUtils.toDateTimeOrNull(value);

      result = time == null ? result : Format.renderDateTime(time);
    } else if (ValueType.DATE.equals(type)) {
      JustDate date = TimeUtils.toDateOrNull(value);

      result = date == null ? result : Format.renderDate(date);
    } else if (col != null && !BeeUtils.isEmpty(col.getEnumKey())) {
      return EnumUtils.getCaption(col.getEnumKey(), BeeUtils.toInt(value));
    }

    return result;
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
            Lists.newArrayList(COL_PROJECT, COL_PROJECT_EVENT, AdministrationConstants.COL_FILE,
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

  private static RowCallback getEventRowCallback(final String eventFilesViewName,
      final long projectId, final List<FileInfo> files, final Callback<BeeRow> idCallback,
      final Callback<Boolean> uploaded) {
    return new RowCallback() {

      @Override
      public void onSuccess(BeeRow result) {
        if (idCallback != null) {
          idCallback.onSuccess(result);
        }
        createFiles(eventFilesViewName, projectId, result.getId(), files, uploaded);
      }
    };
  }

  private ProjectsHelper() {

  }
}
