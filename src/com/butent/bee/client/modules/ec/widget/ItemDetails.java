package com.butent.bee.client.modules.ec.widget;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.modules.ec.EcWidgetFactory;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.HtmlList;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.ArticleCriteria;
import com.butent.bee.shared.modules.ec.ArticleSupplier;
import com.butent.bee.shared.modules.ec.EcCarType;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.modules.ec.EcItemInfo;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class ItemDetails extends Flow {

  public static final String STYLE_PRIMARY = "ItemDetails";

  private static final String STYLE_CONTAINER = "container";
  private static final String STYLE_WRAPPER = "wrapper";
  private static final String STYLE_TABLE = "table";
  private static final String STYLE_LABEL = "label";
  private static final String STYLE_VALUE = "value";
  private static final String STYLE_CURRENCY = "currency";

  private static Widget renderAddToCart(EcItem item) {
    String stylePrefix = EcStyles.name(STYLE_PRIMARY, "addToCart-");
    Flow container = new Flow(stylePrefix + STYLE_CONTAINER);

    CartAccumulator accumulator = new CartAccumulator(item, 1);
    accumulator.addStyleName(stylePrefix + "cart");

    container.add(accumulator);

    HtmlTable table = new HtmlTable(stylePrefix + "price-table");
    int row = 0;

    int colLabel = 0;
    int colValue = 1;
    int colCurrency = 2;

    int listPrice = item.getListPrice();
    int clientPrice = item.getClientPrice();
    int price = item.getPrice();

    String pfx;

    if (listPrice > 0 && listPrice >= clientPrice) {
      pfx = stylePrefix + "list-price-";

      table.setText(row, colLabel, Localized.getConstants().ecListPrice(), pfx + STYLE_LABEL);
      table.setText(row, colValue, EcUtils.formatCents(listPrice), pfx + STYLE_VALUE);
      table.setText(row, colCurrency, EcConstants.CURRENCY, pfx + STYLE_CURRENCY);

      EcStyles.markListPrice(table.getRow(row));
      row++;
    }

    if (price > 0) {
      if (clientPrice > price) {
        pfx = stylePrefix + "client-price-";

        table.setText(row, colLabel, Localized.getConstants().ecClientPrice(), pfx + STYLE_LABEL);
        table.setText(row, colValue, EcUtils.formatCents(clientPrice), pfx + STYLE_VALUE);
        table.setText(row, colCurrency, EcConstants.CURRENCY, pfx + STYLE_CURRENCY);

        EcStyles.markPrice(table.getRow(row));
        row++;

        pfx = stylePrefix + "featured-price-";

        table.setText(row, colValue, EcUtils.formatCents(price), pfx + STYLE_VALUE);
        table.setText(row, colCurrency, EcConstants.CURRENCY, pfx + STYLE_CURRENCY);

        EcStyles.markPrice(table.getRow(row));
        row++;

      } else {
        pfx = stylePrefix + "price-";

        table.setText(row, colLabel, Localized.getConstants().ecClientPrice(), pfx + STYLE_LABEL);
        table.setText(row, colValue, EcUtils.formatCents(price), pfx + STYLE_VALUE);
        table.setText(row, colCurrency, EcConstants.CURRENCY, pfx + STYLE_CURRENCY);

        EcStyles.markPrice(table.getRow(row));
        row++;
      }
    }

    container.add(table);

    return container;
  }

  private static Widget renderAnalogs(EcItem item) {
    if (item.getAnalogCount() <= 0) {
      return null;
    }

    ParameterList params = EcKeeper.createArgs(EcConstants.SVC_GET_ITEM_ANALOGS);
    params.addDataItem(EcConstants.COL_TCD_ARTICLE, item.getArticleId());
    params.addDataItem(EcConstants.COL_TCD_ARTICLE_NR, item.getCode());
    params.addDataItem(EcConstants.COL_TCD_BRAND, item.getBrand());

    final Flow container = new Flow(EcStyles.name(STYLE_PRIMARY, "analogs"));

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        EcKeeper.dispatchMessages(response);

        if (!response.hasErrors()) {
          final List<EcItem> items = EcKeeper.getResponseItems(response);

          if (!BeeUtils.isEmpty(items)) {
            EcKeeper.ensureClientStockLabels(new Consumer<Boolean>() {
              @Override
              public void accept(Boolean input) {
                ItemList itemList = new ItemList(items);
                container.clear();
                container.add(itemList);
              }
            });
          }
        }
      }
    });

    return container;
  }

  private static Widget renderCarTypes(EcItemInfo info) {
    if (info == null || BeeUtils.isEmpty(info.getCarTypes())) {
      return null;
    }

    String stylePrefix = EcStyles.name(STYLE_PRIMARY, "carTypes-");
    Flow container = new Flow(stylePrefix + STYLE_CONTAINER);

    Flow wrapper = new Flow(stylePrefix + STYLE_WRAPPER);

    String styleCarInfo = stylePrefix + "info";

    for (EcCarType ect : info.getCarTypes()) {
      Label infoLabel = new Label(ect.getInfo());
      infoLabel.addStyleName(styleCarInfo);

      wrapper.add(infoLabel);
    }

    container.add(wrapper);

    return container;
  }

  private static Widget renderInfo(EcItem item, EcItemInfo info) {
    String stylePrefix = EcStyles.name(STYLE_PRIMARY, "info-");
    Flow container = new Flow(stylePrefix + STYLE_CONTAINER);

    if (item != null) {
      Widget itemCodeWidget = EcWidgetFactory.renderField(Localized.getConstants().ecItemCode(),
          item.getCode(), stylePrefix + "itemCode");
      if (itemCodeWidget != null) {
        container.add(itemCodeWidget);
        itemCodeWidget.setTitle(BeeUtils.joinWords("ArticleID:", item.getArticleId()));
      }

      if (item.getBrand() != null) {
        Widget brandWidget = EcWidgetFactory.renderField(Localized.getConstants().ecItemBrand(),
            EcKeeper.getBrandName(item.getBrand()), stylePrefix + "brand");
        if (brandWidget != null) {
          container.add(brandWidget);
        }
      }

      if (!BeeUtils.isEmpty(item.getDescription())) {
        CustomDiv descriptionWidget = new CustomDiv(stylePrefix + "description");
        descriptionWidget.setHtml(item.getDescription());
        container.add(descriptionWidget);
      }
    }

    if (info != null && !BeeUtils.isEmpty(info.getCriteria())) {
      String styleCriteria = stylePrefix + "criteria-";
      HtmlTable table = new HtmlTable(styleCriteria + STYLE_TABLE);

      int row = 0;
      for (ArticleCriteria ac : info.getCriteria()) {
        Label nameWidget = new Label(ac.getName());
        table.setWidgetAndStyle(row, 0, nameWidget, styleCriteria + "name");

        if (!BeeUtils.isEmpty(ac.getValue())) {
          Label valueWidget = new Label(ac.getValue());
          table.setWidgetAndStyle(row, 1, valueWidget, styleCriteria + "value");
        }

        row++;
      }

      Simple wrapper = new Simple(table);
      wrapper.addStyleName(styleCriteria + STYLE_WRAPPER);

      container.add(wrapper);
    }

    return container;
  }

  private static Widget renderOeNumbers(EcItemInfo info) {
    if (info == null || BeeUtils.isEmpty(info.getOeNumbers())) {
      return null;
    }

    String stylePrefix = EcStyles.name(STYLE_PRIMARY, "oeNumbers-");

    HtmlList list = new HtmlList(true);
    list.addStyleName(stylePrefix + "list");

    list.addItems(info.getOeNumbers());

    Flow container = new Flow(stylePrefix + STYLE_CONTAINER);
    container.add(list);

    return container;
  }

  private static Widget renderPicture(EcItem item, int width, int height) {
    ItemPicture widget = new ItemPicture(item.getCaption());
    EcStyles.add(widget, STYLE_PRIMARY, "picture");
    StyleUtils.setSize(widget, width, height);

    EcKeeper.setBackgroundPicture(item.getArticleId(), widget);
    widget.setFeaturedOrNovelty(item);

    return widget;
  }

  private static Widget renderRemainders(final EcItem item) {
    if (item == null || BeeUtils.isEmpty(item.getSuppliers())) {
      return null;
    }

    boolean hasRemainders = false;
    for (ArticleSupplier as : item.getSuppliers()) {
      if (!as.getRemainders().isEmpty()) {
        hasRemainders = true;
        break;
      }
    }
    if (!hasRemainders) {
      return null;
    }

    final String stylePrefix = EcStyles.name(STYLE_PRIMARY, "remainders-");
    Flow container = new Flow(stylePrefix + STYLE_CONTAINER);

    final HtmlTable table = new HtmlTable(stylePrefix + STYLE_TABLE);

    EcKeeper.ensureWarehouses(new Consumer<Boolean>() {
      @Override
      public void accept(Boolean input) {
        int row = 0;
        for (ArticleSupplier as : item.getSuppliers()) {
          for (String warehouse : as.getRemainders().keySet()) {
            Label warehouseWidget = new Label(EcKeeper.getWarehouseLabel(warehouse));
            table.setWidgetAndStyle(row, 0, warehouseWidget, stylePrefix + "warehouse");

            double remainder = BeeUtils.toDouble(as.getRemainders().get(warehouse));
            Widget stockWidget = EcWidgetFactory.createStockWidget(remainder);
            table.setWidgetAndStyle(row, 1, stockWidget, stylePrefix + "stock");

            row++;
          }
        }
      }
    });

    Simple wrapper = new Simple(table);
    wrapper.addStyleName(stylePrefix + STYLE_WRAPPER);

    container.add(wrapper);

    return container;
  }

  private static Widget renderSuppliers(EcItem item) {
    if (item == null || BeeUtils.isEmpty(item.getSuppliers())) {
      return null;
    }

    String stylePrefix = EcStyles.name(STYLE_PRIMARY, "suppliers-");
    Flow container = new Flow(stylePrefix + STYLE_CONTAINER);

    HtmlTable table = new HtmlTable(stylePrefix + STYLE_TABLE);
    table.setDefaultCellClasses(StyleUtils.className(FontWeight.BOLDER));

    int row = 0;
    int col = 0;

    table.setHtml(row, col++, Localized.getConstants().ecItemSupplier());
    table.setHtml(row, col++, Localized.getConstants().ecItemSupplierCode());

    table.setColumnCellClasses(col, StyleUtils.className(TextAlign.RIGHT));
    table.setHtml(row, col++, Localized.getConstants().ecItemCost());

    table.setColumnCellClasses(col, StyleUtils.className(TextAlign.RIGHT));
    table.setHtml(row, col++, Localized.getConstants().ecListPriceShort());

    table.setColumnCellClasses(col, StyleUtils.className(TextAlign.RIGHT));
    table.setHtml(row, col++, Localized.getConstants().ecItemPrice());

    row++;
    table.setDefaultCellClasses(StyleUtils.className(FontWeight.NORMAL));

    for (ArticleSupplier as : item.getSuppliers()) {
      col = 0;

      table.setHtml(row, col++, as.getSupplier().name());
      table.setHtml(row, col++, as.getSupplierId());

      table.setHtml(row, col++, EcUtils.formatCents(as.getCost()));
      table.setHtml(row, col++, EcUtils.formatCents(as.getListPrice()));
      table.setHtml(row, col++, EcUtils.formatCents(as.getPrice()));

      row++;
    }

    Simple wrapper = new Simple(table);
    wrapper.addStyleName(stylePrefix + STYLE_WRAPPER);

    container.add(wrapper);

    return container;
  }

  private static Widget renderTabbedPages(EcItem item, EcItemInfo info) {
    if (item == null || info == null) {
      return null;
    }

    TabbedPages widget = new TabbedPages();
    String summary;

    Widget remainders = renderRemainders(item);
    if (remainders != null) {
      int count = 0;
      for (ArticleSupplier as : item.getSuppliers()) {
        count += as.getRemainders().size();
      }

      summary = BeeUtils.toString(count);
      widget.add(remainders, Localized.getConstants().ecItemDetailsRemainders(), summary, null);
    }

    Widget oeNumbers = renderOeNumbers(info);
    if (oeNumbers != null) {
      summary = BeeUtils.toString(info.getOeNumbers().size());
      widget.add(oeNumbers, Localized.getConstants().ecItemDetailsOeNumbers(), summary, null);
    }

    if (EcKeeper.showItemSuppliers()) {
      Widget suppliers = renderSuppliers(item);
      if (suppliers != null) {
        summary = BeeUtils.toString(item.getSuppliers().size());
        widget.add(suppliers, Localized.getConstants().ecItemDetailsSuppliers(), summary, null);
      }
    }

    Widget carTypes = renderCarTypes(info);
    if (carTypes != null) {
      summary = BeeUtils.toString(info.getCarTypes().size());
      widget.add(carTypes, Localized.getConstants().ecItemDetailsCarTypes(), summary, null);
    }

    Widget analogs = renderAnalogs(item);
    if (analogs != null) {
      summary = BeeUtils.toString(item.getAnalogCount());
      widget.add(analogs, Localized.getConstants().ecItemAnalogs(), summary, null);
    }

    return widget;
  }

  public ItemDetails(EcItem item, EcItemInfo info, boolean allowAddToCart) {
    super(EcStyles.name(STYLE_PRIMARY, "panel"));

    int screenWidth = BeeKeeper.getScreen().getWidth();
    int screenHeight = BeeKeeper.getScreen().getHeight();
    if (screenWidth < 100 || screenHeight < 100) {
      return;
    }

    int width = BeeUtils.resize(screenWidth, 100, 1600, 100, 1200);
    int height = BeeUtils.resize(screenHeight, 100, 1600, 100, 1200);
    StyleUtils.setSize(this, width, height);

    int widthMargin = BeeUtils.resize(width, 0, 1000, 0, 20);
    int heightMargin = BeeUtils.resize(height, 0, 1000, 0, 20);

    int rowHeight = (height - heightMargin) / 2;
    int pictureWidth = Math.min(300, width / 3);

    Widget picture = renderPicture(item, pictureWidth, rowHeight);
    if (picture != null) {
      add(picture);
    }

    Widget criteria = renderInfo(item, info);
    if (criteria != null) {
      StyleUtils.makeAbsolute(criteria);
      StyleUtils.setLeft(criteria, pictureWidth + widthMargin);
      StyleUtils.setTop(criteria, 0);
      StyleUtils.setHeight(criteria, rowHeight);

      add(criteria);
    }

    if (allowAddToCart) {
      Widget addToCart = renderAddToCart(item);
      if (addToCart != null) {
        StyleUtils.makeAbsolute(addToCart);
        StyleUtils.setRight(addToCart, 0);
        StyleUtils.setTop(addToCart, heightMargin);

        add(addToCart);
      }
    }

    Widget itemDataTabs = renderTabbedPages(item, info);

    if (itemDataTabs != null) {
      StyleUtils.setTop(itemDataTabs, rowHeight + heightMargin);
      StyleUtils.setHeight(itemDataTabs, rowHeight);

      add(itemDataTabs);
    }
  }
}
