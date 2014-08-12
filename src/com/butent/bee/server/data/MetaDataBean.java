package com.butent.bee.server.data;

import com.google.common.collect.Lists;

import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.jdbc.JdbcException;
import com.butent.bee.server.utils.BeeDataSource;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Handles rpc_db_meta service requests (database ping, current tables list etc).
 */

@Stateless
public class MetaDataBean {

  private static BeeLogger logger = LogUtils.getLogger(MetaDataBean.class);

  @EJB
  ResultSetBean rsb;
  @EJB
  SystemBean sys;

  public ResponseObject doService(String svc, BeeDataSource ds, RequestInfo reqInfo) {
    Assert.notEmpty(svc);
    Assert.notNull(ds);

    ResponseObject response;

    if (BeeUtils.same(svc, Service.DB_PING)) {
      response = ping(ds);

    } else if (BeeUtils.same(svc, Service.DB_INFO)) {
      response = dbInfo(ds, reqInfo);

    } else if (BeeUtils.same(svc, Service.DB_TABLES)) {
      response = getTables(ds, reqInfo);

    } else if (BeeUtils.same(svc, Service.DB_KEYS)) {
      response = getKeys(ds, reqInfo, true);

    } else if (BeeUtils.same(svc, Service.DB_PRIMARY)) {
      response = getKeys(ds, reqInfo, false);

    } else {
      String msg = BeeUtils.joinWords(svc, "meta data service not recognized");
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }

    return response;
  }

  private static ResponseObject dbInfo(BeeDataSource ds, RequestInfo reqInfo) {
    ResponseObject response = new ResponseObject();

    List<ExtendedProperty> prp = null;
    boolean ok = true;

    try {
      prp = ds.getDbInfo(reqInfo.getParameter(0));

    } catch (SQLException ex) {
      logger.error(ex);
      response.addError(ex);
      ok = false;
    }

    if (BeeUtils.isEmpty(prp)) {
      if (ok) {
        response.addInfo(ds.getDsn(), "no info available");
      }
    } else {
      response.setCollection(prp, ExtendedProperty.class);
    }

    return response;
  }

  private ResponseObject getKeys(BeeDataSource ds, RequestInfo reqInfo, boolean result) {
    String table = reqInfo.getParameter(0);
    String catalog = reqInfo.getParameter(1);
    String schema = reqInfo.getParameter(2);

    ResponseObject response;

    try {
      DatabaseMetaData md = ds.getDbMd();
      ResultSet rs = md.getPrimaryKeys(catalog, schema, table);

      if (result) {
        response = rsb.read(rs);

      } else {
        List<String> keys = Lists.newArrayList();
        while (rs.next()) {
          keys.add(rs.getString("COLUMN_NAME"));
        }
        response = ResponseObject.response(keys);
      }

      rs.close();

    } catch (JdbcException ex) {
      logger.error(ex);
      response = ResponseObject.error(ex);

    } catch (SQLException ex) {
      logger.error(ex);
      response = ResponseObject.error(ex);
    }

    return response;
  }

  private ResponseObject getTables(BeeDataSource ds, RequestInfo reqInfo) {
    String catalog = reqInfo.getParameter(Service.VAR_CATALOG);
    String schema = reqInfo.getParameter(Service.VAR_SCHEMA);

    String table = reqInfo.getParameter(Service.VAR_TABLE);
    String type = reqInfo.getParameter(Service.VAR_TYPE);

    boolean check = reqInfo.hasParameter(Service.VAR_CHECK);

    ResponseObject response;

    try {
      DatabaseMetaData md = ds.getDbMd();
      ResultSet rs = md.getTables(null, null, null, null);

      response = rsb.read(rs);
      rs.close();

    } catch (JdbcException ex) {
      logger.error(ex);
      response = ResponseObject.error(ex);

    } catch (SQLException ex) {
      logger.error(ex);
      response = ResponseObject.error(ex);
    }

    if (!response.hasErrors() && response.hasResponse(BeeRowSet.class)
        && !DataUtils.isEmpty((BeeRowSet) response.getResponse())) {

      if (BeeUtils.anyNotEmpty(catalog, schema, table, type) || check) {
        BeeRowSet tables = filterTables((BeeRowSet) response.getResponse(),
            catalog, schema, table, type, check);

        if (DataUtils.isEmpty(tables)) {
          response = ResponseObject.warning("no tables found");
        } else {
          response = ResponseObject.response(tables);
        }
      }
    }

    return response;
  }

  private BeeRowSet filterTables(BeeRowSet input, String catalog, String schema, String name,
      String type, boolean checkDescription) {

    int catIndex = 0;
    int schIndex = 1;
    int nameIndex = 2;
    int typeIndex = 3;

    boolean catCheck = !BeeUtils.isEmpty(catalog);
    boolean schCheck = !BeeUtils.isEmpty(schema);
    boolean nameCheck = !BeeUtils.isEmpty(name);
    boolean typeCheck = !BeeUtils.isEmpty(type);

    List<String> tableNames = checkDescription ? sys.getTableNames() : null;

    BeeRowSet result = new BeeRowSet(input.getColumns());

    for (BeeRow row : input) {
      if (catCheck && !BeeUtils.same(row.getString(catIndex), catalog)) {
        continue;
      }
      if (schCheck && !BeeUtils.same(row.getString(schIndex), schema)) {
        continue;
      }
      if (nameCheck && !BeeUtils.containsSame(row.getString(nameIndex), name)) {
        continue;
      }
      if (typeCheck && !BeeUtils.same(row.getString(typeIndex), type)) {
        continue;
      }

      if (checkDescription && tableNames.contains(row.getString(nameIndex))) {
        continue;
      }

      result.addRow(DataUtils.cloneRow(row));
    }

    return result;
  }

  private static ResponseObject ping(BeeDataSource ds) {
    return ResponseObject.response(ds.toString());
  }
}
