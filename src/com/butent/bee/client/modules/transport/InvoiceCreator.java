package com.butent.bee.client.modules.transport;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;

class InvoiceCreator extends Image implements ClickHandler {

  private final String viewName;
  private final Filter filter;

  public InvoiceCreator(String viewName, Filter filter) {
    super(Global.getImages().silverInvoice());
    setTitle(Localized.getConstants().createInvoice());
    setAlt(getTitle());
    addClickHandler(this);

    this.viewName = Assert.notEmpty(viewName);
    this.filter = filter;
  }

  @Override
  public void onClick(ClickEvent event) {
    GridPanel grid = new GridPanel(viewName, GridOptions.forFilter(filter), false);

    StyleUtils.setSize(grid, 800, 600);

    DialogBox dialog = DialogBox.create(null);
    dialog.setWidget(grid);
    dialog.setAnimationEnabled(true);
    dialog.setHideOnEscape(true);
    dialog.center();
  }
}