package com.butent.bee.server.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.sql.BeeConstants.DataType;
import com.butent.bee.shared.sql.BeeConstants.Keyword;
import com.butent.bee.shared.sql.HasFrom;
import com.butent.bee.shared.sql.IsCondition;
import com.butent.bee.shared.sql.IsFrom;
import com.butent.bee.shared.sql.SqlBuilder;
import com.butent.bee.shared.sql.SqlBuilderFactory;
import com.butent.bee.shared.sql.SqlCreate;
import com.butent.bee.shared.sql.SqlInsert;
import com.butent.bee.shared.sql.SqlSelect;
import com.butent.bee.shared.sql.SqlUpdate;
import com.butent.bee.shared.sql.SqlUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("hiding")
class BeeTable implements HasExtFields, HasStates {

  public class BeeField {
    private boolean custom = false;
    private boolean extended = false;
    private final String name;
    private final DataType type;
    private final int precision;
    private final int scale;
    private final boolean notNull;
    private final boolean unique;
    private final String relation;
    private final boolean cascade;

    private BeeField(String name, DataType type, int precision, int scale,
        boolean notNull, boolean unique, String relation, boolean cascade) {
      Assert.notEmpty(name);
      Assert.notEmpty(type);

      this.name = name;
      this.type = type;
      this.precision = precision;
      this.scale = scale;
      this.notNull = notNull;
      this.unique = unique;
      this.relation = relation;
      this.cascade = cascade;
    }

    public String getName() {
      return name;
    }

    public String getOwner() {
      return BeeTable.this.getName();
    }

    public int getPrecision() {
      return precision;
    }

    public String getRelation() {
      return relation;
    }

    public int getScale() {
      return scale;
    }

    public String getTable() {
      return isExtended() ? getExtTable(getName()) : getOwner();
    }

    public DataType getType() {
      return type;
    }

    public boolean isCascade() {
      return cascade;
    }

    public boolean isCustom() {
      return custom;
    }

    public boolean isExtended() {
      return extended;
    }

    public boolean isNotNull() {
      return notNull;
    }

    public boolean isUnique() {
      return unique;
    }

    BeeField setExtended(boolean extended) {
      this.extended = extended;
      return this;
    }

    private BeeField setCustom() {
      this.custom = true;
      return this;
    }
  }

  public class BeeForeignKey {
    private boolean custom = false;
    private final String tblName;
    private final String name;
    private final String keyField;
    private final String refTable;
    private final Keyword action;

    private BeeForeignKey(String tblName, String keyField, String refTable, Keyword action) {
      Assert.notEmpty(tblName);
      Assert.notEmpty(keyField);
      Assert.notEmpty(refTable);

      this.tblName = tblName;
      this.name = FOREIGN_KEY_PREFIX + Codec.crc32(getTable() + keyField);
      this.keyField = keyField;
      this.refTable = refTable;
      this.action = action;
    }

    public Keyword getAction() {
      return action;
    }

    public String getKeyField() {
      return keyField;
    }

    public String getName() {
      return name;
    }

    public String getOwner() {
      return BeeTable.this.getName();
    }

    public String getRefTable() {
      return refTable;
    }

    public String getTable() {
      return tblName;
    }

    public boolean isCustom() {
      return custom;
    }

    private BeeForeignKey setCustom() {
      this.custom = true;
      return this;
    }
  }

  public class BeeKey {
    private boolean custom = false;
    private final String tblName;
    private final String name;
    private final KeyTypes keyType;
    private final String[] keyFields;

    private BeeKey(KeyTypes keyType, String tblName, String... keyFields) {
      Assert.notEmpty(tblName);
      Assert.notEmpty(keyFields);

      this.tblName = tblName;

      String[] flds = new String[keyFields.length];
      String keyName = getTable();

      for (int i = 0; i < keyFields.length; i++) {
        String fld = keyFields[i];
        Assert.notEmpty(fld);
        flds[i] = fld.trim();
        keyName += flds[i];
      }
      keyName = Codec.crc32(keyName);

      switch (keyType) {
        case PRIMARY:
          keyName = PRIMARY_KEY_PREFIX + keyName;
          break;
        case UNIQUE:
          keyName = UNIQUE_KEY_PREFIX + keyName;
          break;
        case INDEX:
          keyName = INDEX_KEY_PREFIX + keyName;
          break;
        default:
          Assert.untouchable();
          break;
      }
      this.name = keyName;
      this.keyType = keyType;
      this.keyFields = flds;
    }

    public String[] getKeyFields() {
      return keyFields;
    }

