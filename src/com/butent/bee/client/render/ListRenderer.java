package com.butent.bee.client.render;

import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.ui.HasValueStartIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListRenderer extends AbstractCellRenderer implements HasItems, HasValueStartIndex {

  private final List<String> itemList;
  private final HasItems itemProxy;

  private int valueStartIndex;

  public ListRenderer(CellSource cellSource) {
    this(cellSource, null);
  }

  public ListRenderer(CellSource cellSource, Collection<String> items) {
    this(cellSource, items, 0);
  }

  public ListRenderer(CellSource cellSource, Collection<String> items, int valueStartIndex) {
    this(cellSource, items, valueStartIndex, false, null);
  }

  public ListRenderer(CellSource cellSource, boolean useProxy, HasItems proxy) {
    this(cellSource, null, 0, useProxy, proxy);

    if (useProxy && proxy instanceof HasValueStartIndex) {
      setValueStartIndex(((HasValueStartIndex) proxy).getValueStartIndex());
    }
  }

  public ListRenderer(CellSource cellSource, boolean useProxy, HasItems proxy,
      int valueStartIndex) {
    this(cellSource, null, valueStartIndex, useProxy, proxy);
  }

  private ListRenderer(CellSource cellSource, Collection<String> items,
      int valueStartIndex, boolean useProxy, HasItems proxy) {
    super(cellSource);

    if (!useProxy || proxy == null) {
      this.itemList = new ArrayList<>();
      this.itemProxy = null;
      addItems(items);
    } else {
      this.itemList = null;
      this.itemProxy = proxy;
    }

    this.valueStartIndex = valueStartIndex;
  }

  @Override
  public void addItem(String item) {
    getItems().add(item);
  }

  @Override
  public void addItems(Collection<String> items) {
    if (items != null) {
      getItems().addAll(items);
    }
  }

  @Override
  public int getItemCount() {
    return getItems().size();
  }

  @Override
  public List<String> getItems() {
    if (itemProxy == null) {
      return itemList;
    } else {
      return itemProxy.getItems();
    }
  }

  @Override
  public int getValueStartIndex() {
    return valueStartIndex;
  }

  @Override
  public boolean isEmpty() {
    return getItemCount() <= 0;
  }

  @Override
  public boolean isIndex(int index) {
    return index >= 0 && index < getItemCount();
  }

  @Override
  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    Integer value = getInteger(row);
    if (value == null) {
      return null;
    }

    int index = value - getValueStartIndex();
    if (index >= 0 && index < getItemCount()) {
      return getItems().get(index);
    } else {
      return null;
    }
  }

  @Override
  public void setItems(Collection<String> items) {
    if (!getItems().isEmpty()) {
      getItems().clear();
    }
    addItems(items);
  }

  @Override
  public void setValueStartIndex(int valueStartIndex) {
    this.valueStartIndex = valueStartIndex;
  }
}
