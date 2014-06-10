package com.butent.bee.shared.modules.transport;

import com.google.common.collect.Maps;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class TransportConstants {

  public enum AssessmentStatus implements HasLocalizedCaption {
    NEW {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trAssessmentStatusNew();
      }
    },
    ANSWERED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trAssessmentStatusAnswered();
      }
    },
    LOST {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trAssessmentStatusLost();
      }
    },
    APPROVED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trAssessmentStatusApproved();
      }
    };

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }

    public boolean is(Integer status) {
      return status != null && ordinal() == status;
    }
  }

  public enum TranspRegStatus implements HasCaption {
    NEW(Localized.getConstants().trRegistrationStatusNew()),
    CONFIRMED(Localized.getConstants().trRegistrationStatusConfirmed()),
    REJECTED(Localized.getConstants().trRegistrationStatusRejected());

    private final String caption;

    private TranspRegStatus(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public enum CargoRequestStatus implements HasCaption {
    NEW(Localized.getConstants().trRequestStatusNew()),
    ACTIVE(Localized.getConstants().trRequestStatusActive()),
    REJECTED(Localized.getConstants().trRequestStatusRejected()),
    FINISHED(Localized.getConstants().trRequestStatusFinished());

    private final String caption;

    private CargoRequestStatus(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public enum FuelSeason implements HasLocalizedCaption {
    SUMMER {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.summer();
      }
    },
    WINTER {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.winter();
      }
    };

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }
  }

  public enum ImportType implements HasLocalizedCaption {
    COSTS {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trImportCosts();
      }

      @Override
      protected void init() {
        LocalizableConstants locale = Localized.getConstants();
        addProperty(new ImportProperty(VAR_IMPORT_START_ROW, locale.startRow()));
        addProperty(new ImportProperty(VAR_IMPORT_DATE_FORMAT, locale.dateFormat()));
        addProperty(new ImportProperty(COL_VEHICLE, locale.trVehicle(),
            TBL_VEHICLES, COL_VEHICLE_NUMBER));
        addProperty(new ImportProperty(COL_COSTS_DATE, locale.date()));
        addProperty(new ImportProperty(COL_COSTS_ITEM, locale.itemOrService(),
            ClassifierConstants.TBL_ITEMS, ClassifierConstants.COL_ITEM_NAME));
        addProperty(new ImportProperty(COL_COSTS_QUANTITY, locale.quantity()));
        addProperty(new ImportProperty(COL_COSTS_PRICE, locale.price()));
        addProperty(new ImportProperty(COL_COSTS_CURRENCY, locale.currency(),
            AdministrationConstants.TBL_CURRENCIES, AdministrationConstants.COL_CURRENCY_NAME));
        addProperty(new ImportProperty(TradeConstants.COL_TRADE_VAT_PLUS, locale.vatPlus()));
        addProperty(new ImportProperty(COL_COSTS_VAT, locale.vat()));
        addProperty(new ImportProperty(TradeConstants.COL_TRADE_VAT_PERC, locale.vatPercent()));
        addProperty(new ImportProperty(COL_AMOUNT, locale.amount()));
        addProperty(new ImportProperty(COL_COSTS_SUPPLIER, locale.supplier(),
            ClassifierConstants.TBL_COMPANIES, ClassifierConstants.COL_COMPANY_NAME));
        addProperty(new ImportProperty(COL_NUMBER, locale.number()));
        addProperty(new ImportProperty(COL_COSTS_COUNTRY, locale.country(),
            ClassifierConstants.TBL_COUNTRIES, ClassifierConstants.COL_COUNTRY_NAME));
        addProperty(new ImportProperty(COL_COSTS_NOTE, locale.notes()));
        addProperty(new ImportProperty(COL_COSTS_EXTERNAL_ID, locale.externalId()));
      }
    },
    TRACKING {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trImportTracking();
      }

      @Override
      protected void init() {
        LocalizableConstants locale = Localized.getConstants();
        addProperty(new ImportProperty(VAR_IMPORT_LOGIN, locale.loginUserName()));
        addProperty(new ImportProperty(VAR_IMPORT_PASSWORD, locale.loginPassword()));
        addProperty(new ImportProperty(COL_VEHICLE, locale.trVehicle(),
            TBL_VEHICLES, COL_VEHICLE_NUMBER));
        addProperty(new ImportProperty(ClassifierConstants.COL_COUNTRY, locale.country(),
            ClassifierConstants.TBL_COUNTRIES, ClassifierConstants.COL_COUNTRY_NAME));
      }
    };

    public static class ImportProperty {
      private final String name;
      private final String caption;
      private String relTable;
      private String relField;

      public ImportProperty(String name, String caption) {
        Assert.notEmpty(name);
        this.name = name;
        this.caption = BeeUtils.notEmpty(caption, name);
      }

      public ImportProperty(String name, String caption, String relTable, String relField) {
        this(name, caption);
        Assert.notEmpty(relTable);
        Assert.notEmpty(relField);

        this.relTable = relTable;
        this.relField = relField;
      }

      public String getCaption() {
        return caption;
      }

      public String getName() {
        return name;
      }

      public String getRelField() {
        return relField;
      }

      public String getRelTable() {
        return relTable;
      }
    }

    private final Map<String, ImportProperty> properties = Maps.newLinkedHashMap();

    private ImportType() {
      init();
    }

    protected void addProperty(ImportProperty property) {
      properties.put(property.getName(), property);
    }

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }

    public Collection<ImportProperty> getProperties() {
      return Collections.unmodifiableCollection(properties.values());
    }

    public ImportProperty getProperty(String name) {
      return properties.get(name);
    }

    protected abstract void init();
  }

  public enum OrderStatus implements HasLocalizedCaption {
    REQUEST {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trOrderStatusRequest();
      }
    },
    ACTIVE {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trOrderStatusActive();
      }
    },
    CANCELED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trOrderStatusCanceled();
      }
    },
    COMPLETED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.trOrderStatusCompleted();
      }
    };

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }

    public boolean is(Integer status) {
      return status != null && ordinal() == status;
    }
  }

  public enum TripStatus implements HasCaption {
    NEW(Localized.getConstants().trTripStatusNew(), true),
    ACTIVE(Localized.getConstants().trTripStatusActive(), true),
    CANCELED(Localized.getConstants().trTripStatusCanceled(), false),
    COMPLETED(Localized.getConstants().trTripStatusCompleted(), false);

    private final String caption;
    private final boolean editable;

    private TripStatus(String caption, boolean editable) {
      this.caption = caption;
      this.editable = editable;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    public boolean isEditable() {
      return editable;
    }
  }

  public enum VehicleType {
    TRUCK {
      @Override
      public String getTripVehicleIdColumnName() {
        return COL_VEHICLE;
      }

      @Override
      public String getTripVehicleNumberColumnName() {
        return COL_VEHICLE + COL_VEHICLE_NUMBER;
      }
    },
    TRAILER {
      @Override
      public String getTripVehicleIdColumnName() {
        return COL_TRAILER;
      }

      @Override
      public String getTripVehicleNumberColumnName() {
        return COL_TRAILER + COL_VEHICLE_NUMBER;
      }
    };

    public abstract String getTripVehicleIdColumnName();

    public abstract String getTripVehicleNumberColumnName();
  }

  public static void register() {
    EnumUtils.register(AssessmentStatus.class);
    EnumUtils.register(TripStatus.class);
    EnumUtils.register(OrderStatus.class);

    EnumUtils.register(TranspRegStatus.class);
    EnumUtils.register(CargoRequestStatus.class);

    EnumUtils.register(FuelSeason.class);
    EnumUtils.register(ImportType.class);
  }

  public static final String SVC_GET_BEFORE = "GetBeforeData";
  public static final String SVC_GET_UNASSIGNED_CARGOS = "GetUnassignedCargos";
  public static final String SVC_GET_PROFIT = "GetProfit";
  public static final String SVC_GET_FX_DATA = "GetFxData";
  public static final String SVC_GET_SS_DATA = "GetSsData";
  public static final String SVC_GET_DTB_DATA = "GetDtbData";
  public static final String SVC_GET_TRUCK_TB_DATA = "GetTruckTbData";
  public static final String SVC_GET_TRAILER_TB_DATA = "GetTrailerTbData";
  public static final String SVC_GET_COLORS = "GetColors";
  public static final String SVC_GET_CARGO_USAGE = "GetCargoUsage";
  public static final String SVC_GET_CARGO_TOTAL = "GetCargoTotal";
  public static final String SVC_GET_ASSESSMENT_TOTALS = "GetAssessmentTotals";
  public static final String SVC_GET_ASSESSMENT_QUANTITY_REPORT = "GetAssessmentQuantityReport";
  public static final String SVC_GET_ASSESSMENT_TURNOVER_REPORT = "GetAssessmentTurnoverReport";
  public static final String SVC_CREATE_INVOICE_ITEMS = "CreateInvoiceItems";
  public static final String SVC_GET_IMPORT_MAPPINGS = "GetImportMappings";
  public static final String SVC_DO_IMPORT = "DoImport";
  public static final String SVC_GET_CREDIT_INFO = "GetCreditInfo";

  public static final String SVC_SEND_TO_ERP = "SendToERP";
  public static final String SVC_SEND_MESSAGE = "SendMessage";

  public static final String PRM_ERP_REFRESH_INTERVAL = "ERPRefreshIntervalInMinutes";
  public static final String PRM_MESSAGE_TEMPLATE = "MessageTemplate";
  public static final String PRM_INVOICE_PREFIX = "InvoicePrefix";

  public static final String VAR_INCOME = "Income";
  public static final String VAR_EXPENSE = "Expense";

  public static final String VAR_MAPPING_TABLE = "MappingTable";
  public static final String VAR_MAPPING_FIELD = "MappingField";
  public static final String VAR_IMPORT_FILE = "File";
  public static final String VAR_IMPORT_START_ROW = "Row";
  public static final String VAR_IMPORT_DATE_FORMAT = "DateFormat";

  public static final String VAR_IMPORT_LOGIN = "Login";
  public static final String VAR_IMPORT_PASSWORD = "Password";

  public static final String TBL_TRANSPORT_GROUPS = "TransportGroups";

  public static final String TBL_VEHICLES = "Vehicles";
  public static final String TBL_VEHICLE_SERVICES = "VehicleServices";
  public static final String TBL_VEHICLE_TRACKING = "VehicleTracking";
  public static final String TBL_VEHICLE_GROUPS = "VehicleGroups";
  public static final String TBL_VEHICLE_SERVICE_TYPES = "ServiceTypes";
  public static final String TBL_EXPEDITION_TYPES = "ExpeditionTypes";

  public static final String TBL_TRIPS = "Trips";
  public static final String TBL_TRIP_DRIVERS = "TripDrivers";
  public static final String TBL_TRIP_COSTS = "TripCosts";
  public static final String TBL_TRIP_FUEL_COSTS = "TripFuelCosts";
  public static final String TBL_TRIP_ROUTES = "TripRoutes";
  public static final String TBL_TRIP_FUEL_CONSUMPTIONS = "TripFuelConsumptions";
  public static final String TBL_TRIP_USAGE = "TripUsage";

  public static final String TBL_TRANSPORT_SETTINGS = "TransportSettings";

  public static final String TBL_ORDERS = "TransportationOrders";
  public static final String TBL_ORDER_CARGO = "OrderCargo";
  public static final String TBL_CARGO_TRIPS = "CargoTrips";
  public static final String TBL_CARGO_PLACES = "CargoPlaces";
  public static final String TBL_CARGO_INCOMES = "CargoIncomes";
  public static final String TBL_CARGO_INCOMES_USAGE = "CargoIncomesUsage";
  public static final String TBL_CARGO_EXPENSES = "CargoExpenses";
  public static final String TBL_CARGO_EXPENSES_USAGE = "CargoExpensesUsage";
  public static final String TBL_CARGO_HANDLING = "CargoHandling";
  public static final String TBL_SERVICES = "Services";

  public static final String TBL_ASSESSMENTS = "Assessments";
  public static final String TBL_ASSESSMENTS_USAGE = "AssessmentsUsage";
  public static final String TBL_ASSESSMENT_FORWARDERS = "AssessmentForwarders";
  public static final String TBL_SALES_USAGE = "SalesUsage";

  public static final String TBL_DRIVERS = "Drivers";
  public static final String TBL_DRIVER_GROUPS = "DriverGroups";
  public static final String TBL_DRIVER_ABSENCE = "DriverAbsence";
  public static final String TBL_ABSENCE_TYPES = "AbsenceTypes";

  public static final String TBL_FUEL_TYPES = "FuelTypes";

  public static final String TBL_REGISTRATIONS = "TranspRegistrations";
  public static final String TBL_SHIPMENT_REQUESTS = "ShipmentRequests";
  public static final String TBL_CARGO_REQUESTS = "CargoRequests";

  public static final String TBL_IMPORT_OPTIONS = "ImportOptions";
  public static final String TBL_IMPORT_PROPERTIES = "ImportProperties";
  public static final String TBL_IMPORT_MAPPINGS = "ImportMappings";

  public static final String VIEW_ORDERS = "TransportationOrders";

  public static final String VIEW_ORDER_CARGO = "OrderCargo";
  public static final String VIEW_CARGO_TRIPS = "CargoTrips";
  public static final String VIEW_CARGO_HANDLING = "CargoHandling";

  public static final String VIEW_ALL_CARGO = "AllCargo";
  public static final String VIEW_WAITING_CARGO = "WaitingCargo";
  public static final String VIEW_CARGO_PURCHASES = "CargoPurchases";
  public static final String VIEW_CARGO_SALES = "CargoSales";
  public static final String VIEW_CARGO_CREDIT_SALES = "CargoCreditSales";
  public static final String VIEW_CARGO_INVOICES = "CargoInvoices";
  public static final String VIEW_CARGO_CREDIT_INVOICES = "CargoCreditInvoices";
  public static final String VIEW_CARGO_PURCHASE_INVOICES = "CargoPurchaseInvoices";
  public static final String VIEW_CARGO_INCOMES = "CargoIncomes";

  public static final String VIEW_TRIPS = TBL_TRIPS;
  public static final String VIEW_EXPEDITION_TRIPS = "ExpeditionTrips";
  public static final String VIEW_ACTIVE_TRIPS = "ActiveTrips";

  public static final String VIEW_TRIP_CARGO = "TripCargo";
  public static final String VIEW_TRIP_DRIVERS = "TripDrivers";
  public static final String VIEW_TRIP_COSTS = "TripCosts";
  public static final String VIEW_TRIP_FUEL_COSTS = "TripFuelCosts";

  public static final String VIEW_VEHICLES = "Vehicles";
  public static final String VIEW_VEHICLE_SERVICES = "VehicleServices";
  public static final String VIEW_FUEL_CONSUMPTIONS = "FuelConsumptions";
  public static final String VIEW_FUEL_TEMPERATURES = "FuelTemperatures";

  public static final String VIEW_SPARE_PARTS = "SpareParts";

  public static final String VIEW_TRANSPORT_SETTINGS = "TransportSettings";

  public static final String VIEW_DRIVERS = "Drivers";
  public static final String VIEW_DRIVER_ABSENCE = "DriverAbsence";
  public static final String VIEW_ABSENCE_TYPES = "AbsenceTypes";

  public static final String VIEW_ASSESSMENTS = "Assessments";
  public static final String VIEW_CHILD_ASSESSMENTS = "ChildAssessments";
  public static final String VIEW_ASSESSMENT_TRANSPORTATIONS = "AssessmentTransportations";
  public static final String VIEW_ASSESSMENT_EXECUTORS = "AssessmentExecutors";

  public static final String VIEW_REGISTRATIONS = "TranspRegistrations";
  public static final String VIEW_SHIPMENT_REQUESTS = "ShipmentRequests";
  public static final String VIEW_CARGO_REQUESTS = "CargoRequests";
  public static final String VIEW_CARGO_REQUEST_TEMPLATES = "CargoReqTemplates";
  public static final String VIEW_CARGO_REQUEST_FILES = "CargoRequestFiles";

  public static final String VIEW_EXPEDITION_TYPES = "ExpeditionTypes";
  public static final String VIEW_SHIPPING_TERMS = "ShippingTerms";

  public static final String COL_GROUP = "Group";

  public static final String COL_TRIP = "Trip";
  public static final String COL_TRIP_ID = "TripID";
  public static final String COL_TRIP_NO = "TripNo";
  public static final String COL_TRIP_DATE = "Date";
  public static final String COL_TRIP_DATE_FROM = "DateFrom";
  public static final String COL_TRIP_DATE_TO = "DateTo";
  public static final String COL_TRIP_PLANNED_END_DATE = "PlannedEndDate";
  public static final String COL_TRIP_PERCENT = "TripPercent";
  public static final String COL_TRIP_NOTES = "Notes";
  public static final String COL_TRIP_STATUS = "Status";

  public static final String COL_EXPEDITION = "Expedition";
  public static final String COL_FORWARDER = "Forwarder";
  public static final String COL_FORWARDER_VEHICLE = "ForwarderVehicle";

  public static final String COL_CARGO = "Cargo";
  public static final String COL_CARGO_DESCRIPTION = "Description";
  public static final String COL_CARGO_ID = "CargoID";
  public static final String COL_CARGO_INCOME = "CargoIncome";
  public static final String COL_CARGO_PERCENT = "CargoPercent";
  public static final String COL_CARGO_MESSAGE = "Message";
  public static final String COL_CARGO_TRIP = "CargoTrip";
  public static final String COL_CARGO_TRIP_ID = "CargoTripID";
  public static final String COL_CARGO_CMR = "Cmr";
  public static final String COL_CARGO_NOTES = "Notes";
  public static final String COL_CARGO_SHIPPING_TERM = "ShippingTerm";
  public static final String COL_CARGO_QUANTITY = "Quantity";
  public static final String COL_CARGO_WEIGHT = "Weight";
  public static final String COL_CARGO_VOLUME = "Volume";
  public static final String COL_CARGO_LDM = "LDM";
  public static final String COL_CARGO_LENGTH = "Length";
  public static final String COL_CARGO_WIDTH = "Width";
  public static final String COL_CARGO_HEIGHT = "Height";
  public static final String COL_CARGO_PALETTES = "Palettes";
  public static final String COL_CARGO_VALUE = "Value";
  public static final String COL_CARGO_VALUE_CURRENCY = "ValueCurrency";

  public static final String COL_CARGO_HANDLING_NOTES = "Notes";

  public static final String COL_ASSESSMENT = "Assessment";
  public static final String COL_ASSESSMENT_ID = "AssessmentID";
  public static final String COL_ASSESSMENT_STATUS = "Status";
  public static final String COL_ASSESSMENT_NOTES = "Notes";
  public static final String COL_ASSESSMENT_LOG = "Log";
  public static final String COL_ASSESSMENT_EXPENSES = "ExpensesRegistered";

  public static final String COL_STATUS = "Status";
  public static final String COL_OWNER = "Owner";
  public static final String COL_OWNER_NAME = "OwnerName";
  public static final String COL_MODEL = "Model";
  public static final String COL_PARENT_MODEL_NAME = "ParentModelName";
  public static final String COL_MODEL_NAME = "ModelName";
  public static final String COL_NUMBER = "Number";
  public static final String COL_TYPE_NAME = "TypeName";
  public static final String COL_EXPORTED = "Exported";

  public static final String COL_COSTS_DATE = "Date";
  public static final String COL_COSTS_ITEM = "Item";
  public static final String COL_COSTS_QUANTITY = "Quantity";
  public static final String COL_COSTS_PRICE = "Price";
  public static final String COL_COSTS_CURRENCY = "Currency";
  public static final String COL_COSTS_VAT = "Vat";
  public static final String COL_COSTS_COUNTRY = "Country";
  public static final String COL_COSTS_SUPPLIER = "Supplier";
  public static final String COL_COSTS_NOTE = "Note";
  public static final String COL_COSTS_EXTERNAL_ID = "ExternalID";

  public static final String COL_ORDER = "Order";
  public static final String COL_ORDER_ID = "OrderID";
  public static final String COL_ORDER_NO = "OrderNo";
  public static final String COL_ORDER_DATE = "Date";
  public static final String COL_ORDER_MANAGER = "Manager";
  public static final String COL_CUSTOMER = "Customer";
  public static final String COL_CUSTOMER_NAME = "CustomerName";
  public static final String COL_PAYER = "Payer";
  public static final String COL_PAYER_NAME = "PayerName";

  public static final String COL_SERVICE = "Service";
  public static final String COL_SERVICE_NAME = "ServiceName";
  public static final String COL_TRANSPORTATION = "Transportation";
  public static final String COL_DATE = "Date";
  public static final String COL_AMOUNT = "Amount";
  public static final String COL_NOTE = "Note";

  public static final String COL_DESCRIPTION = "Description";
  public static final String COL_CARGO_DIRECTIONS = "Directions";

  public static final String COL_UNLOADING_PLACE = "UnloadingPlace";
  public static final String COL_LOADING_PLACE = "LoadingPlace";

  public static final String COL_PLACE_DATE = "Date";
  public static final String COL_PLACE_COMPANY = "Company";
  public static final String COL_PLACE_CONTACT = "Contact";
  public static final String COL_PLACE_CITY = "City";
  public static final String COL_PLACE_COUNTRY = "Country";
  public static final String COL_PLACE_ADDRESS = "Address";
  public static final String COL_PLACE_POST_INDEX = "PostIndex";
  public static final String COL_PLACE_PHONE = "Phone";
  public static final String COL_PLACE_FAX = "Fax";
  public static final String COL_PLACE_NUMBER = "Number";

  public static final String COL_VEHICLE_ID = "VehicleID";
  public static final String COL_VEHICLE = "Vehicle";
  public static final String COL_TRAILER = "Trailer";
  public static final String COL_VEHICLE_NUMBER = "Number";
  public static final String COL_FUEL = "Fuel";

  public static final String COL_VEHICLE_START_DATE = "StartDate";
  public static final String COL_VEHICLE_END_DATE = "EndDate";
  public static final String COL_VEHICLE_NOTES = "Notes";

  public static final String COL_DRIVER = "Driver";
  public static final String COL_DRIVER_PERSON = "CompanyPerson";
  public static final String COL_DRIVER_START_DATE = "StartDate";
  public static final String COL_DRIVER_END_DATE = "EndDate";
  public static final String COL_DRIVER_EXPERIENCE = "Experience";
  public static final String COL_DRIVER_NOTES = "Notes";

  public static final String COL_TRIP_DRIVER_FROM = "DateFrom";
  public static final String COL_TRIP_DRIVER_TO = "DateTo";
  public static final String COL_TRIP_DRIVER_NOTE = "Note";

  public static final String COL_VEHICLE_SERVICE_DATE = "Date";
  public static final String COL_VEHICLE_SERVICE_DATE_TO = "DateTo";
  public static final String COL_VEHICLE_SERVICE_TYPE = "Type";
  public static final String COL_VEHICLE_SERVICE_NAME = "Name";
  public static final String COL_VEHICLE_SERVICE_NOTES = "Notes";

  public static final String COL_ABSENCE_NAME = "Name";
  public static final String COL_ABSENCE_LABEL = "Label";
  public static final String COL_ABSENCE_COLOR = "Color";

  public static final String COL_ABSENCE = "Absence";
  public static final String COL_ABSENCE_FROM = "DateFrom";
  public static final String COL_ABSENCE_TO = "DateTo";
  public static final String COL_ABSENCE_NOTES = "Notes";

  public static final String COL_IS_TRUCK = "IsTruck";
  public static final String COL_IS_TRAILER = "IsTrailer";

  public static final String COL_FX_PIXELS_PER_CUSTOMER = "FxPixelsPerCustomer";
  public static final String COL_FX_PIXELS_PER_ORDER = "FxPixelsPerOrder";

  public static final String COL_FX_COUNTRY_FLAGS = "FxCountryFlags";
  public static final String COL_FX_PLACE_INFO = "FxPlaceInfo";

  public static final String COL_FX_PLACE_CITIES = "FxPlaceCities";
  public static final String COL_FX_PLACE_CODES = "FxPlaceCodes";

  public static final String COL_FX_PIXELS_PER_DAY = "FxPixelsPerDay";
  public static final String COL_FX_PIXELS_PER_ROW = "FxPixelsPerRow";

  public static final String COL_FX_HEADER_HEIGHT = "FxHeaderHeight";
  public static final String COL_FX_FOOTER_HEIGHT = "FxFooterHeight";

  public static final String COL_FX_THEME = "FxTheme";

  public static final String COL_FX_ITEM_OPACITY = "FxItemOpacity";
  public static final String COL_FX_STRIP_OPACITY = "FxStripOpacity";

  public static final String COL_SS_PIXELS_PER_TRUCK = "SsPixelsPerTruck";
  public static final String COL_SS_PIXELS_PER_TRIP = "SsPixelsPerTrip";

  public static final String COL_SS_SEPARATE_TRIPS = "SsSeparateTrips";
  public static final String COL_SS_SEPARATE_CARGO = "SsSeparateCargo";

  public static final String COL_SS_COUNTRY_FLAGS = "SsCountryFlags";
  public static final String COL_SS_PLACE_INFO = "SsPlaceInfo";
  public static final String COL_SS_PLACE_CITIES = "SsPlaceCities";
  public static final String COL_SS_PLACE_CODES = "SsPlaceCodes";

  public static final String COL_SS_PIXELS_PER_DAY = "SsPixelsPerDay";
  public static final String COL_SS_PIXELS_PER_ROW = "SsPixelsPerRow";

  public static final String COL_SS_HEADER_HEIGHT = "SsHeaderHeight";
  public static final String COL_SS_FOOTER_HEIGHT = "SsFooterHeight";

  public static final String COL_SS_THEME = "SsTheme";

  public static final String COL_SS_ITEM_OPACITY = "SsItemOpacity";
  public static final String COL_SS_STRIP_OPACITY = "SsStripOpacity";

  public static final String COL_DTB_PIXELS_PER_DRIVER = "DtbPixelsPerDriver";

  public static final String COL_DTB_COUNTRY_FLAGS = "DtbCountryFlags";
  public static final String COL_DTB_PLACE_INFO = "DtbPlaceInfo";

  public static final String COL_DTB_PLACE_CITIES = "DtbPlaceCities";
  public static final String COL_DTB_PLACE_CODES = "DtbPlaceCodes";

  public static final String COL_DTB_PIXELS_PER_DAY = "DtbPixelsPerDay";
  public static final String COL_DTB_PIXELS_PER_ROW = "DtbPixelsPerRow";

  public static final String COL_DTB_HEADER_HEIGHT = "DtbHeaderHeight";
  public static final String COL_DTB_FOOTER_HEIGHT = "DtbFooterHeight";

  public static final String COL_DTB_COLOR = "DtbColor";

  public static final String COL_DTB_ITEM_OPACITY = "DtbItemOpacity";
  public static final String COL_DTB_STRIP_OPACITY = "DtbStripOpacity";

  public static final String COL_TRUCK_PIXELS_PER_NUMBER = "TruckPixelsPerNumber";
  public static final String COL_TRUCK_PIXELS_PER_INFO = "TruckPixelsPerInfo";

  public static final String COL_TRUCK_SEPARATE_CARGO = "TruckSeparateCargo";
  public static final String COL_TRUCK_COUNTRY_FLAGS = "TruckCountryFlags";
  public static final String COL_TRUCK_PLACE_INFO = "TruckPlaceInfo";

  public static final String COL_TRUCK_PLACE_CITIES = "TruckPlaceCities";
  public static final String COL_TRUCK_PLACE_CODES = "TruckPlaceCodes";

  public static final String COL_TRUCK_PIXELS_PER_DAY = "TruckPixelsPerDay";
  public static final String COL_TRUCK_PIXELS_PER_ROW = "TruckPixelsPerRow";

  public static final String COL_TRUCK_HEADER_HEIGHT = "TruckHeaderHeight";
  public static final String COL_TRUCK_FOOTER_HEIGHT = "TruckFooterHeight";

  public static final String COL_TRUCK_THEME = "TruckTheme";

  public static final String COL_TRUCK_ITEM_OPACITY = "TruckItemOpacity";
  public static final String COL_TRUCK_STRIP_OPACITY = "TruckStripOpacity";

  public static final String COL_TRAILER_PIXELS_PER_NUMBER = "TrailerPixelsPerNumber";
  public static final String COL_TRAILER_PIXELS_PER_INFO = "TrailerPixelsPerInfo";

  public static final String COL_TRAILER_SEPARATE_CARGO = "TrailerSeparateCargo";
  public static final String COL_TRAILER_COUNTRY_FLAGS = "TrailerCountryFlags";
  public static final String COL_TRAILER_PLACE_INFO = "TrailerPlaceInfo";
  public static final String COL_TRAILER_PLACE_CITIES = "TrailerPlaceCities";
  public static final String COL_TRAILER_PLACE_CODES = "TrailerPlaceCodes";

  public static final String COL_TRAILER_PIXELS_PER_DAY = "TrailerPixelsPerDay";
  public static final String COL_TRAILER_PIXELS_PER_ROW = "TrailerPixelsPerRow";

  public static final String COL_TRAILER_HEADER_HEIGHT = "TrailerHeaderHeight";
  public static final String COL_TRAILER_FOOTER_HEIGHT = "TrailerFooterHeight";

  public static final String COL_TRAILER_THEME = "TrailerTheme";

  public static final String COL_TRAILER_ITEM_OPACITY = "TrailerItemOpacity";
  public static final String COL_TRAILER_STRIP_OPACITY = "TrailerStripOpacity";

  public static final String COL_CARGO_REQUEST_DATE = "Date";
  public static final String COL_CARGO_REQUEST_USER = "User";
  public static final String COL_CARGO_REQUEST_STATUS = "Status";
  public static final String COL_CARGO_REQUEST_EXPEDITION = "Expedition";
  public static final String COL_CARGO_REQUEST_CARGO = "Cargo";
  public static final String COL_CARGO_REQUEST_MANAGER = "Manager";

  public static final String COL_CARGO_REQUEST_TEMPLATE_NAME = "Name";
  public static final String COL_CARGO_REQUEST_TEMPLATE_USER = "User";

  public static final String COL_REGISTRATION_DATE = "Date";
  public static final String COL_REGISTRATION_STATUS = "Status";

  public static final String COL_REGISTRATION_COMPANY_NAME = "CompanyName";
  public static final String COL_REGISTRATION_COMPANY_CODE = "CompanyCode";
  public static final String COL_REGISTRATION_VAT_CODE = "VatCode";
  public static final String COL_REGISTRATION_CONTACT = "Contact";
  public static final String COL_REGISTRATION_CONTACT_POSITION = "ContactPosition";

  public static final String COL_REGISTRATION_ADDRESS = "Address";
  public static final String COL_REGISTRATION_CITY = "City";
  public static final String COL_REGISTRATION_COUNTRY = "Country";

  public static final String COL_REGISTRATION_PHONE = "Phone";
  public static final String COL_REGISTRATION_MOBILE = "Mobile";

  public static final String COL_REGISTRATION_FAX = "Fax";
  public static final String COL_REGISTRATION_EMAIL = "Email";

  public static final String COL_REGISTRATION_EXCHANGE_CODE = "ExchangeCode";

  public static final String COL_REGISTRATION_BANK = "Bank";
  public static final String COL_REGISTRATION_BANK_ADDRESS = "BankAddress";
  public static final String COL_REGISTRATION_BANK_ACCOUNT = "BankAccount";
  public static final String COL_REGISTRATION_SWIFT = "Swift";

  public static final String COL_REGISTRATION_NOTES = "Notes";
  public static final String COL_REGISTRATION_HOST = "Host";
  public static final String COL_REGISTRATION_AGENT = "Agent";

  public static final String COL_QUERY_DATE = "Date";
  public static final String COL_QUERY_STATUS = "Status";
  public static final String COL_QUERY_CUSTOMER_NAME = "CustomerName";
  public static final String COL_QUERY_CUSTOMER_CODE = "CustomerCode";
  public static final String COL_QUERY_CUSTOMER_VAT_CODE = "CustomerVatCode";
  public static final String COL_QUERY_CUSTOMER_ADDRESS = "CustomerAddress";
  public static final String COL_QUERY_CUSTOMER_PHONE = "CustomerPhone";
  public static final String COL_QUERY_CUSTOMER_EMAIL = "CustomerEmail";
  public static final String COL_QUERY_CUSTOMER_CONTACT = "CustomerContact";
  public static final String COL_QUERY_CUSTOMER_CONTACT_POSITION = "CustomerContactPosition";
  public static final String COL_QUERY_CUSTOMER_EXCHANGE_CODE = "CustomerExchangeCode";
  public static final String COL_QUERY_LOADING_EMAIL = "LoadingEmail";
  public static final String COL_QUERY_LOADING_CITY = "LoadingCity";
  public static final String COL_QUERY_UNLOADING_EMAIL = "UnloadingEmail";
  public static final String COL_QUERY_UNLOADING_CITY = "UnloadingCity";
  public static final String COL_QUERY_EXPEDITION = "Expedition";
  public static final String COL_QUERY_DELIVERY_DATE = "DeliveryDate";
  public static final String COL_QUERY_DELIVERY_TIME = "DeliveryTime";
  public static final String COL_QUERY_TERMS_OF_DELIVERY = "TermsOfDelivery";
  public static final String COL_QUERY_CUSTOMS_BROKERAGE = "CustomsBrokerage";
  public static final String COL_QUERY_FREIGHT_INSURANCE = "FreightInsurance";
  public static final String COL_QUERY_CARGO = "Cargo";
  public static final String COL_QUERY_MANAGER = "Manager";
  public static final String COL_QUERY_NOTES = "Notes";
  public static final String COL_QUERY_HOST = "Host";
  public static final String COL_QUERY_AGENT = "Agent";

  public static final String COL_EXPEDITION_TYPE_NAME = "Name";
  public static final String COL_EXPEDITION_TYPE_SELF_SERVICE = "SelfService";

  public static final String COL_SHIPPING_TERM_NAME = "Name";
  public static final String COL_SHIPPING_TERM_SELF_SERVICE = "SelfService";

  public static final String COL_CRF_REQUEST = "CargoRequest";

  public static final String COL_IMPORT_OPTION = "Option";
  public static final String COL_IMPORT_TYPE = "Type";
  public static final String COL_IMPORT_PROPERTY = "Property";
  public static final String COL_IMPORT_VALUE = "Value";
  public static final String COL_IMPORT_MAPPING = "Mapping";

  public static final String FORM_NEW_VEHICLE = "NewVehicle";
  public static final String FORM_ORDER = "TransportationOrder";
  public static final String FORM_TRIP = "Trip";
  public static final String FORM_EXPEDITION_TRIP = "ExpeditionTrip";
  public static final String FORM_CARGO = "OrderCargo";
  public static final String FORM_ASSESSMENT = "Assessment";
  public static final String FORM_ASSESSMENT_FORWARDER = "AssessmentForwarder";
  public static final String FORM_ASSESSMENT_TRANSPORTATION = "AssessmentTransportation";

  public static final String FORM_NEW_CARGO_INVOICE = "NewCargoInvoice";
  public static final String FORM_NEW_CARGO_PURCHASE_INVOICE = "NewCargoPurchaseInvoice";
  public static final String FORM_NEW_CARGO_CREDIT_INVOICE = "NewCargoCreditInvoice";
  public static final String FORM_CARGO_INVOICE = "CargoInvoice";
  public static final String FORM_CARGO_PURCHASE_INVOICE = "CargoPurchaseInvoice";

  public static final String FORM_FX_SETTINGS = "TrFxSettings";
  public static final String FORM_SS_SETTINGS = "TrSsSettings";
  public static final String FORM_DTB_SETTINGS = "TrDtbSettings";
  public static final String FORM_TRUCK_SETTINGS = "TruckTbSettings";
  public static final String FORM_TRAILER_SETTINGS = "TrailerTbSettings";

  public static final String FORM_REGISTRATION = "TranspRegistration";
  public static final String FORM_SHIPMENT_REQUEST = "ShipmentRequest";
  public static final String FORM_NEW_CARGO_REQUEST = "NewCargoRequest";
  public static final String FORM_CARGO_REQUEST = "CargoRequest";

  public static final String FORM_IMPORT_OPTION = "ImportOption";

  public static final String GRID_ASSESSMENT_REQUESTS = "AssessmentRequests";
  public static final String GRID_ASSESSMENT_ORDERS = "AssessmentOrders";

  public static final String PROP_COLORS = "Colors";
  public static final String PROP_COUNTRIES = "Countries";
  public static final String PROP_CITIES = "Cities";
  public static final String PROP_DRIVERS = "Drivers";
  public static final String PROP_ABSENCE = "Absence";
  public static final String PROP_VEHICLES = "Vehicles";
  public static final String PROP_VEHICLE_SERVICES = "VehicleServices";
  public static final String PROP_ORDER_CARGO = "OrderCargo";
  public static final String PROP_TRIPS = "Trips";
  public static final String PROP_TRIP_DRIVERS = "TripDrivers";
  public static final String PROP_FREIGHTS = "Freights";
  public static final String PROP_CARGO_HANDLING = "CargoHandling";

  public static final String ALS_TRIP_DATE = "TripDate";
  public static final String ALS_ORDER_DATE = "OrderDate";
  public static final String ALS_ORDER_STATUS = "OrderStatus";
  public static final String ALS_ORDER_NOTES = "OrderNotes";

  public static final String ALS_FORWARDER_NAME = "ForwarderName";
  public static final String ALS_EXPEDITION_TYPE = "ExpeditionType";
  public static final String ALS_ASSESSMENT_FORWARDER = "AssessmentForwarder";

  public static final String ALS_VEHICLE_NUMBER = "VehicleNumber";
  public static final String ALS_TRAILER_NUMBER = "TrailerNumber";

  public static final String ALS_TRIP_VERSION = "TripVersion";
  public static final String ALS_CARGO_TRIP_VERSION = "CargoTripVersion";

  public static final String ALS_ABSENCE_NAME = "AbsenceName";
  public static final String ALS_ABSENCE_LABEL = "AbsenceLabel";
  public static final String ALS_CUSTOMER_NAME = "CustomerName";

  public static final String ALS_CARGO_DESCRIPTION = "CargoDescription";

  public static final String ALS_LOADING_DATE = "LoadingDate";
  public static final String ALS_LOADING_NUMBER = "LoadingNumber";
  public static final String ALS_LOADING_CONTACT = "LoadingContact";
  public static final String ALS_LOADING_COMPANY = "LoadingCompany";
  public static final String ALS_LOADING_ADDRESS = "LoadingAddress";
  public static final String ALS_LOADING_POST_INDEX = "LoadingPostIndex";
  public static final String ALS_LOADING_CITY_NAME = "LoadingCityName";
  public static final String ALS_LOADING_COUNTRY_NAME = "LoadingCountryName";
  public static final String ALS_LOADING_COUNTRY_CODE = "LoadingCountryCode";

  public static final String ALS_UNLOADING_DATE = "UnloadingDate";
  public static final String ALS_UNLOADING_NUMBER = "UnloadingNumber";
  public static final String ALS_UNLOADING_CONTACT = "UnloadingContact";
  public static final String ALS_UNLOADING_COMPANY = "UnloadingCompany";
  public static final String ALS_UNLOADING_ADDRESS = "UnLoadingAddress";
  public static final String ALS_UNLOADING_POST_INDEX = "UnloadingPostIndex";
  public static final String ALS_UNLOADING_CITY_NAME = "UnloadingCityName";
  public static final String ALS_UNLOADING_COUNTRY_NAME = "UnloadingCountryName";
  public static final String ALS_UNLOADING_COUNTRY_CODE = "UnloadingCountryCode";

  public static final String ALS_PAYER_NAME = "PayerName";

  public static final String ALS_REQUEST_CUSTOMER_FIRST_NAME = "CustomerFirstName";
  public static final String ALS_REQUEST_CUSTOMER_LAST_NAME = "CustomerLastName";
  public static final String ALS_REQUEST_CUSTOMER_COMPANY = "CustomerCompany";

  public static final String DATA_TYPE_ORDER_CARGO = "OrderCargo";
  public static final String DATA_TYPE_TRIP = "Trip";
  public static final String DATA_TYPE_FREIGHT = "Freight";
  public static final String DATA_TYPE_TRUCK = "Truck";
  public static final String DATA_TYPE_TRAILER = "Trailer";
  public static final String DATA_TYPE_DRIVER = "Driver";

  public static final String DEFAULT_CARGO_DESCRIPTION = "*";

  public static final String AR_DEPARTMENT = "Department";
  public static final String AR_MANAGER = "Manager";
  public static final String AR_CUSTOMER = "Customer";

  public static final String AR_RECEIVED = "Received";
  public static final String AR_ANSWERED = "Answered";
  public static final String AR_LOST = "Lost";
  public static final String AR_APPROVED = "Approved";

  public static final String AR_SECONDARY = "Secondary";

  public static final String AR_INCOME = "Income";
  public static final String AR_EXPENSE = "Expense";
  public static final String AR_SECONDARY_INCOME = "SecondaryIncome";
  public static final String AR_SECONDARY_EXPENSE = "SecondaryExpense";
  
  public static final String STYLE_SHEET = "transport";

  public static String defaultLoadingColumnAlias(String colName) {
    return "DefLoad" + colName;
  }

  public static String defaultUnloadingColumnAlias(String colName) {
    return "DefUnload" + colName;
  }

  public static String loadingColumnAlias(String colName) {
    return "Loading" + colName;
  }

  public static String unloadingColumnAlias(String colName) {
    return "Unloading" + colName;
  }

  private TransportConstants() {
  }
}
