package com.butent.bee.server.data;

import com.butent.bee.server.DataSourceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.jdbc.BeeConnection;
import com.butent.bee.server.jdbc.BeeResultSet;
import com.butent.bee.server.jdbc.BeeStatement;
import com.butent.bee.server.jdbc.JdbcConst;
import com.butent.bee.server.jdbc.JdbcUtils;
import com.butent.bee.server.utils.BeeDataSource;
import com.butent.bee.server.utils.SystemInfo;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Manages JDBC connectivity and executes service requests with rpc_db_jdbc tag.
 */

@Stateless
public class DataServiceBean {

  private static BeeLogger logger = LogUtils.getLogger(DataServiceBean.class);

  @EJB
  DataSourceBean dsb;
  @EJB
  MetaDataBean mdb;
  @EJB
  ResultSetBean rsb;

  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    Assert.notEmpty(svc);

    BeeDataSource ds = dsb.getDefaultDs();
    if (ds == null) {
      return ResponseObject.error(svc, "data source not available");
    }

    if (!ds.check()) {
      return ResponseObject.error(svc, "cannot open data source", ds.getErrors());
    }

    ResponseObject response;

    if (Service.isDbMetaService(svc)) {
      response = mdb.doService(svc, ds, reqInfo);

    } else if (BeeUtils.same(svc, Service.DB_JDBC)) {
      response = testJdbc(ds.getConn(), reqInfo);

    } else {
      String msg = BeeUtils.joinWords(svc, "data service not recognized");
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }

    ds.close();

