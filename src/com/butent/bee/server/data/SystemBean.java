package com.butent.bee.server.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.server.Config;
import com.butent.bee.server.DataSourceBean;
import com.butent.bee.server.data.BeeTable.BeeCheck;
import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.data.BeeTable.BeeForeignKey;
import com.butent.bee.server.data.BeeTable.BeeIndex;
import com.butent.bee.server.data.BeeTable.BeeRelation;
import com.butent.bee.server.data.BeeTable.BeeTrigger;
import com.butent.bee.server.data.BeeTable.BeeUniqueKey;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.server.modules.ModuleHolderBean;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.sql.HasFrom;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.IsQuery;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.Defaults.DefaultExpression;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerEvent;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerScope;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerTiming;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerType;
import com.butent.bee.shared.data.XmlTable;
import com.butent.bee.shared.data.XmlTable.XmlCheck;
import com.butent.bee.shared.data.XmlTable.XmlConstraint;
import com.butent.bee.shared.data.XmlTable.XmlField;
import com.butent.bee.shared.data.XmlTable.XmlIndex;
import com.butent.bee.shared.data.XmlTable.XmlReference;
import com.butent.bee.shared.data.XmlTable.XmlTrigger;
import com.butent.bee.shared.data.XmlTable.XmlUnique;
import com.butent.bee.shared.data.XmlView;
import com.butent.bee.shared.data.XmlView.XmlColumn;
import com.butent.bee.shared.data.XmlView.XmlColumns;
import com.butent.bee.shared.data.XmlView.XmlSimpleColumn;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.SysObject;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.rights.RightsUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Ensures core data management functionality containing: data structures for tables and views,
 * current SQL server configuration, creating data tables only when they are in demand, handles data
 * with exceptions etc.
 */

@Singleton
@Lock(LockType.READ)
public class SystemBean {

  @EJB
  DataSourceBean dsb;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  ModuleHolderBean moduleBean;
  @EJB
  FileStorageBean fs;

  private static final BeeLogger logger = LogUtils.getLogger(SystemBean.class);
  private static boolean auditOff;

  private String dbName;
  private String dbSchema;
  private String dbAuditSchema;
  private final Map<String, BeeTable> tableCache = new HashMap<>();
  private final Map<String, BeeView> viewCache = new HashMap<>();

  private EventBus dataEventBus;
  private final Multimap<String, String> fileReferences = HashMultimap.create();

  public List<Property> checkTables(List<String> tbls, String progressId) {
    List<Property> diff = new ArrayList<>();
    List<String> tables;

    if (BeeUtils.isEmpty(tbls)) {
      initTables();
      tables = getTableNames();
    } else {
      tables = new ArrayList<>(tbls);
    }
    int size = tables.size();

    for (int i = 0; i < size; i++) {
      if (!BeeUtils.isEmpty(progressId)) {
        double value = i / (double) size;

        if (!Endpoint.updateProgress(progressId, value)) {
          diff.add(new Property("canceled", BeeUtils.progress(i, size)));
          break;
        }
      }
      createTable(getTable(tables.get(i)), diff);
    }
    return diff;
  }

