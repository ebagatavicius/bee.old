package com.butent.bee.egg.server.data;

import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.sql.BeeConstants.Keywords;
import com.butent.bee.egg.shared.sql.IsCondition;
import com.butent.bee.egg.shared.sql.SqlCreate;
import com.butent.bee.egg.shared.sql.SqlInsert;
import com.butent.bee.egg.shared.sql.SqlSelect;
import com.butent.bee.egg.shared.sql.SqlUpdate;
import com.butent.bee.egg.shared.sql.SqlUtils;
import com.butent.bee.egg.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class IdGeneratorBean {

  private static final String ID_TABLE = "bee_Sequence";
  private static final String ID_KEY = "SequenceName";
  private static final String ID_LAST = "SequenceValue";

  private static final int NEXT_ID_INDEX = 0;
  private static final int LAST_ID_INDEX = 1;

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;

  private int idChunk = 50;
  private Map<String, long[]> idCache = new HashMap<String, long[]>();

  @PreDestroy
  public void destroy() {
    for (Entry<String, long[]> entry : idCache.entrySet()) {
      String source = entry.getKey();
      IsCondition wh = SqlUtils.equal(ID_TABLE, ID_KEY, source);

      SqlSelect ss = new SqlSelect();
      ss.addFields(ID_TABLE, ID_LAST).addFrom(ID_TABLE).setWhere(wh);

      long lastId = qs.getSingleRow(ss).getLong(ID_LAST);

      if (entry.getValue()[LAST_ID_INDEX] == lastId) {
        String idFld = sys.getIdName(source);

        ss = new SqlSelect();
        ss.addMax(source, idFld).addFrom(source);

        lastId = qs.getSingleRow(ss).getLong(idFld);

        SqlUpdate su = new SqlUpdate(ID_TABLE);
        su.addField(ID_LAST, lastId).setWhere(wh);

        qs.updateData(su);
      }
    }
    idCache.clear();
  }

  public long getId(String source) {
    Assert.state(sys.beeTable(source));

    long[] ids = idCache.get(source);

    if (BeeUtils.isEmpty(ids) || ids[NEXT_ID_INDEX] == ids[LAST_ID_INDEX]) {
      ids = prepareId(source);
    }
    return ++ids[NEXT_ID_INDEX];
  }

  private long[] prepareId(String source) {
    int cnt = 0;
    IsCondition wh = SqlUtils.equal(ID_TABLE, ID_KEY, source);

    if (!qs.tableExists(ID_TABLE)) {
      SqlCreate sc = new SqlCreate(ID_TABLE);
      sc.addString(ID_KEY, 30, Keywords.NOTNULL, Keywords.UNIQUE)
        .addLong(ID_LAST, Keywords.NOTNULL);
      qs.updateData(sc);
    } else {
      SqlUpdate su = new SqlUpdate(ID_TABLE);
      su.addField(ID_LAST,
          SqlUtils.expression(SqlUtils.field(ID_LAST), "+",
              SqlUtils.constant(idChunk))).setWhere(wh);
      cnt = qs.updateData(su);
    }

    if (BeeUtils.isEmpty(cnt)) {
      String idFld = sys.getIdName(source);

      SqlSelect ss = new SqlSelect();
      ss.addMax(source, sys.getIdName(source)).addFrom(source);

      long lastId = qs.getSingleRow(ss).getLong(idFld);

      SqlInsert si = new SqlInsert(ID_TABLE);
      si.addField(ID_KEY, source).addField(ID_LAST, lastId + idChunk);
      cnt = qs.updateData(si);
    }
    SqlSelect ss = new SqlSelect();
    ss.addFields(ID_TABLE, ID_LAST).addFrom(ID_TABLE).setWhere(wh);

    long lastId = qs.getSingleRow(ss).getLong(ID_LAST);

    long[] ids = new long[2];
    ids[NEXT_ID_INDEX] = lastId - idChunk;
    ids[LAST_ID_INDEX] = lastId;

    idCache.put(source, ids);
    return ids;
  }
}
