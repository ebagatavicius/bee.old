package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class EcOrderItem implements BeeSerializable, HasCaption {

  private enum Serial {
    ARTICLE_ID, NAME, CODE, QUANTITY_ORDERED, QUANTITY_SUBMIT, PRICE, UNIT, WEIGHT
  }

  public static EcOrderItem restore(String s) {
    EcOrderItem item = new EcOrderItem();
    item.deserialize(s);
    return item;
  }

  private long articleId;

  private String name;
  private String code;

  private Integer quantityOrdered;
  private Integer quantitySubmit;

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

        case QUANTITY_ORDERED:
          setQuantityOrdered(BeeUtils.toIntOrNull(value));
          break;

        case QUANTITY_SUBMIT:
          setQuantitySubmit(BeeUtils.toIntOrNull(value));
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
    return BeeUtils.unbox(getQuantitySubmit()) * BeeUtils.unbox(getPrice());
  }

  @Override
  public String getCaption() {
    return BeeUtils.joinWords(getName(), getCode());
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

        case QUANTITY_ORDERED:
          arr[i++] = getQuantityOrdered();
          break;

        case QUANTITY_SUBMIT:
          arr[i++] = getQuantitySubmit();
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

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public void setWeight(Double weight) {
    this.weight = weight;
  }

  public Integer getQuantityOrdered() {
    return quantityOrdered;
  }

  public void setQuantityOrdered(Integer quantityOrdered) {
    this.quantityOrdered = quantityOrdered;
  }

  public Integer getQuantitySubmit() {
    return quantitySubmit;
  }

  public void setQuantitySubmit(Integer quantitySubmit) {
    this.quantitySubmit = quantitySubmit;
  }
}
