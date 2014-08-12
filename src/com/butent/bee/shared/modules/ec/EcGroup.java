package com.butent.bee.shared.modules.ec;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

public class EcGroup implements BeeSerializable {

  private enum Serial {
    ID, NAME, BRAND_SELECTION, CRITERIA
  }

  public static EcGroup restore(String s) {
    EcGroup group = new EcGroup();
    group.deserialize(s);
    return group;
  }

  private long id;
  private String name;

  private boolean brandSelection;

  private final List<Long> criteria = Lists.newArrayList();

  public EcGroup(long id, String name) {
    this.id = id;
    this.name = name;
  }

  private EcGroup() {
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

        case BRAND_SELECTION:
          setBrandSelection(Codec.unpack(value));
          break;

        case CRITERIA:
          BeeUtils.overwrite(getCriteria(), DataUtils.parseIdList(value));
          break;
      }
    }
  }

  public List<Long> getCriteria() {
    return criteria;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public boolean hasBrandSelection() {
    return brandSelection;
  }

  public boolean hasFilters() {
    return hasBrandSelection() || !getCriteria().isEmpty();
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

        case BRAND_SELECTION:
          arr[i++] = Codec.pack(hasBrandSelection());
          break;

        case CRITERIA:
          arr[i++] = DataUtils.buildIdList(getCriteria());
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setBrandSelection(boolean brandSelection) {
    this.brandSelection = brandSelection;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }
}
