package com.butent.bee.client.modules.ec;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasKeyPressHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Panel;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Settings;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.Thermometer;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.modules.ec.render.CategoryFullNameRenderer;
import com.butent.bee.client.modules.ec.view.EcView;
import com.butent.bee.client.modules.ec.view.ShoppingCart;
import com.butent.bee.client.modules.ec.widget.CartList;
import com.butent.bee.client.modules.ec.widget.ItemDetails;
import com.butent.bee.client.modules.ec.widget.ItemPanel;
import com.butent.bee.client.modules.ec.widget.ItemPicture;
import com.butent.bee.client.modules.ec.widget.Promo;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.tree.Tree;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.HtmlEditor;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewSupplier;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.ec.Cart;
import com.butent.bee.shared.modules.ec.CartItem;
import com.butent.bee.shared.modules.ec.DeliveryMethod;
import com.butent.bee.shared.modules.ec.EcBrand;
import com.butent.bee.shared.modules.ec.EcCarModel;
import com.butent.bee.shared.modules.ec.EcCarType;
import com.butent.bee.shared.modules.ec.EcConstants.CartType;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.modules.ec.EcItemInfo;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EcKeeper {

  private static final BeeLogger logger = LogUtils.getLogger(EcKeeper.class);

  private static final EcData data = new EcData();
  private static final EcPictures pictures = new EcPictures();

  private static final InputText searchBox = new InputText();

  private static final CartList cartList = new CartList();

  private static final EcEventHandler eventHandler = new EcEventHandler();

  private static final String KEY_EC_CONTACTS = MenuService.EDIT_EC_CONTACTS.name().toLowerCase();
  private static final String KEY_TERMS_OF_DELIVERY =
      MenuService.EDIT_TERMS_OF_DELIVERY.name().toLowerCase();

  private static EcCommandWidget activeCommand;

  private static boolean debug;

  private static Set<EcRequest> pendingRequests = new HashSet<>();

  private static boolean listPriceVisible = true;
  private static boolean priceVisible = true;

  private static boolean stockLimited = true;

  private static boolean clientStyleSheetInjected;

  public static void addPictureCellHandlers(AbstractCell<?> cell, String primaryStyle) {
    Assert.notNull(cell);
    pictures.addCellHandlers(cell, primaryStyle);
  }

  public static void addToCart(EcItem ecItem, int quantity) {
    cartList.addToCart(ecItem, quantity);
  }

  public static HandlerRegistration bindKeyPress(HasKeyPressHandlers source) {
    if (eventHandler.getEnabled() == null) {
      eventHandler.setEnabled(false);

      List<String> keys = Lists.newArrayList(COL_CLIENT_TOGGLE_LIST_PRICE, COL_CLIENT_TOGGLE_PRICE,
          COL_CLIENT_TOGGLE_STOCK_LIMIT);

      getClientValues(keys, new Consumer<List<String>>() {
        @Override
        public void accept(List<String> input) {
          eventHandler.setListPriceEnabled(BeeConst.isTrue(BeeUtils.getQuietly(input, 0)));
          eventHandler.setPriceEnabled(BeeConst.isTrue(BeeUtils.getQuietly(input, 1)));
          eventHandler.setStockLimitEnabled(BeeConst.isTrue(BeeUtils.getQuietly(input, 2)));

          eventHandler.setEnabled(eventHandler.isListPriceEnabled()
              || eventHandler.isPriceEnabled()
              || eventHandler.isStockLimitEnabled());
        }
      });
    }

    return source.addKeyPressHandler(eventHandler);
  }

  public static Tree buildCategoryTree(Set<Long> categoryIds) {
    Assert.notEmpty(categoryIds);
    return data.buildCategoryTree(categoryIds);
  }

  public static boolean checkResponse(EcRequest request, ResponseObject response) {
    boolean pending = pendingRequests.contains(request);
    if (isDebug()) {
      logger.debug(request.getService(), request.getLabel());
      logger.debug("response received", request.elapsedMillis(), pending);
    }

    if (pending) {
      dispatchMessages(response);
    }
    return pending && response.hasResponse();
  }

  public static void closeView(IdentifiableWidget view) {
    BeeKeeper.getScreen().closeWidget(view);
    showPromo(true);
  }

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.ECOMMERCE, method);
  }

  public static void dispatchMessages(ResponseObject response) {
    if (response != null) {
      response.notify(BeeKeeper.getScreen());
    }
  }

  public static void doGlobalSearch(String query, final IdentifiableWidget inputWidget) {
    if (!checkSearchQuery(query)) {
      return;
    }

    ParameterList params = createArgs(SVC_GLOBAL_SEARCH);
    params.addDataItem(VAR_QUERY, query);

    requestItems(SVC_GLOBAL_SEARCH, query, params, new Consumer<List<EcItem>>() {
      @Override
      public void accept(List<EcItem> items) {
        AutocompleteProvider.retainValue(inputWidget);
        resetActiveCommand();

        ItemPanel widget = new ItemPanel();
        BeeKeeper.getScreen().show(widget);
        renderItems(widget, items);
      }
    });
  }

  public static void ensureBrands(Consumer<Boolean> callback) {
    Assert.notNull(callback);
    data.ensureBrands(callback);
  }

  public static void ensureCategories(Consumer<Boolean> callback) {
    Assert.notNull(callback);
    data.ensureCategories(callback);
  }

  public static void ensureCategoriesAndBrandsAndStockLabels(Consumer<Boolean> callback) {
    Assert.notNull(callback);
    data.ensureCategoriesAndBrandsAndStockLabels(callback);
  }

  public static void ensureClientStockLabels(Consumer<Boolean> callback) {
    Assert.notNull(callback);
    data.ensureClientStockLabels(callback);
  }

  public static void ensureWarehouses(Consumer<Boolean> callback) {
    Assert.notNull(callback);
    data.ensureWarehouses(callback);
  }

  public static void finalizeRequest(EcRequest request, boolean remove) {
    if (request.hasProgress()) {
      BeeKeeper.getScreen().removeProgress(request.getProgressId());
      request.setProgressId(null);
    }

    if (remove) {
      pendingRequests.remove(request);
    }
  }

  public static String formatStock(double stock) {
    if (stock <= 0) {
      return Localized.getConstants().ecStockAsk();
    } else if (isStockLimited() && MAX_VISIBLE_STOCK > 0 && stock > MAX_VISIBLE_STOCK) {
      return BeeConst.STRING_GT + MAX_VISIBLE_STOCK;
    } else {
      return BeeUtils.toString(stock, 3);
    }
  }

  public static String getBrandName(Long brand) {
    Assert.notNull(brand);
    return data.getBrandName(brand);
  }

  public static void getCarManufacturers(Consumer<List<String>> callback) {
    Assert.notNull(callback);
    data.getCarManufacturers(callback);
  }

  public static void getCarModels(String manufacturer, Consumer<List<EcCarModel>> callback) {
    Assert.notEmpty(manufacturer);
    Assert.notNull(callback);
    data.getCarModels(manufacturer, callback);
  }

  public static Cart getCart(CartType cartType) {
    return cartList.getCart(cartType);
  }

  public static void getCarTypes(Long modelId, Consumer<List<EcCarType>> callback) {
    Assert.notNull(modelId);
    Assert.notNull(callback);
    data.getCarTypes(modelId, callback);
  }

  public static String getCategoryFullName(Long categoryId, String separator) {
    Assert.notNull(categoryId);
    return data.getCategoryFullName(categoryId, separator);
  }

  public static String getCategoryName(Long categoryId) {
    Assert.notNull(categoryId);
    return data.getCategoryName(categoryId);
  }

  public static List<String> getCategoryNames(EcItem item) {
    Assert.notNull(item);
    return data.getCategoryNames(item);
  }

  public static void getClientValues(List<String> keys, Consumer<List<String>> callback) {
    Assert.notEmpty(keys);
    Assert.notNull(callback);
    data.getClientValues(keys, callback);
  }

  public static void getConfiguration(Consumer<Map<String, String>> callback) {
    Assert.notNull(callback);
    data.getConfiguration(callback);
  }

  public static void getItemBrands(Consumer<List<EcBrand>> callback) {
    Assert.notNull(callback);
    data.getItemBrands(callback);
  }

  public static String getPrimaryStockLabel() {
    return data.getPrimaryStockLabel();
  }

  public static int getQuantityInCart(long articleId) {
    return cartList.getQuantity(articleId);
  }

  public static List<EcItem> getResponseItems(ResponseObject response) {
    if (response == null) {
      return new ArrayList<>();
    } else {
      return deserializeItems(response.getResponseAsString());
    }
  }

  public static String getSecondaryStockLabel() {
    return data.getSecondaryStockLabel();
  }

  public static String getWarehouseLabel(String code) {
    return data.getWarehouseLabel(code);
  }

  public static boolean isDebug() {
    return debug;
  }

  public static boolean isListPriceVisible() {
    return listPriceVisible;
  }

  public static boolean isPriceVisible() {
    return priceVisible;
  }

  public static boolean isStockLimited() {
    return stockLimited;
  }

  public static EcRequest maybeCreateRequest(String service, String label) {
    if (!pendingRequests.isEmpty()) {
      for (EcRequest request : pendingRequests) {
        if (request.sameService(service) && request.sameLabel(label)) {
          return null;
        }
      }

      for (EcRequest request : pendingRequests) {
        finalizeRequest(request, false);
      }
      pendingRequests.clear();
    }

    return new EcRequest(service, label);
  }

  public static void onRequestStart(final EcRequest request, int requestId) {
    request.setRequestId(requestId);

    Image cancel = new Image(Global.getImages().closeSmall());
    cancel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        cancelRequest(request);
      }
    });

    Thermometer thermometer = new Thermometer(request.getLabel(), null, cancel);
    String progressId = BeeKeeper.getScreen().addProgress(thermometer);
    if (!BeeUtils.isEmpty(progressId)) {
      request.setProgressId(thermometer.getId());
    }

    pendingRequests.add(request);
  }

  public static void openCart(final CartType cartType) {
    ensureBrands(new Consumer<Boolean>() {
      @Override
      public void accept(Boolean input) {
        data.getDeliveryMethods(new Consumer<List<DeliveryMethod>>() {
          @Override
          public void accept(List<DeliveryMethod> deliveryMethods) {
            Cart cart = getCart(cartType);
            ShoppingCart widget = new ShoppingCart(cartType, cart, deliveryMethods);

            resetActiveCommand();
            searchBox.clearValue();

            BeeKeeper.getScreen().show(widget);
          }
        });
      }
    });
  }

  public static void openItem(final EcItem item, final boolean allowAddToCart) {
    Assert.notNull(item);

    final String activeViewId = getActiveViewId();

    ensureBrands(new Consumer<Boolean>() {
      @Override
      public void accept(Boolean input) {
        if (!Objects.equals(activeViewId, getActiveViewId())) {
          return;
        }

        ParameterList params = createArgs(SVC_GET_ITEM_INFO);

        params.addQueryItem(COL_TCD_ARTICLE, item.getArticleId());

        BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            dispatchMessages(response);

            if (Objects.equals(activeViewId, getActiveViewId())
                && response.hasResponse(EcItemInfo.class)) {
              EcItemInfo ecItemInfo = EcItemInfo.restore(response.getResponseAsString());
              ItemDetails widget = new ItemDetails(item, ecItemInfo, allowAddToCart);

              DialogBox dialog = DialogBox.create(item.getName(),
                  EcStyles.name(ItemDetails.STYLE_PRIMARY, "dialog"));
              dialog.setWidget(widget);

              dialog.setHideOnEscape(true);
              dialog.setAnimationEnabled(true);

              dialog.cascade();
            }
          }
        });
      }
    });
  }

  public static void persistCartItem(CartType cartType, CartItem cartItem) {
    Assert.notNull(cartType);
    Assert.notNull(cartItem);
    persistCartItem(cartType, cartItem.getEcItem().getArticleId(), cartItem.getQuantity());
  }

  public static Cart refreshCart(CartType cartType) {
    cartList.refresh(cartType);
    return getCart(cartType);
  }

  public static void register() {
    MenuService.ENSURE_CATEGORIES_AND_OPEN_GRID.setHandler(new MenuHandler() {
      @Override
      public void onSelection(final String parameters) {
        ensureCategories(new Consumer<Boolean>() {
          @Override
          public void accept(Boolean input) {
            GridFactory.openGrid(parameters);
          }
        });
      }
    });

    MenuService.EDIT_TERMS_OF_DELIVERY.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        editTermsOfDelivery(null);
      }
    });
    MenuService.EDIT_EC_CONTACTS.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        editEcContacts(null);
      }
    });

    ViewFactory.registerSupplier(KEY_TERMS_OF_DELIVERY, new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        editTermsOfDelivery(callback);
      }
    });
    ViewFactory.registerSupplier(KEY_EC_CONTACTS, new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        editEcContacts(callback);
      }
    });

    CategoryFullNameRenderer.Provider provider = new CategoryFullNameRenderer.Provider();

    RendererFactory.registerGcrProvider(GRID_DISCOUNTS, COL_DISCOUNT_CATEGORY, provider);
    RendererFactory.registerGcrProvider(GRID_ARTICLE_CATEGORIES, COL_TCD_CATEGORY, provider);
    RendererFactory.registerGcrProvider(GRID_GROUP_CATEGORIES, COL_GROUP_CATEGORY, provider);

    GridFactory.registerGridInterceptor("EcPricing", new EcPricingHandler());
    GridFactory.registerGridInterceptor("EcCostChanges", new EcCostChangesHandler());

    GridFactory.registerGridInterceptor(GRID_ARTICLE_GRAPHICS, new ArticleGraphicsHandler());
    GridFactory.registerGridInterceptor("EcBanners", new BannerGridInterceptor());
    GridFactory.registerGridInterceptor(GRID_ARTICLE_CODES, new ArticleCodesGridInterceptor());
    GridFactory.registerGridInterceptor(GRID_ARTICLE_CARS, new ArticleCarsGridInterceptor());
    GridFactory.registerGridInterceptor(TBL_TCD_ORPHANS, new EcOrphansGrid());

    FormFactory.registerFormInterceptor("EcRegistration", new EcRegistrationForm());
    FormFactory.registerFormInterceptor("EcOrder", new EcOrderForm());
    FormFactory.registerFormInterceptor(FORM_CATEGORIES, new EcCategoriesForm());
  }

  public static Cart removeFromCart(CartType cartType, EcItem ecItem) {
    if (cartList.removeFromCart(cartType, ecItem)) {
      persistCartItem(cartType, ecItem.getArticleId(), 0);
    }
    return getCart(cartType);
  }

  public static void renderItems(final ItemPanel panel, final List<EcItem> items) {
    Assert.notNull(panel);
    Assert.notNull(items);

    ensureCategoriesAndBrandsAndStockLabels(new Consumer<Boolean>() {
      @Override
      public void accept(Boolean input) {
        if (BeeUtils.isTrue(input)) {
          panel.render(items);
        }
      }
    });
  }

  public static void requestItems(String service, String label, ParameterList params,
      final Consumer<List<EcItem>> callback) {

    Assert.notEmpty(service);
    Assert.notNull(params);
    Assert.notNull(callback);

    final EcRequest request = maybeCreateRequest(service, label);
    if (request == null) {
      return;
    }

    int requestId = BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(request, response)) {
          List<EcItem> items = getResponseItems(response);
          if (!items.isEmpty()) {
            callback.accept(items);
          }
        }

        finalizeRequest(request, true);
      }
    });

    onRequestStart(request, requestId);
  }

  public static void resetCart(CartType cartType) {
    Cart cart = getCart(cartType);
    if (cart != null) {
      cart.reset();
      cartList.refresh(cartType);
    }
  }

  public static void restoreShoppingCarts() {
    BeeKeeper.getRpc().makeGetRequest(createArgs(SVC_GET_SHOPPING_CARTS), new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse()) {
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (arr != null) {
            Set<CartType> restoredTypes = EnumSet.noneOf(CartType.class);

            for (String s : arr) {
              CartItem cartItem = CartItem.restore(s);
              Integer type = BeeUtils.toIntOrNull(cartItem.getNote());

              if (EnumUtils.isOrdinal(CartType.class, type)) {
                CartType cartType = EnumUtils.getEnumByIndex(CartType.class, type);
                cartList.getCart(cartType).add(cartItem.getEcItem(), cartItem.getQuantity());

                restoredTypes.add(cartType);
              }
            }

            for (CartType cartType : restoredTypes) {
              cartList.refresh(cartType);
            }
          }
        }
      }
    });
  }

  public static void saveConfiguration(String key, String value) {
    Assert.notEmpty(key);
    data.saveConfiguration(key, value);
  }

  public static void searchItems(String service, String query,
      final Consumer<List<EcItem>> callback) {

    if (!checkSearchQuery(query)) {
      return;
    }

    ParameterList params = createArgs(service);
    params.addDataItem(VAR_QUERY, query);

    requestItems(service, query, params, callback);
  }

  public static void setBackgroundPicture(Long article, ItemPicture widget) {
    Assert.notNull(article);
    Assert.notNull(widget);

    Multimap<Long, ItemPicture> articleWidgets = ArrayListMultimap.create();
    articleWidgets.put(article, widget);

    setBackgroundPictures(articleWidgets);
  }

  public static void setBackgroundPictures(Multimap<Long, ItemPicture> articleWidgets) {
    Assert.notNull(articleWidgets);
    pictures.setBackground(articleWidgets);
  }

  public static void setListPriceVisible(boolean listPriceVisible) {
    EcKeeper.listPriceVisible = listPriceVisible;
  }

  public static void setPriceVisible(boolean priceVisible) {
    EcKeeper.priceVisible = priceVisible;
  }

  public static void setStockLimited(boolean stockLimited) {
    EcKeeper.stockLimited = stockLimited;
  }

  public static boolean showGlobalSearch() {
    return Settings.getBoolean("showGlobalSearch");
  }

  public static boolean showItemSuppliers() {
    return Settings.getBoolean("showItemSuppliers");
  }

  public static void showPromo(final boolean checkView) {
    ParameterList params = createArgs(SVC_GET_PROMO);

    List<RowInfo> cachedBannerInfo = pictures.getCachedBannerInfo();
    if (!BeeUtils.isEmpty(cachedBannerInfo)) {
      params.addDataItem(VAR_BANNERS, Codec.beeSerialize(cachedBannerInfo));
    }

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        dispatchMessages(response);

        List<EcItem> items = null;

        if (response.hasResponse(Pair.class)) {
          Pair<String, String> pair = Pair.restore(response.getResponseAsString());
          if (!BeeUtils.isEmpty(pair.getA())) {
            pictures.setBanners(BeeRowSet.restore(pair.getA()));
          }

          if (!BeeUtils.isEmpty(pair.getB())) {
            items = deserializeItems(pair.getB());
          }
        }

        if (checkView && BeeKeeper.getScreen().getActiveWidget() instanceof Panel) {
          return;
        }
        if (DataUtils.isEmpty(pictures.getBanners()) && BeeUtils.isEmpty(items)) {
          return;
        }

        resetActiveCommand();

        if (items == null) {
          items = new ArrayList<>();
        }
        Promo widget = new Promo(pictures.getBanners(), items);

        BeeKeeper.getScreen().show(widget);
      }
    });
  }

  public static boolean toggleDebug() {
    debug = !isDebug();
    return debug;
  }

  public static void toggleListPriceVisibility() {
    setListPriceVisible(!isListPriceVisible());
    EcStyles.setVisible(Selectors.getNodes(EcStyles.getListPriceSelector()), isListPriceVisible());
  }

  public static void togglePriceVisibility() {
    setPriceVisible(!isPriceVisible());
    EcStyles.setVisible(Selectors.getNodes(EcStyles.getPriceSelector()), isPriceVisible());
  }

  public static void toggleStockLimited() {
    setStockLimited(!isStockLimited());

    NodeList<Element> nodes = Selectors.getNodes(EcStyles.getStockSelector());
    if (nodes != null) {
      for (int i = 0; i < nodes.getLength(); i++) {
        Element element = nodes.getItem(i);
        double stock = BeeUtils.toDouble(DomUtils.getDataProperty(element, DATA_ATTRIBUTE_STOCK));
        if (stock > MAX_VISIBLE_STOCK) {
          element.setInnerText(formatStock(stock));
        }
      }
    }
  }

  static void doCommand(EcCommandWidget commandWidget) {
    EcView ecView = EcView.create(commandWidget.getService());
    if (ecView != null) {
      searchBox.clearValue();
      BeeKeeper.getScreen().show(ecView);
    }

    if (activeCommand == null || !activeCommand.getService().equals(commandWidget.getService())) {
      if (activeCommand != null) {
        activeCommand.deactivate();
      }

      activeCommand = commandWidget;
      activeCommand.activate();
    }
  }

  static void ensureClientStyleSheet() {
    if (!clientStyleSheetInjected) {
      clientStyleSheetInjected = true;

      UserInterface ui = BeeKeeper.getScreen().getUserInterface();
      if (!ui.getStyleSheets().contains(CLIENT_STYLE_SHEET)) {
        DomUtils.injectStyleSheet(Paths.getStyleSheetUrl(CLIENT_STYLE_SHEET,
            TimeUtils.nowMinutes()));
      }
    }
  }

  static CartList getCartlist() {
    return cartList;
  }

  static InputText getSearchBox() {
    return searchBox;
  }

  private static void cancelRequest(EcRequest request) {
    BeeKeeper.getRpc().cancelRequest(request.getRequestId());
    finalizeRequest(request, true);
  }

  private static boolean checkSearchQuery(String query) {
    if (BeeUtils.hasLength(BeeUtils.trim(query), MIN_SEARCH_QUERY_LENGTH)) {
      return true;
    } else {
      BeeKeeper.getScreen().notifyWarning(
          Localized.getMessages().minSearchQueryLength(MIN_SEARCH_QUERY_LENGTH));
      return false;
    }
  }

  private static List<EcItem> deserializeItems(String serialized) {
    List<EcItem> items = new ArrayList<>();

    if (serialized != null) {
      long millis = System.currentTimeMillis();

      String[] arr = Codec.beeDeserializeCollection(serialized);
      if (arr != null) {
        for (String s : arr) {
          items.add(EcItem.restore(s));
        }
      }

      if (isDebug()) {
        logger.debug("deserialized items", items.size(), TimeUtils.elapsedMillis(millis));
      }
    }

    return items;
  }

  private static void editConfigurationHtml(final String supplierKey, final String caption,
      final String urlColumn, final String htmlColumn, final ViewCallback callback) {

    data.getConfiguration(new Consumer<Map<String, String>>() {
      @Override
      public void accept(Map<String, String> input) {
        final String url = input.get(urlColumn);
        final String html = input.get(htmlColumn);

        HtmlEditor editor =
            new HtmlEditor(supplierKey, caption, url, html, new BiConsumer<String, String>() {
              @Override
              public void accept(String newUrl, String newHtml) {
                if (!BeeUtils.equalsTrim(url, newUrl)) {
                  data.saveConfiguration(urlColumn, newUrl);
                }
                if (!BeeUtils.equalsTrim(html, newHtml)) {
                  data.saveConfiguration(htmlColumn, newHtml);
                }
              }
            });

        if (callback == null) {
          BeeKeeper.getScreen().show(editor);
        } else {
          callback.onSuccess(editor);
        }
      }
    });
  }

  private static void editEcContacts(ViewCallback callback) {
    editConfigurationHtml(KEY_EC_CONTACTS, Localized.getConstants().ecContacts(),
        COL_CONFIG_CONTACTS_URL, COL_CONFIG_CONTACTS_HTML, callback);
  }

  private static void editTermsOfDelivery(ViewCallback callback) {
    editConfigurationHtml(KEY_TERMS_OF_DELIVERY, Localized.getConstants().ecTermsOfDelivery(),
        COL_CONFIG_TOD_URL, COL_CONFIG_TOD_HTML, callback);
  }

  private static String getActiveViewId() {
    IdentifiableWidget activeWidget = BeeKeeper.getScreen().getActiveWidget();
    return (activeWidget == null) ? null : activeWidget.getId();
  }

  private static void persistCartItem(CartType cartType, long article, int quantity) {
    ParameterList params = createArgs(SVC_UPDATE_SHOPPING_CART);

    params.addDataItem(COL_SHOPPING_CART_TYPE, cartType.ordinal());
    params.addDataItem(COL_SHOPPING_CART_ARTICLE, article);
    params.addDataItem(COL_SHOPPING_CART_QUANTITY, quantity);

    BeeKeeper.getRpc().makeRequest(params);
  }

  private static void resetActiveCommand() {
    if (activeCommand != null) {
      activeCommand.deactivate();
      activeCommand = null;
    }
  }

  private EcKeeper() {
  }
}
