package com.butent.bee.server.modules.ec;

import com.google.common.collect.Lists;

import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.QueryServiceBean.ResultSetProcessor;
import com.butent.bee.server.sql.IsSql;
import com.butent.bee.server.sql.SqlBuilder;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@Stateless
@LocalBean
@TransactionManagement(TransactionManagementType.BEAN)
public class TecDocRemote {

  private static BeeLogger logger = LogUtils.getLogger(TecDocRemote.class);
  private static final int MAX_INSERT_BLOCK = 100000;

  @EJB
  QueryServiceBean qs;

  private DataSource dataSource;
  private DataSource crossaiDs;

  public void cleanup(List<IsSql> init) {
    for (IsSql query : init) {
      if (query instanceof SqlCreate) {
        processSql(getDataSource(), "DROP TABLE IF EXISTS " + ((SqlCreate) query).getTarget());
      }
    }
  }

  public void init(List<IsSql> init) {
    DataSource ds = getDataSource();
    SqlBuilder builder = SqlBuilderFactory.getBuilder(qs.dbEngine(ds));

    for (IsSql query : init) {
      if (query instanceof SqlCreate) {
        processSql(getDataSource(), "DROP TABLE IF EXISTS " + ((SqlCreate) query).getTarget());
      }
      processSql(getDataSource(), query.getSqlString(builder));
    }
  }

  public BeeRowSet getCrossai(SimpleRowSet orphans) {
    String tmp = "Bnovo";
    String[] fields = new String[] {"code", "brand"};
    DataSource ds = getCrossaiDs();
    SqlBuilder builder = SqlBuilderFactory.getBuilder(qs.dbEngine(ds));

    processSql(ds, SqlUtils.dropTable("IF EXISTS " + tmp).getSqlString(builder));

    processSql(ds, new SqlCreate(tmp, false)
        .setDataSource(new SqlSelect()
            .addField("tof_articles", "art_article_nr", "code")
            .addField("tof_articles", "art_article_nr", "brand")
            .addField("tof_articles", "art_des_id", "supplierid")
            .addFrom("tof_articles")
            .setWhere(SqlUtils.sqlFalse()))
        .getSqlString(builder));

    BeeLogger messyLogger = LogUtils.getLogger(QueryServiceBean.class);
    boolean isDebugEnabled = messyLogger.isDebugEnabled();

    if (isDebugEnabled) {
      messyLogger.setLevel(LogLevel.INFO);
    }
    SqlInsert insert = new SqlInsert(tmp)
        .addFields(fields);
    int c = 0;

    for (SimpleRow row : orphans) {
      insert.addValues((Object[]) row.getValues());

      if (++c % 1e4 == 0) {
        processSql(ds, insert.getSqlString(builder));
        insert.resetValues();
        logger.debug(tmp, "Inserted", c, "records");
      }
    }
    if (c % 1e4 > 0) {
      processSql(ds, insert.getSqlString(builder));
      logger.debug(tmp, "Inserted", c, "records");
    }
    if (isDebugEnabled) {
      messyLogger.setLevel(LogLevel.DEBUG);
    }
    processSql(ds, SqlUtils.createIndex(tmp, "brand", Lists.newArrayList("brand"), false)
        .getSqlString(builder));

    processSql(ds, new SqlUpdate(BeeUtils.joinItems(tmp, "brands"))
        .addExpression(SqlUtils.field(tmp, "supplierid").getSqlString(builder),
            SqlUtils.field("brands", "supplierid"))
        .setWhere(SqlUtils.joinUsing(tmp, "brands", "brand"))
        .getSqlString(builder));

    String union = new StringBuilder()
        .append("SELECT Bnovo.code AS Kodas,")
        .append("   Bnovo.brand AS Tiekejas,")
        .append("   tof_art_lookup.ARL_DISPLAY_NR AS AnalogoKodas,")
        .append("   brands2.Brand AS AnalogoTiekejas")
        .append(" FROM Bnovo")
        .append(" INNER JOIN brands ON Bnovo.supplierId = brands.SupplierId")
        .append(" INNER JOIN tof_articles ON Bnovo.code = tof_articles.ART_ARTICLE_NR")
        .append("   AND tof_articles.ART_SUP_ID = brands.SupplierNr")
        .append(" INNER JOIN tof_art_lookup ON tof_articles.ART_ID = tof_art_lookup.ARL_ART_ID")
        .append("   AND tof_art_lookup.ARL_KIND = 4")
        .append(" INNER JOIN brands AS brands2 ON tof_art_lookup.ARL_BRA_ID = brands2.SupplierNr")
        .append(" UNION ")
        .append("SELECT Bnovo.code AS Kodas,")
        .append("   Bnovo.brand AS Tiekejas,")
        .append("   tof_articles.ART_ARTICLE_NR AS AnalogoKodas,")
        .append("   brands2.Brand AS AnalogoTiekejas")
        .append(" FROM Bnovo")
        .append(" INNER JOIN brands ON Bnovo.supplierId = brands.SupplierId")
        .append(" INNER JOIN tof_art_lookup ON Bnovo.code = tof_art_lookup.ARL_DISPLAY_NR")
        .append("   AND tof_art_lookup.ARL_KIND = 4")
        .append("   AND brands.SupplierNr = tof_art_lookup.ARL_BRA_ID")
        .append(" INNER JOIN tof_articles ON tof_art_lookup.ARL_ART_ID = tof_articles.ART_ID")
        .append(" INNER JOIN brands AS brands2 ON tof_articles.ART_SUP_ID = brands2.SupplierNr")
        .toString();

    return (BeeRowSet) processSql(ds, union);
  }

