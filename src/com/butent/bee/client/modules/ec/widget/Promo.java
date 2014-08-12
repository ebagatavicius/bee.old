package com.butent.bee.client.modules.ec.widget;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.utils.BrowsingContext;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class Promo extends Flow {

  private static final String STYLE_BANNER = "Banner";
  private static final String STYLE_FEATURED = "Featured";
  private static final String STYLE_NOVELTY = "Novelty";

  private static final String STYLE_SUFFIX_LABEL = "label";
  private static final String STYLE_SUFFIX_CONTAINER = "container";
  private static final String STYLE_SUFFIX_TABLE = "table";
  private static final String STYLE_SUFFIX_ITEM = "item";

  private static final String STYLE_SUFFIX_BANNER = "banner";
  private static final String STYLE_SUFFIX_PICTURE = "picture";
  private static final String STYLE_SUFFIX_NAME = "name";
  private static final String STYLE_SUFFIX_CODE = "code";
  private static final String STYLE_SUFFIX_PRICE = "price";
  private static final String STYLE_SUFFIX_DRILL = "drill";

  private static final String STYLE_SUFFIX_LINK = "link";
  private static final String STYLE_SUFFIX_NO_LINK = "noLink";

  private static Widget renderBanner(String picture, Integer width, Integer height,
      final String link) {

    Image widget = new Image(picture);
    EcStyles.add(widget, STYLE_BANNER, STYLE_SUFFIX_PICTURE);

    if (BeeUtils.isPositive(width)) {
      StyleUtils.setWidth(widget, width);
    }
    if (BeeUtils.isPositive(height)) {
      StyleUtils.setHeight(widget, height);
    }

    if (BeeUtils.isEmpty(link)) {
      EcStyles.add(widget, STYLE_BANNER, STYLE_SUFFIX_NO_LINK);

    } else {
      widget.setTitle(link);
      EcStyles.add(widget, STYLE_BANNER, STYLE_SUFFIX_LINK);

      widget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          BrowsingContext.open(link);
        }
      });
    }

    return widget;
  }

  private static Widget renderItem(final EcItem item, String primaryStyle, String bannerText,
      Widget pictureWidget) {

    Flow panel = new Flow(EcStyles.name(primaryStyle, STYLE_SUFFIX_ITEM));

    if (!BeeUtils.isEmpty(bannerText)) {
      Label banner = new Label(bannerText);
      EcStyles.add(banner, primaryStyle, STYLE_SUFFIX_BANNER);
      panel.add(banner);
    }

    if (pictureWidget != null) {
      EcStyles.add(pictureWidget, primaryStyle, STYLE_SUFFIX_PICTURE);
      panel.add(pictureWidget);
    }

    String name = item.getName();
    if (!BeeUtils.isEmpty(name)) {
      Label itemName = new Label(name);
      EcStyles.add(itemName, primaryStyle, STYLE_SUFFIX_NAME);
      panel.add(itemName);
    }

    String code = item.getCode();
    if (!BeeUtils.isEmpty(code)) {
      Label itemCode = new Label(code);
      EcStyles.add(itemCode, primaryStyle, STYLE_SUFFIX_CODE);
      panel.add(itemCode);
    }

    int price = item.getPrice();
    if (price > 0) {
      String priceInfo = BeeUtils.joinWords(Localized.getConstants().ecItemPrice()
          + BeeConst.STRING_COLON, EcUtils.formatCents(price), EcConstants.CURRENCY);
      Label itemPrice = new Label(priceInfo);
      EcStyles.add(itemPrice, primaryStyle, STYLE_SUFFIX_PRICE);
      panel.add(itemPrice);
    }

    Button details = new Button(Localized.getConstants().ecShowDetails());
    EcStyles.add(details, primaryStyle, STYLE_SUFFIX_DRILL);
    details.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        EcKeeper.openItem(item, true);
      }
    });

    panel.add(details);

    return panel;
  }

  public Promo(BeeRowSet banners, List<EcItem> items) {
    super(EcStyles.name("Promo"));

    if (!DataUtils.isEmpty(banners)) {
      Flow bannerContainer = new Flow(EcStyles.name(STYLE_BANNER, STYLE_SUFFIX_CONTAINER));

      int pictureIndex = banners.getColumnIndex(EcConstants.COL_BANNER_PICTURE);
      int widthIndex = banners.getColumnIndex(EcConstants.COL_BANNER_WIDTH);
      int heightIndex = banners.getColumnIndex(EcConstants.COL_BANNER_HEIGHT);
      int linkIndex = banners.getColumnIndex(EcConstants.COL_BANNER_LINK);

      for (BeeRow banner : banners.getRows()) {
        bannerContainer.add(renderBanner(banner.getString(pictureIndex),
            banner.getInteger(widthIndex), banner.getInteger(heightIndex),
            banner.getString(linkIndex)));
      }

      add(bannerContainer);
    }

    Horizontal featuredTable = null;
    Horizontal noveltyTable = null;

    Multimap<Long, ItemPicture> pictureWidgets = ArrayListMultimap.create();

    for (EcItem item : items) {
      ItemPicture pictureWidget = new ItemPicture(item.getCaption());
      pictureWidgets.put(item.getArticleId(), pictureWidget);

      if (item.isFeatured()) {
        if (featuredTable == null) {
          Label featuredLabel = new Label(Localized.getConstants().ecFeaturedItems());
          EcStyles.add(featuredLabel, STYLE_FEATURED, STYLE_SUFFIX_LABEL);
          add(featuredLabel);

          Flow featuredContainer = new Flow();
          EcStyles.add(featuredContainer, STYLE_FEATURED, STYLE_SUFFIX_CONTAINER);

          featuredTable = new Horizontal();
          EcStyles.add(featuredTable, STYLE_FEATURED, STYLE_SUFFIX_TABLE);

          featuredContainer.add(featuredTable);
          add(featuredContainer);
        }

        featuredTable.add(renderItem(item, STYLE_FEATURED,
            Localized.getConstants().ecFeaturedBanner(), pictureWidget));

      } else {
        if (noveltyTable == null) {
          Label noveltyLabel = new Label(Localized.getConstants().ecNoveltyItems());
          EcStyles.add(noveltyLabel, STYLE_NOVELTY, STYLE_SUFFIX_LABEL);
          add(noveltyLabel);

          Flow noveltyContainer = new Flow();
          EcStyles.add(noveltyContainer, STYLE_NOVELTY, STYLE_SUFFIX_CONTAINER);

          noveltyTable = new Horizontal();
          EcStyles.add(noveltyTable, STYLE_NOVELTY, STYLE_SUFFIX_TABLE);

          noveltyContainer.add(noveltyTable);
          add(noveltyContainer);
        }

        noveltyTable.add(renderItem(item, STYLE_NOVELTY,
            Localized.getConstants().ecNoveltyBanner(), pictureWidget));
      }
    }

    if (!pictureWidgets.isEmpty()) {
      EcKeeper.setBackgroundPictures(pictureWidgets);
    }
  }
}
