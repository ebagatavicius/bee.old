package com.butent.bee.server.rest;

import com.google.common.collect.HashMultimap;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.COL_EVENT;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.modules.tasks.TasksModuleBean;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

@Path("tasks")
public class TasksWorker extends CrudWorker {

  @EJB
  TasksModuleBean task;
  @EJB
  TaskEventsWorker events;

  @GET
  @Path("{" + ID + ":\\d+}/access")
  public RestResponse access(@PathParam(ID) Long taskId) {
    ResponseObject response = task.accessTask(taskId);

    if (response.hasErrors()) {
      return RestResponse.error(ArrayUtils.joinWords(response.getErrors()));
    }
    SimpleRowSet.SimpleRow row = qs.getRow(new SqlSelect()
        .addField(TBL_TASKS, sys.getVersionName(TBL_TASKS), VERSION)
        .addFields(TBL_TASKS, COL_STATUS, COL_EXECUTOR)
        .addFrom(TBL_TASKS)
        .setWhere(sys.idEquals(TBL_TASKS, taskId)));

    Integer oldStatus = row.getInt(COL_STATUS);

    if (Objects.equals(row.getLong(COL_EXECUTOR), usr.getCurrentUserId())
        && (TaskStatus.NOT_VISITED.is(oldStatus))) {

      return update(taskId, row.getLong(VERSION), Json.createObjectBuilder()
          .add(COL_STATUS, TaskStatus.VISITED.ordinal())
          .add(OLD_VALUES, Json.createObjectBuilder().add(COL_STATUS, oldStatus))
          .add(COL_TASK_EVENT, Json.createObjectBuilder().add(COL_EVENT, TaskEvent.VISIT.ordinal()))
          .build());
    }
    return get(taskId);
  }

  @POST
  @Path("{" + ID + ":\\d+}/addobservers")
  @Consumes(MediaType.APPLICATION_JSON)
  public RestResponse addObservers(@PathParam(ID) Long taskId, JsonArray observers) {
    List<Long> users = new ArrayList<>();
    List<String> notes = new ArrayList<>();

    for (JsonObject observer : observers.getValuesAs(JsonObject.class)) {
      Long user = BeeUtils.toLongOrNull(getValue(observer, COL_USER));

      try {
        commit(() -> {
          task.createTaskUser(taskId, user, null);
          notes.add(TaskUtils.getInsertNote(Localized.dictionary().crmTaskObservers(),
              BeeUtils.joinWords(getValue(observer, COL_FIRST_NAME),
                  getValue(observer, COL_LAST_NAME))));
        });
      } catch (BeeException ex) {
        LogUtils.getRootLogger().error(ex);
      }
      users.add(user);
    }
    if (!BeeUtils.isEmpty(notes)) {
      events.insert(Json.createObjectBuilder()
          .add(COL_TASK + ID, taskId)
          .add(COL_PUBLISHER + ID, usr.getCurrentUserId())
          .add(COL_PUBLISH_TIME, System.currentTimeMillis())
          .add(COL_EVENT, TaskEvent.COMMENT.ordinal())
          .add(COL_COMMENT, BeeUtils.buildLines(notes))
          .build());
    }
    return RestResponse.ok(getData(qs.getViewData(TBL_TASK_USERS,
        Filter.and(Filter.equals(COL_TASK, taskId),
            BeeUtils.isEmpty(users) ? Filter.isFalse() : Filter.any(COL_USER, users)), null,
        Arrays.asList(COL_TASK, COL_USER)), null));
  }

  @Override
  public RestResponse delete(Long id, Long version) {
    return RestResponse.forbidden();
  }

