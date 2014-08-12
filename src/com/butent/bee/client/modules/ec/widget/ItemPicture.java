package com.butent.bee.client.modules.ec.widget;

import com.google.common.collect.ImmutableList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.SlideDeck;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

public class ItemPicture extends Flow {

  private static final BeeLogger logger = LogUtils.getLogger(ItemPicture.class);

  private static final String STYLE_PREFIX = EcStyles.name("ItemPicture-");

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_HAS_PICTURE = STYLE_PREFIX + "has-picture";
  private static final String STYLE_WRAPPER = STYLE_PREFIX + "wrapper";

  private static final String STYLE_MORE = STYLE_PREFIX + "more";
  private static final String STYLE_MORE_PLAY = STYLE_PREFIX + "more_play";
  private static final String STYLE_MORE_COUNT = STYLE_PREFIX + "more-count";

  private static final String STYLE_SLIDES = STYLE_PREFIX + "slides";
  private static final String STYLE_OPEN = STYLE_PREFIX + "open";

  private static final String STYLE_FEATURED = STYLE_PREFIX + "featured";
  private static final String STYLE_NOVELTY = STYLE_PREFIX + "novelty";

  private final String itemCaption;

  public ItemPicture(String itemCaption) {
    super(STYLE_CONTAINER);
    this.itemCaption = itemCaption;
  }

  @Override
  public String getIdPrefix() {
    return "picture";
  }

  public String getItemCaption() {
    return itemCaption;
  }

  public void setFeaturedOrNovelty(EcItem item) {
    if (item != null) {
      if (item.isFeatured()) {
        CustomDiv banner = new CustomDiv(STYLE_FEATURED);
        banner.setText(Localized.getConstants().ecFeaturedBanner());
        add(banner);

      } else if (item.isNovelty()) {
        CustomDiv banner = new CustomDiv(STYLE_NOVELTY);
        banner.setText(Localized.getConstants().ecNoveltyBanner());
        add(banner);
      }
    }
  }

  public void setPictures(final ImmutableList<String> pictures) {
    if (!BeeUtils.isEmpty(pictures)) {
      if (getElement().hasClassName(STYLE_HAS_PICTURE)) {
        logger.warning(NameUtils.getName(this), "already contains picture");
        return;
      }
      addStyleName(STYLE_HAS_PICTURE);

      CustomDiv wrapper = new CustomDiv(STYLE_WRAPPER);
      StyleUtils.setBackgroundImage(wrapper, pictures.get(0));
      add(wrapper);

      if (pictures.size() > 1) {
        Flow more = new Flow(STYLE_MORE);

        FaLabel play = new FaLabel(FontAwesome.PLAY, true);
        play.addStyleName(STYLE_MORE_PLAY);
        more.add(play);

        InlineLabel count = new InlineLabel(BeeUtils.toString(pictures.size() - 1));
        count.addStyleName(STYLE_MORE_COUNT);
        more.add(count);

        more.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            event.stopPropagation();

            SlideDeck.create(pictures, new Callback<SlideDeck>() {
              @Override
              public void onSuccess(SlideDeck result) {
                DialogBox dialog = DialogBox.create(getItemCaption(), STYLE_SLIDES);
                StyleUtils.setWidth(dialog.getHeader(), result.getWidth());

                dialog.setWidget(result);

                dialog.setAnimationEnabled(true);
                dialog.setHideOnEscape(true);

                dialog.center();
                result.handleKeyboard();
              }
            });
          }
        });

        add(more);
      }

      addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Image image = new Image(pictures.get(0));
          image.addStyleName(STYLE_OPEN);

          Global.showModalWidget(image, getElement());
        }
      });
    }
  }
}
