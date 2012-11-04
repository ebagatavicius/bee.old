package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.StringArray;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extends {@code StringRow} class, handles core row object's requirements like serialization, id
 * and value management.
 */
public class BeeRow extends StringRow implements BeeSerializable {

  /**
   * Contains a list of parameters for row serialization.
   */
  private enum Serial {
    ID, VERSION, VALUES, SHADOW, PROPERTIES
  }

  public static BeeRow restore(String s) {
    BeeRow row = new BeeRow(0, 0);
    row.deserialize(s);
    return row;
  }
  
  private Map<Integer, String> shadow = null;

  public BeeRow(long id, long version) {
    this(id, BeeConst.EMPTY_STRING_ARRAY);
    setVersion(version);
  }

  public BeeRow(long id, String[] row) {
    super(id, new StringArray(row));
  }

  public BeeRow(long id, long version, String[] row) {
    super(id, new StringArray(row));
    setVersion(version);
  }

  public BeeRow(long id, long version, List<String> data) {
    this(id, version, data.toArray(new String[0]));
  }
  
  @Override
  public BeeRow copy() {
    BeeRow result = new BeeRow(getId(), getVersion(), ArrayUtils.copyOf(getValueArray()));
    copyProperties(result);
    return result;
  }

  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case ID:
          setId(BeeUtils.toLong(value));
          break;

        case VERSION:
          setVersion(BeeUtils.toLong(value));
          break;

        case VALUES:
          String[] vals = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(vals)) {
            setValues(new StringArray(vals));
          }
          break;

        case SHADOW:
          if (!BeeUtils.isEmpty(value)) {
            String[] shArr = Codec.beeDeserializeCollection(value);

            if (!ArrayUtils.isEmpty(shArr)) {
              Map<Integer, String> shMap = new HashMap<Integer, String>(shArr.length / 2);

              for (int j = 0; j < shArr.length; j += 2) {
                shMap.put(BeeUtils.toInt(shArr[j]), shArr[j + 1]);
              }
              setShadow(shMap);
            }
          }
          break;

        case PROPERTIES:
          if (!BeeUtils.isEmpty(value)) {
            setProperties(CustomProperties.restore(value));
          }
          break;
      }
    }
  }

  public String getShadowString(int col) {
    if (shadow != null) {
      return shadow.get(col);
    }
    return null;
  }

  public void preliminaryUpdate(int col, String value) {
    String oldValue = getString(col);

    if (!BeeUtils.equalsTrimRight(value, oldValue)) {
      if (shadow == null) {
        shadow = new HashMap<Integer, String>();
      }
      if (!shadow.containsKey(col)) {
        shadow.put(col, oldValue);

      } else if (BeeUtils.equalsTrimRight(shadow.get(col), value)) {
        shadow.remove(col);

        if (BeeUtils.isEmpty(shadow)) {
          reset();
        }
      }
      super.setValue(col, value);
    }
  }

  public void reset() {
    setShadow(null);
  }

  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ID:
          arr[i++] = getId();
          break;

        case VERSION:
          arr[i++] = getVersion();
          break;

        case VALUES:
          arr[i++] = getValueArray();
          break;

        case SHADOW:
          arr[i++] = shadow;
          break;

        case PROPERTIES:
          arr[i++] = getProperties();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  private void setShadow(Map<Integer, String> shadow) {
    this.shadow = shadow;
  }
}
