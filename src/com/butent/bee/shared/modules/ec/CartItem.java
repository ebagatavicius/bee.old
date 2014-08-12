package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class CartItem implements BeeSerializable {

  private enum Serial {
    EC_ITEM, QUANTITY, NOTE
  }

  public static CartItem restore(String s) {
    CartItem cartItem = new CartItem();
    cartItem.deserialize(s);
    return cartItem;
  }

  private EcItem ecItem;

  private int quantity;

  private String note;

  public CartItem(EcItem ecItem, int quantity) {
    this.ecItem = ecItem;
    this.quantity = quantity;
  }

  private CartItem() {
  }

  public void add(int qty) {
    setQuantity(getQuantity() + qty);
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
        case EC_ITEM:
          setEcItem(EcItem.restore(value));
          break;

        case QUANTITY:
          setQuantity(BeeUtils.toInt(value));
          break;

        case NOTE:
          setNote(value);
          break;
      }
    }
  }

  public EcItem getEcItem() {
    return ecItem;
  }

  public String getNote() {
    return note;
  }

  public int getQuantity() {
    return quantity;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case EC_ITEM:
          arr[i++] = getEcItem();
          break;

        case QUANTITY:
          arr[i++] = getQuantity();
          break;

        case NOTE:
          arr[i++] = getNote();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setNote(String note) {
    this.note = note;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  private void setEcItem(EcItem ecItem) {
    this.ecItem = ecItem;
  }
}
