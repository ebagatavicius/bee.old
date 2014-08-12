package com.butent.bee.shared;

import java.util.Collection;
import java.util.List;

public interface HasItems {

  String TAG_ITEM = "item";

  void addItem(String item);

  void addItems(Collection<String> items);

  int getItemCount();

  List<String> getItems();

  boolean isEmpty();

  boolean isIndex(int index);

  void setItems(Collection<String> items);
}