    return response;
  }

  private ResponseObject testJdbc(Connection conn, RequestInfo reqInfo) {
    DateTime enter = new DateTime();

    Map<String, String> map = reqInfo.getVars();
    if (BeeUtils.isEmpty(map)) {
      return ResponseObject.error(reqInfo.getService(), "Request data not found");
    }

    String sql = map.get(Service.VAR_JDBC_QUERY);
    if (BeeUtils.isEmpty(sql)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_JDBC_QUERY);
    }

    for (String key : map.keySet()) {
      if (BeeConst.isDefault(map.get(key))) {
        map.put(key, BeeConst.STRING_EMPTY);
      }
    }

    String cAc = map.get(Service.VAR_CONNECTION_AUTO_COMMIT);
    String cHo = map.get(Service.VAR_CONNECTION_HOLDABILITY);
    String cRo = map.get(Service.VAR_CONNECTION_READ_ONLY);
    String cTi = map.get(Service.VAR_CONNECTION_TRANSACTION_ISOLATION);

    String sCn = map.get(Service.VAR_STATEMENT_CURSOR_NAME);
    String sEp = map.get(Service.VAR_STATEMENT_ESCAPE_PROCESSING);
    String sFd = map.get(Service.VAR_STATEMENT_FETCH_DIRECTION);
    String sFs = map.get(Service.VAR_STATEMENT_FETCH_SIZE);
    String sMf = map.get(Service.VAR_STATEMENT_MAX_FIELD_SIZE);
    String sMr = map.get(Service.VAR_STATEMENT_MAX_ROWS);
    String sPo = map.get(Service.VAR_STATEMENT_POOLABLE);
    String sQt = map.get(Service.VAR_STATEMENT_QUERY_TIMEOUT);
    String sRc = map.get(Service.VAR_STATEMENT_RS_CONCURRENCY);
    String sRh = map.get(Service.VAR_STATEMENT_RS_HOLDABILITY);
    String sRt = map.get(Service.VAR_STATEMENT_RS_TYPE);

    String rFd = map.get(Service.VAR_RESULT_SET_FETCH_DIRECTION);
    String rFs = map.get(Service.VAR_RESULT_SET_FETCH_SIZE);

    String ret = map.get(Service.VAR_JDBC_RETURN);

    String before = "before:";
    String after = "after:";

    boolean vb;
    boolean ok;
    int v1;
    int v2;
    int vu;

    ResponseObject response = new ResponseObject();

    BeeConnection bc = new BeeConnection(conn);

    if (BeeUtils.isBoolean(cAc)) {
      vb = bc.isAutoCommit();
      bc.updateAutoCommit(conn, cAc);
      v2 = bc.getAutoCommitQuietly(conn);

      if (bc.hasErrors()) {
        bc.revert(conn);
        response.addError(bc.getErrors());
        return response;
      }

      response.addInfo("Connection Auto Commit:", cAc, before, vb, after, BeeUtils.toBoolean(v2));
    }

    if (!BeeUtils.isEmpty(cHo)) {
      v1 = bc.getHoldability();
      bc.updateHoldability(conn, cHo);
      v2 = bc.getHoldabilityQuietly(conn);

      if (bc.hasErrors()) {
        bc.revert(conn);
        response.addError(bc.getErrors());
        return response;
      }

      response.addInfo("Connection Holdability:", cHo,
          before, v1, JdbcUtils.holdabilityAsString(v1),
          after, v2, JdbcUtils.holdabilityAsString(v2));
    }

    if (!BeeUtils.isEmpty(cTi)) {
      v1 = bc.getTransactionIsolation();
      bc.updateTransactionIsolation(conn, cTi);
      v2 = bc.getTransactionIsolationQuietly(conn);

      if (bc.hasErrors()) {
        bc.revert(conn);
        response.addError(bc.getErrors());
        return response;
      }

      response.addInfo("Connection Transaction Isolation:", cTi,
          before, v1, JdbcUtils.transactionIsolationAsString(v1),
          after, v2, JdbcUtils.transactionIsolationAsString(v2));
    }

    if (BeeUtils.isBoolean(cRo)) {
      vb = bc.isReadOnly();
      bc.updateReadOnly(conn, cRo);
      v2 = bc.getReadOnlyQuietly(conn);

      if (bc.hasErrors()) {
        bc.revert(conn);
        response.addError(bc.getErrors());
        return response;
      }

      response.addInfo("Connection Read Only:", cRo, before, vb, after, BeeUtils.toBoolean(v2));
    }

    Statement stmt = bc.createStatement(conn, sRt, sRc, sRh);
    if (bc.hasErrors() || stmt == null) {
      bc.revert(conn);
      response.addError(bc.getErrors());
      if (stmt == null) {
        response.addError("Statement not created");
      }
      JdbcUtils.closeStatement(stmt);
      return response;
    }

    BeeStatement bs = new BeeStatement(stmt);
    if (bs.hasErrors()) {
      bc.revert(conn);
      response.addError(bs.getErrors());
      JdbcUtils.closeStatement(stmt);
      return response;
    }

    if (!BeeUtils.allEmpty(sRt, sRc, sRh)) {
      response.addInfo("Statement parameters:", sRt, sRc, sRh);
      response.addInfo("Statement created:", bs.getResultSetType(),
          JdbcUtils.rsTypeAsString(bs.getResultSetType()), bs.getConcurrency(),
          JdbcUtils.concurrencyAsString(bs.getConcurrency()), bs.getHoldability(),
          JdbcUtils.holdabilityAsString(bs.getHoldability()));
    }

    if (!BeeUtils.isEmpty(sCn)) {
      if (JdbcUtils.supportsCursorName(conn)) {
        response.addInfo("Cursor name:", sCn);
        bs.setCursorName(stmt, sCn);

        if (bs.hasErrors()) {
          bc.revert(conn);
          response.addError(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return response;
        }

      } else {
        response.addWarning("Cursor name:", sCn, JdbcConst.FEATURE_NOT_SUPPORTED);
      }
    }

    if (BeeUtils.isBoolean(sEp)) {
      response.addInfo("Escape Processing:", sEp);
      bs.setEscapeProcessing(stmt, BeeUtils.toBoolean(sEp));

      if (bs.hasErrors()) {
        bc.revert(conn);
        response.addError(bs.getErrors());
        JdbcUtils.closeStatement(stmt);
        return response;
      }
    }

    if (!BeeUtils.isEmpty(sFd)) {
      v1 = bs.getFetchDirection();
      bs.updateFetchDirection(stmt, sFd);
      v2 = bs.getFetchDirectionQuietly(stmt);

      if (bs.hasErrors()) {
        bc.revert(conn);
        response.addError(bs.getErrors());
        JdbcUtils.closeStatement(stmt);
        return response;
      }

      response.addInfo("Statement Fetch Direction:", sFd,
          before, v1, JdbcUtils.fetchDirectionAsString(v1),
          after, v2, JdbcUtils.fetchDirectionAsString(v2));
    }

    if (!BeeUtils.isEmpty(sFs)) {
      ok = true;
      if (BeeUtils.inListSame(sFs, "min", "-")) {
        vu = Integer.MIN_VALUE;
      } else if (BeeUtils.isInt(sFs)) {
        vu = BeeUtils.toInt(sFs);
      } else {
        response.addWarning("Statement Fetch Size:", sFs, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        v1 = bs.getFetchSize();
        bs.updateFetchSize(stmt, vu);
        v2 = bs.getFetchSizeQuietly(stmt);

        if (bs.hasErrors()) {
          bc.revert(conn);
          response.addError(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return response;
        }

        response.addInfo("Statement Fetch Size:", sFs, BeeUtils.bracket(vu), before, v1, after, v2);
      }
    }

    if (!BeeUtils.isEmpty(sMf)) {
      if (BeeUtils.isInt(sMf)) {
        vu = BeeUtils.toInt(sMf);
        ok = true;
      } else {
        response.addWarning("Statement Max Field Size:", sMf, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        v1 = bs.getMaxFieldSize();
        bs.updateMaxFieldSize(stmt, vu);
        v2 = bs.getMaxFieldSizeQuietly(stmt);

        if (bs.hasErrors()) {
          bc.revert(conn);
          response.addError(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return response;
        }

        response.addInfo("Statement Max Field Size:", sMf, BeeUtils.bracket(vu),
            before, v1, after, v2);
      }
    }

    if (!BeeUtils.isEmpty(sMr)) {
      if (BeeUtils.isInt(sMr)) {
        vu = BeeUtils.toInt(sMr);
        ok = true;
      } else {
        response.addWarning("Statement Max Rows:", sMr, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        v1 = bs.getMaxRows();
        bs.updateMaxRows(stmt, vu);
        v2 = bs.getMaxRowsQuietly(stmt);

        if (bs.hasErrors()) {
          bc.revert(conn);
          response.addError(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return response;
        }

        response.addInfo("Statement Max Rows:", sMr, BeeUtils.bracket(vu), before, v1, after, v2);
      }
    }

    if (!BeeUtils.isEmpty(sQt)) {
      if (BeeUtils.isInt(sQt)) {
        vu = BeeUtils.toInt(sQt);
        ok = true;
      } else {
        response.addWarning("Statement Query Timeout:", sQt, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        v1 = bs.getQueryTimeout();
        bs.updateQueryTimeout(stmt, vu);
        v2 = bs.getQueryTimeoutQuietly(stmt);

        if (bs.hasErrors()) {
          bc.revert(conn);
          response.addError(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return response;
        }

        response.addInfo("Statement Query Timeout:", sQt, BeeUtils.bracket(vu),
            before, v1, after, v2);
      }
    }

    if (!BeeUtils.isEmpty(sPo)) {
      vb = bs.isPoolable();
      bs.updatePoolable(stmt, sPo);
      v2 = bs.getPoolableQuietly(stmt);

      if (bs.hasErrors()) {
        bc.revert(conn);
        response.addError(bs.getErrors());
        JdbcUtils.closeStatement(stmt);
        return response;
      }

      response.addInfo("Statement Poolable:", sPo, before, vb, after, BeeUtils.toBoolean(v2));
    }

    long memQ1 = SystemInfo.freeMemory();
    ResultSet rs = bs.executeQuery(stmt, sql);
    long memQ2 = SystemInfo.freeMemory();

    if (bs.hasErrors() || rs == null) {
      bc.revert(conn);
      response.addError(bs.getErrors());
      if (rs == null) {
        response.addError(BeeUtils.bracket(sql), "result set not created");
      }
      JdbcUtils.closeResultSet(rs);
      JdbcUtils.closeStatement(stmt);
      return response;
    }

    List<String> warnings = JdbcUtils.getWarnings(stmt);
    if (!BeeUtils.isEmpty(warnings)) {
      response.addWarning(warnings);
    }

    BeeResultSet br = new BeeResultSet();

    if (!BeeUtils.isEmpty(rFd)) {
      br.setFetchDirection(br.getFetchDirectionQuietly(rs));
      br.setInitialized();

      v1 = br.getFetchDirection();
      br.updateFetchDirection(rs, rFd);
      v2 = br.getFetchDirectionQuietly(rs);

      if (br.hasErrors()) {
        bc.revert(conn);
        response.addError(br.getErrors());
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        return response;
      }

      response.addInfo("Result Set Fetch Direction:", rFd,
          before, v1, JdbcUtils.fetchDirectionAsString(v1),
          after, v2, JdbcUtils.fetchDirectionAsString(v2));
    }

    if (!BeeUtils.isEmpty(rFs)) {
      ok = true;
      if (BeeUtils.inListSame(rFs, "min", "-")) {
        vu = Integer.MIN_VALUE;
      } else if (BeeUtils.isInt(rFs)) {
        vu = BeeUtils.toInt(rFs);
      } else {
        response.addWarning("Result Set Fetch Size:", rFs, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        br.setFetchSize(br.getFetchSizeQuietly(rs));
        br.setInitialized();

        v1 = br.getFetchSize();
        br.updateFetchSize(rs, vu);
        v2 = br.getFetchSizeQuietly(rs);

        if (br.hasErrors()) {
          bc.revert(conn);
          response.addError(br.getErrors());
          JdbcUtils.closeResultSet(rs);
          JdbcUtils.closeStatement(stmt);
          return response;
        }

        response.addInfo("Result Set Fetch Size:", rFs, BeeUtils.bracket(vu),
            before, v1, after, v2);
      }
    }

    if (BeeUtils.containsSame(ret, "col")) {
      ResponseObject mdResponse = rsb.getMetaData(rs);
      response.addMessagesFrom(mdResponse);
      if (mdResponse.hasResponse()) {
        response.setResponse(mdResponse.getResponse());
      }

    } else if (BeeUtils.containsSame(ret, "count")) {
      DateTime start = new DateTime();
      long memC1 = SystemInfo.freeMemory();
      int rc = JdbcUtils.getSize(rs);
      long memC2 = SystemInfo.freeMemory();
      DateTime end = new DateTime();

      response.addInfo(enter.toTimeString(), start.toTimeString(), end.toTimeString());
      response.addInfo(ret, rc,
          BeeUtils.bracket(TimeUtils.toSeconds(end.getTime() - start.getTime())),
          "type", JdbcUtils.getTypeInfo(rs));
      response.addInfo("memory", NameUtils.addName("executeQuery",
          BeeUtils.toString(memQ1 - memQ2)),
          NameUtils.addName(ret, BeeUtils.toString(memC1 - memC2)));

    } else if (BeeUtils.containsSame(ret, "meta")) {
      List<Property> result = new ArrayList<>();

      List<Property> info = BeeConnection.getInfo(conn);
      result.add(new Property("Connection", BeeUtils.bracket(info.size())));
      result.addAll(info);

      info = BeeStatement.getInfo(stmt);
      result.add(new Property("Statement", BeeUtils.bracket(info.size())));
      result.addAll(info);

      info = BeeResultSet.getInfo(rs);
      result.add(new Property("Result Set", BeeUtils.bracket(info.size())));
      result.addAll(info);

      response.setCollection(result, Property.class);

    } else {
      ResponseObject rsResponse = rsb.read(rs);
      response.addMessagesFrom(rsResponse);
      if (rsResponse.hasResponse()) {
        response.setResponse(rsResponse.getResponse());
      }
    }

    JdbcUtils.closeResultSet(rs);
    JdbcUtils.closeStatement(stmt);
    bc.revert(conn);

    return response;
  }
}