  public List<StringBuilder> getRemoteData(SqlSelect query, final SqlInsert insert) {
    return qs.getData(getDataSource(), query, new ResultSetProcessor<List<StringBuilder>>() {
      @Override
      public List<StringBuilder> processResultSet(ResultSet rs) throws SQLException {
        SqlBuilder builder = SqlBuilderFactory.getBuilder();
        StringBuilder sb = new StringBuilder();
        int c = 0;
        List<StringBuilder> inserts = Lists.newArrayList();

        while (rs.next()) {
          if (c == 0) {
            sb = new StringBuilder(insert.getSqlString(builder));
          } else {
            sb.append(",");
          }
          sb.append("(");
          int i = 0;

          for (String field : insert.getFields()) {
            if (i > 0) {
              sb.append(",");
            }
            Object value;

            if (rs.getMetaData().getColumnType(i + 1) == -4 /* LONGBLOB */) {
              value = Codec.toBase64(rs.getBytes(field));
            } else {
              value = rs.getObject(field);
            }
            sb.append(SqlUtils.constant(value).getSqlString(builder));
            i++;
          }
          sb.append(")");
          c++;

          if (c == MAX_INSERT_BLOCK) {
            inserts.add(sb);
            c = 0;
          }
        }
        if (c > 0) {
          inserts.add(sb);
        }
        return inserts;
      }
    });
  }

  private DataSource getCrossaiDs() {
    if (crossaiDs == null) {
      try {
        crossaiDs = (DataSource) InitialContext.doLookup("jdbc/crossai");
      } catch (NamingException ex) {
        try {
          crossaiDs = (DataSource) InitialContext.doLookup("java:jdbc/crossai");
        } catch (NamingException ex2) {
          logger.error(ex);
          crossaiDs = null;
        }
      }
    }
    return crossaiDs;
  }

  private DataSource getDataSource() {
    if (dataSource == null) {
      try {
        dataSource = (DataSource) InitialContext.doLookup("jdbc/tcd");
      } catch (NamingException ex) {
        try {
          dataSource = (DataSource) InitialContext.doLookup("java:jdbc/tcd");
        } catch (NamingException ex2) {
          logger.error(ex);
          dataSource = null;
        }
      }
    }
    return dataSource;
  }

  private Object processSql(DataSource ds, String sql) {
    Assert.notNull(ds);

    Object response = qs.doSql(ds, sql);

    if (response instanceof String) {
      throw new BeeRuntimeException((String) response);
    }
    return response;
  }
}
