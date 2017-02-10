package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

public final class TradeConstants {

  public static void register() {
    EnumUtils.register(OperationType.class);
    EnumUtils.register(TradeDocumentPhase.class);

    EnumUtils.register(TradeVatMode.class);
    EnumUtils.register(TradeDiscountMode.class);

    EnumUtils.register(TradeCostBasis.class);
  }

  public static final String PRM_ERP_REFRESH_INTERVAL = "ERPRefreshIntervalInMinutes";
  public static final String PRM_OVERDUE_INVOICES = "OverdueInvoices";

  public static final String SVC_ITEMS_INFO = "ItemsInfo";
  public static final String SVC_CREDIT_INFO = "CreditInfo";
  public static final String SVC_GET_DOCUMENT_DATA = "getTradeDocumentData";
  public static final String SVC_SEND_TO_ERP = "SendToERP";
  public static final String SVC_REMIND_DEBTS_EMAIL = "RemindDebtsEmail";

  public static final String SVC_GET_DOCUMENT_TYPE_CAPTION_AND_FILTER =
      "getTradeDocumentTypeCaptionAndFilter";

  public static final String SVC_DOCUMENT_PHASE_TRANSITION = "TradeDocumentPhaseTransition";
  public static final String SVC_REBUILD_STOCK = "RebuildStock";
  public static final String SVC_CALCULATE_COST = "CalculateCost";

  public static final String TBL_PURCHASES = "Purchases";
  public static final String TBL_PURCHASE_USAGE = "PurchaseUsage";
  public static final String TBL_PURCHASE_ITEMS = "PurchaseItems";
  public static final String TBL_SALES = "Sales";
  public static final String TBL_SALES_SERIES = "SaleSeries";
  public static final String TBL_SALE_ITEMS = "SaleItems";

  public static final String TBL_TRADE_OPERATIONS = "TradeOperations";
  public static final String TBL_TRADE_SERIES = "TradeSeries";
  public static final String TBL_TRADE_STATUSES = "TradeStatuses";

  public static final String TBL_TRADE_DOCUMENTS = "TradeDocuments";
  public static final String TBL_TRADE_DOCUMENT_ITEMS = "TradeDocumentItems";
  public static final String TBL_TRADE_ITEM_COST = "TradeItemCost";
  public static final String TBL_TRADE_STOCK = "TradeStock";

  public static final String TBL_TRADE_TYPE_OPERATIONS = "TradeTypeOperations";
  public static final String TBL_TRADE_TYPE_STATUSES = "TradeTypeStatuses";
  public static final String TBL_TRADE_TYPE_TAGS = "TradeTypeTags";

  public static final String TBL_EXPENDITURE_TYPES = "ExpenditureTypes";
  public static final String TBL_TRADE_EXPENDITURES = "TradeExpenditures";

  public static final String VAR_VIEW_NAME = "view_name";
  public static final String VAR_ID_LIST = "IdList";
  public static final String VAR_SUBJECT = "Subject";
  public static final String VAR_HEADER = "Header";
  public static final String VAR_FOOTER = "Footer";

  public static final String VIEW_DEBTS = "Debts";
  public static final String VIEW_DEBT_REPORTS = "DebtReports";
  public static final String VIEW_DEBT_REMINDER_TEMPLATE = "DebtReminderTemplates";
  public static final String VIEW_PURCHASE_OPERATIONS = "PurchaseOperations";
  public static final String VIEW_SALE_OPERATIONS = "SaleOperations";

  public static final String VIEW_PURCHASES = "Purchases";
  public static final String VIEW_PURCHASE_ITEMS = "PurchaseItems";
  public static final String VIEW_SALES = "Sales";
  public static final String VIEW_SALE_ITEMS = "SaleItems";
  public static final String VIEW_SALE_FILES = "SaleFiles";

  public static final String VIEW_TRADE_NOTES = "TradeNotes";
  public static final String VIEW_TRADE_OPERATIONS = "TradeOperations";
  public static final String VIEW_TRADE_SERIES = "TradeSeries";
  public static final String VIEW_TRADE_STATUSES = "TradeStatuses";
  public static final String VIEW_TRADE_TAGS = "TradeTags";

  public static final String VIEW_SERIES_MANAGERS = "SeriesManagers";

  public static final String VIEW_TRADE_DOCUMENTS = "TradeDocuments";
  public static final String VIEW_TRADE_DOCUMENT_ITEMS = "TradeDocumentItems";
  public static final String VIEW_TRADE_ITEM_COST = "TradeItemCost";
  public static final String VIEW_TRADE_PAYMENTS = "TradePayments";
  public static final String VIEW_TRADE_STOCK = "TradeStock";

