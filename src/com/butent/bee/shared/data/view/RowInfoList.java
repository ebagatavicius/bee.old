package com.butent.bee.shared.data.view;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("serial")
public class RowInfoList extends ArrayList<RowInfo> implements BeeSerializable {

  public static RowInfoList restore(String s) {
    Assert.notEmpty(s);
    RowInfoList result = new RowInfoList();
    result.deserialize(s);
    return result;
  }

  public RowInfoList() {
    super();
  }

  public RowInfoList(Collection<? extends RowInfo> c) {
    super(c);
  }

  public RowInfoList(int initialCapacity) {
    super(initialCapacity);
  }

  @Override
  public void deserialize(String s) {
    if (!isEmpty()) {
      clear();
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    if (arr != null) {
      for (String el : arr) {
        RowInfo rowInfo = RowInfo.restore(el);
        if (rowInfo != null) {
          add(rowInfo);
        }
      }
    }
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(this);
  }
}
