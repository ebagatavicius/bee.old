package com.butent.bee.server.modules.administration;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.data.BeeTable.BeeIndex;
import com.butent.bee.server.data.BeeTable.BeeUniqueKey;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.imports.ImportProperty;
import com.butent.bee.shared.imports.ImportType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

import java.io.File;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import lt.locator.Report;
import lt.locator.ReportProviderInterface;
import lt.locator.ReportSummarizedPeriod;
import lt.locator.TripSumRepData;

@Stateless
@LocalBean
public class ImportBean {

  private static class ImportObject {
    private final int prpValue = 0;
    private final int prpRelation = 1;
    private final int prpObject = 2;
    private final int prpId = 3;

    private final long objectId;
    private final String viewName;
    private final Map<String, Object[]> props = new HashMap<>();

    public ImportObject(SimpleRowSet rs, long optId, SystemBean sys) {
      Assert.notNull(rs);
      Assert.state(!rs.isEmpty());

      this.objectId = optId;
      this.viewName = rs.getValue(0, COL_IMPORT_DATA);
      ImportType type = EnumUtils.getEnumByIndex(ImportType.class, rs.getInt(0, COL_IMPORT_TYPE));

      for (ImportProperty prop : type.getProperties()) {
        Object[] data = new Object[4];
        SimpleRow row = rs.getRowByKey(COL_IMPORT_PROPERTY, prop.getName());

        if (row != null) {
          data[prpValue] = row.getValue(COL_IMPORT_VALUE);
          data[prpId] = row.getLong(sys.getIdName(TBL_IMPORT_PROPERTIES));
        }
        data[prpObject] = prop;
        props.put(prop.getName(), data);
      }
      if (!BeeUtils.isEmpty(viewName)) {
        BeeView view = sys.getView(viewName);

        for (ViewColumn col : view.getViewColumns()) {
          if (col.isReadOnly() || !BeeUtils.unbox(col.getEditable())
              && BeeUtils.isPositive(col.getLevel())) {
            continue;
          }
          String name = col.getName();
          Object[] data = new Object[4];

          SimpleRow row = rs.getRowByKey(COL_IMPORT_PROPERTY, name);

          if (row != null) {
            data[prpValue] = row.getValue(COL_IMPORT_VALUE);
            data[prpId] = row.getLong(sys.getIdName(TBL_IMPORT_PROPERTIES));

          } else if (view.isColNullable(name)) {
            continue;
          }
          ImportProperty prop = new ImportProperty(name,
              Localized.maybeTranslate(view.getColumnLabel(name)), true);

          if (!BeeUtils.isEmpty(col.getRelation())) {
            prop.setRelation(col.getRelation());
          }
          data[prpObject] = prop;
          props.put(name, data);
        }
      }
    }

    public Map<Long, String> getDeepNames(String parentName) {
      Map<Long, String> propNames = new LinkedHashMap<>();

      for (ImportProperty prop : getProperties()) {
        if (!prop.isDataProperty()) {
          continue;
        }
        String propName = prop.getName();
        Long propId = getPropertyId(propName);
        ImportObject ro = getPropertyRelation(propName);

        propName = BeeUtils.join("_", parentName, propName);
        propNames.put(propId, propName);

        if (ro != null) {
          propNames.putAll(ro.getDeepNames(propName));
        }
      }
      return propNames;
    }

    public Map<String, Pair<ImportProperty, String>> getDeepProperties(String parentName) {
      Map<String, Pair<ImportProperty, String>> propNames = new LinkedHashMap<>();

      for (ImportProperty prop : getProperties()) {
        if (!prop.isDataProperty()) {
          continue;
        }
        String propName = prop.getName();
        String propValue = getPropertyValue(propName);
        ImportObject ro = getPropertyRelation(propName);

        propName = BeeUtils.join("_", parentName, propName);
        propNames.put(propName, Pair.of(prop, propValue));

        if (ro != null) {
          propNames.putAll(ro.getDeepProperties(propName));
        }
      }
      return propNames;
    }

    public long getObjectId() {
      return objectId;
    }

    public Collection<ImportProperty> getProperties() {
      List<ImportProperty> properties = new ArrayList<>();

      for (Object[] data : props.values()) {
        properties.add((ImportProperty) data[prpObject]);
      }
      return properties;
    }

    public ImportProperty getProperty(String prop) {
      if (props.containsKey(prop)) {
        return (ImportProperty) props.get(prop)[prpObject];
      }
      return null;
    }

    public Long getPropertyId(String prop) {
      if (props.containsKey(prop)) {
        return (Long) props.get(prop)[prpId];
      }
      return null;
    }

    public ImportObject getPropertyRelation(String prop) {
      if (props.containsKey(prop)) {
        return (ImportObject) props.get(prop)[prpRelation];
      }
      return null;
    }

    public String getPropertyValue(String prop) {
      if (props.containsKey(prop) && getPropertyRelation(prop) == null) {
        return (String) props.get(prop)[prpValue];
      }
      return null;
    }

    public String getViewName() {
      return viewName;
    }

    public boolean isPropertyLocked(String prop) {
      return getPropertyRelation(prop) != null && props.get(prop)[prpValue] != null;
    }

    public void setPropertyRelation(String prop, ImportObject io) {
      if (props.containsKey(prop)) {
        props.get(prop)[prpRelation] = io;
      }
    }
  }

  private static final String COL_REC_NO = "_RecNo_";
  private static final String COL_REASON = "_Reason_";

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  FileStorageBean fs;
  @EJB
  UserServiceBean usr;
  @EJB
  DataEditorBean deb;
  @Resource
  EJBContext ctx;

