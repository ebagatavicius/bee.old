package com.butent.bee.shared.modules;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class BeeParameter implements BeeSerializable {

  public static BeeParameter createBoolean(String module, String name) {
    return createBoolean(module, name, false, null);
  }

  public static BeeParameter createBoolean(String module, String name, boolean userMode,
      Boolean defValue) {
    return new BeeParameter(module, name, ParameterType.BOOLEAN, userMode,
        defValue != null ? defValue.toString() : null);
  }

  public static BeeParameter createCollection(String module, String name) {
    return createCollection(module, name, false, null);
  }

  public static BeeParameter createCollection(String module, String name, boolean userMode,
      Collection<String> defValue) {
    return new BeeParameter(module, name, ParameterType.COLLECTION, userMode,
        defValue != null ? Codec.beeSerialize(defValue) : null);
  }

  public static BeeParameter createDate(String module, String name) {
    return createDate(module, name, false, null);
  }

  public static BeeParameter createDate(String module, String name, boolean userMode,
      JustDate defValue) {
    return new BeeParameter(module, name, ParameterType.DATE, userMode,
        defValue != null ? defValue.toString() : null);
  }

  public static BeeParameter createDateTime(String module, String name) {
    return createDateTime(module, name, false, null);
  }

  public static BeeParameter createDateTime(String module, String name, boolean userMode,
      DateTime defValue) {
    return new BeeParameter(module, name, ParameterType.DATETIME, userMode,
        defValue != null ? defValue.toCompactString() : null);
  }

  public static BeeParameter createMap(String module, String name) {
    return createMap(module, name, false, null);
  }

  public static BeeParameter createMap(String module, String name, boolean userMode,
      Map<String, String> defValue) {
    return new BeeParameter(module, name, ParameterType.MAP, userMode,
        defValue != null ? Codec.beeSerialize(defValue) : null);
  }

  public static BeeParameter createNumber(String module, String name) {
    return createNumber(module, name, false, null);
  }

  public static BeeParameter createNumber(String module, String name, boolean userMode,
      Number defValue) {
    return new BeeParameter(module, name, ParameterType.NUMBER, userMode,
        defValue != null ? defValue.toString() : null);
  }

  public static BeeParameter createRelation(String module, String name,
      String relationTable, String relationField) {
    return createRelation(module, name, false, relationTable, relationField);
  }

  public static BeeParameter createRelation(String module, String name, boolean userMode,
      String relationTable, String relationField) {
    Assert.notEmpty(relationTable);
    Assert.notEmpty(relationField);

    BeeParameter param = new BeeParameter(module, name, ParameterType.RELATION, userMode, null);
    param.setOptions(Pair.of(relationTable, relationField).serialize());
    return param;
  }

  public static BeeParameter createSet(String module, String name) {
    return createSet(module, name, false, null);
  }

  public static BeeParameter createSet(String module, String name, boolean userMode,
      Set<String> defValue) {

    BeeParameter param = createCollection(module, name, userMode, defValue);
    param.setOptions(BeeUtils.toString(true));
    return param;
  }

  public static BeeParameter createTime(String module, String name) {
    return createTime(module, name, false, null);
  }

  public static BeeParameter createTime(String module, String name, boolean userMode,
      Long defValue) {
    return new BeeParameter(module, name, ParameterType.TIME, userMode,
        defValue != null ? TimeUtils.renderTime(defValue, false) : null);
  }

  public static BeeParameter createTimeOfDay(String module, String name) {
    return createTimeOfDay(module, name, false, null);
  }

  public static BeeParameter createTimeOfDay(String module, String name, boolean userMode,
      Long defValue) {

    BeeParameter param = createTime(module, name, userMode, defValue);
    param.setOptions(BeeUtils.toString(true));
    return param;
  }

  public static BeeParameter createText(String module, String name) {
    return createText(module, name, false, null);
  }

  public static BeeParameter createText(String module, String name, boolean userMode,
      String defValue) {
    return new BeeParameter(module, name, ParameterType.TEXT, userMode, defValue);
  }

  public static BeeParameter restore(String s) {
    BeeParameter parameter = new BeeParameter();
    parameter.deserialize(s);
    return parameter;
  }

  private String module;
  private String name;
  private ParameterType type;
  private String defValue;
  private final Map<Long, String> userValues = Maps.newHashMapWithExpectedSize(1);
  private final Map<Long, String> displayValues = Maps.newHashMapWithExpectedSize(1);
  private boolean supportsUsers;
  private Long id;
  private String options;

  private enum Serial {
    MODULE, NAME, TYPE, DEF_VALUE, USER_VALUES, DISPLAY_VALUES, SUPPORTS_USERS, OPTIONS;
  }

  private BeeParameter() {
  }

  private BeeParameter(String module, String name, ParameterType type, boolean userMode,
      String defValue) {

    setModule(module);
    setName(name);
    setType(type);
    this.supportsUsers = userMode;
    this.defValue = defValue;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String val = arr[i];

      switch (member) {
        case MODULE:
          setModule(val);
          break;
        case NAME:
          setName(val);
          break;
        case TYPE:
          setType(EnumUtils.getEnumByName(ParameterType.class, val));
          break;
        case DEF_VALUE:
          defValue = val;
          break;
        case USER_VALUES:
          for (Entry<String, String> entry : Codec.deserializeMap(val).entrySet()) {
            userValues.put(BeeUtils.toLongOrNull(entry.getKey()), entry.getValue());
          }
          break;
        case DISPLAY_VALUES:
          for (Entry<String, String> entry : Codec.deserializeMap(val).entrySet()) {
            displayValues.put(BeeUtils.toLongOrNull(entry.getKey()), entry.getValue());
          }
          break;
        case SUPPORTS_USERS:
          supportsUsers = BeeUtils.toBoolean(val);
          break;
        case OPTIONS:
          setOptions(val);
          break;
      }
    }
  }

  public Boolean getBoolean() {
    return (Boolean) getTypedValue(ParameterType.BOOLEAN, getValue());
  }

  public Boolean getBoolean(Long userId) {
    return (Boolean) getTypedValue(ParameterType.BOOLEAN, getValue(userId));
  }

  @SuppressWarnings("unchecked")
  public Collection<String> getCollection() {
    return (Collection<String>) getTypedValue(ParameterType.COLLECTION, getValue());
  }

  @SuppressWarnings("unchecked")
  public Collection<String> getCollection(Long userId) {
    return (Collection<String>) getTypedValue(ParameterType.COLLECTION, getValue(userId));
  }

  public JustDate getDate() {
    return (JustDate) getTypedValue(ParameterType.DATE, getValue());
  }

  public JustDate getDate(Long userId) {
    return (JustDate) getTypedValue(ParameterType.DATE, getValue(userId));
  }

  public DateTime getDateTime() {
    return (DateTime) getTypedValue(ParameterType.DATETIME, getValue());
  }

  public DateTime getDateTime(Long userId) {
    return (DateTime) getTypedValue(ParameterType.DATETIME, getValue(userId));
  }

  public String getDisplayValue() {
    return displayValues.get(null);
  }

  public String getDisplayValue(Long userId) {
    Assert.isTrue(DataUtils.isId(userId));

    if (supportsUsers() && displayValues.containsKey(userId)) {
      return displayValues.get(userId);
    } else {
      return getDisplayValue();
    }
  }

  public Long getId() {
    return id;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getMap() {
    return (Map<String, String>) getTypedValue(ParameterType.MAP, getValue());
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getMap(Long userId) {
    return (Map<String, String>) getTypedValue(ParameterType.MAP, getValue(userId));
  }

  public String getModule() {
    return module;
  }

  public String getName() {
    return name;
  }

  public Number getNumber() {
    return (Number) getTypedValue(ParameterType.NUMBER, getValue());
  }

  public Number getNumber(Long userId) {
    return (Number) getTypedValue(ParameterType.NUMBER, getValue(userId));
  }

  public String getOptions() {
    return options;
  }

  public Long getRelation() {
    return (Long) getTypedValue(ParameterType.RELATION, getValue());
  }

  public Long getRelation(Long userId) {
    return (Long) getTypedValue(ParameterType.RELATION, getValue(userId));
  }

  public String getText() {
    return (String) getTypedValue(ParameterType.TEXT, getValue());
  }

  public String getText(Long userId) {
    return (String) getTypedValue(ParameterType.TEXT, getValue(userId));
  }

  public Long getTime() {
    return (Long) getTypedValue(ParameterType.TIME, getValue());
  }

  public Long getTime(Long userId) {
    return (Long) getTypedValue(ParameterType.TIME, getValue(userId));
  }

  public ParameterType getType() {
    return type;
  }

  public Collection<Long> getUsers() {
    return userValues.keySet();
  }

  public String getValue() {
    if (userValues.containsKey(null)) {
      return userValues.get(null);
    } else {
      return defValue;
    }
  }

  public String getValue(Long userId) {
    Assert.isTrue(DataUtils.isId(userId));

    if (supportsUsers() && userValues.containsKey(userId)) {
      return userValues.get(userId);
    } else {
      return getValue();
    }
  }

  public void reset() {
    userValues.clear();
    displayValues.clear();
    setId(null);
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : Serial.values()) {
      switch (member) {
        case MODULE:
          arr[i++] = getModule();
          break;
        case NAME:
          arr[i++] = getName();
          break;
        case TYPE:
          arr[i++] = getType();
          break;
        case DEF_VALUE:
          arr[i++] = defValue;
          break;
        case USER_VALUES:
          arr[i++] = userValues;
          break;
        case DISPLAY_VALUES:
          arr[i++] = displayValues;
          break;
        case SUPPORTS_USERS:
          arr[i++] = supportsUsers;
          break;
        case OPTIONS:
          arr[i++] = options;
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setDisplayValue(String displayValue) {
    if (displayValue == null) {
      displayValues.remove(null);
    } else {
      displayValues.put(null, displayValue);
    }
  }

  public void setDisplayValue(Long userId, String displayValue) {
    Assert.state(supportsUsers(), "Parameter does not support user values: "
        + BeeUtils.join(".", getModule(), getName()));
    Assert.isTrue(DataUtils.isId(userId));

    if (displayValue == null) {
      displayValues.remove(userId);
    } else {
      displayValues.put(userId, displayValue);
    }
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setValue(String value) {
    if (value == null) {
      userValues.remove(null);
    } else {
      userValues.put(null, value);
    }
    setDisplayValue(null);
  }

  public void setValue(Long userId, String value) {
    Assert.state(supportsUsers(), "Parameter does not support user values: "
        + BeeUtils.join(".", getModule(), getName()));
    Assert.isTrue(DataUtils.isId(userId));

    if (value == null) {
      userValues.remove(userId);
    } else {
      userValues.put(userId, value);
    }
    setDisplayValue(userId, null);
  }

  public boolean supportsUsers() {
    return supportsUsers;
  }

  private Object getTypedValue(ParameterType tp, String expr) {
    Assert.state(getType() == tp,
        BeeUtils.joinWords("Parameter type mismach:", getType(), "!=", tp));
    Object val = null;

    switch (tp) {
      case BOOLEAN:
        val = BeeUtils.toBooleanOrNull(expr);
        break;

      case DATE:
        val = TimeUtils.parseDate(expr);
        break;

      case DATETIME:
        val = TimeUtils.parseDateTime(expr);
        break;

      case COLLECTION:
        String[] entries = Codec.beeDeserializeCollection(expr);

        if (entries != null) {
          val = Lists.newArrayList(entries);
        } else {
          val = new ArrayList<>();
        }
        break;

      case MAP:
        val = Codec.deserializeMap(expr);
        break;

      case NUMBER:
        val = BeeUtils.toDoubleOrNull(expr);
        break;

      case RELATION:
        val = BeeUtils.toLongOrNull(expr);
        break;

      case TEXT:
        val = expr;
        break;

      case TIME:
        val = TimeUtils.parseTime(expr);
        break;
    }
    return val;
  }

  private void setModule(String module) {
    Assert.notEmpty(module);
    this.module = module;
  }

  private void setName(String name) {
    Assert.notEmpty(name);
    this.name = name;
  }

  private void setOptions(String options) {
    this.options = options;
  }

  private void setType(ParameterType type) {
    Assert.notNull(type);
    this.type = type;
  }
}
