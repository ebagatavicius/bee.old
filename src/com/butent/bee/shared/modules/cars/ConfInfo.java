package com.butent.bee.shared.modules.cars;

import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfInfo implements BeeSerializable {
  private String price;
  private String description;
  private Map<String, String> criteria = new LinkedHashMap<>();
  private String photo;

  protected ConfInfo() {
  }

  @Override
  public void deserialize(String s) {
    String[] dataInfo = Codec.beeDeserializeCollection(s);
    this.price = dataInfo[0];
    this.description = dataInfo[1];
    setCriteria(Codec.deserializeLinkedHashMap(ArrayUtils.getQuietly(dataInfo, 2)));
    this.photo = dataInfo[3];
  }

  public Map<String, String> getCriteria() {
    return criteria;
  }

  public String getDescription() {
    return description;
  }

  public String getPhoto() {
    return photo;
  }

  public String getPrice() {
    return price;
  }

  public static ConfInfo of(String prc, String descr) {
    ConfInfo info = new ConfInfo();
    info.setPrice(prc);
    info.setDescription(descr);
    return info;
  }

  public static ConfInfo restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    ConfInfo info = new ConfInfo();
    info.deserialize(s);
    return info;
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {price, description, criteria, photo});
  }

  public ConfInfo setCriteria(Map<String, String> newCriteria) {
    this.criteria.clear();

    if (!BeeUtils.isEmpty(newCriteria)) {
      this.criteria.putAll(newCriteria);
    }
    return this;
  }

  public void setDescription(String description) {
    this.description = BeeUtils.isEmpty(description) ? null : description.replace("\n", "<br>");
  }

  public ConfInfo setPhoto(String newPhoto) {
    this.photo = newPhoto;
    return this;
  }

  public void setPrice(String price) {
    this.price = price;
  }
}
