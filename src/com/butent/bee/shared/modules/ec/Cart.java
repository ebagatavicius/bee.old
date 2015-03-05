package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Cart implements BeeSerializable {

  private enum Serial {
    DELIVERY_ADDRESS, DELIVERY_METHOD, COMMENT, ITEMS
  }

  public static Cart restore(String s) {
    Cart cart = new Cart();
    cart.deserialize(s);
    return cart;
  }

  private String deliveryAddress;
  private Long deliveryMethod;

  private String comment;

  private final List<CartItem> items = new ArrayList<>();

  public Cart() {
    super();
  }

  public CartItem add(EcItem ecItem, int quantity) {
    if (ecItem != null && quantity > 0) {
      CartItem item = getItem(ecItem.getArticleId());

      if (item == null) {
        item = new CartItem(ecItem, quantity);
        items.add(item);
      } else {
        item.add(quantity);
      }
      return item;

    } else {
      return null;
    }
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
        case DELIVERY_ADDRESS:
          setDeliveryAddress(value);
          break;

        case DELIVERY_METHOD:
          setDeliveryMethod(BeeUtils.toLongOrNull(value));
          break;

        case COMMENT:
          setComment(value);
          break;

        case ITEMS:
          items.clear();
          String[] itemArr = Codec.beeDeserializeCollection(value);
          if (itemArr != null) {
            for (String it : itemArr) {
              items.add(CartItem.restore(it));
            }
          }
          break;
      }
    }
  }

  public String getComment() {
    return comment;
  }

  public String getDeliveryAddress() {
    return deliveryAddress;
  }

  public Long getDeliveryMethod() {
    return deliveryMethod;
  }

  public List<CartItem> getItems() {
    return items;
  }

  public int getQuantity(long articleId) {
    CartItem item = getItem(articleId);
    return (item == null) ? 0 : item.getQuantity();
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public boolean remove(EcItem ecItem) {
    for (Iterator<CartItem> it = items.iterator(); it.hasNext();) {
      CartItem item = it.next();

      if (item.getEcItem().equals(ecItem)) {
        it.remove();
        return true;
      }
    }
    return false;
  }

  public void reset() {
    setComment(null);
    items.clear();
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case DELIVERY_ADDRESS:
          arr[i++] = getDeliveryAddress();
          break;

        case DELIVERY_METHOD:
          arr[i++] = getDeliveryMethod();
          break;

        case COMMENT:
          arr[i++] = getComment();
          break;

        case ITEMS:
          arr[i++] = items;
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setDeliveryAddress(String deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }

  public void setDeliveryMethod(Long deliveryMethod) {
    this.deliveryMethod = deliveryMethod;
  }

  public int totalCents() {
    int total = 0;
    for (CartItem item : items) {
      total += item.getQuantity() * item.getEcItem().getPrice();
    }
    return total;
  }

  public int totalQuantity() {
    int total = 0;
    for (CartItem item : items) {
      total += item.getQuantity();
    }
    return total;
  }

  private CartItem getItem(long articleId) {
    for (CartItem item : items) {
      if (item.getEcItem().getArticleId() == articleId) {
        return item;
      }
    }
    return null;
  }
}
