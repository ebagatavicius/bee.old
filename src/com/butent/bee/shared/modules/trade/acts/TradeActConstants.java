package com.butent.bee.shared.modules.trade.acts;

import com.butent.bee.shared.utils.EnumUtils;

public final class TradeActConstants {

  public static final String SVC_GET_ITEMS_FOR_SELECTION = "GetItemsForSelection";
  public static final String SVC_COPY_ACT = "CopyAct";
  public static final String SVC_SAVE_ACT_AS_TEMPLATE = "SaveActAsTemplate";
  public static final String SVC_GET_TEMPLATE_ITEMS_AND_SERVICES = "GetTemplateItemsAndServices";
  public static final String SVC_GET_ITEMS_FOR_RETURN = "GetItemsForReturn";
  public static final String SVC_GET_ITEMS_FOR_MULTI_RETURN = "GetItemsForMultiReturn";
  public static final String SVC_RETURN_ACT_ITEMS = "ReturnActItems";
  public static final String SVC_SPLIT_ACT_SERVICES = "SplitActServices";
  public static final String SVC_ALTER_ACT_KIND = "AlterActKind";

  public static final String SVC_GET_ACTS_FOR_INVOICE = "GetActsForInvoice";
  public static final String SVC_GET_SERVICES_FOR_INVOICE = "GetServicesForInvoice";
  public static final String SVC_CREATE_ACT_INVOICE = "CreateActInvoice";

  public static final String SVC_ITEMS_BY_COMPANY_REPORT = "GetItemsByCompanyReport";
  public static final String SVC_STOCK_REPORT = "GetStockReport";
  public static final String SVC_SERVICES_REPORT = "GetServicesReport";
  public static final String SVC_TRANSFER_REPORT = "GetTransferReport";

  public static final String SVC_HAS_INVOICES_OR_SECONDARY_ACTS = "HasInvoicesOrSecondaryActs";

  public static final String TBL_TRADE_ACTS = "TradeActs";
  public static final String TBL_TRADE_ACT_ITEMS = "TradeActItems";
  public static final String TBL_TRADE_ACT_SERVICES = "TradeActServices";

  public static final String TBL_TRADE_ACT_TEMPLATES = "TradeActTemplates";
  public static final String TBL_TRADE_ACT_TMPL_ITEMS = "TradeActTmplItems";
  public static final String TBL_TRADE_ACT_TMPL_SERVICES = "TradeActTmplServices";

  public static final String TBL_TRADE_ACT_NAMES = "TradeActNames";

  public static final String TBL_TRADE_ACT_INVOICES = "TradeActInvoices";

  public static final String VIEW_TRADE_ACTS = "TradeActs";
  public static final String VIEW_TRADE_ACT_ITEMS = "TradeActItems";
  public static final String VIEW_TRADE_ACT_SERVICES = "TradeActServices";

  public static final String VIEW_TRADE_ACT_TEMPLATES = "TradeActTemplates";
  public static final String VIEW_TRADE_ACT_TMPL_ITEMS = "TradeActTmplItems";
  public static final String VIEW_TRADE_ACT_TMPL_SERVICES = "TradeActTmplServices";

  public static final String VIEW_TRADE_ACT_INVOICES = "TradeActInvoices";
  public static final String VIEW_INVOICE_TRADE_ACTS = "InvoiceTradeActs";

  public static final String COL_TRADE_ACT = "TradeAct";
  public static final String COL_TRADE_ACT_TEMPLATE = "TradeActTemplate";

  public static final String COL_TA_DATE = "Date";
  public static final String COL_TA_UNTIL = "Until";
  public static final String COL_TA_SERIES = "Series";
  public static final String COL_TA_NAME = "Name";
  public static final String COL_TA_NUMBER = "Number";
  public static final String COL_TA_KIND = "Kind";
  public static final String COL_TA_OPERATION = "Operation";
  public static final String COL_TA_STATUS = "Status";
  public static final String COL_TA_COMPANY = "Company";
  public static final String COL_TA_CONTACT = "Contact";
  public static final String COL_TA_OBJECT = "Object";
  public static final String COL_TA_MANAGER = "Manager";
  public static final String COL_TA_CURRENCY = "Currency";
  public static final String COL_TA_VEHICLE = "Vehicle";
  public static final String COL_TA_DRIVER = "Driver";
  public static final String COL_TA_NOTES = "Notes";
  public static final String COL_TA_PARENT = "Parent";

