package com.butent.bee.client.modules.ec.view;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.modules.ec.widget.IndexSelector;
import com.butent.bee.client.modules.ec.widget.ItemPanel;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcBrand;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class SearchByBrand extends EcView {

  private static final String STYLE_PREFIX = EcStyles.name("searchByBrand-");
  private static final String STYLE_BRAND = STYLE_PREFIX + "brand-";

  private static final Edges selectorMargins = new Edges(0, 0, 3, 0);

  private final Button brandWidget;
  private final IndexSelector brandSelector;

  private final ItemPanel itemPanel;

  private final List<EcBrand> brands = Lists.newArrayList();
  private int brandIndex = BeeConst.UNDEF;

  SearchByBrand() {
    super();

    this.brandWidget = new Button(Localized.getConstants().ecItemBrand());
    brandWidget.addStyleName(STYLE_BRAND + "widget");

    this.brandSelector = new IndexSelector(STYLE_BRAND + "selector");
    brandSelector.enableAutocomplete(EcConstants.NAME_PREFIX + "brand-selector");

    this.itemPanel = new ItemPanel();
  }

  @Override
  protected void createUi() {
    add(brandWidget);
    add(itemPanel);

    brandWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        openBrands();
      }
    });
  }

  @Override
  protected String getPrimaryStyle() {
    return "searchByBrand";
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        openBrands();
      }
    });
  }

  private int getBrandIndex() {
    return brandIndex;
  }

  private void onSelectBrand(int index) {
    UiHelper.closeDialog(brandSelector);
    if (!BeeUtils.isIndex(brands, index)) {
      return;
    }

    if (index != getBrandIndex()) {
      setBrandIndex(index);

      EcBrand brand = brands.get(index);
      String name = brand.getName();

      brandWidget.setHtml(name);
      brandWidget.addStyleName(STYLE_BRAND + "selected");

      brandSelector.retainValue(name);

      itemPanel.clear();

      ParameterList params = EcKeeper.createArgs(SVC_GET_ITEMS_BY_BRAND);
      params.addQueryItem(COL_TCD_BRAND, brand.getId());

      EcKeeper.requestItems(SVC_GET_ITEMS_BY_BRAND, name, params, new Consumer<List<EcItem>>() {
        @Override
        public void accept(List<EcItem> items) {
          EcKeeper.renderItems(itemPanel, items);
        }
      });
    }
  }

  private void openBrands() {
    EcKeeper.getItemBrands(new Consumer<List<EcBrand>>() {
      @Override
      public void accept(List<EcBrand> input) {
        brands.clear();
        brands.addAll(input);

        if (!brandSelector.hasSelectionHandler()) {
          brandSelector.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
              onSelectBrand(event.getSelectedItem());
            }
          });
        }

        List<String> names = Lists.newArrayList();
        for (EcBrand brand : brands) {
          names.add(brand.getName());
        }
        brandSelector.render(names);

        Popup popup = new Popup(OutsideClick.CLOSE, STYLE_BRAND + "dialog");
        popup.setWidget(brandSelector);

        popup.setHideOnEscape(true);
        popup.showRelativeTo(brandWidget.getElement(), selectorMargins);

        brandSelector.focus();
      }
    });
  }

  private void setBrandIndex(int brandIndex) {
    this.brandIndex = brandIndex;
  }
}
