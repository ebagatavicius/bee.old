package com.butent.bee.client.modules.ec.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.InputEvent;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.CustomWidget;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class IndexSelector extends Flow implements HasSelectionHandlers<Integer> {

  private static final String STYLE_PREFIX = EcStyles.name("IndexSelector-");

  private static final String STYLE_PANEL = STYLE_PREFIX + "panel";

  private static final String STYLE_SEARCH = STYLE_PREFIX + "search";

  private static final String STYLE_ITEM_CONTAINER = STYLE_PREFIX + "itemContainer";
  private static final String STYLE_ITEM = STYLE_PREFIX + "item";

  private static final String STYLE_COUNTER = STYLE_PREFIX + "counter";

  private final InputText searchBox;
  private final Element itemContainer;
  private final CustomDiv counter;

  private int itemCount;

  private String searchQuery;
  private int numberOfHiddenItems;

  private boolean hasSelectionHandler;

  public IndexSelector(String styleName) {
    super(styleName);
    addStyleName(STYLE_PANEL);

    this.searchBox = new InputText();
    searchBox.addStyleName(STYLE_SEARCH);
    DomUtils.setSearch(searchBox);

    searchBox.addInputHandler(new InputHandler() {
      @Override
      public void onInput(InputEvent event) {
        IndexSelector.this.doSearch(IndexSelector.this.searchBox.getValue());
      }
    });

    searchBox.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          IndexSelector.this.onEnterKeyDown();
        }
      }
    });

    add(searchBox);

    this.itemContainer = Document.get().createDivElement();

    CustomWidget container = new CustomWidget(itemContainer, STYLE_ITEM_CONTAINER);
    container.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        IndexSelector.this.onItemClick(event);
      }
    });

    add(container);

    this.counter = new CustomDiv(STYLE_COUNTER);
    add(counter);
  }

  @Override
  public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler) {
    hasSelectionHandler = true;
    return addHandler(handler, SelectionEvent.getType());
  }

  public void enableAutocomplete(String key) {
    AutocompleteProvider.enableAutocomplete(searchBox, key);
  }

  public void focus() {
    if (searchBox != null) {
      searchBox.setFocus(true);
    }
  }

  public boolean hasSelectionHandler() {
    return hasSelectionHandler;
  }

  @Override
  public boolean isEmpty() {
    return getItemCount() <= 0;
  }

  public void render(List<String> data) {
    if (searchBox != null) {
      searchBox.clearValue();
    }

    setSearchQuery(null);
    setNumberOfHiddenItems(0);

    DomUtils.clear(itemContainer);

    addItems(data);
    setItemCount(data.size());

    refreshCounter();
  }

  public void retainValue(String value) {
    AutocompleteProvider.retainValue(searchBox, value);
  }

  private void addItem(String item, int index) {
    Element itemElement = Document.get().createDivElement();

    itemElement.addClassName(STYLE_ITEM);
    itemElement.setInnerText(item);

    DomUtils.setDataIndex(itemElement, index);

    itemContainer.appendChild(itemElement);
  }

  private void addItems(List<String> items) {
    for (int i = 0; i < items.size(); i++) {
      addItem(items.get(i), i);
    }
  }

  private void doSearch(String input) {
    String oldQuery = getSearchQuery();
    String newQuery = BeeUtils.trim(input);
    setSearchQuery(newQuery);

    if (BeeUtils.same(oldQuery, newQuery)) {
      return;
    }
    if (getNumberOfVisibleItems() <= 0 && BeeUtils.containsSame(newQuery, oldQuery)) {
      return;
    }
    if (getNumberOfHiddenItems() <= 0 && BeeUtils.isEmpty(newQuery)) {
      return;
    }

    int hideCnt = 0;

    for (Element ie = itemContainer.getFirstChildElement(); ie != null; ie =
        ie.getNextSiblingElement()) {

      if (StyleUtils.hasClassName(ie, STYLE_ITEM)) {
        boolean match = matches(ie, newQuery);
        StyleUtils.setVisible(ie, match);

        if (!match) {
          hideCnt++;
        }
      }
    }

    if (getNumberOfHiddenItems() != hideCnt) {
      setNumberOfHiddenItems(hideCnt);
      refreshCounter();
    }
  }

  private void fireSelection(int index) {
    SelectionEvent.fire(this, index);
  }

  private int getItemCount() {
    return itemCount;
  }

  private int getNumberOfHiddenItems() {
    return numberOfHiddenItems;
  }

  private int getNumberOfVisibleItems() {
    return getItemCount() - getNumberOfHiddenItems();
  }

  private String getSearchQuery() {
    return searchQuery;
  }

  private static boolean matches(Element itemElement, String query) {
    return BeeUtils.isEmpty(query)
        ? true : BeeUtils.containsSame(itemElement.getInnerText(), query);
  }

  private void onEnterKeyDown() {
    if (!BeeUtils.isEmpty(getSearchQuery()) && getNumberOfVisibleItems() > 0) {
      int index = DomUtils.getDataIndexInt(DomUtils.getFirstVisibleChild(itemContainer));
      if (!BeeConst.isUndef(index)) {
        fireSelection(index);
      }
    }
  }

  private void onItemClick(ClickEvent event) {
    int index = DomUtils.getDataIndexInt(EventUtils.getEventTargetElement(event));
    if (!BeeConst.isUndef(index)) {
      fireSelection(index);
    }
  }

  private void refreshCounter() {
    int cnt = getItemCount();

    String text;
    if (cnt <= 0) {
      text = BeeConst.STRING_EMPTY;
    } else if (getNumberOfHiddenItems() > 0) {
      text = BeeUtils.join(BeeConst.STRING_SLASH, getNumberOfVisibleItems(), cnt);
    } else {
      text = BeeUtils.toString(cnt);
    }

    counter.setHtml(text);
  }

  private void setItemCount(int itemCount) {
    this.itemCount = itemCount;
  }

  private void setNumberOfHiddenItems(int numberOfHiddenItems) {
    this.numberOfHiddenItems = numberOfHiddenItems;
  }

  private void setSearchQuery(String searchQuery) {
    this.searchQuery = searchQuery;
  }
}