  public static final String VIEW_TRADE_DOCUMENT_TYPES = "TradeDocumentTypes";
  public static final String VIEW_TRADE_DOCUMENT_TAGS = "TradeDocumentTags";

  public static final String VIEW_EXPENDITURE_TYPES = "ExpenditureTypes";
  public static final String VIEW_TRADE_EXPENDITURES = "TradeExpenditures";

  public static final String VIEW_TRADE_MOVEMENT = "TradeMovement";

  public static final String COL_PURCHASE = "Purchase";

  public static final String COL_SALE = "Sale";
  public static final String COL_SALE_PROFORMA = "Proforma";
  public static final String COL_SALE_PAYER = "Payer";

  public static final String COL_TRADE_DATE = "Date";
  public static final String COL_TRADE_NUMBER = "Number";
  public static final String COL_TRADE_OPERATION = "Operation";
  public static final String COL_TRADE_INVOICE_PREFIX = "InvoicePrefix";
  public static final String COL_TRADE_SALE_SERIES = "SaleSeries";
  public static final String COL_TRADE_INVOICE_NO = "InvoiceNo";
  public static final String COL_TRADE_AMOUNT = "Amount";
  public static final String COL_TRADE_CURRENCY = "Currency";
  public static final String COL_TRADE_PAYMENT_TIME = "PaymentTime";
  public static final String COL_TRADE_PAID = "Paid";
  public static final String COL_TRADE_WAREHOUSE_FROM = "WarehouseFrom";
  public static final String COL_TRADE_WAREHOUSE_TO = "WarehouseTo";
  public static final String COL_TRADE_SUPPLIER = "Supplier";
  public static final String COL_TRADE_CUSTOMER = "Customer";
  public static final String COL_TRADE_PAYER = "Payer";
  public static final String COL_TRADE_MANAGER = "Manager";
  public static final String COL_TRADE_TERM = "Term";
  public static final String COL_TRADE_NOTES = "Notes";
  public static final String COL_TRADE_EXPORTED = "Exported";
  public static final String COL_TRADE_KIND = "Kind";
  public static final String COL_TRADE_DEBT = "Debt";
  public static final String COL_TRADE_DEBT_COUNT = "DebtCount";
  public static final String COL_TRADE_CHECK_NO = "CheckNo";
  public static final String COL_TRADE_JOIN = "Join";

  public static final String COL_TRADE_BOL_SERIES = "BoLSeries";
  public static final String COL_TRADE_BOL_NUMBER = "BoLNumber";
  public static final String COL_TRADE_BOL_ISSUE_DATE = "BoLIssueDate";
  public static final String COL_TRADE_BOL_DEPARTURE_DATE = "BoLDepartureDate";
  public static final String COL_TRADE_BOL_UNLOADING_DATE = "BoLUnloadingDate";
  public static final String COL_TRADE_BOL_LOADING = "BoLLoading";
  public static final String COL_TRADE_BOL_UNLOADING = "BoLUnloading";
  public static final String COL_TRADE_BOL_DRIVER_TAB_NO = "BoLDriverTabNo";
  public static final String COL_TRADE_BOL_VEHICLE_NUMBER = "BoLVehicleNumber";
  public static final String COL_TRADE_BOL_DRIVER = "BoLDriver";
  public static final String COL_TRADE_BOL_CARRIER = "BoLCarrier";

  public static final String COL_TRADE_VAT_PLUS = "VatPlus";
  public static final String COL_TRADE_VAT = "Vat";
  public static final String COL_TRADE_VAT_PERC = "VatPercent";

  public static final String COL_TRADE_DISCOUNT = "Discount";
  public static final String COL_TRADE_DISCOUNT_PERC = "DiscountPercent";

  public static final String COL_TRADE_ITEM_ORDINAL = "Ordinal";
  public static final String COL_TRADE_ITEM_ARTICLE = "Article";
  public static final String COL_TRADE_ITEM_QUANTITY = "Quantity";
  public static final String COL_TRADE_ITEM_PRICE = "Price";
  public static final String COL_TRADE_ITEM_NOTE = "Note";
  public static final String COL_TRADE_ITEM_PARENT = "Parent";
  public static final String COL_TRADE_ITEM_WAREHOUSE_FROM = "ItemWarehouseFrom";
  public static final String COL_TRADE_ITEM_WAREHOUSE_TO = "ItemWarehouseTo";
  public static final String COL_TRADE_ITEM_EMPLOYEE = "Employee";

  public static final String COL_SERIES = "Series";
  public static final String COL_SERIES_NAME = "SeriesName";
  public static final String COL_SERIES_MANAGER = "Manager";
  public static final String COL_SERIES_DEFAULT = "IsDefault";