    public String getName() {
      return name;
    }

    public String getOwner() {
      return BeeTable.this.getName();
    }

    public String getTable() {
      return tblName;
    }

    public boolean isCustom() {
      return custom;
    }

    public boolean isPrimary() {
      return keyType.equals(KeyTypes.PRIMARY);
    }

    public boolean isUnique() {
      return keyType.equals(KeyTypes.UNIQUE);
    }

    private BeeKey setCustom() {
      this.custom = true;
      return this;
    }
  }

  private class ExtSingleTable implements HasExtFields {

    private final String extIdName = getName() + getIdName();
    private final String extLockName = getName() + getLockName();

    @Override
    public SqlCreate createExtTable(SqlCreate query, BeeField field) {
      SqlCreate sc = null;

      if (BeeUtils.isEmpty(query)) {
        String tblName = field.getTable();

        sc = new SqlCreate(tblName, false)
            .addLong(extIdName, Keyword.NOT_NULL)
            .addLong(extLockName, Keyword.NOT_NULL);

        addKey(true, tblName, extIdName).setCustom();
        addForeignKey(tblName, extIdName, getName(), Keyword.CASCADE).setCustom();
      } else {
        sc = query;
      }
      sc.addField(field.getName(), field.getType(), field.getPrecision(), field.getScale(),
            field.isNotNull() ? Keyword.NOT_NULL : null);

      return sc;
    }

    @Override
    public String getExtTable(String fldName) {
      Assert.state(hasField(fldName));
      return getName() + "_EXT";
    }

    @Override
    public SqlInsert insertExtField(SqlInsert query, long rootId, BeeField field, Object newValue) {
      SqlInsert si = null;

      if (BeeUtils.isEmpty(query)) {
        si = new SqlInsert(field.getTable())
            .addConstant(extLockName, System.currentTimeMillis())
            .addConstant(extIdName, rootId);
      } else {
        si = query;
      }
      si.addConstant(field.getName(), newValue);

      return si;
    }

