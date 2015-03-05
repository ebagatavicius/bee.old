package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;

import java.util.ArrayList;
import java.util.List;

/**
 * Enables using definitions for user interface components.
 */

public class DefinitionList extends Widget implements IdentifiableWidget {

  private static final int INSERT_AT_END = -1;
  private List<Element> items = new ArrayList<>();

  public DefinitionList() {
    setElement(Document.get().createDLElement());

    DomUtils.createId(this, getIdPrefix());
    setStyleName(BeeConst.CSS_CLASS_PREFIX + "DefinitionList");
  }

  public void addDefinition(String text) {
    insertItem(text, true, INSERT_AT_END);
  }

  public void addItem(String text) {
    insertItem(text, false, INSERT_AT_END);
  }

  public void clear() {
    for (Element item : items) {
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
    return "d-list";
  }

  public Element getItem(int index) {
    checkIndex(index);
    return items.get(index);
  }

  public int getItemCount() {
    return items.size();
  }

  public String getItemText(int index) {
    checkIndex(index);
    return getItem(index).getInnerText();
  }

  public void insertDefinition(String text, int index) {
    insertItem(text, true, index);
  }

  public void insertItem(String item, boolean definition, int index) {
    Element child = DomUtils.createDefinitionItem(definition, item).cast();

    String tag = child.getTagName().toLowerCase();
    child.setClassName(BeeConst.CSS_CLASS_PREFIX + "Definition-" + tag);

    if ((index < 0) || (index >= getItemCount())) {
      getElement().appendChild(child);
      items.add(child);
    } else {
      getElement().insertBefore(getItem(index), child);
      items.add(index, child);
    }
  }

  public void insertItem(String text, int index) {
    insertItem(text, false, index);
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

  public void setItemText(int index, String text) {
    checkIndex(index);
    Assert.notNull(text);

    getItem(index).setInnerText(text);
  }

  private void checkIndex(int index) {
    Assert.isIndex(items, index);
  }
}
