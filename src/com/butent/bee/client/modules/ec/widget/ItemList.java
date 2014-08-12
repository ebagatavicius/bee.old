package com.butent.bee.client.modules.ec.widget;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.modules.ec.EcWidgetFactory;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.ArticleCriteria;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class ItemList extends Flow implements KeyDownHandler {

  private static final String STYLE_PRIMARY = "ItemList";
  private static final String STYLE_WAREHOUSE = "warehouse";

  private static final String STYLE_HEADER_ROW = EcStyles.name(STYLE_PRIMARY, "header");
  private static final String STYLE_ITEM_ROW = EcStyles.name(STYLE_PRIMARY, "item");

  private static final String STYLE_PICTURE = EcStyles.name(STYLE_PRIMARY, "picture");
  private static final String STYLE_INFO = EcStyles.name(STYLE_PRIMARY, "info");
  private static final String STYLE_STOCK_0 = EcStyles.name(STYLE_PRIMARY, "stock0");
  private static final String STYLE_STOCK_1 = EcStyles.name(STYLE_PRIMARY, "stock1");
  private static final String STYLE_STOCK_2 = EcStyles.name(STYLE_PRIMARY, "stock2");
  private static final String STYLE_STOCK_WRAPPER = EcStyles.name(STYLE_PRIMARY, "stockWrapper");
  private static final String STYLE_NO_STOCK = EcStyles.name(STYLE_PRIMARY, "noStock");
  private static final String STYLE_HAS_STOCK = EcStyles.name(STYLE_PRIMARY, "hasStock");
  private static final String STYLE_UNIT = EcStyles.name(STYLE_PRIMARY, "unit");
  private static final String STYLE_QUANTITY = EcStyles.name(STYLE_PRIMARY, "quantity");
  private static final String STYLE_LIST_PRICE = EcStyles.name(STYLE_PRIMARY, "list-price");
  private static final String STYLE_PRICE = EcStyles.name(STYLE_PRIMARY, "price");

  private static final String STYLE_CLIENT_AND_FEATURED_PRICE_PANEL = EcStyles.name(STYLE_PRIMARY,
      "client-and-fetured-price-panel");
  private static final String STYLE_CLIENT_PRICE_PREFIX = EcStyles.name(STYLE_PRIMARY,
      "client-price-");
  private static final String STYLE_FEATURED_PRICE_PREFIX = EcStyles.name(STYLE_PRIMARY,
      "featured-price-");

  private static final String STYLE_ITEM_NAME = EcStyles.name(STYLE_PRIMARY, "name");
  private static final String STYLE_ITEM_CODE = EcStyles.name(STYLE_PRIMARY, "code");
  private static final String STYLE_ITEM_ANALOGS = EcStyles.name(STYLE_PRIMARY, "analogs");

  private static final String STYLE_ITEM_BRAND = EcStyles.name(STYLE_PRIMARY, "brand");
  private static final String STYLE_ITEM_DESCRIPTION = EcStyles.name(STYLE_PRIMARY, "description");

  private static final String STYLE_CRITERIA_PREFIX = EcStyles.name(STYLE_PRIMARY, "criteria-");
  private static final String STYLE_CRITERIA_PANEL = STYLE_CRITERIA_PREFIX + "panel";
  private static final String STYLE_CRITERIA_ENTRY = STYLE_CRITERIA_PREFIX + "entry";
  private static final String STYLE_CRITERIA_NAME = STYLE_CRITERIA_PREFIX + "name";
  private static final String STYLE_CRITERIA_VALUE = STYLE_CRITERIA_PREFIX + "value";

  private static final String STYLE_LABEL = "-label";

  private static final int PAGE_SIZE = 50;

  private static Widget renderClientAndFeaturedPrice(int clientPrice, int featuredPrice) {
    Flow panel = new Flow(STYLE_CLIENT_AND_FEATURED_PRICE_PANEL);

    Flow clientPanel = new Flow(STYLE_CLIENT_PRICE_PREFIX + "panel");

    InlineLabel clientValue = new InlineLabel(EcUtils.formatCents(clientPrice));
    clientValue.addStyleName(STYLE_CLIENT_PRICE_PREFIX + "value");
    clientPanel.add(clientValue);

    InlineLabel clientCurrency = new InlineLabel(EcConstants.CURRENCY);
    clientCurrency.addStyleName(STYLE_CLIENT_PRICE_PREFIX + "currency");
    clientPanel.add(clientCurrency);

    panel.add(clientPanel);

    Flow featuredPanel = new Flow(STYLE_FEATURED_PRICE_PREFIX + "panel");

    InlineLabel featuredValue = new InlineLabel(EcUtils.formatCents(featuredPrice));
    featuredValue.addStyleName(STYLE_FEATURED_PRICE_PREFIX + "value");
    featuredPanel.add(featuredValue);

    InlineLabel featuredCurrency = new InlineLabel(EcConstants.CURRENCY);
    featuredCurrency.addStyleName(STYLE_FEATURED_PRICE_PREFIX + "currency");
    featuredPanel.add(featuredCurrency);

    panel.add(featuredPanel);

    return panel;
  }

  private static Widget renderCriteria(List<ArticleCriteria> criteria) {
    Flow panel = new Flow(STYLE_CRITERIA_PANEL);

    for (ArticleCriteria criterion : criteria) {
      Flow entry = new Flow(STYLE_CRITERIA_ENTRY);

      InlineLabel nameLabel = new InlineLabel(criterion.getName());
      nameLabel.addStyleName(STYLE_CRITERIA_NAME);
      entry.add(nameLabel);

      InlineLabel valueLabel = new InlineLabel(criterion.getValue());
      valueLabel.addStyleName(STYLE_CRITERIA_VALUE);
      entry.add(valueLabel);

      panel.add(entry);
    }

    return panel;
  }

  private static Widget renderInfo(final EcItem item) {
    Flow panel = new Flow();

    String name = item.getName();
    if (!BeeUtils.isEmpty(name)) {
      Label itemName = new Label(name);
      itemName.addStyleName(STYLE_ITEM_NAME);

      String categoryNames = BeeUtils.join(BeeConst.STRING_EOL, EcKeeper.getCategoryNames(item));
      if (!BeeUtils.isEmpty(categoryNames)) {
        itemName.setTitle(categoryNames);
      }

      itemName.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          EcKeeper.openItem(item, true);
        }
      });

      panel.add(itemName);
    }

    String code = item.getCode();
    if (!BeeUtils.isEmpty(code)) {
      Widget codeWidget = EcWidgetFactory.renderField(Localized.getConstants().ecItemCode(), code,
          STYLE_ITEM_CODE);
      panel.add(codeWidget);
      codeWidget.setTitle(BeeUtils.joinWords("ArticleID:", item.getArticleId()));
    }

    if (item.getAnalogCount() > 0) {
      String analogLabel = BeeUtils.joinWords(Localized.getConstants().ecItemAnalogs(),
          BeeUtils.parenthesize(item.getAnalogCount()));
      InternalLink analogs = new InternalLink(analogLabel);
      analogs.addStyleName(STYLE_ITEM_ANALOGS);
      panel.add(analogs);

      analogs.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          ParameterList params = EcKeeper.createArgs(EcConstants.SVC_GET_ITEM_ANALOGS);
          params.addDataItem(EcConstants.COL_TCD_ARTICLE, item.getArticleId());
          params.addDataItem(EcConstants.COL_TCD_ARTICLE_NR, item.getCode());
          params.addDataItem(EcConstants.COL_TCD_BRAND, item.getBrand());

          BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              response.notify(BeeKeeper.getScreen());

              if (!response.hasErrors()) {
                List<EcItem> items = EcKeeper.getResponseItems(response);

                if (!BeeUtils.isEmpty(items)) {
                  ItemList analogList = new ItemList(items);

                  Simple analogPanel = new Simple(analogList);
                  analogPanel.addStyleName(STYLE_ITEM_ANALOGS + "-panel");

                  String caption = BeeUtils.joinWords(Localized.getConstants().ecItemAnalogs(),
                      item.getName(), item.getCode(), EcKeeper.getBrandName(item.getBrand()));
                  DialogBox dialog = DialogBox.create(caption, STYLE_ITEM_ANALOGS + "-dialog");

                  dialog.setWidget(analogPanel);

                  dialog.setAnimationEnabled(true);
                  dialog.setHideOnEscape(true);

                  dialog.cascade();

                } else {
                  BeeKeeper.getScreen().notifyWarning(Localized.getConstants().noData());
                }
              }
            }
          });
        }
      });
    }

    Long brand = item.getBrand();
    if (brand != null) {
      Widget brandWidget = EcWidgetFactory.renderField(Localized.getConstants().ecItemBrand(),
          EcKeeper.getBrandName(brand), STYLE_ITEM_BRAND);
      if (brandWidget != null) {
        panel.add(brandWidget);
      }
    }

    String description = item.getDescription();
    if (!BeeUtils.isEmpty(description)) {
      CustomDiv descriptionWidget = new CustomDiv(STYLE_ITEM_DESCRIPTION);
      descriptionWidget.setHtml(description);
      panel.add(descriptionWidget);
    }

    if (!item.getCriteria().isEmpty()) {
      Widget criteriaWidget = renderCriteria(item.getCriteria());
      if (criteriaWidget != null) {
        panel.add(criteriaWidget);
      }
    }

    return panel;
  }

  private static Widget renderPrice(int price, String style) {
    String stylePrefix = style + "-";

    Flow panel = new Flow();

    InlineLabel value = new InlineLabel(EcUtils.formatCents(price));
    value.addStyleName(stylePrefix + "value");
    panel.add(value);

    InlineLabel currency = new InlineLabel(EcConstants.CURRENCY);
    currency.addStyleName(stylePrefix + "currency");
    panel.add(currency);

    return panel;
  }

  private static Widget renderStock(int stock, String unit) {
    Flow wrapper = new Flow(STYLE_STOCK_WRAPPER);

    Widget stockWidget = EcWidgetFactory.createStockWidget(stock);
    stockWidget.addStyleName((stock > 0) ? STYLE_HAS_STOCK : STYLE_NO_STOCK);

    wrapper.add(stockWidget);

    if (stock > 0 && unit != null) {
      InlineLabel unitWidget = new InlineLabel(unit);
      unitWidget.addStyleName(STYLE_UNIT);
      wrapper.add(unitWidget);
    }

    return wrapper;
  }

  private final HtmlTable table;
  private final Button moreWidget;

  private final List<EcItem> data = Lists.newArrayList();

  private final String primaryStockLabel;
  private final String secondaryStockLabel;

  public ItemList(List<EcItem> items) {
    this();
    render(items);
  }

  private ItemList() {
    super();
    addStyleName(EcStyles.name(STYLE_PRIMARY));

    this.table = new HtmlTable();
    EcStyles.add(table, STYLE_PRIMARY, "table");
    add(table);

    this.moreWidget = new Button(Localized.getConstants().ecMoreItems());
    EcStyles.add(moreWidget, STYLE_PRIMARY, "more");
    moreWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showMoreItems();
      }
    });
    add(moreWidget);

    this.primaryStockLabel = EcKeeper.getPrimaryStockLabel();
    this.secondaryStockLabel = EcKeeper.getSecondaryStockLabel();
  }

  @Override
  public void onKeyDown(KeyDownEvent event) {
    int keyCode = event.getNativeKeyCode();

    if (BeeUtils.inList(keyCode, KeyCodes.KEY_DOWN, KeyCodes.KEY_UP,
        KeyCodes.KEY_PAGEDOWN, KeyCodes.KEY_PAGEUP, KeyCodes.KEY_END, KeyCodes.KEY_HOME)
        && table.getRowCount() > 2) {

      Integer eventRow = table.getEventRow(event, false);
      if (eventRow == null) {
        return;
      }

      boolean hasModifiers = EventUtils.hasModifierKey(event.getNativeEvent());

      int min = 1;
      int max = table.getRowCount() - 1;

      int oldRow = BeeUtils.clamp(eventRow, min, max);
      if (oldRow == max && moreWidget.isVisible()
          && BeeUtils.inList(keyCode, KeyCodes.KEY_DOWN, KeyCodes.KEY_PAGEDOWN)) {
        moreWidget.click();
        return;
      }

      int newRow = BeeConst.UNDEF;

      switch (keyCode) {
        case KeyCodes.KEY_DOWN:
          newRow = hasModifiers ? max : BeeUtils.rotateForwardInclusive(oldRow, min, max);
          break;

        case KeyCodes.KEY_UP:
          newRow = hasModifiers ? min : BeeUtils.rotateBackwardInclusive(oldRow, min, max);
          break;

        case KeyCodes.KEY_PAGEDOWN:
          newRow = max;
          if (!hasModifiers && oldRow < max - 1) {
            for (int row = oldRow + 1; row < max; row++) {
              CartAccumulator accumulator = getCartAccumulator(row);
              if (accumulator == null) {
                break;

              } else if (!DomUtils.isInView(accumulator.getInput())) {
                newRow = row;
                break;
              }
            }
          }
          break;

        case KeyCodes.KEY_PAGEUP:
          newRow = min;
          if (!hasModifiers && oldRow > min + 1) {
            for (int row = oldRow - 1; row > min; row--) {
              CartAccumulator accumulator = getCartAccumulator(row);
              if (accumulator == null) {
                break;

              } else if (!DomUtils.isInView(accumulator.getInput())) {
                newRow = row;
                break;
              }
            }
          }
          break;

        case KeyCodes.KEY_END:
          if (hasModifiers) {
            newRow = max;
          }
          break;

        case KeyCodes.KEY_HOME:
          if (hasModifiers) {
            newRow = min;
          }
          break;
      }

      if (!BeeConst.isUndef(newRow) && newRow != oldRow) {
        focusRow(newRow);
      }
    }
  }

  public void render(List<EcItem> items) {
    if (!table.isEmpty()) {
      table.clear();
    }
    StyleUtils.hideDisplay(moreWidget);

    if (!data.isEmpty()) {
      data.clear();
    }

    if (!BeeUtils.isEmpty(items)) {
      data.addAll(items);
      int row = 0;
      int col = 0;

      if (items.size() > 1) {
        Label caption = new Label(BeeUtils.joinWords(Localized.getConstants().ecFoundItems(),
            BeeUtils.bracket(items.size())));
        EcStyles.add(caption, STYLE_PRIMARY, "caption");
        table.setWidget(row, col, caption);
      }

      col += 2;

      if (hasWarehouses()) {
        if (!BeeUtils.isEmpty(primaryStockLabel)) {
          Label wrh1 = new Label(primaryStockLabel);
          EcStyles.add(wrh1, STYLE_PRIMARY, STYLE_WAREHOUSE);
          EcStyles.add(wrh1, STYLE_PRIMARY, STYLE_WAREHOUSE + "1");
          table.setWidget(row, col++, wrh1);
        }

        if (!BeeUtils.isEmpty(secondaryStockLabel)) {
          Label wrh2 = new Label(secondaryStockLabel);
          EcStyles.add(wrh2, STYLE_PRIMARY, STYLE_WAREHOUSE);
          EcStyles.add(wrh2, STYLE_PRIMARY, STYLE_WAREHOUSE + "2");
          table.setWidget(row, col++, wrh2);
        }

      } else {
        Label wrh = new Label("S1");
        EcStyles.add(wrh, STYLE_PRIMARY, STYLE_WAREHOUSE);
        EcStyles.add(wrh, STYLE_PRIMARY, STYLE_WAREHOUSE + "0");
        table.setWidget(row, col++, wrh);
      }

      Label listPriceLabel = new Label(Localized.getConstants().ecListPrice());
      listPriceLabel.addStyleName(STYLE_LIST_PRICE + STYLE_LABEL);
      EcStyles.markListPrice(listPriceLabel.getElement());
      table.setWidget(row, col++, listPriceLabel);

      Label priceLabel = new Label(Localized.getConstants().ecClientPrice());
      priceLabel.addStyleName(STYLE_PRICE + STYLE_LABEL);
      EcStyles.markPrice(priceLabel.getElement());

      table.setWidget(row, col++, priceLabel);

      table.getRowFormatter().addStyleName(row, STYLE_HEADER_ROW);

      Multimap<Long, ItemPicture> pictureWidgets = ArrayListMultimap.create();

      int pageSize = (items.size() > PAGE_SIZE * 3 / 2) ? PAGE_SIZE : items.size();

      row++;
      for (EcItem item : items) {
        if (row > pageSize) {
          break;
        }

        ItemPicture pictureWidget = new ItemPicture(item.getCaption());
        pictureWidget.setFeaturedOrNovelty(item);

        renderItem(row++, item, pictureWidget);

        pictureWidgets.put(item.getArticleId(), pictureWidget);
      }

      if (pageSize < items.size()) {
        StyleUtils.unhideDisplay(moreWidget);
      }

      if (!pictureWidgets.isEmpty()) {
        EcKeeper.setBackgroundPictures(pictureWidgets);
      }

      focusRow(1);
    }
  }

  private void focusRow(final int row) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        CartAccumulator cartAccumulator = getCartAccumulator(row);
        if (cartAccumulator != null && DomUtils.isVisible(cartAccumulator)) {
          cartAccumulator.focus();
        }
      }
    });
  }

  private CartAccumulator getCartAccumulator(int row) {
    if (row >= 0 && row < table.getRowCount()) {
      int cc = table.getCellCount(row);

      for (int col = 0; col < cc; col++) {
        Widget widget = table.getWidget(row, col);

        if (widget instanceof CartAccumulator) {
          return (CartAccumulator) widget;
        }
      }
    }
    return null;
  }

  private boolean hasWarehouses() {
    return !BeeUtils.allEmpty(primaryStockLabel, secondaryStockLabel);
  }

  private void renderItem(int row, EcItem item, Widget pictureWidget) {
    int col = 0;
    if (pictureWidget != null) {
      table.setWidgetAndStyle(row, col, pictureWidget, STYLE_PICTURE);
    }
    col++;

    Widget info = renderInfo(item);
    if (info != null) {
      table.setWidgetAndStyle(row, col, info, STYLE_INFO);
    }
    col++;

    if (hasWarehouses()) {
      if (!BeeUtils.isEmpty(primaryStockLabel)) {
        Widget stock1 = renderStock(item.getPrimaryStock(), item.getUnit());
        if (stock1 != null) {
          table.setWidgetAndStyle(row, col++, stock1, STYLE_STOCK_1);
        }
      }

      if (!BeeUtils.isEmpty(secondaryStockLabel)) {
        Widget stock2 = renderStock(item.getSecondaryStock(), item.getUnit());
        if (stock2 != null) {
          table.setWidgetAndStyle(row, col++, stock2, STYLE_STOCK_2);
        }
      }

    } else {
      Widget stock0 = renderStock(item.totalStock(), item.getUnit());
      if (stock0 != null) {
        table.setWidgetAndStyle(row, col++, stock0, STYLE_STOCK_0);
      }
    }

    int listPrice = item.getListPrice();
    int clientPrice = item.getClientPrice();
    int price = item.getPrice();

    if (listPrice > 0 && listPrice >= clientPrice) {
      Widget listPriceWidget = renderPrice(listPrice, STYLE_LIST_PRICE);
      EcStyles.markListPrice(listPriceWidget.getElement());
      table.setWidgetAndStyle(row, col, listPriceWidget, STYLE_LIST_PRICE);
    }
    col++;

    if (price > 0) {
      Widget priceWidget;
      if (clientPrice > price) {
        priceWidget = renderClientAndFeaturedPrice(clientPrice, price);
      } else {
        priceWidget = renderPrice(price, STYLE_PRICE);
      }

      EcStyles.markPrice(priceWidget.getElement());
      table.setWidgetAndStyle(row, col, priceWidget, STYLE_PRICE);
    }
    col++;

    CartAccumulator accumulator = new CartAccumulator(item, 1);
    accumulator.addKeyDownHandler(this);

    table.setWidgetAndStyle(row, col++, accumulator, STYLE_QUANTITY);

    table.getRowFormatter().addStyleName(row, STYLE_ITEM_ROW);
  }

  private void showMoreItems() {
    int pageStart = table.getRowCount() - 1;
    int more = data.size() - pageStart;

    if (more > 0) {
      Multimap<Long, ItemPicture> pictureWidgets = ArrayListMultimap.create();

      int pageSize = (more > PAGE_SIZE * 3 / 2) ? PAGE_SIZE : more;

      for (int i = pageStart; i < pageStart + pageSize; i++) {
        EcItem item = data.get(i);

        ItemPicture pictureWidget = new ItemPicture(item.getCaption());
        pictureWidget.setFeaturedOrNovelty(item);

        renderItem(i + 1, item, pictureWidget);

        pictureWidgets.put(item.getArticleId(), pictureWidget);
      }

      if (pageSize >= more) {
        StyleUtils.hideDisplay(moreWidget);
      }

      if (!pictureWidgets.isEmpty()) {
        EcKeeper.setBackgroundPictures(pictureWidgets);
      }

      focusRow(pageStart + 1);
    }
  }
}
