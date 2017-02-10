package com.butent.bee.server.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.HasFrom;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.IsFrom;
import com.butent.bee.server.sql.SqlBuilder;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeObject;
import com.butent.bee.shared.data.Defaults.DefaultExpression;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerEvent;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerScope;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerTiming;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerType;
import com.butent.bee.shared.data.XmlTable;
import com.butent.bee.shared.data.XmlTable.XmlEnum;
import com.butent.bee.shared.data.XmlTable.XmlField;
import com.butent.bee.shared.data.XmlTable.XmlRelation;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Implements database table management - contains parameters for table and it's fields, keys,
 * extensions and methods for operating with the table.
 */

public class BeeTable implements BeeObject, HasExtFields, HasStates, HasTranslations,
    HasExtendedInfo {

  public final class BeeCheck {
    private final String tblName;
    private final String checkName;
    private final String expression;

    private BeeCheck(String tblName, String expression) {
      this.tblName = tblName;
      this.checkName = CHECK_PREFIX + Codec.crc32(tblName + expression);
      this.expression = expression;
    }

    public String getExpression() {
      return expression;
    }

    public String getName() {
      return checkName;
    }

    public BeeTable getOwner() {
      return BeeTable.this;
    }

    public String getTable() {
      return tblName;
    }
  }

  public class BeeField {
    private final String fieldName;
    private final SqlDataType type;
    private final int precision;
    private final int scale;
    private final boolean notNull;
    private final boolean unique;
    private final DefaultExpression defExpr;
    private final Object defValue;
    private final boolean extended;
    private final boolean translatable;
    private final String label;
    private final boolean fieldAuditable;
    private final String enumKey;
    private final String expression;

    protected BeeField(XmlField xmlField, String expression, boolean extended) {
      this.fieldName = xmlField.name;
      this.type = EnumUtils.getEnumByName(SqlDataType.class, xmlField.type);

      Assert.notEmpty(this.fieldName);
      Assert.notNull(this.type);

      this.precision = (xmlField.precision == null) ? BeeConst.UNDEF : xmlField.precision;
      this.scale = (xmlField.scale == null) ? BeeConst.UNDEF : xmlField.scale;
      this.notNull = xmlField.notNull;
      this.unique = xmlField.unique;
      this.label = xmlField.label;
      this.extended = extended;
      this.translatable = xmlField.translatable;
      this.defExpr = xmlField.defExpr;
      this.fieldAuditable = xmlField.audit;
      this.expression = expression;

      String key = (xmlField instanceof XmlEnum) ? ((XmlEnum) xmlField).key : null;

      if (!BeeUtils.isEmpty(key)) {
        if (!EnumUtils.isRegistered(key)) {
          LogUtils.getRootLogger().severe("Table:", getOwner().getName(), "Field:", this.getName(),
              "Enum class not registered:", key);
          key = null;
        }
      }
      this.enumKey = key;

      switch (this.type) {
        case BOOLEAN:
          this.defValue = type.parse(xmlField.defValue);

          IsExpression fld = SqlUtils.name(xmlField.name);
          addCheck(getStorageTable(), SqlUtils.or(SqlUtils.isNull(fld), SqlUtils.equals(fld, 1))
              .getSqlString(SqlBuilderFactory.getBuilder()));
          break;

        case DATE:
          JustDate date = TimeUtils.parseDate(xmlField.defValue);

          if (date != null) {
            this.defValue = date.getDays();
          } else {
            this.defValue = null;
          }
          break;

        case DATETIME:
          DateTime time = TimeUtils.parseDateTime(xmlField.defValue);

          if (time != null) {
            this.defValue = time.getTime();
          } else {
            this.defValue = null;
          }
          break;

        default:
          this.defValue = type.parse(xmlField.defValue);
          break;
      }
      if (isUnique()) {
        addUniqueKey(getStorageTable(), Lists.newArrayList(this.getName()));
      }
    }

    public Pair<DefaultExpression, Object> getDefaults() {
      Pair<DefaultExpression, Object> defaults = null;

      if (defExpr != null || defValue != null) {
        defaults = Pair.of(defExpr, defValue);
      }
      return defaults;
    }

    public String getEnumKey() {
      return enumKey;
    }

    public String getExpression() {
      return expression;
    }

    public String getLabel() {
      return label;
    }

    public String getName() {
      return fieldName;
    }

    public BeeTable getOwner() {
      return BeeTable.this;
    }

    public int getPrecision() {
      return precision;
    }

    public int getScale() {
      return scale;
    }

    public String getStorageTable() {
      return isExtended() ? getExtTable(this) : getOwner().getName();
    }

    public SqlDataType getType() {
      return type;
    }

    public boolean isAuditable() {
      return fieldAuditable;
    }

    public boolean isExtended() {
      return extended;
    }

    public boolean isNotNull() {
      return notNull;
    }

    public boolean isTranslatable() {
      return translatable;
    }

    public boolean isUnique() {
      return unique;
    }
  }

  public final class BeeForeignKey {
    private final String tblName;
    private final String keyName;
    private final List<String> keyFields;
    private final List<String> refFields;
    private final String refTable;
    private final SqlKeyword cascade;

    private BeeForeignKey(String tblName, List<String> fields, String refTable,
        List<String> refFields, SqlKeyword cascade) {

      this.tblName = tblName;
      this.keyFields = fields;
      this.refTable = refTable;
      this.refFields = refFields;
      this.cascade = cascade;
      this.keyName = FOREIGN_KEY_PREFIX + Codec.crc32(tblName + BeeUtils.join("", fields) + refTable
          + BeeUtils.join("", refFields) + cascade);
    }

    public SqlKeyword getCascade() {
      return cascade;
    }

    public List<String> getFields() {
      return keyFields;
    }

    public String getName() {
      return keyName;
    }

    public BeeTable getOwner() {
      return BeeTable.this;
    }

    public List<String> getRefFields() {
      return refFields;
    }

    public String getRefTable() {
      return refTable;
    }

    public String getTable() {
      return tblName;
    }
  }

  public final class BeeIndex {
    private final String tblName;
    private final String indexName;
    private final boolean unique;

    private final List<String> indexFields;
    private final String expression;

    private BeeIndex(String tblName, List<String> fields, boolean unique) {
      this.tblName = tblName;
      this.unique = unique;
      this.indexFields = fields;
      this.expression = null;
      this.indexName = (unique ? UNIQUE_INDEX_PREFIX : INDEX_KEY_PREFIX)
          + Codec.crc32(tblName + BeeUtils.join("", fields));
    }

    private BeeIndex(String tblName, String expression, boolean unique) {
      this.tblName = tblName;
      this.unique = unique;
      this.indexFields = null;
      this.expression = expression;
      this.indexName = (unique ? UNIQUE_INDEX_PREFIX : INDEX_KEY_PREFIX)
          + Codec.crc32(tblName + expression);
    }

    public String getExpression() {
      return expression;
    }

    public List<String> getFields() {
      return indexFields;
    }

    public String getName() {
      return indexName;
    }

    public BeeTable getOwner() {
      return BeeTable.this;
    }

    public String getTable() {
      return tblName;
    }

    public boolean isUnique() {
      return unique;
    }
  }

  public final class BeeRelation extends BeeField {
    private final String relation;
    private final SqlKeyword cascade;
    private final boolean editable;

    private BeeRelation(XmlRelation xmlField, String expression, boolean extended) {
      super(xmlField, expression, extended);

      this.relation = xmlField.relation;
      this.cascade = EnumUtils.getEnumByName(SqlKeyword.class, xmlField.cascade);
      this.editable = xmlField.editable;

      Assert.notEmpty(this.relation);

      addIndex(getStorageTable(), Lists.newArrayList(this.getName()), false);

      addForeignKey(getStorageTable(), Lists.newArrayList(this.getName()), getRelation(), null,
          getCascade());
    }

    public SqlKeyword getCascade() {
      return cascade;
    }

    public String getRelation() {
      return relation;
    }

    public boolean isEditable() {
      return editable;
    }

    @Override
    public boolean isTranslatable() {
      return false;
    }
  }

  public final class BeeTrigger {
    private final String tblName;
    private final SqlTriggerType type;
    private final Map<String, ?> parameters;
    private final SqlTriggerTiming timing;
    private final EnumSet<SqlTriggerEvent> events;
    private final SqlTriggerScope scope;
    private final String triggerName;

    private BeeTrigger(String tblName, SqlTriggerType type, Map<String, ?> parameters,
        SqlTriggerTiming timing, EnumSet<SqlTriggerEvent> events, SqlTriggerScope scope) {

      this.tblName = tblName;
      this.type = type;
      this.parameters = parameters;
      this.timing = timing;
      this.events = events;
      this.scope = scope;

      this.triggerName = TRIGGER_PREFIX + Codec.crc32(BeeUtils.joinWords(tblName, type,
          parameters, timing, events, scope));
    }

    public EnumSet<SqlTriggerEvent> getEvents() {
      return events;
    }

    public String getName() {
      return triggerName;
    }

    public BeeTable getOwner() {
      return BeeTable.this;
    }

    public Map<String, ?> getParameters() {
      return parameters;
    }

    public SqlTriggerScope getScope() {
      return scope;
    }

    public String getTable() {
      return tblName;
    }

    public SqlTriggerTiming getTiming() {
      return timing;
    }

    public SqlTriggerType getType() {
      return type;
    }
  }

  public final class BeeUniqueKey {
    private final String tblName;
    private final String keyName;
    private final List<String> keyFields;
    private final boolean primary;

    private BeeUniqueKey(String tblName, List<String> fields, boolean primary) {
      this.tblName = tblName;
      this.keyName = (primary ? PRIMARY_KEY_PREFIX : UNIQUE_KEY_PREFIX)
          + Codec.crc32(tblName + BeeUtils.join("", fields));
      this.keyFields = fields;
      this.primary = primary;
    }

    private BeeUniqueKey(String tblName, List<String> fields) {
      this(tblName, fields, false);
    }

    public List<String> getFields() {
      return keyFields;
    }

    public String getName() {
      return keyName;
    }

    public BeeTable getOwner() {
      return BeeTable.this;
    }

    public String getTable() {
      return tblName;
    }

    public boolean isPrimary() {
      return primary;
    }
  }

  private class ExtSingleTable implements HasExtFields {

    private final String extIdName = getIdName();
    private final String extVersionName = getVersionName();

    @Override
    public SqlCreate createExtTable(SqlCreate query, BeeField field) {
      Assert.state(hasField(field) && field.isExtended());
      SqlCreate sc = query;

      if (query == null) {
        String tblName = field.getStorageTable();

        sc = new SqlCreate(tblName, false)
            .addLong(extIdName, true)
            .addLong(extVersionName, true);

        addUniqueKey(tblName, Lists.newArrayList(extIdName));
        addForeignKey(tblName, Lists.newArrayList(extIdName), getName(), null, SqlKeyword.DELETE);
      }
      sc.addField(field.getName(), field.getType(), field.getPrecision(), field.getScale(),
          field.isNotNull());
      return sc;
    }

    @Override
    public String getExtTable(BeeField field) {
      Assert.state(field.isExtended());
      return getName() + "_EXT";
    }

    @Override
    public SqlInsert insertExtField(SqlInsert query, long rootId, BeeField field, Object newValue) {
      Assert.state(hasField(field) && field.isExtended());
      SqlInsert si = query;

      if (query == null) {
        si = new SqlInsert(getExtTable(field))
            .addConstant(extVersionName, System.currentTimeMillis())
            .addConstant(extIdName, rootId);
      }
      si.addConstant(field.getName(), newValue);
      return si;
    }

    @Override
    public String joinExtField(HasFrom<?> query, String tblAlias, BeeField field) {
      Assert.state(hasField(field) && field.isExtended());
      String extAlias = null;

      String tblName = getName();
      String alias = BeeUtils.notEmpty(tblAlias, tblName);
      String extTable = field.getStorageTable();

      for (IsFrom from : query.getFrom()) {
        Object src = from.getSource();

        if (src instanceof String && BeeUtils.same((String) src, extTable)) {
          String tmpAlias = BeeUtils.notEmpty(from.getAlias(), extTable);
          SqlBuilder builder = SqlBuilderFactory.getBuilder();

          if (from.getSqlString(builder)
              .endsWith(SqlUtils.join(alias, getIdName(), tmpAlias, extIdName)
                  .getSqlString(builder))) {

            extAlias = tmpAlias;
            break;
          }
        }
      }
      if (BeeUtils.isEmpty(extAlias)) {
        if (BeeUtils.isEmpty(tblAlias)) {
          extAlias = extTable;
        } else {
          extAlias = SqlUtils.uniqueName();
        }
        query.addFromLeft(extTable, extAlias,
            SqlUtils.join(alias, getIdName(), extAlias, extIdName));
      }
      return extAlias;
    }

    @Override
    public SqlUpdate updateExtField(SqlUpdate query, long rootId, BeeField field, Object newValue) {
      Assert.state(hasField(field) && field.isExtended());
      SqlUpdate su = query;

      if (query == null) {
        String tblName = getExtTable(field);

        su = new SqlUpdate(tblName)
            .addConstant(extVersionName, System.currentTimeMillis())
            .setWhere(SqlUtils.equals(tblName, extIdName, rootId));
      }
      su.addConstant(field.getName(), newValue);
      return su;
    }
  }

  private class StateSingleTable implements HasStates {

    private final String stateIdName = getIdName();
    private final String stateVersionName = getVersionName();
    private final int bitCount = Integer.SIZE;
    private final Multimap<RightsState, String> stateFields = TreeMultimap.create();

    @Override
    public boolean activateState(RightsState state, long bit) {
      Assert.state(hasState(state));
      return stateFields.put(state, getStateField(state, bit));
    }

    @Override
    public IsCondition checkState(String stateAlias, RightsState state, long... bits) {
      Assert.state(hasState(state));

      Entry<String, Integer> defEntry = getMasks(state, 0).entrySet().iterator().next();
      String fld = defEntry.getKey();
      Integer mask = defEntry.getValue();
      IsCondition defaultCondition;

      if (mask == null) {
        defaultCondition = state.isChecked() ? SqlUtils.sqlTrue() : SqlUtils.sqlFalse();
      } else {
        if (state.isChecked()) {
          defaultCondition = SqlUtils.or(SqlUtils.isNull(stateAlias, fld),
              SqlUtils.notEqual(SqlUtils.bitAnd(stateAlias, fld, mask), mask));
        } else {
          defaultCondition = SqlUtils.and(SqlUtils.notNull(stateAlias, fld),
              SqlUtils.notEqual(SqlUtils.bitAnd(stateAlias, fld, mask), 0));
        }
      }
      HasConditions wh = SqlUtils.or();
      Map<String, Integer> bitMasks = getMasks(state, bits);

      for (Entry<String, Integer> entry : bitMasks.entrySet()) {
        fld = entry.getKey();
        mask = entry.getValue();

        if (mask == null) {
          wh.add(defaultCondition);
        } else {
          wh.add(SqlUtils.and(SqlUtils.isNull(stateAlias, fld), defaultCondition),
              SqlUtils.and(SqlUtils.notNull(stateAlias, fld),
                  SqlUtils.notEqual(SqlUtils.bitAnd(stateAlias, fld, mask),
                      state.isChecked() ? mask : 0)));
        }
      }
      if (wh.isEmpty()) {
        wh.add(defaultCondition);
      }
      return wh;
    }

    @Override
    public SqlCreate createStateTable(SqlCreate query, RightsState state) {
      Assert.state(hasState(state));
      SqlCreate sc = query;

      if (isStateActive(state)) {
        if (sc == null) {
          String tblName = getStateTable(state);

          sc = new SqlCreate(tblName, false)
              .addLong(stateIdName, true)
              .addLong(stateVersionName, true);

          addUniqueKey(tblName, Lists.newArrayList(stateIdName));
          addForeignKey(tblName, Lists.newArrayList(stateIdName), getName(), null,
              SqlKeyword.DELETE);
        }
        for (String col : stateFields.get(state)) {
          sc.addInteger(col, false);
        }
      }
      return sc;
    }

    @Override
    public String getStateTable(RightsState state) {
      Assert.state(hasState(state));
      return getName() + "_STATE";
    }

    @Override
    public void initState(RightsState state, Collection<String> flds) {
      Assert.state(hasState(state));
      Set<String> fldList = new HashSet<>();

      if (flds != null) {
        for (String fld : flds) {
          if (fld.matches(BeeUtils.join("_", state.name(), "[0-9]+", "[0-9]+"))) {
            fldList.add(fld);
          }
        }
      }
      stateFields.replaceValues(state, fldList);
    }

    @Override
    public SqlInsert insertState(long id, RightsState state, long bit, boolean on) {
      Assert.state(hasState(state));

      if (state.isChecked() == on) {
        return null;
      }
      SqlInsert si = new SqlInsert(getStateTable(state))
          .addConstant(stateVersionName, System.currentTimeMillis())
          .addConstant(stateIdName, id);

      for (Entry<String, Integer> entry : getMasks(state, bit).entrySet()) {
        si.addConstant(entry.getKey(), entry.getValue());
      }
      return si;
    }

    @Override
    public String joinState(HasFrom<?> query, String tblAlias, RightsState state) {
      Assert.state(hasState(state));
      String stateAlias = null;

      if (isStateActive(state)) {
        String tblName = getName();
        String alias = BeeUtils.notEmpty(tblAlias, tblName);
        String stateTable = getStateTable(state);

        for (IsFrom from : query.getFrom()) {
          Object src = from.getSource();

          if (src instanceof String && BeeUtils.same((String) src, stateTable)) {
            String tmpAlias = BeeUtils.notEmpty(from.getAlias(), stateTable);
            SqlBuilder builder = SqlBuilderFactory.getBuilder();

            if (from.getSqlString(builder)
                .endsWith(SqlUtils.join(alias, getIdName(), tmpAlias, stateIdName)
                    .getSqlString(builder))) {

              stateAlias = tmpAlias;
              break;
            }
          }
        }
        if (BeeUtils.isEmpty(stateAlias)) {
          if (BeeUtils.same(alias, tblName)) {
            stateAlias = stateTable;
          } else {
            stateAlias = SqlUtils.uniqueName();
          }
          query.addFromLeft(stateTable, stateAlias,
              SqlUtils.join(alias, getIdName(), stateAlias, stateIdName));
        }
      }
      return stateAlias;
    }

    @Override
    public SqlUpdate updateState(long id, RightsState state, long bit, boolean on) {
      Assert.state(hasState(state));
      String tblName = getStateTable(state);

      SqlUpdate su = new SqlUpdate(tblName)
          .addConstant(stateVersionName, System.currentTimeMillis())
          .setWhere(SqlUtils.equals(tblName, stateIdName, id));

      Map<String, Integer> bitMasks = getMasks(state, bit);

      for (String bitFld : bitMasks.keySet()) {
        int mask = bitMasks.get(bitFld);
        IsExpression fld = SqlUtils.nvl(SqlUtils.field(tblName, bitFld), 0);

        if (state.isChecked() == on) {
          su.addExpression(bitFld, SqlUtils.bitAnd(fld, ~mask));
        } else {
          su.addExpression(bitFld, SqlUtils.bitOr(fld, mask));
        }
      }
      return su;
    }

    @Override
    public SqlUpdate updateStateDefaults(long id, RightsState state, boolean on, long... bits) {
      Assert.state(hasState(state));
      String tblName = getStateTable(state);

      SqlUpdate su = new SqlUpdate(tblName)
          .addConstant(stateVersionName, System.currentTimeMillis())
          .setWhere(SqlUtils.equals(tblName, stateIdName, id));

      Map<String, Integer> bitMasks = getMasks(state, bits);

      for (String bitFld : bitMasks.keySet()) {
        Integer mask = bitMasks.get(bitFld);

        if (mask != null) {
          IsExpression fld = SqlUtils.nvl(SqlUtils.field(tblName, bitFld), 0);

          if (state.isChecked() == on) {
            su.addExpression(bitFld, SqlUtils.bitAnd(fld, mask));
          } else {
            su.addExpression(bitFld, SqlUtils.bitOr(fld, ~mask));
          }
        }
      }
      return su;
    }

    @Override
    public SqlSelect verifyState(SqlSelect query, String tblAlias, RightsState state,
        long... bits) {
      Assert.state(hasState(state));
      String stateAlias = joinState(query, tblAlias, state);
      IsCondition wh = null;

      if (!BeeUtils.isEmpty(stateAlias)) {
        wh = checkState(stateAlias, state, bits);

      } else if (!state.isChecked()) {
        wh = SqlUtils.sqlFalse();
      }
      if (wh != null) {
        query.setWhere(SqlUtils.and(query.getWhere(), wh));
      }
      return query;
    }

    private Map<String, Integer> getMasks(RightsState state, long... bits) {
      Map<String, Integer> bitMasks = new HashMap<>();

      if (bits != null) {
        for (long bit : bits) {
          String fld = getStateField(state, bit);

          if (stateFields.containsEntry(state, fld)) {
            long pos = bit % bitCount;
            int mask;

            if (bitMasks.containsKey(fld)) {
              mask = bitMasks.get(fld);
            } else {
              mask = 0;
            }
            bitMasks.put(fld, mask | (1 << pos));
          } else {
            bitMasks.put(fld, null);
          }
        }
      }
      return bitMasks;
    }

    private String getStateField(RightsState state, long bit) {
      long from = ((long) Math.floor(bit / bitCount)) * bitCount;
      return BeeUtils.join("_", state.name(), from, from + bitCount - 1);
    }

    private boolean isStateActive(RightsState state) {
      return stateFields.containsKey(state);
    }
  }

  private class TranslationSingleTable implements HasTranslations {

    private final String translationIdName = getIdName();
    private final String translationVersionName = getVersionName();
    private static final String translationLocaleName = "Locale";

    @Override
    public SqlCreate createTranslationTable(SqlCreate query, BeeField field) {
      Assert.state(hasField(field) && field.isTranslatable());
      SqlCreate sc = query;
      String tblName = getTranslationTable(field);

      if (sc == null) {
        sc = new SqlCreate(tblName, false)
            .addLong(translationIdName, true)
            .addLong(translationVersionName, true)
            .addString(translationLocaleName, 2, true);

        addUniqueKey(tblName, Lists.newArrayList(translationIdName, translationLocaleName));
        addForeignKey(tblName, Lists.newArrayList(translationIdName), getName(), null,
            SqlKeyword.DELETE);
      }
      sc.addField(field.getName(), field.getType(), field.getPrecision(), field.getScale(), false);
      return sc;
    }

    @Override
    public String getTranslationField(BeeField field, String locale) {
      Assert.state(hasField(field) && field.isTranslatable());
      Assert.notEmpty(locale);
      return field.getName();
    }

    @Override
    public String getTranslationTable(BeeField field) {
      Assert.state(hasField(field) && field.isTranslatable());
      return getName() + "_TRAN";
    }

    @Override
    public SqlInsert insertTranslationField(SqlInsert query, long rootId, BeeField field,
        String locale, Object newValue) {
      Assert.state(hasField(field) && field.isTranslatable());
      Assert.notEmpty(locale);
      SqlInsert si = query;

      if (query == null) {
        si = new SqlInsert(getTranslationTable(field))
            .addConstant(translationLocaleName, locale)
            .addConstant(translationVersionName, System.currentTimeMillis())
            .addConstant(translationIdName, rootId);
      }
      si.addConstant(getTranslationField(field, locale), newValue);
      return si;
    }

    @Override
    public String joinTranslationField(HasFrom<?> query, String tblAlias, BeeField field,
        String locale) {
      Assert.state(hasField(field) && field.isTranslatable());
      Assert.notEmpty(locale);
      String tranAlias = null;

      String tblName = getName();
      String alias = BeeUtils.notEmpty(tblAlias, tblName);
      String tranTable = getTranslationTable(field);

      for (IsFrom from : query.getFrom()) {
        Object src = from.getSource();

        if (src instanceof String && BeeUtils.same((String) src, tranTable)) {
          String tmpAlias = BeeUtils.notEmpty(from.getAlias(), tranTable);
          SqlBuilder builder = SqlBuilderFactory.getBuilder();

          if (from.getSqlString(builder)
              .endsWith(SqlUtils.and(
                  SqlUtils.join(alias, getIdName(), tmpAlias, translationIdName),
                  SqlUtils.equals(tmpAlias, translationLocaleName, locale))
                  .getSqlString(builder))) {

            tranAlias = tmpAlias;
            break;
          }
        }
      }
      if (BeeUtils.isEmpty(tranAlias)) {
        if (BeeUtils.isEmpty(tblAlias)) {
          tranAlias = tranTable;
        } else {
          tranAlias = SqlUtils.uniqueName();
        }
        query.addFromLeft(tranTable, tranAlias,
            SqlUtils.and(
                SqlUtils.join(alias, getIdName(), tranAlias, translationIdName),
                SqlUtils.equals(tranAlias, translationLocaleName, locale)));
      }
      return tranAlias;
    }

    @Override
    public SqlUpdate updateTranslationField(SqlUpdate query, long rootId, BeeField field,
        String locale, Object newValue) {
      Assert.state(hasField(field) && field.isTranslatable());
      Assert.notEmpty(locale);
      SqlUpdate su = query;

      if (query == null) {
        String tblName = getTranslationTable(field);

        su = new SqlUpdate(tblName)
            .addConstant(translationVersionName, System.currentTimeMillis())
            .setWhere(SqlUtils.equals(tblName, translationIdName, rootId,
                translationLocaleName, locale));
      }
      su.addConstant(getTranslationField(field, locale), newValue);
      return su;
    }
  }

  static final String UNIQUE_INDEX_PREFIX = "UI_";
  static final String INDEX_KEY_PREFIX = "IK_";
  static final String PRIMARY_KEY_PREFIX = "PK_";
  static final String UNIQUE_KEY_PREFIX = "UK_";
  static final String FOREIGN_KEY_PREFIX = "FK_";
  static final String CHECK_PREFIX = "CK_";
  static final String TRIGGER_PREFIX = "TR_";

  private final String module;
  private final String name;
  private final int idChunk;
  private final String idName;
  private final String versionName;
  private final boolean auditable;
  private final boolean mergeable;
  private final BeeUniqueKey primaryKey;

  private final Map<String, BeeField> fields = new LinkedHashMap<>();
  private final Map<String, BeeForeignKey> foreignKeys = new LinkedHashMap<>();
  private final Map<String, BeeIndex> indexes = new LinkedHashMap<>();
  private final Map<String, BeeUniqueKey> uniqueKeys = new LinkedHashMap<>();
  private final Map<String, BeeCheck> checks = new LinkedHashMap<>();
  private final Map<String, BeeTrigger> triggers = new LinkedHashMap<>();

  private final HasExtFields extSource;
  private final HasStates stateSource;
  private final HasTranslations translationSource;

  private boolean active;

  BeeTable(String moduleName, XmlTable xmlTable, boolean noAudit) {
    Assert.notEmpty(xmlTable.name);
    Assert.notEmpty(xmlTable.idName);
    Assert.notEmpty(xmlTable.versionName);
    Assert.state(!BeeUtils.same(xmlTable.idName, xmlTable.versionName));

    this.module = moduleName;
    this.name = xmlTable.name;
    this.idChunk = xmlTable.idChunk;
    this.idName = xmlTable.idName;
    this.versionName = xmlTable.versionName;
    this.auditable = !noAudit && xmlTable.audit;
    this.mergeable = xmlTable.mergeable;

    this.extSource = new ExtSingleTable();
    this.stateSource = new StateSingleTable();
    this.translationSource = new TranslationSingleTable();

    this.primaryKey = new BeeUniqueKey(getName(), Lists.newArrayList(getIdName()), true);
  }

  @Override
  public boolean activateState(RightsState state, long bit) {
    return stateSource.activateState(state, bit);
  }

  @Override
  public IsCondition checkState(String stateAlias, RightsState state, long... bits) {
    return stateSource.checkState(stateAlias, state, bits);
  }

  @Override
  public SqlCreate createExtTable(SqlCreate query, BeeField field) {
    return extSource.createExtTable(query, field);
  }

  @Override
  public SqlCreate createStateTable(SqlCreate query, RightsState state) {
    return stateSource.createStateTable(query, state);
  }

  @Override
  public SqlCreate createTranslationTable(SqlCreate query, BeeField field) {
    return translationSource.createTranslationTable(query, field);
  }

  public Collection<BeeCheck> getChecks() {
    return ImmutableList.copyOf(checks.values());
  }

  public Map<String, Pair<DefaultExpression, Object>> getDefaults() {
    Map<String, Pair<DefaultExpression, Object>> defaults = null;

    for (BeeField field : getFields()) {
      Pair<DefaultExpression, Object> pair = field.getDefaults();

      if (pair != null) {
        if (defaults == null) {
          defaults = new HashMap<>();
        }
        defaults.put(field.getName(), pair);
      }
    }
    return defaults;
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = new ArrayList<>();
    PropertyUtils.addProperties(info, false, "Module", getModule(), "Name", getName(),
        "Id Chunk", getIdChunk(), "Id Name", getIdName(), "Version Name", getVersionName(),
        "Active", isActive(), "Auditable", isAuditable(), "Mergeable", isMergeable());

    info.add(new ExtendedProperty("Fields", BeeUtils.toString(fields.size())));
    int i = 0;
    for (Map.Entry<String, BeeField> entry : fields.entrySet()) {
      String key = BeeUtils.joinWords("Field", ++i, entry.getKey());
      BeeField field = entry.getValue();

      PropertyUtils.addChildren(info, key, "Name", field.getName(), "Type", field.getType(),
          "Precision", field.getPrecision(), "Scale", field.getScale(),
          "Not Null", field.isNotNull(), "Unique", field.isUnique(),
          "Defaults", field.getDefaults(),
          "Extended", field.isExtended(), "Translatable", field.isTranslatable(),
          "Label", field.getLabel(), "Auditable", isAuditable() && field.isAuditable(),
          "Enum key", field.getEnumKey());

      if (field instanceof BeeRelation) {
        PropertyUtils.addChildren(info,
            "Relation", ((BeeRelation) field).getRelation(),
            "Cascade", ((BeeRelation) field).getCascade());
      }
    }

    List<BeeUniqueKey> keyInfo = Lists.newArrayList(primaryKey);
    keyInfo.addAll(uniqueKeys.values());
    info.add(new ExtendedProperty("Keys", BeeUtils.toString(keyInfo.size())));
    i = 0;
    for (BeeUniqueKey uk : keyInfo) {
      String key = BeeUtils.joinWords("Key", ++i, uk.getName());

      PropertyUtils.addChildren(info, key, "Table", uk.getTable(), "Name", uk.getName(),
          "Type", uk.isPrimary() ? "PRIMARY" : "UNIQUE");
      List<String> keyFields = uk.getFields();
      int cnt = keyFields.size();
      for (int k = 0; k < cnt; k++) {
        info.add(new ExtendedProperty(key, BeeUtils.joinWords("Key Field",
            BeeUtils.progress(k + 1, cnt)), keyFields.get(k)));
      }
    }

    info.add(new ExtendedProperty("Foreign Keys", BeeUtils.toString(foreignKeys.size())));
    i = 0;
    for (Map.Entry<String, BeeForeignKey> entry : foreignKeys.entrySet()) {
      String key = BeeUtils.joinWords("Foreign Key", ++i, entry.getKey());
      BeeForeignKey fk = entry.getValue();

      PropertyUtils.addChildren(info, key, "Table", fk.getTable(), "Name", fk.getName(),
          "Fields", fk.getFields(), "Ref Table", fk.getRefTable(), "Ref Fields", fk.getRefFields(),
          "Cascade", fk.getCascade());
    }

    info.add(new ExtendedProperty("Indexes", BeeUtils.toString(indexes.size())));
    i = 0;
    for (Map.Entry<String, BeeIndex> entry : indexes.entrySet()) {
      String index = BeeUtils.joinWords("Index", ++i, entry.getKey());
      BeeIndex ik = entry.getValue();

      PropertyUtils.addChildren(info, index, "Table", ik.getTable(), "Name", ik.getName(),
          "Unique", ik.isUnique(), "Expression", ik.getExpression());

      List<String> keyFields = ik.getFields();

      if (!BeeUtils.isEmpty(keyFields)) {
        int cnt = keyFields.size();

        for (int k = 0; k < cnt; k++) {
          info.add(new ExtendedProperty(index, BeeUtils.joinWords("Index Field",
              BeeUtils.progress(k + 1, cnt)), keyFields.get(k)));
        }
      }
    }

    info.add(new ExtendedProperty("Checks", BeeUtils.toString(checks.size())));
    i = 0;
    for (Map.Entry<String, BeeCheck> entry : checks.entrySet()) {
      String key = BeeUtils.joinWords("Check", ++i, entry.getKey());
      BeeCheck check = entry.getValue();

      PropertyUtils.addChildren(info, key, "Table", check.getTable(), "Name", check.getName(),
          "Expression", check.getExpression());
    }

    info.add(new ExtendedProperty("Triggers", BeeUtils.toString(triggers.size())));
    i = 0;
    for (Map.Entry<String, BeeTrigger> entry : triggers.entrySet()) {
      String key = BeeUtils.joinWords("Trigger", ++i, entry.getKey());
      BeeTrigger trigger = entry.getValue();

      PropertyUtils.addChildren(info, key, "Table", trigger.getTable(), "Name", trigger.getName(),
          "Timing", trigger.getTiming(), "Event", trigger.getEvents(), "Scope", trigger.getScope(),
          "Content", trigger.getParameters());
    }

    return info;
  }

  @Override
  public String getExtTable(BeeField field) {
    return extSource.getExtTable(field);
  }

  public BeeField getField(String fldName) {
    Assert.state(hasField(fldName), BeeUtils.joinWords("Unknown field name:", getName(), fldName));
    return fields.get(BeeUtils.normalize(fldName));
  }

  public int getFieldCount() {
    return fields.size();
  }

  public Collection<String> getFieldNames() {
    List<String> names = new ArrayList<>();
    for (BeeField field : fields.values()) {
      names.add(field.getName());
    }
    return names;
  }

  public Collection<BeeField> getFields() {
    return ImmutableList.copyOf(fields.values());
  }

  public Collection<BeeForeignKey> getForeignKeys() {
    return ImmutableList.copyOf(foreignKeys.values());
  }

  public int getIdChunk() {
    return idChunk;
  }

  public String getIdName() {
    return idName;
  }

  public Collection<BeeIndex> getIndexes() {
    return ImmutableList.copyOf(indexes.values());
  }

  @Override
  public String getModule() {
    return module;
  }

  @Override
  public String getName() {
    return name;
  }

  public static EnumSet<RightsState> getStates() {
    return EnumSet.of(RightsState.VIEW, RightsState.EDIT, RightsState.DELETE);
  }

  @Override
  public String getStateTable(RightsState state) {
    return stateSource.getStateTable(state);
  }

  @Override
  public String getTranslationField(BeeField field, String locale) {
    return translationSource.getTranslationField(field, locale);
  }

  @Override
  public String getTranslationTable(BeeField field) {
    return translationSource.getTranslationTable(field);
  }

  public Collection<BeeTrigger> getTriggers() {
    return ImmutableList.copyOf(triggers.values());
  }

  public Collection<BeeUniqueKey> getUniqueKeys() {
    List<BeeUniqueKey> keys = Lists.newArrayList(primaryKey);
    keys.addAll(uniqueKeys.values());
    return keys;
  }

  public Set<Set<String>> getUniqueness() {
    Set<Set<String>> result = new HashSet<>();

    fields.values().stream()
        .filter(BeeField::isUnique)
        .map(BeeField::getName)
        .forEach(e -> result.add(Collections.singleton(e)));

    uniqueKeys.values().stream()
        .map(BeeUniqueKey::getFields)
        .forEach(e -> result.add(new HashSet<>(e)));

    indexes.values().stream()
        .filter(e -> e.isUnique() && !BeeUtils.isEmpty(e.getFields()))
        .map(BeeIndex::getFields)
        .forEach(e -> result.add(new HashSet<>(e)));

    return result;
  }

  public String getVersionName() {
    return versionName;
  }

  public boolean hasField(BeeField field) {
    if (field == null || !hasField(field.getName())) {
      return false;
    }
    return getField(field.getName()) == field;
  }

  public boolean hasField(String fldName) {
    return !BeeUtils.isEmpty(fldName) && fields.containsKey(BeeUtils.normalize(fldName));
  }

  public static boolean hasState(RightsState state) {
    return getStates().contains(state);
  }

  @Override
  public void initState(RightsState state, Collection<String> flds) {
    stateSource.initState(state, flds);
  }

  @Override
  public SqlInsert insertExtField(SqlInsert query, long rootId, BeeField field, Object newValue) {
    return extSource.insertExtField(query, rootId, field, newValue);
  }

  @Override
  public SqlInsert insertState(long id, RightsState state, long bit, boolean on) {
    return stateSource.insertState(id, state, bit, on);
  }

  @Override
  public SqlInsert insertTranslationField(SqlInsert query, long rootId, BeeField field,
      String locale, Object newValue) {
    return translationSource.insertTranslationField(query, rootId, field, locale, newValue);
  }

  public boolean isActive() {
    return active;
  }

  public boolean isAuditable() {
    return auditable;
  }

  public boolean isEmpty() {
    return fields.isEmpty();
  }

  public boolean isMergeable() {
    return mergeable;
  }

  @Override
  public String joinExtField(HasFrom<?> query, String tblAlias, BeeField field) {
    return extSource.joinExtField(query, tblAlias, field);
  }

  @Override
  public String joinState(HasFrom<?> query, String tblAlias, RightsState state) {
    return stateSource.joinState(query, tblAlias, state);
  }

  @Override
  public String joinTranslationField(HasFrom<?> query, String tblAlias, BeeField field,
      String locale) {
    return translationSource.joinTranslationField(query, tblAlias, field, locale);
  }

  @Override
  public SqlUpdate updateExtField(SqlUpdate query, long rootId, BeeField field, Object newValue) {
    return extSource.updateExtField(query, rootId, field, newValue);
  }

  @Override
  public SqlUpdate updateState(long id, RightsState state, long bit, boolean on) {
    return stateSource.updateState(id, state, bit, on);
  }

  @Override
  public SqlUpdate updateStateDefaults(long id, RightsState state, boolean on, long... bits) {
    return stateSource.updateStateDefaults(id, state, on, bits);
  }

  @Override
  public SqlUpdate updateTranslationField(SqlUpdate query, long rootId, BeeField field,
      String locale, Object newValue) {
    return translationSource.updateTranslationField(query, rootId, field, locale, newValue);
  }

  @Override
  public SqlSelect verifyState(SqlSelect query, String tblAlias, RightsState state,
      long... bits) {
    return stateSource.verifyState(query, tblAlias, state, bits);
  }

  void addCheck(String tblName, String expression) {
    BeeCheck check = new BeeCheck(tblName, expression);
    checks.put(BeeUtils.normalize(check.getName()), check);
  }

  void addField(XmlField xmlField, String expression, boolean extended) {
    BeeField field = (xmlField instanceof XmlRelation)
        ? new BeeRelation((XmlRelation) xmlField, expression, extended)
        : new BeeField(xmlField, expression, extended);

    String fieldName = field.getName();

    Assert.state(!hasField(fieldName)
            && !BeeUtils.inListSame(fieldName, getIdName(), getVersionName()),
        BeeUtils.joinWords("Duplicate field name:", getName(), fieldName));

    fields.put(BeeUtils.normalize(fieldName), field);
  }

  void addForeignKey(String tblName, List<String> flds, String refTable, List<String> refFields,
      SqlKeyword cascade) {
    BeeForeignKey fKey = new BeeForeignKey(tblName, flds, refTable, refFields, cascade);
    foreignKeys.put(BeeUtils.normalize(fKey.getName()), fKey);
  }

  void addIndex(String tblName, List<String> flds, boolean unique) {
    BeeIndex index = new BeeIndex(tblName, flds, unique);
    indexes.put(BeeUtils.normalize(index.getName()), index);
  }

  void addIndex(String tblName, String expression, boolean unique) {
    BeeIndex index = new BeeIndex(tblName, expression, unique);
    indexes.put(BeeUtils.normalize(index.getName()), index);
  }

  void addTrigger(String tblName, SqlTriggerType type, Map<String, ?> parameters,
      SqlTriggerTiming timing, EnumSet<SqlTriggerEvent> events, SqlTriggerScope scope) {

    BeeTrigger trigger = new BeeTrigger(tblName, type, parameters, timing, events, scope);
    triggers.put(BeeUtils.normalize(trigger.getName()), trigger);
  }

  void addUniqueKey(String tblName, List<String> flds) {
    BeeUniqueKey uniqueKey = new BeeUniqueKey(tblName, flds);
    uniqueKeys.put(BeeUtils.normalize(uniqueKey.getName()), uniqueKey);
  }

  void setActive(boolean active) {
    this.active = active;
  }
}
