package com.butent.bee.shared.modules.classifiers;

import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.EnumUtils;

public final class ClassifierConstants {

  public static final String SVC_CREATE_COMPANY = "create_company";
  public static final String SVC_COMPANY_INFO = "GetCompanyInfo";
  public static final String SVC_GENERATE_QR_CODE = "GenerateQrCode";
  public static final String SVC_GENERATE_QR_CODE_PERSON = "GenerateQrCodePerson";

  public static final String SVC_GET_COMPANY_TYPE_REPORT = "get_company_type_report";
  public static final String TIMER_REMIND_COMPANY_ACTIONS = "timer_remind_company_actions";

  public static final String TBL_ITEMS = "Items";
  public static final String TBL_UNITS = "Units";

  public static final String TBL_ITEM_CATEGORY_TREE = "CategoryTree";
  public static final String TBL_ITEM_CATEGORIES = "ItemCategories";

  public static final String TBL_CONTACTS = "Contacts";
  public static final String TBL_EMAILS = "Emails";

  public static final String TBL_COMPANY_PERSONS = "CompanyPersons";
  public static final String TBL_COMPANY_USERS = "CompanyUsers";
  public static final String TBL_COMPANIES = "Companies";
  public static final String TBL_COMPANY_TYPES = "CompanyTypes";
  public static final String TBL_COMPANY_CONTACTS = "CompanyContacts";
  public static final String TBL_COMPANY_OBJECTS = "CompanyObjects";

  public static final String TBL_COMPANY_RELATION_TYPES = "CompanyRelationTypes";
  public static final String TBL_COMPANY_RELATION_TYPE_STORE = "CompRelTypeStore";

  public static final String TBL_COMPANY_ACTIVITY_STORE = "CompActStore";

  public static final String TBL_PERSONS = "Persons";
  public static final String TBL_POSITIONS = "Positions";

  public static final String TBL_CITIES = "Cities";
  public static final String TBL_COUNTRIES = "Countries";

  public static final String TBL_BANKS = "Banks";
  public static final String TBL_COMPANY_BANK_ACCOUNTS = "CompanyBankAccounts";

  public static final String TBL_BRANCHES = "Branches";
  public static final String TBL_WAREHOUSES = "Warehouses";

  public static final String TBL_HOLIDAYS = "Holidays";

  public static final String FORM_COMPANY = "Company";
  public static final String FORM_COMPANY_ACTION = "CompanyAction";
  public static final String FORM_PERSON = "Person";
  public static final String FORM_NEW_COMPANY = "NewCompany";
  public static final String FORM_COMPANY_PERSON = "CompanyPerson";

  public static final String VIEW_COMPANIES = "Companies";
  public static final String VIEW_COMPANY_PERSONS = "CompanyPersons";
  public static final String VIEW_PERSONS = "Persons";
  public static final String VIEW_POSITIONS = "Positions";

  public static final String VIEW_ITEMS = "Items";
  public static final String VIEW_ITEM_CATEGORY_TREE = "CategoryTree";
  public static final String VIEW_ITEM_CATEGORIES = "ItemCategories";

  public static final String VIEW_COUNTRIES = "Countries";
  public static final String VIEW_CITIES = "Cities";
  public static final String VIEW_BRANCHES = "Branches";
  public static final String VIEW_WAREHOUSES = "Warehouses";

  public static final String VIEW_COMPANY_TYPES = "CompanyTypes";
  public static final String VIEW_COMPANY_GROUPS = "CompanyGroups";
  public static final String VIEW_COMPANY_PRIORITIES = "CompanyPriorities";
  public static final String VIEW_COMPANY_RELATION_TYPES = "CompanyRelationTypes";
  public static final String VIEW_COMPANY_RELATION_TYPE_STORE = "CompRelTypeStore";
  public static final String VIEW_RELATION_TYPE_STATES = "RelationTypeStates";
  public static final String VIEW_FINANCIAL_STATES = "FinancialStates";
  public static final String VIEW_COMPANY_SIZES = "CompanySizes";
  public static final String VIEW_INFORMATION_SOURCES = "InformationSources";
  public static final String VIEW_COMPANY_ACTIVITIES = "CompanyActivities";
  public static final String VIEW_COMPANY_ACTIVITY_STORE = "CompActStore";
  public static final String VIEW_COMPANY_BANK_ACCOUNTS = "CompanyBankAccounts";

  public static final String VIEW_COMPANY_CONTACTS = "CompanyContacts";
  public static final String VIEW_COMPANY_DEPARTMENTS = "CompanyDepartments";
  public static final String VIEW_COMPANY_USERS = "CompanyUsers";
  public static final String VIEW_COMPANY_OBJECTS = "CompanyObjects";

  public static final String VIEW_HOLIDAYS = "Holidays";

