package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.SelectableValue;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class EcCriterion implements BeeSerializable {

  private enum Serial {
    ID, NAME, VALUES
  }

  public static EcCriterion restore(String s) {
    EcCriterion criterion = new EcCriterion();
    criterion.deserialize(s);
    return criterion;
  }

  private long id;
  private String name;

  private final List<SelectableValue> values = new ArrayList<>();

  public EcCriterion(long id, String name) {
    this.id = id;
    this.name = name;
  }

  private EcCriterion() {
  }

  public boolean clearSelection() {
    boolean changed = false;

    for (SelectableValue selectableValue : values) {
      changed |= selectableValue.isSelected();
      selectableValue.setSelected(false);
    }

    return changed;
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

        case NAME:
          setName(value);
          break;

        case VALUES:
          getValues().clear();

          String[] vs = Codec.beeDeserializeCollection(value);
          if (vs != null) {
            for (String v : vs) {
              getValues().add(SelectableValue.restore(v));
            }
          }
          break;
      }
    }
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getSize() {
    return getValues().size();
  }

  public List<SelectableValue> getValues() {
    return values;
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

        case NAME:
          arr[i++] = getName();
          break;

        case VALUES:
          arr[i++] = getValues();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }
}