  @POST
  @Path("{" + ID + ":\\d+}/removeobservers")
  @Consumes(MediaType.APPLICATION_JSON)
  public RestResponse removeObservers(@PathParam(ID) Long taskId, JsonArray observers) {
    List<Long> users = new ArrayList<>();
    List<String> notes = new ArrayList<>();

    for (JsonObject observer : observers.getValuesAs(JsonObject.class)) {
      users.add(BeeUtils.toLongOrNull(getValue(observer, COL_USER)));
      notes.add(TaskUtils.getDeleteNote(Localized.dictionary().crmTaskObservers(),
          BeeUtils.joinWords(getValue(observer, COL_FIRST_NAME),
              getValue(observer, COL_LAST_NAME))));
    }
    if (!BeeUtils.isEmpty(users)) {
      try {
        commit(() -> {
          int cnt = qs.updateData(new SqlDelete(TBL_TASK_USERS)
              .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TASK_USERS, COL_TASK, taskId),
                  SqlUtils.inList(TBL_TASK_USERS, COL_USER, users))));

          if (BeeUtils.isPositive(cnt)) {
            events.insert(Json.createObjectBuilder()
                .add(COL_TASK + ID, taskId)
                .add(COL_PUBLISHER + ID, usr.getCurrentUserId())
                .add(COL_PUBLISH_TIME, System.currentTimeMillis())
                .add(COL_EVENT, TaskEvent.COMMENT.ordinal())
                .add(COL_COMMENT, BeeUtils.buildLines(notes))
                .build());
          }
        });
      } catch (BeeException ex) {
        LogUtils.getRootLogger().error(ex);
      }
    }
    return RestResponse.empty();
  }

  @Override
  public RestResponse get(Filter filter) {
    long time = System.currentTimeMillis();
    BeeRowSet rowSet = qs.getViewData(getViewName(), filter);

    HashMultimap<String, String> children = HashMultimap.create();
    children.putAll(TBL_FILES, Arrays.asList(COL_FILE, ALS_FILE_NAME));
    children.putAll(PROP_OBSERVERS, Arrays.asList(COL_TASK, COL_USER, ID));

    return RestResponse.ok(getData(rowSet, children)).setLastSync(time);
  }

  @Override
  public RestResponse getAll(Long lastSynced) {
    CompoundFilter filter = Filter.or();
    Long userId = usr.getCurrentUserId();

    filter.add(Filter.and(Filter.equals(COL_EXECUTOR, userId),
        Filter.any(COL_STATUS, EnumSet.of(TaskStatus.NOT_VISITED, TaskStatus.ACTIVE))));

    filter.add(Filter.and(Filter.equals(COL_OWNER, userId), Filter.isNot(Filter.any(COL_STATUS,
        EnumSet.of(TaskStatus.APPROVED, TaskStatus.CANCELED)))));

    filter.add(Filter.isNull(COL_LAST_ACCESS))
        .add(Filter.compareWithColumn(COL_PUBLISH_TIME, Operator.GT, COL_LAST_ACCESS));

    return get(filter);
  }

  @GET
  @Path("{" + ID + ":\\d+}/events")
  public RestResponse getEvents(@PathParam(ID) Long taskId,
      @HeaderParam(RestResponse.LAST_SYNC_TIME) Long lastSynced) {

    return events.get(Filter.and(Filter.equals(COL_TASK + ID, taskId),
        Filter.compareVersion(Operator.GT, BeeUtils.unbox(lastSynced))));
  }

  @Override
  public RestResponse insert(JsonObject data) {
    LogUtils.getRootLogger().debug(data);

    Holder<Long> idHolder = Holder.of(BeeUtils.toLongOrNull(getValue(data, ID)));

    if (DataUtils.isId(idHolder.get()) || !usr.canCreateData(getViewName())) {
      return RestResponse.forbidden();
    }
    RestResponse response = null;

    try {
      commit(() -> {
        BeeView view = sys.getView(getViewName());
        List<BeeColumn> cols = new ArrayList<>();
        List<String> vals = new ArrayList<>();

        for (String col : data.keySet()) {
          if (view.hasColumn(col)) {
            BeeColumn column = view.getBeeColumn(col);

            if (!column.isForeign()) {
              String value = getValue(data, col);

              if (!BeeUtils.isEmpty(value)) {
                cols.add(column);
                vals.add(value);
              }
            }
          }
        }
        BeeRowSet rs = DataUtils.createRowSetForInsert(view.getName(), cols, vals);
        rs.getRow(0).setProperty(PROP_EXECUTORS, getValue(data, COL_EXECUTOR));

        JsonArray observers = data.getJsonArray(PROP_OBSERVERS);

        if (Objects.nonNull(observers)) {
          Set<Long> ids = new HashSet<>();

          for (JsonObject observer : observers.getValuesAs(JsonObject.class)) {
            ids.add(BeeUtils.toLongOrNull(getValue(observer, COL_USER)));
          }
          rs.getRow(0).setProperty(PROP_OBSERVERS, DataUtils.buildIdList(ids));
        }
        Map<String, String> params = new HashMap<>();
        params.put(VAR_TASK_DATA, Codec.beeSerialize(rs));

        ResponseObject resp = task.doTaskEvent(TaskEvent.CREATE.name(), params);

        if (resp.hasErrors()) {
          throw new BeeRuntimeException(ArrayUtils.joinWords(resp.getErrors()));
        }
        idHolder.set(BeeUtils.peek(((BeeRowSet) resp.getResponse()).getRowIds()));
      });
    } catch (BeeException e) {
      response = RestResponse.error(e);
      e.printStackTrace();
    }
    if (Objects.isNull(response)) {
      events.storeFiles(idHolder.get(), null, data.getJsonArray(TBL_FILES));
      response = get(idHolder.get());
    }
    return response;
  }

  @Override
  public RestResponse update(Long id, Long version, JsonObject taskData) {
    LogUtils.getRootLogger().debug(taskData);

    if (!usr.canEditData(getViewName())) {
      return RestResponse.forbidden();
    }
    Holder<RestResponse> error = Holder.absent();

    try {
      commit(() -> {
        DataInfo info = sys.getDataInfo(getViewName());
        List<BeeColumn> columns = info.getColumns();
        BeeRow row = DataUtils.createEmptyRow(columns.size());
        row.setId(id);
        row.setVersion(version);
        BeeRow oldRow = DataUtils.cloneRow(row);
        BeeRowSet updated = null;

        JsonObject oldTask = taskData.getJsonObject(OLD_VALUES);
        JsonObject taskEvent = taskData.getJsonObject(COL_TASK_EVENT);

        if (Objects.nonNull(oldTask) && Objects.nonNull(taskEvent)) {
          for (String col : oldTask.keySet()) {
            int idx = DataUtils.getColumnIndex(col, columns);

            if (!BeeConst.isUndef(idx)) {
              row.setValue(idx, getValue(taskData, col));
              oldRow.setValue(idx, getValue(oldTask, col));
            }
          }
          updated = DataUtils.getUpdated(info.getViewName(), columns, oldRow, row, null);
        }
        if (!DataUtils.isEmpty(updated)) {
          Map<String, String> params = new HashMap<>();
          params.put(VAR_TASK_DATA, Codec.beeSerialize(updated));

          DateTimeFormatInfo dateTimeFormatInfo = usr.getDateTimeFormatInfo();

          List<String> notes = TaskUtils.getUpdateNotes(info, oldRow, row,
              Formatter.getDateRenderer(dateTimeFormatInfo),
              Formatter.getDateTimeRenderer(dateTimeFormatInfo));

          if (!notes.isEmpty()) {
            params.put(VAR_TASK_NOTES, Codec.beeSerialize(notes));
          }
          String comment = getValue(taskEvent, COL_COMMENT);

          if (!BeeUtils.isEmpty(comment)) {
            params.put(VAR_TASK_COMMENT, comment);
          }
          String time = getValue(taskEvent, COL_DURATION);

          if (!BeeUtils.isEmpty(time)) {
            params.put(VAR_TASK_DURATION_DATE, getValue(taskEvent, COL_DURATION_DATE));
            params.put(VAR_TASK_DURATION_TYPE, getValue(taskEvent, COL_DURATION_TYPE + ID));
            params.put(VAR_TASK_DURATION_TIME, time);
          }
          TaskEvent event = EnumUtils.getEnumByIndex(TaskEvent.class,
              getValue(taskEvent, COL_EVENT));

          if (event == TaskEvent.VISIT) {
            params.put(VAR_TASK_VISITED, "1");
          }
          ResponseObject resp = task.doTaskEvent(event.name(), params);

          if (resp.hasErrors()) {
            throw new BeeRuntimeException(ArrayUtils.joinWords(resp.getErrors()));
          }
          if (updated.containsColumn(COL_EXECUTOR)) {
            Long executor = updated.getLong(0, COL_EXECUTOR);

            SimpleRowSet.SimpleRow taskRow = qs.getRow(new SqlSelect()
                .addFields(TBL_TASKS, COL_OWNER)
                .addField(TBL_TASK_USERS, sys.getIdName(TBL_TASK_USERS), ID)
                .addFrom(TBL_TASKS)
                .addFromLeft(TBL_TASK_USERS,
                    SqlUtils.and(sys.joinTables(TBL_TASKS, TBL_TASK_USERS, COL_TASK),
                        SqlUtils.equals(TBL_TASK_USERS, COL_USER, executor)))
                .setWhere(sys.idEquals(TBL_TASKS, id)));

            Long userId = taskRow.getLong(ID);

            if (Objects.isNull(userId)) {
              task.createTaskUser(id, executor, null);

            } else if (!Objects.equals(taskRow.getLong(COL_OWNER), executor)) {
              qs.updateData(new SqlUpdate(TBL_TASK_USERS)
                  .addConstant(COL_LAST_ACCESS, null)
                  .setWhere(sys.idEquals(TBL_TASK_USERS, userId)));
            }
          }
          events.storeFiles(id, ((BeeRow) resp.getResponse())
                  .getPropertyLong(PROP_LAST_EVENT_ID),
              taskEvent.getJsonArray(TBL_FILES));
        } else {
          error.set(RestResponse.error(Localized.dictionary().noData()));
        }
      });
    } catch (BeeException e) {
      error.set(RestResponse.error(e));
    }
    RestResponse response = get(id);

    if (error.isNotNull()) {
      response = error.get().setResult(response.getResult());
    }
    return response;
  }

  @Override
  protected String getViewName() {
    return "RestTasks";
  }
}
