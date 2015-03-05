package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Service;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.EnumUtils;

public final class EcConstants {

  public enum CartType implements HasCaption {
    MAIN(Localized.getConstants().ecShoppingCartMain(),
        Localized.getConstants().ecShoppingCartMainShort()),
    ALTERNATIVE(Localized.getConstants().ecShoppingCartAlternative(),
        Localized.getConstants().ecShoppingCartAlternativeShort());

    private final String caption;
    private final String label;

    private CartType(String caption, String label) {
      this.caption = caption;
      this.label = label;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    public String getLabel() {
      return label;
    }
  }

  public enum EcClientType implements HasLocalizedCaption {
    COMPANY {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.ecClientTypeCompany();
      }
    },
    PERSON {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.ecClientTypePerson();
      }
    };

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }
  }

  public enum EcDisplayedPrice implements HasCaption {
    EOLTAS, MOTOPROFIL, MIN, MAX;

    public static EcSupplier getSupplier(EcDisplayedPrice displayedPrice) {
      if (displayedPrice != null && displayedPrice.ordinal() < EcSupplier.values().length) {
        return EcSupplier.values()[displayedPrice.ordinal()];
      } else {
        return null;
      }
    }

    @Override
    public String getCaption() {
      return name();
    }
  }

  public enum EcOrderStatus implements HasCaption {
    NEW {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.ecOrderStatusNew();
      }

      @Override
      public String getSubject(LocalizableConstants constants) {
        return constants.ecOrderStatusNewSubject();
      }
    },

    ACTIVE {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.ecOrderStatusActive();
      }

      @Override
      public String getSubject(LocalizableConstants constants) {
        return constants.ecOrderStatusActiveSubject();
      }
    },

    REJECTED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.ecOrderStatusRejected();
      }

      @Override
      public String getSubject(LocalizableConstants constants) {
        return constants.ecOrderStatusRejectedSubject();
      }
    },

    FINISHED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.ecOrderStatusFinished();
      }

      @Override
      public String getSubject(LocalizableConstants constants) {
        return constants.ecOrderStatusFinishedSubject();
      }
    };

    public static EcOrderStatus get(Integer status) {
      if (status != null && status >= 0 && status < values().length) {
        return values()[status];
      } else {
        return null;
      }
    }

    public static boolean in(int status, EcOrderStatus... statuses) {
      for (EcOrderStatus st : statuses) {
        if (st.ordinal() == status) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }

    public abstract String getCaption(LocalizableConstants constants);

    public abstract String getSubject(LocalizableConstants constants);
  }

  public enum EcSupplier implements HasCaption {
    EOLTAS("Eolt"), MOTOPROFIL("MP");

    private final String shortName;

    private EcSupplier(String shortName) {
      this.shortName = shortName;
    }

    public String getBasePriceListColumnName() {
      return "BasePriceList" + ordinal();
    }

    public String getBasePriceListParameterName() {
      return "BasePriceList" + shortName;
    }

    @Override
    public String getCaption() {
      return name();
    }

    public String getClientPriceListColumnName() {
      return "ClientPriceList" + ordinal();
    }

    public String getClientPriceListParameterName() {
      return "ClientPriceList" + shortName;
    }

    public String getShortName() {
      return shortName;
    }
  }

  public static void register() {
    EnumUtils.register(EcClientType.class);
    EnumUtils.register(EcOrderStatus.class);
    EnumUtils.register(EcDisplayedPrice.class);
    EnumUtils.register(EcSupplier.class);
  }

  public static final String SVC_GET_PROMO = "getPromo";

  public static final String SVC_FINANCIAL_INFORMATION = "financialInformation";
  public static final String SVC_SHOW_TERMS_OF_DELIVERY = "showTermsOfDelivery";
  public static final String SVC_SHOW_CONTACTS = "showContacts";

  public static final String SVC_GLOBAL_SEARCH = "globalSearch";

  public static final String SVC_GET_CATEGORIES = "getCategories";

  public static final String SVC_SEARCH_BY_ITEM_CODE = "searchByItemCode";
  public static final String SVC_SEARCH_BY_OE_NUMBER = "searchByOeNumber";
  public static final String SVC_SEARCH_BY_CAR = "searchByCar";
  public static final String SVC_SEARCH_BY_BRAND = "searchByBrand";

  public static final String SVC_GENERAL_ITEMS = "generalItems";
  public static final String SVC_BIKE_ITEMS = "bikeItems";

  public static final String SVC_GET_CAR_MANUFACTURERS = "getCarManufacturers";
  public static final String SVC_GET_CAR_MODELS = "getCarModels";
  public static final String SVC_GET_CAR_TYPES = "getCarTypes";

  public static final String SVC_GET_ITEMS_BY_CAR_TYPE = "getItemsByCarType";
  public static final String SVC_GET_CAR_TYPE_HISTORY = "getCarTypeHistory";

  public static final String SVC_GET_ITEM_BRANDS = "getItemBrands";
  public static final String SVC_GET_ITEMS_BY_BRAND = "getItemsByBrand";
  public static final String SVC_GET_ITEM_ANALOGS = "getItemAnalogs";

  public static final String SVC_GET_DELIVERY_METHODS = "getDeliveryMethods";

  public static final String SVC_SUBMIT_ORDER = "submitOrder";

  public static final String SVC_GET_CONFIGURATION = "getConfiguration";
  public static final String SVC_CLEAR_CONFIGURATION = "clearConfiguration";
  public static final String SVC_SAVE_CONFIGURATION = "saveConfiguration";

  public static final String SVC_GET_ITEM_INFO = "getItemInfo";

  public static final String SVC_GET_PICTURES = "getPictures";

  public static final String SVC_UPDATE_COSTS = "updateCosts";

  public static final String SVC_SEND_TO_ERP = "sendToERP";
  public static final String SVC_MAIL_ORDER = "mailOrder";
  public static final String SVC_REGISTER_ORDER_EVENT = "registerOrderEvent";

  public static final String SVC_MERGE_CATEGORY = "mergeCategory";

  public static final String SVC_GET_SHOPPING_CARTS = "getShoppingCarts";
  public static final String SVC_UPDATE_SHOPPING_CART = "updateShoppingCart";

  public static final String SVC_GET_ITEM_GROUPS = "getItemGroups";
  public static final String SVC_GET_GROUP_FILTERS = "getGroupFilters";
  public static final String SVC_GET_GROUP_ITEMS = "getGroupItems";

  public static final String SVC_GET_CLIENT_INFO = "getClientInfo";
  public static final String SVC_GET_CLIENT_STOCK_LABELS = "getClientStockLabels";

  public static final String SVC_ADD_TO_UNSUPPLIED_ITEMS = "addToUnsuppliedItems";

  public static final String SVC_UPLOAD_GRAPHICS = "uploadGraphics";
  public static final String SVC_UPLOAD_BANNERS = "uploadBanners";

  public static final String SVC_CREATE_CLIENT = "createClient";
  public static final String SVC_CREATE_ITEM = "createItem";
  public static final String SVC_ADOPT_ORPHANS = "adoptOrphans";

  public static final String SVC_ADD_ARTICLE_CAR_TYPES = "addArticleCarTypes";

  public static final String VAR_PREFIX = Service.RPC_VAR_PREFIX + "ec_";

  public static final String VAR_QUERY = VAR_PREFIX + "query";
  public static final String VAR_OFFSET = VAR_PREFIX + "offset";
  public static final String VAR_LIMIT = VAR_PREFIX + "limit";

  public static final String VAR_MANUFACTURER = VAR_PREFIX + "manufacturer";
  public static final String VAR_MODEL = VAR_PREFIX + "model";
  public static final String VAR_TYPE = VAR_PREFIX + "type";

  public static final String VAR_CART = VAR_PREFIX + "cart";
  public static final String VAR_FILTER = VAR_PREFIX + "filter";

  public static final String VAR_ORDER = VAR_PREFIX + "order";
  public static final String VAR_STATUS = VAR_PREFIX + "status";
  public static final String VAR_MAIL = VAR_PREFIX + "mail";

  public static final String VAR_BANNERS = VAR_PREFIX + "banners";

  public static final String TBL_BANNERS = "EcBanners";
  public static final String TBL_DELIVERY_METHODS = "DeliveryMethods";
  public static final String TBL_CLIENTS = "EcClients";
  public static final String TBL_CONFIGURATION = "EcConfiguration";
  public static final String TBL_DISCOUNTS = "EcDiscounts";
  public static final String TBL_GROUP_CATEGORIES = "EcGroupCategories";
  public static final String TBL_GROUP_CRITERIA = "EcGroupCriteria";
  public static final String TBL_GROUPS = "EcGroups";
  public static final String TBL_HISTORY = "EcHistory";
  public static final String TBL_MANAGERS = "EcManagers";
  public static final String TBL_ORDER_EVENTS = "EcOrderEvents";
  public static final String TBL_ORDER_ITEMS = "EcOrderItems";
  public static final String TBL_ORDERS = "EcOrders";
  public static final String TBL_PRIMARY_WAREHOUSES = "PrimaryWarehouses";
  public static final String TBL_REGISTRATIONS = "EcRegistrations";
  public static final String TBL_REJECTION_REASONS = "RejectionReasons";
  public static final String TBL_SECONDARY_WAREHOUSES = "SecondaryWarehouses";
  public static final String TBL_SHOPPING_CARTS = "ShoppingCarts";
  public static final String TBL_UNSUPPLIED_ITEMS = "UnsuppliedItems";

  public static final String TBL_TCD_ARTICLES = "TcdArticles";
  public static final String TBL_TCD_ARTICLE_CODES = "TcdArticleCodes";
  public static final String TBL_TCD_ARTICLE_PRICES = "TcdArticlePrices";

  public static final String TBL_TCD_PRICELISTS = "TcdPriceLists";

  public static final String TBL_TCD_CATEGORIES = "TcdCategories";
  public static final String TBL_TCD_TECDOC_CATEGORIES = "TcdTecDocCategories";
  public static final String TBL_TCD_ARTICLE_CATEGORIES = "TcdArticleCategories";

  public static final String TBL_TCD_GRAPHICS = "TcdGraphics";
  public static final String TBL_TCD_ARTICLE_GRAPHICS = "TcdArticleGraphics";

  public static final String TBL_TCD_MANUFACTURERS = "TcdManufacturers";
  public static final String TBL_TCD_BRANDS = "TcdBrands";
  public static final String TBL_TCD_MODELS = "TcdModels";
  public static final String TBL_TCD_TYPES = "TcdTypes";
  public static final String TBL_TCD_TYPE_ARTICLES = "TcdTypeArticles";

  public static final String TBL_TCD_ARTICLE_SUPPLIERS = "TcdArticleSuppliers";
  public static final String TBL_TCD_REMAINDERS = "TcdRemainders";
  public static final String TBL_TCD_BRANDS_MAPPING = "TcdBrandsMapping";

  public static final String TBL_TCD_CRITERIA = "TcdCriteria";
  public static final String TBL_TCD_ARTICLE_CRITERIA = "TcdArticleCriteria";

  public static final String TBL_TCD_ORPHANS = "TcdOrphans";

  public static final String VIEW_ARTICLE_CODES = "TcdArticleCodes";
  public static final String VIEW_ARTICLES = "TcdArticles";
  public static final String VIEW_BANNERS = "EcBanners";
  public static final String VIEW_DELIVERY_METHODS = "DeliveryMethods";
  public static final String VIEW_CATEGORIES = "TcdCategories";
  public static final String VIEW_CLIENTS = "EcClients";
  public static final String VIEW_CONFIGURATION = "EcConfiguration";
  public static final String VIEW_GROUP_CRITERIA = "EcGroupCriteria";
  public static final String VIEW_HISTORY = "EcHistory";
  public static final String VIEW_MANAGERS = "EcManagers";
  public static final String VIEW_ORDER_EVENTS = "EcOrderEvents";
  public static final String VIEW_ORDER_ITEMS = "EcOrderItems";
  public static final String VIEW_ORDERS = "EcOrders";
  public static final String VIEW_REGISTRATIONS = "EcRegistrations";
  public static final String VIEW_REJECTION_REASONS = "RejectionReasons";
  public static final String VIEW_UNSUPPLIED_ITEMS = "UnsuppliedItems";

  public static final String COL_BANNER_SORT = "Sort";
  public static final String COL_BANNER_PICTURE = "Picture";
  public static final String COL_BANNER_WIDTH = "Width";
  public static final String COL_BANNER_HEIGHT = "Height";
  public static final String COL_BANNER_LINK = "Link";
  public static final String COL_BANNER_SHOW_AFTER = "ShowAfter";
  public static final String COL_BANNER_SHOW_BEFORE = "ShowBefore";

  public static final String COL_DELIVERY_METHOD_ID = "DeliveryMethodID";
  public static final String COL_DELIVERY_METHOD_NAME = "Name";
  public static final String COL_DELIVERY_METHOD_NOTES = "Notes";

  public static final String COL_CLIENT = "Client";
  public static final String COL_CLIENT_USER = "User";
  public static final String COL_CLIENT_REGISTERED = "Registered";
  public static final String COL_CLIENT_TYPE = "Type";
  public static final String COL_CLIENT_PRIMARY_BRANCH = "PrimaryBranch";
  public static final String COL_CLIENT_SECONDARY_BRANCH = "SecondaryBranch";
  public static final String COL_CLIENT_MANAGER = "Manager";
  public static final String COL_CLIENT_PERSON_CODE = "PersonCode";
  public static final String COL_CLIENT_ACTIVITY = "Activity";
  public static final String COL_CLIENT_CREDIT_LIMIT_WARNING = "CreditLimitWarning";
  public static final String COL_CLIENT_DISCOUNT_PERCENT = "DiscountPercent";
  public static final String COL_CLIENT_DISCOUNT_PARENT = "DiscountParent";
  public static final String COL_CLIENT_DISPLAYED_PRICE = "DisplayedPrice";
  public static final String COL_CLIENT_TOGGLE_LIST_PRICE = "ToggleListPrice";
  public static final String COL_CLIENT_TOGGLE_PRICE = "TogglePrice";
  public static final String COL_CLIENT_TOGGLE_STOCK_LIMIT = "ToggleStockLimit";
  public static final String COL_CLIENT_CAR_TYPE_HISTORY_SIZE = "CarTypeHistorySize";
  public static final String COL_CLIENT_NOTES = "Notes";

  public static final String COL_CONFIG_ID = "ConfigurationID";
  public static final String COL_CONFIG_MARGIN_DEFAULT_PERCENT = "MarginDefaultPercent";
  public static final String COL_CONFIG_TOD_URL = "TodUrl";
  public static final String COL_CONFIG_TOD_HTML = "TodHtml";
  public static final String COL_CONFIG_CONTACTS_URL = "ContactsUrl";
  public static final String COL_CONFIG_CONTACTS_HTML = "ContactsHtml";
  public static final String COL_CONFIG_MAIL_ACCOUNT = "MailAccount";
  public static final String COL_CONFIG_INCOMING_MAIL = "IncomingMail";

  public static final String COL_DISCOUNT_CLIENT = "Client";
  public static final String COL_DISCOUNT_DATE_FROM = "DateFrom";
  public static final String COL_DISCOUNT_DATE_TO = "DateTo";
  public static final String COL_DISCOUNT_CATEGORY = "Category";
  public static final String COL_DISCOUNT_BRAND = "Brand";
  public static final String COL_DISCOUNT_SUPPLIER = "Supplier";
  public static final String COL_DISCOUNT_ARTICLE = "Article";
  public static final String COL_DISCOUNT_PERCENT = "Percent";
  public static final String COL_DISCOUNT_PRICE = "Price";

  public static final String COL_GROUP = "Group";
  public static final String COL_GROUP_BRAND_SELECTION = "BrandSelection";
  public static final String COL_GROUP_CATEGORY = "Category";
  public static final String COL_GROUP_CRITERIA = "Criteria";
  public static final String COL_GROUP_CRITERIA_ORDINAL = "Ordinal";
  public static final String COL_GROUP_MOTO = "Moto";
  public static final String COL_GROUP_NAME = "GroupName";
  public static final String COL_GROUP_ORDINAL = "Ordinal";

  public static final String COL_HISTORY_DATE = "Date";
  public static final String COL_HISTORY_USER = "User";
  public static final String COL_HISTORY_SERVICE = "Service";
  public static final String COL_HISTORY_QUERY = "Query";
  public static final String COL_HISTORY_ARTICLE = "Article";
  public static final String COL_HISTORY_COUNT = "Count";
  public static final String COL_HISTORY_DURATION = "Duration";

  public static final String COL_MANAGER_USER = "User";
  public static final String COL_MANAGER_TAB_NR = "TabNr";
  public static final String COL_MANAGER_MAIL_ACCOUNT = "MailAccount";
  public static final String COL_MANAGER_INCOMING_MAIL = "IncomingMail";
  public static final String COL_MANAGER_NOTES = "Notes";

  public static final String COL_ORDER_EVENT_ORDER = "Order";
  public static final String COL_ORDER_EVENT_DATE = "Date";
  public static final String COL_ORDER_EVENT_STATUS = "Status";
  public static final String COL_ORDER_EVENT_USER = "User";

  public static final String COL_ORDER_ITEM_ORDER = "Order";
  public static final String COL_ORDER_ITEM_ARTICLE = "Article";
  public static final String COL_ORDER_ITEM_QUANTITY_ORDERED = "QuantityOrdered";
  public static final String COL_ORDER_ITEM_QUANTITY_SUBMIT = "QuantitySubmit";
  public static final String COL_ORDER_ITEM_PRICE = "Price";
  public static final String COL_ORDER_ITEM_NOTE = "Note";

  public static final String COL_ORDER_DATE = "Date";
  public static final String COL_ORDER_NUMBER = "Number";
  public static final String COL_ORDER_STATUS = "Status";
  public static final String COL_ORDER_CLIENT = "Client";
  public static final String COL_ORDER_MANAGER = "Manager";
  public static final String COL_ORDER_DELIVERY_METHOD = "DeliveryMethod";
  public static final String COL_ORDER_DELIVERY_ADDRESS = "DeliveryAddress";
  public static final String COL_ORDER_COPY_BY_MAIL = "CopyByMail";
  public static final String COL_ORDER_CLIENT_COMMENT = "ClientComment";
  public static final String COL_ORDER_MANAGER_COMMENT = "ManagerComment";
  public static final String COL_ORDER_REJECTION_REASON = "RejectionReason";
  public static final String COL_ORDER_NOTES = "Notes";

  public static final String ALS_ORDER_MANAGER_FIRST_NAME = "ManagerFirstName";
  public static final String ALS_ORDER_MANAGER_LAST_NAME = "ManagerLastName";
  public static final String ALS_ORDER_DELIVERY_METHOD_NAME = "DeliveryMethodName";
  public static final String ALS_ORDER_REJECTION_REASON_NAME = "RejectionReasonName";
  public static final String ALS_ORDER_CLIENT_USER = "ClientUser";
  public static final String ALS_ORDER_CLIENT_FIRST_NAME = "ClientFirstName";
  public static final String ALS_ORDER_CLIENT_LAST_NAME = "ClientLastName";
  public static final String ALS_ORDER_CLIENT_COMPANY_NAME = "ClientCompanyName";

  public static final String COL_REGISTRATION_DATE = "Date";
  public static final String COL_REGISTRATION_TYPE = "Type";
  public static final String COL_REGISTRATION_BRANCH = "Branch";
  public static final String COL_REGISTRATION_COMPANY_NAME = "CompanyName";
  public static final String COL_REGISTRATION_COMPANY_CODE = "CompanyCode";
  public static final String COL_REGISTRATION_VAT_CODE = "VatCode";
  public static final String COL_REGISTRATION_PERSON_CODE = "PersonCode";
  public static final String COL_REGISTRATION_FIRST_NAME = "FirstName";
  public static final String COL_REGISTRATION_LAST_NAME = "LastName";
  public static final String COL_REGISTRATION_EMAIL = "Email";
  public static final String COL_REGISTRATION_PHONE = "Phone";
  public static final String COL_REGISTRATION_CITY = "City";
  public static final String COL_REGISTRATION_ADDRESS = "Address";
  public static final String COL_REGISTRATION_POST_INDEX = "PostIndex";
  public static final String COL_REGISTRATION_COUNTRY = "Country";
  public static final String COL_REGISTRATION_ACTIVITY = "Activity";
  public static final String COL_REGISTRATION_NOTES = "Notes";
  public static final String COL_REGISTRATION_LANGUAGE = "Language";
  public static final String COL_REGISTRATION_HOST = "Host";
  public static final String COL_REGISTRATION_AGENT = "Agent";

  public static final String COL_REJECTION_REASON_NAME = "Name";
  public static final String COL_REJECTION_REASON_NOTES = "Notes";

  public static final String COL_SHOPPING_CART_CREATED = "Created";
  public static final String COL_SHOPPING_CART_CLIENT = "Client";
  public static final String COL_SHOPPING_CART_TYPE = "CartType";
  public static final String COL_SHOPPING_CART_ARTICLE = "Article";
  public static final String COL_SHOPPING_CART_QUANTITY = "Quantity";

  public static final String COL_UNSUPPLIED_ITEM_CLIENT = "Client";
  public static final String COL_UNSUPPLIED_ITEM_DATE = "Date";
  public static final String COL_UNSUPPLIED_ITEM_ORDER = "Order";
  public static final String COL_UNSUPPLIED_ITEM_ARTICLE = "Article";
  public static final String COL_UNSUPPLIED_ITEM_QUANTITY = "Quantity";
  public static final String COL_UNSUPPLIED_ITEM_PRICE = "Price";
  public static final String COL_UNSUPPLIED_ITEM_NOTE = "Note";

  public static final String COL_TCD_ARTICLE = "Article";
  public static final String COL_TCD_ARTICLE_NAME = "ArticleName";
  public static final String COL_TCD_ARTICLE_NR = "ArticleNr";
  public static final String COL_TCD_ARTICLE_UNIT = "Unit";
  public static final String COL_TCD_ARTICLE_WEIGHT = "Weight";
  public static final String COL_TCD_ARTICLE_DESCRIPTION = "Description";
  public static final String COL_TCD_ARTICLE_VISIBLE = "Visible";
  public static final String COL_TCD_ARTICLE_NOVELTY = "Novelty";
  public static final String COL_TCD_ARTICLE_FEATURED = "Featured";
  public static final String COL_TCD_ARTICLE_FEATURED_PRICE = "FeaturedPrice";
  public static final String COL_TCD_ARTICLE_FEATURED_PERCENT = "FeaturedPercent";

  public static final String COL_TCD_SUPPLIER = "Supplier";
  public static final String COL_TCD_SUPPLIER_ID = "SupplierID";

  public static final String COL_TCD_SEARCH_NR = "SearchNr";
  public static final String COL_TCD_OE_CODE = "OECode";
  public static final String COL_TCD_CODE_NR = "CodeNr";

  public static final String COL_TCD_CATEGORY = "Category";
  public static final String COL_TCD_CATEGORY_PARENT = "Parent";
  public static final String COL_TCD_CATEGORY_NAME = "CategoryName";
  public static final String COL_TCD_CATEGORY_FULL_NAME = "FullName";
  public static final String COL_TCD_CATEGORY_MARGIN_PERCENT = "MarginPercent";

  public static final String COL_TCD_MODEL = "Model";
  public static final String COL_TCD_MODEL_NAME = "ModelName";
  public static final String COL_TCD_MODEL_VISIBLE = "Visible";
  public static final String COL_TCD_MANUFACTURER_NAME = "ManufacturerName";
  public static final String COL_TCD_MANUFACTURER = "Manufacturer";
  public static final String COL_TCD_MF_VISIBLE = "Visible";

  public static final String COL_TCD_TYPE = "Type";
  public static final String COL_TCD_TYPE_NAME = "TypeName";
  public static final String COL_TCD_PRODUCED_FROM = "ProducedFrom";
  public static final String COL_TCD_PRODUCED_TO = "ProducedTo";
  public static final String COL_TCD_CCM = "Ccm";
  public static final String COL_TCD_KW_FROM = "KwFrom";
  public static final String COL_TCD_KW_TO = "KwTo";
  public static final String COL_TCD_CYLINDERS = "Cylinders";
  public static final String COL_TCD_MAX_WEIGHT = "MaxWeight";
  public static final String COL_TCD_ENGINE = "Engine";
  public static final String COL_TCD_FUEL = "Fuel";
  public static final String COL_TCD_BODY = "Body";
  public static final String COL_TCD_AXLE = "Axle";
  public static final String COL_TCD_TYPE_VISIBLE = "Visible";

  public static final String COL_TCD_REMAINDER = "Remainder";

  public static final String COL_TCD_PRICELIST = "PriceList";
  public static final String COL_TCD_PRICELIST_NAME = "PriceListName";

  public static final String COL_TCD_PRICE = "Price";
  public static final String COL_TCD_COST = "Cost";
  public static final String COL_TCD_UPDATED_COST = "UpdatedCost";
  public static final String COL_TCD_UPDATE_TIME = "UpdateTime";

  public static final String COL_TCD_BRAND = "Brand";
  public static final String COL_TCD_BRAND_NAME = "BrandName";

  public static final String COL_TCD_ARTICLE_SUPPLIER = "ArticleSupplier";
  public static final String COL_TCD_SUPPLIER_BRAND = "SupplierBrand";

  public static final String COL_TCD_CRITERIA = "Criteria";
  public static final String COL_TCD_CRITERIA_NAME = "CriteriaName";
  public static final String COL_TCD_CRITERIA_VALUE = "Value";

  public static final String COL_TCD_GRAPHICS = "Graphics";
  public static final String COL_TCD_SORT = "Sort";
  public static final String COL_TCD_GRAPHICS_TYPE = "Type";
  public static final String COL_TCD_GRAPHICS_RESOURCE = "Resource";

  public static final String COL_CW_CLIENT = "Client";
  public static final String COL_CW_WAREHOUSE = "Warehouse";

  public static final String PRM_BUTENT_INTERVAL = "ButentIntervalInMinutes";
  public static final String PRM_BUTENT_PRICES = "ButentPrices";
  public static final String PRM_MOTONET_HOURS = "MotoprofilRefreshHours";
  public static final String PRM_PROMO_FEATURED = "PromoFeatured";
  public static final String PRM_PROMO_NOVELTY = "PromoNovelty";

  public static final String GRID_DISCOUNTS = "EcDiscounts";
  public static final String GRID_ARTICLE_CATEGORIES = "TcdArticleCategories";
  public static final String GRID_ARTICLE_CODES = "TcdArticleCodes";
  public static final String GRID_ARTICLE_GRAPHICS = "TcdArticleGraphics";
  public static final String GRID_ARTICLE_CARS = "TcdTypeArticles";
  public static final String GRID_GROUP_CATEGORIES = "EcGroupCategories";

  public static final String FORM_CATEGORIES = "TcdCategories";

  public static final String CATEGORY_ID_SEPARATOR = ",";
  public static final char CATEGORY_NAME_SEPARATOR = '\n';

  public static final String CURRENCY = "Eur";

  public static final String WEIGHT_UNIT = "kg";
  public static final int WEIGHT_SCALE = 3;

  public static final int MAX_VISIBLE_STOCK = 5;
  public static final String DATA_ATTRIBUTE_STOCK = "stock";

  public static final int MIN_SEARCH_QUERY_LENGTH = 3;
  public static final int DEFAULT_CAR_TYPE_HISTORY_SIZE = 10;

  public static final String PICTURE_PREFIX = "data:image/";

  public static final String CLIENT_STYLE_SHEET = "ec";

  public static final String NAME_PREFIX = "ec-";

  private EcConstants() {
  }
}