  public static final String COL_OPERATION_NAME = "OperationName";
  public static final String COL_OPERATION_TYPE = "OperationType";
  public static final String COL_OPERATION_KIND = "Kind";
  public static final String COL_OPERATION_PRICE = "Price";
  public static final String COL_OPERATION_VAT_MODE = "OperationVatMode";
  public static final String COL_OPERATION_DISCOUNT_MODE = "OperationDiscountMode";
  public static final String COL_OPERATION_WAREHOUSE_FROM = "WarehouseFrom";
  public static final String COL_OPERATION_WAREHOUSE_TO = "WarehouseTo";
  public static final String COL_OPERATION_DEFAULT = "IsDefault";
  public static final String COL_OPERATION_CASH_REGISTER_NO = "CashRegisterNo";

  public static final String COL_STATUS_NAME = "StatusName";
  public static final String COL_STATUS_ACTIVE = "StatusActive";
  public static final String COL_TEMPLATE_NAME = "Name";
  public static final String COL_TEMPLATE_SUBJECT = "Subject";
  public static final String COL_TEMPLATE_FIRST_PARAGRAPH = "FirstParagraph";
  public static final String COL_TEMPLATE_LAST_PARAGRAPH = "LastParagraph";

  public static final String COL_TRADE_DOCUMENT_PHASE = "Phase";
  public static final String COL_TRADE_DOCUMENT_STATUS = "Status";
  public static final String COL_TRADE_DOCUMENT_NUMBER_1 = "Number1";
  public static final String COL_TRADE_DOCUMENT_NUMBER_2 = "Number2";
  public static final String COL_TRADE_DOCUMENT_RECEIVED_DATE = "ReceivedDate";

  public static final String COL_TRADE_DOCUMENT_DISCOUNT = "DocumentDiscount";
  public static final String COL_TRADE_DOCUMENT_PRICE_NAME = "PriceName";
  public static final String COL_TRADE_DOCUMENT_VAT_MODE = "DocumentVatMode";
  public static final String COL_TRADE_DOCUMENT_DISCOUNT_MODE = "DocumentDiscountMode";

  public static final String COL_TRADE_DOCUMENT = "TradeDocument";
  public static final String COL_TRADE_DOCUMENT_ITEM = "TradeDocumentItem";

  public static final String COL_TRADE_DOCUMENT_ITEM_DISCOUNT = "Discount";
  public static final String COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT = "DiscountIsPercent";
  public static final String COL_TRADE_DOCUMENT_ITEM_VAT = "Vat";
  public static final String COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT = "VatIsPercent";

  public static final String COL_DOCUMENT_TYPE_NAME = "DocumentTypeName";
  public static final String COL_DOCUMENT_TYPE = "DocumentType";

  public static final String COL_TRADE_TAG = "TradeTag";
  public static final String COL_TAG_NAME = "TagName";

  public static final String COL_PRIMARY_DOCUMENT_ITEM = "PrimaryDocumentItem";
  public static final String COL_STOCK_QUANTITY = "Quantity";
  public static final String COL_STOCK_WAREHOUSE = "Warehouse";

  public static final String COL_TRADE_PAYMENT_DATE = "PaymentDate";
  public static final String COL_TRADE_PAYMENT_AMOUNT = "PaymentAmount";
  public static final String COL_TRADE_PAYMENT_ACCOUNT = "PaymentAccount";
  public static final String COL_TRADE_PAYMENT_SERIES = "PaymentSeries";
  public static final String COL_TRADE_PAYMENT_NUMBER = "PaymentNumber";

  public static final String COL_TRADE_PAYMENT_TYPE_ACCOUNT = "PaymentTypeAccount";

  public static final String COL_EXPENDITURE_TYPE_NAME = "ExpenditureTypeName";
  public static final String COL_EXPENDITURE_TYPE_DEBIT = "Debit";
  public static final String COL_EXPENDITURE_TYPE_CREDIT = "Credit";
  public static final String COL_EXPENDITURE_TYPE_COST_BASIS = "CostBasis";
  public static final String COL_EXPENDITURE_TYPE_SUPPLIER = "Supplier";
  public static final String COL_EXPENDITURE_TYPE_OPERATION = "Operation";
  public static final String COL_EXPENDITURE_TYPE_WAREHOUSE = "Warehouse";
  public static final String COL_EXPENDITURE_TYPE_ITEM = "Item";