  public String clampValue(String tblName, String fldName, String value) {
    if (value == null) {
      return null;
    } else if (value.isEmpty()) {
      return BeeConst.STRING_EMPTY;
    } else {
      int precision = getFieldPrecision(tblName, fldName);
      if (precision > 0 && value.length() > precision) {
        return BeeUtils.left(value.trim(), precision);
      } else {
        return value;
      }
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  @Lock(LockType.WRITE)
  public void ensureTables() {
    HashMultimap<String, String> flds = HashMultimap.create();

    for (SimpleRow row : qs.dbFields(getDbName(), getDbSchema(), null)) {
      flds.put(row.getValue(SqlConstants.TBL_NAME), row.getValue(SqlConstants.FLD_NAME));
    }
    for (String tblName : getTableNames()) {
      boolean rebuild = !flds.containsKey(tblName);

      if (!rebuild) {
        Collection<String> activeFields = getTableFieldNames(tblName);
        Set<String> existingFields = flds.get(tblName);

        for (String fldName : activeFields) {
          if (!existingFields.contains(fldName)) {
            logger.warning(tblName, fldName, "column not found, rebuilding");
            rebuild = true;
            break;
          }
        }
        if (!rebuild && activeFields.size() + 2 != existingFields.size()) {
          logger.warning(tblName, "column count doesn't match, rebuilding");
          rebuild = true;
        }
      } else {
        logger.warning(tblName, "table not found, rebuilding");
      }
      if (rebuild) {
        rebuildTable(tblName);
      }
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void eventEnd(long historyId, Object... result) {
    qs.updateData(new SqlUpdate(TBL_EVENT_HISTORY)
        .addConstant(COL_EVENT_ENDED, System.currentTimeMillis())
        .addConstant(COL_EVENT_RESULT, ArrayUtils.join(BeeConst.STRING_EOL, result))
        .setWhere(idEquals(TBL_EVENT_HISTORY, historyId)));
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void eventError(long historyId, Throwable err, Object... result) {
    List<Object> messages = new ArrayList<>();
    messages.add("ERROR");

    if (err != null) {
      Throwable cause = err;

      while (cause.getCause() != null) {
        cause = cause.getCause();
      }
      messages.add(cause.toString());
    }
    if (!ArrayUtils.isEmpty(result)) {
      messages.addAll(Arrays.asList(result));
    }
    qs.updateData(new SqlUpdate(TBL_EVENT_HISTORY)
        .addConstant(COL_EVENT_ENDED, System.currentTimeMillis())
        .addConstant(COL_EVENT_RESULT, BeeUtils.join(BeeConst.STRING_EOL, messages))
        .setWhere(idEquals(TBL_EVENT_HISTORY, historyId)));
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public long eventStart(String event) {
    return qs.insertData(new SqlInsert(TBL_EVENT_HISTORY)
        .addConstant(COL_EVENT, event)
        .addConstant(COL_EVENT_STARTED, System.currentTimeMillis()));
  }

  public void filterVisibleState(SqlSelect query, String tblName, String tblAlias) {
    BeeTable table = getTable(tblName);
    table.verifyState(query, tblAlias, RightsState.VIEW, usr.getUserRoles());
  }

  public String getAuditSource(String tableName) {
    return BeeUtils.join(".", dbAuditSchema, BeeUtils.join("_", tableName,
        AUDIT_SUFFIX));
  }

  public CellSource getCellSource(String viewName, String colName) {
    BeeView view = getView(viewName);

    int index = view.getRowSetIndex(colName);

    if (BeeConst.isUndef(index)) {
      return null;
    } else {
      return CellSource.forColumn(view.getBeeColumn(colName), index);
    }
  }

  public List<DataInfo> getDataInfo() {
    List<DataInfo> result = new ArrayList<>();

    Collection<String> viewNames = getViewNames();
    for (String viewName : viewNames) {
      result.add(getDataInfo(viewName));
    }

    return result;
  }

  public DataInfo getDataInfo(String viewName) {
    BeeView view = getView(viewName);
    BeeTable source = getTable(view.getSourceName());

    List<BeeColumn> columns = view.getRowSetColumns();
    List<ViewColumn> viewColumns = view.getViewColumns();

    return new DataInfo(view.getModule(), viewName, source.getName(), source.getIdName(),
        source.getVersionName(), view.getCaption(), view.getEditForm(),
        view.getRowCaption(), view.getNewRowForm(), view.getNewRowColumns(),
        view.getNewRowCaption(), view.getCacheMaximumSize(), view.getCacheEviction(),
        columns, viewColumns, view.getRelationInfo());
  }

  public String getDbName() {
    return dbName;
  }

  public String getDbSchema() {
    return dbSchema;
  }

  public int getFieldPrecision(String tblName, String fldName) {
    return getTable(tblName).getField(fldName).getPrecision();
  }

  public int getFieldScale(String tblName, String fldName) {
    return getTable(tblName).getField(fldName).getScale();
  }

  public String getIdName(String tblName) {
    return getTable(tblName).getIdName();
  }

  public BeeTable getTable(String tblName) {
    Assert.state(isTable(tblName), "Not a base table: " + tblName);
    return tableCache.get(BeeUtils.normalize(tblName));
  }

  public Map<String, Pair<DefaultExpression, Object>> getTableDefaults(String tblName) {
    return getTable(tblName).getDefaults();
  }

  public Collection<BeeField> getTableFields(String tblName) {
    return getTable(tblName).getFields();
  }

  public Collection<String> getTableFieldNames(String tblName) {
    return getTable(tblName).getFieldNames();
  }

  public List<ExtendedProperty> getTableInfo(String tblName) {
    return getTable(tblName).getExtendedInfo();
  }

  public List<String> getTableNames() {
    List<String> tables = new ArrayList<>();

    for (BeeTable table : getTables()) {
      tables.add(table.getName());
    }
    return tables;
  }

  public String getVersionName(String tblName) {
    return getTable(tblName).getVersionName();
  }

  public BeeView getView(String viewName) {
    Assert.state(isView(viewName), "Not a view: " + viewName);
    BeeView view = viewCache.get(BeeUtils.normalize(viewName));

    if (view == null) {
      view = getDefaultView(viewName);
      SysObject.register(view, viewCache, true, logger);
    }
    return view;
  }

  public Collection<String> getViewNames() {
    Collection<String> views = new ArrayList<>();

    for (BeeView view : viewCache.values()) {
      views.add(view.getName());
    }
    return views;
  }

  public Collection<BeeView> getViews() {
    return viewCache.values();
  }

  public String getViewSource(String viewName) {
    return getView(viewName).getSourceName();
  }

  public boolean hasField(String tblName, String fldName) {
    return getTable(tblName).hasField(fldName);
  }

  public IsCondition idEquals(String tblName, long id) {
    return SqlUtils.equals(tblName, getIdName(tblName), id);
  }

  public IsCondition idInList(String tblName, Collection<Long> ids) {
    return SqlUtils.inList(tblName, getIdName(tblName), ids);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  @Lock(LockType.WRITE)
  public void init() {
    auditOff = BeeUtils.toBoolean(Config.getProperty(Service.PROPERTY_AUDIT_OFF));
    dataEventBus = new EventBus();
    initTables();
  }

  @Lock(LockType.WRITE)
  public void initTables() {
    initTables(BeeUtils.notEmpty(SqlBuilderFactory.getDsn(), dsb.getDefaultDsn()));
  }

  @Lock(LockType.WRITE)
  public void initTables(String dsn) {
    Assert.state(SqlBuilderFactory.setDefaultBuilder(qs.dbEngine(dsn), dsn));
    dbName = qs.dbName();
    dbSchema = qs.dbSchema();
    dbAuditSchema = BeeUtils.join("_", dbSchema, AUDIT_SUFFIX);
    fileReferences.clear();

    initObjects(SysObject.TABLE);

    for (BeeTable table : getTables()) {
      for (BeeForeignKey fKey : table.getForeignKeys()) {
        Assert.state(isTable(fKey.getRefTable()),
            BeeUtils.joinWords("Unknown relation:", table.getName() + "." + fKey.getFields(),
                "->", fKey.getRefTable()));

        if (!BeeUtils.isEmpty(fKey.getRefFields())) {
          BeeTable refTable = getTable(fKey.getRefTable());

          for (String fld : fKey.getRefFields()) {
            Assert.state(refTable.hasField(fld),
                BeeUtils.joinWords("Unrecognized foreign key field:", refTable.getName(), fld));
          }
        }
        if (Objects.equals(fKey.getRefTable(), TBL_FILES)) {
          fileReferences.putAll(table.getName(), fKey.getFields());
        }
      }
    }
    initDbTables();
    initDbTriggers();
    updateVehicleModels();
  }

  @Lock(LockType.WRITE)
  public void initViews() {
    initObjects(SysObject.VIEW);

    Map<String, String> viewModules = new HashMap<>();

    for (BeeView view : getViews()) {
      viewModules.put(view.getName(), view.getModule());
    }
    RightsUtils.setViewModules(viewModules);
  }

  public boolean isAuditable(String tblName) {
    return isTable(tblName) && getTable(tblName).isAuditable();
  }

  public boolean isExtField(String tblName, String fldName) {
    return getTable(tblName).getField(fldName).isExtended();
  }

  public boolean isTable(String tblName) {
    return !BeeUtils.isEmpty(tblName) && tableCache.containsKey(BeeUtils.normalize(tblName));
  }

  public boolean isUnique(String tblName, String fldName) {
    return isTable(tblName) && getTable(tblName).hasField(fldName)
        && getTable(tblName).getField(fldName).isUnique();
  }

  public boolean isView(String viewName) {
    return !BeeUtils.isEmpty(viewName)
        && (viewCache.containsKey(BeeUtils.normalize(viewName)) || isTable(viewName));
  }

  public IsCondition joinTables(String tblName, String dstTable, String dstField) {
    return joinTables(tblName, null, dstTable, dstField);
  }

  public IsCondition joinTables(String tblName, String tblAlias, String dstTable, String dstField) {
    return SqlUtils.join(BeeUtils.notEmpty(tblAlias, tblName), getIdName(tblName),
        dstTable, dstField);
  }

  public String joinTranslationField(HasFrom<?> query, String tblName, String tblAlias,
      String fldName, String locale) {
    Assert.notNull(query);
    BeeTable table = getTable(tblName);
    BeeField field = table.getField(fldName);

    return table.joinTranslationField(query, tblAlias, field, locale);
  }

  public void normalizeFileReference(SqlInsert insert) {
    if (!insert.isMultipleInsert() && fileReferences.containsKey(insert.getTarget())) {
      insert.getFields()
          .stream()
          .filter(f -> fileReferences.containsEntry(insert.getTarget(), f))
          .forEach(f -> {
            Object expr = insert.getValue(f).getValue();

            if (expr instanceof Value) {
              try {
                insert.updExpression(f, SqlUtils.constant(fs.commitFile(((Value) expr).getLong())));
              } catch (IOException e) {
                logger.error(e);
              }
            }
          });
    }
  }

  public void normalizeFileReference(SqlUpdate update) {
    if (fileReferences.containsKey(update.getTarget())) {
      update.getUpdates().keySet()
          .stream()
          .filter(f -> fileReferences.containsEntry(update.getTarget(), f))
          .forEach(f -> {
            Object expr = update.getValue(f);

            if (expr instanceof IsExpression) {
              expr = ((IsExpression) expr).getValue();
            }
            if (expr instanceof Value) {
              try {
                update.updExpression(f, SqlUtils.constant(fs.commitFile(((Value) expr).getLong())));
              } catch (IOException e) {
                logger.error(e);
              }
            }
          });
    }
  }

  public void postDataEvent(DataEvent event) {
    dataEventBus.post(event);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  @Lock(LockType.WRITE)
  public void rebuildActiveTables() {
    initTables();

    for (BeeTable table : getTables()) {
      if (table.isActive()) {
        rebuildTable(table);
      }
    }
  }

  @Lock(LockType.WRITE)
  public void rebuildTable(String tblName) {
    rebuildTable(getTable(tblName));
  }

  @Lock(LockType.WRITE)
  public void registerDataEventHandler(DataEventHandler eventHandler) {
    dataEventBus.register(eventHandler);
  }

  private void createAuditTables(BeeTable table) {
    if (!table.isAuditable()) {
      return;
    }
    if (!qs.dbSchemaExists(dbName, dbAuditSchema)) {
      makeStructureChanges(SqlUtils.createSchema(dbAuditSchema));
    }
    String auditName = BeeUtils.join("_", table.getName(), AUDIT_SUFFIX);
    String auditPath = BeeUtils.join(".", dbAuditSchema, auditName);

    if (!qs.dbTableExists(dbName, dbAuditSchema, auditName)) {
      makeStructureChanges(
          new SqlCreate(auditPath, false)
              .addDateTime(AUDIT_FLD_TIME, true)
              .addLong(AUDIT_FLD_USER, false)
              .addLong(AUDIT_FLD_TX, false)
              .addString(AUDIT_FLD_MODE, 1, true)
              .addLong(AUDIT_FLD_ID, true)
              .addString(AUDIT_FLD_FIELD, 30, false)
              .addText(AUDIT_FLD_VALUE, false),
          SqlUtils.createIndex(auditPath, "IK_" + Codec.crc32(auditName + AUDIT_FLD_ID),
              Lists.newArrayList(AUDIT_FLD_ID), false));
    }
  }

  private void createChecks(Collection<BeeCheck> checks) {
    for (BeeCheck check : checks) {
      makeStructureChanges(SqlUtils.createCheck(check.getTable(), check.getName(),
          check.getExpression()));
    }
  }

  private void createForeignKeys(Collection<BeeForeignKey> fKeys) {
    HashMultimap<String, String> flds = HashMultimap.create();

    for (SimpleRow row : qs.dbFields(getDbName(), getDbSchema(), null)) {
      flds.put(row.getValue(SqlConstants.TBL_NAME), row.getValue(SqlConstants.FLD_NAME));
    }
    for (BeeForeignKey fKey : fKeys) {
      String tblName = fKey.getTable();
      List<String> fields = fKey.getFields();
      String refTblName = fKey.getRefTable();
      List<String> refFields = fKey.getRefFields();

      boolean ok = true;

      for (String fldName : fields) {
        if (!flds.containsEntry(tblName, fldName)) {
          ok = false;
          break;
        }
      }
      if (ok) {
        if (!BeeUtils.isEmpty(refFields)) {
          for (String fldName : refFields) {
            if (!flds.containsEntry(refTblName, fldName)) {
              ok = false;
              break;
            }
          }
        } else {
          refFields = Lists.newArrayList(getIdName(refTblName));
        }
      }
      if (ok) {
        makeStructureChanges(SqlUtils.createForeignKey(tblName, fKey.getName(),
            fields, refTblName, refFields, fKey.getCascade()));
      }
    }
  }

  private void createIndexes(Collection<BeeIndex> indexes) {
    for (BeeIndex index : indexes) {
      if (!BeeUtils.isEmpty(index.getExpression())) {
        makeStructureChanges(SqlUtils.createIndex(index.getTable(), index.getName(),
            index.getExpression(), index.isUnique()));
      } else {
        makeStructureChanges(SqlUtils.createIndex(index.getTable(), index.getName(),
            index.getFields(), index.isUnique()));
      }
    }
  }

  private Map<String, String> createTable(BeeTable table, List<Property> diff) {
    String tblName = table.getName();
    Map<String, SqlCreate> newTables = new HashMap<>();

    newTables.put(tblName, new SqlCreate(tblName, false)
        .addLong(table.getIdName(), true)
        .addLong(table.getVersionName(), true));

    for (BeeField field : table.getFields()) {
      tblName = field.getStorageTable();

      if (field.isExtended()) {
        SqlCreate sc = table.createExtTable(newTables.get(tblName), field);

        if (sc != null) {
          newTables.put(tblName, sc);
        }
      } else if (!BeeUtils.isEmpty(field.getExpression())) {
        newTables.get(tblName).addField(field.getName(), field.getType(), field.getExpression(),
            field.isNotNull());
      } else {
        newTables.get(tblName).addField(field.getName(), field.getType(), field.getPrecision(),
            field.getScale(), field.isNotNull());
      }
      if (field.isTranslatable()) {
        tblName = table.getTranslationTable(field);
        SqlCreate sc = table.createTranslationTable(newTables.get(tblName), field);

        if (sc != null) {
          newTables.put(tblName, sc);
        }
      }
    }
    for (RightsState state : BeeTable.getStates()) {
      tblName = table.getStateTable(state);
      SqlCreate sc = table.createStateTable(newTables.get(tblName), state);

      if (sc != null) {
        newTables.put(tblName, sc);
      }
    }
    Map<String, String> rebuilds = new HashMap<>();

    for (SqlCreate sc : newTables.values()) {
      tblName = sc.getTarget();
      String tblBackup = null;
      boolean update = !qs.dbTableExists(getDbName(), getDbSchema(), tblName);

      if (update) {
        if (diff != null) {
          PropertyUtils.addProperty(diff, tblName, "DOES NOT EXIST");
          return null;
        } else {
          makeStructureChanges(sc);
        }
      } else {
        tblBackup = tblName + "_BAK";
        logger.debug("Checking indexes...");
        int c = 0;
        Set<String> indexes = new HashSet<>();

        for (String index : qs.dbIndexes(getDbName(), getDbSchema(), tblName)
            .getColumn(SqlConstants.KEY_NAME)) {

          if (index.startsWith(BeeTable.UNIQUE_INDEX_PREFIX)
              || index.startsWith(BeeTable.INDEX_KEY_PREFIX)) {
            indexes.add(index);
          }
        }
        for (BeeIndex index : table.getIndexes()) {
          if (BeeUtils.same(index.getTable(), tblName)) {
            if (indexes.contains(index.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("INDEX", index.getName(), index.getFields(),
                  "NOT IN", indexes);
              logger.warning(tblName, msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
        }
        if (!update && indexes.size() > c) {
          String msg = "TOO MANY INDEXES";
          logger.warning(tblName, msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update) {
        logger.debug("Checking unique keys...");
        int c = 0;
        Set<String> keys = new HashSet<>();

        for (String key : qs.dbConstraints(getDbName(), getDbSchema(), tblName,
            SqlKeyword.UNIQUE, SqlKeyword.PRIMARY_KEY).getColumn(SqlConstants.KEY_NAME)) {

          if (key.startsWith(BeeTable.UNIQUE_KEY_PREFIX)
              || key.startsWith(BeeTable.PRIMARY_KEY_PREFIX)) {
            keys.add(key);
          }
        }
        for (BeeUniqueKey key : table.getUniqueKeys()) {
          if (BeeUtils.same(key.getTable(), tblName)) {
            if (keys.contains(key.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("KEY", key.getName(), key.getFields(), "NOT IN",
                  keys);
              logger.warning(tblName, msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
        }
        if (!update && keys.size() > c) {
          String msg = "TOO MANY UNIQUE KEYS";
          logger.warning(tblName, msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update) {
        logger.debug("Checking foreign keys...");
        int c = 0;
        Set<String> fKeys = new HashSet<>();

        for (String fKey : qs.dbConstraints(getDbName(), getDbSchema(), tblName,
            SqlKeyword.FOREIGN_KEY).getColumn(SqlConstants.KEY_NAME)) {

          if (fKey.startsWith(BeeTable.FOREIGN_KEY_PREFIX)) {
            fKeys.add(fKey);
          }
        }
        for (BeeForeignKey fKey : table.getForeignKeys()) {
          if (BeeUtils.same(fKey.getTable(), tblName)
              && (BeeUtils.same(fKey.getRefTable(), table.getName())
              || getTable(fKey.getRefTable()).isActive())) {

            if (fKeys.contains(fKey.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("FOREIGN KEY", fKey.getName(),
                  BeeUtils.parenthesize(BeeUtils.join(" ON DELETE ",
                      fKey.getFields() + "->" + fKey.getRefTable(), fKey.getCascade())),
                  "NOT IN", fKeys);
              logger.warning(tblName, msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
        }
        if (!update && fKeys.size() > c) {
          String msg = "TOO MANY FOREIGN KEYS";
          logger.warning(tblName, msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update) {
        logger.debug("Checking check constraints...");
        int c = 0;
        Set<String> checks = new HashSet<>();

        for (String check : qs.dbConstraints(getDbName(), getDbSchema(), tblName, SqlKeyword.CHECK)
            .getColumn(SqlConstants.KEY_NAME)) {

          if (check.startsWith(BeeTable.CHECK_PREFIX)) {
            checks.add(check);
          }
        }
        for (BeeCheck check : table.getChecks()) {
          if (BeeUtils.same(check.getTable(), tblName)) {
            if (checks.contains(check.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("CHECK", check.getName(), "NOT IN", checks);
              logger.warning(tblName, msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
        }
        if (!update && checks.size() > c) {
          String msg = "TOO MANY CHECK CONSTRAINTS";
          logger.warning(tblName, msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update) {
        logger.debug("Checking triggers...");
        int c = 0;
        Set<String> triggers = new HashSet<>();

        for (String trigger : qs.dbTriggers(getDbName(), getDbSchema(), tblName)
            .getColumn(SqlConstants.TRIGGER_NAME)) {

          if (trigger.startsWith(BeeTable.TRIGGER_PREFIX)) {
            triggers.add(trigger);
          }
        }
        for (BeeTrigger trigger : table.getTriggers()) {
          if (BeeUtils.same(trigger.getTable(), tblName)) {
            if (triggers.contains(trigger.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("TRIGGER", trigger.getName(), "NOT IN",
                  BeeUtils.parenthesize(triggers));
              logger.warning(tblName, msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              }
            }
          }
        }
        if (triggers.size() > c) {
          String msg = "TOO MANY TRIGGERS";
          logger.warning(tblName, msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update && table.isAuditable() && isTable(tblName)) {
        logger.debug("Checking audit tables...");
        String auditName = BeeUtils.join("_", tblName, AUDIT_SUFFIX);

        if (!qs.dbTableExists(dbName, dbAuditSchema, auditName)) {
          String msg = BeeUtils.joinWords("AUDIT TABLE",
              BeeUtils.join(".", dbAuditSchema, auditName), "DOES NOT EXIST");
          logger.warning(tblName, msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          }
        }
      }
      if (!BeeUtils.isEmpty(tblBackup)) {
        if (qs.dbTableExists(getDbName(), getDbSchema(), tblBackup)) {
          makeStructureChanges(SqlUtils.dropTable(tblBackup));
        }
        makeStructureChanges(sc.setTarget(tblBackup));

        SimpleRowSet oldFields = qs.dbFields(getDbName(), getDbSchema(), tblName);
        SimpleRowSet newFields = qs.dbFields(getDbName(), getDbSchema(), tblBackup);

        if (!update) {
          logger.debug("Checking fields...");
          int c = 0;

          for (SimpleRow newFieldInfo : newFields) {
            SimpleRow oldFieldInfo = null;
            String fldName = newFieldInfo.getValue(SqlConstants.FLD_NAME);

            for (SimpleRow oldInfo : oldFields) {
              if (BeeUtils.same(oldInfo.getValue(SqlConstants.FLD_NAME), fldName)) {
                c++;
                oldFieldInfo = oldInfo;
                break;
              }
            }
            if (oldFieldInfo != null) {
              for (String info : oldFieldInfo.getColumnNames()) {
                if (!BeeUtils.same(info, SqlConstants.TBL_NAME)
                    && !Objects.equals(oldFieldInfo.getValue(info), newFieldInfo.getValue(info))) {

                  String msg = BeeUtils.joinWords("FIELD", fldName + ":",
                      info, oldFieldInfo.getValue(info), "!=", newFieldInfo.getValue(info));
                  logger.warning(tblName, msg);

                  if (diff != null) {
                    PropertyUtils.addProperty(diff, tblName, msg);
                  } else {
                    update = true;
                    break;
                  }
                }
              }
              if (update) {
                break;
              }
            } else {
              String msg = BeeUtils.joinWords("FIELD", fldName, "DOES NOT EXIST");
              logger.warning(tblName, msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
          if (!update && oldFields.getNumberOfRows() > c) {
            String msg = "TOO MANY FIELDS";
            logger.warning(tblName, msg);

            if (diff != null) {
              PropertyUtils.addProperty(diff, tblName, msg);
            } else {
              update = true;
            }
          }
        }
        if (update) {
          Map<String, String> updFlds = new LinkedHashMap<>();
          String[] oldList = oldFields.getColumn(SqlConstants.FLD_NAME);

          for (String newFld : newFields.getColumn(SqlConstants.FLD_NAME)) {
            for (String oldFld : oldList) {
              if (BeeUtils.same(newFld, oldFld)) {
                updFlds.put(newFld, oldFld);
              }
            }
          }
          if (!BeeUtils.isEmpty(updFlds)) {
            Object res = qs.doSql(new SqlInsert(tblBackup)
                .addFields(updFlds.keySet().toArray(new String[0]))
                .setDataSource(new SqlSelect()
                    .addFields(tblName, updFlds.values().toArray(new String[0]))
                    .addFrom(tblName))
                .getQuery());

            Assert.state(res instanceof Number, BeeUtils.join(": ", "Error inserting data", res));
          }
        } else {
          makeStructureChanges(SqlUtils.dropTable(tblBackup));
        }
      }
      if (update) {
        rebuilds.put(tblName, tblBackup);
      }
    }
    return rebuilds;
  }

  private void createTriggers(Collection<BeeTrigger> triggers) {
    Multimap<String, String> dbTriggers = HashMultimap.create();

    for (BeeTrigger trigger : triggers) {
      String tblName = trigger.getTable();
      String trgName = trigger.getName();

      if (!dbTriggers.containsKey(tblName)) {
        for (String triggerName : qs.dbTriggers(getDbName(), getDbSchema(), tblName)
            .getColumn(SqlConstants.TRIGGER_NAME)) {
          dbTriggers.put(tblName, triggerName);
        }
      }
      if (!dbTriggers.containsEntry(tblName, trgName)) {
        dbTriggers.put(tblName, trgName);

        makeStructureChanges(SqlUtils.createTrigger(trgName, tblName, trigger.getType(),
            trigger.getParameters(), trigger.getTiming(), trigger.getEvents(), trigger.getScope()));
      }
    }
  }

  private void createUniqueKeys(Collection<BeeUniqueKey> uniqueKeys) {
    for (BeeUniqueKey uniqueKey : uniqueKeys) {
      IsQuery query;

      if (uniqueKey.isPrimary()) {
        query = SqlUtils.createPrimaryKey(uniqueKey.getTable(), uniqueKey.getName(),
            uniqueKey.getFields());
      } else {
        query = SqlUtils.createUniqueKey(uniqueKey.getTable(), uniqueKey.getName(),
            uniqueKey.getFields());
      }
      makeStructureChanges(query);
    }
  }

  private BeeView getDefaultView(String tblName) {
    XmlColumns columns = new XmlColumns();
    columns.columns = new ArrayList<>();

    for (BeeField field : getTableFields(tblName)) {
      XmlColumn column = new XmlSimpleColumn();
      column.name = field.getName();
      columns.columns.add(column);
    }
    XmlView xmlView = new XmlView();
    xmlView.name = tblName;
    xmlView.source = tblName;
    xmlView.columns = columns;

    return new BeeView(getTable(tblName).getModule(), xmlView, tableCache);
  }

  public Collection<BeeTable> getTables() {
    return ImmutableList.copyOf(tableCache.values());
  }

  private boolean initDataObject(SysObject obj, String moduleName, String objectName,
      String resource, boolean initial) {

    String schema = Config.getSchemaPath(obj.getSchemaName());

    try {
      switch (obj) {
        case TABLE:
          BeeTable table = initTable(moduleName, objectName,
              XmlUtils.unmarshal(XmlTable.class, resource, schema));

          return SysObject.register(table, tableCache, initial, logger);
        case VIEW:
          BeeView view = initView(moduleName, objectName,
              XmlUtils.unmarshal(XmlView.class, resource, schema));

          return SysObject.register(view, viewCache, initial, logger);
        default:
          return false;
      }
    } catch (Throwable e) {
      logger.error(e);
      return false;
    }
  }

  private void initDbTables() {
    String[] dbTables = qs.dbTables(dbName, dbSchema, null).getColumn(SqlConstants.TBL_NAME);
    Set<String> names = new HashSet<>();

    for (String name : dbTables) {
      names.add(BeeUtils.normalize(name));
    }
    for (BeeTable table : getTables()) {
      String tblName = table.getName();
      table.setActive(names.contains(BeeUtils.normalize(tblName)));

      Map<String, String[]> tableFields = new HashMap<>();

      for (RightsState state : BeeTable.getStates()) {
        tblName = table.getStateTable(state);

        if (names.contains(BeeUtils.normalize(tblName))) {
          if (!tableFields.containsKey(tblName)) {
            tableFields.put(tblName,
                qs.dbFields(getDbName(), getDbSchema(), tblName).getColumn(SqlConstants.FLD_NAME));
          }
          table.initState(state, Sets.newHashSet(tableFields.get(tblName)));
        }
      }
    }
  }

  private void initDbTriggers() {
    for (BeeTable table : getTables()) {
      table.addTrigger(table.getName(), SqlTriggerType.VERSION,
          Collections.singletonMap("versionName", table.getVersionName()),
          SqlTriggerTiming.BEFORE, EnumSet.of(SqlTriggerEvent.INSERT, SqlTriggerEvent.UPDATE),
          SqlTriggerScope.ROW);

      Map<String, List<Map<String, String>>> tr = new HashMap<>();

      for (BeeField field : table.getFields()) {
        if (field instanceof BeeRelation && ((BeeRelation) field).isEditable()) {
          String tblName = field.getStorageTable();
          String relTable = ((BeeRelation) field).getRelation();

          List<Map<String, String>> entry = tr.get(tblName);

          if (BeeUtils.isEmpty(entry)) {
            entry = new ArrayList<>();
            tr.put(tblName, entry);
          }
          entry.add(ImmutableMap.of("field", field.getName(),
              "relTable", relTable, "relField", getIdName(relTable)));
        }
      }
      for (String tblName : tr.keySet()) {
        table.addTrigger(tblName, SqlTriggerType.RELATION,
            ImmutableMap.of("fields", tr.get(tblName)),
            SqlTriggerTiming.AFTER, EnumSet.of(SqlTriggerEvent.DELETE), SqlTriggerScope.ROW);
      }

      if (table.isAuditable()) {
        HashMultimap<String, String> fields = HashMultimap.create();

        for (BeeField field : table.getFields()) {
          if (field.isAuditable()) {
            fields.put(field.getStorageTable(), field.getName());
          }
        }
        for (String tblName : fields.keySet()) {
          table.addTrigger(tblName, SqlTriggerType.AUDIT,
              ImmutableMap.of("auditSchema", dbAuditSchema,
                  "auditTable", BeeUtils.join("_", table.getName(), AUDIT_SUFFIX),
                  "idName", table.getIdName(),
                  "fields", fields.get(tblName)),
              SqlTriggerTiming.AFTER,
              EnumSet.of(SqlTriggerEvent.INSERT, SqlTriggerEvent.UPDATE, SqlTriggerEvent.DELETE),
              SqlTriggerScope.ROW);
        }
      }
    }
  }

  private void initObjects(SysObject obj) {
    Assert.notNull(obj);

    switch (obj) {
      case TABLE:
        tableCache.clear();
        break;
      case VIEW:
        viewCache.clear();
        break;
      default:
        Assert.unsupported("Not a data object: " + obj.getName());
    }
    int cnt = 0;
    String ext = "." + obj.getFileExtension();

    for (String moduleName : moduleBean.getModules()) {
      List<File> resources = FileUtils.findFiles("*" + ext,
          Collections.singleton(new File(Config.CONFIG_DIR,
              moduleBean.getResourcePath(moduleName, obj.getPath()))), null, null, false, true);

      if (!BeeUtils.isEmpty(resources)) {
        for (File resource : resources) {
          String resourcePath = resource.getPath();
          String objectName = BeeUtils.removeSuffix(FileNameUtils.getName(resourcePath), ext);
          cnt += initDataObject(obj, moduleName, objectName, resourcePath, true) ? 1 : 0;
        }
      }
    }
    if (!qs.dbTableExists(getDbName(), getDbSchema(), TBL_CUSTOM_CONFIG)) {
      return;
    }
    Multimap<String, Pair<String, String>> custom = HashMultimap.create();

    BeeRowSet rs = (BeeRowSet) qs.doSql(new SqlSelect()
        .addFields(TBL_CUSTOM_CONFIG, COL_CONFIG_MODULE, COL_CONFIG_OBJECT, COL_CONFIG_DATA)
        .addFrom(TBL_CUSTOM_CONFIG)
        .setWhere(SqlUtils.equals(TBL_CUSTOM_CONFIG, COL_CONFIG_TYPE, obj)).getQuery());

    for (int i = 0; i < rs.getNumberOfRows(); i++) {
      Module module = EnumUtils.getEnumByIndex(Module.class, rs.getInteger(i, COL_CONFIG_MODULE));

      if (Objects.nonNull(module)) {
        custom.put(module.getName(),
            Pair.of(rs.getString(i, COL_CONFIG_OBJECT), rs.getString(i, COL_CONFIG_DATA)));
      }
    }
    for (String moduleName : moduleBean.getModules()) {
      for (Pair<String, String> pair : custom.get(moduleName)) {
        cnt += initDataObject(obj, moduleName, pair.getA(), pair.getB(), false) ? 1 : 0;
      }
    }
    if (cnt <= 0) {
      logger.severe("No", obj.getName(), "descriptions found");
    } else {
      logger.info("Loaded", cnt, obj.getName(), "descriptions");
    }
  }

  private static BeeTable initTable(String moduleName, String tableName, XmlTable xmlTable) {
    Assert.notEmpty(tableName);
    BeeTable table = null;

    if (xmlTable != null) {
      if (!BeeUtils.same(xmlTable.name, tableName)) {
        logger.warning("Table name doesn't match resource name:", xmlTable.name, "!=", tableName);
      } else {
        table = new BeeTable(moduleName, xmlTable, auditOff);
        String tbl = table.getName();

        if (!BeeUtils.isEmpty(xmlTable.fields)) {
          for (XmlField field : xmlTable.fields) {
            String expression;

            switch (SqlBuilderFactory.getBuilder().getEngine()) {
              case POSTGRESQL:
                expression = field.postgreSql;
                break;
              case MSSQL:
                expression = field.msSql;
                break;
              case ORACLE:
                expression = field.oracle;
                break;
              default:
                expression = null;
                break;
            }
            table.addField(field, expression, false);
          }
        }
        if (!BeeUtils.isEmpty(xmlTable.indexes)) {
          for (XmlIndex index : xmlTable.indexes) {
            String expression;

            switch (SqlBuilderFactory.getBuilder().getEngine()) {
              case POSTGRESQL:
                expression = index.postgreSql;
                break;
              case MSSQL:
                expression = index.msSql;
                break;
              case ORACLE:
                expression = index.oracle;
                break;
              default:
                expression = null;
                break;
            }
            if (!BeeUtils.isEmpty(expression)) {
              table.addIndex(tableName, expression, index.unique);

            } else if (!BeeUtils.isEmpty(index.fields)) {
              for (String fld : index.fields) {
                Assert.state(table.hasField(fld),
                    BeeUtils.joinWords("Unrecognized index field:", tbl, fld));
              }
              table.addIndex(tableName, index.fields, index.unique);
            }
          }
        }
        if (!BeeUtils.isEmpty(xmlTable.constraints)) {
          for (XmlConstraint constraint : xmlTable.constraints) {
            if (constraint instanceof XmlCheck) {
              String expression;

              switch (SqlBuilderFactory.getBuilder().getEngine()) {
                case POSTGRESQL:
                  expression = constraint.postgreSql;
                  break;
                case MSSQL:
                  expression = constraint.msSql;
                  break;
                case ORACLE:
                  expression = constraint.oracle;
                  break;
                default:
                  expression = null;
                  break;
              }
              if (!BeeUtils.isEmpty(expression)) {
                table.addCheck(tableName, expression);
              }
            } else if (constraint instanceof XmlUnique) {
              List<String> fields = ((XmlUnique) constraint).fields;

              if (!BeeUtils.isEmpty(fields)) {
                for (String fld : fields) {
                  Assert.state(table.hasField(fld),
                      BeeUtils.joinWords("Unrecognized unique key field:", tbl, fld));
                }
                table.addUniqueKey(tableName, fields);
              }
            } else if (constraint instanceof XmlReference) {
              List<String> fields = ((XmlReference) constraint).fields;
              List<String> refFields = ((XmlReference) constraint).refFields;

              if (!BeeUtils.isEmpty(fields)) {
                for (String fld : fields) {
                  Assert.state(table.hasField(fld),
                      BeeUtils.joinWords("Unrecognized foreign key field:", tbl, fld));
                }
                Assert.state(BeeUtils.isEmpty(refFields)
                        ? fields.size() == 1 : fields.size() == refFields.size(),
                    "Field count doesn't match");

                table.addForeignKey(tableName, fields, ((XmlReference) constraint).refTable,
                    refFields, EnumUtils.getEnumByName(SqlKeyword.class,
                        ((XmlReference) constraint).cascade));
              }
            }
          }
        }
        if (!BeeUtils.isEmpty(xmlTable.triggers)) {
          for (XmlTrigger trigger : xmlTable.triggers) {
            String body;
            List<SqlTriggerEvent> events = new ArrayList<>();

            for (String event : trigger.events) {
              events.add(EnumUtils.getEnumByName(SqlTriggerEvent.class, event));
            }
            switch (SqlBuilderFactory.getBuilder().getEngine()) {
              case POSTGRESQL:
                body = trigger.postgreSql;
                break;
              case MSSQL:
                body = trigger.msSql;
                break;
              case ORACLE:
                body = trigger.oracle;
                break;
              default:
                body = null;
                break;
            }
            if (!BeeUtils.isEmpty(body)) {
              table.addTrigger(tableName, SqlTriggerType.CUSTOM,
                  ImmutableMap.of("body", body),
                  EnumUtils.getEnumByName(SqlTriggerTiming.class, trigger.timing),
                  EnumSet.copyOf(events),
                  EnumUtils.getEnumByName(SqlTriggerScope.class, trigger.scope));
            }
          }
        }
        if (table.isEmpty()) {
          logger.warning("Table has no fields defined:", tbl);
          table = null;
        }
      }
    }
    return table;
  }

  private BeeView initView(String moduleName, String viewName, XmlView xmlView) {
    Assert.notEmpty(viewName);
    BeeView view = null;

    if (xmlView != null) {
      if (!BeeUtils.same(xmlView.name, viewName)) {
        logger.warning("View name doesn't match resource name:", xmlView.name, "!=", viewName);
      } else {
        String src = xmlView.source;

        if (!isTable(src)) {
          logger.warning("Unrecognized view source:", xmlView.name, src);
        } else {
          view = new BeeView(moduleName, xmlView, tableCache);

          if (view.isEmpty()) {
            logger.warning("View has no columns defined:", view.getName());
            view = null;
          }
        }
      }
    }
    return view;
  }

  private void makeStructureChanges(IsQuery... queries) {
    Assert.notNull(queries);

    for (IsQuery query : queries) {
      if (qs.updateData(query) < 0) {
        Assert.untouchable();
      }
    }
  }

  private void rebuildTable(BeeTable table) {
    table.setActive(false);
    String tblMain = table.getName();
    Map<String, String> rebuilds = createTable(table, null);

    if (rebuilds.containsKey(tblMain)) {
      Collection<BeeIndex> indexes = new ArrayList<>();

      for (BeeIndex index : table.getIndexes()) {
        if (BeeUtils.same(index.getTable(), tblMain)) {
          indexes.add(index);
        }
      }
      Collection<BeeUniqueKey> uniqueKeys = new ArrayList<>();

      for (BeeUniqueKey key : table.getUniqueKeys()) {
        if (BeeUtils.same(key.getTable(), tblMain)) {
          uniqueKeys.add(key);
        }
      }
      Collection<BeeForeignKey> foreignKeys = new ArrayList<>();

      for (BeeForeignKey fKey : table.getForeignKeys()) {
        String refTable = fKey.getRefTable();

        if ((BeeUtils.same(refTable, tblMain) && !rebuilds.containsKey(fKey.getTable()))
            || (BeeUtils.same(fKey.getTable(), tblMain)
            && (BeeUtils.same(refTable, tblMain) || getTable(refTable).isActive()))) {

          foreignKeys.add(fKey);
        }
      }
      for (BeeTable other : getTables()) {
        if (!BeeUtils.same(other.getName(), tblMain) && other.isActive()) {
          for (BeeForeignKey fKey : other.getForeignKeys()) {
            if (BeeUtils.same(fKey.getRefTable(), tblMain)) {
              foreignKeys.add(fKey);
            }
          }
        }
      }
      Collection<BeeCheck> checks = new ArrayList<>();

      for (BeeCheck check : table.getChecks()) {
        if (BeeUtils.same(check.getTable(), tblMain)) {
          checks.add(check);
        }
      }
      String tblBackup = rebuilds.get(tblMain);

      if (!BeeUtils.isEmpty(tblBackup)) {
        for (SimpleRow fKeys : qs
            .dbForeignKeys(getDbName(), getDbSchema(), null, tblMain)) {
          String fk = fKeys.getValue(SqlConstants.KEY_NAME);
          String tbl = fKeys.getValue(SqlConstants.TBL_NAME);
          makeStructureChanges(SqlUtils.dropForeignKey(tbl, fk));
        }
        makeStructureChanges(SqlUtils.dropTable(tblMain));
        makeStructureChanges(SqlUtils.renameTable(tblBackup, tblMain));
      }
      createIndexes(indexes);
      createUniqueKeys(uniqueKeys);
      createChecks(checks);
      createForeignKeys(foreignKeys);
    }

    for (String tbl : rebuilds.keySet()) {
      if (!BeeUtils.same(tbl, tblMain)) {
        Collection<BeeIndex> indexes = new ArrayList<>();

        for (BeeIndex index : table.getIndexes()) {
          if (BeeUtils.same(index.getTable(), tbl)) {
            indexes.add(index);
          }
        }
        Collection<BeeUniqueKey> uniqueKeys = new ArrayList<>();

        for (BeeUniqueKey key : table.getUniqueKeys()) {
          if (BeeUtils.same(key.getTable(), tbl)) {
            uniqueKeys.add(key);
          }
        }
        Collection<BeeForeignKey> foreignKeys = new ArrayList<>();

        for (BeeForeignKey fKey : table.getForeignKeys()) {
          String refTable = fKey.getRefTable();

          if (BeeUtils.same(fKey.getTable(), tbl)
              && (BeeUtils.same(refTable, tblMain) || getTable(refTable).isActive())) {

            foreignKeys.add(fKey);
          }
        }
        Collection<BeeCheck> checks = new ArrayList<>();

        for (BeeCheck check : table.getChecks()) {
          if (BeeUtils.same(check.getTable(), tbl)) {
            checks.add(check);
          }
        }
        String tblBackup = rebuilds.get(tbl);

        if (!BeeUtils.isEmpty(tblBackup)) {
          makeStructureChanges(SqlUtils.dropTable(tbl), SqlUtils.renameTable(tblBackup, tbl));
        }
        createIndexes(indexes);
        createUniqueKeys(uniqueKeys);
        createChecks(checks);
        createForeignKeys(foreignKeys);
      }
    }
    createAuditTables(table);
    createTriggers(table.getTriggers());

    table.setActive(true);
  }

  private void updateVehicleModels() {
    if (!getTable(TBL_VEHICLE_BRANDS).isActive() && getTable(TBL_VEHICLE_MODELS).isActive()
        && Objects.nonNull(qs.dbFields(dbName, dbSchema, TBL_VEHICLE_MODELS)
        .getRowByKey(SqlConstants.FLD_NAME, "Parent"))) {

      rebuildTable(TBL_VEHICLE_BRANDS);

      Set<Long> used = new HashSet<>();
      String brandId = getIdName(TBL_VEHICLE_BRANDS);
      String modelId = getIdName(TBL_VEHICLE_MODELS);

      qs.insertData(new SqlInsert(TBL_VEHICLE_BRANDS)
          .addFields(brandId, COL_VEHICLE_BRAND_NAME)
          .setDataSource(new SqlSelect()
              .addFields(TBL_VEHICLE_MODELS, modelId, COL_VEHICLE_MODEL_NAME)
              .addFrom(TBL_VEHICLE_MODELS)
              .setWhere(SqlUtils.in(TBL_VEHICLE_MODELS, modelId,
                  new SqlSelect().setDistinctMode(true)
                      .addFields(TBL_VEHICLE_MODELS, "Parent")
                      .addFrom(TBL_VEHICLE_MODELS)
                      .setWhere(SqlUtils.notNull(TBL_VEHICLE_MODELS, "Parent"))))));

      for (SimpleRow fk : qs.dbForeignKeys(getDbName(), getDbSchema(), null, TBL_VEHICLE_MODELS)) {
        String tbl = fk.getValue(SqlConstants.TBL_NAME);
        String name = fk.getValue(SqlConstants.KEY_NAME);

        makeStructureChanges(SqlUtils.dropForeignKey(tbl, name));

        if (!Objects.equals(tbl, TBL_VEHICLE_MODELS) && isTable(tbl)) {
          getTable(tbl).getForeignKeys().stream().filter(key -> name.equals(key.getName()))
              .findAny().ifPresent(key -> used.addAll(Arrays.asList(qs.getLongColumn(new SqlSelect()
              .setDistinctMode(true)
              .addFields(tbl, key.getFields())
              .addFrom(tbl)))));
        }
      }
      used.remove(null);
      String tmp = SqlUtils.uniqueName();

      qs.updateData(new SqlCreate(tmp, false)
          .setDataSource(new SqlSelect()
              .addAllFields(TBL_VEHICLE_MODELS)
              .addField(TBL_VEHICLE_MODELS, "Parent", COL_VEHICLE_BRAND)
              .addFrom(TBL_VEHICLE_MODELS)
              .setWhere(SqlUtils.or(SqlUtils.notNull(TBL_VEHICLE_MODELS, "Parent"),
                  idInList(TBL_VEHICLE_MODELS, used)))));

      if (qs.sqlExists(tmp, SqlUtils.isNull(tmp, COL_VEHICLE_BRAND))) {
        long id = qs.insertData(new SqlInsert(TBL_VEHICLE_BRANDS)
            .addConstant(brandId, BeeUtils.unbox(qs.getLong(new SqlSelect()
                .addMax(TBL_VEHICLE_BRANDS, brandId).addFrom(TBL_VEHICLE_BRANDS))) + 1)
            .addConstant(COL_VEHICLE_BRAND_NAME, "???"));

        qs.updateData(new SqlUpdate(tmp)
            .addConstant(COL_VEHICLE_BRAND, id)
            .setWhere(SqlUtils.isNull(tmp, COL_VEHICLE_BRAND)));
      }
      makeStructureChanges(SqlUtils.dropTable(TBL_VEHICLE_MODELS));
      makeStructureChanges(SqlUtils.renameTable(tmp, TBL_VEHICLE_MODELS));
      rebuildTable(TBL_VEHICLE_MODELS);
    }
  }
}