  public ResponseObject doImport(RequestInfo reqInfo) {
    ResponseObject response = null;

    ImportType type = EnumUtils.getEnumByIndex(ImportType.class,
        BeeUtils.toIntOrNull(reqInfo.getParameter(COL_IMPORT_TYPE)));

    if (type != null) {
      switch (type) {
        case COSTS:
          response = importCosts(BeeUtils.toLong(reqInfo.getParameter(COL_IMPORT_OPTION)),
              reqInfo.getParameter(VAR_IMPORT_FILE), reqInfo.getParameter(Service.VAR_PROGRESS));
          break;

        case TRACKING:
          response = importTracking(BeeUtils.toLong(reqInfo.getParameter(COL_IMPORT_OPTION)),
              BeeUtils.toIntOrNull(reqInfo.getParameter(VAR_DATE_LOW)),
              BeeUtils.toIntOrNull(reqInfo.getParameter(VAR_DATE_HIGH)),
              reqInfo.getParameter(Service.VAR_PROGRESS));
          break;

        default:
          response = importData(BeeUtils.toLong(reqInfo.getParameter(COL_IMPORT_OPTION)),
              reqInfo.getParameter(VAR_IMPORT_FILE), reqInfo.getParameter(Service.VAR_PROGRESS));
          break;
      }
      if (response.hasErrors() || BeeUtils.toBoolean(reqInfo.getParameter(VAR_IMPORT_TEST))) {
        ctx.setRollbackOnly();
      }
    } else {
      response = ResponseObject.error("Import type not recognized");
    }
    return response;
  }

  private void applyMappings(ImportObject io, String data, String parentName) {
    String idName = sys.getIdName(TBL_IMPORT_MAPPINGS);
    Table<Long, Long, Map<Long, String>> table = HashBasedTable.create();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_IMPORT_MAPPINGS, idName, COL_IMPORT_MAPPING)
        .addFields(TBL_IMPORT_CONDITIONS, COL_IMPORT_PROPERTY, COL_IMPORT_VALUE)
        .addFrom(TBL_IMPORT_MAPPINGS)
        .addFromInner(io.getViewName(),
            sys.joinTables(io.getViewName(), TBL_IMPORT_MAPPINGS, COL_IMPORT_MAPPING))
        .addFromInner(TBL_IMPORT_CONDITIONS,
            sys.joinTables(TBL_IMPORT_MAPPINGS, TBL_IMPORT_CONDITIONS, COL_IMPORT_MAPPING))
        .setWhere(SqlUtils.equals(TBL_IMPORT_MAPPINGS, COL_IMPORT_OPTION, io.getObjectId()));

    for (SimpleRow row : qs.getData(query)) {
      Long id = row.getLong(idName);
      Long mapping = row.getLong(COL_IMPORT_MAPPING);

      if (!table.contains(id, mapping)) {
        table.put(id, mapping, new HashMap<>());
      }
      table.get(id, mapping).put(row.getLong(COL_IMPORT_PROPERTY), row.getValue(COL_IMPORT_VALUE));
    }
    Map<Long, String> names = io.getDeepNames(parentName);

