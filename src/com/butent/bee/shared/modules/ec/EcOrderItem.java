package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class EcOrderItem implements BeeSerializable {

  private enum Serial {
    ARTICLE_ID, NAME, CODE, QUANTITY, PRICE, UNIT, WEIGHT
  }

  public static EcOrderItem restore(String s) {
    EcOrderItem item = new EcOrderItem();
    item.deserialize(s);
    return item;
  }

  private long articleId;

  private String name;
  private String code;

  private Integer quantity;
  private Double price;

  private String unit;
  private Double weight;

  public EcOrderItem() {
    super();
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
        case ARTICLE_ID:
          setArticleId(BeeUtils.toLong(value));
          break;

        case NAME:
          setName(value);
          break;

        case CODE:
          setCode(value);
          break;

        case QUANTITY:
          setQuantity(BeeUtils.toIntOrNull(value));
          break;

        case PRICE:
          setPrice(BeeUtils.toDoubleOrNull(value));
          break;

        case UNIT:
          setUnit(value);
          break;

        case WEIGHT:
          setWeight(BeeUtils.toDouble(value));
          break;
      }
    }
  }

  public long getArticleId() {
    return articleId;
  }
  
  public double getAmount() {
    return BeeUtils.unbox(getQuantity()) * BeeUtils.unbox(getPrice());
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public Double getPrice() {
    return price;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public String getUnit() {
    return unit;
  }

  public Double getWeight() {
    return weight;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ARTICLE_ID:
          arr[i++] = getArticleId();
          break;

        case NAME:
          arr[i++] = getName();
          break;

        case CODE:
          arr[i++] = getCode();
          break;

        case QUANTITY:
          arr[i++] = getQuantity();
          break;

        case PRICE:
          arr[i++] = getPrice();
          break;

        case UNIT:
          arr[i++] = getUnit();
          break;

        case WEIGHT:
          arr[i++] = getWeight();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setArticleId(long articleId) {
    this.articleId = articleId;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPrice(Double price) {
    this.price = price;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public void setWeight(Double weight) {
    this.weight = weight;
  }
}
