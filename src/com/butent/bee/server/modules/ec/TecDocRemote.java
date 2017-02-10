package com.butent.bee.server.modules.ec;

import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.sql.IsSql;
import com.butent.bee.server.sql.SqlBuilder;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
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

  static class TcdData {
    private final SqlCreate base;
    private final SqlSelect baseSource;
    private final List<String[]> baseIndexes = new ArrayList<>();
    private final List<IsSql> preparations = new ArrayList<>();

    TcdData(SqlCreate base, SqlSelect baseSource) {
      this.base = base;
      this.baseSource = baseSource;
    }

    void addPreparation(IsSql preparation) {
      this.preparations.add(preparation);
    }

    void addBaseIndexes(String... fldList) {
      this.baseIndexes.add(fldList);
    }
  }

  private static BeeLogger logger = LogUtils.getLogger(TecDocRemote.class);
  private static final int MAX_INSERT_BLOCK = 100000;

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  ConcurrencyBean cb;

  private DataSource dataSource;
  private DataSource crossaiDs;

  public void cleanup(List<IsSql> init) {
    for (IsSql query : init) {
      if (query instanceof SqlCreate) {
        processSql(getDataSource(), "DROP TABLE IF EXISTS " + ((SqlCreate) query).getTarget());
      }
    }
  }

  public void importTcd(List<TcdData> builds) {
    if (!qs.dbSchemaExists(sys.getDbName(), TecDocBean.TCD_SCHEMA)) {
      cb.synchronizedCall(TecDocBean.TCD_SCHEMA,
          () -> qs.updateData(SqlUtils.createSchema(TecDocBean.TCD_SCHEMA)));
    }
    for (TcdData entry : builds) {
      SqlCreate create = entry.base;
      SqlSelect query = entry.baseSource;

      if (qs.dbTableExists(sys.getDbName(), TecDocBean.TCD_SCHEMA, create.getTarget())) {
        continue; // qs.updateData(SqlUtils.dropTable(SqlUtils.table(tcdSchema, target)));
      }
      init(entry.preparations);

      String target = SqlUtils.table(TecDocBean.TCD_SCHEMA, create.getTarget());
      create.setTarget(target);

      cb.synchronizedCall(target, () -> qs.updateData(create));
      List<SqlCreate.SqlField> fields = create.getFields();

      int total = 0;
      int chunkTotal = 0;
      int chunk = query.getLimit();
      int offset = 0;

      do {
        if (chunk > 0) {
          chunkTotal = 0;
          query.setOffset(offset);
        }
        SqlInsert insert = new SqlInsert(target);

        for (SqlCreate.SqlField field : fields) {
          insert.addFields(field.getName());
        }
        List<StringBuilder> inserts = getRemoteData(query, insert);

        boolean isDebugEnabled = QueryServiceBean.debugOff();

        for (StringBuilder sql : inserts) {
          Holder<Integer> cnt = Holder.of(0);

          cb.synchronizedCall(target, () -> {
            Object value = qs.doSql(sql.toString());

            if (value instanceof Integer) {
              cnt.set((Integer) value);
            }
          });
          total += cnt.get();
          chunkTotal += cnt.get();
          logger.debug(target, "inserted rows:", total);
        }
        QueryServiceBean.debugOn(isDebugEnabled);

        if (chunk > 0) {
          offset += chunk;
        }
      } while (chunk > 0 && chunkTotal == chunk);

      for (String[] index : entry.baseIndexes) {
        cb.synchronizedCall(target, () -> qs.sqlIndex(target, index));
      }
      cleanup(entry.preparations);
    }
  }

  public void init(List<IsSql> init) {
    DataSource ds = getDataSource();
    SqlBuilder builder = SqlBuilderFactory.getBuilder(QueryServiceBean.dbEngine(ds));

    for (IsSql query : init) {
      if (query instanceof SqlCreate) {
        processSql(ds, "DROP TABLE IF EXISTS " + ((SqlCreate) query).getTarget());
      }
      processSql(ds, query.getSqlString(builder));
    }
  }

  public BeeRowSet getCrossai(SimpleRowSet orphans) {
    String tmp = SqlUtils.uniqueName();
    DataSource ds = getCrossaiDs();
    SqlBuilder builder = SqlBuilderFactory.getBuilder(QueryServiceBean.dbEngine(ds));

    processSql(ds, new SqlCreate(tmp)
        .setDataSource(new SqlSelect()
            .addField("tof_articles", "art_article_nr", "code")
            .addField("tof_articles", "art_article_nr", "brand")
            .addFrom("tof_articles")
            .setWhere(SqlUtils.sqlFalse()))
        .getSqlString(builder));

    BeeLogger messyLogger = LogUtils.getLogger(QueryServiceBean.class);
    boolean isDebugEnabled = messyLogger.isDebugEnabled();

    if (isDebugEnabled) {
      messyLogger.setLevel(LogLevel.INFO);
    }
    SqlInsert insert = new SqlInsert(tmp)
        .addFields("code", "brand");
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
    String bnovo = SqlUtils.uniqueName();

    processSql(ds, "CREATE TABLE " + bnovo + " ENGINE MyISAM AS SELECT"
        + "  CONVERT(code using ascii) collate ascii_bin AS code,"
        + "  CONVERT(brand using ascii) collate ascii_bin AS brand"
        + " FROM " + tmp);

    processSql(ds, SqlUtils.dropTable(tmp).getSqlString(builder));

    String union = new StringBuilder()
        .append("SELECT")
        .append("  Bnovo.code AS eolto_Kodas,")
        .append("  brands2.Brand AS eolto_Tiekejas,")
        .append("  tof_art_lookup.ARL_DISPLAY_NR AS analogo_Kodas,")
        .append("  brands.Brand AS analogo_Tiekejas")
        .append(" FROM")
        .append("  tof_art_lookup")
        .append(" Inner Join tof_articles ON tof_articles.ART_ID = tof_art_lookup.ARL_ART_ID")
        .append("   AND tof_art_lookup.ARL_KIND = 4")
        .append(" Inner Join brands ON tof_art_lookup.ARL_BRA_ID = brands.SupplierNr")
        .append(" Inner Join brands AS brands2 ON tof_articles.ART_SUP_ID = brands2.SupplierNr")
        .append(" Inner Join Bnovo ON Bnovo.code = tof_articles.ART_ARTICLE_NR")
        .append("   AND Bnovo.brand = brands2.Brand")
        .append(" UNION")
        .append(" SELECT")
        .append("  Bnovo.code  AS eolto_Kodas,")
        .append("  brands.Brand AS eolto_Tiekejas,")
        .append("  tof_articles.ART_ARTICLE_NR,")
        .append("  brands2.Brand AS analogo_Tiekejas")
        .append(" FROM")
        .append("  tof_art_lookup")
        .append(" Inner Join brands ON brands.SupplierNr = tof_art_lookup.ARL_BRA_ID")
        .append("   AND tof_art_lookup.ARL_KIND = 4")
        .append(" Inner Join Bnovo ON Bnovo.brand = brands.Brand")
        .append("   AND Bnovo.code = tof_art_lookup.ARL_DISPLAY_NR")
        .append(" Inner Join tof_articles ON tof_art_lookup.ARL_ART_ID = tof_articles.ART_ID")
        .append(" Inner Join brands AS brands2 ON tof_articles.ART_SUP_ID = brands2.SupplierNr")
        .append(" UNION")
        .append(" SELECT")
        .append("  Bnovo.code  AS eolto_Kodas,")
        .append("  brands.Brand AS eolto_Tiekejas,")
        .append("  tof_art_lookup.ARL_DISPLAY_NR  AS analogo_Kodas,")
        .append("  brands2.Brand AS analogo_Tiekejas")
        .append(" FROM")
        .append("  Bnovo")
        .append(" Inner Join brands ON Bnovo.brand = brands.Brand")
        .append(" Inner Join crossainew ON crossainew.SupplierId = brands.SupplierId")
        .append("   AND Bnovo.code = crossainew.CodeExistTcd")
        .append(" Inner Join tof_articles ON brands.SupplierNr = tof_articles.ART_SUP_ID")
        .append("   AND crossainew.CodeExistTcd = tof_articles.ART_ARTICLE_NR")
        .append(" Inner Join tof_art_lookup ON tof_articles.ART_ID = tof_art_lookup.ARL_ART_ID")
        .append("   AND tof_art_lookup.ARL_KIND = 4")
        .append(" Inner Join brands AS brands2 ON tof_art_lookup.ARL_BRA_ID = brands2.SupplierNr")
        .append(" UNION")
        .append(" SELECT")
        .append("  Bnovo.code,")
        .append("  brands.Brand,")
        .append("  tof_art_lookup.ARL_DISPLAY_NR,")
        .append("  brands2.Brand")
        .append(" FROM")
        .append("  Bnovo")
        .append(" Inner Join brands ON Bnovo.brand = brands.Brand")
        .append(" Inner Join crossainew ON crossainew.SupplierId = brands.SupplierNr")
        .append("   AND Bnovo.code = crossainew.Code")
        .append(" Inner Join tof_articles")
        .append("  ON crossainew.SupplierComperableId = tof_articles.ART_SUP_ID")
        .append("   AND crossainew.CodeComperable = tof_articles.ART_ARTICLE_NR")
        .append(" Inner Join tof_art_lookup ON tof_articles.ART_ID = tof_art_lookup.ARL_ART_ID")
        .append("   AND tof_art_lookup.ARL_KIND = 4")
        .append(" Inner Join brands AS brands2 ON tof_art_lookup.ARL_BRA_ID = brands2.SupplierNr")
        .append(" UNION")
        .append(" SELECT")
        .append("  Bnovo.code,")
        .append("  brands.Brand,")
        .append("  tof_articles.ART_ARTICLE_NR as analogo_Kodas,")
        .append("  brands2.Brand AS analogo_Tiekejas")
        .append(" FROM")
        .append("  Bnovo")
        .append(" Inner Join brands ON Bnovo.brand = brands.Brand")
        .append(" Inner Join crossainew ON crossainew.SupplierId = brands.SupplierNr")
        .append("   AND Bnovo.code = crossainew.Code")
        .append(" Inner Join tof_art_lookup")
        .append("  ON crossainew.CodeComperable = tof_art_lookup.ARL_DISPLAY_NR")
        .append("   AND tof_art_lookup.ARL_BRA_ID = crossainew.SupplierComperableId")
        .append("   AND tof_art_lookup.ARL_KIND = 4")
        .append(" Inner Join tof_articles ON tof_articles.ART_ID = tof_art_lookup.ARL_ART_ID")
        .append(" Inner Join brands AS brands2 ON tof_articles.ART_SUP_ID  = brands2.SupplierNr")
        .append(" UNION")
        .append(" SELECT")
        .append("  Bnovo.code,")
        .append("  brands.Brand,")
        .append("  crossainew.CodeComperable as analogo_Kodas,")
        .append("  brands2.Brand AS analogo_Tiekejas")
        .append(" FROM")
        .append("  Bnovo")
        .append(" Inner Join brands ON Bnovo.brand = brands.Brand")
        .append(" Inner Join crossainew ON crossainew.SupplierId = brands.SupplierNr")
        .append("   AND Bnovo.code = crossainew.Code")
        .append(" Inner Join brands AS brands2")
        .append("  ON crossainew.SupplierComperableId = brands2.SupplierNr")
        .toString();

    BeeRowSet rs = (BeeRowSet) processSql(ds, union.replace("Bnovo", bnovo));

    processSql(ds, SqlUtils.dropTable(bnovo).getSqlString(builder));

    return rs;
  }

  public List<StringBuilder> getRemoteData(SqlSelect query, final SqlInsert insert) {
    return qs.getData(getDataSource(), query, rs -> {
      SqlBuilder builder = SqlBuilderFactory.getBuilder();
      StringBuilder sb = new StringBuilder();
      int c = 0;
      List<StringBuilder> inserts = new ArrayList<>();

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
    });
  }

  private DataSource getCrossaiDs() {
    if (crossaiDs == null) {
      try {
        crossaiDs = InitialContext.doLookup("jdbc/crossai");
      } catch (NamingException ex) {
        try {
          crossaiDs = InitialContext.doLookup("java:jdbc/crossai");
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
        dataSource = InitialContext.doLookup("jdbc/tcd");
      } catch (NamingException ex) {
        try {
          dataSource = InitialContext.doLookup("java:jdbc/tcd");
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
