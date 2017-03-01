package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ItemQuantities implements BeeSerializable {

  private String article;
  private double stock;
  private final Map<Long, Double> reservedMap = new TreeMap<>();

  public ItemQuantities(String article) {
    setArticle(article);
  }

  private ItemQuantities() {
  }

  public ItemQuantities addReserved(DateTime dateTime, Double quantity) {
    long millis = Objects.isNull(dateTime) ? 0 : dateTime.getTime();
    reservedMap.put(millis, BeeUtils.unbox(reservedMap.get(millis)) + BeeUtils.unbox(quantity));
    return this;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 3);

    setArticle(arr[0]);
    setStock(BeeUtils.toDouble(arr[1]));

    reservedMap.clear();
    Codec.deserializeHashMap(arr[2]).forEach((millis, qty) ->
        reservedMap.put(BeeUtils.toLong(millis), BeeUtils.toDouble(qty)));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ItemQuantities)) {
      return false;
    }
    ItemQuantities itemQuantities = (ItemQuantities) o;

    return Objects.equals(article, itemQuantities.article);
  }

  public String getArticle() {
    return article;
  }

  public double getReserved() {
    return reservedMap.values().stream().mapToDouble(Double::doubleValue).sum();
  }

  public Map<DateTime, Double> getReservedMap() {
    Map<DateTime, Double> map = new LinkedHashMap<>();
    reservedMap.forEach((millis, qty) -> map.put(new DateTime(millis), qty));
    return map;
  }

  public double getStock() {
    return stock;
  }

  @Override
  public int hashCode() {
    return Objects.hash(article);
  }

  public static ItemQuantities restore(String s) {
    ItemQuantities itemQuantities = new ItemQuantities();
    itemQuantities.deserialize(s);
    return itemQuantities;
  }

  public static ItemQuantities stock(String article, Double stock) {
    ItemQuantities itemQuantities = new ItemQuantities(article);
    itemQuantities.setStock(stock);
    return itemQuantities;
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {article, stock, reservedMap});
  }

  public void setStock(Double stock) {
    this.stock = BeeUtils.unbox(stock);
  }

  @Override
  public String toString() {
    return "a=" + article + " s=" + BeeUtils.toString(stock)
        + " r=" + BeeUtils.toString(getReserved());
  }

  private void setArticle(String article) {
    this.article = article;
  }
}
