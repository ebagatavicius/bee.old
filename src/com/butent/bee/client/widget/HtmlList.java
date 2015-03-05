package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasItems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implements a user interface component that can contain a list of HTML code items.
 */

public class HtmlList extends Widget implements IdentifiableWidget, HasItems {

  private static final int INSERT_AT_END = -1;

  private final boolean ordered;

  private final List<LIElement> items = new ArrayList<>();

  public HtmlList() {
    this(false);
  }

  public HtmlList(boolean ordered) {
    if (ordered) {
      setElement(Document.get().createOLElement());
    } else {
      setElement(Document.get().createULElement());
    }

    setStyleName(BeeConst.CSS_CLASS_PREFIX + "HtmlList");
    DomUtils.createId(this, getIdPrefix());

    this.ordered = ordered;
  }

  @Override
  public void addItem(String item) {
    insertItem(item, INSERT_AT_END);
  }

  @Override
  public void addItems(Collection<String> col) {
    for (String item : col) {
      addItem(item);
    }
  }

  public void clear() {
    for (LIElement item : items) {
      getElement().removeChild(item);
    }
    items.clear();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "html-list";
  }

  public LIElement getItem(int index) {
    checkIndex(index);
    return items.get(index);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  public String getItemHtml(int index) {
    checkIndex(index);
    return getItem(index).getInnerHTML();
  }

  @Override
  public List<String> getItems() {
    List<String> result = new ArrayList<>();
    for (int i = 0; i < getItemCount(); i++) {
      result.add(getItemHtml(i));
    }
    return result;
  }

  public String getItemText(int index) {
    checkIndex(index);
    return getItem(index).getInnerText();
  }

  public void insertItem(String item, int index) {
    LIElement child = DomUtils.createListItem(item).cast();
    child.setClassName(BeeConst.CSS_CLASS_PREFIX + "HtmlListItem");

    if ((index < 0) || (index >= getItemCount())) {
      getElement().appendChild(child);
      items.add(child);
    } else {
      getElement().insertBefore(getItem(index), child);
      items.add(index, child);
    }
  }

  @Override
  public boolean isEmpty() {
    return getItemCount() <= 0;
  }

  @Override
  public boolean isIndex(int index) {
    return index >= 0 && index < getItemCount();
  }

  public boolean isOrdered() {
    return ordered;
  }

  public void removeItem(int index) {
    checkIndex(index);

    getElement().removeChild(getItem(index));
    items.remove(index);
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setItemHtml(int index, String html) {
    checkIndex(index);
    Assert.notNull(html);

    getItem(index).setInnerHTML(html);
  }

  @Override
  public void setItems(Collection<String> items) {
    if (getItemCount() > 0) {
      clear();
    }
    if (items != null) {
      addItems(items);
    }
  }

  public void setItemText(int index, String text) {
    checkIndex(index);
    Assert.notNull(text);

    getItem(index).setInnerText(text);
  }

  private void checkIndex(int index) {
    Assert.isIndex(items, index);
  }
}
