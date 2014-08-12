package com.butent.bee.client.modules.ec.view;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.widget.ItemPanel;
import com.butent.bee.client.modules.ec.widget.ItemSelector;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class SearchByItem extends EcView implements SelectionHandler<InputText> {

  private final String service;
  private final String selectorCaption;

  private final ItemPanel itemPanel;

  SearchByItem(String service, String selectorCaption) {
    super();
    this.service = service;
    this.selectorCaption = selectorCaption;

    this.itemPanel = new ItemPanel();
  }

  @Override
  public void onSelection(SelectionEvent<InputText> event) {
    itemPanel.clear();

    final InputText editor = event.getSelectedItem();

    EcKeeper.searchItems(service, BeeUtils.trim(editor.getValue()), new Consumer<List<EcItem>>() {
      @Override
      public void accept(List<EcItem> input) {
        AutocompleteProvider.retainValue(editor);
        EcKeeper.renderItems(itemPanel, input);
      }
    });
  }

  @Override
  protected void createUi() {
    ItemSelector selector = new ItemSelector(selectorCaption, EcConstants.NAME_PREFIX + service);
    selector.addSelectionHandler(this);

    add(selector);
    add(itemPanel);
  }

  @Override
  protected String getPrimaryStyle() {
    return "searchByItem";
  }
}
