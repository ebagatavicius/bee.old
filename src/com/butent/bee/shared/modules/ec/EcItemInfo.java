package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class EcItemInfo implements BeeSerializable {

  private enum Serial {
    CRITERIA, CAR_TYPES, OE_NUMBERS
  }

  public static EcItemInfo restore(String s) {
    EcItemInfo ecItemInfo = new EcItemInfo();
    ecItemInfo.deserialize(s);
    return ecItemInfo;
  }

  private final List<ArticleCriteria> criteria = new ArrayList<>();

  private final List<EcCarType> carTypes = new ArrayList<>();

  private final List<String> oeNumbers = new ArrayList<>();

  public EcItemInfo() {
    super();
  }

  public void addCarType(EcCarType carType) {
    if (carType != null) {
      carTypes.add(carType);
    }
  }

  public void addCriteria(ArticleCriteria ac) {
    if (ac != null) {
      criteria.add(ac);
    }
  }

  public void addOeNumber(String oeNumber) {
    if (!BeeUtils.isEmpty(oeNumber)) {
      oeNumbers.add(oeNumber);
    }
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      String[] values = Codec.beeDeserializeCollection(arr[i]);
      if (values == null) {
        continue;
      }

      switch (members[i]) {
        case CRITERIA:
          criteria.clear();
          for (String v : values) {
            criteria.add(ArticleCriteria.restore(v));
          }
          break;

        case CAR_TYPES:
          carTypes.clear();
          for (String v : values) {
            carTypes.add(EcCarType.restore(v));
          }
          break;

        case OE_NUMBERS:
          oeNumbers.clear();
          for (String v : values) {
            oeNumbers.add(v);
          }
      }
    }
  }

  public List<EcCarType> getCarTypes() {
    return carTypes;
  }

  public List<ArticleCriteria> getCriteria() {
    return criteria;
  }

  public List<String> getOeNumbers() {
    return oeNumbers;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case CRITERIA:
          arr[i++] = getCriteria();
          break;

        case CAR_TYPES:
          arr[i++] = getCarTypes();
          break;

        case OE_NUMBERS:
          arr[i++] = getOeNumbers();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }
}