  public static final String GRID_PERSONS = "Persons";
  public static final String GRID_COMPANIES = "Companies";
  public static final String GRID_COMPANY_BANK_ACCOUNTS = "CompanyBankAccounts";

  public static final String GRID_COMPANY_USERS = "CompanyUsers";
  public static final String GRID_ITEMS = "Items";

  public static final String COL_COMPANY = "Company";
  public static final String COL_COMPANY_NAME = "Name";
  public static final String COL_COMPANY_CODE = "Code";
  public static final String COL_COMPANY_VAT_CODE = "VATCode";
  public static final String COL_COMPANY_PERSON = "CompanyPerson";
  public static final String COL_COMPANY_TYPE = "CompanyType";
  public static final String COL_COMPANY_GROUP = "CompanyGroup";
  public static final String COL_COMPANY_PRIORITY = "CompanyPriority";
  public static final String COL_COMPANY_EXCHANGE_CODE = "ExchangeCode";
  public static final String COL_COMPANY_CREDIT_LIMIT = "CreditLimit";
  public static final String COL_COMPANY_LIMIT_CURRENCY = "LimitCurrency";
  public static final String COL_COMPANY_CREDIT_DAYS = "CreditDays";
  public static final String COL_COMPANY_RELATION_TYPE_STATE = "CompanyRelationTypeState";
  public static final String COL_COMPANY_FINANCIAL_STATE = "FinancialState";
  public static final String COL_COMPANY_SIZE = "CompanySize";
  public static final String COL_COMPANY_INFORMATION_SOURCE = "InformationSource";

  public static final String COL_PERSON = "Person";
  public static final String COL_FIRST_NAME = "FirstName";
  public static final String COL_LAST_NAME = "LastName";
  public static final String COL_PHOTO = "Photo";
  public static final String COL_DATE_OF_BIRTH = "DateOfBirth";

  public static final String COL_CATEGORY = "Category";
  public static final String COL_CATEGORY_NAME = "Name";

  public static final String COL_ITEM = "Item";
  public static final String COL_ITEM_NAME = "Name";
  public static final String COL_ITEM_ARTICLE = "Article";
  public static final String COL_ITEM_BARCODE = "Barcode";
  public static final String COL_ITEM_IS_SERVICE = "IsService";
  public static final String COL_ITEM_EXTERNAL_CODE = "ExternalCode";
  public static final String COL_ITEM_PRICE = "Price";
  public static final String COL_ITEM_CURRENCY = "Currency";
  public static final String COL_ITEM_COST = "Cost";
  public static final String COL_ITEM_COST_CURRENCY = "CostCurrency";
  public static final String COL_ITEM_PRICE_1 = "Price1";
  public static final String COL_ITEM_CURRENCY_1 = "Currency1";
  public static final String COL_ITEM_PRICE_2 = "Price2";
  public static final String COL_ITEM_CURRENCY_2 = "Currency2";
  public static final String COL_ITEM_PRICE_3 = "Price3";
  public static final String COL_ITEM_CURRENCY_3 = "Currency3";
  public static final String COL_ITEM_TYPE = "Type";
  public static final String COL_ITEM_GROUP = "Group";
  public static final String COL_ITEM_WEIGHT = "Weight";
  public static final String COL_ITEM_AREA = "Area";
  public static final String COL_ITEM_ORDINAL = "Ordinal";
  public static final String COL_ITEM_DPW = "DaysPerWeek";
  public static final String COL_ITEM_MIN_TERM = "MinTerm";

  public static final String COL_UNIT = "Unit";
  public static final String COL_UNIT_NAME = "Name";
  public static final String COL_TIME_UNIT = "TimeUnit";
  public static final String COL_BASE_UNIT = "BaseUnit";
  public static final String COL_UNIT_FACTOR = "Factor";

  public static final String COL_CONTACT = "Contact";
  public static final String COL_PHONE = "Phone";
  public static final String COL_MOBILE = "Mobile";
  public static final String COL_FAX = "Fax";
  public static final String COL_EMAIL = "Email";
  public static final String COL_EMAIL_ADDRESS = "Email";
  public static final String COL_ADDRESS = "Address";
  public static final String COL_POST_INDEX = "PostIndex";
  public static final String COL_WEBSITE = "Website";
  public static final String COL_NOTES = "Notes";
  public static final String COL_REMIND_EMAIL = "RemindEmail";
  public static final String COL_REMIND_ACTIONS = "MailCompanyActions";
  public static final String COL_REMIND_ACTION_BEFORE = "CompanyActionRemindBefore";
  public static final String COL_EMAIL_INVOICES = "EmailInvoices";

  public static final String COL_CITY = "City";
  public static final String COL_CITY_NAME = "Name";
  public static final String COL_COUNTRY = "Country";
  public static final String COL_COUNTRY_NAME = "Name";
  public static final String COL_COUNTRY_CODE = "Code";

