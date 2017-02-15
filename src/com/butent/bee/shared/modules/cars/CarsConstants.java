package com.butent.bee.shared.modules.cars;

public final class CarsConstants {
  public static final String SVC_GET_CONFIGURATION = "GetConfiguration";
  public static final String SVC_SAVE_DIMENSIONS = "SaveDimensions";
  public static final String SVC_SET_BUNDLE = "SetBundle";
  public static final String SVC_DELETE_BUNDLES = "DeleteBundles";
  public static final String SVC_SET_OPTION = "SetOption";
  public static final String SVC_DELETE_OPTION = "DeleteOption";
  public static final String SVC_SET_RELATION = "SetRelation";
  public static final String SVC_DELETE_RELATION = "DeleteRelation";
  public static final String SVC_SET_RESTRICTIONS = "SetRestrictions";
  public static final String SVC_SAVE_OBJECT = "SaveObject";
  public static final String SVC_SAVE_OBJECT_INFO = "SaveObjectInfo";
  public static final String SVC_GET_OBJECT = "GetObject";
  public static final String SVC_GET_CALENDAR = "GetCalendar";

  public static final String TBL_CONF_GROUPS = "ConfGroups";
  public static final String TBL_CONF_OPTIONS = "ConfOptions";
  public static final String TBL_CONF_PACKET_OPTIONS = "ConfPacketOptions";
  public static final String TBL_CONF_PRICELIST = "ConfPricelist";
  public static final String TBL_CONF_DIMENSIONS = "ConfDimensions";
  public static final String TBL_CONF_BUNDLES = "ConfBundles";
  public static final String TBL_CONF_BUNDLE_OPTIONS = "ConfBundleOptions";
  public static final String TBL_CONF_BRANCH_BUNDLES = "ConfBranchBundles";
  public static final String TBL_CONF_BRANCH_OPTIONS = "ConfBranchOptions";
  public static final String TBL_CONF_RELATIONS = "ConfRelations";
  public static final String TBL_CONF_RESTRICTIONS = "ConfRestrictions";
  public static final String TBL_CONF_OBJECTS = "ConfObjects";
  public static final String TBL_CONF_OBJECT_OPTIONS = "ConfObjectOptions";
  public static final String TBL_CONF_TEMPLATES = "ConfTemplates";

  public static final String TBL_CAR_ORDERS = "CarOrders";
  public static final String TBL_CAR_ORDER_ITEMS = "CarOrderItems";

  public static final String TBL_CAR_DISCOUNTS = "CarDiscounts";

  public static final String TBL_CAR_BUNDLES = "CarBundles";
  public static final String TBL_CAR_BUNDLE_JOBS = "CarBundleJobs";
  public static final String TBL_CAR_BUNDLE_ITEMS = "CarBundleItems";

  public static final String TBL_SERVICE_ORDERS = "CarServiceOrders";
  public static final String TBL_SERVICE_ORDER_ITEMS = "CarServiceItems";
  public static final String TBL_SERVICE_ORDER_JOBS = "CarServiceJobs";
  public static final String TBL_SERVICE_JOB_PROGRESS = "CarJobProgress";
  public static final String TBL_SERVICE_EVENTS = "CarServiceEvents";

  public static final String VIEW_CARS = "Cars";
  public static final String VIEW_CAR_RECALLS = "CarRecalls";

  public static final String COL_BRANCH = "Branch";
  public static final String COL_BRANCH_NAME = "BranchName";
  public static final String COL_OPTION = "Option";
  public static final String COL_OPTION_NAME = "OptionName";
  public static final String COL_CODE = "Code";
  public static final String COL_CODE2 = "Code2";
  public static final String COL_DESCRIPTION = "Description";
  public static final String COL_CRITERIA = "Criteria";
  public static final String COL_BUNDLE = "Bundle";
  public static final String COL_KEY = "Key";
  public static final String COL_TYPE = "Type";
  public static final String COL_GROUP = "Group";
  public static final String COL_GROUP_NAME = "GroupName";
  public static final String COL_BLOCKED = "Blocked";
  public static final String COL_ORDINAL = "Ordinal";
  public static final String COL_REQUIRED = "Required";
  public static final String COL_BRANCH_BUNDLE = "BranchBundle";
  public static final String COL_BRANCH_OPTION = "BranchOption";
  public static final String COL_PRICE = "Price";
  public static final String COL_DENIED = "Denied";
  public static final String COL_OBJECT = "Object";
  public static final String COL_PHOTO_CODE = "PhotoCode";
  public static final String COL_PACKET = "Packet";

  public static final String COL_ORDER = "Order";
  public static final String COL_CAR = "Car";
  public static final String COL_SERVICE_ORDER = "ServiceOrder";
  public static final String COL_JOB = "Job";
  public static final String COL_DURATION = "Duration";
  public static final String COL_SERVICE_EVENT = "ServiceEvent";
  public static final String COL_BUNDLE_NAME = "BundleName";
  public static final String COL_VALID_UNTIL = "ValidUntil";

  public static final String COL_CHECKED = "Checked";
  public static final String COL_CAR_DISCOUNT = "CarDiscount";
  public static final String COL_PRODUCED_FROM = "ProducedFrom";
  public static final String COL_PRODUCED_TO = "ProducedTo";

  public static final String FORM_CONF_OPTION = "ConfOption";
  public static final String FORM_CAR_ORDER = "CarOrder";
  public static final String FORM_CAR = "Car";
  public static final String FORM_TEMPLATE = "ConfTemplate";
  public static final String FORM_CAR_SERVICE_ORDER = "CarServiceOrder";
  public static final String FORM_CAR_SERVICE_EVENT = "CarServiceEvent";

  public static final String VAR_PRICE_DEFAULT = "PriceDefault";
  public static final String VAR_PRICE_OPTIONAL = "PriceOptional";
  public static final String VAR_REL_REQUIRED = "RelRequired";
  public static final String VAR_REL_DENIED = "RelDenied";

  public static final String STAGE_ACTION_READONLY = "MakeOrderReadOnly";
  public static final String STAGE_ACTION_LOST = "EnterOrderLostReason";
  public static final String STAGE_TRIGGER_NEW = "OnCreateNewOrder";
  public static final String STAGE_TRIGGER_SENT = "OnSendOrder";

  private CarsConstants() {
  }
}