  public static final String COL_TA_TEMPLATE_NAME = "Template";

  public static final String COL_TA_ITEM = "Item";

  public static final String COL_TA_SERVICE_FROM = "DateFrom";
  public static final String COL_TA_SERVICE_TO = "DateTo";
  public static final String COL_TA_SERVICE_TARIFF = "Tariff";
  public static final String COL_TA_SERVICE_FACTOR = "Factor";
  public static final String COL_TA_SERVICE_DAYS = "DaysPerWeek";
  public static final String COL_TA_SERVICE_MIN = "MinTerm";

  public static final String COL_TA_INVOICE_SERVICE = "TradeActService";
  public static final String COL_TA_INVOICE_ITEM = "SaleItem";
  public static final String COL_TA_INVOICE_FROM = "DateFrom";
  public static final String COL_TA_INVOICE_TO = "DateTo";

  public static final String COL_TRADE_ACT_NAME = "ActName";

  public static final String ALS_RETURNED_QTY = "ReturnedQty";
  public static final String ALS_REMAINING_QTY = "RemainingQty";

  public static final String ALS_BASE_AMOUNT = "BaseAmount";
  public static final String ALS_DISCOUNT_AMOUNT = "DiscountAmount";
  public static final String ALS_WITHOUT_VAT = "WithoutVat";
  public static final String ALS_VAT_AMOUNT = "VatAmount";
  public static final String ALS_TOTAL_AMOUNT = "TotalAmount";
  public static final String ALS_ITEM_TOTAL = "ItemTotal";

  public static final String GRID_TRADE_ACTS = "TradeActs";
  public static final String GRID_TRADE_ACT_TEMPLATES = "TradeActTemplates";

  public static final String GRID_TRADE_ACT_ITEMS = "TradeActItems";
  public static final String GRID_TRADE_ACT_SERVICES = "TradeActServices";

  public static final String GRID_TRADE_ACTS_AND_ITEMS = "TradeActsAndItems";

  public static final String FORM_TRADE_ACT = "TradeAct";
  public static final String FORM_INVOICE_BUILDER = "TradeActInvoiceBuilder";

  public static final String PRP_QUANTITY = "qty";
  public static final String PRP_ITEM_PRICE = "item_price";
  public static final String PRP_WAREHOUSE_PREFIX = "w-";
  public static final String PRP_PARENT_ACT = "parent_act";
  public static final String PRP_ITEM_TOTAL = "item_total";
  public static final String PRP_RETURNED_QTY = "returned_qty";
  public static final String PRP_TA_SERVICE_FROM = "service_from";
  public static final String PRP_TA_SERVICE_TO = "service_to";
  public static final String PRP_INVOICE_PERIODS = "invoice_periods";

  public static final String PRM_IMPORT_TA_ITEM_RX = "ImportActItemRegEx";
  public static final String RX_IMPORT_ACT_ITEM = "^(.+);(.*);(\\d+\\.*\\d*)$";

  public static final String PRM_TA_NUMBER_LENGTH = "ActNumberLength";
  public static final String PRM_RETURNED_ACT_STATUS = "ReturnedActStatus";

  public static final String PFX_START_STOCK = "StartStock_";
  public static final String PFX_MOVEMENT = "Movement_";
  public static final String PFX_END_STOCK = "EndStock_";

  public static final String SFX_QUANTITY = "_qty";
  public static final String SFX_WEIGHT = "_wgt";

  public static final int DPW_MIN = 5;
  public static final int DPW_MAX = 7;

  public static void register() {
    EnumUtils.register(TradeActKind.class);
    EnumUtils.register(TradeActTimeUnit.class);
  }

  private TradeActConstants() {
  }
}