  public static final String COL_COMPANY_USER_COMPANY = "Company";
  public static final String COL_COMPANY_USER_USER = "User";
  public static final String COL_COMPANY_USER_RESPONSIBILITY = "Responsibility";
  public static final String COL_POSITION = "Position";
  public static final String COL_POSITION_NAME = "Name";

  public static final String COL_BRANCH_NAME = "Name";
  public static final String COL_BRANCH_CODE = "Code";
  public static final String COL_BRANCH_PRIMARY_WAREHOUSE = "PrimaryWarehouse";

  public static final String COL_DEFAULT_BANK_ACCOUNT = "DefaultBankAccount";
  public static final String COL_DEFAULT_COMPANY_USER = "DefaultCompanyUser";

  public static final String COL_WAREHOUSE = "Warehouse";
  public static final String COL_WAREHOUSE_CODE = "Code";
  public static final String COL_WAREHOUSE_NAME = "Name";
  public static final String COL_WAREHOUSE_SUPPLIER_CODE = "SupplierCode";
  public static final String COL_WAREHOUSE_BRANCH = "Branch";

  public static final String COL_RELATION_TYPE = "RelationType";
  public static final String COL_RELATION_TYPE_NAME = "Name";

  public static final String COL_ACTIVITY = "Activity";

  public static final String COL_COMPANY_TYPE_NAME = "Name";

  public static final String COL_COMPANY_OBJECT_NAME = "ObjectName";
  public static final String COL_COMPANY_OBJECT_ADDRESS = "ObjectAddress";

  public static final String COL_BANK = "Bank";
  public static final String COL_BANK_NAME = "Name";
  public static final String COL_BANK_CODE = "BankCode";
  public static final String COL_SWIFT_CODE = "SWIFTCode";

  public static final String COL_BANK_ACCOUNT = "BankAccount";

  public static final String COL_HOLY_COUNTRY = "Country";
  public static final String COL_HOLY_DAY = "Date";

  public static final String ALS_COMPANY_NAME = "CompanyName";
  public static final String ALS_COMPANY_CODE = "CompanyCode";
  public static final String ALS_COMPANY_TYPE = "CompanyType";
  public static final String ALS_COMPANY_TYPE_NAME = "TypeName";

  public static final String ALS_CONTACT_FIRST_NAME = "ContactFirstName";
  public static final String ALS_CONTACT_LAST_NAME = "ContactLastName";
  public static final String ALS_CONTACT_PERSON = "ContactPerson";
  public static final String ALS_CONTACT_COMPANY_NAME = "ContactCompanyName";

  public static final String ALS_CITY_NAME = "CityName";
  public static final String ALS_COUNTRY_NAME = "CountryName";

  public static final String ALS_POSITION_NAME = "PositionName";
  public static final String ALS_PRIMARY_POSITION = "PrimaryPosition";
  public static final String ALS_PRIMARY_POSITION_NAME = "PrimaryPositionName";

  public static final String ALS_EMAIL_ID = "EmailId";

  public static final String ALS_CATEGORY_PARENT_NAME = "ParentName";

  public static final String ALS_BANK_NAME = "BankName";

  public static final String ALS_ITEM_NAME = "ItemName";
  public static final String ALS_ITEM_VAT = "ItemVat";
  public static final String ALS_ITEM_VAT_PERCENT = "ItemVatPercent";

  public static final String ALS_UNIT_NAME = "UnitName";

  public static final String ALS_WAREHOUSE_CODE = "WarehouseCode";

  public static final String ALS_PARENT_TYPE_NAME = "ParentTypeName";
  public static final String ALS_ITEM_TYPE_NAME = "ItemTypeName";
  public static final String ALS_PARENT_GROUP_NAME = "ParentGroupName";
  public static final String ALS_ITEM_GROUP_NAME = "ItemGroupName";

  public static final String ALS_ITEM_TYPES = "ItemTypes";
  public static final String ALS_ITEM_GROUPS = "ItemGroups";

  public static final String PROP_COMPANY_NAMES = "CompanyNames";
  public static final String PROP_COMPANY_IDS = "CompanyIds";

  public static final String FILTER_COMPANY_CREATION_AND_TYPE = "company_creation_and_type";
  public static final String FILTER_COMPANY_USAGE = "company_usage";

  public static final String QR_FLOW_PANEL = "qrFlowPanel";
  public static final String QR_TYPE = "qrType";
  public static final String QR_COMPANY = "qrCompany";
  public static final String QR_PERSON = "qrPerson";

  public static final long DEFAULT_REMIND_ACTIONS_TIMER_TIMEOUT = TimeUtils.MILLIS_PER_HOUR;

  public static void register() {
    EnumUtils.register(ItemPrice.class);
  }

  private ClassifierConstants() {
  }
}