    @Override
    public String joinExtField(HasFrom<?> query, String tblAlias, BeeField field) {
      String extAlias = null;

      String tblName = getName();
      String alias = BeeUtils.ifString(tblAlias, tblName);
      String extTable = field.getTable();

      for (IsFrom from : query.getFrom()) {
        Object src = from.getSource();

        if (src instanceof String && BeeUtils.same((String) src, extTable)) {
          SqlBuilder builder = SqlBuilderFactory.getBuilder();
          String strFrom = from.getSqlString(builder, false);
          String strAlias = SqlUtils.field(alias, getIdName()).getSqlString(builder, false);

          if (strFrom.contains(strAlias)) {
            extAlias = BeeUtils.ifString(from.getAlias(), extTable);
            break;
          }
        }
      }
      if (BeeUtils.isEmpty(extAlias)) {
        if (BeeUtils.same(alias, tblName)) {
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
      SqlUpdate su = null;

      if (BeeUtils.isEmpty(query)) {
        String tblName = field.getTable();

        su = new SqlUpdate(tblName)
            .addConstant(extLockName, System.currentTimeMillis())
            .setWhere(SqlUtils.equal(tblName, extIdName, rootId));
      } else {
        su = query;
      }
      su.addConstant(field.getName(), newValue);

      return su;
    }
  }

  private enum KeyTypes {
    PRIMARY, UNIQUE, INDEX
  }

  private class StateSingleTable<T extends Number> implements HasStates {

    private final int bitCount;

    public StateSingleTable(int size) {
      Assert.isPositive(size);
      bitCount = size;
    }

    @Override
    public IsCondition checkState(String stateAlias, BeeState state, boolean mdRole, long... bits) {
      IsCondition wh = null;
      Map<Long, Boolean> bitMap = Maps.newHashMap();
      for (long bit : bits) {
        bitMap.put(bit, true);
      }
      Map<String, T> bitMasks = getMasks(getStateField(state), bitMap);

      if (BeeUtils.isEmpty(bitMasks)) {
        wh = SqlUtils.sqlFalse();
      } else {
        for (String fld : bitMasks.keySet()) {
          T mask = bitMasks.get(fld);

          if (state.isChecked()) {
            wh = SqlUtils.and(wh,
                SqlUtils.or(SqlUtils.isNull(stateAlias, fld),
                    SqlUtils.notEqual(SqlUtils.bitAnd(stateAlias, fld, mask), mask)));
          } else {
            wh = SqlUtils.or(wh,
                SqlUtils.and(SqlUtils.isNotNull(stateAlias, fld),
                    SqlUtils.notEqual(SqlUtils.bitAnd(stateAlias, fld, mask), 0)));
          }
        }
      }
      return wh;
    }

    @Override
    public SqlCreate createStateTable(SqlCreate query, BeeState state) {
      SqlCreate sc = null;

      if (BeeUtils.isEmpty(query)) {
        String tblName = getStateTable(state);

        sc = new SqlCreate(tblName, false)
            .addLong(getIdName(), Keyword.NOT_NULL);

        addKey(true, tblName, getIdName()).setCustom();
        addForeignKey(tblName, getIdName(), getName(), Keyword.CASCADE).setCustom();
      } else {
        sc = query;
      }
      Set<String> cols = Sets.newHashSet();
      String stateField = getStateField(state);
      // TODO
      // if (state.supportsUsers()) {
      // for (long user : users) {
      // cols.add(stateField + "Users" + (long) Math.floor((user - 1) / bitCount));
      // }
      // }
      // if (state.supportsRoles()) {
      // for (long role : roles) {
      // cols.add(stateField + "Roles" + (long) Math.floor((role - 1) / bitCount));
      // }
      // }
      for (String col : cols) {
        if (bitCount <= Integer.SIZE) {
          sc.addInt(col);
        } else {
          sc.addLong(col);
        }
      }
      return sc;
    }

    @Override
    public String getStateField(BeeState state) {
      Assert.state(hasState(state));
      return state.getName();
    }

    @Override
    public String getStateTable(BeeState state) {
      Assert.state(hasState(state));
      return getName() + "_STATE";
    }

    @Override
    public SqlInsert insertState(long id, BeeState state, Map<Long, Boolean> bits) {
      Map<String, T> bitMasks = getMasks(getStateField(state), bits);

      String stateTable = getStateTable(state);

      SqlInsert si = new SqlInsert(stateTable)
          .addConstant(getIdName(), id);

      for (String bitFld : bitMasks.keySet()) {
        si.addConstant(bitFld, bitMasks.get(bitFld));
      }
      return si;
    }

    @Override
    public String joinState(HasFrom<?> query, String tblAlias, BeeState state) {
      String stateAlias = null;

      if (true /* TODO isStateActive(state) */) {
        String tblName = getName();
        String alias = BeeUtils.ifString(tblAlias, tblName);
        String stateTable = getStateTable(state);

        for (IsFrom from : query.getFrom()) {
          Object src = from.getSource();

          if (src instanceof String && BeeUtils.same((String) src, stateTable)) {
            SqlBuilder builder = SqlBuilderFactory.getBuilder();
            String strFrom = from.getSqlString(builder, false);
            String strAlias = SqlUtils.field(alias, getIdName()).getSqlString(builder, false);

            if (strFrom.contains(strAlias)) {
              stateAlias = BeeUtils.ifString(from.getAlias(), stateTable);
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
              SqlUtils.joinUsing(alias, stateAlias, getIdName()));
        }
      }
      return stateAlias;
    }

    @Override
    public void setStateActive(BeeState state, boolean active) {
      Assert.state(hasState(state));
      states.put(state, active);
    }

    @Override
    public SqlUpdate updateState(long id, BeeState state, Map<Long, Boolean> bits) {
      Map<String, T> bitMasks = getMasks(getStateField(state), bits);

      String stateTable = getStateTable(state);

      SqlUpdate su = new SqlUpdate(stateTable)
          .setWhere(SqlUtils.equal(stateTable, getIdName(), id));

      for (String bitFld : bitMasks.keySet()) {
        su.addConstant(bitFld, bitMasks.get(bitFld));
      }
      return su;
    }

    @Override
    public void verifyState(SqlSelect query, String tblAlias, BeeState state,
        long user, long... roles) {
      String stateAlias = joinState(query, tblAlias, state);
      IsCondition wh = null;

      if (!BeeUtils.isEmpty(stateAlias)) {
        if (state.supportsUsers()) {
          wh = checkState(stateAlias, state, false, user);
        }
        if (state.supportsRoles()) {
          IsCondition roleWh = checkState(stateAlias, state, true, roles);

          if (BeeUtils.isEmpty(wh)) {
            wh = roleWh;
          } else {
            wh = SqlUtils.or(wh, roleWh);
          }
        }
      } else if (!state.isChecked()) {
        wh = SqlUtils.sqlFalse();
      }
      query.setWhere(SqlUtils.and(query.getWhere(), wh));
    }

    @SuppressWarnings("unchecked")
    private Map<String, T> getMasks(String stateFld, Map<Long, Boolean> bits) {
      Map<String, T> bitMasks = Maps.newHashMap();

      for (long bit : bits.keySet()) {
        if (BeeUtils.isEmpty(bit)) {
          continue;
        }
        long pos = (bit - 1);
        String colName = stateFld + (long) Math.floor(pos / bitCount);
        pos = pos % bitCount;
        Long mask = 0L;

        if (bitMasks.containsKey(colName)) {
          mask = bitMasks.get(colName).longValue();
        }
        if (bits.get(bit)) {
          mask = mask | (1L << pos);
        }
        bitMasks.put(colName, (T) mask);
      }
      return bitMasks;
    }
  }

  private static final String DEFAULT_ID_FIELD = "ID";
  private static final String DEFAULT_LOCK_FIELD = "Version";

  private static final String PRIMARY_KEY_PREFIX = "PK_";
  private static final String UNIQUE_KEY_PREFIX = "UK_";
  private static final String INDEX_KEY_PREFIX = "IK_";
  private static final String FOREIGN_KEY_PREFIX = "FK_";

  private final String name;
  private final String idName;
  private final String lockName;

  private Map<String, BeeField> fields = Maps.newLinkedHashMap();
  private Map<String, BeeForeignKey> foreignKeys = Maps.newLinkedHashMap();
  private Map<String, BeeKey> keys = Maps.newLinkedHashMap();
  private Map<BeeState, Boolean> states = Maps.newLinkedHashMap();

  private final HasExtFields extSource;
  private HasStates stateSource;

  private boolean active = false;
  private boolean custom = false;

  BeeTable(String name, String idName, String lockName) {
    Assert.notEmpty(name);

    this.name = name;
    this.idName = BeeUtils.ifString(idName, DEFAULT_ID_FIELD);
    this.lockName = BeeUtils.ifString(lockName, DEFAULT_LOCK_FIELD);

    this.extSource = new ExtSingleTable();
    this.stateSource = new StateSingleTable<Long>(Long.SIZE);

    BeeKey key = new BeeKey(KeyTypes.PRIMARY, getName(), getIdName());
    keys.put(key.getName(), key);
  }

  @Override
  public IsCondition checkState(String stateAlias, BeeState state, boolean mdRole, long... bits) {
    return stateSource.checkState(stateAlias, state, mdRole, bits);
  }

  @Override
  public SqlCreate createExtTable(SqlCreate query, BeeField field) {
    return extSource.createExtTable(query, field);
  }

  @Override
  public SqlCreate createStateTable(SqlCreate query, BeeState state) {
    return stateSource.createStateTable(query, state);
  }

  @Override
  public String getExtTable(String fldName) {
    return extSource.getExtTable(fldName);
  }

  public BeeField getField(String fldName) {
    Assert.state(hasField(fldName), "Unknown field name: " + fldName);
    return fields.get(fldName);
  }

  public Collection<BeeField> getFields() {
    return ImmutableList.copyOf(fields.values());
  }

  public Collection<BeeForeignKey> getForeignKeys() {
    return ImmutableList.copyOf(foreignKeys.values());
  }

  public String getIdName() {
    return idName;
  }

  public Collection<BeeKey> getKeys() {
    return ImmutableList.copyOf(keys.values());
  }

  public String getLockName() {
    return lockName;
  }

  public Collection<BeeField> getMainFields() {
    Collection<BeeField> flds = Lists.newArrayList();

    for (BeeField field : getFields()) {
      if (field.isUnique()) {
        flds.add(field);
      }
    }
    return flds;
  }

  public String getName() {
    return name;
  }

  public BeeState getState(String stateName) {
    for (BeeState state : getStates()) {
      if (BeeUtils.equals(state.getName(), stateName)) {
        return state;
      }
    }
    Assert.untouchable("Unknown state: " + stateName);
    return null;
  }

  @Override
  public String getStateField(BeeState state) {
    return stateSource.getStateField(state);
  }

  public Collection<BeeState> getStates() {
    return ImmutableList.copyOf(states.keySet());
  }

  @Override
  public String getStateTable(BeeState state) {
    return stateSource.getStateTable(state);
  }

  public boolean hasField(String fldName) {
    return fields.containsKey(fldName);
  }

  public boolean hasFields() {
    return !BeeUtils.isEmpty(getFields());
  }

  public boolean hasState(BeeState state) {
    return getStates().contains(state);
  }

  public boolean hasStates() {
    return !BeeUtils.isEmpty(getStates());
  }

  @Override
  public SqlInsert insertExtField(SqlInsert query, long rootId, BeeField field, Object newValue) {
    return extSource.insertExtField(query, rootId, field, newValue);
  }

  @Override
  public SqlInsert insertState(long id, BeeState state, Map<Long, Boolean> bits) {
    return stateSource.insertState(id, state, bits);
  }

  public boolean isActive() {
    return active;
  }

  public boolean isCustom() {
    return custom;
  }

  public boolean isEmpty() {
    return !hasFields();
  }

  @Override
  public String joinExtField(HasFrom<?> query, String tblAlias, BeeField field) {
    return extSource.joinExtField(query, tblAlias, field);
  }

  @Override
  public String joinState(HasFrom<?> query, String tblAlias, BeeState state) {
    return stateSource.joinState(query, tblAlias, state);
  }

  @Override
  public void setStateActive(BeeState state, boolean active) {
    stateSource.setStateActive(state, active);
  }

  @Override
  public SqlUpdate updateExtField(SqlUpdate query, long rootId, BeeField field, Object newValue) {
    return extSource.updateExtField(query, rootId, field, newValue);
  }

  @Override
  public SqlUpdate updateState(long id, BeeState state, Map<Long, Boolean> bits) {
    return stateSource.updateState(id, state, bits);
  }

  @Override
  public void verifyState(SqlSelect query, String tblAlias, BeeState state,
      long user, long... roles) {
    stateSource.verifyState(query, tblAlias, state, user, roles);
  }

  BeeField addField(String name, DataType type, int precision, int scale,
      boolean notNull, boolean unique, String relation, boolean cascade) {

    BeeField field = new BeeField(name, type, precision, scale, notNull, unique, relation, cascade);
    String fieldName = field.getName();

    Assert.state(!hasField(fieldName), "Dublicate field name: " + getName() + " " + fieldName);
    fields.put(fieldName, field);

    return field;
  }

  BeeForeignKey addForeignKey(String tblName, String keyField, String refTable, Keyword action) {
    BeeForeignKey fKey = new BeeForeignKey(tblName, keyField, refTable, action);
    foreignKeys.put(fKey.getName(), fKey);
    return fKey;
  }

  BeeKey addKey(boolean unique, String tblName, String... keyFields) {
    BeeKey key = new BeeKey(unique ? KeyTypes.UNIQUE : KeyTypes.INDEX, tblName, keyFields);
    keys.put(key.getName(), key);
    return key;
  }

  BeeState addState(BeeState state) {
    Assert.state(!hasState(state), "Dublicate state: " + getName() + " " + state.getName());
    states.put(state, false);
    return state;
  }

  int applyChanges(BeeTable extension) {
    dropCustom();
    int cnt = 0;

    for (BeeField fld : extension.getFields()) {
      addField(fld.getName()
          , fld.getType()
          , fld.getPrecision()
          , fld.getScale()
          , fld.isNotNull()
          , fld.isUnique()
          , fld.getRelation()
          , fld.isCascade())
          .setExtended(fld.isExtended())
          .setCustom();
      cnt++;
    }
    for (BeeForeignKey fKey : extension.getForeignKeys()) {
      addForeignKey(fKey.getTable(), fKey.getKeyField(), fKey.getRefTable(), fKey.getAction())
          .setCustom();
      cnt++;
    }
    for (BeeKey key : extension.getKeys()) {
      if (!key.isPrimary()) {
        addKey(key.isUnique(), key.getTable(), key.getKeyFields())
            .setCustom();
        cnt++;
      }
    }
    for (BeeState state : extension.getStates()) {
      addState(state); // TODO .setCustom();
      cnt++;
    }
    return cnt;
  }

  void setActive(boolean active) {
    this.active = active;
  }

  void setCustom() {
    this.custom = true;
  }

  private void dropCustom() {
    for (BeeField field : Lists.newArrayList(getFields())) {
      if (field.isCustom()) {
        fields.remove(field.getName());
      }
    }
    for (BeeKey key : Lists.newArrayList(getKeys())) {
      if (key.isCustom()) {
        keys.remove(key.getName());
      }
    }
    for (BeeForeignKey fKey : Lists.newArrayList(getForeignKeys())) {
      if (fKey.isCustom()) {
        foreignKeys.remove(fKey.getName());
      }
    }
    for (BeeState state : Lists.newArrayList(getStates())) {
      // TODO if (state.isCustom()) {
      // states.remove(state);
      // }
    }
  }
}
