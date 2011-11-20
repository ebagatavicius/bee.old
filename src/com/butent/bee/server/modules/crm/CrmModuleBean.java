package com.butent.bee.server.modules.crm;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.modules.crm.CrmConstants;
import com.butent.bee.shared.modules.crm.CrmConstants.TaskEvent;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.TimeUtils;

import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class CrmModuleBean implements BeeModule {

  private static final Splitter USER_ID_SPLITTER =
      Splitter.on(BeeConst.CHAR_COMMA).omitEmptyStrings().trimResults();

  private static Logger logger = Logger.getLogger(CrmModuleBean.class.getName());

  @EJB
  SystemBean sys;
  @EJB
  DataEditorBean deb;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @Resource
  EJBContext ctx;

  @Override
  public ResponseObject doService(RequestInfo reqInfo) {
    ResponseObject response = null;
    String svc = reqInfo.getParameter(CrmConstants.CRM_METHOD);
    TaskEvent ev;

    try {
      ev = TaskEvent.valueOf(svc);
    } catch (Exception e) {
      ev = null;
    }
    if (ev != null) {
      response = doTaskEvent(ev, reqInfo);

    } else {
      String msg = BeeUtils.concat(1, "CRM service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public String getName() {
    return CrmConstants.CRM_MODULE;
  }

  private ResponseObject doTaskEvent(TaskEvent event, RequestInfo reqInfo) {
    ResponseObject response = null;
    long time = System.currentTimeMillis();
    long currentUser = usr.getCurrentUserId();

    switch (event) {
      case ACTIVATED:
        BeeRowSet rs = BeeRowSet.restore(reqInfo.getParameter(CrmConstants.VAR_TASK_DATA));
        String executors = reqInfo.getParameter(CrmConstants.VAR_TASK_EXECUTORS);
        String observers = reqInfo.getParameter(CrmConstants.VAR_TASK_OBSERVERS);

        int exIndex = DataUtils.getColumnIndex(CrmConstants.COL_EXECUTOR, rs.getColumns());
        int cnt = 0;

        for (String exId : USER_ID_SPLITTER.split(executors)) {
          rs.getRow(0).setValue(exIndex, exId);

          response = deb.commitRow(rs, false);

          if (!response.hasErrors()) {
            cnt++;
            long taskId = ((BeeRow) response.getResponse()).getId();
            long executor = BeeUtils.toLong(exId);
            response = registerTaskEvent(taskId, time, reqInfo, event, null);

            if (!response.hasErrors() && executor != currentUser) {
              response = registerTaskVisit(taskId, executor, null, true);
            }
            if (!response.hasErrors() && !BeeUtils.isEmpty(observers)) {
              for (String obsId : USER_ID_SPLITTER.split(observers)) {
                long observer = BeeUtils.toLong(obsId);

                if (observer != currentUser && observer != executor) {
                  response = registerTaskVisit(taskId, observer, null, true);

                  if (response.hasErrors()) {
                    break;
                  }
                }
              }
              if (response.hasErrors()) {
                break;
              }
            }
          }
        }
        if (!response.hasErrors()) {
          response = ResponseObject.response(cnt);
        }
        break;

      case VISITED:
        response =
            registerTaskVisit(BeeUtils.toLong(reqInfo.getParameter(CrmConstants.VAR_TASK_ID)),
                currentUser, time, false);
        break;

      case COMMENTED:
        response =
            registerTaskEvent(BeeUtils.toLong(reqInfo.getParameter(CrmConstants.VAR_TASK_ID)),
                time, reqInfo, event, null);
        break;

      case FORWARDED:
        rs = BeeRowSet.restore(reqInfo.getParameter(CrmConstants.VAR_TASK_DATA));
        long taskId = rs.getRow(0).getId();
        long oldUser = BeeUtils.toLong(rs.getShadowString(0, CrmConstants.COL_EXECUTOR));
        long newUser = BeeUtils.toLong(rs.getString(0, CrmConstants.COL_EXECUTOR));

        response = deb.commitRow(rs, false);

        if (!response.hasErrors()) {
          response = registerTaskVisit(taskId, newUser, time - 1, false);

          if (!response.hasErrors()) {
            response = registerTaskEvent(taskId, time, reqInfo, event,
                BeeUtils.concat(" -> ", usr.getUserSign(oldUser), usr.getUserSign(newUser)));

            if (!response.hasErrors()
                && !BeeUtils.isEmpty(reqInfo.getParameter(CrmConstants.VAR_TASK_OBSERVE))) {
              String tbl = "TaskUsers";
              qs.updateData(new SqlDelete(tbl)
                  .setWhere(SqlUtils.and(SqlUtils.equal(tbl, "Task", taskId),
                      SqlUtils.equal(tbl, "User", oldUser))));
            }
          }
        }
        break;

      case EXTENDED:
        rs = BeeRowSet.restore(reqInfo.getParameter(CrmConstants.VAR_TASK_DATA));
        taskId = rs.getRow(0).getId();
        String oldTerm = rs.getShadowString(0, "FinishTime");
        String newTerm = rs.getString(0, "FinishTime");

        response = deb.commitRow(rs, false);

        if (!response.hasErrors()) {
          response = registerTaskEvent(taskId, time, reqInfo, event,
              BeeUtils.concat(" -> ",
                  TimeUtils.toDateTimeOrNull(oldTerm), TimeUtils.toDateTimeOrNull(newTerm)));
        }
        break;

      case UPDATED:
        rs = BeeRowSet.restore(reqInfo.getParameter(CrmConstants.VAR_TASK_DATA));
        taskId = rs.getRow(0).getId();
        Map<String, String> prm = Maps.newHashMap();
        reqInfo.setParams(prm);

        for (BeeColumn col : rs.getColumns()) {
          String colName = col.getId();
          String oldValue = rs.getShadowString(0, colName);
          String value = rs.getString(0, colName);
          prm.put(CrmConstants.VAR_TASK_COMMENT, BeeUtils.concat(" -> ", oldValue, value));

          if (response == null || !response.hasErrors()) {
            response = registerTaskEvent(taskId, time, reqInfo, event, colName);
          }
        }
        if (!response.hasErrors()) {
          response = deb.commitRow(rs, true);
        }
        break;

      case SUSPENDED:
      case CANCELED:
      case COMPLETED:
      case APPROVED:
      case RENEWED:
        rs = BeeRowSet.restore(reqInfo.getParameter(CrmConstants.VAR_TASK_DATA));
        taskId = rs.getRow(0).getId();

        response = deb.commitRow(rs, false);

        if (!response.hasErrors()) {
          response = registerTaskEvent(taskId, time, reqInfo, event, null);
        }
        break;

      case DELETED:
        Assert.untouchable();
    }
    if (response.hasErrors()) {
      ctx.setRollbackOnly();
    }
    return response;
  }

  private ResponseObject registerTaskEvent(long taskId, long time, RequestInfo reqInfo,
      TaskEvent event, String eventNote) {
    ResponseObject response = null;

    int minutes = BeeUtils.toInt(reqInfo.getParameter(CrmConstants.VAR_TASK_DURATION_TIME));

    if (BeeUtils.isPositive(minutes)) {
      response = qs.insertDataWithResponse(new SqlInsert("EventDurations")
          .addConstant("DurationDate",
              BeeUtils.toLong(reqInfo.getParameter(CrmConstants.VAR_TASK_DURATION_DATE)))
          .addConstant("DurationType",
              BeeUtils.toLong(reqInfo.getParameter(CrmConstants.VAR_TASK_DURATION_TYPE)))
          .addConstant("Duration", minutes));
    }
    long currentUser = usr.getCurrentUserId();

    if (response == null || !response.hasErrors()) {
      response = qs.insertDataWithResponse(new SqlInsert("TaskEvents")
          .addConstant("Task", taskId)
          .addConstant("Publisher", currentUser)
          .addConstant("PublishTime", time)
          .addConstant("Comment", reqInfo.getParameter(CrmConstants.VAR_TASK_COMMENT))
          .addConstant(CrmConstants.COL_EVENT, event.ordinal())
          .addConstant("EventNote", eventNote)
          .addConstant("EventDuration", response == null ? null : (Long) response.getResponse()));
    }
    if (!response.hasErrors()) {
      response =
          registerTaskVisit(taskId, currentUser, time, BeeUtils.equals(event, TaskEvent.ACTIVATED));
    }
    return response;
  }

  private ResponseObject registerTaskVisit(long taskId, long userId, Long time, boolean newVisit) {
    ResponseObject response = null;
    String tbl = "TaskUsers";

    if (!newVisit) {
      response = qs.updateDataWithResponse(new SqlUpdate(tbl)
          .addConstant(CrmConstants.COL_LAST_ACCESS, time)
          .setWhere(SqlUtils.and(SqlUtils.equal(tbl, "Task", taskId),
              SqlUtils.equal(tbl, "User", userId))));
    }
    if (newVisit || BeeUtils.isEmpty(response.getResponse(-1, logger))) {
      response = qs.insertDataWithResponse(new SqlInsert(tbl)
          .addConstant("Task", taskId)
          .addConstant("User", userId)
          .addConstant(CrmConstants.COL_LAST_ACCESS, time));
    }
    if (!response.hasErrors()) {
      response = ResponseObject.response(time);
    }
    return response;
  }
}