  public static final String COL_EXPENDITURE_TYPE = "ExpenditureType";
  public static final String COL_EXPENDITURE_DATE = "Date";
  public static final String COL_EXPENDITURE_AMOUNT = "Amount";
  public static final String COL_EXPENDITURE_CURRENCY = "Currency";
  public static final String COL_EXPENDITURE_VAT = "Vat";
  public static final String COL_EXPENDITURE_VAT_IS_PERCENT = "VatIsPercent";
  public static final String COL_EXPENDITURE_SERIES = "Series";
  public static final String COL_EXPENDITURE_NUMBER = "Number";
  public static final String COL_EXPENDITURE_SUPPLIER = "Supplier";
  public static final String COL_EXPENDITURE_GENERATED_DOCUMENT = "GeneratedDocument";

  public static final String COL_TRADE_ITEM_COST = "Cost";
  public static final String COL_TRADE_ITEM_COST_CURRENCY = "Currency";

  public static final String ALS_TRADE_BOL_SERIES = "BoLSeriesName";
  public static final String ALS_TRADE_BOL_DRIVER_TAB_NO = "DriverTabNo";
  public static final String ALS_TRADE_BOL_DRIVER_EMPLOYEES = "DriverEmployees";

  public static final String ALS_CUSTOMER_NAME = "CustomerName";
  public static final String ALS_PAYER_NAME = "PayerName";
  public static final String ALS_SUPPLIER_NAME = "SupplierName";

  public static final String ALS_OPERATION_BACKGROUND = "OperationBackground";
  public static final String ALS_OPERATION_FOREGROUND = "OperationForeground";
  public static final String ALS_STATUS_BACKGROUND = "StatusBackground";
  public static final String ALS_STATUS_FOREGROUND = "StatusForeground";

  public static final String ALS_OVERDUE_COUNT = "OverdueCount";
  public static final String ALS_OVERDUE_SUM = "OverdueSum";
  public static final String ALS_SALES_COUNT = "SalesCount";
  public static final String ALS_SALES_SUM = "SalesSum";
  public static final String ALS_TRADE_STATUS_NAME = "TradeStatusName";
  public static final String ALS_TRADE_STATUS = "TradeStatus";

  public static final String ALS_EXPENDITURE_TYPE_SUPPLIER = "TypeSupplier";

  public static final String ALS_WAREHOUSE_FROM_CODE = "WarehouseFromCode";
  public static final String ALS_STOCK_PRIMARY_DATE = "PrimaryDate";

  public static final String ALS_COST_CURRENCY = "CostCurrency";
  public static final String ALS_PARENT_COST = "ParentCost";
  public static final String ALS_PARENT_COST_CURRENCY = "ParentCostCurrency";

  public static final String PROP_REMIND_EMAIL = "RemindEmail";
  public static final String PROP_OVERALL_TOTAL = "OveralTotal";
  public static final String PROP_STOCK = "Stock";

  public static final String VAR_TOTAL = "Total";
  public static final String VAR_DEBT = "Debt";
  public static final String VAR_OVERDUE = "Overdue";

  public static final String GRID_TRADE_OPERATIONS = "TradeOperations";
  public static final String GRID_TRADE_STATUSES = "TradeStatuses";
  public static final String GRID_TRADE_TAGS = "TradeTags";

  public static final String GRID_SERIES_MANAGERS = "SeriesManagers";
  public static final String GRID_DEBTS = "Debts";
  public static final String GRID_DEBT_REPORTS = "DebtReports";
  public static final String GRID_SALES = "Sales";

  public static final String FORM_DEBT_REPORT_TEMPLATE = "DebtReportTemplate";

  public static final String GRID_TRADE_DOCUMENTS = "TradeDocuments";
  public static final String GRID_TRADE_DOCUMENT_ITEMS = "TradeDocumentItems";
  public static final String GRID_TRADE_PAYMENTS = "TradePayments";
  public static final String GRID_TRADE_DOCUMENT_FILES = "TradeDocumentFiles";

  public static final String GRID_TRADE_STOCK = "TradeStock";

  public static final String GRID_EXPENDITURE_TYPES = "ExpenditureTypes";
  public static final String GRID_TRADE_EXPENDITURES = "TradeExpenditures";

  public static final String GRID_ITEM_MOVEMENT = "ItemMovement";

  public static final String FORM_SALES_INVOICE = "SalesInvoice";
  public static final String FORM_PRINT_SALES_INVOICE = "PrintSalesInvoice";

  public static final String FORM_TRADE_DOCUMENT = "TradeDocument";

  public static final int MAX_STOCK_DEPTH = 1_000;

  public static String keyStockWarehouse(String warehouseCode) {
    return PROP_STOCK + BeeUtils.trim(warehouseCode);
  }

  private TradeConstants() {
  }
}
