package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class EcOrder implements BeeSerializable {

  private enum Serial {
    ORDER_ID, DATE, STATUS, MANAGER, DELIVERY_ADDRESS, DELIVERY_METHOD, COMMENT, REJECTION_REASON,
    ITEMS, EVENTS
  }

  public static EcOrder restore(String s) {
    EcOrder order = new EcOrder();
    order.deserialize(s);
    return order;
  }

  private long orderId;
  private DateTime date;

  private int status;
  private String manager;

  private String deliveryMethod;
  private String deliveryAddress;

  private String comment;

  private String rejectionReason;

  private final List<EcOrderItem> items = new ArrayList<>();
  private final List<EcOrderEvent> events = new ArrayList<>();

  public EcOrder() {
    super();
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (members[i]) {
        case ORDER_ID:
          setOrderId(BeeUtils.toLong(value));
          break;

        case DATE:
          setDate(DateTime.restore(value));
          break;

        case STATUS:
          setStatus(BeeUtils.toInt(value));
          break;

        case MANAGER:
          setManager(value);
          break;

        case DELIVERY_ADDRESS:
          setDeliveryAddress(value);
          break;

        case DELIVERY_METHOD:
          setDeliveryMethod(value);
          break;

        case COMMENT:
          setComment(value);
          break;

        case REJECTION_REASON:
          setRejectionReason(value);
          break;

        case ITEMS:
          items.clear();
          String[] itemArr = Codec.beeDeserializeCollection(value);
          if (itemArr != null) {
            for (String it : itemArr) {
              items.add(EcOrderItem.restore(it));
            }
          }
          break;

        case EVENTS:
          events.clear();
          String[] eventArr = Codec.beeDeserializeCollection(value);
          if (eventArr != null) {
            for (String ev : eventArr) {
              events.add(EcOrderEvent.restore(ev));
            }
          }
          break;
      }
    }
  }

  public double getAmount() {
    double total = 0;
    for (EcOrderItem item : items) {
      total += item.getAmount();
    }
    return total;
  }

  public String getComment() {
    return comment;
  }

  public DateTime getDate() {
    return date;
  }

  public String getDeliveryAddress() {
    return deliveryAddress;
  }

  public String getDeliveryMethod() {
    return deliveryMethod;
  }

  public List<EcOrderEvent> getEvents() {
    return events;
  }

  public List<EcOrderItem> getItems() {
    return items;
  }

  public String getManager() {
    return manager;
  }

  public long getOrderId() {
    return orderId;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public int getStatus() {
    return status;
  }

  public double getWeight() {
    double total = 0;
    for (EcOrderItem item : items) {
      total += BeeUtils.unbox(item.getQuantitySubmit()) * BeeUtils.unbox(item.getWeight());
    }
    return total;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ORDER_ID:
          arr[i++] = getOrderId();
          break;

        case DATE:
          arr[i++] = getDate();
          break;

        case STATUS:
          arr[i++] = getStatus();
          break;

        case MANAGER:
          arr[i++] = getManager();
          break;

        case DELIVERY_ADDRESS:
          arr[i++] = getDeliveryAddress();
          break;

        case DELIVERY_METHOD:
          arr[i++] = getDeliveryMethod();
          break;

        case COMMENT:
          arr[i++] = getComment();
          break;

        case REJECTION_REASON:
          arr[i++] = getRejectionReason();
          break;

        case ITEMS:
          arr[i++] = getItems();
          break;

        case EVENTS:
          arr[i++] = getEvents();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setDate(DateTime date) {
    this.date = date;
  }

  public void setDeliveryAddress(String deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }

  public void setDeliveryMethod(String deliveryMethod) {
    this.deliveryMethod = deliveryMethod;
  }

  public void setManager(String manager) {
    this.manager = manager;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public void setStatus(int status) {
    this.status = status;
  }
}
