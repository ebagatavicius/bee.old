package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class EcBrand implements BeeSerializable {

  private enum Serial {
    ID, NAME, SELECTED
  }

  public static EcBrand restore(String s) {
    EcBrand ecBrand = new EcBrand();
    ecBrand.deserialize(s);
    return ecBrand;
  }

  private long id;
  private String name;

  private boolean selected;

  public EcBrand(long id, String name) {
    this.id = id;
    this.name = name;
  }

  private EcBrand() {
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

        case SELECTED:
          setSelected(Codec.unpack(value));
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

  public boolean isSelected() {
    return selected;
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

        case SELECTED:
          arr[i++] = Codec.pack(isSelected());
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  private void setId(long id) {
    this.id = id;
  }

  private void setName(String name) {
    this.name = name;
  }
}
