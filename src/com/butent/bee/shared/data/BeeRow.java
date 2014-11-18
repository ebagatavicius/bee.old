package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    ID, VERSION, EDITABLE, REMOVABLE, VALUES, SHADOW, PROPERTIES
  }

  private static final String PROPERTY_CHILDREN = "_row_children";

  public static BeeRow restore(String s) {
    BeeRow row = new BeeRow(0, 0);
    row.deserialize(s);
    return row;
  }

  public BeeRow(long id, long version) {
    this(id, BeeConst.EMPTY_STRING_ARRAY);
    setVersion(version);
  }

  public BeeRow(long id, long version, List<String> values) {
    super(id, values);
    setVersion(version);
  }

  public BeeRow(long id, long version, String[] row) {
    super(id, row);
    setVersion(version);
  }

  public BeeRow(long id, String[] row) {
    super(id, row);
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
          setId(BeeUtils.toLong(value));
          break;

        case VERSION:
          setVersion(BeeUtils.toLong(value));
          break;

        case EDITABLE:
          setEditable(Codec.unpack(value));
          break;

        case REMOVABLE:
          setRemovable(Codec.unpack(value));
          break;

        case VALUES:
          setValues(Codec.beeDeserializeCollection(value));
          break;

        case SHADOW:
          if (!BeeUtils.isEmpty(value)) {
            String[] shArr = Codec.beeDeserializeCollection(value);

            if (!ArrayUtils.isEmpty(shArr)) {
              Map<Integer, String> shMap = new HashMap<>(shArr.length / 2);

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

  public Collection<RowChildren> getChildren() {
    String serialized = getProperty(PROPERTY_CHILDREN);
    if (BeeUtils.isEmpty(serialized)) {
      return Collections.emptyList();
    }

    Collection<RowChildren> children = new ArrayList<>();

    String[] arr = Codec.beeDeserializeCollection(serialized);
    if (!ArrayUtils.isEmpty(arr)) {
      for (String s : arr) {
        children.add(RowChildren.restore(s));
      }
    }

    return children;
  }

  public boolean hasChildren() {
    return !BeeUtils.isEmpty(getProperty(PROPERTY_CHILDREN));
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

        case VERSION:
          arr[i++] = getVersion();
          break;

        case EDITABLE:
          arr[i++] = Codec.pack(isEditable());
          break;

        case REMOVABLE:
          arr[i++] = Codec.pack(isRemovable());
          break;

        case VALUES:
          arr[i++] = getValues();
          break;

        case SHADOW:
          arr[i++] = getShadow();
          break;

        case PROPERTIES:
          arr[i++] = getProperties();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setChildren(Collection<RowChildren> children) {
    if (BeeUtils.isEmpty(children)) {
      clearProperty(PROPERTY_CHILDREN);
    } else {
      setProperty(PROPERTY_CHILDREN, Codec.beeSerialize(children));
    }
  }
}
