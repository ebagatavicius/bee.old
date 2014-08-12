package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.Codec;

public class ArticleCriteria implements BeeSerializable {

  public static ArticleCriteria restore(String s) {
    ArticleCriteria criteria = new ArticleCriteria();
    criteria.deserialize(s);
    return criteria;
  }

  private String name;
  private String value;

  public ArticleCriteria(String name, String value) {
    this.name = name;
    this.value = value;
  }

  private ArticleCriteria() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    setName(arr[0]);
    setValue(arr[1]);
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String serialize() {
    String[] arr = new String[] {getName(), getValue()};
    return Codec.beeSerialize(arr);
  }

  private void setName(String name) {
    this.name = name;
  }

  private void setValue(String value) {
    this.value = value;
  }
}
