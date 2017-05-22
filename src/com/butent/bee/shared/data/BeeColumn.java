package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.Defaults.DefaultExpression;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@code TableColumn} class, handles core column object's requirements like serialization,
 * id and parameters management.
 */

public class BeeColumn extends TableColumn implements BeeSerializable, HasExtendedInfo {

  /**
   * Contains a list of parameters for column serialization.
   */

  private enum Serial {
    ID, LABEL, VALUE_TYPE, PRECISION, SCALE, ISNULL, READ_ONLY, EDITABLE, LEVEL, DEFAULTS, ENUM_KEY
  }

  public static BeeColumn forRowId(String id) {
    return new BeeColumn(DataUtils.ID_TYPE, id);
  }

  public static BeeColumn forRowVersion(String id) {
    return new BeeColumn(DataUtils.VERSION_TYPE, id);
  }

  public static BeeColumn restore(String s) {
    BeeColumn c = new BeeColumn();
    c.deserialize(s);
    return c;
  }

  private boolean nullable;
  private boolean readOnly;
  private boolean editable = true;

  private int level;
  private Pair<DefaultExpression, Object> defaults;

  public BeeColumn() {
    super(ValueType.TEXT);
  }

  public BeeColumn(String id) {
    this(ValueType.TEXT, id, id);
  }

  public BeeColumn(ValueType type, String id) {
    this(type, id, id);
  }

  public BeeColumn(ValueType type, String id, boolean nillable) {
    this(type, id);
    setNullable(nillable);
  }

  public BeeColumn(ValueType type, String id, int precision, int scale) {
    this(type, id);
    setPrecision(precision);
    setScale(scale);
  }

  public BeeColumn(ValueType type, String label, String id) {
    super(type, label, id);
  }

  @Override
  public BeeColumn copy() {
    BeeColumn result = new BeeColumn(getType(), getLabel(), getId());

    result.setPattern(getPattern());
    if (getProperties() != null) {
      result.setProperties(getProperties().copy());
    }

    result.setPrecision(getPrecision());
    result.setScale(getScale());
    result.setNullable(isNullable());
    result.setReadOnly(isReadOnly());
    result.setEditable(isEditable());

    return result;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case ID:
          setId(value);
          break;
        case LABEL:
          setLabel(value);
          break;
        case VALUE_TYPE:
          setType(ValueType.getByTypeCode(value));
          break;
        case PRECISION:
          setPrecision(BeeUtils.toInt(value));
          break;
        case SCALE:
          setScale(BeeUtils.toInt(value));
          break;
        case ISNULL:
          setNullable(Codec.unpack(value));
          break;
        case READ_ONLY:
          setReadOnly(Codec.unpack(value));
          break;
        case EDITABLE:
          setEditable(Codec.unpack(value));
          break;
        case LEVEL:
          setLevel(BeeUtils.toInt(value));
          break;
        case DEFAULTS:
          String[] def = Codec.beeDeserializeCollection(value);

          if (ArrayUtils.length(def) == 2) {
            setDefaults(Pair.of(EnumUtils.getEnumByName(DefaultExpression.class, def[0]),
                (Object) def[1]));
          }
          break;
        case ENUM_KEY:
          setEnumKey(value);
          break;
      }
    }
  }

  public Pair<DefaultExpression, Object> getDefaults() {
    return defaults;
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<Property> lst = getInfo();

    PropertyUtils.addProperties(lst, "Pattern", getPattern());

    if (getProperties() != null) {
      lst.addAll(getProperties().getInfo());
    }

    List<ExtendedProperty> result = new ArrayList<>();
    PropertyUtils.appendChildrenToExtended(result, getId(), lst);

    return result;
  }

  @Override
  public List<Property> getInfo() {
    return PropertyUtils.createProperties("Id", getId(),
        "Label", getLabel(),
        "Type", getType(),
        "Precision", getPrecision(),
        "Scale", getScale(),
        "Nullable", isNullable(),
        "Read Only", isReadOnly(),
        "Editable", isEditable(),
        "Level", getLevel(),
        "Defaults", getDefaults(),
        "Enum Key", getEnumKey());
  }

  public int getLevel() {
    return level;
  }

  public boolean hasDefaults() {
    if (getDefaults() == null) {
      return false;
    } else {
      return getDefaults().getA() != null || getDefaults().getB() != null;
    }
  }

  public boolean isEditable() {
    return editable;
  }

  public boolean isForeign() {
    return getLevel() > 0;
  }

  public boolean isInsertable(String value) {
    return isEditable()
        && (!BeeUtils.isEmpty(value) || getType() == ValueType.BOOLEAN && hasDefaults());
  }

  public boolean isNullable() {
    return nullable;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ID:
          arr[i++] = getId();
          break;
        case LABEL:
          arr[i++] = getLabel();
          break;
        case VALUE_TYPE:
          arr[i++] = (getType() == null) ? null : getType().getTypeCode();
          break;
        case PRECISION:
          arr[i++] = getPrecision();
          break;
        case SCALE:
          arr[i++] = getScale();
          break;
        case ISNULL:
          arr[i++] = Codec.pack(isNullable());
          break;
        case READ_ONLY:
          arr[i++] = Codec.pack(isReadOnly());
          break;
        case EDITABLE:
          arr[i++] = Codec.pack(isEditable());
          break;
        case LEVEL:
          arr[i++] = getLevel();
          break;
        case DEFAULTS:
          arr[i++] = (getDefaults() == null) ? null
              : new Object[] {getDefaults().getA(), getDefaults().getB()};
          break;
        case ENUM_KEY:
          arr[i++] = getEnumKey();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setDefaults(Pair<DefaultExpression, Object> defaults) {
    this.defaults = defaults;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  @Override
  public String toString() {
    return getInfo().toString();
  }
}