    for (Long mapping : table.columnKeySet()) {
      HasConditions condition = SqlUtils.or();

      for (Map<Long, String> conds : table.column(mapping).values()) {
        HasConditions clause = SqlUtils.and();

        for (Entry<Long, String> entry : conds.entrySet()) {
          String name = names.get(entry.getKey());

          if (!BeeUtils.isEmpty(name)) {
            IsCondition wh;

            if (entry.getValue() == null) {
              wh = SqlUtils.isNull(data, name);
            } else {
              wh = SqlUtils.equals(data, name, entry.getValue());
            }
            clause.add(wh);
          }
        }
        condition.add(clause);
      }
      qs.updateData(new SqlUpdate(data)
          .addConstant(parentName, mapping)
          .setWhere(SqlUtils.and(SqlUtils.isNull(data, parentName), condition)));
    }
  }

  private static IsExpression cast(BeeView view, String tmp, String col) {
    IsExpression xpr = SqlUtils.field(tmp, col);
    SqlDataType type = view.getColumnType(col);

    switch (type) {
      case CHAR:
      case STRING:
      case TEXT:
      case BLOB:
        break;

      default:
        xpr = SqlUtils.cast(xpr, type, view.getColumnPrecision(col), view.getColumnScale(col));
        break;
    }
    return xpr;
  }

  private String commitData(ImportObject io, String data, String parentName, String parentCap,
      String progress, Map<String, Pair<Integer, BeeRowSet>> status, boolean readOnly) {

    String idName = SqlUtils.uniqueName();
    HasConditions clause = SqlUtils.or();
    HasConditions updateClause = SqlUtils.and();

    // PREPARE VIEW DATA
    SqlSelect query = new SqlSelect()
        .addMax(data, COL_REC_NO)
        .addEmptyText(COL_REASON)
        .addEmptyLong(idName)
        .addFrom(data)
        .setWhere(clause);

    if (!BeeUtils.isEmpty(parentName)) {
      query.setWhere(SqlUtils.and(SqlUtils.isNull(data, parentName), query.getWhere()));
      updateClause.add(SqlUtils.isNull(data, parentName));
      applyMappings(io, data, parentName);
    }
    BeeView view = sys.getView(io.getViewName());
    Map<String, BeeField> cols = new LinkedHashMap<>();
    String tmp = SqlUtils.temporaryName();
    String progressCap = BeeUtils.join("->", parentCap,
        Localized.maybeTranslate(view.getCaption(), usr.getLocalizableDictionary()));

    for (ImportProperty prop : io.getProperties()) {
      String col = prop.getName();

      if (!prop.isDataProperty()) {
        continue;
      }
      String realCol = BeeUtils.join("_", parentName, col);

      if (BeeUtils.same(view.getSourceName(), TBL_EMAILS)
          && BeeUtils.same(view.getColumnField(col), COL_EMAIL_ADDRESS)) {

        qs.updateData(new SqlUpdate(data)
            .addExpression(realCol,
                SqlUtils.expression("LTRIM(RTRIM(LOWER(", SqlUtils.name(realCol), ")))"))
            .setWhere(SqlUtils.notNull(data, realCol)));
      }
      query.addField(data, realCol, col)
          .addGroup(data, realCol);

      clause.add(SqlUtils.notNull(data, realCol));

      ImportObject ro = io.getPropertyRelation(col);

      if (ro != null) {
        String error = commitData(ro, data, realCol, progressCap, progress, status,
            readOnly || io.isPropertyLocked(col) || !usr.canCreateData(ro.getViewName()));

        if (!BeeUtils.isEmpty(error)) {
          return error;
        }
      }
      updateClause.add(SqlUtils.or(SqlUtils.and(SqlUtils.isNull(tmp, col),
          SqlUtils.isNull(data, realCol)), SqlUtils.and(SqlUtils.notNull(tmp, col),
          SqlUtils.notNull(data, realCol), SqlUtils.join(tmp, col, data, realCol))));

      cols.put(col, sys.getTable(view.getColumnTable(col)).getField(view.getColumnField(col)));
    }
    if (BeeUtils.isEmpty(cols)) {
      return null;
    }
    qs.updateData(new SqlCreate(tmp).setDataSource(query));

    if (qs.sqlCount(tmp, null) == 0) {
      qs.sqlDropTemp(tmp);
      return null;
    }
    // GET UNIQUE KEYS
    List<Set<String>> uniqueKeys = new ArrayList<>();

    for (Entry<String, BeeField> entry : cols.entrySet()) {
      if (entry.getValue().isUnique()) {
        uniqueKeys.add(Sets.newHashSet(entry.getKey()));
      }
    }
    BeeTable table = sys.getTable(view.getSourceName());

    for (int i = 0; i < 2; i++) {
      List<List<String>> unique = new ArrayList<>();

      switch (i) {
        case 0:
          for (BeeUniqueKey key : table.getUniqueKeys()) {
            unique.add(key.getFields());
          }
          break;
        case 1:
          for (BeeIndex index : table.getIndexes()) {
            if (index.isUnique() && !BeeUtils.isEmpty(index.getFields())) {
              unique.add(index.getFields());
            }
          }
          break;
      }
      for (List<String> key : unique) {
        Set<String> keys = new HashSet<>();

        for (String fld : key) {
          for (Entry<String, BeeField> entry : cols.entrySet()) {
            BeeField field = entry.getValue();

            if (BeeUtils.same(field.getName(), fld)
                && BeeUtils.same(field.getStorageTable(), table.getName())) {
              keys.add(entry.getKey());
            }
          }
        }
        if (keys.size() == key.size() && !uniqueKeys.contains(keys)) {
          uniqueKeys.add(keys);
        }
      }
    }
    // FIND MATCHING ROWS FROM DATABASE
    String vw = SqlUtils.temporaryName();
    qs.updateData(new SqlCreate(vw)
        .setDataSource(view.getQuery(usr.getCurrentUserId(), null, null, cols.keySet())));

    SqlUpdate update = new SqlUpdate(tmp)
        .addExpression(idName, SqlUtils.field(vw, view.getSourceIdName()))
        .setFrom(vw, SqlUtils.isNull(tmp, idName));

    for (Set<String> key : uniqueKeys) {
      HasConditions condition = SqlUtils.and();

      for (String col : key) {
        condition.add(SqlUtils.notNull(tmp, col),
            cols.get(col).isNotNull() ? null : SqlUtils.notNull(vw, col),
            SqlUtils.compare(cast(view, tmp, col), Operator.EQ, SqlUtils.field(vw, col)));
      }
      qs.updateData(update.setWhere(condition));
    }
    if (qs.sqlExists(tmp, SqlUtils.isNull(tmp, idName))) {
      clause = SqlUtils.and();

      for (String col : cols.keySet()) {
        IsCondition condition = SqlUtils.and(SqlUtils.notNull(tmp, col),
            SqlUtils.compare(cast(view, tmp, col), Operator.EQ, SqlUtils.field(vw, col)));

        if (cols.get(col).isNotNull()) {
          clause.add(condition);
        } else {
          condition = SqlUtils.and(SqlUtils.notNull(vw, col), condition);
          clause.add(SqlUtils.or(SqlUtils.and(SqlUtils.isNull(tmp, col), SqlUtils.isNull(vw, col)),
              condition));
        }
      }
      qs.updateData(update.setWhere(clause));
    }
    qs.sqlDropTemp(vw);

    // INSERT
    if (!readOnly) {
      String ins = SqlUtils.temporaryName();
      qs.updateData(new SqlCreate(ins)
          .setDataSource(new SqlSelect()
              .addAllFields(tmp)
              .addFrom(tmp)
              .setWhere(SqlUtils.isNull(tmp, idName))));

      // CHECK UNIQUE FIELDS
      for (Set<String> key : uniqueKeys) {
        HasConditions condition = SqlUtils.and();
        Set<String> flds = new HashSet<>();

        for (String col : key) {
          condition.add(SqlUtils.notNull(ins, col));
          flds.add(BeeUtils.join("_", parentName, col));
        }
        SimpleRowSet rs = qs.getData(new SqlSelect()
            .addFields(ins, key.toArray(new String[0]))
            .addFrom(ins)
            .setWhere(condition)
            .addGroup(ins, key.toArray(new String[0]))
            .setHaving(SqlUtils.more(SqlUtils.aggregate(SqlFunction.COUNT, null), 1)));

        if (!rs.isEmpty()) {
          condition = SqlUtils.or();

          for (SimpleRow row : rs) {
            HasConditions wh = SqlUtils.and();

            for (String col : key) {
              wh.add(SqlUtils.equals(ins, col, row.getValue(col)));
            }
            condition.add(wh);
          }
          qs.updateData(new SqlUpdate(tmp)
              .addConstant(COL_REASON, "Duplicate " + BeeUtils.join("+", flds))
              .setFrom(ins, SqlUtils.joinUsing(tmp, ins, COL_REC_NO))
              .setWhere(condition));

          qs.updateData(new SqlDelete(ins)
              .setWhere(condition));
        }
      }
      BeeRowSet rowSet = new BeeRowSet(view.getName(), new ArrayList<>());

      for (Entry<String, BeeField> entry : cols.entrySet()) {
        String col = entry.getKey();
        BeeField field = entry.getValue();

        // CHECK REQUIRED FIELDS
        if (field.isNotNull()) {
          qs.updateData(new SqlUpdate(tmp)
              .addConstant(COL_REASON, "Empty " + BeeUtils.join("_", parentName, col))
              .setFrom(ins, SqlUtils.joinUsing(tmp, ins, COL_REC_NO))
              .setWhere(SqlUtils.isNull(ins, col)));

          qs.updateData(new SqlDelete(ins)
              .setWhere(SqlUtils.isNull(ins, col)));
        }
        rowSet.addColumn(view.getBeeColumn(col));
      }
      SimpleRowSet newRows = qs.getData(new SqlSelect()
          .addAllFields(ins)
          .addFrom(ins));
      int c = 0;
      Pair<Integer, BeeRowSet> pair = null;

      if (!BeeUtils.isEmpty(progress)) {
        Endpoint.updateProgress(progress, progressCap, 0);
      }
      for (SimpleRow row : newRows) {
        if (!BeeUtils.isEmpty(progress)
            && !Endpoint.updateProgress(progress, ++c / (double) newRows.getNumberOfRows())) {

          qs.sqlDropTemp(ins);
          qs.sqlDropTemp(tmp);
          return Localized.getConstants().canceled();
        }
        rowSet.clearRows();
        List<String> values = new ArrayList<>();

        for (String col : cols.keySet()) {
          String value = row.getValue(col);

          if (rowSet.getColumn(col).getType() == ValueType.DATE && !BeeUtils.isEmpty(value)) {
            value = TimeUtils.toDateTimeOrNull(value).getDate().serialize();
          }
          values.add(value);
        }
        rowSet.addRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION, values);
        ResponseObject response = deb.commitRow(rowSet, 0, RowInfo.class,
            new Function<SQLException, ResponseObject>() {
              @Override
              public ResponseObject apply(SQLException ex) {
                return ResponseObject.error(ex);
              }
            });

        if (!response.hasErrors()) {
          qs.updateData(new SqlUpdate(tmp)
              .addConstant(idName, ((RowInfo) response.getResponse()).getId())
              .setWhere(SqlUtils.equals(tmp, COL_REC_NO, row.getLong(COL_REC_NO))));

          if (pair == null) {
            String viewName = view.getName();

            if (!status.containsKey(viewName)) {
              status.put(viewName, Pair.of(0, null));
            }
            pair = status.get(viewName);
          }
          pair.setA(pair.getA() + 1);
        } else {
          throw new BeeRuntimeException(BeeUtils.join("\n",
              Lists.newArrayList(response.getErrors())) + "\n" + rowSet.getRow(0));
        }
      }
      qs.sqlDropTemp(ins);
    }
    // PROCESS UNRECOGNIZED RECORDS
    qs.updateData(new SqlUpdate(tmp)
        .addConstant(COL_REASON, "No rights")
        .setWhere(SqlUtils.and(SqlUtils.isNull(tmp, idName), SqlUtils.isNull(tmp, COL_REASON))));

    int c = qs.updateData(new SqlUpdate(tmp)
        .addConstant(idName, BeeConst.UNDEF)
        .setWhere(SqlUtils.isNull(tmp, idName)));

    if (c > 0) {
      BeeRowSet newRs = (BeeRowSet) qs.doSql(new SqlSelect()
          .setDistinctMode(true)
          .addFields(tmp, COL_REASON)
          .addFields(data, io.getDeepProperties(parentName).keySet().toArray(new String[0]))
          .addFrom(data)
          .addFromInner(tmp, updateClause)
          .setWhere(SqlUtils.equals(tmp, idName, BeeConst.UNDEF))
          .addOrder(tmp, COL_REASON)
          .getQuery());

      String viewName = view.getName();

      if (!status.containsKey(viewName)) {
        status.put(viewName, Pair.of(0, null));
      }
      if (status.get(viewName).getB() != null) {
        status.get(viewName).getB().addRows(newRs.getRows());
      } else {
        status.get(viewName).setB(newRs);
      }
    }
    // UPDATE PARENT RELATION
    if (!BeeUtils.isEmpty(parentName)) {
      qs.updateData(new SqlUpdate(data)
          .addExpression(parentName, SqlUtils.field(tmp, idName))
          .setFrom(tmp, updateClause));

      if (c > 0) {
        qs.updateData(new SqlDelete(data)
            .setWhere(SqlUtils.equals(data, parentName, BeeConst.UNDEF)));
      }
    }
    qs.sqlDropTemp(tmp);
    return null;
  }

  private SqlCreate createStructure(ImportObject io) {
    SqlCreate create = new SqlCreate(SqlUtils.temporaryName())
        .addInteger(COL_REC_NO, true);

    for (Entry<String, Pair<ImportProperty, String>> e : io.getDeepProperties(null).entrySet()) {
      String propName = e.getKey();
      ImportProperty prop = e.getValue().getA();

      if (!BeeUtils.isEmpty(prop.getRelation())) {
        create.addLong(propName, false);
      } else {
        create.addText(propName, false);
      }
    }
    return create;
  }

  private ResponseObject importCosts(Long optId, String fileName, String progress) {
    ImportObject io = initImport(optId);
    final DateFormat dtf;

    if (!BeeUtils.isEmpty(io.getPropertyValue(VAR_IMPORT_DATE_FORMAT))) {
      dtf = new SimpleDateFormat(io.getPropertyValue(VAR_IMPORT_DATE_FORMAT));
    } else {
      dtf = null;
    }
    String prfx = "_";

    SqlCreate create = createStructure(io)
        .addBoolean(prfx, false)
        .addBoolean(COL_FUEL, false)
        .addLong(COL_TRIP, false);

    String tmp = create.getTarget();
    qs.updateData(create);

    String error = loadXLSData(io, fileName, tmp, progress,
        new Function<Map<String, String>, Boolean>() {
          @Override
          public Boolean apply(Map<String, String> values) {
            double qty = BeeUtils.toDouble(values.get(COL_COSTS_QUANTITY));
            double prc = BeeUtils.toDouble(values.get(COL_COSTS_PRICE));

            if (!BeeUtils.isPositive(qty)) {
              return false;
            }
            if (!BeeUtils.isPositive(prc)) {
              prc = BeeUtils.round(BeeUtils.toDouble(values.get(COL_AMOUNT)) / qty, 5);
            }
            if (BeeUtils.isPositive(prc)) {
              values.put(COL_COSTS_PRICE, BeeUtils.toString(prc));
            } else {
              return false;
            }
            String value = values.get(COL_COSTS_DATE);

            if (!BeeUtils.isEmpty(value)) {
              DateTime date;

              if (dtf != null) {
                try {
                  date = new DateTime(dtf.parse(value));
                } catch (ParseException e) {
                  date = null;
                }
              } else {
                date = TimeUtils.parseDateTime(value);
              }
              values.put(COL_COSTS_DATE,
                  date != null ? BeeUtils.toString(date.getDate().getTime()) : null);
            }
            value = values.get(COL_COSTS_EXTERNAL_ID);

            if (BeeUtils.isEmpty(value)) {
              StringBuilder sb = new StringBuilder();

              for (String rel : new String[] {
                  COL_VEHICLE, COL_COSTS_ITEM, COL_COSTS_SUPPLIER,
                  COL_COSTS_DATE, COL_NUMBER, COL_COSTS_NOTE}) {
                ImportObject ro = io.getPropertyRelation(rel);

                if (ro != null) {
                  for (ImportProperty prop : ro.getProperties()) {
                    if (prop.isDataProperty()) {
                      sb.append(values.get(rel + "_" + prop.getName()));
                    }
                  }
                } else {
                  sb.append(values.get(rel));
                }
              }
              values.put(COL_COSTS_EXTERNAL_ID, Codec.md5(sb.toString()));
            }
            return true;
          }
        });

    if (!BeeUtils.isEmpty(error)) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.error(error);
    }
    List<ExtendedProperty> info = new ArrayList<>();
    Map<String, Pair<Integer, BeeRowSet>> status = new LinkedHashMap<>();

    for (ImportProperty prop : io.getProperties()) {
      if (prop.isDataProperty()) {
        ImportObject ro = io.getPropertyRelation(prop.getName());

        if (ro != null) {
          error = commitData(ro, tmp, prop.getName(), null, progress, status,
              io.isPropertyLocked(prop.getName()) || !usr.canCreateData(ro.getViewName()));

          if (!BeeUtils.isEmpty(error)) {
            qs.sqlDropTemp(tmp);
            return ResponseObject.error(error);
          }
        }
      }
    }
    SqlSelect select = new SqlSelect().addAllFields(tmp).addFrom(tmp);

    for (String col : new String[] {COL_COSTS_ITEM, COL_COSTS_CURRENCY}) {
      IsCondition wh = SqlUtils.isNull(tmp, col);
      BeeRowSet rs = (BeeRowSet) qs.doSql(select.setWhere(wh).getQuery());

      if (rs.getNumberOfRows() > 0) {
        qs.updateData(new SqlDelete(tmp).setWhere(wh));
        status.put(Localized.getMessages().dataNotAvailable(col), Pair.of(null, rs));
      }
    }
    qs.updateData(new SqlUpdate(tmp)
        .addExpression(COL_FUEL, SqlUtils.constant(true))
        .setFrom(TBL_FUEL_TYPES,
            SqlUtils.join(tmp, COL_COSTS_ITEM, TBL_FUEL_TYPES, COL_ITEM)));

    for (String tbl : new String[] {TBL_TRIP_COSTS, TBL_TRIP_FUEL_COSTS}) {
      HasConditions wh = SqlUtils.and(BeeUtils.same(tbl, TBL_TRIP_FUEL_COSTS)
              ? SqlUtils.notNull(tmp, COL_FUEL) : SqlUtils.isNull(tmp, COL_FUEL),
          SqlUtils.notNull(tbl, COL_COSTS_EXTERNAL_ID));

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(prfx, SqlUtils.constant(true))
          .setFrom(tbl, SqlUtils.and(wh, SqlUtils.joinUsing(tmp, tbl, COL_COSTS_EXTERNAL_ID))));

      if (!BeeUtils.isEmpty(io.getPropertyValue(COL_NUMBER))) {
        SqlSelect query = new SqlSelect()
            .addEmptyBoolean(prfx)
            .addFields(tbl, sys.getIdName(tbl))
            .addFields(tmp, COL_NUMBER)
            .addFields(TBL_TRIPS, COL_TRIP_NO)
            .addFrom(tmp)
            .addFromInner(tbl, SqlUtils.joinUsing(tmp, tbl, COL_COSTS_EXTERNAL_ID))
            .addFromLeft(TBL_TRIPS, sys.joinTables(TBL_TRIPS, tbl, COL_TRIP))
            .setWhere(wh);

        wh = SqlUtils.and();
        String[] flds = new String[] {COL_COSTS_QUANTITY, COL_COSTS_PRICE, COL_COSTS_VAT};

        for (String fld : flds) {
          query.addExpr(SqlUtils.nvl(SqlUtils.field(tbl, fld), 0), fld)
              .addExpr(SqlUtils.nvl(SqlUtils.cast(SqlUtils.field(tmp, fld),
                  SqlDataType.DECIMAL, 15, 5), 0), prfx + fld);

          wh.add(SqlUtils.equals(SqlUtils.name(fld), SqlUtils.name(prfx + fld)));
        }
        String diff = qs.sqlCreateTemp(query);

        qs.updateData(new SqlUpdate(diff)
            .addExpression(prfx, SqlUtils.constant(true))
            .setWhere(wh));

        qs.updateData(new SqlUpdate(tbl)
            .addExpression(COL_NUMBER, SqlUtils.field(diff, COL_NUMBER))
            .setFrom(diff, SqlUtils.joinUsing(tbl, diff, sys.getIdName(tbl)))
            .setWhere(SqlUtils.notNull(diff, prfx)));

        String key = BeeUtils.joinWords(Localized.getConstants().changedValues(),
            Localized.maybeTranslate(sys.getView(tbl).getCaption()));

        for (String fld : flds) {
          BeeRowSet rs = (BeeRowSet) qs.doSql(new SqlSelect()
              .addFields(diff, COL_TRIP_NO)
              .addConstant(io.getProperty(fld).getCaption(), "Value")
              .addField(diff, fld, "Old")
              .addField(diff, prfx + fld, "New")
              .addFrom(diff)
              .setWhere(SqlUtils.notEqual(SqlUtils.name(fld), SqlUtils.name(prfx + fld)))
              .getQuery());

          if (rs.getNumberOfRows() > 0) {
            if (!status.containsKey(key)) {
              status.put(key, Pair.of(null, rs));
            } else {
              status.get(key).getB().addRows(rs.getRows());
            }
          }
        }
        qs.sqlDropTemp(diff);
      }
    }
    IsExpression dt = SqlUtils.cast(SqlUtils.field(tmp, COL_COSTS_DATE), SqlDataType.DATE, 0, 0);

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(COL_TRIP, SqlUtils.field(TBL_TRIPS, sys.getIdName(TBL_TRIPS)))
        .setFrom(TBL_TRIPS,
            SqlUtils.and(SqlUtils.join(tmp, COL_VEHICLE, TBL_TRIPS, COL_VEHICLE),
                SqlUtils.moreEqual(dt, SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_FROM),
                    SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE))),
                SqlUtils.less(dt, SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_TO),
                    SqlUtils.field(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE), Long.MAX_VALUE)),
                SqlUtils.isNull(tmp, prfx), SqlUtils.notNull(tmp, COL_VEHICLE))));

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(COL_COSTS_NOTE, SqlUtils.field(TBL_VEHICLES, COL_VEHICLE_NUMBER))
        .setFrom(TBL_VEHICLES, sys.joinTables(TBL_VEHICLES, tmp, COL_VEHICLE))
        .setWhere(SqlUtils.isNull(tmp, COL_TRIP)));

    for (String tbl : new String[] {TBL_TRIP_COSTS, TBL_TRIP_FUEL_COSTS}) {
      if (!BeeUtils.isEmpty(progress)) {
        Endpoint.updateProgress(progress,
            Localized.maybeTranslate(sys.getView(tbl).getCaption()), 0);
      }
      SqlSelect query = new SqlSelect()
          .addFields(tmp, COL_COSTS_DATE, COL_COSTS_QUANTITY, COL_COSTS_PRICE,
              COL_TRADE_VAT_PLUS, COL_COSTS_VAT, COL_TRADE_VAT_PERC,
              COL_NUMBER, COL_COSTS_NOTE, COL_TRIP, COL_COSTS_EXTERNAL_ID,
              COL_COSTS_CURRENCY, COL_COSTS_COUNTRY, COL_COSTS_SUPPLIER)
          .addFrom(tmp)
          .setWhere(SqlUtils.isNull(tmp, prfx));

      if (BeeUtils.same(tbl, TBL_TRIP_COSTS)) {
        query.addFields(tmp, COL_COSTS_ITEM)
            .setWhere(SqlUtils.and(query.getWhere(), SqlUtils.isNull(tmp, COL_FUEL)));
      } else {
        query.setWhere(SqlUtils.and(query.getWhere(), SqlUtils.notNull(tmp, COL_FUEL)));
      }
      SimpleRowSet rs = qs.getData(query);

      for (int i = 0; i < rs.getNumberOfRows(); i++) {
        if (!BeeUtils.isEmpty(progress)
            && !Endpoint.updateProgress(progress, i / (double) rs.getNumberOfRows())) {

          qs.sqlDropTemp(tmp);
          return ResponseObject.error(Localized.getConstants().canceled());
        }
        qs.insertData(new SqlInsert(tbl)
            .addFields(rs.getColumnNames())
            .addValues((Object[]) rs.getValues(i)));
      }
      status.put(tbl, Pair.of(rs.getNumberOfRows(), null));
    }
    qs.sqlDropTemp(tmp);
    return ResponseObject.response(status);
  }

  private ResponseObject importData(Long optionId, String fileName, String progress) {
    ImportObject io = initImport(optionId);

    SqlCreate create = createStructure(io);
    String tmp = create.getTarget();
    qs.updateData(create);

    String error = loadXLSData(io, fileName, tmp, progress,
        new Function<Map<String, String>, Boolean>() {
          @Override
          public Boolean apply(Map<String, String> values) {
            BeeView view = sys.getView(io.getViewName());

            for (Entry<String, String> entry : values.entrySet()) {
              if (view.hasColumn(entry.getKey())
                  && view.getColumnType(entry.getKey()) == SqlDataType.DATE
                  && !BeeUtils.isEmpty(entry.getValue())) {

                entry.setValue(BeeUtils.toString(TimeUtils.toDateTimeOrNull(entry.getValue())
                    .getDate().getTime()));
              }
            }
            return true;
          }
        });
    if (!BeeUtils.isEmpty(error)) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.error(error);
    }
    Map<String, Pair<Integer, BeeRowSet>> status = new LinkedHashMap<>();

    error = commitData(io, tmp, null, null, progress, status, !usr.canCreateData(io.getViewName()));

    qs.sqlDropTemp(tmp);

    if (!BeeUtils.isEmpty(error)) {
      return ResponseObject.error(error);
    }
    return ResponseObject.response(status);
  }

  private ResponseObject importTracking(Long optionId, Integer from, Integer to, String progress) {
    ImportObject io = initImport(optionId);

    SimpleRowSet objects = qs.getData(new SqlSelect()
        .addField(TBL_VEHICLES, sys.getIdName(TBL_VEHICLES), COL_VEHICLE)
        .addFields(TBL_VEHICLES, COL_VEHICLE_NUMBER)
        .addFrom(TBL_VEHICLES)
        .setWhere(SqlUtils.notNull(TBL_VEHICLES, COL_VEHICLE_NUMBER)));

    if (DataUtils.isEmpty(objects)) {
      return ResponseObject.error(usr.getLocalizableConstants().noData());
    }
    SqlCreate create = createStructure(io);
    String tmp = create.getTarget();
    qs.updateData(create);

    String country = null;
    ImportObject co = io.getPropertyRelation(COL_COUNTRY);

    if (co != null) {
      Map<Long, String> names = io.getDeepNames(null);
      country = names.get(co.getPropertyId(COL_COUNTRY_CODE));

      if (BeeUtils.isEmpty(country)) {
        country = names.get(co.getPropertyId(COL_COUNTRY_NAME));
      }
    }
    int recNo = 0;
    String login = io.getPropertyValue(VAR_IMPORT_LOGIN);
    String pwd = io.getPropertyValue(VAR_IMPORT_PASSWORD);

    JustDate dateFrom = new JustDate(from);
    JustDate dateTo = new JustDate(to);

    ReportProviderInterface port = new Report().getReportProviderImplPort();
    int c = 0;

    for (SimpleRow row : objects) {
      if (!BeeUtils.isEmpty(progress)
          && !Endpoint.updateProgress(progress, ++c / (double) objects.getNumberOfRows())) {

        qs.sqlDropTemp(tmp);
        return ResponseObject.error(Localized.getConstants().canceled());
      }
      boolean exists = true;

      for (int i = 0; i < TimeUtils.dayDiff(dateFrom, dateTo); i++) {
        DateTime dt = TimeUtils.nextDay(dateFrom, i).getDateTime();
        TimeUtils.addMinute(dt, TimeUtils.MINUTES_PER_DAY - 1);

        ReportSummarizedPeriod report = port.getSummarizedReport(login, pwd,
            TimeUtils.startOfDay(dt).toString(), dt.toString(), row.getValue(COL_VEHICLE_NUMBER),
            null, false, !BeeUtils.isEmpty(country), null, null);

        switch (report.getErrorCode()) {
          case 0:
            break;

          case -6:
            exists = false;
            break;

          default:
            qs.sqlDropTemp(tmp);
            return ResponseObject.error(report.getErrorMessage(),
                BeeUtils.parenthesize(report.getErrorCode()));
        }
        if (!exists) {
          break;
        }
        List<TripSumRepData> data = !BeeUtils.isEmpty(country)
            ? report.getTripSummaryByCountriesGrouped()
            : Collections.singletonList(report.getTripSummary());

        if (!BeeUtils.isEmpty(data)) {
          for (TripSumRepData tripData : data) {
            SqlInsert insert = new SqlInsert(tmp)
                .addConstant(COL_REC_NO, ++recNo)
                .addConstant(COL_VEHICLE, row.getLong(COL_VEHICLE))
                .addConstant(COL_DATE, dt.getDate().getTime());

            if (!BeeUtils.isEmpty(country)) {
              insert.addConstant(country, tripData.getName());
            }
            if (io.getPropertyId("DriveTimeInHours") != null) {
              insert.addConstant("DriveTimeInHours", tripData.getDriveTimeInHours());
            }
            if (io.getPropertyId("StopTimeInHours") != null) {
              insert.addConstant("StopTimeInHours", tripData.getStopTimeInHours());
            }
            if (io.getPropertyId("GpsDistanceInKm") != null) {
              insert.addConstant("GpsDistanceInKm", tripData.getGpsDistanceInKm());
            }
            if (io.getPropertyId("GpsAverageFuelUsage") != null) {
              insert.addConstant("GpsAverageFuelUsage", tripData.getGpsAverageFuelUsage());
            }
            if (io.getPropertyId("CounterFuelUsedInLiters") != null) {
              insert.addConstant("CounterFuelUsedInLiters", tripData.getCounterFuelUsedInLiters());
            }
            if (io.getPropertyId("CanDistanceInKm") != null) {
              insert.addConstant("CanDistanceInKm", tripData.getCanDistanceInKm());
            }
            if (io.getPropertyId("CanFuelUsedInLiters") != null) {
              insert.addConstant("CanFuelUsedInLiters", tripData.getCanFuelUsedInLiters());
            }
            if (io.getPropertyId("CanAverageFuelUsage") != null) {
              insert.addConstant("CanAverageFuelUsage", tripData.getCanAverageFuelUsage());
            }
            if (io.getPropertyId("CanOdometerValueStartInKm") != null) {
              insert.addConstant("CanOdometerValueStartInKm",
                  tripData.getCanOdometerValueStartInKm());
            }
            if (io.getPropertyId("CanOdometerValueEndInKm") != null) {
              insert.addConstant("CanOdometerValueEndInKm", tripData.getCanOdometerValueEndInKm());
            }
            qs.insertData(insert);
          }
        }
      }
    }
    Map<String, Pair<Integer, BeeRowSet>> status = new LinkedHashMap<>();

    String error = commitData(io, tmp, null, null, progress, status,
        !usr.canCreateData(io.getViewName()));

    qs.sqlDropTemp(tmp);

    if (BeeUtils.isEmpty(error)) {
      return ResponseObject.error(error);
    }
    return ResponseObject.response(status);
  }

  private ImportObject initImport(Long optId) {
    Assert.state(DataUtils.isId(optId));

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_IMPORT_OPTIONS, COL_IMPORT_TYPE, COL_IMPORT_DATA)
        .addFields(TBL_IMPORT_PROPERTIES, sys.getIdName(TBL_IMPORT_PROPERTIES),
            COL_IMPORT_PROPERTY, COL_IMPORT_VALUE, COL_IMPORT_RELATION_OPTION)
        .addFrom(TBL_IMPORT_OPTIONS)
        .addFromLeft(TBL_IMPORT_PROPERTIES,
            sys.joinTables(TBL_IMPORT_OPTIONS, TBL_IMPORT_PROPERTIES, COL_IMPORT_OPTION))
        .setWhere(sys.idEquals(TBL_IMPORT_OPTIONS, optId)));

    ImportObject io = new ImportObject(rs, optId, sys);

    for (ImportProperty prop : io.getProperties()) {
      if (!BeeUtils.isEmpty(prop.getRelation())) {
        Long id = BeeUtils.toLongOrNull(rs.getValueByKey(COL_IMPORT_PROPERTY, prop.getName(),
            COL_IMPORT_RELATION_OPTION));

        if (DataUtils.isId(id)) {
          io.setPropertyRelation(prop.getName(), initImport(id));
        }
      }
    }
    return io;
  }

  private String loadXLSData(ImportObject io, String fileName, String target,
      String progress, Function<Map<String, String>, Boolean> rowValidator) {
    File file = new File(fileName);

    if (!file.isFile() || !file.canRead()) {
      return usr.getLocalizableMesssages().fileNotFound(fileName);
    }
    Sheet shit;

    try {
      Workbook wb = WorkbookFactory.create(file);
      wb.setMissingCellPolicy(Row.RETURN_BLANK_AS_NULL);

      String shitName = io.getPropertyValue(VAR_IMPORT_SHEET);
      shit = wb.getSheetAt(BeeUtils.isEmpty(shitName) ? 0 : wb.getSheetIndex(shitName));
    } catch (Exception e) {
      file.delete();
      return e.getMessage();
    }
    Map<String, Integer> indexes = new HashMap<>();
    Map<String, Pair<ImportProperty, String>> props = io.getDeepProperties(null);

    for (Entry<String, Pair<ImportProperty, String>> entry : props.entrySet()) {
      String colValue = entry.getValue().getB();

      if (BeeUtils.isPrefix(colValue, '=')) {
        indexes.put(entry.getKey(), CellReference.convertColStringToIndex(colValue.substring(1)));
      }
    }
    Map<String, String> values = new HashMap<>();
    int recNo = 0;
    int startRow = BeeUtils.max(BeeUtils.toInt(io.getPropertyValue(VAR_IMPORT_START_ROW)) - 1,
        shit.getFirstRowNum());
    int endRow = shit.getLastRowNum();

    for (int i = startRow; i <= endRow; i++) {
      if (!BeeUtils.isEmpty(progress)
          && !Endpoint.updateProgress(progress, recNo / (double) (endRow - startRow + 1))) {
        return Localized.getConstants().canceled();
      }
      recNo++;
      Row row = shit.getRow(i);
      if (row == null) {
        continue;
      }
      values.clear();

      for (Entry<String, Pair<ImportProperty, String>> entry : props.entrySet()) {
        String prop = entry.getKey();
        String value = entry.getValue().getB();
        Integer col = indexes.get(prop);

        if (BeeUtils.isNonNegative(col)) {
          value = null;
          Cell cell = row.getCell(col);

          if (cell != null) {
            switch (cell.getCellType()) {
              case Cell.CELL_TYPE_STRING:
                value = cell.getRichStringCellValue().getString();
                break;
              case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                  Date date = cell.getDateCellValue();

                  if (date != null) {
                    value = BeeUtils.toString(date.getTime());
                  }
                } else {
                  value = BeeUtils.toString(cell.getNumericCellValue());
                }
                break;
              case Cell.CELL_TYPE_BOOLEAN:
                value = BeeUtils.toString(cell.getBooleanCellValue());
                break;
              case Cell.CELL_TYPE_FORMULA:
                value = cell.getCellFormula();
                break;
            }
          }
        }
        values.put(prop, value != null ? value.trim() : value);
      }
      if (rowValidator != null && BeeUtils.isFalse(rowValidator.apply(values))) {
        continue;
      }
      SqlInsert insert = new SqlInsert(target);

      for (String name : values.keySet()) {
        insert.addNotEmpty(name, values.get(name));
      }
      if (!insert.isEmpty()) {
        qs.updateData(insert.addConstant(COL_REC_NO, recNo));
      }
    }
    file.delete();
    return null;
  }
}
