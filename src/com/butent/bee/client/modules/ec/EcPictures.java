package com.butent.bee.client.modules.ec;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.modules.ec.widget.ItemPicture;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.List;
import java.util.Set;

class EcPictures {

  private static final BeeLogger logger = LogUtils.getLogger(EcPictures.class);

  private static boolean hasPictures(ResponseObject response) {
    return response != null && !response.hasErrors() && response.hasResponse()
        && !response.hasResponse(String.class);
  }

  private static boolean isPicture(String picture) {
    return picture != null;
  }

  private static void setPictures(Collection<ItemPicture> widgets, List<String> pictures) {
    if (widgets != null && !BeeUtils.isEmpty(pictures)) {
      for (ItemPicture widget : widgets) {
        if (widget != null) {
          widget.setPictures(ImmutableList.copyOf(pictures));
        }
      }
    }
  }

  private final Cache<Long, ImmutableList<String>> cache =
      CacheBuilder.newBuilder().maximumSize(2000).build();
  private final Set<Long> noPicture = Sets.newHashSet();

  private BeeRowSet banners;

  EcPictures() {
  }

  void addCellHandlers(AbstractCell<?> cell, final String primaryStyle) {
    cell.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onCellEvent(event, primaryStyle);
      }
    });

    cell.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          onCellEvent(event, primaryStyle);
        }
      }
    });
  }

  BeeRowSet getBanners() {
    return banners;
  }

  List<RowInfo> getCachedBannerInfo() {
    List<RowInfo> result = Lists.newArrayList();

    if (!DataUtils.isEmpty(getBanners())) {
      for (BeeRow row : getBanners().getRows()) {
        result.add(new RowInfo(row, false));
      }
    }

    return result;
  }

  void onCellEvent(DomEvent<?> event, String primaryStyle) {
    Element cellElement = event.getRelativeElement();
    if (cellElement == null) {
      return;
    }

    ImageElement imageElement = DomUtils.getImageElement(cellElement);
    if (imageElement == null) {
      return;
    }

    if (event.getSource() instanceof AbstractCell) {
      ((AbstractCell<?>) event.getSource()).setEventCanceled(true);
    }

    Image image = new Image(imageElement.getSrc());
    if (!BeeUtils.isEmpty(primaryStyle)) {
      EcStyles.add(image, primaryStyle, "picture");
    }

    Global.showModalWidget(image, cellElement);
  }

  void setBackground(final Multimap<Long, ItemPicture> articleWidgets) {
    final Set<Long> articles = Sets.newHashSet();

    for (Long article : articleWidgets.keySet()) {
      if (noPicture.contains(article)) {
        continue;
      }

      List<String> pictures = cache.getIfPresent(article);
      if (BeeUtils.isEmpty(pictures)) {
        articles.add(article);
      } else {
        setPictures(articleWidgets.get(article), pictures);
      }
    }

    if (!articles.isEmpty()) {
      ParameterList params = EcKeeper.createArgs(EcConstants.SVC_GET_PICTURES);
      params.addDataItem(EcConstants.COL_TCD_ARTICLE, DataUtils.buildIdList(articles));

      BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);

          if (hasPictures(response)) {
            String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

            if (arr != null) {
              Long lastArticle = null;
              List<String> pictures = Lists.newArrayList();

              for (int i = 0; i < arr.length - 1; i += 2) {
                Long article = BeeUtils.toLongOrNull(arr[i]);
                String picture = arr[i + 1];

                if (article != null && isPicture(picture)) {
                  if (lastArticle == null) {
                    lastArticle = article;
                    pictures.add(picture);

                  } else if (article.equals(lastArticle)) {
                    pictures.add(picture);

                  } else {
                    if (articleWidgets.containsKey(lastArticle)) {
                      setPictures(articleWidgets.get(lastArticle), pictures);
                    }

                    cache.put(lastArticle, ImmutableList.copyOf(pictures));
                    articles.remove(lastArticle);

                    lastArticle = article;
                    pictures.clear();
                    pictures.add(picture);
                  }
                }
              }

              if (lastArticle != null && !pictures.isEmpty()) {
                if (articleWidgets.containsKey(lastArticle)) {
                  setPictures(articleWidgets.get(lastArticle), pictures);
                }

                cache.put(lastArticle, ImmutableList.copyOf(pictures));
                articles.remove(lastArticle);
              }

              if (EcKeeper.isDebug()) {
                logger.debug("picture cache", cache.size());
              }
            }
          }

          if (!articles.isEmpty() && !response.hasErrors()) {
            noPicture.addAll(articles);
            if (EcKeeper.isDebug()) {
              logger.debug("no picture", articles, noPicture.size());
            }
          }
        }
      });
    }
  }

  void setBanners(BeeRowSet banners) {
    this.banners = banners;
  }
}
