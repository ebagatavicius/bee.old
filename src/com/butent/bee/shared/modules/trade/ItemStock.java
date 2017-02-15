package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Objects;

public class ItemStock implements BeeSerializable {

  public static ItemStock restore(String s) {
    ItemStock itemStock = new ItemStock();
    itemStock.deserialize(s);
    return itemStock;
  }

  private String article;
  private double quantity;

  public ItemStock(String article, double quantity) {
    this.article = article;
    this.quantity = quantity;
  }

  private ItemStock() {
  }

  public String getArticle() {
    return article;
  }

  public double getQuantity() {
    return quantity;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    setArticle(arr[0]);
    setQuantity(BeeUtils.toDouble(arr[1]));
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {getArticle(), getQuantity()});
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ItemStock)) {
      return false;
    }
    ItemStock itemStock = (ItemStock) o;
    return Double.compare(itemStock.quantity, quantity) == 0
        && Objects.equals(article, itemStock.article);
  }

  @Override
  public int hashCode() {
    return Objects.hash(article, quantity);
  }

  @Override
  public String toString() {
    return "a=" + article + " q=" + BeeUtils.toString(quantity);
  }

  private void setArticle(String article) {
    this.article = article;
  }

  private void setQuantity(double quantity) {
    this.quantity = quantity;
  }
}
