package com.butent.bee.server;

import static com.butent.bee.shared.html.builder.Factory.datalist;
import static com.butent.bee.shared.html.builder.Factory.option;

import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.html.builder.elements.Datalist;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class ProxyBean {

  @EJB
  QueryServiceBean qs;
  @EJB
  DataEditorBean deb;

  public ResponseObject commitRow(BeeRowSet rs) {
    return deb.commitRow(rs);
  }

  public Datalist getDataList(String tblName, String fldName) {
    String[] values = qs.getColumn(new SqlSelect().setDistinctMode(true)
        .addFields(tblName, fldName)
        .addFrom(tblName)
        .setWhere(SqlUtils.notNull(tblName, fldName))
        .addOrder(tblName, fldName));

    if (values == null || values.length <= 0) {
      return null;
    }

    Datalist datalist = datalist();

    for (String value : values) {
      datalist.append(option().value(value));
    }

    return datalist;
  }

  public ResponseObject insert(SqlInsert si) {
    return qs.insertDataWithResponse(si);
  }
}
